package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.ClickGuiIcons;
import tokyo.ghoul.client.ui.clickgui.ClickGuiLayout;

import java.awt.Color;

public final class GuiStyle {
    private GuiStyle() {}

    private static final float K = 0.7f;
    private static final float LAYOUT_W = 1410f;
    private static final float LAYOUT_H = 700f;
    private static final float MAX_SCREEN_SHARE = 0.95f;

    public static final Color ACCENT = new Color(76, 86, 255);
    public static final Color WINDOW = new Color(13, 13, 15);
    public static final Color PANEL = new Color(19, 19, 21);
    public static final Color INSET = new Color(14, 14, 16);
    public static final Color CARD = new Color(22, 22, 25);
    public static final Color SELECTED = new Color(30, 30, 33);
    public static final Color CHIP = new Color(33, 33, 37);
    public static final Color FIELD = new Color(28, 28, 31);
    public static final Color STROKE = new Color(255, 255, 255, 4);
    public static final float OUTLINE = 0.3f;

    public static final Color TEXT = new Color(238, 238, 242);
    public static final Color LABEL = new Color(212, 212, 218);
    public static final Color MUTED = new Color(110, 110, 116);
    public static final Color DIM = new Color(88, 88, 94);
    public static final Color FAINT = new Color(56, 56, 61);

    public static final Color SURFACE = new Color(20, 20, 22, 210);
    public static final Color ROW = new Color(35, 35, 38, 235);
    public static final Color ROW_ON = new Color(54, 54, 58, 245);
    public static final Color ROW_HOVER = new Color(66, 66, 71, 245);
    public static final Color ROW_TEXT = new Color(116, 116, 122);
    public static final Color ROW_TEXT_ON = new Color(240, 240, 244);
    public static final Color HEADER_TEXT = new Color(166, 166, 172);
    public static final Color DIVIDER = new Color(64, 64, 70);
    public static final Color ICON_DIM = new Color(124, 124, 130);
    public static final Color SETTING_TEXT = new Color(140, 140, 146);
    public static final Color TRACK = new Color(70, 70, 76);
    public static final Color FILL = new Color(226, 226, 232);
    public static final Color BADGE = new Color(212, 212, 218);
    public static final Color BADGE_TEXT = new Color(26, 26, 30);

    public static final Identifier GEAR = ClickGuiIcons.SETTINGS;
    public static final Identifier SLIDERS = icon("sliders.svg");
    public static final Identifier SLIDERS_OPEN = icon("sliders_open.svg");
    public static final Identifier CHEVRON_DOWN = icon("chevron_down.svg");
    public static final Identifier CHEVRON_UP = icon("chevron_up.svg");
    public static final Identifier SEARCH = icon("search.svg");
    public static final Identifier CHECK = icon("check.svg");
    public static final Identifier KEYBOARD = icon("keyboard.svg");
    public static final Identifier PENCIL = icon("pencil.svg");
    public static final Identifier TRASH = icon("trash.svg");
    public static final Identifier CHEVRON = icon("chevron.svg");

    public static final Font REGULAR = Fonts.SF_REGULAR;
    public static final Font MEDIUM = Fonts.SF_MEDIUM;
    public static final Font SEMIBOLD = Fonts.SF_SEMIBOLD;
    public static final Font BOLD = Fonts.SF_BOLD;

    public static float r(float px) {
        return ClickGuiLayout.scaled(px * K) * fit();
    }

    private static float fit() {
        float layoutW = ClickGuiLayout.scaled(LAYOUT_W * K);
        float layoutH = ClickGuiLayout.scaled(LAYOUT_H * K);
        if (layoutW <= 0f || layoutH <= 0f) return 1f;
        var window = Minecraft.getInstance().getWindow();
        float byW = window.getGuiScaledWidth() * MAX_SCREEN_SHARE / layoutW;
        float byH = window.getGuiScaledHeight() * MAX_SCREEN_SHARE / layoutH;
        return Math.min(1f, Math.min(byW, byH));
    }

    public static String ellipsize(Font font, String text, float size, float maxW) {
        if (font.getWidth(text, size) <= maxW) return text;
        String cut = text;
        while (cut.length() > 1 && font.getWidth(cut + "..", size) > maxW) cut = cut.substring(0, cut.length() - 1);
        return cut + "..";
    }

    public static void glass(float x, float y, float w, float h, float radius, float alpha) {
        if (alpha <= 0.003f) return;
        var matrices = RenderUtil.matrices();
        RenderUtil.RECT.drawGlassShadow(matrices, x, y, w, h, radius, r(8f), new Color(0, 0, 0, (int) (55 * alpha)));
        RenderUtil.BLUR_RECT.draw(matrices, x, y, w, h, radius, new Color(255, 255, 255, (int) (255 * alpha)), 0f);
        panel(x, y, w, h, radius, SURFACE, alpha);
    }

    public static Color a(Color color, float alpha) {
        int value = Math.round(color.getAlpha() * Math.max(0f, Math.min(1f, alpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), value);
    }

    public static Color mix(Color from, Color to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }

    public static float width(Font font, String text, float size) {
        return font.getWidth(text, size);
    }

    public static void text(Font font, String text, float x, float centerY, float size, Color color) {
        font.drawText(RenderUtil.matrices(), text, x, centerY - size / 2f, size, color);
    }

    public static void textRight(Font font, String text, float rightX, float centerY, float size, Color color) {
        text(font, text, rightX - font.getWidth(text, size), centerY, size, color);
    }

    public static void textCenter(Font font, String text, float centerX, float centerY, float size, Color color) {
        text(font, text, centerX - font.getWidth(text, size) / 2f, centerY, size, color);
    }

    public static void icon(Identifier texture, float cx, float cy, float size, Color color) {
        ClickGuiIcons.draw(cx - size / 2f, cy - size / 2f, size, size, texture, color);
    }

    public static void rect(float x, float y, float w, float h, float radius, Color color) {
        if (color.getAlpha() <= 0) return;
        RenderUtil.RECT.draw(RenderUtil.matrices(), x, y, w, h, radius, color);
    }

    public static void panel(float x, float y, float w, float h, float radius, Color fill, float alpha) {
        rect(x, y, w, h, radius, a(fill, alpha));
        RenderUtil.RECT.drawOutline(RenderUtil.matrices(), x, y, w, h, radius, OUTLINE, a(STROKE, alpha));
    }

    public static boolean inside(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static float ease(float current, float target, float speed, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-speed * dt));
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/gui/" + name);
    }
}
