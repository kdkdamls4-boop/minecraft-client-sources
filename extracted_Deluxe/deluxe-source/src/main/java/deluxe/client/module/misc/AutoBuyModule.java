package deluxe.client.module.misc;

import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.ui.AutoBuySettingsScreen;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.ClientChat;
import deluxe.client.event.ClientTickEvent;
import deluxe.client.ui.DeluxeAutoBuyScreen;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.ui.panel.AutoBuyPanel;
import deluxe.client.storage.AutoBuyItem;
import deluxe.client.setting.SectionSetting;
import deluxe.client.util.Stopwatch;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.InventoryS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

@ModuleInfo(name = "AutoBuy", description = "Скупает лоты с аукциона по своим ценам, меню открывается биндом.", category = ModuleCategory.MISC)
public final class AutoBuyModule extends Module {
   private static AutoBuyModule autoBuyModule;
   private static final Pattern field05376 = Pattern.compile(".*выставлен на продажу за.*");
   private static final Pattern field05377 = Pattern.compile(".*у вас купили.*");
   private static final Pattern field05378 = Pattern.compile("^\\[☃] У Вас купили (.+) за \\$([\\d,]+) на /ah$");
   private static final Pattern field05379 = Pattern.compile(".*вы успешно купили.*");
   private static final Pattern field05380 = Pattern.compile(".*максимальная цена на этот товар.*");
   private static final Pattern field05381 = Pattern.compile(".*вы убрали предмет с продажи.*");
   private static final String[] field05382 = new String[]{
      "Игрок", "Барон", "Страж", "Герой", "Аспид", "Сквид", "Глава", "Элита", "Титан", "Принц", "Князь", "Герцог"
   };
   private static final int[] field05383 = new int[]{3, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 21};
   public KeybindSetting keybindSetting;
   public BooleanSetting field05385;
   public BooleanSetting field05386;
   public NumberSetting field05387;
   public NumberSetting field05388;
   public NumberSetting field05389;
   public NumberSetting field05390;
   private boolean field05391;
   private AutoBuyModule.Nested0408 field05392 = AutoBuyModule.Nested0408.IDLE;
   private final Stopwatch field05393 = new Stopwatch();
   private final Stopwatch field05394 = new Stopwatch();
   private final Stopwatch field05395 = new Stopwatch();
   private final Stopwatch field05396 = new Stopwatch();
   private final Stopwatch field05397 = new Stopwatch();
   private final Stopwatch field05398 = new Stopwatch();
   private final Stopwatch field05399 = new Stopwatch();
   private final ArrayList<String> field05400 = new ArrayList<>();
   private final ArrayList<String> field05401 = new ArrayList<>();
   private final ArrayList<String> field05402 = new ArrayList<>();
   private final ArrayList<Integer> field05403 = new ArrayList<>();
   private final HashMap<String, Long> field05404 = new HashMap<>();
   private final HashMap<String, Long> field05405 = new HashMap<>();
   private ServerInfo field05406;
   private String field05407;
   private String field05408 = "ah";
   private String field05409 = "";
   private String field05410 = "";
   private String field05411 = "";
   private int field05412 = 1;
   private boolean field05413;
   private boolean field05414;
   private int field05415;
   private boolean field05416;
   private boolean field05417;
   private boolean field05418;
   private boolean field05419;
   private final HashSet<String> field05420 = new HashSet<>();
   private int field05421;
   private int field05422;
   private int field05423;
   private int field05424 = field05383[0];
   private final HashMap<String, Long> field05425 = new HashMap<>();
   private final HashMap<String, Integer> field05426 = new HashMap<>();
   private final Stopwatch field05427 = new Stopwatch();
   private int field05428;
   private boolean field05429;
   private final HashSet<String> field05430 = new HashSet<>();
   private final Stopwatch field05431 = new Stopwatch();
   private final Stopwatch field05432 = new Stopwatch();
   private final Stopwatch field05433 = new Stopwatch();
   private int field05434;
   private int field05435;
   private final HashSet<String> field05436 = new HashSet<>();
   private int field05437;
   private long field05438;
   private final EventListener<PlayerTickEvent> field05439 = playerTickEvent -> {
      if (mc.player != null && mc.getWindow() != null) {
         if (this.keybindSetting.isBound()
            && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle())
            && !this.field05391
            && !(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof DeluxeAutoBuyScreen)) {
            AutoBuyPanel.method3080().method3074();
            mc.setScreen(DeluxeAutoBuyScreen.instance());
         }

         this.field05391 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         this.field05399.reset();
         if (mc.getCurrentServerEntry() != null) {
            this.field05406 = mc.getCurrentServerEntry();
         }

         ScoreboardObjective scoreboardObjective = mc.world == null
            ? null
            : mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
         if (scoreboardObjective != null
            && scoreboardObjective.getDisplayName().getString().split("-").length > 1
            && !scoreboardObjective.getDisplayName().getString().split("-")[1].replaceAll("\\D", "").isEmpty()) {
            this.field05407 = "an" + scoreboardObjective.getDisplayName().getString().split("-")[1].replaceAll("\\D", "");
         }

         label838:
         for (ScoreboardEntry scoreboardEntry : scoreboardObjective == null ? List.<ScoreboardEntry>of() : mc.world.getScoreboard().getScoreboardEntries(scoreboardObjective)) {
            String local2633_5 = AuctionHelperModule.method2310()
               .method2321(
                  Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name())
                     .getString()
                     .replaceAll("(?i)§[0-9a-fk-or]", "")
               );
            if (local2633_5.contains("ранг")) {
               for (int index = 0; index < field05382.length; index++) {
                  if (local2633_5.contains(AuctionHelperModule.method2310().method2321(field05382[index]))) {
                     this.field05424 = field05383[index];
                     break label838;
                  }
               }
               break;
            }
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE
            && InventoryUtils.method0555(itemStack -> AuctionItemCatalog.method1072(itemStack.getItem())) != -1
            && (
               InventoryUtils.method0546(0, 35) == -1 && this.field05432.hasExceededMillis(8000L << this.field05435)
                  || !this.field05401.isEmpty() && this.method1740() > 20 && this.field05432.hasExceededMillis(60000L)
            )) {
            this.field05432.reset();
            this.field05393.reset();
            this.field05392 = AutoBuyModule.Nested0408.STASH;
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE
            && !this.field05401.isEmpty()
            && InventoryUtils.method0546(0, 35) == -1
            && this.field05433.hasExceededMillis(5000L << this.field05435)) {
            this.field05421 = this.field05401.size();
            this.field05393.reset();
            this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE) {
            for (int index2 = 0; index2 <= 35; index2++) {
               AutoBuyItem autoBuyItem = mc.player.getInventory().getStack(index2).isEmpty()
                  ? null
                  : AuctionItemCatalog.method1075(mc.player.getInventory().getStack(index2));
               if (autoBuyItem != null
                  && AutoBuySettingsScreen.method1139().field03122.containsKey(autoBuyItem.name())
                  && !this.field05420.contains(autoBuyItem.name())
                  && !this.field05401.contains(autoBuyItem.name())) {
                  this.field05401.add(autoBuyItem.name());
               }
            }
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE
            && this.field05423 < this.field05424
            && this.field05401.size() >= this.field05424 - this.field05423
            && this.field05431.hasExceededMillis(2000L)
            && this.field05433.hasExceededMillis(5000L << this.field05435)) {
            this.field05421 = this.field05424 - this.field05423;
            this.field05393.reset();
            this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE
            && this.field05385.isChecked()
            && !this.field05401.isEmpty()
            && this.field05423 >= this.field05424
            && this.method1746() >= this.field05423
            && this.field05431.hasExceededMillis(3000L)
            && this.field05427.hasExceededMillis(15000L)) {
            this.field05427.reset();
            this.field05393.reset();
            this.field05392 = AutoBuyModule.Nested0408.RELIST_SEARCH;
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE && this.field05397.hasExceededMillis(60000L)) {
            this.field05397.reset();
            this.field05393.reset();
            this.field05392 = AutoBuyModule.Nested0408.RESELL;
         }

