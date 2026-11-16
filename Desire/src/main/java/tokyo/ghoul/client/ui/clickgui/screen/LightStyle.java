package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class LightStyle {
    private LightStyle() {}


    private static final Identifier GLOW = FileUtil.getImage("interface/glow");
    public static final Identifier GEAR_BOLD = Identifier.fromNamespaceAndPath("desire", "textures/menu/gui/gear_bold.svg");

    public static final Identifier INFO = Identifier.fromNamespaceAndPath("desire", "textures/menu/gui/info_fill.svg");

    public static final Font INTER = Fonts.MENU_INTER;
    /** Half of Inter's capital-letter ink height, in em. */
    private static final float INTER_CAP_HALF = 0.371f;

    /** How far below centerY GuiStyle.text puts the capital letters of Inter; subtracted so caps sit on centerY. */
    public static float lift(Font font, float size) {
        return font == INTER ? (font.getMetrics().baselineHeight() - 0.5f - INTER_CAP_HALF) * size : 0f;
    }

    public static void text(Font font, String text, float x, float centerY, float size, Color color) {
        GuiStyle.text(font, text, x, centerY - lift(font, size), size, color);
    }

    public static void textRight(Font font, String text, float rightX, float centerY, float size, Color color) {
        GuiStyle.textRight(font, text, rightX, centerY - lift(font, size), size, color);
    }

    public static void textCenter(Font font, String text, float centerX, float centerY, float size, Color color) {
        GuiStyle.textCenter(font, text, centerX, centerY - lift(font, size), size, color);
    }

    public static final Color WHITE = new Color(240, 238, 232);
    public static final Color GRAY = new Color(150, 147, 138);
    public static final Color MUTE = new Color(98, 96, 88);
    public static final Color DARK = new Color(66, 64, 58);

    public static Color accent() {
        return UIColors.primary();
    }

    public static Color accent(int alpha) {
        return UIColors.primary(alpha);
    }

    public static Color secondary(int alpha) {
        return UIColors.secondary(alpha);
    }

    /** Near-white tinted towards the theme accent, used for every translucent surface and line. */
    public static Color tint(int alpha) {
        Color c = mix(Color.WHITE, accent(), 0.12f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    public static Color bg() {
        Color c = mix(new Color(11, 11, 11), accent(), 0.025f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), 250);
    }

    public static void outline(float x, float y, float w, float h, float radius, Color color) {
        if (color.getAlpha() <= 0) return;
        RenderUtil.RECT.drawOutline(RenderUtil.matrices(), x, y, w, h, radius, r(1.4f), color);
    }

    /** Soft radial glow centred on (cx, cy). */
    public static void glow(float cx, float cy, float w, float h, Color color) {
        if (color.getAlpha() <= 0) return;
        RenderUtil.TEXTURE_RECT.draw(RenderUtil.matrices(), cx - w / 2f, cy - h / 2f, w, h, 0.01f, color,
                0f, 0f, 1f, 1f, GLOW);
    }

    /** Vertical gradient that is strongest at the bottom edge and fades out towards the top. */
    public static void glowUp(float x, float y, float w, float h, float radius, Color color) {
        if (color.getAlpha() <= 0) return;
        Color clear = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), x, y, w, h, radius, clear, clear, color, color);
    }

    /** Thin outline on the sides and bottom only: sliced vertically so it fades to nothing at the top edge. */
    public static void openOutline(float x, float y, float w, float h, float radius, Color color) {
        if (color.getAlpha() <= 0) return;
        var matrices = RenderUtil.matrices();
        int slices = 6;
        float step = h / slices;
        for (int i = 1; i < slices; i++) {
            float t = (float) i / (slices - 1);
            Color c = a(color, t * t);
            ScissorUtil.start(matrices, x - r(2f), y + step * i, w + r(4f), i == slices - 1 ? step + r(2f) : step);
            RenderUtil.RECT.drawOutline(matrices, x, y, w, h, radius, r(0.5f), c);
            ScissorUtil.stop(matrices);
        }
    }

    public static List<String> wrap(Font font, String text, float size, float maxW, int maxLines) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty() || maxLines <= 0) return lines;
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int i = 0;
        for (; i < words.length; i++) {
            String candidate = line.isEmpty() ? words[i] : line + " " + words[i];
            if (font.getWidth(candidate, size) <= maxW || line.isEmpty()) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            lines.add(line.toString());
            if (lines.size() == maxLines) break;
            line.setLength(0);
            line.append(words[i]);
        }
        if (lines.size() < maxLines && !line.isEmpty()) {
            lines.add(line.toString());
            i = words.length;
        }
        if (i < words.length && !lines.isEmpty()) {
            int last = lines.size() - 1;
            lines.set(last, ellipsize(font, lines.get(last) + " " + words[i], size, maxW));
        }
        for (int k = 0; k < lines.size(); k++) lines.set(k, ellipsize(font, lines.get(k), size, maxW));
        return lines;
    }
}
