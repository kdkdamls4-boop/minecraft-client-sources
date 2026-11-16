package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.FramebufferResizeEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import tokyo.ghoul.client.features.modules.render.AmbienceModule;

@UtilityClass
public class AmbienceSaturationProgram implements QuickImports {
    private TextureTarget copyTarget;
    private boolean init = false;

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

        if (copyTarget != null) copyTarget.destroyBuffers();
        copyTarget = createCopyTarget();
    }

    public void render() {
        float saturation = AmbienceModule.getSaturation();
        if (Math.abs(saturation - 1.0f) < 0.001f) return;
        if (mc.level == null || mc.player == null) return;

        RenderTarget main = mc.gameRenderer.mainRenderTarget();
        if (main == null || copyTarget == null) return;
        if (copyTarget.width != main.width || copyTarget.height != main.height) recreate();

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                main.getColorTexture(), copyTarget.getColorTexture(),
                0, 0, 0, 0, 0, main.width, main.height
        );

        EvaRenderer.getInstance().fullscreen(EvaPipelines.AMBIENCE_SATURATION, main,
                copyTarget.getColorTextureView(), EvaTextures.sampler(), EvaUniform.of(saturation));
    }

    private TextureTarget createCopyTarget() {
        Minecraft client = Minecraft.getInstance();
        return new TextureTarget("Desire Ambience Saturation", client.getWindow().getWidth(), client.getWindow().getHeight(), false,
                client.gameRenderer.mainRenderTarget().getColorTexture().getFormat());
    }
}
