package net.skycore.client.module.misc;

import com.mojang.blaze3d.platform.InputConstants.Key;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.KeyMappingGetKeyAccessor;
import net.skycore.client.mixin.LivingEntityGetNoJumpDelayAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.util.KeyState;
import net.skycore.client.util.ServerAddressMatcher;
import net.skycore.client.util.NeedsPacketOrderSafeHelper;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.util.StartFallFlyingHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class ElytraHelperModule extends ClientModule {
   private boolean autoTakeoffArmed;
   private boolean wasOnGround;
   private boolean fireworkRequested;
   private int swapCooldownTicks;
   private final BooleanSetting equipChestplate;
   private boolean autoTakeoffJumpActive;
   private boolean wasInElytraFlight;
   private final BooleanSetting notifications;
   public static final ElytraHelperModule INSTANCE = new ElytraHelperModule();
   private final BooleanSetting fireworkOnTakeoff;
   private boolean autoTakeoffFireworkPending;
   private final BooleanSetting autoFly;
   private final KeyBindSetting fireworkKey;
   private final BooleanSetting fireworkOffhand;
   private int autoTakeoffCycleTick;
   private boolean autoTakeoff1171GlideSent;
   private Boolean forcedSwapToChestplate;
   private boolean swapRequested;
   private int movementStopTicks;
   private final KeyBindSetting keySwap = this.add(new KeyBindSetting("Swap Key", -1));

   private void handlePendingSwap() {
      if (this.swapRequested) {
         if (this.swapCooldownTicks <= 0) {
            if (this.movementStopTicks <= 0) {
               this.swapRequested = false;
               Boolean forcedSwapToChestplate2 = this.forcedSwapToChestplate;
               this.forcedSwapToChestplate = null;
               this.executeSwap(forcedSwapToChestplate2);
               this.swapCooldownTicks = 2;
            }
         }
      }
   }

   private void executeSwap(Boolean condition) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         boolean is = condition != null ? condition : instance.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
         int slot = is ? UseItemHelper.getChestPlateSlot() : UseItemHelper.getSlot(Items.ELYTRA);
         if (slot != -1) {
            ItemStack inventory = instance.getInventory().getItem(slot).copy();
            UseItemHelper.moveItemSwap(slot, 6);
            if (this.notifications.get() && !inventory.isEmpty()) {
               SendDonationHelper.displayInfo(is ? "Swapped to chestplate" : "Swapped to elytra");
            }
         }
      }
   }

   private void useFireworkPacket() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.gameMode != null && player.connection != null) {
         if (player.getMainHandItem().is(Items.FIREWORK_ROCKET)) {
            UseItemHelper.useItem(InteractionHand.MAIN_HAND);
         } else if (player.getOffhandItem().is(Items.FIREWORK_ROCKET)) {
            UseItemHelper.useItem(InteractionHand.OFF_HAND);
         } else {
            int slot = UseItemHelper.getSlot(Items.FIREWORK_ROCKET);
            if (slot != -1) {
               int selectedSlot = player.getInventory().getSelectedSlot();
               if (this.fireworkOffhand.get()) {
                  int slot2 = slot < 9 ? slot + 36 : slot;
                  UseItemHelper.clickSlotId(slot2, 40, ContainerInput.SWAP, false);
                  UseItemHelper.useItem(InteractionHand.OFF_HAND);
                  UseItemHelper.clickSlotId(slot2, 40, ContainerInput.SWAP, false);
                  UseItemHelper.closeInventoryQuietly();
               } else if (slot < 9) {
                  if (slot != selectedSlot) {
                     player.connection.send(new ServerboundSetCarriedItemPacket(slot));
                     UseItemHelper.useItem(InteractionHand.MAIN_HAND);
                     player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot));
                  } else {
                     UseItemHelper.useItem(InteractionHand.MAIN_HAND);
                  }
               } else {
                  int slot3 = (selectedSlot + 1) % 9;
                  UseItemHelper.clickSlotId(slot, slot3, ContainerInput.SWAP, false);
                  player.connection.send(new ServerboundSetCarriedItemPacket(slot3));
                  UseItemHelper.useItem(InteractionHand.MAIN_HAND);
                  player.connection.send(new ServerboundSetCarriedItemPacket(selectedSlot));
                  UseItemHelper.clickSlotId(slot, slot3, ContainerInput.SWAP, false);
                  UseItemHelper.closeInventoryQuietly();
               }
            }
         }
      }
   }

   private void onMoveInput(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled()) {
         if (this.movementStopTicks > 0) {
            playerInputEvent.setMoveVector(Vec2.ZERO);
            playerInputEvent.setKeyPresses(Input.EMPTY);
            Minecraft instance = Minecraft.getInstance();
            if (instance.options != null) {
               instance.options.keyJump.setDown(false);
               instance.options.keyShift.setDown(false);
            }
         } else {
            if (this.autoTakeoffJumpActive) {
               playerInputEvent.setJump(true);
            }
         }
      }
   }

   private void onKey(KeyEvent keyEvent) {
      int value = 1;
      value = (byte)1;
      value = (byte)-1;
      if (this.isEnabled()) {
         if (keyEvent.pressed()) {
            if (keyEvent.client().gui.screen() == null) {
               value = keyEvent.key().key();
               this.handleBind(value);
               return;
            }
         }
      }
   }

   private void onUpdate(PlayerTickEndEvent playerTickEndEvent) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null && instance.level != null) {
            if (this.swapCooldownTicks > 0) {
               this.swapCooldownTicks--;
            }

            if (this.movementStopTicks > 0) {
               this.movementStopTicks--;
            }

            this.handlePendingSwap();
            this.handlePendingFirework();
            if (player.isFallFlying()) {
               if (this.autoTakeoffArmed) {
                  this.autoTakeoffFireworkPending = true;
                  this.autoTakeoffArmed = false;
               }

               this.wasInElytraFlight = true;
            }

            this.handleLandingSwap();
            this.handleAutoTakeoff();
            this.handleAutoTakeoffFirework();
         }
      }
   }

   private void handleBind(int value) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (value == this.keySwap.get() && this.keySwap.get() != -1) {
            int slot = UseItemHelper.getSlot(Items.ELYTRA) != -1 ? 1 : 0;
            int chestPlateSlot = UseItemHelper.getChestPlateSlot() != -1 ? 1 : 0;
            if ((slot != 0 || chestPlateSlot != 0) && this.swapCooldownTicks == 0) {
               this.movementStopTicks = 2;
               this.requestSwap(null);
            }
         }

         if (value == this.fireworkKey.get() && this.fireworkKey.get() != -1 && player.isFallFlying()) {
            if (this.isLegitFirework()) {
               if (!this.fireworkRequested) {
                  this.movementStopTicks = 2;
                  this.fireworkRequested = true;
               }
            } else {
               this.useFirework();
            }
         }
      }
   }

   private void handleAutoTakeoffFirework() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (this.autoTakeoffFireworkPending && instance != null && instance.isFallFlying()) {
         this.autoTakeoffFireworkPending = false;
         this.wasInElytraFlight = true;
         if (this.fireworkOnTakeoff.get() && UseItemHelper.getSlot(Items.FIREWORK_ROCKET) != -1) {
            if (this.isLegitFirework()) {
               if (!this.fireworkRequested) {
                  this.movementStopTicks = 2;
                  this.fireworkRequested = true;
               }
            } else {
               this.useFirework();
            }
         }
      }
   }

   public boolean shouldForceAutoTakeoffJump() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (this.isEnabled()) {
         if (this.autoFly.get() && this.autoTakeoffArmed && this.autoTakeoffJumpActive) {
            if (this.movementStopTicks <= 0
               && player != null
               && instance.level != null
               && !player.isFallFlying()
               && !NeedsPacketOrderSafeHelper.needsReallyWorld1171Takeoff()) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean isLegitFirework() {
      return ServerAddressMatcher.isReallyWorld();
   }

   private void handlePendingFirework() {
      if (this.fireworkRequested && this.isLegitFirework()) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance == null || !instance.isFallFlying()) {
            this.fireworkRequested = false;
         } else if (this.movementStopTicks <= 0) {
            this.fireworkRequested = false;
            if (instance.isFallFlying()) {
               this.useFirework();
            }
         }
      } else if (!this.isLegitFirework()) {
         this.fireworkRequested = false;
      }
   }

   private void handleAutoTakeoff1171() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      this.autoTakeoffArmed = true;
      if (player.onGround()) {
         this.autoTakeoff1171GlideSent = false;
         this.autoTakeoffJumpActive = false;
         if (!this.isJumpKeyPressed()) {
            ((LivingEntityGetNoJumpDelayAccessor)player).skycore$setNoJumpDelay(0);
            player.jumpFromGround();
         }
      } else {
         this.autoTakeoffJumpActive = false;
         if (!this.autoTakeoff1171GlideSent && this.isJumpKeyPressed()) {
            StartFallFlyingHelper.startFallFlying();
            this.autoTakeoff1171GlideSent = true;
         }
      }
   }

   @Override
   public boolean affectsGameplay() {
      return true;
   }

   public void swapToElytra() {
      this.requestSwap(false);
   }

   public void swapToChestplate() {
      this.requestSwap(true);
   }

   private ElytraHelperModule() {
      super("Elytra Helper", "Quickly swaps elytra and uses fireworks", ModuleCategory.MISC);
      this.fireworkKey = this.add(new KeyBindSetting("Firework Key", -1));
      this.equipChestplate = this.add(new BooleanSetting("Unequip Elytra on Landing", false));
      this.autoFly = this.add(new BooleanSetting("Auto Takeoff", false));
      this.fireworkOnTakeoff = this.add(new BooleanSetting("Firework on Takeoff", false));
      this.fireworkOffhand = this.add(new BooleanSetting("Firework in Offhand", false));
      this.notifications = this.add(new BooleanSetting("Notifications", true));
      this.equipChestplate.visibleWhen(() -> {
         return this.keySwap.get() != -1;
      });
      this.fireworkOnTakeoff.visibleWhen(this.autoFly::get);
      this.fireworkOffhand.visibleWhen(() -> !this.isLegitFirework());
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onMoveInput);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onUpdate);
   }

   private boolean canStartGlide(ItemStack itemStack) {
      if (itemStack.getMaxDamage() > 0) {
         if (itemStack.getDamageValue() < itemStack.getMaxDamage() - 1) {
            return true;
         }
      }

      return false;
   }

   private void handleAutoTakeoff() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.options != null && this.movementStopTicks <= 0) {
         ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.CHEST);
         if (!this.autoFly.get()
            || player.getAbilities().flying
            || player.isInWater()
            || player.isInLava()
            || player.isFallFlying()
            || !itemBySlot.is(Items.ELYTRA)
            || !this.canStartGlide(itemBySlot)) {
            this.autoTakeoffArmed = false;
            this.autoTakeoffJumpActive = false;
            this.autoTakeoffCycleTick = 0;
            this.autoTakeoff1171GlideSent = false;
         } else if (NeedsPacketOrderSafeHelper.needsReallyWorld1171Takeoff()) {
            this.handleAutoTakeoff1171();
         } else {
            this.autoTakeoffArmed = true;
            int autoTakeoffCycleTick2 = this.autoTakeoffCycleTick;
            this.autoTakeoffJumpActive = ((autoTakeoffCycleTick2 != 0 && autoTakeoffCycleTick2 != 1 ? 0 : 1) != 0);
            if (autoTakeoffCycleTick2 == 0) {
               instance.options.keyJump.setDown(true);
               if (player.onGround()) {
                  ((LivingEntityGetNoJumpDelayAccessor)player).skycore$setNoJumpDelay(0);
                  player.jumpFromGround();
               }
            } else if (autoTakeoffCycleTick2 == 1) {
               instance.options.keyJump.setDown(false);
               instance.options.keyJump.setDown(true);
            } else {
               instance.options.keyJump.setDown(false);
            }

            this.autoTakeoffCycleTick = (this.autoTakeoffCycleTick + 1) % 3;
         }
      }
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      int intermediate = -1;
      if (this.isEnabled()) {
         if (mouseButtonEvent.pressed()) {
            Minecraft minecraft = mouseButtonEvent.client();
            if (minecraft.gui.screen() == null) {
               MouseButtonInfo mouseButtonInfo = mouseButtonEvent.button();
               intermediate = 1000 + mouseButtonInfo.button();
               this.handleBind(intermediate);
               return;
            }
         }
      }
   }

   private void useFirework() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && player.isFallFlying()) {
         if (this.isLegitFirework()) {
            UseItemHelper.inventorySwapClick(Items.FIREWORK_ROCKET);
         } else {
            this.useFireworkPacket();
         }
      }
   }

   private void handleLandingSwap() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (!this.equipChestplate.get()) {
            this.wasOnGround = instance.onGround();
         } else {
            if (instance.onGround()
               && !this.wasOnGround
               && this.wasInElytraFlight
               && instance.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
               && this.swapCooldownTicks == 0) {
               int chestPlateSlot = UseItemHelper.getChestPlateSlot();
               if (chestPlateSlot != -1) {
                  ItemStack inventory = instance.getInventory().getItem(chestPlateSlot).copy();
                  UseItemHelper.moveToArmor(chestPlateSlot, 6);
                  if (this.notifications.get() && !inventory.isEmpty()) {
                     SendDonationHelper.displayInfo("Swapped to chestplate");
                  }
               } else {
                  UseItemHelper.pickupItem(6, 0);
                  int emptyInventorySlot = UseItemHelper.findEmptyInventorySlot();
                  if (emptyInventorySlot != -1) {
                     UseItemHelper.pickupItem(emptyInventorySlot, 0);
                  }
               }

               this.wasInElytraFlight = false;
               this.swapCooldownTicks = 2;
            }

            this.wasOnGround = instance.onGround();
         }
      }
   }

   private void requestSwap(Boolean forcedSwapToChestplate2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         this.forcedSwapToChestplate = forcedSwapToChestplate2;
         if (!this.isEnabled()) {
            this.executeSwap(this.forcedSwapToChestplate);
            this.forcedSwapToChestplate = null;
         } else {
            this.swapRequested = true;
         }
      }
   }

   @Override
   protected void onDisable() {
      this.swapRequested = false;
      this.fireworkRequested = false;
      this.forcedSwapToChestplate = null;
      this.movementStopTicks = 0;
      this.swapCooldownTicks = 0;
      this.wasInElytraFlight = false;
      this.autoTakeoffArmed = false;
      this.autoTakeoffJumpActive = false;
      this.autoTakeoffFireworkPending = false;
      this.autoTakeoffCycleTick = 0;
      this.autoTakeoff1171GlideSent = false;
      Minecraft instance = Minecraft.getInstance();
      if (instance.options != null && instance.getWindow() != null) {
         instance.options.keyJump.setDown(KeyState.isDown(((KeyMappingGetKeyAccessor)instance.options.keyJump).skycore$getKey().getValue()));
      }

      super.onDisable();
   }

   private boolean isJumpKeyPressed() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.options == null) {
         return false;
      }

      if (instance.options.keyJump.isDown()) {
         return true;
      }

      if (instance.getWindow() == null) {
         return false;
      }

      Key key = ((KeyMappingGetKeyAccessor)instance.options.keyJump).skycore$getKey();
      int value = key.getValue();
      if (value >= 0) {
         if (KeyState.isDown(value)) {
            return true;
         }
      }

      return false;
   }
}
