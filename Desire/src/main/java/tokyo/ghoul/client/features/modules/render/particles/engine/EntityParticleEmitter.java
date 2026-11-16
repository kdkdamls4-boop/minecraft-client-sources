package tokyo.ghoul.client.features.modules.render.particles.engine;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public abstract class EntityParticleEmitter extends ParticleEmitter {
    protected final Entity entity;
    private final int duration;
    private final int perTick;
    private int ticks;

    protected EntityParticleEmitter(Entity entity, int duration, int perTick) {
        this.entity = entity;
        this.duration = Math.max(1, duration);
        this.perTick = Math.max(1, perTick);
    }

    protected EntityParticleEmitter(Entity entity, int duration) {
        this(entity, duration, 16);
    }

    @Override
    protected void emit(ClientParticleManager manager) {
        if (entity == null || entity.level() == null) {
            finish();
            return;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < perTick; i++) {
            double dx = random.nextDouble() * 2.0 - 1.0;
            double dy = random.nextDouble() * 2.0 - 1.0;
            double dz = random.nextDouble() * 2.0 - 1.0;
            if (dx * dx + dy * dy + dz * dz > 1.0) continue;

            spawn(manager,
                    entity.getX(dx / 4.0), entity.getY(0.5 + dy / 4.0), entity.getZ(dz / 4.0),
                    dx, dy + 0.2, dz);
        }

        if (++ticks >= duration) finish();
    }

    protected abstract void spawn(ClientParticleManager manager, double x, double y, double z,
                                  double motionX, double motionY, double motionZ);
}
