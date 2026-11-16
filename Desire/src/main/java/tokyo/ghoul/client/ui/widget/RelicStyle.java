package tokyo.ghoul.client.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.clickgui.ClickGuiTab;

import java.awt.Color;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

public final class RelicStyle {
    private RelicStyle() {}

    private static final float SCALE = 0.16f;

    public static final Color ACCENT = new Color(181, 161, 255);
    public static final Color ACCENT_DIM = new Color(181, 161, 255, 190);
    public static final Color TEXT = new Color(208, 208, 208);
    public static final Color ROW_TEXT = new Color(236, 236, 240);
    public static final Color BORDER = new Color(36, 37, 38);
    public static final Color FILL = new Color(21, 22, 23, 245);
    public static final Color HEADER = new Color(17, 18, 19, 248);
    public static final Color SEPARATOR = new Color(31, 32, 35);
    public static final Color SLOT_X = new Color(110, 108, 106);

    private static final Identifier GLOW = FileUtil.getImage("interface/glow");

    private static final float LIST_SCALE = 0.9f;

    public static float u(float px) {
        return RenderService.getInstance().scaledHud(px * SCALE * 2f / 3f);
    }

    public static float ul(float px) {
        return u(px * LIST_SCALE);
    }

    public static Color accent() {
        return UIColors.primary();
    }

    public static Color accent2() {
        return UIColors.secondary();
    }

    public static Color a(Color color, float alpha) {
        if (color == ACCENT) color = UIColors.primary();
        else if (color == ACCENT_DIM) color = UIColors.primary(190);
        int value = Math.max(0, Math.min(255, Math.round(color.getAlpha() * alpha)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), value);
    }


    private static final class Width {
        float size = -1f;
        float width;
    }

    private static final class Fit {
        Font font;
        float size = -1f, maxWidth = -1f;
        String result;
    }

    private static final Map<Font, Map<String, Width>> WIDTHS = new IdentityHashMap<>();
    private static final Map<String, Fit> FITS = new HashMap<>();

    public static float width(Font font, String text, float size) {
        Map<String, Width> map = WIDTHS.computeIfAbsent(font, f -> new HashMap<>());
        Width cached = map.get(text);
        if (cached == null) {
            if (map.size() > 1024) map.clear();
            cached = new Width();
            map.put(text, cached);
        }
        if (cached.size != size) {
            cached.width = font.getWidth(text, size);
            cached.size = size;
        }
        return cached.width;
    }

    public static String fit(Font font, String text, float size, float maxWidth) {
        Fit cached = FITS.get(text);
        if (cached != null && cached.font == font && cached.size == size && cached.maxWidth == maxWidth) {
            return cached.result;
        }

        String result = text;
        if (width(font, text, size) > maxWidth) {
            result = "..";
            int lo = 1, hi = text.length() - 1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                String cut = text.substring(0, mid).stripTrailing() + "..";
                if (font.getWidth(cut, size) <= maxWidth) {
                    result = cut;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
        }

        if (FITS.size() > 256) FITS.clear();
        Fit entry = cached != null ? cached : new Fit();
        entry.font = font;
        entry.size = size;
        entry.maxWidth = maxWidth;
        entry.result = result;
        FITS.put(text, entry);
        return result;
    }


    public static void panel(PoseStack ms, float x, float y, float w, float h, float corner, float stroke,
                             Color border, Color fill, float alpha, boolean glass) {
        if (alpha <= 0.001f) return;
        if (glass) {
            glass(ms, x, y, w, h, corner, alpha);
            return;
        }
        RenderUtil.RECT.draw(ms, x, y, w, h, corner, a(border, alpha));
        RenderUtil.RECT.draw(ms, x + stroke, y + stroke, w - stroke * 2f, h - stroke * 2f,
                Math.max(0f, corner - stroke), a(fill, alpha));
    }

    public static void glass(PoseStack ms, float x, float y, float w, float h, float corner, float alpha) {
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, corner, a(Color.WHITE, alpha), 0f);
        RenderUtil.RECT.draw(ms, x, y, w, h, corner, a(new Color(14, 14, 16, 150), alpha));
    }

    public static void menuPanel(PoseStack ms, float x, float y, float w, float h, float corner, float alpha) {
        if (alpha <= 0.001f) return;
        if (Widget.isWatermarkLiquidGlass()) {
            glass(ms, x, y, w, h, corner, alpha);
        } else {
            RenderUtil.RECT.draw(ms, x, y, w, h, corner, a(FILL, alpha));
        }
    }

    public static void menuHover(PoseStack ms, float x, float y, float w, float h, float corner, float alpha) {
        RenderUtil.RECT.draw(ms, x, y, w, h, corner, a(UIColors.primary(64), alpha));
    }

