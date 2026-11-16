package tokyo.ghoul.api.utils.player;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.experimental.UtilityClass;
import net.minecraft.client.KeyMapping;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationPlan;

@UtilityClass
public class MoveUtil implements QuickImports {
    public KeyMapping[] getMovementKeys() {
        return new KeyMapping[]{
                mc.options.keySprint,
                mc.options.keyUp,
                mc.options.keyDown,
                mc.options.keyLeft,
                mc.options.keyRight,
                mc.options.keyJump
        };
    }

    public void updateMovementKeys() {
        for (KeyMapping movementKey : getMovementKeys()) {
            movementKey.setDown(InputConstants.isKeyDown(mc.getWindow(), movementKey.getDefaultKey().getValue()));
        }
    }

    public boolean w() {
        return mc.options.keyUp.isDown();
    }

    public boolean s() {
        return mc.options.keyDown.isDown();
    }

    public boolean a() {
        return mc.options.keyLeft.isDown();
    }

    public boolean d() {
        return mc.options.keyRight.isDown();
    }

    public boolean isMoving() {
        return mc.player.zza != 0f || mc.player.xxa != 0f;
    }

    public double direction(float rotationYaw, final float moveForward, final float moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    public double[] forward(double speed) {
        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();
        float forward = mc.player.input.getMoveVector().y;
        float strafe = mc.player.input.getMoveVector().x;
        float yaw = currentRotationPlan == null ? mc.player.getYRot() : rotation.getYRot();
        if (forward != 0.0f) {
            if (strafe > 0.0f) {
                yaw += ((forward > 0.0f) ? -45 : 45);
            } else if (strafe < 0.0f) {
                yaw += ((forward > 0.0f) ? 45 : -45);
            }
            strafe = 0.0f;
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        double cosStrafe = Math.sin(Math.toRadians(yaw + 90.0f));
        double sinStrafe = Math.cos(Math.toRadians(yaw + 90.0f));
        double x = forward * speed * sinStrafe + strafe * speed * cosStrafe;
        double z = forward * speed * cosStrafe - strafe * speed * sinStrafe;
        return new double[]{x, z};
    }

    public void setSpeed(double speed) {
        double[] forward = forward(speed);

        mc.player.setDeltaMovement(forward[0], mc.player.getDeltaMovement().y, forward[1]);
    }
}
