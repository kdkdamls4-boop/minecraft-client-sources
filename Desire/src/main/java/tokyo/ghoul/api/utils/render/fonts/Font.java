package tokyo.ghoul.api.utils.render.fonts;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.awt.Color;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import tokyo.ghoul.api.system.backend.Pair;
import lombok.Getter;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;

import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.other.ReplaceUtil;
import tokyo.ghoul.api.utils.other.TextUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.FontData.AtlasData;
import tokyo.ghoul.api.utils.render.fonts.FontData.GlyphData;
import tokyo.ghoul.api.utils.render.fonts.FontData.MetricsData;
import tokyo.ghoul.client.services.RenderService;

public final class Font {
    public static final long GRADIENT_PERIOD = 3000L;

    private final String name;
    private final Identifier texture;
    @Getter private final AtlasData atlas;
    @Getter private final MetricsData metrics;
    private final Map<Integer, MsdfGlyph> glyphs;
    private final Map<Integer, Map<Integer, Float>> kernings;

    public Font(String name, Identifier texture, AtlasData atlas, MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
    }

    private Pair<Float, Float> offset(float x, float y) {
        float scale = RenderService.getInstance().getScale();

        float x1 = x;
        float y1 = y;

        boolean isPS = name.contains(Fonts.ps);
        boolean isWYV = name.startsWith("wyvern/bold") || name.startsWith("wyvern/medium")
                || name.startsWith("wyvern/regular") || name.startsWith("wyvern/semibold")
                || name.startsWith("wyvern/roundbold");
        boolean isSF = name.contains(Fonts.sf) || isWYV;

        if (isSF || isPS) {
            y1 -= scale;
            if (isPS) {
                x1 -= scale / 2f;
            }
        }

        return new Pair<>(x1, y1);
    }

    public void drawText(PoseStack matrixStack, Component text, float x, float y, float size, float thickness, float smoothness, float spacing, int outlineColor, float outlineThickness) {
        if (text == null) return;

        try {
            Matrix4f matrix = matrixStack.last().pose();

            BufferBuilder builder = EvaRenderer.getInstance().begin(textPipeline());
            applyGlyphs(matrix, builder, TextUtil.parseTextToColoredGlyphs(ReplaceUtil.replaceSymbols(text)), size, (thickness + outlineThickness * 0.5f) * 0.5f * size, spacing, x, y + getMetrics().baselineHeight() * size, 0f);

            submit(builder, outlineThickness, thickness, smoothness, outlineColor);
        } catch (Exception e) {
        }
    }

    public void drawText(PoseStack matrixStack, String text, float x, float y, float size, float thickness, int color, int colorSecond, float offset, float smoothness, float spacing, int outlineColor, float outlineThickness) {
        drawText(matrixStack, text, x, y, size, thickness, color, colorSecond, offset, GRADIENT_PERIOD, smoothness, spacing, outlineColor, outlineThickness);
    }

    public void drawText(PoseStack matrixStack, String text, float x, float y, float size, float thickness, int color, int colorSecond, float offset, long period, float smoothness, float spacing, int outlineColor, float outlineThickness) {
        try {
            Matrix4f matrix = matrixStack.last().pose();

            BufferBuilder builder = EvaRenderer.getInstance().begin(textPipeline());
            applyGlyphs(matrix, builder, text, size, (thickness + outlineThickness * 0.5f) * 0.5f * size, spacing, x, y + getMetrics().baselineHeight() * size, 0f, color, colorSecond, offset, period);

            submit(builder, outlineThickness, thickness, smoothness, outlineColor);
        } catch (Exception e) {
        }
    }

    public void drawGlyphGradient(PoseStack matrixStack, char c, float x, float y, float size, Color left, Color right) {
        MsdfGlyph glyph = this.glyphs.get((int) c);
        if (glyph == null) return;

        try {
            Matrix4f matrix = matrixStack.last().pose();

            BufferBuilder builder = EvaRenderer.getInstance().begin(textPipeline());
            glyph.applyGradient(matrix, builder, size, x, y + getMetrics().baselineHeight() * size, 0f, left.getRGB(), right.getRGB());

            submit(builder, 0f, 0f, 0.5f, -1);
        } catch (Exception e) {
        }
    }

