package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.features.modules.render.particles.ParticleRender;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@ModuleRegister(name = "Kill Effects", description = "Эффекты при убийстве", category = Category.RENDER)
public class KillEffectsModule extends Module {
    @Getter private static final KillEffectsModule instance = new KillEffectsModule();
    private static final float PARTICLE_HALF_SIZE = 0.05F;
    private static final int MAX_PARTICLES = 1200;
    private static final Random RANDOM = new Random();

    public final BooleanSetting gravity = new BooleanSetting("Gravity").value(true);
    public final ColorSetting color = new ColorSetting("Color").value(new Color(0xFF6496FF, true));

    private final List<Particle> particles = new ArrayList<>();
    private final Map<Integer, Boolean> aliveStates = new HashMap<>();

    public KillEffectsModule() {
        addSettings(gravity, color);
    }

    @Override
    public void onDisable() {
        particles.clear();
        aliveStates.clear();
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tick = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(event -> onRender(event)));
        addEvents(tick, render);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null) {
            particles.clear();
            aliveStates.clear();
            return;
        }
        long now = System.currentTimeMillis();
        Set<Integer> seen = new HashSet<>();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) continue;
            int id = entity.getId();
            seen.add(id);
            Boolean wasAlive = aliveStates.put(id, living.isAlive());
            if (Boolean.TRUE.equals(wasAlive) && !living.isAlive() && !living.isRemoved()) {
                spawnParticles(living, now);
            }
        }
        aliveStates.keySet().retainAll(seen);
    }

    private void onRender(Render3DEvent.Render3DEventData event) {
        if (mc.player == null || mc.level == null) return;
        long now = System.currentTimeMillis();
        Color base = color.getValue();
        PoseStack matrices = event.matrixStack();
        var texture = ParticleRender.getTexture("Glow");

        RenderUtil.WORLD.startRender(matrices);
        for (Particle particle : particles) {
            if (particle.isDead(now)) continue;
            particle.update(now);
            renderBillboard(matrices, particle, now, base, texture);
        }
        RenderUtil.WORLD.endRender(matrices);
        particles.removeIf(particle -> particle.isDead(now));
    }

    private void renderBillboard(PoseStack matrices, Particle particle, long now, Color base, net.minecraft.resources.Identifier texture) {
        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        float alpha = particle.alpha(now);
        Color col = ColorUtil.setAlpha(base, (int) (255 * 0.9F * alpha));
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer buffer = RenderUtil.WORLD.textured(texture);

        matrices.pushPose();
        matrices.translate(particle.x - cam.x, particle.y - cam.y, particle.z - cam.z);
        matrices.mulPose(Axis.YP.rotationDegrees(-camera.yRot()));
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        float half = PARTICLE_HALF_SIZE;
        buffer.addVertex(matrix, half, -half, 0f).setUv(0f, 1f).setColor(col.getRGB());
        buffer.addVertex(matrix, -half, -half, 0f).setUv(1f, 1f).setColor(col.getRGB());
        buffer.addVertex(matrix, -half, half, 0f).setUv(1f, 0f).setColor(col.getRGB());
        buffer.addVertex(matrix, half, half, 0f).setUv(0f, 0f).setColor(col.getRGB());
        matrices.popPose();
    }

    private void spawnParticles(LivingEntity entity, long now) {
        Vec3 pos = entity.position();
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        float yaw = (float) Math.toRadians(-entity.yRotO + 90.0F);
        boolean hasGravity = gravity.getValue();
        int count = Math.min(194, MAX_PARTICLES - particles.size());
        if (count <= 0) return;

        float hOffset = height - 0.2F;
        float spread = width * 0.4F;
        spawnSphere(pos.add(0.0D, hOffset, 0.0D), spread, count / 10, hasGravity, now);
        spawnBody(pos, width * 0.4F, height * 0.85F, width * 0.2F, yaw, count / 4, hasGravity, now);
        for (int i = -1; i <= 1; i += 2) {
            Vec3 armPos = new Vec3(Math.sin(yaw) * width * 0.5 * i, height * 0.75F, Math.cos(yaw) * width * 0.5 * i);
            spawnLimb(pos.add(armPos), width * 0.4F, width * 0.15F, count / 8, hasGravity, now);
            Vec3 legPos = new Vec3(Math.sin(yaw) * width * 0.15 * i, height * 0.4F, Math.cos(yaw) * width * 0.15 * i);
            spawnLimb(pos.add(legPos), height * 0.45F, width * 0.15F, count / 6, hasGravity, now);
        }
    }

    private void spawnSphere(Vec3 pos, float radius, int count, boolean hasGravity, long now) {
        for (int i = 0; i < count; ++i) {
            float angle1 = RANDOM.nextFloat() * (float) Math.PI * 2.0F;
            float angle2 = (float) Math.acos(2.0F * RANDOM.nextFloat() - 1.0F);
            float r = radius * (float) Math.cbrt(RANDOM.nextFloat());
            float px = r * (float) (Math.sin(angle2) * Math.cos(angle1));
            float py = r * (float) (Math.sin(angle2) * Math.sin(angle1));
            float pz = r * (float) Math.cos(angle2);
            float speedBase = hasGravity ? 0.02F : 0.008F;
            float vx = (RANDOM.nextFloat() - 0.5F) * speedBase;
            float vy = hasGravity ? 0.03F + RANDOM.nextFloat() * 0.04F : (RANDOM.nextFloat() - 0.5F) * 0.008F;
            float vz = (RANDOM.nextFloat() - 0.5F) * speedBase;
            particles.add(new Particle(pos, px, py, pz, vx, vy, vz, hasGravity, now));
        }
    }

    private void spawnBody(Vec3 pos, float radiusX, float height, float spread, float yaw, int count, boolean hasGravity, long now) {
        for (int i = 0; i < count; ++i) {
            float ox = (RANDOM.nextFloat() - 0.5F) * radiusX * 2.0F;
            float oy = RANDOM.nextFloat() * height;
            float oz = (RANDOM.nextFloat() - 0.5F) * spread * 2.0F;
            float px = (float) (ox * Math.cos(yaw) - oz * Math.sin(yaw));
            float pz = (float) (ox * Math.sin(yaw) + oz * Math.cos(yaw));
            float speedBase = hasGravity ? 0.025F : 0.01F;
            float vx = (RANDOM.nextFloat() - 0.5F) * speedBase;
            float vy = hasGravity ? 0.04F + RANDOM.nextFloat() * 0.05F : (RANDOM.nextFloat() - 0.5F) * 0.01F;
            float vz = (RANDOM.nextFloat() - 0.5F) * speedBase;
            particles.add(new Particle(pos, px, oy, pz, vx, vy, vz, hasGravity, now));
        }
    }

    private void spawnLimb(Vec3 pos, float height, float radius, int count, boolean hasGravity, long now) {
        for (int i = 0; i < count; ++i) {
            float oy = -RANDOM.nextFloat() * height;
            float ox = (RANDOM.nextFloat() - 0.5F) * radius * 2.0F;
            float oz = (RANDOM.nextFloat() - 0.5F) * radius * 2.0F;
            float speedBase = hasGravity ? 0.018F : 0.006F;
            float vx = (RANDOM.nextFloat() - 0.5F) * speedBase;
            float vy = hasGravity ? 0.025F + RANDOM.nextFloat() * 0.035F : (RANDOM.nextFloat() - 0.5F) * 0.006F;
            float vz = (RANDOM.nextFloat() - 0.5F) * speedBase;
            particles.add(new Particle(pos, ox, oy, oz, vx, vy, vz, hasGravity, now));
        }
    }

    private final class Particle {
        double x, y, z;
        float vx, vy, vz;
        final long startTime;
        long lastUpdate;
        final float gravityFactor;
        final long lifeTime;
        final boolean hasGravity;
        final float friction;

        Particle(Vec3 origin, float ox, float oy, float oz, float vx, float vy, float vz, boolean hasGravity, long now) {
            this.x = origin.x + ox;
            this.y = origin.y + oy;
            this.z = origin.z + oz;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.startTime = now;
            this.lastUpdate = now;
            this.hasGravity = hasGravity;
            this.gravityFactor = 0.005F + RANDOM.nextFloat() * 0.005F;
            this.lifeTime = 1508 + RANDOM.nextInt(1508);
            this.friction = hasGravity ? 0.999F : 0.995F;
        }

        void update(long now) {
            float bounceFriction = 0.5F;
            float delta = Math.min((float) (now - lastUpdate) / 16.67F, 5.0F);
            lastUpdate = now;
            if (hasGravity) vy -= gravityFactor * delta;
            float drag = (float) Math.pow(friction, delta);
            vx *= drag;
            vy *= drag;
            vz *= drag;
            double nextX = x + vx * delta;
            double nextY = y + vy * delta;
            double nextZ = z + vz * delta;
            if (hasGravity && mc.level != null) {
                if (!mc.level.getBlockState(BlockPos.containing(x, nextY - 0.05, z)).isAir()) {
                    vy = -vy * bounceFriction;
                    nextY = y;
                }
                if (!mc.level.getBlockState(BlockPos.containing(nextX, y, z)).isAir()) {
                    vx = -vx * bounceFriction;
                    nextX = x;
                }
                if (!mc.level.getBlockState(BlockPos.containing(x, y, nextZ)).isAir()) {
                    vz = -vz * bounceFriction;
                    nextZ = z;
                }
            }
            if (Math.abs(vy) <= 1.0E-4F) {
                vx = 0.0F;
                vz = 0.0F;
            }
            x = nextX;
            y = nextY;
            z = nextZ;
        }

        float alpha(long now) {
            return 1.0F - Math.clamp((float) (now - startTime) / lifeTime, 0.0F, 1.0F);
        }

        boolean isDead(long now) {
            return now - startTime > lifeTime;
        }
    }
}
