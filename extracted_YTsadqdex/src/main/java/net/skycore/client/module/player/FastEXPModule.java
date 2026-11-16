package net.skycore.client.module.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec2;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionStop;
import net.skycore.client.combat.FreeLookController;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationController;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.HudSetOverlayMessageTimeAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.BeforeMovementHelper;
import net.skycore.client.setting.KeyBindSetting;
import net.skycore.client.util.KeyState;
import net.skycore.client.util.MovementInput;
import net.skycore.client.util.NeedsPacketOrderSafeHelper;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.util.SpoofElytraAsChestplateHelper;
import net.skycore.client.util.UseItemHelper;

public final class FastEXPModule extends ClientModule {
   private int movementStopTicks;
   private int pendingExpSlot;
   private static final int ROTATION_PRIORITY = 48;
   private int selectedSlot;
   private boolean restockPending;
   private boolean restorePending;
   private boolean active;
   private static final int STOP_TICKS = 2;
   private boolean noExpNotified;
   public static final FastEXPModule INSTANCE = new FastEXPModule();
   private final KeyBindSetting throwKey = this.add(new KeyBindSetting("Throw Key", -1));
   private boolean swapPending;
   private int sourceSlot;

   private boolean performPendingSwap(Minecraft minecraft, LocalPlayer player) {
      int selectedSlot2 = this.selectedSlot >= 0 ? this.selectedSlot : player.getInventory().getSelectedSlot();
      int pendingExpSlot2 = this.pendingExpSlot;
      this.swapPending = false;
      this.pendingExpSlot = -1;
      if (pendingExpSlot2 < 0) {
         return false;
      }

      SpoofElytraAsChestplateHelper.hold(selectedSlot2, 20);
      suppressTooltip(minecraft);
      UseItemHelper.clickSlotId(UseItemHelper.toMenuSlot(pendingExpSlot2), selectedSlot2, ContainerInput.SWAP, false);
      UseItemHelper.syncCurrentPlayItem();
      this.sourceSlot = pendingExpSlot2;
      return player.getInventory().getItem(selectedSlot2).is(Items.EXPERIENCE_BOTTLE);
   }

   private void beginStop() {
      if (!NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         this.movementStopTicks = 0;
      } else {
         this.movementStopTicks = 2;
         ActionStop.stop = true;
         ActionStop.stopTicks = Math.max(ActionStop.stopTicks, 2);
      }
   }

   private static void suppressTooltip(Minecraft minecraft) {
      if (minecraft.gui != null) {
         HudSetOverlayMessageTimeAccessor hudSetOverlayMessageTimeAccessor = (HudSetOverlayMessageTimeAccessor)minecraft.gui.hud;
         hudSetOverlayMessageTimeAccessor.skycore$setOverlayMessageTime(0);
         hudSetOverlayMessageTimeAccessor.skycore$setToolHighlightTimer(0);
      }
   }

   private void finishSession(Minecraft minecraft) {
      if (this.active || this.swapPending || this.restorePending || this.restockPending) {
         SpoofElytraAsChestplateHelper.refresh(8);
         suppressTooltip(minecraft);
         RotationManager.INSTANCE.stopRotationIfPriority(48);
      }

      this.active = false;
      this.selectedSlot = -1;
      this.sourceSlot = -1;
      this.pendingExpSlot = -1;
      this.swapPending = false;
      this.restorePending = false;
      this.restockPending = false;
      this.movementStopTicks = 0;
   }

