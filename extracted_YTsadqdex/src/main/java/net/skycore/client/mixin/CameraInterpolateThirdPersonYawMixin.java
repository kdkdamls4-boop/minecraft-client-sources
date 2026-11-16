package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.skycore.client.module.movement.FreeCameraModule;
import net.skycore.client.module.render.InterpolateF5Module;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public abstract class CameraInterpolateThirdPersonYawMixin {
   @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"))
   private float skycore$interpolateThirdPersonYaw(Entity entity, float yaw, Operation<Float> operation) {
      InterpolateF5Module interpolateF5Module = skycore$module();
      return interpolateF5Module == null ? (Float)operation.call(new Object[]{entity, yaw}) : interpolateF5Module.getInterpolatedYaw(yaw);
   }

   @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"))
   private float skycore$interpolateThirdPersonPitch(Entity entity, float pitch, Operation<Float> operation) {
      InterpolateF5Module interpolateF5Module = skycore$module();
      return interpolateF5Module == null ? (Float)operation.call(new Object[]{entity, pitch}) : interpolateF5Module.getInterpolatedPitch(pitch);
   }

   @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
   private float skycore$interpolateThirdPersonDistance(Camera camera, float distance, Operation<Float> operation, @Local(argsOnly = true) float distance2) {
      InterpolateF5Module interpolateF5Module = skycore$module();
      if (interpolateF5Module != null) {
         distance = interpolateF5Module.getInterpolatedDistance(distance2);
      }

      return FreeCameraModule.INSTANCE.isEnabled() ? distance : (Float)operation.call(new Object[]{camera, distance});
   }

   @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;move(FFF)V"))
   private void skycore$interpolateThirdPersonHeight(Camera camera, float value, float height, float value2, Operation<Void> operation, @Local(argsOnly = true) float height2) {
      InterpolateF5Module interpolateF5Module = skycore$module();
      if (interpolateF5Module != null) {
         height += interpolateF5Module.getInterpolatedHeightOffset(height2);
      }

      operation.call(new Object[]{camera, value, height, value2});
   }

   private static InterpolateF5Module skycore$module() {
      Minecraft instance = Minecraft.getInstance();
      return instance.player != null && instance.level != null && !instance.options.getCameraType().isFirstPerson() ? InterpolateF5Module.enabled() : null;
   }
}
