package tokyo.ghoul.client.features.modules.render.particles.engine;

import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class ClientParticle implements QuickImports {
    private static final AABB EMPTY_BOX = new AABB(0, 0, 0, 0, 0, 0);
    private static final double MAX_COLLIDE_MOTION_SQ = 100.0 * 100.0;
    private static final BlockPos.MutableBlockPos CHECK_POS = new BlockPos.MutableBlockPos();

    private double prevX, prevY, prevZ;
    private double x, y, z;
    private double motionX, motionY, motionZ;
    private AABB box = EMPTY_BOX;

    private boolean onGround;
    private boolean collide = true;
    private boolean resting;
    private boolean dead;
    private boolean rolling;
    private boolean groundBoost;

    private float width = 0.6F, height = 1.8F;
    private int age, lifetime, delay;

    private float gravity;
    private float drag = 0.98F;
    private float bounce;
    private float bounceLeft;
    private double peakY;

    private float angle, prevAngle, spin;
    private float scaleStart = 1.0F, scaleMid = 1.0F, scaleEnd = 1.0F, scaleMidPoint = 0.2F;

    private ParticleCollisionMode collisionMode = ParticleCollisionMode.SWEPT;
    private ParticleInterpolationMode interpolationMode = ParticleInterpolationMode.SMOOTH;
    private double maxDistanceSq;

    public ClientParticle(Builder builder) {
        Objects.requireNonNull(builder, "builder");
        this.collide = builder.collide;
        this.gravity = builder.gravity;
        this.drag = builder.drag;
        this.bounce = Mth.clamp(builder.bounce, 0.0F, 1.95F);
        this.bounceLeft = this.bounce;
        this.peakY = builder.y;
        this.groundBoost = builder.groundBoost;
        this.spin = builder.spin;
        this.scaleStart = builder.scaleStart;
        this.scaleMid = builder.scaleMid;
        this.scaleEnd = builder.scaleEnd;
        this.scaleMidPoint = builder.scaleMidPoint;
        this.delay = Math.max(0, builder.delay);
        this.collisionMode = builder.collisionMode;
        this.interpolationMode = builder.interpolationMode;
        this.maxDistanceSq = builder.maxDistance <= 0.0 ? 0.0 : builder.maxDistance * builder.maxDistance;

        setSize(builder.width, builder.height);
        setPos(builder.x, builder.y, builder.z);
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;

        if (builder.randomizeMotion) {
            randomizeMotion(builder.motionX, builder.motionY, builder.motionZ);
        } else {
            this.motionX = builder.motionX;
            this.motionY = builder.motionY;
            this.motionZ = builder.motionZ;
        }

        this.lifetime = builder.lifetime <= 0 ? randomLifetime() : builder.lifetime;
    }

    public boolean isAlive() {
        return !dead;
    }

    public void kill() {
        dead = true;
    }

    public void tick() {
        try {
            update();
        } catch (Throwable throwable) {
            kill();
        }
    }

    private void update() {
        if (dead) return;

        prevX = x;
        prevY = y;
        prevZ = z;
        prevAngle = angle;
        angle += spin;

        int currentAge = age++;
        if (currentAge >= lifetime) {
            kill();
            return;
        }
        if (currentAge < delay) return;

        if (resting) {
            stop();
            return;
        }

        switch (collisionMode) {
            case SWEPT -> sweptStep();
            case RICOCHET -> ricochetStep();
        }
    }


    private void sweptStep() {
        if (rolling) {
            rollStep();
            return;
        }

        motionY -= 0.04 * gravity;
        move(motionX, motionY, motionZ);
        if (!onGround && y > peakY) peakY = y;

        if (groundBoost && y == prevY) {
            motionX *= 1.1;
            motionZ *= 1.1;
        }

        motionX *= drag;
        motionY *= drag;
        motionZ *= drag;

        if (onGround) {
            motionX *= 0.7;
            motionZ *= 0.7;
        }
    }

    private void rollStep() {
        motionY = -0.04 * gravity;
        move(motionX, motionY, motionZ);
        if (onGround) motionY = 0.0;

        motionX *= 0.986F;
        motionZ *= 0.986F;
        if (motionX * motionX + motionZ * motionZ < 4.0E-5) {
            resting = true;
            stop();
        }
    }

    private void ricochetStep() {
        reflectOffBlocks();
        motionX *= 0.999F;
        motionY *= 0.999F;
        motionZ *= 0.999F;
        setPos(x + motionX, y + motionY, z + motionZ);
        motionX *= 0.9998F;
        motionY -= 0.015F * gravity;
        motionZ *= 0.9998F;
    }

    private void reflectOffBlocks() {
        if (!collide) return;
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) return;
        if (motionX == 0.0 && motionY == 0.0 && motionZ == 0.0) return;

        Vec3 from = new Vec3(x, y, z);
        Vec3 to = from.add(motionX, motionY, motionZ);
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() != HitResult.Type.BLOCK) return;

        Vec3i normal = hit.getDirection().getUnitVec3i();
        double dot = 2.0 * (motionX * normal.getX() + motionY * normal.getY() + motionZ * normal.getZ());
        motionX -= normal.getX() * dot;
        motionY -= normal.getY() * dot;
        motionZ -= normal.getZ() * dot;
    }

    public void move(double dx, double dy, double dz) {
        if (resting) return;

        double wantX = dx, wantY = dy, wantZ = dz;
        ClientLevel level = mc.level;

        if (collide && level != null && (dx != 0.0 || dy != 0.0 || dz != 0.0)
                && dx * dx + dy * dy + dz * dz < MAX_COLLIDE_MOTION_SQ) {
            Vec3 motion = new Vec3(dx, dy, dz);
            if (!isPathClear(level, box, motion)) {
                Vec3 collided = Entity.collideBoundingBox((Entity) null, motion, box, level, Collections.emptyList());
                dx = collided.x;
                dy = collided.y;
                dz = collided.z;
            }
        }

        if (dx != 0.0 || dy != 0.0 || dz != 0.0) {
            box = box.move(dx, dy, dz);
            syncPosFromBox();
        }

        boolean hitVertical = wantY != dy;
        boolean hitHorizontal = wantX != dx || wantZ != dz;
        boolean hitCeiling = hitVertical && wantY > 0.0;
        onGround = hitVertical && wantY < 0.0;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        if (wantX != dx) {
            if (bounceLeft > 0.0F) {
                motionX = -wantX * bounceLeft;
                bounceLeft *= 0.9F;
                double scatter = Math.abs(wantX) * 0.1F;
                motionZ += (random.nextDouble() * 2.0 - 1.0) * scatter;
                motionY += random.nextDouble() * scatter * 0.5;
            } else {
                motionX = 0.0;
            }
        }

        if (wantZ != dz) {
            if (bounceLeft > 0.0F) {
                motionZ = -wantZ * bounceLeft;
                bounceLeft *= 0.9F;
                double scatter = Math.abs(wantZ) * 0.1F;
                motionX += (random.nextDouble() * 2.0 - 1.0) * scatter;
                motionY += random.nextDouble() * scatter * 0.5;
            } else {
                motionZ = 0.0;
            }
        }

        if (hitVertical) {
            if (bounceLeft <= 0.0F) {
                motionY = 0.0;
            } else if (onGround) {
                double fallHeight = Math.max(0.0, peakY - y);
                double bounceSpeed = Math.sqrt(Math.max(fallHeight, 0.01)) * 0.2 * bounceLeft;
                bounceLeft *= 0.9F;
                peakY = y;

                if (bounceSpeed > 0.055) {
                    motionY = bounceSpeed;
                    double scatter = bounceSpeed * 0.1F;
                    motionX += (random.nextDouble() * 2.0 - 1.0) * scatter;
                    motionZ += (random.nextDouble() * 2.0 - 1.0) * scatter;
                } else {
                    motionY = 0.0;
                    if (bounceSpeed > 0.01) {
                        double direction = random.nextDouble() * Math.PI * 2.0;
                        motionX += Math.cos(direction) * bounceSpeed * 0.5;
                        motionZ += Math.sin(direction) * bounceSpeed * 0.5;
                    }
                    rolling = true;
                }
            } else if (hitCeiling) {
                motionY = -Math.abs(wantY) * bounceLeft;
                bounceLeft *= 0.9F;
            }
        }

        if (!rolling) {
            double speedSq = motionX * motionX + motionY * motionY + motionZ * motionZ;
            if (onGround && !hitHorizontal && Math.abs(wantY) >= 1.0E-5 && Math.abs(dy) < 1.0E-5 && speedSq <= 2.25E-4) {
                resting = true;
                stop();
            } else if (speedSq > 2.25E-4) {
                resting = false;
            }
        }
    }

    private static boolean isPathClear(Level level, AABB box, Vec3 motion) {
        int minX = Mth.floor(box.minX + Math.min(motion.x, 0.0) - 1.0E-7) - 1;
        int maxX = Mth.floor(box.maxX + Math.max(motion.x, 0.0) + 1.0E-7) + 1;
        int minY = Mth.floor(box.minY + Math.min(motion.y, 0.0) - 1.0E-7) - 1;
        int maxY = Mth.floor(box.maxY + Math.max(motion.y, 0.0) + 1.0E-7) + 1;
        int minZ = Mth.floor(box.minZ + Math.min(motion.z, 0.0) - 1.0E-7) - 1;
        int maxZ = Mth.floor(box.maxZ + Math.max(motion.z, 0.0) + 1.0E-7) + 1;

        for (int bx = minX; bx <= maxX; bx++) {
            int edgeX = bx == minX || bx == maxX ? 1 : 0;
            for (int by = minY; by <= maxY; by++) {
                int edgeY = edgeX + (by == minY || by == maxY ? 1 : 0);
                for (int bz = minZ; bz <= maxZ; bz++) {
                    int edges = edgeY + (bz == minZ || bz == maxZ ? 1 : 0);
                    if (edges == 3) continue;

                    BlockState state = level.getBlockState(CHECK_POS.set(bx, by, bz));
                    boolean blocking = switch (edges) {
                        case 0 -> !state.isAir();
                        case 1 -> state.hasLargeCollisionShape();
                        default -> state.is(Blocks.MOVING_PISTON);
                    };
                    if (blocking) return false;
                }
            }
        }
        return true;
    }

    private void randomizeMotion(double baseX, double baseY, double baseZ) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        motionX = baseX + (random.nextDouble() * 2.0 - 1.0) * 0.4;
        motionY = baseY + (random.nextDouble() * 2.0 - 1.0) * 0.4;
        motionZ = baseZ + (random.nextDouble() * 2.0 - 1.0) * 0.4;

        double speed = (random.nextDouble() + random.nextDouble() + 1.0) * 0.15;
        double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (length >= 1.0E-8) {
            motionX = motionX / length * speed * 0.4;
            motionY = motionY / length * speed * 0.4 + 0.1;
            motionZ = motionZ / length * speed * 0.4;
        }
    }

    private static int randomLifetime() {
        return (int) (4.0F / (ThreadLocalRandom.current().nextFloat() * 0.9F + 0.1F));
    }

    private void stop() {
        motionX = 0.0;
        motionY = 0.0;
        motionZ = 0.0;
    }


    public void setPos(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        float half = width / 2.0F;
        box = new AABB(x - half, y, z - half, x + half, y + height, z + half);
    }

    private void setSize(float width, float height) {
        if (width == this.width && height == this.height) return;
        this.width = width;
        this.height = height;
        double minX = (box.minX + box.maxX - width) / 2.0;
        double minZ = (box.minZ + box.maxZ - width) / 2.0;
        box = new AABB(minX, box.minY, minZ, minX + width, box.minY + height, minZ + width);
    }

    private void syncPosFromBox() {
        x = (box.minX + box.maxX) / 2.0;
        y = box.minY;
        z = (box.minZ + box.maxZ) / 2.0;
    }


    public double renderX(float partialTicks) {
        return prevX + (x - prevX) * partialTicks;
    }

    public double renderY(float partialTicks) {
        return prevY + (y - prevY) * partialTicks;
    }

    public double renderZ(float partialTicks) {
        return prevZ + (z - prevZ) * partialTicks;
    }

    public float renderAngle(float partialTicks) {
        return prevAngle + (angle - prevAngle) * partialTicks;
    }

    public float renderScale(float partialTicks) {
        int life = Math.max(1, lifetime);
        float progress = Math.min(1.0F, Math.max(0.0F, age - 1 + partialTicks) / life);

        if (progress < scaleMidPoint) {
            float t = progress / Math.max(1.0E-4F, scaleMidPoint);
            return scaleStart + (scaleMid - scaleStart) * smoothStep(t);
        }
        float t = (progress - scaleMidPoint) / Math.max(1.0E-4F, 1.0F - scaleMidPoint);
        return scaleMid + (scaleEnd - scaleMid) * smoothStep(t);
    }

    private static float smoothStep(float value) {
        float t = Math.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }


    public static class Builder {
        private final double x, y, z;
        private double motionX, motionY, motionZ;
        private int lifetime;
        private float gravity;
        private float drag = 0.98F;
        private float bounce;
        private boolean collide = true;
        private boolean groundBoost;
        private float width = 0.2F, height = 0.2F;
        private int delay;
        private boolean randomizeMotion;
        private float spin;
        private float scaleStart = 1.0F, scaleMid = 1.0F, scaleEnd = 1.0F, scaleMidPoint = 0.2F;
        private ParticleCollisionMode collisionMode = ParticleCollisionMode.SWEPT;
        private ParticleInterpolationMode interpolationMode = ParticleInterpolationMode.SMOOTH;
        private double maxDistance;

        public Builder(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Builder motion(double x, double y, double z) {
            this.motionX = x;
            this.motionY = y;
            this.motionZ = z;
            return this;
        }

        public Builder lifetime(int ticks) {
            this.lifetime = ticks;
            return this;
        }

        public Builder gravity(float gravity) {
            this.gravity = gravity;
            return this;
        }

        public Builder drag(float drag) {
            this.drag = drag;
            return this;
        }

        public Builder bounce(float bounce) {
            this.bounce = Mth.clamp(bounce, 0.0F, 1.95F);
            return this;
        }

        public Builder collide(boolean collide) {
            this.collide = collide;
            return this;
        }

        public Builder groundBoost(boolean groundBoost) {
            this.groundBoost = groundBoost;
            return this;
        }

        public Builder size(float width, float height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder delay(int ticks) {
            this.delay = Math.max(0, ticks);
            return this;
        }

        public Builder randomizeMotion(boolean randomize) {
            this.randomizeMotion = randomize;
            return this;
        }

        public Builder spin(float spin) {
            this.spin = spin;
            return this;
        }

        public Builder scale(float start, float mid, float end, float midPoint) {
            this.scaleStart = start;
            this.scaleMid = mid;
            this.scaleEnd = end;
            this.scaleMidPoint = midPoint;
            return this;
        }

        public Builder collisionMode(ParticleCollisionMode mode) {
            this.collisionMode = mode == null ? ParticleCollisionMode.SWEPT : mode;
            return this;
        }

        public Builder interpolation(ParticleInterpolationMode mode) {
            this.interpolationMode = mode == null ? ParticleInterpolationMode.SMOOTH : mode;
            return this;
        }

        public Builder maxDistance(double distance) {
            this.maxDistance = distance;
            return this;
        }
    }
}
