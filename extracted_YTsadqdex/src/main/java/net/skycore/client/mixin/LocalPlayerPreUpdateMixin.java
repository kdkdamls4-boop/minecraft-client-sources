package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.RotationController;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.combat.ServerRotationManager;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.event.PlayerTickStartEvent;
import net.skycore.client.module.combat.AimbotModule;
import net.skycore.client.module.combat.CriticalsModule;
import net.skycore.client.module.movement.GuiMoveModule;
import net.skycore.client.util.BeforeMovementHelper;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.util.SynchronizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class, priority = 1100)
public abstract class LocalPlayerPreUpdateMixin {
   @Inject(method = "tick", at = @At("HEAD"))
   private void skycore$preUpdate(CallbackInfo callbackInfo) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      RotationController.beginTick();
      RotationController.syncEntityRenderRotations(player);
      if (SynchronizeHelper.isActive()) {
         AimbotModule.INSTANCE.onPlayerTickStart();
         RotationManager.tickIdle();
         ServerRotationManager.tickIdle();
      }
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V"))
   private void skycore$playerPrePhysics(CallbackInfo callbackInfo) {
      if (SynchronizeHelper.isActive()) {
         CriticalsModule.INSTANCE.onPlayerPreTick();
         SkycoreClient.events().post(new PlayerTickEndEvent((LocalPlayer)(Object)this));
      }
   }

   @Inject(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z", shift = Shift.AFTER))
   private void skycore$experienceBeforeMovement(CallbackInfo callbackInfo) {
      ScheduleHelper.beforeMovement();
      BeforeMovementHelper.beforeMovement((LocalPlayer)(Object)this);
   }

   @Inject(method = "closeContainer", at = @At("HEAD"), cancellable = true)
   private void skycore$closeContainer(CallbackInfo callbackInfo) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      if (player == Minecraft.getInstance().player && GuiMoveModule.INSTANCE.interceptInventoryClose()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "tick", at = @At("TAIL"))
   private void skycore$bobTowardFreeLook(CallbackInfo callbackInfo) {
      LocalPlayer player = (LocalPlayer)(Object)this;
      if (SynchronizeHelper.isActive()) {
         SkycoreClient.events().post(new PlayerTickStartEvent(player));
      }

      if (RotationController.isActive() && !RotationController.isClientLook()) {
         float freeYaw = RotationController.getFreeYaw();
         float freePitch = RotationController.getFreePitch();
         player.yBob = player.yBobO + Mth.wrapDegrees(freeYaw - player.yBobO) * 0.5F;
         player.xBob = player.xBobO + (freePitch - player.xBobO) * 0.5F;
      }
   }
}
