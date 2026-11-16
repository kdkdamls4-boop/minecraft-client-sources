package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.gui.GuiTexture;

import java.awt.Color;

public final class ClickGuiIcons {
    private ClickGuiIcons() {}

    public static final Identifier BRUSH = icon("brush.png");
    public static final Identifier SWORDS = icon("swords.png");
    public static final Identifier MOVEMENT = icon("department_movement.png");
    public static final Identifier RENDER = icon("department_render.png");
    public static final Identifier PLAYER = icon("department_player.png");
    public static final Identifier BOXES = icon("boxes.png");
    public static final Identifier AUTOBUY = icon("cart.svg");
    public static final Identifier GRID = icon("grid.png");
    public static final Identifier PALETTE = icon("palette.png");
    public static final Identifier COSMETICS = Identifier.fromNamespaceAndPath("desire", "textures/menu/icons/star.svg");
    public static final Identifier SETTINGS = icon("settings.svg");
    public static final Identifier LOAD = icon("exit.png");
    public static final Identifier DELETE = icon("delete.png");
    public static final Identifier TOGGLE_ON = icon("on.svg");
    public static final Identifier TOGGLE_OFF = icon("off.svg");

    public static void draw(float x, float y, float size, Identifier texture, Color color) {
        draw(x, y, size, size, texture, color);
    }

    public static void drawTabIcon(float x, float y, float size, ClickGuiTab tab, Color color) {
        float labelBaseline = Fonts.SF_MEDIUM.getMetrics().baselineHeight() * size;
        float iconBaseline = Fonts.WFONT.getMetrics().baselineHeight() * size;
        Fonts.WFONT.drawText(RenderUtil.matrices(), tab.getIconGlyph(), x, y + (labelBaseline - iconBaseline), size, color);
    }

    public static void drawCardPanel(float x, float y, float width, float height, float radius, float alphaScale) {
        int a = (int) (alphaScale * 255f);
        Color frost = ColorUtil.setAlpha(
                ColorUtil.interpolate(UIColors.primary(), new Color(10, 14, 18), 0.12f), (int) (a * 0.28f));
        RenderUtil.BLUR_RECT.draw(RenderUtil.matrices(), x, y, width, height, radius, frost, 0.08f);
        RenderUtil.RECT.draw(RenderUtil.matrices(), x, y, width, height, radius,
                ColorUtil.setAlpha(new Color(255, 255, 255), (int) (a * 0.05f)));
    }

    public static void drawGlassPanel(float x, float y, float width, float height, float radius, float alphaScale) {
        Color glass = UIColors.cardFill();
        RenderUtil.BLUR_RECT.draw(RenderUtil.matrices(), x, y, width, height, radius, ColorUtil.setAlpha(glass, (int) (235 * alphaScale)));
        RenderUtil.BLUR_RECT.draw(RenderUtil.matrices(), x, y, width, height, radius, ColorUtil.setAlpha(glass, (int) (90 * alphaScale)));
        RenderUtil.BLUR_RECT.draw(RenderUtil.matrices(), x, y, width, height, radius, ColorUtil.setAlpha(glass, (int) (45 * alphaScale)));
    }

    public static void drawGradientPanel(float x, float y, float width, float height, float radius, int alpha) {
        Color secondary = UIColors.secondary();
        Color primary = UIColors.primary();
        Color top = ColorUtil.interpolate(new Color(0, 0, 0), secondary, 0.24f);
        Color bottom = ColorUtil.interpolate(new Color(0, 0, 0), secondary, 0.12f);
        Color topRight = ColorUtil.interpolate(new Color(0, 0, 0), primary, 0.24f);
        Color bottomRight = ColorUtil.interpolate(new Color(0, 0, 0), primary, 0.12f);
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), x, y, width, height, radius,
                ColorUtil.setAlpha(top, alpha), ColorUtil.setAlpha(topRight, alpha),
                ColorUtil.setAlpha(bottom, alpha), ColorUtil.setAlpha(bottomRight, alpha));
    }

    public static final String LOGO_GLYPH = "S";

    public static void drawGradientLogo(float x, float y, float size, int alpha) {
        drawLogo(x, y, size, ColorUtil.setAlpha(UIColors.gradient(0), alpha), ColorUtil.setAlpha(UIColors.gradient(64), alpha));
    }

    public static void drawLogo(float x, float y, float size, Color left, Color right) {
        Fonts.LOGO.drawGlyphGradient(RenderUtil.matrices(), LOGO_GLYPH.charAt(0), x, y, size, left, right);
    }

    public static void draw(float x, float y, float width, float height, Identifier texture, Color color) {
        if (texture.getPath().endsWith(".svg")) {
            drawSvg(x, y, width, height, texture, color);
            return;
        }
        RenderUtil.TEXTURE_RECT.drawSmooth(RenderUtil.matrices(), x, y, width, height, 0f, color, 0f, 0f, 1f, 1f, texture);
    }

    public static void drawToggle(float x, float y, float width, float height, float enabled, Color onColor, Color offColor) {
        if (enabled < 0.995f) {
            drawSvg(x, y, width, height, TOGGLE_OFF, withAlpha(offColor, 1f - enabled));
        }
        if (enabled > 0.005f) {
            drawSvg(x, y, width, height, TOGGLE_ON, withAlpha(onColor, enabled));
        }
    }

    private static void drawSvg(float x, float y, float width, float height, Identifier texture, Color color) {
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        var setup = GuiTexture.load(texture).textureSetup(width * scale, height * scale);
        RenderUtil.TEXTURE_RECT.draw(RenderUtil.matrices(), x, y, width, height, 0.01f, color, 0f, 0f, 1f, 1f, setup.texure0());
    }

    private static Color withAlpha(Color color, float factor) {
        int alpha = Math.max(0, Math.min(255, Math.round(color.getAlpha() * factor)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/clickgui/" + name);
    }
}
