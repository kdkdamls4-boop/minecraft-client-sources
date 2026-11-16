package deluxe.mixin;

import deluxe.client.module.combat.AimAssistModule;
import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.module.render.CameraFeatureModule;
import deluxe.client.event.EntityMovementEvent;
import deluxe.client.event.EntityYawEvent;
import deluxe.client.event.EventBus;
import deluxe.client.module.render.FreeCameraModule;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.MouseLookEvent;
import deluxe.client.module.player.NoPushModule;
import deluxe.client.RotationManager;
import deluxe.client.module.render.SeeInvisibleModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements MinecraftAccess {
   @Unique
   private static EntityYawEvent sharedHitBoxEvent;
   @Unique
   private static boolean sharedHitBoxEventBusy;

   @Inject(method = "changeLookDirection(DD)V", at = @At("HEAD"), cancellable = true)
   private void steerFreeCamera(double local8229_1, double local8229_3, CallbackInfo callbackInfo) {
      if ((Object)this == mc.player && FreeCameraModule.method0910().isEnabled()) {
         FreeCameraModule.method0910().method0908(local8229_1, local8229_3);
         callbackInfo.cancel();
      } else if ((Object)this == mc.player && CameraFeatureModule.method0782().method0789(local8229_1, local8229_3)) {
         callbackInfo.cancel();
      } else {
         if ((Object)this == mc.player && RotationManager.method0658().method0654(local8229_1, local8229_3)) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "changeLookDirection(DD)V", at = @At("RETURN"))
   private void steerAimAssist(double local8230_1, double local8230_3, CallbackInfo callbackInfo) {
      if ((Object)this == mc.player) {
         AimAssistModule.method0883().method0896(local8230_1, local8230_3);
      }

      if ((Object)this == mc.player) {
         AttackAuraModule.method1029().method1002();
      }
   }

   @ModifyVariable(method = "move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), argsOnly = true)
   private Vec3d applyMoveEvent(Vec3d vec3d) {
      if ((Object)this != mc.player) {
         return vec3d;
      }

      if (EventBus.getInstance().hasListeners(EntityMovementEvent.class)) {
         EntityMovementEvent entityMovementEvent = new EntityMovementEvent(vec3d);
         EventBus.getInstance().post(entityMovementEvent);
         vec3d = entityMovementEvent.method4394();
      }

      if (!EventBus.getInstance().hasListeners(MouseLookEvent.class)) {
         return vec3d;
      }

      MouseLookEvent mouseLookEvent = new MouseLookEvent(vec3d.x, vec3d.y, vec3d.z, mc.player.isOnGround());
      EventBus.getInstance().post(mouseLookEvent);
      mc.player.setOnGround(mouseLookEvent.method4022());
      if (mouseLookEvent.method4421()) {
         mc.player.setVelocity(mouseLookEvent.method4018(), mouseLookEvent.method4023(), mouseLookEvent.method4016());
      }

      return new Vec3d(mouseLookEvent.method4018(), mouseLookEvent.method4023(), mouseLookEvent.method4016());
   }

   @Inject(method = "pushAwayFrom(Lnet/minecraft/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void removeEntityPush(Entity entity, CallbackInfo callbackInfo) {
      if (NoPushModule.method3305().method3306("Сущности")) {
         if ((Object)this == mc.player || entity == mc.player) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "adjustMovementForPiston(Lnet/minecraft/util/math/Vec3d;)Lnet/minecraft/util/math/Vec3d;", at = @At("HEAD"), cancellable = true)
   private void removePistonPush(Vec3d vec3d, CallbackInfoReturnable<Vec3d> local8233_2) {
      if ((Object)this == mc.player && NoPushModule.method3305().method3306("Блоки")) {
         local8233_2.setReturnValue(Vec3d.ZERO);
      }
   }

   @Redirect(method = "updateMovementInFluid(Lnet/minecraft/registry/tag/TagKey;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isPushedByFluids()Z"))
   private boolean removeFluidPush(Entity entity, TagKey<Fluid> local8234_2, double local8234_3) {
      return entity != mc.player
            || (local8234_2 == FluidTags.WATER ? !NoPushModule.method3305().method3306("Вода") : !NoPushModule.method3305().method3306("Лава"))
         ? entity.isPushedByFluids()
         : false;
   }

   @Inject(method = "isInvisibleTo(Lnet/minecraft/entity/player/PlayerEntity;)Z", at = @At("HEAD"), cancellable = true)
   private void showInvisibleEntities(PlayerEntity playerEntity, CallbackInfoReturnable<Boolean> local8235_2) {
      if (playerEntity == mc.player && (Object)this instanceof AbstractClientPlayerEntity && SeeInvisibleModule.method4439().isEnabled()) {
         local8235_2.setReturnValue(false);
      }
   }

   @Inject(method = "getBoundingBox()Lnet/minecraft/util/math/Box;", at = @At("RETURN"), cancellable = true)
   private void applyEntityHitBox(CallbackInfoReturnable<Box> local8236_1) {
      if ((Object)this instanceof LivingEntity && mc.player != null && (Object)this != mc.player && EventBus.getInstance().hasListeners(EntityYawEvent.class)) {
         boolean local8236_2 = !sharedHitBoxEventBusy && mc.isOnThread();
         if (local8236_2 && sharedHitBoxEvent == null) {
            sharedHitBoxEvent = new EntityYawEvent(null, 0.0F);
         }

         EntityYawEvent entityYawEvent = local8236_2 ? sharedHitBoxEvent : new EntityYawEvent((Entity)(Object)this, 0.0F);
         float local8236_4;
         if (local8236_2) {
            sharedHitBoxEventBusy = true;

            try {
               entityYawEvent.method3193((Entity)(Object)this);
               entityYawEvent.method3194(0.0F);
               EventBus.getInstance().post(entityYawEvent);
               local8236_4 = entityYawEvent.method3192();
            } finally {
               entityYawEvent.method3193(null);
               sharedHitBoxEventBusy = false;
            }
         } else {
            EventBus.getInstance().post(entityYawEvent);
            local8236_4 = entityYawEvent.method3192();
         }

         if (local8236_4 != 0.0F) {
            Box box = (Box)local8236_1.getReturnValue();
            local8236_1.setReturnValue(
               new Box(
                  box.minX - local8236_4,
                  box.minY,
                  box.minZ - local8236_4,
                  box.maxX + local8236_4,
                  box.maxY,
                  box.maxZ + local8236_4
               )
            );
         }
      }
   }

   static {
      VMBridge.identifyClass(EntityMixin.class, "qY92U9te");
   }
}