         if (this.field05386.isChecked()
            && this.field05392 == AutoBuyModule.Nested0408.IDLE
            && this.field05390.intValue() > 0
            && this.field05396.hasExceededMillis(this.field05390.intValue() * 60000L)) {
            this.field05400.addAll(AutoBuySettingsScreen.method1139().field03122.keySet());
            this.field05393.reset();
            this.field05392 = AutoBuyModule.Nested0408.SEND;
         }

         switch (this.field05392) {
            case IDLE:
            default:
               {
break;
}
            case SEND:
               {
if (!this.field05393.hasExceededMillis(this.method1728(300L, 200L, 400L))) {
                  return;
               }

               if (this.field05400.isEmpty()) {
                  this.field05396.reset();
                  if (this.field05385.isChecked() && (!this.field05413 || this.field05401.isEmpty())) {
                     mc.player.networkHandler.sendChatCommand(this.field05408);
                  }

                  if (this.field05413 && !this.field05401.isEmpty()) {
                     this.field05421 = this.field05401.size();
                  }

                  this.field05393.reset();
                  this.field05392 = this.field05413 && !this.field05401.isEmpty() ? AutoBuyModule.Nested0408.SELL_PICK : AutoBuyModule.Nested0408.IDLE;
                  return;
               }

               this.field05410 = this.field05400.remove(0);
               mc.player.networkHandler.sendChatCommand("ah search " + this.field05410);
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.WAIT_LOTS;
               break;
}
            case WAIT_LOTS:
               {
if (!this.method1743()) {
                  if (!this.field05393.hasExceededMillis(2500L)) {
                     return;
                  }

                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.SEND;
                  return;
               }

               if (!this.field05393.hasExceededMillis(this.method1728(450L, 350L, 600L))) {
                  return;
               }

               int local2633_3 = -1;

               for (int index3 = 0; index3 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index3++) {
                  ItemStack itemStack2 = mc.player.currentScreenHandler.getSlot(index3).getStack();
                  if (!itemStack2.isEmpty()
                     && AuctionItemCatalog.method1055(this.field05410) != null
                     && AuctionItemCatalog.method1059(itemStack2, AuctionItemCatalog.method1055(this.field05410))
                     && AuctionHelperModule.method2310().method2314(itemStack2) > 0
                     && (local2633_3 == -1 || AuctionHelperModule.method2310().method2314(itemStack2) / Math.max(1, itemStack2.getCount()) < local2633_3)) {
                     local2633_3 = AuctionHelperModule.method2310().method2314(itemStack2) / Math.max(1, itemStack2.getCount());
                  }
               }

               if (local2633_3 != -1) {
                  this.field05404.put(this.field05410, Math.max(1L, Math.round(local2633_3 * this.field05388.floatValue() / 100.0F)));
               }

               if (local2633_3 != -1) {
                  this.field05425.remove(this.field05410);
                  this.field05426.remove(this.field05410);
               }

               if (AutoBuySettingsScreen.method1139().field03122.get(this.field05410) != null) {
                  AutoBuySettingsScreen.method1139().field03122.get(this.field05410)
                     .field03214
                     .setValue(Long.toString(Math.max(1L, Math.round(local2633_3 * this.field05387.floatValue() / 100.0F))));
                  AutoBuySettingsScreen.method1139().field03122.get(this.field05410)
                     .field03215
                     .setValue(Long.toString(Math.max(1L, Math.round(local2633_3 * this.field05388.floatValue() / 100.0F))));
               }

               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.SEND;
               break;
}
            case SELL_PICK:
               {
if (this.field05401.isEmpty() || this.field05421 <= 0) {
                  this.field05413 = false;
                  this.field05403.clear();
                  if (this.field05385.isChecked()) {
                     mc.player.networkHandler.sendChatCommand(this.field05408);
                  }

                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.IDLE;
                  return;
               }

               if (!this.field05393.hasExceededMillis(250L)) {
                  return;
               }

               if (this.field05434 > 3) {
                  this.field05434 = 0;
                  this.field05421 = 0;
                  this.field05413 = false;
                  this.field05403.clear();
                  this.field05433.reset();
                  if (this.field05385.isChecked()) {
                     mc.player.networkHandler.sendChatCommand(this.field05408);
                  }

                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.IDLE;
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
                  this.field05393.reset();
                  return;
               }

               if (AuctionItemCatalog.method1055(this.field05401.get(0)) != null
                  && AuctionItemCatalog.method1059(mc.player.getMainHandStack(), AuctionItemCatalog.method1055(this.field05401.get(0)))
                  && mc.player.getMainHandStack().getCount()
                     == this.method1736(this.field05401.get(0), mc.player.getMainHandStack().getCount())) {
                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.SELL_SEND;
                  return;
               }

               for (int index4 = 0; index4 < this.field05401.size(); index4++) {
                  for (Slot slot : mc.player.currentScreenHandler.slots) {
                     if (slot.inventory == mc.player.getInventory()
                        && AuctionItemCatalog.method1055(this.field05401.get(index4)) != null
                        && AuctionItemCatalog.method1059(slot.getStack(), AuctionItemCatalog.method1055(this.field05401.get(index4)))
                        && slot.getStack().getCount() == this.method1736(this.field05401.get(index4), slot.getStack().getCount())) {
                        this.field05401.add(0, this.field05401.remove(index4));
                        if (slot.getIndex() <= 8) {
                           InventoryUtils.method0553(slot.getIndex(), false);
                        } else {
                           InventoryUtils.method0547(slot.id, InventoryUtils.method0554(), SlotActionType.SWAP, false);
                        }

                        this.field05393.reset();
                        this.field05392 = AutoBuyModule.Nested0408.SELL_SEND;
                        return;
                     }
                  }
               }

               for (int index5 = 0; index5 < this.field05401.size(); index5++) {
                  for (Slot slot2 : mc.player.currentScreenHandler.slots) {
                     if (slot2.inventory == mc.player.getInventory()
                        && AuctionItemCatalog.method1055(this.field05401.get(index5)) != null
                        && AuctionItemCatalog.method1059(slot2.getStack(), AuctionItemCatalog.method1055(this.field05401.get(index5)))
                        && slot2.getStack().getCount() > this.method1736(this.field05401.get(index5), slot2.getStack().getCount())) {
                        for (Slot slot3 : mc.player.currentScreenHandler.slots) {
                           if (slot3.inventory == mc.player.getInventory() && !slot3.hasStack()) {
                              InventoryUtils.method0547(slot2.id, 0, SlotActionType.PICKUP, false);

                              for (int index6 = 0;
                                 index6 < this.method1736(this.field05401.get(index5), slot2.getStack().getCount());
                                 index6++
                              ) {
                                 InventoryUtils.method0547(slot3.id, 1, SlotActionType.PICKUP, false);
                              }

                              InventoryUtils.method0547(slot2.id, 0, SlotActionType.PICKUP, false);
                              this.field05393.reset();
                              return;
                           }
                        }
                     }
                  }
               }

               if (!this.field05393.hasExceededMillis(1500L)) {
                  return;
               }

               if (InventoryUtils.method0555(itemStack3 -> AuctionItemCatalog.method1072(itemStack3.getItem())) != -1
                  && this.field05432.hasExceededMillis(8000L)) {
                  this.field05432.reset();
                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.STASH;
                  return;
               }

               this.field05434++;
               this.field05401
                  .removeIf(
                     local2619_1 -> (
                           AuctionItemCatalog.method1055(local2619_1) == null
                              || InventoryUtils.method0555(
                                    itemStack4 -> AuctionItemCatalog.method1059(itemStack4, AuctionItemCatalog.method1055(local2619_1))
                                 )
                                 == -1
                        )
                        && (
                           InventoryUtils.method0555(itemStack5 -> AuctionItemCatalog.method1072(itemStack5.getItem())) == -1
                              || this.field05436.contains(local2619_1)
                        )
                  );
               this.field05435 = Math.min(6, this.field05435 + 1);
               this.field05421 = 0;
               this.field05413 = false;
               this.field05403.clear();
               this.field05433.reset();
               if (this.field05385.isChecked()) {
                  mc.player.networkHandler.sendChatCommand(this.field05408);
               }

               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.IDLE;
               break;
}
            case SELL_SEND:
               {
if (!this.field05393.hasExceededMillis(200L)) {
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
                  this.field05393.reset();
                  return;
               }

               if (this.field05401.isEmpty() || AuctionItemCatalog.method1075(mc.player.getMainHandStack()) == null) {
                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
                  return;
               }

               this.field05411 = AuctionItemCatalog.method1075(mc.player.getMainHandStack()).name();
               if ((
                     AutoBuySettingsScreen.method1139().field03122.get(this.field05411) != null
                        ? AutoBuySettingsScreen.method1135(AutoBuySettingsScreen.method1139().field03122.get(this.field05411).field03215.getValue())
                        : this.field05404.getOrDefault(this.field05411, 0L)
                  )
                  <= 0L) {
                  if (!this.field05400.contains(this.field05411)) {
                     this.field05400.add(this.field05411);
                  }

                  this.field05413 = true;
                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.SEND;
                  return;
               }

               this.field05417 = false;
               this.field05418 = false;
               this.field05419 = false;
               this.field05412 = mc.player.getMainHandStack().getCount();
               if (this.method1726(this.field05411) * this.field05412 > 10000000L && this.field05412 > 1 && this.method1737()) {
                  this.field05401.add(this.field05411);
                  this.field05393.reset();
                  return;
               }

               long local2633_3 = Math.max(1L, this.method1726(this.field05411) * mc.player.getMainHandStack().getCount());
               mc.player.networkHandler.sendChatCommand("ah sell " + local2633_3);
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.SELL_WAIT;
               break;
}
            case SELL_WAIT:
               {
if (this.field05417) {
                  this.field05417 = false;
                  this.field05434 = 0;
                  this.field05421--;
                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
                  return;
               }

               if (this.field05418) {
                  this.field05418 = false;
                  if (mc.player.getMainHandStack().getCount() > 1 && this.method1737()) {
                     this.field05401.add(this.field05411);
                     this.field05419 = true;
                     this.field05393.reset();
                     return;
                  }

                  this.field05420.add(this.field05411);
                  this.field05401.remove(this.field05411);
                  this.field05434++;
                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
                  return;
               }

               if (this.field05419) {
                  if (!this.field05393.hasExceededMillis(450L)) {
                     return;
                  }

                  this.field05419 = false;
                  mc.player
                     .networkHandler
                     .sendChatCommand("ah sell " + Math.max(1L, this.method1726(this.field05411) * mc.player.getMainHandStack().getCount()));
                  this.field05393.reset();
                  return;
               }

               if (!this.field05393.hasExceededMillis(1500L)) {
                  return;
               }

               this.field05434++;
               if (!this.field05401.isEmpty()) {
                  this.field05401.add(this.field05401.remove(0));
               }

               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
               break;
}
            case FLIP_SEARCH:
               {
if (!this.field05393.hasExceededMillis(250L)) {
                  return;
               }

               mc.player.networkHandler.sendChatCommand("ah " + mc.player.getName().getString());
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.FLIP_BUY;
               break;
}
            case FLIP_BUY:
               {
if (!this.method1743()) {
                  if (!this.field05393.hasExceededMillis(2500L)) {
                     return;
                  }

                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.IDLE;
                  return;
               }

               if (!this.field05393.hasExceededMillis(this.method1728(600L, 450L, 700L))) {
                  return;
               }

               ArrayList<Integer> local2633_3 = new ArrayList<>();

               for (int index7 = 0; index7 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index7++) {
                  ItemStack itemStack6 = mc.player.currentScreenHandler.getSlot(index7).getStack();
                  if (!itemStack6.isEmpty()
                     && AuctionItemCatalog.method1075(itemStack6) != null
                     && AuctionHelperModule.method2310().method2314(itemStack6) > 0
                     && !AuctionHelperModule.method2310()
                        .method2321(itemStack6.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                        .contains("не актуален")) {
                     local2633_3.add(index7);
                  }
               }

               local2633_3.sort(
                  Comparator.comparingInt(
                     local2621_0 -> AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(local2621_0).getStack())
                  )
               );

               for (int local2633_5 : local2633_3) {
                  if (this.field05437 <= 0) {
                     break;
                  }

                  ItemStack itemStack7 = mc.player.currentScreenHandler.getSlot(local2633_5).getStack();
                  if (!this.field05403.contains(local2633_5)
                     && (this.method1749() < 0 || AuctionHelperModule.method2310().method2314(itemStack7) <= this.method1749())) {
                     this.field05403.add(local2633_5);
                     this.field05437--;
                     this.field05400.add(AuctionItemCatalog.method1075(itemStack7).name());
                     this.field05401.add(AuctionItemCatalog.method1075(itemStack7).name());
                     InventoryUtils.method0547(local2633_5, 0, SlotActionType.QUICK_MOVE, true);
                     this.field05393.reset();
                     if (this.field05437 > 0) {
                        return;
                     }
                     break;
                  }
               }

               this.field05413 = true;
               this.field05393.reset();
               this.field05392 = this.field05400.isEmpty() ? AutoBuyModule.Nested0408.IDLE : AutoBuyModule.Nested0408.SEND;
               break;
}
            case RESELL:
               {
if (!this.field05393.hasExceededMillis(250L)) {
                  return;
               }

               mc.player.networkHandler.sendChatCommand("ah resell");
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.RESUME_PAGE;
               break;
}
            case REJOIN_HUB:
               {
if (!this.field05393.hasExceededMillis(400L)) {
                  return;
               }

               mc.player.networkHandler.sendChatCommand("hub");
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.REJOIN_ANARCHY;
               break;
}
            case REJOIN_ANARCHY:
               {
if (!this.field05393.hasExceededMillis(1500L)) {
                  return;
               }

               if (this.field05407 != null) {
                  mc.player.networkHandler.sendChatCommand(this.field05407);
               }

               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.RESUME_PAGE;
               break;
}
            case RESUME_PAGE:
               {
if (!this.field05393.hasExceededMillis(800L)) {
                  return;
               }

               if (this.field05385.isChecked()) {
                  mc.player.networkHandler.sendChatCommand(this.field05408);
               }

               this.field05403.clear();
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.IDLE;
               break;
}
            case STASH:
               {
if (!this.field05393.hasExceededMillis(120L)) {
                  return;
               }

               if (!this.method1724()) {
                  if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                     mc.player.closeHandledScreen();
                     return;
                  }

                  for (Slot slot4 : mc.player.currentScreenHandler.slots) {
                     if (slot4.inventory == mc.player.getInventory() && AuctionItemCatalog.method1072(slot4.getStack().getItem())) {
                        InventoryUtils.method0547(slot4.id, 1, SlotActionType.PICKUP, false);
                        this.field05393.reset();
                        return;
                     }
                  }

                  this.field05393.reset();
                  this.field05392 = AutoBuyModule.Nested0408.IDLE;
                  return;
               }

               for (Slot slot5 : mc.player.currentScreenHandler.slots) {
                  if (slot5.inventory != mc.player.getInventory()
                     && !this.field05401.isEmpty()
                     && AuctionItemCatalog.method1055(this.field05401.get(0)) != null
                     && AuctionItemCatalog.method1059(slot5.getStack(), AuctionItemCatalog.method1055(this.field05401.get(0)))) {
                     InventoryUtils.method0547(slot5.id, 0, SlotActionType.QUICK_MOVE, false);
                     mc.player.closeHandledScreen();
                     this.field05393.reset();
                     this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
                     return;
                  }
               }

               for (Slot slot6 : mc.player.currentScreenHandler.slots) {
                  if (slot6.inventory == mc.player.getInventory()
                     && slot6.hasStack()
                     && this.method1740() > 20
                     && !this.field05432.hasExceededMillis(8000L)
                     && AuctionItemCatalog.method1075(slot6.getStack()) != null) {
                     InventoryUtils.method0547(slot6.id, 0, SlotActionType.QUICK_MOVE, false);
                     this.field05393.reset();
                     return;
                  }
               }

               if (!this.field05401.isEmpty()) {
                  this.field05436.add(this.field05401.get(0));
               }

               mc.player.closeHandledScreen();
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.SELL_PICK;
               break;
}
            case RELIST_SEARCH:
               {
if (!this.field05393.hasExceededMillis(250L)) {
                  return;
               }

               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
                  this.field05393.reset();
                  return;
               }

