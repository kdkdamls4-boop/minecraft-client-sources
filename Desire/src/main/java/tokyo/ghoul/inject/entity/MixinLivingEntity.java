package tokyo.ghoul.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.api.event.events.player.move.JumpEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationPlan;
import tokyo.ghoul.client.features.modules.render.SwingAnimationModule;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends MixinEntity {
    @Shadow
    public abstract boolean isFallFlying();

    @Shadow
    public int noJumpDelay;

    @Shadow
    public net.minecraft.world.InteractionHand swingingArm;

    @Shadow
    public abstract net.minecraft.world.item.ItemStack getUseItem();

    @Inject(method = "completeUsingItem", at = @At("HEAD"))
    private void onCompleteUsingItem(CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) return;
        tokyo.ghoul.client.features.modules.misc.AssistModule.onFinishUsing(getUseItem().copy());
    }

    @Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
    private void getArmSwingAnimationEnd(final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        int duration = SwingAnimationModule.getInstance()
                .swingDuration(this.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND);
        if (duration > 0) callbackInfoReturnable.setReturnValue(duration);
    }

    @Inject(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getJumpPower()F"))
    public void onJumping(CallbackInfo ci) {
        if (((Object) this) != SharedClass.player()) {
            return;
        }

        JumpEvent.getInstance().call();
    }

    @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    public float fixJumpVelocity(float original) {
        if ((Object) this != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null || !currentRotationPlan.moveCorrection()) {
            return original;
        }

        return rotation.getYRot();
    }

    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
    private float fixGlidingVelocity(float original) {
        Rotation rotation = silentElytraRotation();
        return rotation != null ? rotation.getXRot() : original;
    }


    @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 fixGlidingVelocityVector(Vec3 original) {
        Rotation rotation = silentElytraRotation();
        return rotation != null ? rotation.getVector() : original;
    }

    @Inject(method = "updateFallFlyingMovement", at = @At("RETURN"), cancellable = true)
    private void onUpdateFallFlyingMovement(Vec3 original, CallbackInfoReturnable<Vec3> cir) {
        if ((Object) this == SharedClass.player()) {
            tokyo.ghoul.api.event.events.player.move.TravelPostEvent.TravelPostEventData data =
                    new tokyo.ghoul.api.event.events.player.move.TravelPostEvent.TravelPostEventData(cir.getReturnValue());
            boolean cancelled = tokyo.ghoul.api.event.events.player.move.TravelPostEvent.getInstance().call(data);
            if (cancelled || (data.getVelocity() != null && !data.getVelocity().equals(cir.getReturnValue()))) {
                cir.setReturnValue(data.getVelocity());
            }
        }
    }

    @Unique
    private Rotation silentElytraRotation() {
        if ((Object) this != SharedClass.player()) return null;
        RotationManager rotationManager = RotationManager.getInstance();
        if (rotationManager.getCurrentRotationPlan() == null) return null;
        return rotationManager.getRotation();
    }
}