    public static void headerPanel(PoseStack ms, float x, float y, float w, float h, float headerH,
                                   float corner, float stroke, float alpha, boolean glass) {
        if (alpha <= 0.001f) return;
        if (glass) {
            glass(ms, x, y, w, h, corner, alpha);
            RenderUtil.RECT.draw(ms, x, y, w, headerH, new Vector4f(corner, corner, 0f, 0f),
                    a(new Color(0, 0, 0, 70), alpha));
            return;
        }
        RenderUtil.RECT.draw(ms, x, y, w, h, corner, a(BORDER, alpha));
        RenderUtil.RECT.draw(ms, x + stroke, y + stroke, w - stroke * 2f, h - stroke * 2f,
                Math.max(0f, corner - stroke), a(FILL, alpha));
        float r = Math.max(0f, corner - stroke);
        RenderUtil.RECT.draw(ms, x + stroke, y + stroke, w - stroke * 2f, headerH - stroke,
                new Vector4f(r, r, 0f, 0f), a(HEADER, alpha));
    }

    public static void glow(PoseStack ms, float cx, float cy, float w, float h, float alpha) {
        RenderUtil.TEXTURE_RECT.draw(ms, cx - w / 2f, cy - h / 2f, w, h, 0.01f,
                a(UIColors.primary(38), alpha), 0f, 0f, 1f, 1f, GLOW);
    }


    public static void icon(PoseStack ms, Identifier texture, float x, float cy, float w, float h, Color color, float alpha) {
        glow(ms, x + w / 2f, cy, w + u(60f), h + u(46f), alpha);
        if (color == ACCENT) {
            HudIcons.drawGradient(ms, x, cy - h / 2f, w, h, texture, a(UIColors.primary(), alpha), a(UIColors.secondary(), alpha));
        } else {
            HudIcons.draw(ms, x, cy - h / 2f, w, h, texture, a(color, alpha));
        }
    }

    public static void separator(PoseStack ms, float x, float cy, float w, float h, float alpha) {
        RenderUtil.RECT.draw(ms, x, cy - h / 2f, w, h, w / 2f, a(SEPARATOR, alpha));
    }

    public static void tabIcon(PoseStack ms, ClickGuiTab tab, float x, float top, float size, float alpha) {
        float labelBaseline = Fonts.HUD_MEDIUM.getMetrics().baselineHeight() * size;
        float iconBaseline = Fonts.WFONT.getMetrics().baselineHeight() * size;
        int value = Math.max(0, Math.min(255, Math.round(255 * alpha)));
        float lift = size * 0.08f;
        Fonts.WFONT.drawGradientText(ms, tab.getIconGlyph(), x, top + (labelBaseline - iconBaseline) - lift, size,
                UIColors.primary(value), UIColors.secondary(value), size / 2f);
    }


    public static void progressBar(PoseStack ms, float x, float y, float w, float h, float progress, float alpha) {
        float r = h / 2f;
        RenderUtil.RECT.draw(ms, x, y, w, h, r, a(UIColors.primary(33), alpha));
        float fill = Math.max(0f, Math.min(1f, progress)) * w;
        if (fill > h * 0.5f) {
            Color left = a(UIColors.primary(45), alpha);
            Color right = a(UIColors.secondary(), alpha);
            RenderUtil.GRADIENT_RECT.draw(ms, x, y, fill, h, r, left, right, left, right);
        }
    }

    public static void cover(PoseStack ms, Identifier texture, float aspect, float x, float y, float size, float radius, float alpha) {
        float u0 = 0f, v0 = 0f, uw = 1f, vh = 1f;
        if (aspect > 1f) {
            uw = 1f / aspect;
            u0 = (1f - uw) / 2f;
        } else if (aspect < 1f && aspect > 0f) {
            vh = aspect;
            v0 = (1f - vh) / 2f;
        }
        RenderUtil.TEXTURE_RECT.draw(ms, x, y, size, size, radius, a(Color.WHITE, alpha), u0, v0, uw, vh, texture);
    }


    public static void text(PoseStack ms, Font font, String text, float x, float cy, float size, Color color, float alpha) {
        textAt(ms, font, text, x, cy - size / 2f + size * 0.09f, size, color, alpha);
    }

    public static void textAt(PoseStack ms, Font font, String text, float x, float top, float size, Color color, float alpha) {
        if (color == ACCENT || color == ACCENT_DIM) {
            int base = color == ACCENT_DIM ? 190 : 255;
            int value = Math.max(0, Math.min(255, Math.round(base * alpha)));
            font.drawGradientText(ms, text, x, top, size, UIColors.primary(value), UIColors.secondary(value),
                    width(font, text, size) / 4f);
            return;
        }
        font.drawText(ms, text, x, top, size, a(color, alpha));
    }
}
