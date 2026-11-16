package tokyo.ghoul.api.utils.render.display;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;

public class RectRender {
    private static final float SMOOTHNESS = 0.8f;

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, float radius, Color color) {
        draw(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), color);
    }

    public void draw(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, Color color) {
        Matrix4f matrix = matrixStack.last().pose();

        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder builder = renderer.begin(EvaPipelines.RECT);
        int colorInt = color.getRGB();

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setColor(colorInt);

        renderer.submit(builder, EvaPipelines.RECT, EvaUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, SMOOTHNESS, 0f
        ));
    }

    public void drawGlassShadow(PoseStack matrixStack, float x, float y, float width, float height, float radius, float blur, Color color) {
        float spread = Math.max(blur, 1.0f);
        Matrix4f matrix = matrixStack.last().pose();
        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder builder = renderer.begin(EvaPipelines.GLASS_SHADOW);
        int colorInt = color.getRGB();

        builder.addVertex(matrix, x - spread, y - spread, 0f).setColor(colorInt);
        builder.addVertex(matrix, x - spread, y + height + spread, 0f).setColor(colorInt);
        builder.addVertex(matrix, x + width + spread, y + height + spread, 0f).setColor(colorInt);
        builder.addVertex(matrix, x + width + spread, y - spread, 0f).setColor(colorInt);

        renderer.submit(builder, EvaPipelines.GLASS_SHADOW, EvaUniform.of(
                radius, radius, radius, radius,
                width, height, spread, 0f
        ));
    }

    public void drawPill(PoseStack matrixStack, float x, float y, float width, float height, float capRadius, float smoothness, Color color) {
        Matrix4f matrix = matrixStack.last().pose();

        float pad = smoothness * 0.75F;
        float adjustedX = x - pad;
        float adjustedY = y - pad;
        float adjustedWidth = width + pad * 2.0F;
        float adjustedHeight = height + pad * 2.0F;

        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder builder = renderer.begin(EvaPipelines.PILL);
        int colorInt = color.getRGB();

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setColor(colorInt);

        renderer.submit(builder, EvaPipelines.PILL, EvaUniform.of(
                capRadius, adjustedWidth, adjustedHeight, smoothness, 0f
        ));
    }

    public void drawOutline(PoseStack matrixStack, float x, float y, float width, float height, float radius, float thickness, Color outlineColor) {
        drawOutline(matrixStack, x, y, width, height, new Vector4f(radius, radius, radius, radius), thickness, outlineColor);
    }

    public void drawOutline(PoseStack matrixStack, float x, float y, float width, float height, Vector4f radius, float thickness, Color outlineColor) {
        Matrix4f matrix = matrixStack.last().pose();

        float horizontalPadding = -SMOOTHNESS / 2.0F + SMOOTHNESS * 2.0F;
        float verticalPadding = SMOOTHNESS / 2.0F + SMOOTHNESS;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder builder = renderer.begin(EvaPipelines.RECT);
        int colorInt = outlineColor.getRGB();

        builder.addVertex(matrix, adjustedX, adjustedY, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0f).setColor(colorInt);
        builder.addVertex(matrix, adjustedX + adjustedWidth, adjustedY, 0f).setColor(colorInt);

        renderer.submit(builder, EvaPipelines.RECT, EvaUniform.of(
                radius.x, radius.z, radius.w, radius.y,
                width, height, SMOOTHNESS, thickness
        ));
    }

    public void drawOutline(PoseStack matrixStack, float x, float y, float width, float height, float radius, float thickness, Color outlineColor, Color fillColor) {
        draw(matrixStack, x, y, width, height, radius, outlineColor);
        draw(matrixStack, x + thickness, y + thickness, width - thickness * 2f, height - thickness * 2f, Math.max(0f, radius - thickness), fillColor);
    }
}
