package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.player.MoveUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;

public class FunTimeRotation extends RotationMode {
    public static boolean attack;

    public static int attackedTicks = 0;
    public static int attackCount = 0;

    public static int idAttack = 0;

    public static long lastAttack = 0;
    public static float jitterUniquePower = 0f;

    public static boolean firstAttack = true;

    public FunTimeRotation() {
        super("FunTime");
    }

    public static void updateAttackState(boolean attack) {
        FunTimeRotation.attack = attack;

        if (attack) {
            attackCount++;
            if (firstAttack && attackCount >= 2) {
                firstAttack = false;
                attackCount = 0;
            }

            jitterUniquePower = MathUtil.randomInRange(5f, 15f);

            lastAttack = System.currentTimeMillis();

            idAttack = MathUtil.randomInRange(0, 1);

            attackedTicks = MathUtil.randomInRange(12, 16) * 3;
        }
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        if (attackedTicks > 0) attackedTicks = Math.max(attackedTicks - 1, 0);

        boolean intensiveAttack = System.currentTimeMillis() - lastAttack < 500;
        boolean lateAttack = System.currentTimeMillis() - lastAttack > 800;

        float prevPitch = targetRotation.getXRot();

        if (!MoveUtil.isMoving() && !attack && attackedTicks > 9 * 3) {
            float prevYaw = targetRotation.getYRot();

            float jitterPower = 45f + jitterUniquePower;
            float jitter = firstAttack ? 0f : idAttack == 0 ? -jitterPower : jitterPower;

            targetRotation = new Rotation(prevYaw + jitter, prevPitch);
        }

        float swing = (float) (Math.sin(mc.player.tickCount * 0.9f) * MathUtil.randomInRange(16.8f, 17.1f));

        float yawSpeed = 69f / 3f;
        float pitchSpeed = 15f / 3f;

        if (lateAttack) {
            firstAttack = true;
            targetRotation = new Rotation(mc.player.getYRot(), prevPitch);
            yawSpeed *= 0.2f;
        }

        Rotation delta = RotationUtil.calculateDelta(currentRotation, targetRotation);
        float yawDelta = delta.getYRot();
        float pitchDelta = delta.getXRot();

        boolean glitchDelta = !attack && intensiveAttack && MoveUtil.isMoving();

        float finalDeltaYaw = glitchDelta ? swing / 3f : yawDelta + swing;
        float finalDeltaPitch = glitchDelta ? 0f : pitchDelta;

        float finalYaw = currentRotation.getYRot() + Mth.clamp(finalDeltaYaw, -yawSpeed, yawSpeed);
        float finalPitch = currentRotation.getXRot() + Mth.clamp(finalDeltaPitch, -pitchSpeed, pitchSpeed);

        return new Rotation(
                finalYaw, finalPitch
        );
    }
}