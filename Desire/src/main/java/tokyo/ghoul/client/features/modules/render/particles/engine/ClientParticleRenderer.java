package tokyo.ghoul.client.features.modules.render.particles.engine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.RenderUtil;

import java.util.List;

public final class ClientParticleRenderer implements QuickImports {
    private final Vector3f right = new Vector3f();
    private final Vector3f up = new Vector3f();
    private final Quaternionf cameraRotation = new Quaternionf();

    public void render(PoseStack matrices, float partialTicks, List<Identifier> textures, boolean glow) {
        ClientParticleManager manager = ClientParticleManager.getInstance();
        if (manager.isEmpty() || textures.isEmpty()) return;

        manager.updateVisibility();

        Camera camera = mc.gameRenderer.mainCamera();
        Vec3 cam = camera.position();
        cameraRotation.set(camera.rotation());
        Vector3f baseRight = cameraRotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
        Vector3f baseUp = cameraRotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));

        RenderUtil.WORLD.startRender(matrices);
        Matrix4f pose = matrices.last().pose();

        VertexConsumer[] buffers = new VertexConsumer[textures.size()];

        for (ClientParticleManager.Entry entry : manager.entries()) {
            if (!entry.isVisible()) continue;
            ClientParticle particle = entry.getParticle();
            if (!particle.isAlive()) continue;

            float alpha = entry.alpha(partialTicks);
            if (alpha <= 0.001F) continue;
            float scale = particle.renderScale(partialTicks);
            if (scale <= 0.001F) continue;

            float angle = particle.getSpin() != 0.0F ? particle.renderAngle(partialTicks) : 0.0F;
            if (angle != 0.0F) {
                float cos = (float) Math.cos(angle);
                float sin = (float) Math.sin(angle);
                right.set(baseRight).mul(cos).add(baseUp.x * sin, baseUp.y * sin, baseUp.z * sin);
                up.set(baseUp).mul(cos).sub(baseRight.x * sin, baseRight.y * sin, baseRight.z * sin);
            } else {
                right.set(baseRight);
                up.set(baseUp);
            }

            int slot = Math.min(textures.size() - 1, (int) (entry.getTextureSeed() * textures.size()));
            if (buffers[slot] == null) buffers[slot] = RenderUtil.WORLD.textured(textures.get(slot));
            VertexConsumer buffer = buffers[slot];

            float half = entry.getSize() * scale;
            float cx = (float) (particle.renderX(partialTicks) - cam.x) + up.x * half * 0.5F;
            float cy = (float) (particle.renderY(partialTicks) - cam.y) + up.y * half * 0.5F;
            float cz = (float) (particle.renderZ(partialTicks) - cam.z) + up.z * half * 0.5F;

            float fade = alpha * 0.75F;
            int color = entry.getColor();
            quad(buffer, pose, cx, cy, cz, half, withAlpha(color, (int) (255.0F * fade * alphaOf(color))));

            if (glow) {
                quad(buffer, pose, cx, cy, cz, half * 2.0F, withAlpha(color, (int) (64.0F * fade * alphaOf(color))));
                quad(buffer, pose, cx, cy, cz, half * 4.0F, withAlpha(color, (int) (32.0F * fade * alphaOf(color))));
            }
        }

        RenderUtil.WORLD.endRender(matrices);
    }

    private void quad(VertexConsumer buffer, Matrix4f pose, float cx, float cy, float cz, float half, int color) {
        float rx = right.x * half, ry = right.y * half, rz = right.z * half;
        float ux = up.x * half, uy = up.y * half, uz = up.z * half;

        buffer.addVertex(pose, cx - rx - ux, cy - ry - uy, cz - rz - uz).setUv(0.0F, 1.0F).setColor(color);
        buffer.addVertex(pose, cx + rx - ux, cy + ry - uy, cz + rz - uz).setUv(1.0F, 1.0F).setColor(color);
        buffer.addVertex(pose, cx + rx + ux, cy + ry + uy, cz + rz + uz).setUv(1.0F, 0.0F).setColor(color);
        buffer.addVertex(pose, cx - rx + ux, cy - ry + uy, cz - rz + uz).setUv(0.0F, 0.0F).setColor(color);
    }

    private static float alphaOf(int color) {
        return (color >>> 24 & 0xFF) / 255.0F;
    }

    private static int withAlpha(int color, int alpha) {
        return Math.clamp(alpha, 0, 255) << 24 | color & 0xFFFFFF;
    }
}
