package deluxe.mixin;

import deluxe.client.module.render.CameraFeatureModule;
import deluxe.client.module.render.FreeCameraModule;
import deluxe.client.MinecraftAccess;
import deluxe.client.RotationManager;
import deluxe.client.module.render.SmoothFeatureModule;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin implements MinecraftAccess {
   @Shadow
   protected abstract void setPos(Vec3d vec3d);

   @Shadow
   protected abstract void setRotation(float var1, float var2);

   @Inject(method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V", at = @At("TAIL"))
   private void moveCameraToFreeCamera(
      World world, Entity entity, boolean local8244_3, boolean local8244_4, float local8244_5, CallbackInfo callbackInfo
   ) {
      if (FreeCameraModule.method0910().method0913()) {
         this.setRotation(FreeCameraModule.method0910().method0915(), FreeCameraModule.method0910().method0912());
         this.setPos(FreeCameraModule.method0910().method0907(local8244_5));
      } else if (CameraFeatureModule.method0782().method0781()) {
         this.setRotation(CameraFeatureModule.method0782().method0787(), CameraFeatureModule.method0782().method0783());
         this.setPos(CameraFeatureModule.method0782().method0790(local8244_5));
      }
   }

   @Inject(method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V", at = @At("TAIL"))
   private void glideCameraAfterFlag(
      World world, Entity entity, boolean local8245_3, boolean local8245_4, float local8245_5, CallbackInfo callbackInfo
   ) {
      if (SmoothFeatureModule.method3278().method3282()) {
         this.setPos(((Camera)(Object)this).getCameraPos().add(SmoothFeatureModule.method3278().method3283()));
      }
   }

   @Redirect(method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getYaw(F)F"))
   private float steerCameraYaw(Entity entity, float local8246_2) {
      return entity == mc.player && RotationManager.method0658().method0667()
         ? RotationManager.method0658().method0666()
         : entity.getYaw(local8246_2);
   }

   @Redirect(method = "update(Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;ZZF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getPitch(F)F"))
   private float steerCameraPitch(Entity entity, float local8247_2) {
      return entity == mc.player && RotationManager.method0658().method0667()
         ? RotationManager.method0658().method0655()
         : entity.getPitch(local8247_2);
   }

   @Inject(method = "isThirdPerson()Z", at = @At("HEAD"), cancellable = true)
   private void forceDetachedWhileFreeCamera(CallbackInfoReturnable<Boolean> local8248_1) {
      if (FreeCameraModule.method0910().method0909() || CameraFeatureModule.method0782().method0792()) {
         local8248_1.setReturnValue(true);
      }
   }
}
