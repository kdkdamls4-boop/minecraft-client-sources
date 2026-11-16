package tokyo.ghoul.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.api.event.events.player.move.EntityVelocityEvent;
import tokyo.ghoul.api.event.events.player.move.VelocityEvent;
import tokyo.ghoul.api.event.events.player.world.PushEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void cancelGlowingHook(CallbackInfoReturnable<Boolean> cir) {
        if (RemovalsModule.getInstance().isGlowEffect()) {
            cir.setReturnValue(false);
        }
    }

    @Redirect(method = "moveRelative", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"))
    public Vec3 updateVelocityHook(Vec3 movementInput, float speed, float yaw) {
        if ((Object) this == SharedClass.player()) {
            VelocityEvent.VelocityEventData event = new VelocityEvent.VelocityEventData(movementInput, speed, yaw, Entity.getInputVector(movementInput, speed, yaw));
            VelocityEvent.getInstance().call(event);
            return event.getVelocity();
        }


        return Entity.getInputVector(movementInput, speed, yaw);
    }

    @ModifyReturnValue(method = "getDeltaMovement", at = @At("RETURN"))
    private Vec3 deltaMovementHook(Vec3 original) {
        if ((Object) this != SharedClass.player()) return original;
        EntityVelocityEvent.EntityVelocityEventData event = new EntityVelocityEvent.EntityVelocityEventData(original);
        return EntityVelocityEvent.getInstance().call(event) ? event.getVelocity() : original;
    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void onPushAwayFrom(Entity entity, CallbackInfo ci) {
        if ((Object) this == SharedClass.player()
                && PushEvent.getInstance().call(new PushEvent.PushEventData(PushEvent.PushEventData.PushingSource.ENTITY))) {
            ci.cancel();
        }
    }

}
