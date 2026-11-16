package deluxe.mixin;

import deluxe.client.MinecraftAccess;
import deluxe.client.render.EntityRenderRotations;
import deluxe.client.module.movement.SpiderModule;
import deluxe.client.module.render.SwordAnimationsModule;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin2 implements MinecraftAccess {
   @Inject(method = "getHandSwingDuration()I", at = @At("HEAD"), cancellable = true)
   private void customHandSwingDuration(CallbackInfoReturnable<Integer> local8296_1) {
      if (SwordAnimationsModule.method1770().method1762() && (Object)this == mc.player) {
         local8296_1.setReturnValue(SwordAnimationsModule.method1770().method1756());
      }
   }

   @Inject(method = "tick()V", at = @At("HEAD"))
   private void shiftVisualRotationHistory(CallbackInfo callbackInfo) {
      if ((Object)this == mc.player) {
         EntityRenderRotations.previousPitch = EntityRenderRotations.pitch;
         EntityRenderRotations.previousHeadYaw = EntityRenderRotations.headYaw;
      }
   }

   @Inject(method = "jump()V", at = @At("HEAD"), cancellable = true)
   private void holdSpiderJump(CallbackInfo callbackInfo) {
      if ((Object)this == mc.player && SpiderModule.method2836().method2853()) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "tick()V", at = @At("TAIL"))
   private void captureVisualRotation(CallbackInfo callbackInfo) {
      if ((Object)this == mc.player) {
         EntityRenderRotations.pitch = ((LivingEntity)(Object)this).getPitch();
         EntityRenderRotations.headYaw = ((LivingEntity)(Object)this).headYaw;
      }
   }
}
