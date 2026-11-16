package tokyo.ghoul.client.features.modules.render.jumpeffect;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.move.JumpEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.post.PostFx;
import tokyo.ghoul.api.utils.render.post.PostPipelines;
import tokyo.ghoul.api.utils.render.post.PostTarget;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Jump Effect", description = "Эффект при прыжке", category = Category.RENDER)
public class JumpEffectModule extends Module {
    @Getter private static final JumpEffectModule instance = new JumpEffectModule();
    private static final int MAX_RIPPLES = 8;
    private static final int LIFE_TICKS = 22;
    private static final int UNIFORM_SIZE = new Std140SizeCalculator()
            .putMat4f().putMat4f().putVec4().putVec4().putVec4().putVec4()
            .putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4()
            .putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4().putVec4()
            .get();

    public final SliderSetting radius = new SliderSetting("Radius").value(2f).range(1f, 5f).step(0.1f);
    public final SliderSetting amplitude = new SliderSetting("Amplitude").value(1f).range(0.1f, 3f).step(0.05f);
    public final SliderSetting aberration = new SliderSetting("Aberration").value(20f).range(0f, 40f).step(1f);

    private final List<Ripple> ripples = new ArrayList<>();
    private GpuBuffer uniforms;
    private PostTarget sceneCopy;
    private boolean groundKnown;
    private boolean wasOnGround;

    public JumpEffectModule() {
        addSettings(radius, amplitude, aberration);
    }

    @Override
    public void onDisable() {
        ripples.clear();
        groundKnown = false;
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener jump = JumpEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            addRipple(mc.player.position());
        }));
        EventListener tick = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player != null && mc.level != null) {
                boolean onGround = mc.player.onGround();
                if (groundKnown && wasOnGround && !onGround && mc.player.getDeltaMovement().y > 0.0 && !recentRipple()) {
                    addRipple(mc.player.position());
                }
                wasOnGround = onGround;
                groundKnown = true;
            } else {
                groundKnown = false;
            }
            ripples.forEach(Ripple::tick);
            ripples.removeIf(r -> r.age >= LIFE_TICKS);
        }));
        addEvents(jump, tick);
    }

    public void render(CameraRenderState camera, float partial) {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (ripples.isEmpty() || target == null || target.getColorTexture() == null || target.getDepthTextureView() == null || camera == null || !camera.initialized) {
            return;
        }
        if (sceneCopy == null) sceneCopy = new PostTarget("static-jump-effect-scene", PostPipelines.EFFECT_FORMAT, false);
        TextureTarget scene = sceneCopy.ensure(target.width, target.height);
        if (scene == null || scene.getColorTexture() == null) return;
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(target.getColorTexture(), scene.getColorTexture(), 0, 0, 0, 0, 0, target.width, target.height);
        write(camera, partial);
        PostFx.pass("Static Jump Effect", PostPipelines.JUMP_EFFECT, target, pass -> {
            pass.setUniform("JumpData", uniforms);
            pass.bindTexture("SceneSampler", scene.getColorTextureView(), PostFx.linearSampler());
            pass.bindTexture("DepthSampler", target.getDepthTextureView(), PostFx.nearestSampler());
        });
    }

    private void write(CameraRenderState camera, float partial) {
        if (uniforms == null) uniforms = PostFx.createUniforms("Static Jump Effect UBO", UNIFORM_SIZE);
        Matrix4f mvp = new Matrix4f(camera.projectionMatrix).mul(camera.viewRotationMatrix);
        Matrix4f inverse = new Matrix4f(mvp).invert();
        Vec3 cp = new Vec3(camera.pos.x(), camera.pos.y(), camera.pos.z());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Std140Builder b = Std140Builder.onStack(stack, UNIFORM_SIZE).putMat4f(inverse).putMat4f(mvp)
                    .putVec4(0.38f, amplitude.getValue(),
                            RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F,
                            aberration.getValue());
            float[] c1 = color(UIColors.primary()), c2 = color(UIColors.secondary());
            b.putVec4(c1[0], c1[1], c1[2], c1[3]).putVec4(c2[0], c2[1], c2[2], c2[3]);
            float t = PostFx.shaderTime();
            b.putVec4((float) Math.sin(t), (float) Math.cos(t), 0, 0);
            for (int i = 0; i < MAX_RIPPLES; i++) {
                Ripple r = i < ripples.size() ? ripples.get(i) : null;
                if (r == null) b.putVec4(0, 0, 0, 0);
                else {
                    float p = r.progress(partial);
                    Vec3 v = r.origin.subtract(cp);
                    b.putVec4((float) v.x, (float) v.y, (float) v.z, radius.getValue() * (1 - (float) Math.pow(1 - p, 3)));
                }
            }
            for (int i = 0; i < MAX_RIPPLES; i++) {
                Ripple r = i < ripples.size() ? ripples.get(i) : null;
                b.putVec4(r == null ? 0 : 1 - r.progress(partial), 0, 0, 0);
            }
            ByteBuffer data = b.get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(uniforms.slice(), data);
        }
    }

    private boolean recentRipple() {
        return !ripples.isEmpty() && ripples.getLast().age <= 2;
    }

    private void addRipple(Vec3 position) {
        if (recentRipple()) return;
        if (ripples.size() == MAX_RIPPLES) ripples.removeFirst();
        ripples.add(new Ripple(position));
    }

    private static float[] color(Color c) {
        return new float[]{c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f};
    }

    public void release() {
        if (uniforms != null) {
            uniforms.close();
            uniforms = null;
        }
        if (sceneCopy != null) {
            sceneCopy.release();
            sceneCopy = null;
        }
    }

    private static final class Ripple {
        final Vec3 origin;
        int age, previous;

        Ripple(Vec3 origin) {
            this.origin = origin;
        }

        void tick() {
            previous = age++;
        }

        float progress(float p) {
            return Math.clamp((previous + (age - previous) * p) / (float) LIFE_TICKS, 0, 1);
        }
    }
}
