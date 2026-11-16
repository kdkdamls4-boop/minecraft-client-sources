package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.FramebufferResizeEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import tokyo.ghoul.client.features.modules.render.PredictionsModule;
import tokyo.ghoul.client.features.modules.render.nametags.NameTagsModule;
import tokyo.ghoul.client.ui.clickgui.ClickGui;
import tokyo.ghoul.client.ui.widget.Widget;
import tokyo.ghoul.client.ui.widget.WidgetManager;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class KawaseBlurProgram implements QuickImports {
    private static final long BLUR_INTERVAL_NS = 1_000_000_000L / 40L;

    public final List<TextureTarget> fbos = new ArrayList<>();

    private boolean init = false;
    private long lastBlurNs;

    public void load() {
        if (!init) {
            recreate();
            init = true;
        }

        FramebufferResizeEvent.getInstance().subscribe(new Listener<>(event -> recreate()));
    }

    public void recreate() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) return;

        fbos.forEach(RenderTarget::destroyBuffers);
        fbos.clear();

        fbos.add(createFbo());
        fbos.add(createFbo());
    }

    public void render(PoseStack matrixStack) {
        if (InterfaceModule.getGlassy() == 1f || fbos.size() < 2 || !needsBlur()) return;

        long now = System.nanoTime();
        if (now - lastBlurNs < BLUR_INTERVAL_NS) return;
        lastBlurNs = now;

        blurMainTarget();
    }

    public void captureNow() {
        if (fbos.size() < 2) return;
        blurMainTarget();
    }

    public void captureNow(int iterations) {
        if (fbos.size() < 2) return;
        blurMainTarget(iterations, InterfaceModule.getOffset());
    }

    private void blurMainTarget() {
        blurMainTarget(InterfaceModule.getPasses(), InterfaceModule.getOffset());
    }

    private void blurMainTarget(int passes, float offset) {
        int iterations = Math.max(passes, 1);

        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        RenderTarget fbo0 = fbos.get(0);
        RenderTarget fbo1 = fbos.get(1);

        for (int i = 0; i < iterations; i++) {
            RenderTarget sourceH = (i == 0) ? main : fbo0;
            applyBlurPass(EvaPipelines.GAUSSIAN_H, sourceH, fbo1, offset);
            applyBlurPass(EvaPipelines.GAUSSIAN_V, fbo1, fbo0, offset);
        }
    }

    private boolean needsBlur() {
        if (mc.gui != null && mc.gui.screen() instanceof ClickGui) return true;
        if (PredictionsModule.getInstance().isEnabled()) return true;
        if (NameTagsModule.getInstance().isEnabled() && NameTagsModule.getInstance().liquidGlass.getValue()) return true;
        if (!InterfaceModule.getInstance().isEnabled()) return false;
        for (Widget widget : WidgetManager.getInstance().getWidgets()) {
            if (widget.isEnabled() && (widget.isLiquidGlass() || Widget.isWatermarkLiquidGlass())) return true;
        }
        return false;
    }

    private void applyBlurPass(RenderPipeline pipeline, RenderTarget source, RenderTarget destination, float offset) {
        EvaRenderer.getInstance().fullscreen(pipeline, destination, source.getColorTextureView(), EvaTextures.sampler(), EvaUniform.of(
                0f, 0f, 0f, 0f,
                0.5f / source.width, 0.5f / source.height, offset, 0f
        ));
    }

    private TextureTarget createFbo() {
        Minecraft client = Minecraft.getInstance();
        return new TextureTarget("Static Blur", client.getWindow().getWidth(), client.getWindow().getHeight(), false, client.gameRenderer.mainRenderTarget().getColorTexture().getFormat());
    }
}
