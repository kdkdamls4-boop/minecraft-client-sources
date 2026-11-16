package tokyo.ghoul.client.ui.clickgui.screen;

import tokyo.ghoul.api.utils.render.RenderUtil;

import java.awt.Color;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class ColorPicker {
    public static final ColorPicker INSTANCE = new ColorPicker();

    public interface Target {
        float[] hsva();

        void apply();

        Color color();

        default String hex() {
            return String.format("#%06X", color().getRGB() & 0xFFFFFF);
        }

        default void commit() {}
    }

    private static final Color[] HUES = {
            new Color(255, 0, 0), new Color(255, 255, 0), new Color(0, 255, 0),
            new Color(0, 255, 255), new Color(0, 0, 255), new Color(255, 0, 255), new Color(255, 0, 0)
    };

    private Target target;
    private float anchorRight, anchorTop;
    private float x, y;
    private float anim;
    private boolean open;
    private int drag;

    private ColorPicker() {}

    public void open(Target setting, float right, float top) {
        target = setting;
        anchorRight = right;
        anchorTop = top;
        open = true;
        drag = 0;
    }

    public void close() {
        open = false;
        drag = 0;
    }

    public boolean isOpen() {
        return open;
    }

    public static float width() {
        return r(176f);
    }

    public static float height() {
        return r(156f);
    }

    private float pad() {
        return r(5f);
    }

    private float squareH() {
        return r(84f);
    }

    private float alphaBarY() {
        return y + pad() + squareH() + r(12f);
    }

    private float hueBarY() {
        return alphaBarY() + r(13f);
    }

    private float barX() {
        return x + r(8f);
    }

    private float barW() {
        return width() - r(16f);
    }

    public void render(float screenW, float screenH, double mx, double my, float alpha, float dt) {
        anim = ease(anim, open ? 1f : 0f, open ? 18f : 22f, dt);
        if (target == null || anim < 0.01f) {
            if (!open) target = null;
            return;
        }
        if (drag != 0) applyDrag(mx, my);

        float a = alpha * anim;
        float[] hsva = target.hsva();
        x = Math.max(r(4f), Math.min(anchorRight - width(), screenW - width() - r(4f)));
        y = Math.min(anchorTop + r(6f) + (1f - anim) * r(6f), screenH - height() - r(4f));
        float w = width(), h = height();

        RenderUtil.RECT.drawGlassShadow(RenderUtil.matrices(), x, y, w, h, r(9f), r(7f), a(new Color(0, 0, 0, 80), a));
        panel(x, y, w, h, r(9f), new Color(20, 20, 23), a);

        float sx = x + pad(), sy = y + pad(), sw = w - pad() * 2f, sh = squareH();
        Color pure = Color.getHSBColor(hsva[0], 1f, 1f);
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), sx, sy, sw, sh, r(6f),
                a(Color.WHITE, a), a(pure, a), a(Color.WHITE, a), a(pure, a));
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), sx, sy, sw, sh, r(6f),
                a(new Color(0, 0, 0, 0), a), a(new Color(0, 0, 0, 0), a),
                a(Color.BLACK, a), a(Color.BLACK, a));
        knob(sx + sw * hsva[1], sy + sh * (1f - hsva[2]), target.color(), a);

        Color solid = new Color(target.color().getRGB() & 0xFFFFFF);
        float bh = r(4f);
        float bx = barX(), bw = barW();
        rect(bx, alphaBarY() - bh / 2f, bw, bh, bh / 2f, a(new Color(40, 40, 46), a));
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), bx, alphaBarY() - bh / 2f, bw, bh, bh / 2f,
                a(new Color(solid.getRed(), solid.getGreen(), solid.getBlue(), 0), a), a(solid, a),
                a(new Color(solid.getRed(), solid.getGreen(), solid.getBlue(), 0), a), a(solid, a));
        knob(bx + bw * hsva[3], alphaBarY(), target.color(), a);

        float seg = bw / (HUES.length - 1);
        for (int i = 0; i < HUES.length - 1; i++) {
            RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), bx + seg * i, hueBarY() - bh / 2f, seg + 0.5f, bh, 0f,
                    a(HUES[i], a), a(HUES[i + 1], a), a(HUES[i], a), a(HUES[i + 1], a));
        }
        knob(bx + bw * hsva[0], hueBarY(), pure, a);

        float rowY = y + h - r(18f);
        rect(x + r(8f), rowY - r(0.5f), w - r(16f), r(1f), 0f, a(new Color(255, 255, 255, 8), a));
        String hex = target.hex();
        float size = r(12f);
        float textW = SEMIBOLD.getWidth(hex, size);
        float dot = r(10f);
        float startX = x + w - r(12f) - textW - dot - r(6f);
        rect(startX, rowY + r(1f) - dot / 2f + r(1f), dot, dot, r(3f), a(solid, a));
        text(SEMIBOLD, hex, startX + dot + r(6f), rowY + r(2f), size, a(LABEL, a));
        text(MEDIUM, Math.round(hsva[3] * 100f) + "%", x + r(12f), rowY + r(2f), r(11f), a(DIM, a));
    }

    private void knob(float cx, float cy, Color fill, float alpha) {
        float s = r(10f);
        rect(cx - s / 2f - r(1f), cy - s / 2f - r(1f), s + r(2f), s + r(2f), s, a(new Color(0, 0, 0, 60), alpha));
        rect(cx - s / 2f, cy - s / 2f, s, s, s / 2f, a(Color.WHITE, alpha));
        float inner = s - r(4f);
        rect(cx - inner / 2f, cy - inner / 2f, inner, inner, inner / 2f,
                a(new Color(fill.getRed(), fill.getGreen(), fill.getBlue()), alpha));
    }

    public boolean mouseClicked(double mx, double my) {
        if (!open) return false;
        if (!inside(mx, my, x, y, width(), height())) {
            close();
            return true;
        }
        float sx = x + pad(), sy = y + pad(), sw = width() - pad() * 2f;
        if (inside(mx, my, sx, sy, sw, squareH())) drag = 1;
        else if (inside(mx, my, barX() - r(4f), alphaBarY() - r(6f), barW() + r(8f), r(12f))) drag = 2;
        else if (inside(mx, my, barX() - r(4f), hueBarY() - r(6f), barW() + r(8f), r(12f))) drag = 3;
        applyDrag(mx, my);
        return true;
    }

    public void mouseReleased() {
        if (drag != 0 && target != null) target.commit();
        drag = 0;
    }

    private void applyDrag(double mx, double my) {
        if (target == null) return;
        float[] hsva = target.hsva();
        switch (drag) {
            case 1 -> {
                float sx = x + pad(), sy = y + pad(), sw = width() - pad() * 2f;
                hsva[1] = clamp((float) (mx - sx) / sw);
                hsva[2] = 1f - clamp((float) (my - sy) / squareH());
            }
            case 2 -> hsva[3] = clamp((float) (mx - barX()) / barW());
            case 3 -> hsva[0] = Math.min(0.999f, clamp((float) (mx - barX()) / barW()));
            default -> {
                return;
            }
        }
        target.apply();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
