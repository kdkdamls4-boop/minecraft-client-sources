package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.combat.RotationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererFixElytraFlyingTwistMixin {
   @Inject(
      method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
      at = @At("TAIL")
   )
   private void skycore$fixElytraFlyingTwist(Avatar avatar, AvatarRenderState avatarRenderState, float yaw, CallbackInfo callbackInfo) {
      Minecraft instance = Minecraft.getInstance();
      if (avatar instanceof LocalPlayer player && player == instance.player) {
         if (RotationController.isActive() && !RotationController.isClientLook()) {
            float renderServerYaw = RotationController.getRenderServerYaw(yaw);
            float visualBodyYaw = RotationController.getVisualBodyYaw(yaw);
            avatarRenderState.bodyRot = visualBodyYaw;
            avatarRenderState.yRot = Mth.wrapDegrees(renderServerYaw - visualBodyYaw);
            avatarRenderState.xRot = RotationController.getRenderServerPitch(yaw);
            if (avatarRenderState.isFallFlying) {
               float yRot = player.getYRot();
               float xRot = player.getXRot();
               Vec3 position = Vec3.directionFromRotation(xRot, yRot);
               Vec3 position2;
               if (avatar instanceof ClientAvatarEntity clientAvatarEntity) {
                  position2 = clientAvatarEntity.avatarState().deltaMovementOnPreviousTick().lerp(avatar.getDeltaMovement(), yaw);
               } else {
                  position2 = avatar.getDeltaMovement();
               }

               if (position2.horizontalDistanceSqr() > 1.0E-5 && position.horizontalDistanceSqr() > 1.0E-5) {
                  Vec3 position3 = position2.horizontal().normalize();
                  Vec3 position4 = position.horizontal().normalize();
                  double value = position3.dot(position4);
                  double x = position2.x * position.z - position2.z * position.x;
                  avatarRenderState.shouldApplyFlyingYRot = true;
                  avatarRenderState.flyingYRot = (float)(Math.signum(x) * Math.acos(Math.min(1.0, Math.abs(value))));
               } else {
                  avatarRenderState.shouldApplyFlyingYRot = false;
                  avatarRenderState.flyingYRot = 0.0F;
               }
            }
         }
      }
   }
}
