package tokyo.ghoul.inject.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.combat.SprintManager;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.api.utils.player.MoveUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationPlan;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

@Mixin(KeyboardInput.class)
public class MixinKeyboardInput extends MixinInput {
    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;"))
    private Input onTick(Input original) {
        MovementInputEvent.MovementInputEventData movementInputEvent = new MovementInputEvent.MovementInputEventData(original, original.jump(), original.shift(), new DirectionalInput(original));
        MovementInputEvent.getInstance().call(movementInputEvent);

        DirectionalInput untransformedDirectionalInput = movementInputEvent.getDirectionalInput();
        DirectionalInput directionalInput = transformDirection(untransformedDirectionalInput);

        SprintEvent.SprintEventData sprintEvent = new SprintEvent.SprintEventData(directionalInput);
        SprintEvent.getInstance().call(sprintEvent);

        boolean freezeInput = SprintManager.shouldFreezeMovementInput();
        if (freezeInput) {
            untransformedDirectionalInput = DirectionalInput.NONE;
            directionalInput = DirectionalInput.NONE;
            sprintEvent.setSprint(false);
        }

        this.untransformed = new Input(
                untransformedDirectionalInput.isForwards(),
                untransformedDirectionalInput.isBackwards(),
                untransformedDirectionalInput.isLeft(),
                untransformedDirectionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );

        if (freezeInput) {
            return new Input(
                    false, false, false, false,
                    movementInputEvent.isJump(),
                    movementInputEvent.isSneak(),
                    false
            );
        }

        return new Input(
                directionalInput.isForwards(),
                directionalInput.isBackwards(),
                directionalInput.isLeft(),
                directionalInput.isRight(),
                movementInputEvent.isJump(),
                movementInputEvent.isSneak(),
                sprintEvent.isSprint()
        );
    }

    @Unique
    private DirectionalInput transformDirection(DirectionalInput input) {
        LocalPlayer player = SharedClass.player();
        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getCurrentRotation();
        RotationPlan rotationPlan = rotationManager.getCurrentRotationPlan();

        if (rotationPlan == null || rotation == null || player == null || !rotationPlan.moveCorrection()) {
            return input;
        }

        float z = KeyboardInput.calculateImpulse(input.isForwards(), input.isBackwards());
        float x = KeyboardInput.calculateImpulse(input.isLeft(), input.isRight());

        float yaw = rotation.getYRot();
        float direction = player.getYRot();

        if (rotationPlan.provider() instanceof AuraModule && (z != 0 || x != 0)) {
            AuraModule auraModule = AuraModule.getInstance();

            Vec3 position = auraModule.target != null ? auraModule.target.position() : null;

            if (position == null) {
                return input;
            }

            double deltaX = position.x - player.getX();
            double deltaZ = position.z - player.getZ();

            double angleToTarget = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0;
            return correctDirection(z, x, (float)angleToTarget, yaw);
        } else if (rotationPlan.freeMoveCorrection() && (z != 0 || x != 0)) {
            return correctDirection(z, x, direction, yaw);
        }

        return input;
    }

    @Unique
    private DirectionalInput correctDirection(float forward, float strafe,
                                               float referenceYaw, float movementYaw) {
        double desiredAngle = Mth.wrapDegrees(Math.toDegrees(
                MoveUtil.direction(referenceYaw, forward, strafe)));
        float bestForward = 0f;
        float bestStrafe = 0f;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1f; predictedForward <= 1f; predictedForward += 1f) {
            for (float predictedStrafe = -1f; predictedStrafe <= 1f; predictedStrafe += 1f) {
                if (predictedForward == 0f && predictedStrafe == 0f) continue;
                double predictedAngle = Mth.wrapDegrees(Math.toDegrees(
                        MoveUtil.direction(movementYaw, predictedForward, predictedStrafe)));
                float difference = Math.abs(Mth.wrapDegrees((float)(desiredAngle - predictedAngle)));
                if (difference < closestDifference) {
                    closestDifference = difference;
                    bestForward = predictedForward;
                    bestStrafe = predictedStrafe;
                }
            }
        }
        return new DirectionalInput(bestForward, bestStrafe);
    }
}
