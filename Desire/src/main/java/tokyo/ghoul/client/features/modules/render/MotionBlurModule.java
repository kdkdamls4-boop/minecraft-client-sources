package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.FramebufferResizeEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.render.pipeline.EvaMotionBlurUniform;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;

@Getter
@ModuleRegister(name = "Motion Blur", description = "Размытие в движении", category = Category.RENDER)
public class MotionBlurModule extends Module {
    @Getter private static final MotionBlurModule instance = new MotionBlurModule();

    private final SliderSetting blurStrength = new SliderSetting("Сила").value(1.5f).range(0.1f, 5.0f).step(0.05f);
    private final SliderSetting samples = new SliderSetting("Сэмплы").value(16f).range(4f, 32f).step(1f);
    private final ModeSetting algorithm = new ModeSetting("Режим").values("Centered", "Forward").value("Centered");

    private TextureTarget copyTarget;
    private Matrix4f prevModelView;
    private Matrix4f prevProjection;
    private Vector3f prevCameraPos;

    public MotionBlurModule() {
        addSettings(blurStrength, samples, algorithm);
    }

    @Override
    public void onEvent() {
        EventListener resize = FramebufferResizeEvent.getInstance().subscribe(new Listener<>(event -> resetBuffers()));
        addEvents(resize);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetState();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        resetState();
        resetBuffers();
        EvaRenderer.getInstance().endMotionBlurFrame();
    }

    private void resetState() {
        prevModelView = null;
        prevProjection = null;
        prevCameraPos = null;
    }

    private void resetBuffers() {
        if (copyTarget != null) {
            copyTarget.destroyBuffers();
            copyTarget = null;
        }
    }

    public void render() {
        if (!isEnabled() || mc.level == null || mc.player == null) return;

        if (mc.gui != null && mc.gui.screen() != null) {
            resetState();
            return;
        }

        CameraRenderState camera = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if (camera == null || !camera.initialized || camera.viewRotationMatrix == null || camera.projectionMatrix == null) {
            return;
        }

        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (main == null || main.width <= 1 || main.height <= 1
                || main.getColorTexture() == null || main.getColorTextureView() == null
                || main.getDepthTextureView() == null) {
            return;
        }

        if (copyTarget == null || copyTarget.width != main.width || copyTarget.height != main.height) {
            resetBuffers();
            copyTarget = new TextureTarget("Desire Motion Blur Scene", main.width, main.height, false,
                    main.getColorTexture().getFormat());
        }

        Matrix4f currentProj = new Matrix4f(camera.projectionMatrix);
        Matrix4f currentView = new Matrix4f(camera.viewRotationMatrix);
        Vector3f currentCamPos = new Vector3f((float) camera.pos.x(), (float) camera.pos.y(), (float) camera.pos.z());

        if (prevModelView == null || prevProjection == null || prevCameraPos == null) {
            prevModelView = new Matrix4f(currentView);
            prevProjection = new Matrix4f(currentProj);
            prevCameraPos = currentCamPos;
            return;
        }

        if (currentCamPos.distanceSquared(prevCameraPos) > 64f) {
            prevModelView = new Matrix4f(currentView);
            prevProjection = new Matrix4f(currentProj);
            prevCameraPos = currentCamPos;
            return;
        }

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getColorTexture(), copyTarget.getColorTexture(),
                0, 0, 0, 0, 0, main.width, main.height
        );

        Matrix4f mvInverse = new Matrix4f(currentView).invert();
        Matrix4f projInverse = new Matrix4f(currentProj).invert();

        float sampleCount = samples.getValue();
        float invSamples = 1.0f / sampleCount;
        float halfSamp = sampleCount * 0.5f;
        float algo = algorithm.is("Centered") ? 1.0f : 0.0f;
        float strength = blurStrength.getValue();

        EvaMotionBlurUniform uniform = new EvaMotionBlurUniform(
                mvInverse, projInverse, prevModelView, prevProjection,
                currentCamPos, prevCameraPos,
                main.width, main.height, algo,
                strength, invSamples, sampleCount, halfSamp
        );

        EvaRenderer.getInstance().motionBlur(
                main,
                copyTarget.getColorTextureView(),
                main.getDepthTextureView(),
                EvaTextures.sampler(),
                uniform
        );

        prevModelView = new Matrix4f(currentView);
        prevProjection = new Matrix4f(currentProj);
        prevCameraPos = currentCamPos;
    }
}
