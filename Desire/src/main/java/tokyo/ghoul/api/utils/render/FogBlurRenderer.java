package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import tokyo.ghoul.api.utils.render.post.KawaseBlur;
import tokyo.ghoul.api.utils.render.post.PostFx;
import tokyo.ghoul.api.utils.render.post.PostPipelines;
import tokyo.ghoul.api.utils.render.post.PostTarget;
import tokyo.ghoul.client.features.modules.render.AmbienceModule;

public final class FogBlurRenderer {
    private static final int UNIFORM_SIZE = new Std140SizeCalculator().putMat4f().putVec4().get();
    private static final int KAWASE_SIZE = new Std140SizeCalculator().putVec4().get();
    private static final FogBlurRenderer INSTANCE = new FogBlurRenderer();

    private final KawaseBlur blur = new KawaseBlur("desire-fog-blur",
            PostPipelines.HAND_BLUR_DOWN, PostPipelines.HAND_BLUR_UP,
            "HandBlurUniforms", "HandBlurUniforms", KAWASE_SIZE,
            (builder, width, height, offset, alpha) -> builder.putVec4(1f / width, 1f / height, offset, alpha));
    private PostTarget sceneCopy;
    private GpuBuffer uniforms;

    private FogBlurRenderer() {
    }

    public static FogBlurRenderer getInstance() {
        return INSTANCE;
    }

    public void render(AmbienceModule ambience, CameraRenderState camera) {
        Minecraft mc = Minecraft.getInstance();
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (camera == null || !camera.initialized || camera.projectionMatrix == null
                || target == null || target.width <= 1 || target.height <= 1
                || target.getColorTexture() == null || target.getColorTextureView() == null
                || target.getDepthTextureView() == null) {
            return;
        }

        if (sceneCopy == null) sceneCopy = new PostTarget("desire-fog-blur-scene", PostPipelines.EFFECT_FORMAT, false);
        TextureTarget scene = sceneCopy.ensure(target.width, target.height);
        if (scene == null || scene.getColorTexture() == null) return;
        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                target.getColorTexture(), scene.getColorTexture(),
                0, 0, 0, 0, 0, target.width, target.height
        );

        float strength = ambience.fogBlurStrength();
        int levels = Math.max(2, Math.min(5, 1 + Math.round(strength / 2.5f)));
        float offset = 0.8f + strength * 0.22f;
        GpuTextureView blurred = blur.run(scene.getColorTextureView(), target.width, target.height,
                levels, offset, PostFx.linearSampler());

        float start = Math.max(0f, ambience.fogStart());
        float end = ambience.fogEnd();
        float full = start + Math.max(1f, end - start) * 0.6f;
        float amount = Math.min(1f, 0.55f + strength * 0.045f);
        float reverseZ = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1f : 0f;
        Matrix4f inverseProjection = new Matrix4f(camera.projectionMatrix).invert();

        if (uniforms == null) uniforms = PostFx.createUniforms("Desire Fog Blur UBO", UNIFORM_SIZE);
        PostFx.writeUniforms(uniforms, UNIFORM_SIZE, builder -> builder
                .putMat4f(inverseProjection)
                .putVec4(start, full, amount, reverseZ));

        PostFx.pass("Desire Fog Blur", PostPipelines.FOG_BLUR, target, pass -> {
            pass.setUniform("FogBlurUniforms", uniforms);
            pass.bindTexture("SceneSampler", scene.getColorTextureView(), PostFx.linearSampler());
            pass.bindTexture("BlurSampler", blurred, PostFx.linearSampler());
            pass.bindTexture("DepthSampler", target.getDepthTextureView(), PostFx.nearestSampler());
        });
    }

    public void release() {
        if (sceneCopy != null) sceneCopy.release();
        sceneCopy = null;
        blur.release();
    }
}
