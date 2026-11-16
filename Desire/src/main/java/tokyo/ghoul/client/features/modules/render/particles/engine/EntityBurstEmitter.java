package tokyo.ghoul.client.features.modules.render.particles.engine;

import net.minecraft.world.entity.Entity;

import java.util.concurrent.ThreadLocalRandom;

public class EntityBurstEmitter extends EntityParticleEmitter {

    public EntityBurstEmitter(Entity entity, int duration) {
        super(entity, duration);
    }

    @Override
    protected void spawn(ClientParticleManager manager, double x, double y, double z,
                         double motionX, double motionY, double motionZ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int lifetime = 80 + random.nextInt(40);
        float size = random.nextFloat(0.2F, 0.25F);

        manager.spawn(new ClientParticle.Builder(x, y, z)
                        .motion(motionX, motionY, motionZ)
                        .gravity(0.225F)
                        .drag(0.75F)
                        .lifetime(lifetime)
                        .collide(true)
                        .size(size, size),
                totemColor(random), size, Math.max(1, lifetime / 2));
    }

    private static int totemColor(ThreadLocalRandom random) {
        float r, g, b;
        if (random.nextInt(4) == 0) {
            r = 0.6F + random.nextFloat() * 0.2F;
            g = 0.6F + random.nextFloat() * 0.3F;
            b = random.nextFloat() * 0.2F;
        } else {
            r = 0.1F + random.nextFloat() * 0.2F;
            g = 0.4F + random.nextFloat() * 0.3F;
            b = random.nextFloat() * 0.2F;
        }

        int red = Math.min(255, Math.max(0, (int) (r * 255.0F)));
        int green = Math.min(255, Math.max(0, (int) (g * 255.0F)));
        int blue = Math.min(255, Math.max(0, (int) (b * 255.0F)));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
