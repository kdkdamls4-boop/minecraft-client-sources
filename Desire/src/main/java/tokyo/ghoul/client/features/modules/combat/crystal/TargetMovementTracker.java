package tokyo.ghoul.client.features.modules.combat.crystal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class TargetMovementTracker {
    private static final int SIZE = 10;

    private final Vec3[] positions = new Vec3[SIZE];
    private Entity target;
    private int head;
    private int count;

    public void track(Entity entity) {
        if (entity != target) {
            target = entity;
            head = 0;
            count = 0;
        }
        positions[head] = entity.position();
        head = (head + 1) % SIZE;
        if (count < SIZE) count++;
    }

    public boolean isTracking(Entity entity) {
        return entity == target;
    }

    public float speed() {
        if (count < 2) return 0.0F;
        Vec3 newest = positions[(head - 1 + SIZE) % SIZE];
        Vec3 oldest = positions[(head - count + SIZE) % SIZE];
        return (float) (newest.distanceTo(oldest) / (count - 1));
    }

    public void reset() {
        target = null;
        head = 0;
        count = 0;
    }
}
