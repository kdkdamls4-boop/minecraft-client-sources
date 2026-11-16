package tokyo.ghoul.client.features.modules.combat.crystal;

import net.minecraft.util.Mth;
import org.joml.Vector2f;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.InstantRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;

public final class CrystalRotations implements QuickImports {
    public static final int ACTION = 62;
    public static final int FINISH_MINING = 65;

    private static int grantedTick = -1;
    private static int grantedPriority;

    private CrystalRotations() {}

    public static boolean request(Module owner, Vector2f rotation) {
        return request(owner, rotation, ACTION);
    }

    public static boolean request(Module owner, Vector2f rotation, int priority) {
        if (mc.player == null || rotation == null) return false;

        int tick = mc.player.tickCount;
        if (tick < grantedTick) reset();
        if (grantedTick == tick && priority <= grantedPriority) return false;

        grantedTick = tick;
        grantedPriority = priority;

        float yaw = rotation.x + MatrixPlacement.yawJitter(rotation.x);
        float pitch = Mth.clamp(rotation.y + MatrixPlacement.pitchJitter(), -90.0F, 90.0F);

        RotationStrategy strategy = new RotationStrategy(new InstantRotation(), true).ticksUntilReset(2);
        RotationManager.getInstance().addRotation(new Rotation(yaw, pitch), strategy, TaskPriority.HIGH, owner);
        RotationManager.getInstance().updateNow();
        return true;
    }

    public static void reset() {
        grantedTick = -1;
        grantedPriority = 0;
    }

    public static Rotation aim() {
        return RotationManager.getInstance().getRotation();
    }

    public static float aimYaw() {
        return aim().getYRot();
    }

    public static float aimPitch() {
        return aim().getXRot();
    }
}
