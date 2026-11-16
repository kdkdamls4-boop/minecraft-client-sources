package net.skycore.client.module.misc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.hud.NotificationsHud;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.render.InterfaceModule;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class ReallyWorldHelperModule extends ClientModule {
   private final BooleanSetting filter;
   public static final ReallyWorldHelperModule INSTANCE = new ReallyWorldHelperModule();
   private boolean swapRequested;
   private int returnDelayTicks;
   private final KeyBindSetting bindTrapKey;
   private final KeyBindSetting antiPoletKey;
   private long actionGeneration;
   private int sneakTicks;
   private final BooleanSetting close = this.add(new BooleanSetting("Close menu", false));
   private int queuedReturnSlot;
   private final BooleanSetting grimBypass;
   private final Set<String> bannedWords;
   private final BooleanSetting bindTrap;
   private boolean trapRequested;
   private boolean returnSwapRequested;

   private void onKey(KeyEvent keyEvent) {
      if (this.isEnabled() && keyEvent.pressed()) {
         if (keyEvent.client().gui.screen() == null) {
            net.minecraft.client.input.KeyEvent keyEvent2 = keyEvent.key();
            this.handleBind(keyEvent2.key());
            return;
         }
      }
   }

   private void executeAntiPolet() {
      if (UseItemHelper.getSlot(Items.FIREWORK_STAR) == -1) {
         SendDonationHelper.displayInfo("No anti-flight item (firework star).");
      } else {
         ItemStack itemStack = this.itemIcon(Items.FIREWORK_STAR);
         int slot = UseItemHelper.antipoletToOffhand(Items.FIREWORK_STAR);
         if (slot != -1) {
            this.notifyUsed("Использован Антиполёт", itemStack);
            this.queuedReturnSlot = slot;
            this.returnDelayTicks = 6;
            this.sneakTicks = 4;
            Minecraft instance = Minecraft.getInstance();
            if (instance.options != null) {
               instance.options.keyShift.setDown(true);
            }
         }
      }
   }

   private void onInput(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled() && playerInputEvent != null) {
         if (this.sneakTicks > 0) {
            playerInputEvent.setShift(true);
            Minecraft instance = Minecraft.getInstance();
            if (instance.options != null) {
               instance.options.keyShift.setDown(true);
            }
         }
      }
   }

   private void requestBindTrap() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (instance.level != null) {
            if (!this.trapRequested) {
               ItemCooldowns cooldowns = player.getCooldowns();
               if (cooldowns.isOnCooldown(Items.HEART_OF_THE_SEA.getDefaultInstance())) {
                  return;
               }

               if (UseItemHelper.getItemSlot(Items.HEART_OF_THE_SEA) == -1) {
                  return;
               }

               this.trapRequested = true;
               long actionGeneration2 = this.actionGeneration;
               Runnable runnable = () -> {
                  if (this.isEnabled()) {
                     if (actionGeneration2 == this.actionGeneration) {
                        this.handlePendingTrap();
                     }
                  }
               };
               ScheduleHelper.schedule(runnable);
               return;
            }
         }
      }
   }

   @Override
   public boolean affectsGameplay() {
      return true;
   }

   private void handlePendingTrap() {
      if (this.trapRequested) {
         ItemStack itemStack = this.itemIcon(Items.HEART_OF_THE_SEA);
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null && !itemStack.isEmpty() && !player.getCooldowns().isOnCooldown(itemStack)) {
            UseItemHelper.inventorySwapClick(Items.HEART_OF_THE_SEA);
            this.notifyUsed("Использована трапка", itemStack);
         }

         this.trapRequested = false;
      }
   }

   public void requestAntiPolet() {
      if (!this.swapRequested && !this.returnSwapRequested && this.queuedReturnSlot == -1) {
         this.swapRequested = true;
         long actionGeneration2 = this.actionGeneration;
         Runnable instance = () -> {
            if (this.isEnabled()) {
               if (actionGeneration2 == this.actionGeneration && this.swapRequested) {
                  this.swapRequested = false;
                  Minecraft instance2 = Minecraft.getInstance();
                  if (instance2.player != null) {
                     this.executeAntiPolet();
                  }

                  return;
               }
            }
         };
         ScheduleHelper.schedule(instance);
      }
   }

   public void cancelPendingAction() {
      this.actionGeneration++;
      this.trapRequested = false;
      this.swapRequested = false;
      this.returnSwapRequested = false;
      this.queuedReturnSlot = -1;
      this.sneakTicks = 0;
      this.returnDelayTicks = 0;
      Minecraft instance = Minecraft.getInstance();
      if (instance.options != null) {
         Minecraft instance2 = Minecraft.getInstance();
         instance2.options.keyShift.setDown(false);
      }
   }

   public boolean shouldCloseMenu(ClientboundOpenScreenPacket clientboundOpenScreenPacket) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (this.closeMenuEnabled() && player != null) {
         if (player.tickCount < 100) {
            Component title = clientboundOpenScreenPacket.getTitle();
            String string = title.getString();
            if (string.contains("ꈁꀀꈂꌁꈂꀁ§0ꈃꄀ")) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean shouldBlockChat(String text) {
      if (this.isEnabled()) {
         if (this.filter.get() && text != null) {
            String text2 = text.toLowerCase(Locale.ROOT);

            for (String text3 : this.bannedWords) {
               if (text2.matches(".*" + text3 + ".*")) {
                  SendDonationHelper.displayWarning("В вашем сообщении найдено запрещённое слово, отправка отменена!");
                  return true;
               }
            }

            return false;
         }
      }

      return false;
   }

   @Override
   protected void onDisable() {
      this.cancelPendingAction();
   }

   private ItemStack itemIcon(Item item) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         return ItemStack.EMPTY;
      }

      if (player.getMainHandItem().is(item)) {
         return player.getMainHandItem().copy();
      }

      ItemStack offhandItem = player.getOffhandItem();
      if (offhandItem.is(item)) {
         return player.getOffhandItem().copy();
      }

      int slot = UseItemHelper.getSlot(item);
      ItemStack itemStack;
      if (slot < 0) {
         itemStack = ItemStack.EMPTY;
      } else {
         itemStack = player.getInventory().getItem(slot).copy();
      }

      return itemStack;
   }

   private void handleBind(int value) {
      if (value == this.antiPoletKey.get()) {
         if (this.antiPoletKey.get() != -1) {
            this.requestAntiPolet();
         }
      }

      Boolean get = this.bindTrap.get();
      if (get) {
         if (value == this.bindTrapKey.get()) {
            this.requestBindTrap();
         }
      }
   }

   private void notifyUsed(String text, ItemStack itemStack) {
      var optional = SkycoreClient.modules().get(InterfaceModule.class);
      optional.ifPresent(interfaceModule -> {
         NotificationsHud notificationsHud = interfaceModule.notifications();
         notificationsHud.onItemUsed(text, itemStack);
      });
   }

   public boolean closeMenuEnabled() {
      return this.isEnabled() && this.close.get();
   }

   private ReallyWorldHelperModule() {
      super("ReallyWorld Helper", "ReallyWorld QoL helpers", ModuleCategory.MISC);
      this.filter = this.add(new BooleanSetting("Chat filter", false));
      this.antiPoletKey = this.add(new KeyBindSetting("Anti flight key", -1));
      this.bindTrap = this.add(new BooleanSetting("Trap bind", false));
      this.bindTrapKey = this.add(new KeyBindSetting("Trap key", 1002));
      this.grimBypass = this.add(new BooleanSetting("Grim bypass", true));
      this.queuedReturnSlot = -1;
      this.bannedWords = new HashSet<>(
         Arrays.asList(
            "акриен(а|у|ом|е|чик)?",
            "рич(а|у|ом|ей|е)?",
            "ньюкод(ом|а|у|ами|ик|е)?",
            "экспенсив(ом|а|у|ами|е)?",
            "импакт(ом|а|у|ами|ик|е)?",
            "экселлент(ом|а|у|ами|ик|е)?",
            "экселент(ом|а|у|ами|ик)?",
            "катлаван(ом|а|у|ами|чик)?",
            "катлован(ом|а|у|ами|чик)?",
            "целестиал(ом|а|у|ами|е)?",
            "целк(ой|а|у|ами|очка|е)?",
            "матикс(ом|а|у|ами|е)?",
            "инерти(я|ей|ю|ями|е)?",
            "эксп(а|ой|ою|у|уличка|е)?",
            "флюгер(ом|а|у|ами)?",
            "рикер(а|у|ом|очек)?",
            "фанпе(й|ю|я|ем|е|йчик)?",
            "вексайд(ом|а|у|ами|ик|е)?",
            "нурсултан(а|у|е|ом|чик)?",
            "нурик(а|у|ом|е)?",
            "нурлан(а|у|ом|чик|е)?",
            "векс(ом|у|а|ами|ик|е)?",
            "релейк(ом|у|а|ами|е)?",
            "арбуз(ом|а|у|ами|ик|е)?",
            "вилд(ом|у|а|ами|ик|е)?",
            "фантайм(е|а|у)?",
            "холик(е|а|у)?",
            "холиворлд(а|у|е)?",
            "рокстар(ом|а|у|ами|чик|е)?",
            "рогалик(а|у|ом|е)?",
            "тандерхак(ом|у|и|ами|а|е)?",
            "ликвидбаунс(а|у|ами|е)?",
            "expensive",
            "celestial",
            "newcode",
            "arbuz",
            "akrien",
            "nursultan",
            "relake",
            "wild",
            "wurst",
            "catlovan",
            "excellent",
            "rockstar",
            "catlavan",
            "impact",
            "matix",
            "inertia",
            "wex",
            "wexside",
            "nurik",
            "nurlan",
            "rich",
            "funpay",
            "fluger",
            "riker",
            "funtime",
            "holyworld",
            "wwe",
            "hvh",
            "rogalik",
            "thunderhack",
            "liquidbounce"
         )
      );
      this.bindTrapKey.visibleWhen(this.bindTrap::get);
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onInput);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, playerTickEndEvent -> {
         if (this.isEnabled()) {
            this.newTick();
         }
      });
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      int value = 1;
      value = (byte)1;
      if (this.isEnabled()) {
         if (mouseButtonEvent.pressed()) {
            if (mouseButtonEvent.client().gui.screen() == null) {
               MouseButtonInfo mouseButtonInfo = mouseButtonEvent.button();
               value = 1000 + mouseButtonInfo.button();
               this.handleBind(value);
               return;
            }
         }
      }
   }

   public void newTick() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && instance.connection != null) {
         if (this.returnDelayTicks > 0) {
            this.returnDelayTicks--;
            if (this.returnDelayTicks == 0 && this.queuedReturnSlot != -1) {
               this.returnSwapRequested = true;
               int queuedReturnSlot2 = this.queuedReturnSlot;
               long actionGeneration2 = this.actionGeneration;
               ScheduleHelper.schedule(() -> {
                  if (this.isEnabled()) {
                     if (actionGeneration2 == this.actionGeneration && this.returnSwapRequested) {
                        Minecraft instance2 = Minecraft.getInstance();
                        if (instance2.player != null) {
                           UseItemHelper.moveItem(45, queuedReturnSlot2);
                        }

                        this.returnSwapRequested = false;
                        this.queuedReturnSlot = -1;
                        return;
                     }
                  }
               });
            }
         }

         if (this.sneakTicks > 0) {
            this.sneakTicks--;
            if (this.sneakTicks == 0 && Minecraft.getInstance().options != null) {
               Minecraft.getInstance().options.keyShift.setDown(false);
            }
         }
      }
   }
}