    private void submit(BufferBuilder builder, float outlineThickness, float thickness, float smoothness, int outlineColor) {
        boolean outlineEnabled = outlineThickness > 0.0f;
        float[] outline = outlineEnabled ? ColorUtil.normalize(outlineColor) : new float[4];

        AbstractTexture currentTexture = Minecraft.getInstance().getTextureManager().getTexture(this.texture);
        EvaRenderer.getInstance().submit(builder, textPipeline(), TextureSetup.singleTexture(currentTexture.getTextureView(), EvaTextures.sampler()), EvaUniform.of(
                0f, 0f, 0f, 0f,
                0f, 0f, smoothness, 0f,
                0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f,
                0f, 0f, 0f, 0f,
                getAtlas().range(), thickness, outlineEnabled ? 1f : 0f, outlineThickness,
                outline[0], outline[1], outline[2], outline[3]
        ));
    }

    private RenderPipeline textPipeline() {
        float range = getAtlas().range();
        if (range <= 10.5f) return EvaPipelines.TEXT_10;
        if (range <= 12.5f) return EvaPipelines.TEXT_12;
        if (range <= 22f) return EvaPipelines.TEXT_20;
        return EvaPipelines.TEXT_32;
    }



    public void drawText(PoseStack matrixStack, Component text, float x, float y, float size, float thickness) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, thickness, 0.5f, 0f, -1, thickness);
    }

    public void drawText(PoseStack matrixStack, String text, float x, float y, float size, Color color, float thickness) {
        drawText(matrixStack, text, x, y, size, color, thickness, 0f);
    }

    public void drawText(PoseStack matrixStack, String text, float x, float y, float size, Color color, float thickness, float spacing) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, thickness, color.getRGB(), -1, -1f, 0.5f, spacing, -1, thickness);
    }

    public void drawGradientText(PoseStack matrixStack, String text, float x, float y, float size, Color colorFirst, Color colorSecond, float offset, float thickness) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, thickness, colorFirst.getRGB(), colorSecond.getRGB(), offset, 0.5f, 0f, -1, thickness);
    }

    /** Градиент с собственным периодом перелива в мс (по умолчанию {@link #GRADIENT_PERIOD}). */
    public void drawGradientText(PoseStack matrixStack, String text, float x, float y, float size, Color colorFirst, Color colorSecond, float offset, long period) {
        Pair<Float, Float> coordinates = offset(x, y);

        drawText(matrixStack, text, coordinates.left(), coordinates.right(), size, 0f, colorFirst.getRGB(), colorSecond.getRGB(), offset, period, 0.5f, 0f, -1, 0f);
    }

    public void drawGradientText(PoseStack matrixStack, String text, float x, float y, float size, Color color, Color colorSecond, float offset) {
        drawGradientText(matrixStack, text, x, y, size, color, colorSecond, offset, 0f);
    }

    public void drawText(PoseStack matrixStack, Component text, float x, float y, float size) {
        drawText(matrixStack, text, x, y, size, 0f);
    }

    public void drawText(PoseStack matrixStack, String text, float x, float y, float size, Color color) {
        drawText(matrixStack, text, x, y, size, color, 0f);
    }

    public void drawCenteredText(PoseStack matrixStack, String text, float x, float y, float size, Color color, float thickness) {
        drawText(matrixStack, text, x - getWidth(text, size, thickness) / 2f, y, size, color, thickness);
    }

    public void drawCenteredText(PoseStack matrixStack, String text, float x, float y, float size, Color color) {
        drawCenteredText(matrixStack, text, x, y, size, color, 0f);
    }

    public void drawCenteredGradientText(PoseStack matrixStack, String text, float x, float y, float size, Color color, Color colorSecond, float offset, float thickness) {
        drawGradientText(matrixStack, text, x - getWidth(text, size, thickness) / 2f, y, size, color, colorSecond, offset, thickness);
    }

    public void drawCenteredGradientText(PoseStack matrixStack, String text, float x, float y, float size, Color color, Color colorSecond, float offset) {
        drawCenteredGradientText(matrixStack, text, x, y, size, color, colorSecond, offset, 0f);
    }

    public void drawWrap(PoseStack matrixStack, String text, float x, float y, float width, float size, Color color, float offset, Duration cycleDuration, Duration pauseDuration) {
        if (color.getAlpha() <= 0) return;

        float textWidth = getWidth(text, size);

        if (textWidth <= width) {
            drawText(matrixStack, text, x, y, size, color);
        } else {
            ScissorUtil.start(matrixStack, x, y - size / 4F, width, size * 1.5F);
            long cycleMillis = cycleDuration.toMillis();
            long pauseMillis = pauseDuration.toMillis();
            long totalCycleTime = cycleMillis + pauseMillis;

            long elapsed = System.currentTimeMillis() % totalCycleTime;

            float progress = (elapsed < cycleMillis)
                    ? (float) elapsed / cycleMillis
                    : 1.0F;

            float value = (Easing.SINE_BOTH.apply(progress) * (textWidth + offset));

            drawText(matrixStack, text, x - value, y, size, color);
            drawText(matrixStack, text, x - value + (textWidth + offset), y, size, color);
            ScissorUtil.stop(matrixStack);
        }
    }



    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color, int colorSecond, float offset) {
        applyGlyphs(matrix, consumer, text, size, thickness, spacing, x, y, z, color, colorSecond, offset, GRADIENT_PERIOD);
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, String text, float size, float thickness, float spacing, float x, float y, float z, int color, int colorSecond, float offset, long period) {
        int prevChar = -1;
        float startX = x;
        float totalWidth = getWidth(text, size);
        float time = (System.currentTimeMillis() % period) / (float) period;

        for (int i = 0; i < text.length(); i++) {
            int _char = text.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(_char);

            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault(_char, 0.0f) * size;
            }

            int currentColor = color;
            if (offset > 1.0f) {
                currentColor = ColorUtil.gradient(color, colorSecond, x - startX, totalWidth, time, offset);
            }

            x += glyph.apply(matrix, consumer, size, x, y, z, currentColor) + thickness + spacing;
            prevChar = _char;
        }
    }

    public void applyGlyphs(Matrix4f matrix, VertexConsumer consumer, List<MsdfGlyph.ColoredGlyph> glyphs, float size, float thickness, float spacing, float x, float y, float z) {
        int prevChar = -1;
        for (int i = 0; i < glyphs.size(); i++) {
            MsdfGlyph.ColoredGlyph glyphData = glyphs.get(i);
            int _char = glyphData.c();
            int color = glyphData.color();

            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault(_char, 0.0f) * size;
            }

            x += glyph.apply(matrix, consumer, size, x, y, z, color) + thickness + spacing;
            prevChar = _char;
        }
    }

    public float getHeight(float size) {
        return size;
    }

    public float getWidth(Component text, float size) {
        return getWidth(text, size, 0f);
    }

    public float getWidth(Component text, float size, float thickness) {
        if (text == null) return 0f;
        
        List<MsdfGlyph.ColoredGlyph> glyphs = TextUtil.parseTextToColoredGlyphs(text);
        int prevChar = -1;
        float width = 0.0f;

        for (int i = 0; i < glyphs.size(); i++) {
            int _char = glyphs.get(i).c();
            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph == null)
                continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault(_char, 0.0f) * size * (1f + thickness);
            }

            width += glyph.getWidth(size);
            prevChar = _char;
        }

        return width;
    }

    public float getWidth(String text, float size) {
        return getWidth(text, size, 0f);
    }

    public float getWidth(String text, float size, float thickness) {
        return getWidth(text, size, thickness, 0f);
    }

    public float getWidth(String text, float size, float thickness, float spacing) {
        int prevChar = -1;
        float width = 0.0f;

        String finalText = text;

        for (int i = 0; i < finalText.length(); i++) {
            int _char = finalText.charAt(i);
            MsdfGlyph glyph = this.glyphs.get(_char);
            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault(_char, 0.0f) * size * (1f + thickness);
            }
            width += glyph.getWidth(size) * (1f + thickness);
            if (i + 1 < finalText.length()) width += spacing;
            prevChar = _char;
        }
        return width;
    }

    public static FontBuilder builder() {
        return new FontBuilder();
    }
}
