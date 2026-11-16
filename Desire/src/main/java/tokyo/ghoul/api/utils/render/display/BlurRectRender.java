package tokyo.ghoul.api.utils.render.display;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.render.KawaseBlurProgram;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.client.gui.render.TextureSetup;

public class BlurRectRender implements QuickImports {
    private static final float SMOOTHNESS = 0.8f;

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color, float mix) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, color, color, color, mix);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, InterfaceModule.getGlassy());
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        draw(matrixStack, x, y, width, height, radius, color, color, color, color, InterfaceModule.getGlassy());
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color, float mix) {
        draw(matrixStack, x, y, width, height, radius, color, color, color, color, mix);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight) {
        draw(matrixStack, x, y, width, height, radius, topLeft, topRight, bottomLeft, bottomRight, InterfaceModule.getGlassy());
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color, float mix, RenderTarget fbo) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color, color, color, color, mix, fbo);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, float mix) {
        if (KawaseBlurProgram.fbos.isEmpty()) return;
        draw(matrixStack, x, y, width, height, radius, topLeft, topRight, bottomLeft, bottomRight, mix, KawaseBlurProgram.fbos.getFirst());
    }

    public void drawHud(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color, float appear, RenderTarget fbo) {
        if (fbo == null) return;
        float[] c = ColorUtil.normalize(color);
        Matrix4f matrix = matrixStack.last().pose();
        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder builder = renderer.begin(EvaPipelines.HUD_BLURRED_RECT);
        int rgb = color.getRGB();

        builder.addVertex(matrix, x, y, 0f).setUv(0f, 0f).setColor(rgb);
        builder.addVertex(matrix, x, y + height, 0f).setUv(0f, 1f).setColor(rgb);
        builder.addVertex(matrix, x + width, y + height, 0f).setUv(1f, 1f).setColor(rgb);
        builder.addVertex(matrix, x + width, y, 0f).setUv(1f, 0f).setColor(rgb);

        renderer.submit(builder, EvaPipelines.HUD_BLURRED_RECT, TextureSetup.singleTexture(fbo.getColorTextureView(), EvaTextures.sampler()), EvaUniform.of(
                radius, radius, radius, radius,
                width, height, 0f, c[3],
                c[0], c[1], c[2], c[3],
                c[0], c[1], c[2], c[3],
                c[0], c[1], c[2], c[3],
                c[0], c[1], c[2], c[3],
                appear, 0f, 0f, 0f
        ));
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, float mix, RenderTarget fbo) {
        draw(matrixStack, x, y, width, height, radius, topLeft, topRight, bottomLeft, bottomRight, mix, fbo, EvaPipelines.BLURRED_RECT);
    }

    private void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, float mix, RenderTarget fbo, com.mojang.blaze3d.pipeline.RenderPipeline pipeline) {
        if (fbo == null) return;

        float[] tl = ColorUtil.normalize(topLeft);
        float[] bl = ColorUtil.normalize(bottomLeft);
        float[] br = ColorUtil.normalize(bottomRight);
        float[] tr = ColorUtil.normalize(topRight);
        float alpha = (tl[3] + bl[3] + br[3] + tr[3]) / 4f;

        Matrix4f matrix = matrixStack.last().pose();

        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        float uLeft, uRight, vTop, vBottom;

        if (mix != 1) {
            int texW = fbo.width;
            int texH = fbo.height;
            double scale = (double) texW / mc.getWindow().getGuiScaledWidth();
            float fx = (float) (x * scale);
            float fy = (float) (y * scale);
            float fw = (float) (width * scale);
            float fh = (float) (height * scale);
            uLeft = fx / texW;
            uRight = (fx + fw) / texW;
            vTop = 1f - (fy / texH);
            vBottom = 1f - ((fy + fh) / texH);
        } else {
            uLeft = uRight = vTop = vBottom = 0f;
        }

        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder builder = renderer.begin(pipeline);

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setUv(uLeft, vTop).setColor(topLeft.getRGB());
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setUv(uLeft, vBottom).setColor(bottomLeft.getRGB());
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setUv(uRight, vBottom).setColor(bottomRight.getRGB());
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setUv(uRight, vTop).setColor(topRight.getRGB());

        renderer.submit(builder, pipeline, TextureSetup.singleTexture(fbo.getColorTextureView(), EvaTextures.sampler()), EvaUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, SMOOTHNESS, mix,
                tl[0], tl[1], tl[2], tl[3],
                bl[0], bl[1], bl[2], bl[3],
                br[0], br[1], br[2], br[3],
                tr[0], tr[1], tr[2], tr[3],
                alpha, 0f, 0f, 0f
        ));
    }
}
