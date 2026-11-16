package tokyo.ghoul.client.ui.clickgui.screen;

import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.client.ui.theme.Theme;
import tokyo.ghoul.client.ui.theme.ThemeEditor;
import tokyo.ghoul.client.ui.theme.ThemeSelectable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class CsThemesPage {
    private static final int COLUMNS = 2;
    private static final Color WHITE_02 = CsModuleCard.white(0.02f);
    private static final Color WHITE_04 = CsModuleCard.white(0.04f);
    private static final Color WHITE_05 = CsModuleCard.white(0.05f);
    private static final Color WHITE_07 = CsModuleCard.white(0.07f);
    private static final Color WHITE_25 = CsModuleCard.white(0.25f);
    private static final Color WHITE_45 = CsModuleCard.white(0.45f);
    private static final Color PANEL = new Color(24, 23, 23);
    private static final Color DANGER = new Color(255, 97, 100);

    private final StringBuilder name = new StringBuilder();
    private boolean typing;
    private float focus;
    private float createHover;
    private float trashHover;
    private float scroll, scrollTarget;

    private float x, y, w, h;
    private final List<Theme> shown = new ArrayList<>();
    private final Map<Theme, float[]> cardAnims = new IdentityHashMap<>();

    private Theme colorsOf;
    private final List<ThemesPage.ThemeColor> colors = new ArrayList<>();
    private final Map<ThemesPage.ThemeColor, Float> rowHover = new IdentityHashMap<>();

    public boolean isTyping() {
        return typing;
    }

    public void releaseKeyboard() {
        typing = false;
    }

    public void refresh() {
        Theme current = UIColors.currentTheme();
        for (ThemeSelectable selectable : ThemeEditor.getInstance().getThemeSelectables()) {
            Theme theme = selectable.getTheme();
            if (theme != current && theme.getName().equals(current.getName())) {
                ThemeEditor.getInstance().setCurrentTheme(theme);
                return;
            }
        }
    }

    public void resetScroll() {
        scroll = scrollTarget = 0f;
    }


    private float gap() { return r(9f); }
    private float gridW() { return w - editorW() - gap() - r(10f); }
    private float cardW() { return (gridW() - gap() * (COLUMNS - 1)) / COLUMNS; }
    private float cardH() { return r(96f); }
    private float editorW() { return r(300f); }
    private float editorX() { return x + w - editorW(); }
    private float pad() { return r(15f); }
    private float innerX() { return editorX() + pad(); }
    private float innerW() { return editorW() - pad() * 2f; }
    private float colorsY() { return y + r(84f); }
    private float rowH() { return r(30f); }
    private float rowGap() { return r(6f); }
    private float barH() { return r(30f); }
    private float barY() { return y + h - pad() - barH(); }
    private float createW() { return r(78f); }
    private float trashX() { return editorX() + editorW() - pad() - r(8f); }
    private float trashY() { return y + r(25f); }

    private float cardX(int i) { return x + (i % COLUMNS) * (cardW() + gap()); }
    private float cardY(int i) { return y + (i / COLUMNS) * (cardH() + gap()) - scroll; }


    public void render(float x, float y, float w, float h, float alpha, double mx, double my, float dt) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        focus = ease(focus, typing ? 1f : 0f, 16f, dt);

        Theme current = UIColors.currentTheme();
        if (current != colorsOf) {
            colorsOf = current;
            colors.clear();
            rowHover.clear();
            colors.addAll(ThemesPage.editableColors(current));
        }
        drawGrid(current, alpha, mx, my, dt);
        drawEditor(current, alpha, mx, my, dt);
    }

    private void drawGrid(Theme current, float alpha, double mx, double my, float dt) {
        shown.clear();
        for (ThemeSelectable selectable : ThemeEditor.getInstance().getThemeSelectables()) shown.add(selectable.getTheme());

        int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
        float content = rows * (cardH() + gap()) - gap();
        float max = Math.max(0f, content - h);
        scrollTarget = Math.max(0f, Math.min(max, scrollTarget));
        scroll = ease(scroll, scrollTarget, 17f, dt);

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x, y, gridW(), h);
        boolean live = inside(mx, my, x, y, gridW(), h);
        for (int i = 0; i < shown.size(); i++) {
            float cx = cardX(i), cy = cardY(i);
            if (cy + cardH() < y || cy > y + h) continue;
            Theme theme = shown.get(i);
            drawCard(theme, cx, cy, theme.getName().equals(current.getName()), alpha,
                    live && inside(mx, my, cx, cy, cardW(), cardH()), dt);
        }
        ScissorUtil.stop(matrices);

        if (max > 0f) {
            float trackX = x + gridW() + r(4f), thumbH = Math.max(r(30f), h * h / content);
            rect(trackX, y, r(2f), h, r(1f), a(WHITE_04, alpha));
            rect(trackX, y + (h - thumbH) * (scroll / max), r(2f), thumbH, r(1f),
                    a(mix(CsModuleCard.white(0.08f), CsModuleCard.accent(), 0.5f), alpha));
        }
    }

    private void drawCard(Theme theme, float cx, float cy, boolean active, float alpha, boolean hovered, float dt) {
        float[] anim = cardAnims.computeIfAbsent(theme, k -> new float[]{0f, active ? 1f : 0f});
        anim[0] = ease(anim[0], hovered ? 1f : 0f, 17f, dt);
        anim[1] = ease(anim[1], active ? 1f : 0f, 14f, dt);
        float hot = anim[0], on = anim[1];
        float cw = cardW(), ch = cardH(), radius = r(15f);
        Color primary = ThemesPage.solid(theme.getPrimaryColor());
        Color secondary = ThemesPage.solid(theme.getSecondaryColor());

        rect(cx, cy, cw, ch, radius, a(mix(WHITE_02, WHITE_05, Math.max(hot, on * 0.6f)), alpha));

        float sw = Math.min(cw * 0.55f, r(150f)), sh = ch * 0.62f;
        Color clear = new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 0);
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), cx + cw - sw, cy + ch - sh, sw, sh, radius,
                a(clear, alpha), a(new Color(primary.getRed(), primary.getGreen(), primary.getBlue(), 70), alpha),
                a(new Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), 40), alpha),
                a(new Color(secondary.getRed(), secondary.getGreen(), secondary.getBlue(), 170), alpha));

        float metaY = cy + r(23f);
        text(MEDIUM, theme.isPreset() ? "Preset" : "Custom", cx + r(15f), metaY, r(12f), a(WHITE_25, alpha));
        textRight(MEDIUM, hex(primary), cx + cw - r(15f), metaY, r(12f), a(WHITE_25, alpha));

        float titleSize = r(15f), ty = cy + r(55f);
        String title = ellipsize(MEDIUM, theme.getName(), titleSize, cw - r(50f));
        text(MEDIUM, title, cx + r(15f), ty, titleSize, a(Color.WHITE, alpha));
        if (on > 0.01f) {
            float dot = r(6f) * (0.6f + 0.4f * on);
            float dx = cx + r(15f) + MEDIUM.getWidth(title, titleSize) + r(8f);
            rect(dx, ty - dot / 2f, dot, dot, dot / 2f, a(primary, alpha * on));
        }
        String hint = active ? "Applied" : "Click to apply";
        text(REGULAR, hint, cx + r(15f), cy + r(75f), r(11.5f), a(mix(WHITE_25, WHITE_45, Math.max(hot, on)), alpha));
    }

    private void drawEditor(Theme current, float alpha, double mx, double my, float dt) {
        float ex = editorX(), ew = editorW();
        rect(ex, y, ew, h, r(15f), a(WHITE_02, alpha));
        boolean preset = current.isPreset();
        float ix = innerX(), iw = innerW();
        Color accent = CsModuleCard.accent();

        float titleSize = r(15f);
        text(MEDIUM, ellipsize(MEDIUM, current.getName(), titleSize, iw - r(24f)), ix, y + r(25f), titleSize,
                a(Color.WHITE, alpha));
        text(REGULAR, preset ? "Preset  ·  create a copy to edit" : "Custom theme", ix, y + r(43f), r(11.5f),
                a(WHITE_25, alpha));
        if (!preset) {
            trashHover = ease(trashHover, inside(mx, my, trashX() - r(10f), trashY() - r(10f), r(20f), r(20f)) ? 1f : 0f, 18f, dt);
            icon(TRASH, trashX(), trashY(), r(13f), a(mix(WHITE_25, DANGER, trashHover), alpha));
        }
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), ix, y + r(60f), iw, r(5f), r(2.5f),
                a(ThemesPage.solid(current.getPrimaryColor()), alpha), a(ThemesPage.solid(current.getSecondaryColor()), alpha),
                a(ThemesPage.solid(current.getPrimaryColor()), alpha), a(ThemesPage.solid(current.getSecondaryColor()), alpha));

        float listBottom = barY() - r(12f);
        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, ix, colorsY(), iw, listBottom - colorsY());
        float ry = colorsY();
        float rowAlpha = alpha * (preset ? 0.5f : 1f);
        for (ThemesPage.ThemeColor color : colors) {
            boolean hovered = !preset && inside(mx, my, ix, ry, iw, rowH()) && my < listBottom;
            float hv = ease(rowHover.getOrDefault(color, 0f), hovered ? 1f : 0f, 16f, dt);
            rowHover.put(color, hv);
            rect(ix, ry, iw, rowH(), r(7f), a(mix(WHITE_04, WHITE_07, hv), alpha));
            float cy = ry + rowH() / 2f;
            text(MEDIUM, color.element.getName(), ix + r(10f), cy, r(12.5f), a(mix(WHITE_45, Color.WHITE, hv), rowAlpha));
            float s = r(12f), swx = ix + iw - r(10f) - s;
            textRight(MEDIUM, color.hex(), swx - r(8f), cy, r(11.5f), a(WHITE_25, rowAlpha));
            rect(swx, cy - s / 2f, s, s, r(4f), a(ThemesPage.solid(color.color()), rowAlpha));
            ry += rowH() + rowGap();
        }
        ScissorUtil.stop(matrices);

        float by = barY(), fw = iw - createW() - r(6f), cy = by + barH() / 2f;
        rect(ix, by, fw, barH(), r(7f), a(mix(WHITE_04, WHITE_07, focus), alpha));
        icon(PENCIL, ix + r(14f), cy, r(10f), a(mix(WHITE_25, Color.WHITE, focus), alpha));
        float size = r(12f);
        if (name.isEmpty() && !typing) {
            text(MEDIUM, "Theme name..", ix + r(28f), cy, size, a(WHITE_25, alpha));
        } else {
            String shownName = name.toString();
            while (!shownName.isEmpty() && MEDIUM.getWidth(shownName, size) > fw - r(36f)) shownName = shownName.substring(1);
            text(MEDIUM, shownName, ix + r(28f), cy, size, a(Color.WHITE, alpha));
            if (typing && System.currentTimeMillis() % 1000 < 500) {
                float caretX = ix + r(28f) + MEDIUM.getWidth(shownName, size) + r(1f);
                rect(caretX, cy - r(6f), r(0.9f), r(12f), 0f, a(Color.WHITE, alpha));
            }
        }
        float bx = ix + iw - createW();
        createHover = ease(createHover, inside(mx, my, bx, by, createW(), barH()) ? 1f : 0f, 16f, dt);
        rect(bx, by, createW(), barH(), r(7f), a(mix(accent, Color.WHITE, createHover * 0.25f), alpha));
        textCenter(SEMIBOLD, "Create", bx + createW() / 2f, cy, r(12f), a(PANEL, alpha));
    }

    private static String hex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }


    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        Theme current = UIColors.currentTheme();

        if (inside(mx, my, x, y, gridW(), h)) {
            for (int i = 0; i < shown.size(); i++) {
                if (inside(mx, my, cardX(i), cardY(i), cardW(), cardH())) {
                    ThemeEditor.getInstance().setCurrentTheme(shown.get(i));
                    return true;
                }
            }
            return false;
        }
        if (!inside(mx, my, editorX(), y, editorW(), h)) return false;

        if (!current.isPreset() && inside(mx, my, trashX() - r(10f), trashY() - r(10f), r(20f), r(20f))) {
            ThemesPage.deleteTheme(current);
            cardAnims.remove(current);
            return true;
        }
        if (!current.isPreset()) {
            float ry = colorsY(), ix = innerX(), iw = innerW();
            for (ThemesPage.ThemeColor color : colors) {
                if (inside(mx, my, ix, ry, iw, rowH()) && my < barY() - r(12f)) {
                    color.sync();
                    ColorPicker.INSTANCE.open(color, editorX() - r(8f), ry - r(6f));
                    return true;
                }
                ry += rowH() + rowGap();
            }
        }
        float fw = innerW() - createW() - r(6f);
        if (inside(mx, my, innerX(), barY(), fw, barH())) {
            typing = true;
            return true;
        }
        if (inside(mx, my, innerX() + innerW() - createW(), barY(), createW(), barH())) {
            create();
            return true;
        }
        return true;
    }

    private void create() {
        ThemesPage.createTheme(name.toString());
        name.setLength(0);
        typing = false;
        scrollTarget = Float.MAX_VALUE;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my, x, y, gridW() + r(10f), h)) return false;
        scrollTarget -= (float) amount * r(48f);
        return true;
    }

    public boolean keyPressed(int key) {
        if (!typing) return false;
        if (key == GLFW.GLFW_KEY_BACKSPACE && !name.isEmpty()) name.deleteCharAt(name.length() - 1);
        else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) create();
        else if (key == GLFW.GLFW_KEY_ESCAPE) typing = false;
        return true;
    }

    public boolean charTyped(char chr) {
        if (!typing) return false;
        if (name.length() < 20 && ThemesPage.nameChar(chr)) name.append(chr);
        return true;
    }
}
