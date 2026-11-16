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

@UtilityClass
public class MagnifierProgram implements QuickImports {
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
    }

    private float[] mouseUv() {
        double x = mc.mouseHandler.xpos();
        double y = mc.mouseHandler.ypos();
        double width = mc.getWindow().getWidth();
        double height = mc.getWindow().getHeight();

        if (x < 0 || y < 0 || x >= width || y >= height) return new float[]{0.5f, 0.5f};

        return new float[]{(float) (x / width), (float) (1.0 - y / height)};
    }

    private TextureTarget createCopyTarget() {
        Minecraft client = Minecraft.getInstance();
        return new TextureTarget("Static Lens", client.getWindow().getWidth(), client.getWindow().getHeight(), false, client.gameRenderer.mainRenderTarget().getColorTexture().getFormat());
    }
}
