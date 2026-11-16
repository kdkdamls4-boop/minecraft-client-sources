package net.skycore.client.module.misc;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.FinishedHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class AutoDuelModule extends ClientModule implements MinecraftAccess {
   private String pendingBetEsc;
   private static final Pattern PLAYER_NAME = Pattern.compile("^\\w{3,16}$");
   private static final List<String> KIT_MODES = List.of(
      "Shield", "Thorns III", "Bow", "Totems", "Healing", "Heads", "Classic", "Cheater Paradise", "Netherite"
   );
   private double lastPosY;
   private long nextBet;
   private String pendingBal;
   private final FinishedHelper counter2;
   private String queuedDuel;
   private final List<String> sent;
   private long betEscAt;
   private long spamUntil;
   private double lastPosZ;
   private final StringSetting minBalance;
   private static final long SPAM_WAIT_MS = 8000L;
   private final MultiSelectSetting mode = this.add(new MultiSelectSetting("Kit", KIT_MODES, new LinkedHashSet<>(KIT_MODES)));
   private final BooleanSetting noFriends = this.add(new BooleanSetting("Skip Friends", true));
   private final StringSetting betAmount;
   private static final Pattern BALANCE_LINE = Pattern.compile("(?iu)баланс\\s*»\\s*(?:баланс\\s+)?(\\w{3,16})\\s*:\\s*([\\d\\s,\\.\\u00a0]+)");
   private final FinishedHelper counter;
   public static final AutoDuelModule INSTANCE = new AutoDuelModule();
   private String queuedMoneyDuel;
   private final FinishedHelper counterTo;
   private final HashMap<String, Long> duelTimestamps;
   private double lastPosX;
   private final FinishedHelper counterChoice;
   private final BooleanSetting bet = this.add(new BooleanSetting("Bet", true));

   private long parseAmount(String text) {
      String text2 = this.digits(text);
      if (text2.isEmpty()) {
         return 0L;
      }

      try {
         return Long.parseLong(text2);
      } catch (NumberFormatException failure) {
         return 0L;
      }
   }

   private AutoDuelModule() {
      super("Auto Duel", "Automatically challenges players to duels", ModuleCategory.MISC);
      this.sent = Lists.newArrayList();
      this.counter = new FinishedHelper();
      this.counter2 = new FinishedHelper();
      this.counterChoice = new FinishedHelper();
      this.counterTo = new FinishedHelper();
      this.duelTimestamps = new HashMap<>();
      this.minBalance = this.add(new StringSetting("Min Balance", "0", 12));
      this.betAmount = this.add(new StringSetting("Amount", "1000000", 12));
      this.minBalance.visibleWhen(this::useMoneyBet);
      this.betAmount.visibleWhen(this::useMoneyBet);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onPacket);
   }

   private void onPreTick(PlayerTickEndEvent event) {
      if (this.isEnabled() && mc.player != null && mc.level != null) {
         var onlinePlayers = this.getOnlinePlayers();
         double x = Math.sqrt(
            Math.pow(this.lastPosX - mc.player.getX(), 2.0) + Math.pow(this.lastPosY - mc.player.getY(), 2.0) + Math.pow(this.lastPosZ - mc.player.getZ(), 2.0)
         );
         if (x > 500.0) {
            this.setEnabled(false);
         } else {
            this.lastPosX = mc.player.getX();
            this.lastPosY = mc.player.getY();
            this.lastPosZ = mc.player.getZ();
            if (this.counter2.finished(80L * Math.max(1, onlinePlayers.size()))) {
               this.sent.clear();
               this.counter2.reset();
            }

            boolean condition = mc.gui.screen() instanceof AbstractContainerScreen;
            if (this.useMoneyBet() && this.pendingBetEsc != null) {
               if (condition) {
                  this.closeDuelScreen();
                  this.queuedMoneyDuel = this.pendingBetEsc;
                  this.pendingBetEsc = null;
                  this.betEscAt = System.currentTimeMillis();
                  this.counter.reset();
                  return;
               }

               if (this.counter.finished(1500L)) {
                  this.queuedMoneyDuel = this.pendingBetEsc;
                  this.pendingBetEsc = null;
                  this.betEscAt = System.currentTimeMillis();
                  this.counter.reset();
               }
            }

            if (!condition && System.currentTimeMillis() >= this.spamUntil && this.counter.finished(this.commandDelayMs())) {
               if (this.useMoneyBet()) {
                  if (this.queuedMoneyDuel != null && System.currentTimeMillis() - this.betEscAt >= 250L) {
                     this.sendMoneyDuel(this.queuedMoneyDuel);
                     this.queuedMoneyDuel = null;
                  } else if (this.queuedDuel != null) {
                     this.sendDuel(this.queuedDuel);
                     this.queuedDuel = null;
                  } else if (this.pendingBal != null && this.counter.finished(2500L)) {
                     this.sent.add(this.pendingBal);
                     this.pendingBal = null;
                     this.counter.reset();
                  } else if (this.pendingBal == null && this.pendingBetEsc == null && this.queuedMoneyDuel == null) {
                     this.sendNextBal(onlinePlayers);
                  }
               } else {
                  this.sendNextPlainDuel(onlinePlayers);
               }
            }

            if (!this.useMoneyBet() && mc.player.containerMenu instanceof ChestMenu && mc.gui.screen() instanceof AbstractContainerScreen abstractContainerScreen) {
               String string = abstractContainerScreen.getTitle().getString();
               if (string.contains("Выбор набора (1/1)")) {
                  ArrayList elements = new ArrayList();
                  int value = 0;

                  for (String text : KIT_MODES) {
                     if (this.mode.isSelected(text)) {
                        elements.add(value);
                     }

                     value++;
                  }

                  Collections.shuffle(elements);
                  if (!elements.isEmpty() && this.counterChoice.finished(80L)) {
                     UseItemHelper.clickSlotId((Integer)elements.getFirst(), 0, ContainerInput.QUICK_MOVE, true);
                     this.counterChoice.reset();
                  }
               } else if (string.contains("Настройка поединка") && this.counterTo.finished(80L)) {
                  UseItemHelper.clickSlotId(0, 0, ContainerInput.QUICK_MOVE, true);
                  this.counterTo.reset();
               }
            }
         }
      }
   }

   private List<String> getOnlinePlayers() {
      if (mc.player != null && mc.player.connection != null) {
         var stream = mc.player.connection.getOnlinePlayers().stream();
         Function<net.minecraft.client.multiplayer.PlayerInfo, com.mojang.authlib.GameProfile> function = PlayerInfo::getProfile;
         var stream2 = stream.map(function);
         Function<com.mojang.authlib.GameProfile, java.lang.String> function2 = GameProfile::name;
         var stream3 = stream2.map(function2);
         Predicate<java.lang.String> predicate = text -> {
            return PLAYER_NAME.matcher(text).matches();
         };
         var stream4 = stream3.filter(predicate);
         return stream4.collect(Collectors.toList());
      } else {
         return List.of();
      }
   }

   private String digits(String text) {
      if (text != null) {
         if (!text.isEmpty()) {
            return text.replaceAll("\\D", "");
         }
      }

      return "";
   }

   private long peekNextBet() {
      return Math.max(this.baseBet(), this.nextBet);
   }

   private void sendMoneyDuel(String text) {
      if (mc.player != null && mc.player.connection != null) {
         long timestamp = this.takeNextBet();
         String text2 = "duel " + text + " " + timestamp;
         mc.player.connection.sendCommand(text2);
         this.duelTimestamps.put(text, System.currentTimeMillis());
         this.counter.reset();
      }
   }

   @Override
   protected void onEnable() {
      super.onEnable();
      this.resetQueue();
      if (mc.player != null) {
         double x = mc.player.getX();
         this.lastPosX = x;
         this.lastPosY = mc.player.getY();
         double z = mc.player.getZ();
         this.lastPosZ = z;
      }
   }

   private String strip(String text) {
      if (text == null) {
         return "";
      }

      String text2 = ChatFormatting.stripFormatting(text);
      if (text2 == null) {
         text2 = text;
      }

      String text3 = text2.replaceAll("§.", "").replace(' ', ' ');
      return text3.trim();
   }

   private void closeDuelScreen() {
      if (mc.player != null) {
         mc.player.closeContainer();
      } else {
         mc.gui.setScreen(null);
      }
   }

   private long baseBet() {
      return Math.max(0L, this.parseAmount(this.betAmount.get()));
   }

   private boolean containsAny(String text, String... stringValues) {

      for (String text2 : stringValues) {
         if (text.contains(text2)) {
            return true;
         }
      }

      return false;
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      if (this.isEnabled() && packetReceiveEvent.getPacket() instanceof ClientboundSystemChatPacket clientboundSystemChatPacket) {
         String string = this.strip(clientboundSystemChatPacket.content() == null ? null : clientboundSystemChatPacket.content().getString());
         if (!string.isEmpty()) {
            String text = string.toLowerCase(Locale.ROOT);
            if (BALANCE_LINE.matcher(string).find() || this.looksLikeBalanceSpam(text)) {
               packetReceiveEvent.cancel();
               if (!this.useMoneyBet()) {
                  return;
               }
            }

            if ((!text.contains("начало") || !text.contains("через") || !text.contains("секунд"))
               && !text.contains("дуэли » во время поединка запрещено использовать команды")) {
               if (this.containsAny(text, "не спамь", "подождите", "не так быстро", "кулдаун", "cooldown")) {
                  this.spamUntil = System.currentTimeMillis() + 8000L;
                  this.counter.reset();
               } else if (this.useMoneyBet()) {
                  Matcher matcher = BALANCE_LINE.matcher(string);
                  if (!matcher.find()) {
                     if (this.pendingBal != null && this.containsAny(text, "не найден", "не в сети", "оффлайн", "offline")) {
                        this.sent.add(this.pendingBal);
                        this.pendingBal = null;
                        this.counter.reset();
                     }
                  } else {
                     String text2 = matcher.group(1);
                     long timestamp = this.parseAmount(matcher.group(2));
                     if (text2 != null && timestamp >= 0L && this.pendingBal != null && this.pendingBal.equalsIgnoreCase(text2)) {
                        this.sent.add(this.pendingBal);
                        String pendingBal2 = this.pendingBal;
                        this.pendingBal = null;
                        this.counter.reset();
                        if (timestamp > this.requiredBalance()) {
                           this.queuedDuel = pendingBal2;
                        }
                     }
                  }
               }
            } else {
               this.setEnabled(false);
            }
         }
      }
   }

   private boolean canChallenge(String text) {
      if (!this.sent.contains(text)) {
         String name = mc.getUser().getName();
         if (!text.equals(name)) {
            Boolean get = this.noFriends.get();
            if (get && FriendManager.INSTANCE.isFriend(text)) {
               return false;
            }

            if (!this.duelTimestamps.containsKey(text)) {
               return true;
            }

            long now = System.currentTimeMillis();
            return now - this.duelTimestamps.get(text) >= 31000L;
         }
      }

      return false;
   }

   private long takeNextBet() {
      long timestamp = this.peekNextBet();
      this.nextBet = timestamp == Long.MAX_VALUE ? timestamp : timestamp + 1L;
      return timestamp;
   }

   private boolean looksLikeBalanceSpam(String text) {
      return text.contains("баланс") && (text.contains("»") || text.contains(":"));
   }

   private boolean useMoneyBet() {
      Boolean get = this.bet.get();
      if (get) {
         String text = this.betDigits();
         if (!text.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private long commandDelayMs() {
      return this.useMoneyBet() ? 1000L : 800L;
   }

   private void sendNextPlainDuel(List<String> strings) {
      Iterator iterator = strings.iterator();

      String text;
      do {
         if (!iterator.hasNext()) {
            return;
         }

         text = (String)iterator.next();
      } while (!this.canChallenge(text));

      this.sent.add(text);
      this.sendDuel(text);
   }

   private void resetQueue() {
      this.sent.clear();
      this.pendingBal = null;
      this.queuedDuel = null;
      this.pendingBetEsc = null;
      this.queuedMoneyDuel = null;
      this.betEscAt = 0L;
      this.spamUntil = 0L;
      long timestamp = this.baseBet();
      this.nextBet = timestamp;
      this.counter.reset();
      this.counter2.reset();
   }

   @Override
   protected void onDisable() {
      this.resetQueue();
      super.onDisable();
   }

   private void sendDuel(String pendingBetEsc2) {
      if (mc.player != null) {
         if (mc.player.connection != null) {
            if (this.useMoneyBet()) {
               String text = "duel " + pendingBetEsc2;
               mc.player.connection.sendCommand(text);
               this.pendingBetEsc = pendingBetEsc2;
            } else {
               mc.player.connection.sendCommand("duel " + pendingBetEsc2);
               var duelTimestamps2 = this.duelTimestamps;
               Long now = System.currentTimeMillis();
               duelTimestamps2.put(pendingBetEsc2, now);
            }

            this.counter.reset();
            return;
         }
      }
   }

   @Override
   public boolean affectsGameplay() {
      return true;
   }

   private long requiredBalance() {
      long timestamp = this.parseAmount(this.minBalance.get());
      if (this.useMoneyBet()) {
         long timestamp2 = this.peekNextBet();
         timestamp = Math.max(timestamp, timestamp2);
      }

      return Math.max(0L, timestamp);
   }

   private String betDigits() {
      return this.digits(this.betAmount.get());
   }

   private void sendNextBal(List<String> strings) {
      Iterator iterator = strings.iterator();

      String text;
      do {
         if (!iterator.hasNext()) {
            return;
         }

         text = (String)iterator.next();
      } while (!this.canChallenge(text));

      mc.player.connection.sendCommand("bal " + text);
      this.pendingBal = text;
      this.counter.reset();
   }
}
