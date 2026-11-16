package deluxe.mixin;

import deluxe.client.module.movement.AutoSprintModule;
import deluxe.client.module.combat.BackTrackModule;
import deluxe.client.event.EventBus;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.PostAttackEvent;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements MinecraftAccess {
   @Redirect(method = "knockbackTarget(Lnet/minecraft/entity/Entity;FLnet/minecraft/util/math/Vec3d;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/Vec3d;multiply(DDD)Lnet/minecraft/util/math/Vec3d;"))
   private Vec3d keepAttackSprintVelocity(Vec3d vec3d, double local8255_2, double local8255_4, double local8255_6) {
      return (Object)this == mc.player && AutoSprintModule.method3360().method3361()
         ? vec3d.multiply(1.0, local8255_4, 1.0)
         : vec3d.multiply(local8255_2, local8255_4, local8255_6);
   }

   @Inject(method = "hasBlindnessEffect()Z", at = @At("HEAD"), cancellable = true)
   private void sprintThroughBlindness(CallbackInfoReturnable<Boolean> local8256_1) {
      if ((Object)this == mc.player && AutoSprintModule.method3360().method3362()) {
         local8256_1.setReturnValue(false);
      }
   }

   @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("TAIL"))
   private void keepSprintAfterAttack(Entity entity, CallbackInfo callbackInfo) {
      if ((Object)this == mc.player && EventBus.getInstance().hasListeners(PostAttackEvent.class)) {
         EventBus.getInstance().post(new PostAttackEvent());
      }
   }

   @Inject(method = "attack(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"))
   private void trackOwnHitForBackTrack(Entity entity, CallbackInfo callbackInfo) {
      if ((Object)this == mc.player) {
         BackTrackModule.method2909().method2908(entity);
      }
   }
}