   private void keepStopped() {
      if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         ActionStop.stop = true;
         ActionStop.stopTicks = Math.max(ActionStop.stopTicks, 1);
      }
   }

   private void aimDown(LocalPlayer player) {
      float yRot = FreeLookController.isActive() ? FreeLookController.getFreeYaw() : player.getYRot();
      Rotation rotation = new Rotation(yRot, 90.0F);
      RotationManager.update(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 4, 48, false);
   }

   private boolean ensureExpInSelected(Minecraft minecraft, LocalPlayer player) {
      int selectedSlot2 = player.getInventory().getSelectedSlot();
      if (this.swapPending) {
         return !this.canSwapNow() ? true : this.performPendingSwap(minecraft, player);
      }

      if (player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)) {
         if (!this.active) {
            this.selectedSlot = selectedSlot2;
            this.sourceSlot = -1;
            SpoofElytraAsChestplateHelper.hold(selectedSlot2, 20);
            suppressTooltip(minecraft);
            this.active = true;
         }

         return true;
      } else if (this.active && this.sourceSlot >= 0) {
         if (player.getInventory().getItem(selectedSlot2).is(Items.EXPERIENCE_BOTTLE)) {
            return true;
         }

         int slot = UseItemHelper.getSlot(Items.EXPERIENCE_BOTTLE);
         if (slot == -1) {
            if (!this.noExpNotified) {
               SendDonationHelper.displayWarning("Закончился опыт");
               this.noExpNotified = true;
            }

            this.requestRestore(false);
            return true;
         } else {
            this.pendingExpSlot = slot;
            this.requestRestore(true);
            return true;
         }
      } else {
         int slot2 = UseItemHelper.getSlot(Items.EXPERIENCE_BOTTLE);
         if (slot2 == -1) {
            return false;
         }

         if (slot2 == selectedSlot2) {
            if (!this.active) {
               this.selectedSlot = selectedSlot2;
               this.sourceSlot = -1;
               SpoofElytraAsChestplateHelper.hold(selectedSlot2, 20);
               suppressTooltip(minecraft);
               this.active = true;
            }

            return true;
         } else {
            this.selectedSlot = selectedSlot2;
            this.pendingExpSlot = slot2;
            SpoofElytraAsChestplateHelper.hold(selectedSlot2, 20);
            suppressTooltip(minecraft);
            this.active = true;
            this.beginStop();
            this.swapPending = true;
            ScheduleHelper.cancel();
            return true;
         }
      }
   }

   private void onMoveInput(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled()) {
         if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
            if (this.movementStopTicks <= 0) {
               if (!this.swapPending && !this.restorePending && !this.restockPending) {
                  return;
               }
            }

            playerInputEvent.setMoveVector(Vec2.ZERO);
            playerInputEvent.setKeyPresses(Input.EMPTY);
            this.keepStopped();
         }
      }
   }

   private boolean lookingDown() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return false;
      }

      float pitch;
      if (RotationController.isActive()) {
         pitch = RotationController.getServerPitch();
      } else {
         pitch = instance.getXRot();
      }

      float value = pitch;
      return value >= 85.0F;
   }

   private void requestStop(Minecraft minecraft) {
      if (!this.active && !this.swapPending) {
         if (!this.restorePending) {
            if (!this.restockPending) {
               return;
            }
         }
      }

      this.swapPending = false;
      this.restockPending = false;
      this.pendingExpSlot = -1;
      if (this.sourceSlot >= 0) {
         this.requestRestore(false);
      } else {
         this.finishSession(minecraft);
      }
   }

   private boolean canSwapNow() {
      if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         if (this.movementStopTicks <= 0) {
            if (!MovementInput.hasPlayerMovement()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   @Override
   protected void onDisable() {
      Minecraft instance = Minecraft.getInstance();
      this.forceCleanup(instance);
      super.onDisable();
   }

   private void restoreSwap(LocalPlayer player) {
      int value = 0;
      if (this.sourceSlot >= 0 && this.selectedSlot >= 0) {
         value = UseItemHelper.toMenuSlot(this.sourceSlot);
         UseItemHelper.clickSlotId(value, this.selectedSlot, ContainerInput.SWAP, false);
         UseItemHelper.syncCurrentPlayItem();
         this.sourceSlot = -1;
         this.restorePending = false;
      } else {
         this.sourceSlot = -1;
         this.restorePending = false;
      }
   }

   private FastEXPModule() {
      super("Fast EXP", "Быстро кидает пузырьки опыта по зажатому бинду, скрывая свап", ModuleCategory.PLAYER);
      this.selectedSlot = -1;
      this.sourceSlot = -1;
      this.pendingExpSlot = -1;
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onMoveInput);
   }

   private void forceCleanup(Minecraft minecraft) {
      if (!this.active && !this.swapPending) {
         if (!this.restorePending && !this.restockPending) {
            if (this.sourceSlot < 0) {
               if (this.movementStopTicks <= 0) {
                  return;
               }
            }
         }
      }

      LocalPlayer player = minecraft.player;
      if (player != null && this.sourceSlot >= 0) {
         this.restoreSwap(player);
      }

      this.finishSession(minecraft);
   }

   private void throwBottle(LocalPlayer player) {
      if (player.connection != null && player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)) {
         BeforeMovementHelper.defer(player, InteractionHand.MAIN_HAND);
      }
   }

   private void onPreTick(PlayerTickEndEvent event) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (this.isEnabled() && player != null && instance.level != null && instance.gui.screen() == null) {
         if (this.movementStopTicks > 0) {
            this.movementStopTicks--;
            this.keepStopped();
         }

         int computedValue = this.throwKey.get() != -1 && KeyState.isDown(this.throwKey) ? 1 : 0;
         if (this.restorePending) {
            if (this.canSwapNow()) {
               this.restoreSwap(player);
               if (this.restockPending) {
                  this.restockPending = false;
                  this.swapPending = true;
                  this.beginStop();
               } else {
                  this.finishSession(instance);
               }
            }
         } else if (computedValue == 0) {
            this.requestStop(instance);
            this.noExpNotified = false;
         } else {
            int mainHandItem = !player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)
                  && !player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE)
                  && UseItemHelper.getSlot(Items.EXPERIENCE_BOTTLE) == -1
                  && (!this.active || this.selectedSlot < 0 || !player.getInventory().getItem(this.selectedSlot).is(Items.EXPERIENCE_BOTTLE))
               ? 0
               : 1;
            if (mainHandItem == 0 && !this.swapPending && !this.restorePending && !this.restockPending) {
               if (!this.noExpNotified) {
                  SendDonationHelper.displayWarning(this.active ? "Закончился опыт" : "Нет опыта");
                  this.noExpNotified = true;
               }

               if (this.active) {
                  this.requestStop(instance);
               }
            } else {
               this.aimDown(player);
               if (!this.ensureExpInSelected(instance, player)) {
                  if (!this.noExpNotified) {
                     SendDonationHelper.displayWarning(this.active ? "Закончился опыт" : "Нет опыта");
                     this.noExpNotified = true;
                  }

                  this.requestStop(instance);
               } else if (!this.swapPending && !this.restorePending && !this.restockPending) {
                  SpoofElytraAsChestplateHelper.refresh(12);
                  suppressTooltip(instance);
                  if (this.lookingDown()) {
                     this.throwBottle(player);
                  }
               }
            }
         }
      } else {
         this.forceCleanup(instance);
      }
   }

   private void requestRestore(boolean restockPending2) {
      if (this.sourceSlot >= 0) {
         if (!this.restorePending) {
            this.restockPending = restockPending2;
            this.beginStop();
            this.restorePending = true;
            ScheduleHelper.cancel();
            return;
         }
      }
   }
}
