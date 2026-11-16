package tokyo.ghoul.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.event.events.player.move.MotionEvent;
import tokyo.ghoul.api.event.events.player.other.CloseScreenEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.player.move.MoveEvent;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.event.events.player.world.PushEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.combat.SprintManager;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationPlan;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity extends AbstractClientPlayer {
    public MixinClientPlayerEntity(ClientLevel world, GameProfile profile) {
        super(world, profile);
    }

    @Shadow
    public ClientInput input;

    @Inject(method = "tick", at = @At("HEAD"))
    public void tickHook(CallbackInfo ci) {
        UpdateEvent.getInstance().call();
    }

    @Inject(method = "move", at = @At("HEAD"))
    public void resetWebState(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        tokyo.ghoul.client.features.modules.movement.NoWebModule.setInWeb(false);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"), cancellable = true)
    public void moveHook(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        MoveEvent.MoveEventData event = new MoveEvent.MoveEventData(movement.x, movement.y, movement.z);
        if (MoveEvent.getInstance().call(event)) {
            super.move(movementType, new Vec3(event.getX(), event.getY(), event.getZ()));
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = {"sendPosition", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float silentRotationYaw(float original) {
        RotationManager rotationManager = RotationManager.getInstance();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        Rotation rotation = rotationManager.getRotation();

        if (SharedClass.player() != null) {
            float yaw = rotation.getYRot();

            this.setYHeadRot(yaw);
            this.setYBodyRot(yaw);
        }

        return rotation.getYRot();
    }

    @ModifyExpressionValue(method = {"sendPosition", "tick"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float silentRotationPitch(float original) {
        RotationPlan rotation = RotationManager.getInstance().getCurrentRotationPlan();
        if (rotation == null) {
            return original;
        }

        Rotation rot = RotationManager.getInstance().getRotation();

        return rot.getXRot();
    }

    @ModifyExpressionValue(method = "modifyInput(Lnet/minecraft/world/phys/Vec2;)Lnet/minecraft/world/phys/Vec2;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;itemUseSpeedMultiplier()F"))
    private float noSlowItemUseSpeed(float original) {
        if ((Object) this != SharedClass.player()) return original;
        return tokyo.ghoul.client.features.modules.movement.NoSlowModule.getInstance().useSpeedMultiplier(original);
    }

    @Inject(method = "sendPosition", at = @At(value = "HEAD"), cancellable = true)
    private void preMotion(CallbackInfo ci) {
        MotionEvent.MotionEventData event = new MotionEvent.MotionEventData(getX(), getY(), getZ(), getViewYRot(1), getViewXRot(1), onGround());
        if (MotionEvent.getInstance().call(event)) ci.cancel();
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
    private boolean sprintEventTick(boolean original) {
        return sprintHook(original);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;shouldStopRunSprinting()Z"))
    private boolean sprintEventStop(boolean original) {
        return original || !sprintHook(true);
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
    private boolean sprintEventInput(boolean original) {
        return sprintHook(original);
    }

    @Inject(method = "closeContainer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"), cancellable = true)
    private void onCloseHandledScreen(CallbackInfo ci) {
        if (CloseScreenEvent.getInstance().call()) ci.cancel();
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        if (PushEvent.getInstance().call(new PushEvent.PushEventData(PushEvent.PushEventData.PushingSource.BLOCK))) {
            ci.cancel();
        }
    }

    @Unique
    private boolean sprintHook(boolean origin) {
        if (SprintManager.shouldFreezeMovementInput()) {
            return false;
        }
        return SprintEvent.getInstance().call(new SprintEvent.SprintEventData(new DirectionalInput(input)));
    }
}
