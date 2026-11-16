package net.skycore.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.skycore.client.combat.RotationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraSyncCameraRotationMixin {
   @Inject(method = "alignWithEntity", at = @At("HEAD"))
   private void skycore$syncCameraRotation(float value, CallbackInfo callbackInfo) {
      Camera camera = (Camera)(Object)this;
      Entity entity = camera.entity();
      if (entity != null) {
         RotationController.applyRenderInterpolation();
         RotationController.syncFreeLook(entity.getViewYRot(value), entity.getViewXRot(value));
      }
   }

   @Redirect(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V"))
   private void skycore$applyVisualFreeLook(Camera camera, float angle, float angle2) {
      Minecraft instance = Minecraft.getInstance();
      if (RotationController.isActive() && !RotationController.isClientLook() && camera.entity() != null && camera.entity() == instance.player) {
         float freeYaw = RotationController.getFreeYaw();
         float freePitch = RotationController.getFreePitch();
         if (instance.options.getCameraType().isMirrored()) {
            freeYaw += 180.0F;
            freePitch = -freePitch;
         }

         ((CameraInvokeSetRotationAccessor)camera).skycore$invokeSetRotation(freeYaw, freePitch);
      } else {
         ((CameraInvokeSetRotationAccessor)camera).skycore$invokeSetRotation(angle, angle2);
      }
   }
}