               this.field05428 = 0;
               this.field05429 = true;
               this.field05430.clear();
               this.field05403.clear();
               mc.player.networkHandler.sendChatCommand("ah " + mc.player.getName().getString());
               this.field05393.reset();
               this.field05392 = AutoBuyModule.Nested0408.RELIST_PULL;
               break;
}
            case RELIST_PULL:
               {
if (!this.method1743()) {
                  if (!this.field05393.hasExceededMillis(2500L)) {
                     return;
                  }

                  this.field05393.reset();
                  this.field05392 = this.field05428 > 0 ? AutoBuyModule.Nested0408.SELL_PICK : AutoBuyModule.Nested0408.RESUME_PAGE;
                  this.field05421 = this.field05428;
                  return;
               }

               if (!this.field05429 && !this.field05393.hasExceededMillis(1500L)) {
                  return;
               }

               if (!this.field05393.hasExceededMillis(this.method1728(600L, 450L, 350L)) || System.currentTimeMillis() - this.field05438 < 90L) {
                  return;
               }

               for (int index8 = 0; index8 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index8++) {
                  ItemStack itemStack8 = mc.player.currentScreenHandler.getSlot(index8).getStack();
                  if (!itemStack8.isEmpty()
                     && AuctionItemCatalog.method1075(itemStack8) != null
                     && this.field05428 < this.field05424
                     && InventoryUtils.method0546(0, 35) != -1) {
                     String local2633_5 = AuctionItemCatalog.method1075(itemStack8).name();
                     if (this.field05430.add(local2633_5)) {
                        long local2633_6 = AuctionHelperModule.method2310().method2314(itemStack8) <= 0
                           ? this.method1726(local2633_5)
                           : AuctionHelperModule.method2310().method2314(itemStack8) / Math.max(1, itemStack8.getCount());
                        long local2633_8 = AutoBuySettingsScreen.method1139().field03122.get(local2633_5) == null
                           ? 1L
                           : Math.max(1L, AutoBuySettingsScreen.method1135(AutoBuySettingsScreen.method1139().field03122.get(local2633_5).field03214.getValue()));
                        this.field05425
                           .put(
                              local2633_5,
                              this.field05426.getOrDefault(local2633_5, 0) == 0
                                 ? local2633_6
                                 : Math.max(local2633_8, Math.round(local2633_6 * (1.0 - this.field05389.floatValue() / 100.0)))
                           );
                        this.field05426.put(local2633_5, this.field05426.getOrDefault(local2633_5, 0) + 1);
                     }

                     this.field05401.add(Math.min(this.field05428, this.field05401.size()), local2633_5);
                     this.field05428++;
                     this.field05429 = false;
                     InventoryUtils.method0547(index8, 0, SlotActionType.QUICK_MOVE, false);
                     this.field05393.reset();
                     return;
                  }
               }

               this.field05427.reset();
               this.field05421 = this.field05428;
               this.field05403.clear();
               this.field05393.reset();
               this.field05392 = this.field05428 > 0 ? AutoBuyModule.Nested0408.SELL_PICK : AutoBuyModule.Nested0408.RESUME_PAGE;
}
         }

         if (this.field05392 == AutoBuyModule.Nested0408.IDLE && this.field05385.isChecked()) {
            if (this.field05414) {
               this.field05414 = false;
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               mc.player.networkHandler.sendChatCommand(this.field05408);
               this.field05416 = false;
               this.field05403.clear();
               this.field05395.reset();
            } else if (mc.player.currentScreenHandler == mc.player.playerScreenHandler
               || !AuctionHelperModule.method2310().method2321(this.field05409).contains("подтверждение")
                  && !AuctionHelperModule.method2310().method2321(this.field05409).contains("подозрительная цена")) {
               if (!this.method1743()) {
                  if (!(mc.currentScreen instanceof ChatScreen)
                     && !(mc.currentScreen instanceof DeluxeAutoBuyScreen)
                     && this.field05395.hasExceededMillis(300L)) {
                     mc.player.networkHandler.sendChatCommand(this.field05408);
                     this.field05403.clear();
                     this.field05395.reset();
                  }
               } else {
                  if (this.field05416) {
                     if (!this.field05394.hasExceededMillis(400L)) {
                        return;
                     }

                     this.field05416 = false;
                  }

                  int local2633_3 = -1;
                  long local2633_4 = Long.MAX_VALUE;

                  for (int index9 = 0; index9 < mc.player.currentScreenHandler.slots.size() - 36 - 9; index9++) {
                     ItemStack itemStack9 = mc.player.currentScreenHandler.getSlot(index9).getStack();
                     AutoBuyItem autoBuyItem2 = !itemStack9.isEmpty() && !this.field05403.contains(index9)
                        ? AuctionItemCatalog.method1075(itemStack9)
                        : null;
                     if (autoBuyItem2 != null
                        && AutoBuySettingsScreen.method1139().method1142(autoBuyItem2.name())
                        && !AuctionHelperModule.method2310()
                           .method2321(itemStack9.getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                           .contains("не актуален")
                        && (
                           AutoBuySettingsScreen.method1135(AutoBuySettingsScreen.method1139().field03122.get(autoBuyItem2.name()).field03214.getValue()) <= 0L
                              || AuctionHelperModule.method2310().method2314(itemStack9) / Math.max(1, itemStack9.getCount())
                                 <= AutoBuySettingsScreen.method1135(AutoBuySettingsScreen.method1139().field03122.get(autoBuyItem2.name()).field03214.getValue())
                        )
                        && AuctionHelperModule.method2310().method2314(itemStack9) >= 0
                        && (this.method1749() < 0 || AuctionHelperModule.method2310().method2314(itemStack9) <= this.method1749())
                        && AuctionHelperModule.method2310().method2314(itemStack9) < local2633_4) {
                        local2633_3 = index9;
                        local2633_4 = AuctionHelperModule.method2310().method2314(itemStack9);
                     }
                  }

                  if (local2633_3 != -1 && this.method1746() > this.field05423) {
                     if (System.currentTimeMillis() - this.field05438 >= 90L) {
                        this.field05403.add(local2633_3);
                        InventoryUtils.method0547(local2633_3, 0, SlotActionType.QUICK_MOVE, true);
                        this.field05416 = true;
                        this.field05394.reset();
                        this.field05395.reset();
                     }
                  } else if (this.field05395.hasExceededMillis(400L)) {
                     InventoryUtils.method0547(mc.player.currentScreenHandler.slots.size() - 36 - 5, 0, SlotActionType.QUICK_MOVE, false);
                     this.field05403.clear();
                     this.field05395.reset();
                  }
               }
            } else if (mc.player
               .currentScreenHandler
               .slots
               .stream()
               .anyMatch(
                  slot7 -> slot7.hasStack()
                     && AuctionHelperModule.method2310()
                        .method2321(slot7.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                        .contains("не актуален")
               )) {
               mc.player.closeHandledScreen();
               mc.player.networkHandler.sendChatCommand(this.field05408);
               this.field05416 = false;
               this.field05395.reset();
            } else {
               InventoryUtils.method0547(0, 0, SlotActionType.QUICK_MOVE, false);
               this.field05395.reset();
            }
         }
      }
   };
   private final EventListener<ClientTickEvent> field05440 = clientTickEvent -> {
      if (clientTickEvent.isPre() && (mc.player == null || mc.world == null)) {
         this.field05393.reset();
         if (this.field05406 != null
            && (
               mc.currentScreen instanceof DisconnectedScreen
                  || mc.currentScreen instanceof TitleScreen
                  || mc.currentScreen instanceof MultiplayerScreen
            )
            && this.field05399.hasExceededMillis(5000L)) {
            this.field05399.reset();
            this.field05392 = AutoBuyModule.Nested0408.REJOIN_ANARCHY;
            ConnectScreen.connect(
               new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(this.field05406.address), this.field05406, false, null
            );
         }
      }
   };
   private final EventListener<PacketEvent> field05441 = packetEvent -> {
      if (packetEvent.isOutgoing()
         && this.field05392 == AutoBuyModule.Nested0408.IDLE
         && packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandExecutionC2SPacket
         && (commandExecutionC2SPacket.command().equals("ah") || commandExecutionC2SPacket.command().startsWith("ah "))
         && !commandExecutionC2SPacket.command().startsWith("ah sell")) {
         this.field05408 = commandExecutionC2SPacket.command();
      }

      if (packetEvent.isIncoming()
         && (packetEvent.getPacket() instanceof InventoryS2CPacket || packetEvent.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket)) {
         this.field05438 = System.currentTimeMillis();
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof OpenScreenS2CPacket openScreenS2CPacket) {
         this.field05409 = openScreenS2CPacket.getName().getString();
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof CloseScreenS2CPacket) {
         this.field05409 = "";
      }

      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         if (field05379.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))).matches()) {
            this.field05416 = false;
            if (this.field05392 == AutoBuyModule.Nested0408.FLIP_BUY) {
               this.field05423 = Math.max(0, this.field05423 - 1);
            }

            for (AutoBuyItem autoBuyItem3 : AuctionItemCatalog.method1077()) {
               if (AuctionHelperModule.method2310()
                  .method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                  .contains(AuctionHelperModule.method2310().method2321(autoBuyItem3.name()))) {
                  this.field05401.add(autoBuyItem3.name());
                  this.field05436.remove(autoBuyItem3.name());
                  break;
               }
            }

            this.field05415++;
         } else if (field05381.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
            .matches()) {
            this.field05423 = Math.max(0, this.field05423 - 1);
            this.field05429 = true;
         } else if (field05377.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
            .matches()) {
            this.field05422++;
            this.field05423 = Math.max(0, this.field05423 - 1);
            this.field05435 = 0;
            this.field05431.reset();
            this.field05427.reset();
            String local2641_3 = gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim();
            if (field05378.matcher(local2641_3).matches()) {
               this.field05425.remove(field05378.matcher(local2641_3).replaceAll("$1").replaceAll("\\s+[xх]\\d+$", "").trim());
               this.field05426.remove(field05378.matcher(local2641_3).replaceAll("$1").replaceAll("\\s+[xх]\\d+$", "").trim());
               mc.execute(
                  () -> AutoBuySettingsScreen.method1139()
                     .method1141(
                        field05378.matcher(local2641_3).replaceAll("$1"),
                        AutoBuySettingsScreen.method1135(field05378.matcher(local2641_3).replaceAll("$2").replace(",", ""))
                     )
               );
            }
         } else if (field05380.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
            .matches()) {
            if (this.field05392 == AutoBuyModule.Nested0408.SELL_WAIT) {
               this.field05418 = true;
            }
         } else if (!AuctionHelperModule.method2310()
            .method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
            .contains("не так быстро")) {
            if (AuctionHelperModule.method2310()
               .method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
               .contains("этот товар уже купили")) {
               this.field05414 = true;
            } else if (field05376.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "")))
               .matches()) {
               this.field05417 = true;
               this.field05435 = 0;
               if (this.field05392 == AutoBuyModule.Nested0408.SELL_WAIT) {
                  this.field05423 = Math.min(this.field05424, this.field05423 + 1);
               }

               if (!this.field05401.remove(this.field05411) && !this.field05401.isEmpty()) {
                  this.field05401.remove(0);
               }

               if (AuctionItemCatalog.method1055(this.field05411) != null
                  && InventoryUtils.method0555(itemStack10 -> AuctionItemCatalog.method1059(itemStack10, AuctionItemCatalog.method1055(this.field05411)))
                     == -1
                  && InventoryUtils.method0555(itemStack11 -> AuctionItemCatalog.method1072(itemStack11.getItem())) == -1) {
                  this.field05401.removeIf(local2607_1 -> local2607_1.equals(this.field05411));
               }
            } else if (!gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim().equalsIgnoreCase("Данная команда недоступна в режиме AFK")
               && !gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").trim().equalsIgnoreCase("Недопустимо нажимать в режиме AFK")) {
               if (AuctionHelperModule.method2310()
                  .method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                  .contains("освободите хранилище или арендуйте больше слотов")) {
                  this.field05423 = this.field05424;
               }

               if (AuctionHelperModule.method2310()
                     .method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                     .contains("освободите хранилище или арендуйте больше слотов")
                  && InventoryUtils.method0546(0, 35) != -1) {
                  if (this.field05392 == AutoBuyModule.Nested0408.IDLE
                     || this.field05392 == AutoBuyModule.Nested0408.SELL_PICK
                     || this.field05392 == AutoBuyModule.Nested0408.SELL_SEND
                     || this.field05392 == AutoBuyModule.Nested0408.SELL_WAIT) {
                     this.field05437 = 3;
                     this.field05403.clear();
                     this.field05393.reset();
                     this.field05392 = AutoBuyModule.Nested0408.FLIP_SEARCH;
                  }
               }
            } else {
               this.field05392 = AutoBuyModule.Nested0408.REJOIN_HUB;
               this.field05393.reset();
            }
         }
      }
   };

   private boolean method1724() {
      return mc.player.currentScreenHandler != mc.player.playerScreenHandler && !AuctionHelperModule.method2310().method2331(this.field05409);
   }

   @Override
   protected void onDisable() {
      this.field05385.setValue(false);
      this.field05386.setValue(false);
      this.field05400.clear();
      this.field05402.clear();
      this.field05403.clear();
      this.field05413 = false;
      this.field05415 = 0;
      this.field05421 = 0;
      this.field05422 = 0;
      this.field05423 = 0;
      this.field05416 = false;
      this.field05437 = 0;
      this.field05418 = false;
      this.field05419 = false;
      this.field05420.clear();
      this.field05425.clear();
      this.field05426.clear();
      this.field05428 = 0;
      this.field05429 = false;
      this.field05430.clear();
      this.field05435 = 0;
      this.field05434 = 0;
      this.field05436.clear();
      this.field05424 = field05383[0];
      this.field05392 = AutoBuyModule.Nested0408.IDLE;
   }

   private boolean method1725(String local2612_1) {
      return this.field05402.contains(local2612_1)
         || AuctionItemCatalog.method1055(local2612_1) != null && AuctionItemCatalog.method1076(AuctionItemCatalog.method1055(local2612_1));
   }

   private long method1726(String local2613_1) {
      if (this.field05425.getOrDefault(local2613_1, 0L) > 0L) {
         return this.field05425.get(local2613_1);
      } else {
         return AutoBuySettingsScreen.method1139().field03122.get(local2613_1) != null
            ? AutoBuySettingsScreen.method1135(AutoBuySettingsScreen.method1139().field03122.get(local2613_1).field03215.getValue())
            : this.field05404.getOrDefault(local2613_1, 0L);
      }
   }

   private long method1728(long local2615_1, long local2615_3, long local2615_5) {
      return local2615_5;
   }

   public boolean method1732() {
      return this.field05385.isChecked();
   }

   public static AutoBuyModule method1734() {
      if (autoBuyModule == null) {
         autoBuyModule = new AutoBuyModule();
      }

      return autoBuyModule;
   }

   private int method1736(String local2624_1, int local2624_2) {
      return AuctionItemCatalog.method1055(local2624_1) != null && AuctionItemCatalog.method1076(AuctionItemCatalog.method1055(local2624_1)) ? 1 : local2624_2;
   }

   private boolean method1737() {
      Slot slot = null;
      Slot slot2 = null;

      for (Slot slot3 : mc.player.currentScreenHandler.slots) {
         if (slot3.inventory == mc.player.getInventory() && slot3.getIndex() <= 35) {
            if (slot3.getIndex() == InventoryUtils.method0554()) {
               slot = slot3;
            } else if (slot2 == null && !slot3.hasStack()) {
               slot2 = slot3;
            }
         }
      }

      if (slot != null && slot2 != null) {
         InventoryUtils.method0547(slot.id, 1, SlotActionType.PICKUP, false);
         InventoryUtils.method0547(slot2.id, 0, SlotActionType.PICKUP, false);
         return true;
      } else {
         return false;
      }
   }

   private int method1740() {
      int local2628_1 = 0;

      for (int index = 0; index <= 35; index++) {
         if (!mc.player.getInventory().getStack(index).isEmpty()) {
            local2628_1++;
         }
      }

      return local2628_1;
   }

   public void method1742() {
      this.field05386.setValue(!this.field05386.isChecked());
      this.field05400.clear();
      this.field05392 = AutoBuyModule.Nested0408.IDLE;
      if (!this.field05386.isChecked()) {
         ClientChat.method2267("Автопарсер остановлен.");
      } else if (AutoBuySettingsScreen.method1139().field03122.isEmpty()) {
         ClientChat.method2267("В меню AutoBuy не выбрано ни одного предмета.");
      } else {
         this.field05400.addAll(AutoBuySettingsScreen.method1139().field03122.keySet());
         this.field05393.reset();
         this.field05392 = AutoBuyModule.Nested0408.SEND;
      }
   }

   private boolean method1743() {
      return mc.player.currentScreenHandler != mc.player.playerScreenHandler && AuctionHelperModule.method2310().method2331(this.field05409);
   }

   public void method1744() {
      this.field05385.setValue(!this.field05385.isChecked());
      this.field05403.clear();
      ClientChat.method2267(
         this.field05385.isChecked()
            ? "Автобай запущен, предметов в списке: &7" + AutoBuySettingsScreen.method1139().field03122.size() + "&r, лотов по донату: &7" + this.field05424
            : "Автобай остановлен."
      );
   }

   private int method1746() {
      return 36 - this.method1740();
   }

   private int method1749() {
      return mc.world != null && mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR) != null
         ? mc.world
            .getScoreboard()
            .getTeams()
            .stream()
            .filter(team -> team.getName().startsWith("TAB-Sidebar-"))
            .map(team2 -> team2.getPrefix().getString().toLowerCase(Locale.ROOT))
            .filter(local2623_0 -> local2623_0.contains("монеты") || local2623_0.contains("$"))
            .map(local2640_0 -> local2640_0.replaceAll("\\D", ""))
            .filter(local2609_0 -> !local2609_0.isEmpty() && local2609_0.length() <= 9)
            .map(Integer::parseInt)
            .findFirst()
            .orElse(-1)
         : -1;
   }

   public boolean method1750() {
      return this.field05386.isChecked();
   }

   private AutoBuyModule() {
      this.addSetting(new SectionSetting("Скупка"));
      this.keybindSetting = this.addSetting(new KeybindSetting("Открыть меню", "Клавиша, которая открывает окно AutoBuy."));
      this.field05385 = this.addSetting(new BooleanSetting("Автобай", "Скупать выбранные предметы на открытой странице аукциона.", false));
      this.field05386 = this.addSetting(new BooleanSetting("Автопарсер", "Проходить выбранные предметы и подставлять им цены.", false));
      this.field05389 = this.addSetting(
         new NumberSetting(
               "Скидка перевыставления", "На сколько процентов дешеветь помеченному лоту за каждый круг перевыставления, ниже цены покупки не опускается."
            )
            .withIntegerRange(1, 50)
            .withSuffix("%")
            .setValue(5.0F)
      );
      this.field05390 = this.addSetting(
         new NumberSetting("Перепарс", "Через сколько минут автопарсер заново проходит все предметы, 0 значит один проход.")
            .withIntegerRange(0, 60)
            .withSuffix(" мин")
            .setValue(10.0F)
      );
      this.field05387 = new NumberSetting("Процент покупки", "Какой процент от самого дешёвого лота автопарсер ставит в лимит покупки.")
         .withIntegerRange(10, 200)
         .withSuffix("%")
         .setValue(95.0F);
      this.field05388 = new NumberSetting("Процент продажи", "Какой процент от самого дешёвого лота автопарсер ставит в цену продажи.")
         .withIntegerRange(10, 400)
         .withSuffix("%")
         .setValue(115.0F);
   }

   public static final class Nested0407 extends SliderWidget {
      private final String field05582;
      private final NumberSetting numberSetting;

      @Override
      protected void updateMessage() {
         this.setMessage(Text.literal(this.field05582 + ": " + this.numberSetting.intValue() + "%"));
      }

      @Override
      protected void applyValue() {
         this.numberSetting.setFraction((float)this.value);
      }

      public Nested0407(int local2646_1, int local2646_2, int local2646_3, String local2646_4, NumberSetting initialNumberSetting) {
         super(local2646_1, local2646_2, local2646_3, 20, Text.literal(local2646_4 + ": " + initialNumberSetting.intValue() + "%"), initialNumberSetting.getFraction());
         this.field05582 = local2646_4;
         this.numberSetting = initialNumberSetting;
      }
   }

   private enum Nested0408 {
      IDLE,
      SEND,
      WAIT_LOTS,
      SELL_PICK,
      SELL_SEND,
      SELL_WAIT,
      FLIP_SEARCH,
      FLIP_BUY,
      RESELL,
      REJOIN_HUB,
      REJOIN_ANARCHY,
      RESUME_PAGE,
      STASH,
      RELIST_SEARCH,
      RELIST_PULL;
   }
}
