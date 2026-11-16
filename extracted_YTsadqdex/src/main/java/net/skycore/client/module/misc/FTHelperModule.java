package net.skycore.client.module.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.FreeLookController;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class FTHelperModule extends ClientModule {
   private int aimDownTicks;
   private final BooleanSetting windChargeAutoJump;
   private final ModeSetting windChargeMode;
   private FTHelperModule.ActionBind pending;
   private final KeyBindSetting openShulker;
   private final List<FTHelperModule.ActionBind> actions;
   public static final FTHelperModule INSTANCE = new FTHelperModule();
   private final ModeSetting serverMode = this.add(new ModeSetting("Server Mode", "FT", "FT", "Others"));
   private final ModeSetting buffThrowMode;

   private void queue(FTHelperModule.ActionBind pending2) {
      if (this.pending == null) {
         this.pending = pending2;
         this.aimDownTicks = 0;
      }
   }

   private void clearPending() {
      this.pending = null;
      this.aimDownTicks = 0;
   }

   private void useWindCharge(LocalPlayer player) {
      int slot = UseItemHelper.getSlot(Items.WIND_CHARGE);
      if (slot < 0) {
         if (!player.getOffhandItem().isEmpty() && player.getOffhandItem().is(Items.WIND_CHARGE)) {
            if (this.windChargeMode.is("Under self")) {
               this.useHandWithPitch(player, InteractionHand.OFF_HAND, 90.0F);
            } else {
               UseItemHelper.useItem(InteractionHand.OFF_HAND);
            }
         } else {
            SendDonationHelper.displayWarning("Wind Charge not found");
         }
      } else {
         if (this.windChargeMode.is("Under self") && this.windChargeAutoJump.get() && Minecraft.getInstance().options != null) {
            Minecraft.getInstance().options.keyJump.setDown(true);
         }

         if (this.windChargeMode.is("Under self")) {
            this.useSlotWithPitch(player, slot, 90.0F);
         } else {
            UseItemHelper.inventorySwapClickSlot(slot);
         }
      }
   }

   private void useSlotWithPitch(LocalPlayer player, int slot, float value) {
      if (player.connection != null) {
         int selectedSlot = player.getInventory().getSelectedSlot();
         if (slot < 9) {
            UseItemHelper.switchToHotbarSlot(slot);
            player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, player.getYRot(), value));
            UseItemHelper.switchToHotbarSlot(selectedSlot);
         } else {
            int slot2 = UseItemHelper.toMenuSlot(slot);
            UseItemHelper.clickSlotId(slot2, selectedSlot, ContainerInput.SWAP, false);
            UseItemHelper.syncCurrentPlayItem();
            player.connection.send(new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, 0, player.getYRot(), value));
            UseItemHelper.clickSlotId(slot2, selectedSlot, ContainerInput.SWAP, false);
            UseItemHelper.syncCurrentPlayItem();
         }
      }
   }

   private FTHelperModule() {
      super("FT Helper", "FunTime item binds (no HUD)", ModuleCategory.MISC);
      this.windChargeMode = this.add(new ModeSetting("Wind Charge", "Under self", "Under self", "Crosshair"));
      this.windChargeAutoJump = this.add(new BooleanSetting("Auto Jump", false));
      this.buffThrowMode = this.add(new ModeSetting("Buff throw", "Crosshair", "Crosshair", "Under self"));
      this.openShulker = this.add(new KeyBindSetting("Open Shulker", -1));
      this.actions = new ArrayList<>();
      this.windChargeAutoJump.visibleWhen(() -> {
         return this.windChargeMode.is("Under self");
      });
      this.register("GodsAura", "Gods Aura", false, false, false, new String[]{"божья аура"}, new String[]{"божья аура", "god's aura", "gods aura"});
      this.register("Trap", "Trap", false, false, false, new String[]{"трапка"}, new String[]{"трапка", "trap"});
      this.register("Plast", "Plast", false, false, false, new String[]{"пласт"}, new String[]{"пласт", "plast"});
      this.register("Disorientation", "Disorientation", false, false, false, new String[]{"дезориентация"}, new String[]{"дезориентация", "disorientation"});
      this.register("VisibleDust", "Visible Dust", false, false, false, new String[]{"явная пыль"}, new String[]{"явная пыль", "visible dust"});
      this.register("FreezeBall", "Freeze Ball", false, false, false, new String[]{"снежок заморозки"}, new String[]{"снежок заморозки", "freeze ball"});
      this.register("FieryTornado", "Fiery Tornado", false, false, false, new String[]{"огненный смерч"}, new String[]{"огненный смерч", "fiery tornado"});
      this.register("WindCharge", "Wind Charge", false, false, true, new String[]{"wind charge", "заряд ветра"}, new String[]{"wind charge", "заряд ветра"});
      this.register("PotionAssassin", "Assassin Potion", true, false, false, new String[]{"зелье ассасина"}, new String[]{"зелье ассасина"});
      this.register("PotionHolyWater", "Holy Water", true, false, false, new String[]{"святая вода"}, new String[]{"святая вода"});
      this.register("PotionRage", "Rage Potion", true, false, false, new String[]{"зелье гнева"}, new String[]{"зелье гнева"});
      this.register("PotionPaladin", "Paladin Potion", true, false, false, new String[]{"зелье палладина"}, new String[]{"зелье палладина"});
      this.register("PotionPopper", "Popper", true, true, false, new String[]{"хлопушка"}, new String[]{"хлопушка"});
      this.register("PotionRadiation", "Radiation Potion", true, true, false, new String[]{"зелье радиации"}, new String[]{"зелье радиации"});
      this.register("PotionDrowsiness", "Drowsiness Potion", true, true, false, new String[]{"зелье снотворного"}, new String[]{"зелье снотворного"});
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
   }

   @Override
   protected void onDisable() {
      this.clearPending();
   }

   private void useHandWithPitch(LocalPlayer player, InteractionHand interactionHand, float value) {
      if (player.connection != null) {
         ClientPacketListener connection = player.connection;
         ServerboundUseItemPacket serverboundUseItemPacket = new ServerboundUseItemPacket(interactionHand, 0, player.getYRot(), value);
         connection.send(serverboundUseItemPacket);
      }
   }

   private void openShulkerNow() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && Minecraft.getInstance().gameMode != null) {
         for (int value = 0; value < 36; value++) {
            ItemStack item = instance.getInventory().getItem(value);
            if (!item.isEmpty() && item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
               UseItemHelper.clickSlotId(UseItemHelper.toMenuSlot(value), 1, ContainerInput.PICKUP, false);
               return;
            }
         }

         SendDonationHelper.displayWarning("No shulker in inventory");
      }
   }

   @Override
   protected void onTick() {
      if (this.isEnabled() && this.pending != null) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance == null || instance.connection == null) {
            this.clearPending();
         } else if (this.shouldAimDown(this.pending) && this.aimDownTicks < 3) {
            this.aimPitchDown(instance);
            this.aimDownTicks++;
         } else {
            FTHelperModule.ActionBind pending2 = this.pending;
            this.clearPending();
            if (pending2.matchByItem) {
               this.useWindCharge(instance);
            } else {
               this.useByName(pending2);
            }
         }
      }
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      int value = 1;
      value = (byte)1;
      if (this.isEnabled() && mouseButtonEvent.pressed()) {
         Minecraft minecraft = mouseButtonEvent.client();
         if (minecraft.gui.screen() == null) {
            MouseButtonInfo mouseButtonInfo = mouseButtonEvent.button();
            value = 1000 + mouseButtonInfo.button();
            this.handleBind(value);
            return;
         }
      }
   }

   @Override
   public boolean affectsGameplay() {
      return true;
   }

   private void onKey(KeyEvent keyEvent) {
      if (this.isEnabled() && keyEvent.pressed()) {
         Minecraft minecraft = keyEvent.client();
         if (minecraft.gui.screen() == null) {
            net.minecraft.client.input.KeyEvent keyEvent2 = keyEvent.key();
            this.handleBind(keyEvent2.key());
            return;
         }
      }
   }

   private void handleBind(int index) {
      if (index != -1) {
         if (index == this.openShulker.get()) {
            this.openShulkerNow();
         } else {
            for (FTHelperModule.ActionBind actionBind : this.actions) {
               if (actionBind.bind.get() != -1 && actionBind.bind.get() == index) {
                  this.queue(actionBind);
                  return;
               }
            }
         }
      }
   }

   private void useByName(FTHelperModule.ActionBind actionBind) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         String[] stringValues = this.serverMode.is("FT") ? actionBind.ftTags : actionBind.otherTags;
         if (!instance.getOffhandItem().isEmpty() && this.matchesStack(instance.getOffhandItem(), stringValues, actionBind)) {
            if (this.shouldAimDown(actionBind)) {
               this.useHandWithPitch(instance, InteractionHand.OFF_HAND, 90.0F);
            } else {
               UseItemHelper.useItem(InteractionHand.OFF_HAND);
            }
         } else {
            int pitch = -1;

            for (int value = 0; value < 36; value++) {
               if (this.matchesStack(instance.getInventory().getItem(value), stringValues, actionBind)) {
                  pitch = value;
                  break;
               }
            }

            if (pitch < 0) {
               SendDonationHelper.displayWarning(actionBind.label + " not found");
            } else {
               if (this.shouldAimDown(actionBind)) {
                  this.useSlotWithPitch(instance, pitch, 90.0F);
               } else {
                  UseItemHelper.inventorySwapClickSlot(pitch);
               }
            }
         }
      }
   }

   private void register(String text, String text2, boolean condition, boolean condition2, boolean condition3, String[] stringValues, String[] stringValues2) {
      KeyBindSetting keyBindSetting = this.add(new KeyBindSetting(text2, -1));
      this.actions.add(new FTHelperModule.ActionBind(text, text2, condition, condition2, condition3, stringValues, stringValues2, keyBindSetting));
   }

   private void aimPitchDown(LocalPlayer player) {
      float yaw;
      if (FreeLookController.isActive()) {
         yaw = FreeLookController.getFreeYaw();
      } else {
         yaw = player.getYRot();
      }

      float yaw2 = yaw;
      if (!FreeLookController.isActive()) {
         FreeLookController.setFreeYaw(yaw2);
         FreeLookController.setFreePitch(player.getXRot());
         FreeLookController.setActive(true);
      }

      float yRot = player.getYRot();
      RotationManager.updateExact(new Rotation(yRot, 90.0F), 12, 2);
   }

   private boolean shouldAimDown(FTHelperModule.ActionBind actionBind) {
      if (actionBind.matchByItem) {
         return this.windChargeMode.is("Under self");
      }

      if (actionBind.splash) {
         if (!actionBind.debuff) {
            return this.buffThrowMode.is("Under self");
         }
      }

      return false;
   }

   private boolean matchesStack(ItemStack itemStack, String[] stringValues, FTHelperModule.ActionBind actionBind) {
      if (itemStack != null && !itemStack.isEmpty()) {
         if (actionBind.matchByItem) {
            return itemStack.is(Items.WIND_CHARGE);
         }

         String hoverName = itemStack.getHoverName().getString().toLowerCase(Locale.ROOT);

         for (String text : stringValues) {
            if (text != null && !text.isEmpty() && hoverName.contains(text.toLowerCase(Locale.ROOT))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public record ActionBind(String id, String label, boolean splash, boolean debuff, boolean matchByItem, String[] ftTags, String[] otherTags, KeyBindSetting bind) {
      public String id() {
         return this.id;
      }

      public String[] ftTags() {
         return this.ftTags;
      }

      public String[] otherTags() {
         return this.otherTags;
      }

      public boolean splash() {
         return this.splash;
      }

      public KeyBindSetting bind() {
         return this.bind;
      }

      public boolean matchByItem() {
         return this.matchByItem;
      }

      public boolean debuff() {
         return this.debuff;
      }

      public String label() {
         return this.label;
      }
   }
}
