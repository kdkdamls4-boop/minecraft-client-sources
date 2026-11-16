package tokyo.ghoul.client.ui.clickgui.screen;

import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.configs.ThemeManager;
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

public final class ThemesPage {
    private static final int COLUMNS = 3;

    private final StringBuilder name = new StringBuilder();
    private boolean typing;
    private float focus;
    private float createHover;
    private float trashHover;
    private float scroll, scrollTarget;

    private float x, y, w, h;
    private final List<Theme> shown = new ArrayList<>();
    private final Map<Theme, float[]> tileAnims = new IdentityHashMap<>();

    private Theme colorsOf;
    private final List<ThemeColor> colors = new ArrayList<>();
    private final Map<ThemeColor, Float> rowHover = new IdentityHashMap<>();

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


    private float gridW() {
        return r(336f);
    }

    private float gridGap() {
        return r(10f);
    }

    private float tileW() {
        return (gridW() - gridGap() * (COLUMNS - 1)) / COLUMNS;
    }

    private float tileH() {
        return r(60f);
    }

    private float sideX() {
        return x + gridW() + r(24f);
    }

    private float sideW() {
        return x + w - sideX();
    }

    private float colorsY() {
        return y + r(74f);
    }

    private float colorRowH() {
        return r(33f);
    }

    private float colorGap() {
        return r(6f);
    }

    private float barH() {
        return r(38f);
    }

    private float barY() {
        return y + h - barH();
    }

    private float createW() {
        return r(86f);
    }

    private float trashX() {
        return sideX() + sideW() - r(10f);
    }


    public void render(float x, float y, float w, float h, String query, float alpha, double mx, double my, float dt) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        focus = ease(focus, typing ? 1f : 0f, 16f, dt);

        Theme current = UIColors.currentTheme();
        if (current != colorsOf) rebuildColors(current);

        drawGrid(current, query, alpha, mx, my, dt);
        drawSide(current, alpha, mx, my, dt);
    }

    private void drawGrid(Theme current, String query, float alpha, double mx, double my, float dt) {
        shown.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (ThemeSelectable selectable : ThemeEditor.getInstance().getThemeSelectables()) {
            Theme theme = selectable.getTheme();
            if (q.isEmpty() || theme.getName().toLowerCase(Locale.ROOT).contains(q)) shown.add(theme);
        }

        int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
        float content = rows * (tileH() + gridGap()) - gridGap();
        scrollTarget = Math.max(0f, Math.min(Math.max(0f, content - h), scrollTarget));
        scroll = ease(scroll, scrollTarget, 16f, dt);

        if (shown.isEmpty()) {
            textCenter(MEDIUM, "Nothing found", x + gridW() / 2f, y + r(40f), r(13f), a(DIM, alpha));
            return;
        }

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x - r(4f), y, gridW() + r(8f), h);
        boolean live = inside(mx, my, x, y, gridW(), h);
        for (int i = 0; i < shown.size(); i++) {
            Theme theme = shown.get(i);
            float tx = x + (i % COLUMNS) * (tileW() + gridGap());
            float ty = y + (i / COLUMNS) * (tileH() + gridGap()) - scroll;
            if (ty + tileH() < y || ty > y + h) continue;
            drawTile(theme, tx, ty, theme.getName().equals(current.getName()), alpha,
                    live && inside(mx, my, tx, ty, tileW(), tileH()), dt);
        }
        ScissorUtil.stop(matrices);

        float maxScroll = Math.max(0f, content - h);
        if (maxScroll > 0f) {
            float trackX = x + gridW() + r(9f), thumbH = Math.max(r(36f), h * h / content);
            float thumbY = y + (h - thumbH) * (scroll / maxScroll);
            rect(trackX, thumbY, r(2.5f), thumbH, r(1.25f), a(new Color(255, 255, 255, 34), alpha));
        }
    }

    private void drawTile(Theme theme, float tx, float ty, boolean active, float alpha, boolean hovered, float dt) {
        float[] anim = tileAnims.computeIfAbsent(theme, k -> new float[]{0f, active ? 1f : 0f});
        anim[0] = ease(anim[0], hovered ? 1f : 0f, 16f, dt);
        anim[1] = ease(anim[1], active ? 1f : 0f, 16f, dt);
        float tw = tileW(), th = tileH(), radius = r(8f);
        var matrices = RenderUtil.matrices();

        Color primary = solid(theme.getPrimaryColor()), secondary = solid(theme.getSecondaryColor());
        Color middle = mix(primary, secondary, 0.5f);
        RenderUtil.GRADIENT_RECT.draw(matrices, tx, ty, tw, th, radius,
                a(primary, alpha), a(middle, alpha), a(middle, alpha), a(secondary, alpha));
        RenderUtil.GRADIENT_RECT.draw(matrices, tx, ty, tw, th, radius,
                a(new Color(0, 0, 0, 0), alpha), a(new Color(0, 0, 0, 0), alpha),
                a(new Color(0, 0, 0, 150), alpha), a(new Color(0, 0, 0, 150), alpha));
        if (anim[0] > 0.01f) rect(tx, ty, tw, th, radius, a(new Color(255, 255, 255, 22), alpha * anim[0]));

        float size = r(12.5f);
        text(MEDIUM, ellipsize(MEDIUM, theme.getName(), size, tw - r(18f)), tx + r(9f), ty + th - r(13f), size,
                a(Color.WHITE, alpha));
        if (!theme.isPreset()) {
            rect(tx + r(9f), ty + r(9f), r(5f), r(5f), r(2.5f), a(new Color(255, 255, 255, 200), alpha));
        }

        if (anim[1] > 0.01f) {
            RenderUtil.RECT.drawOutline(matrices, tx, ty, tw, th, radius, OUTLINE * 5f,
                    a(new Color(255, 255, 255, 230), alpha * anim[1]));
            float c = r(16f), cx = tx + tw - r(8f) - c, cy = ty + r(8f);
            rect(cx, cy, c, c, c / 2f, a(Color.WHITE, alpha * anim[1]));
            icon(CHECK, cx + c / 2f, cy + c / 2f, r(10f), a(BADGE_TEXT, alpha * anim[1]));
        }
    }

    private void drawSide(Theme current, float alpha, double mx, double my, float dt) {
        float sx = sideX(), sw = sideW();
        boolean preset = current.isPreset();

        float titleSize = r(16f);
        text(SEMIBOLD, ellipsize(SEMIBOLD, current.getName(), titleSize, sw - r(30f)), sx, y + r(12f), titleSize,
                a(ROW_TEXT_ON, alpha));
        text(MEDIUM, preset ? "Preset - create a copy to edit" : "Custom theme", sx, y + r(32f), r(11.5f),
                a(DIM, alpha));
        if (!preset) {
            trashHover = ease(trashHover, inside(mx, my, trashX() - r(10f), y + r(2f), r(20f), r(20f)) ? 1f : 0f, 18f, dt);
            icon(TRASH, trashX(), y + r(12f), r(13f), a(mix(ICON_DIM, new Color(255, 92, 102), trashHover), alpha));
        }
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), sx, y + r(48f), sw, r(8f), r(4f),
                a(solid(current.getPrimaryColor()), alpha), a(solid(current.getSecondaryColor()), alpha),
                a(solid(current.getPrimaryColor()), alpha), a(solid(current.getSecondaryColor()), alpha));

        float listBottom = barY() - r(12f);
        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, sx, colorsY(), sw, listBottom - colorsY());
        float ry = colorsY();
        float rowAlpha = alpha * (preset ? 0.55f : 1f);
        for (ThemeColor color : colors) {
            boolean hovered = !preset && inside(mx, my, sx, ry, sw, colorRowH()) && my < listBottom;
            float hv = ease(rowHover.getOrDefault(color, 0f), hovered ? 1f : 0f, 16f, dt);
            rowHover.put(color, hv);
            rect(sx, ry, sw, colorRowH(), r(7f), a(mix(ROW, ROW_ON, hv), alpha));
            float cy = ry + colorRowH() / 2f;
            text(MEDIUM, color.element.getName(), sx + r(13f), cy, r(13.5f), a(mix(SETTING_TEXT, ROW_TEXT_ON, hv), rowAlpha));
            float s = r(15f), swx = sx + sw - r(12f) - s;
            textRight(MEDIUM, color.hex(), swx - r(8f), cy, r(12.5f), a(SETTING_TEXT, rowAlpha));
            rect(swx - r(1.5f), cy - s / 2f - r(1.5f), s + r(3f), s + r(3f), s, a(new Color(255, 255, 255, 30), rowAlpha));
            rect(swx, cy - s / 2f, s, s, s / 2f, a(solid(color.color()), rowAlpha));
            ry += colorRowH() + colorGap();
        }
        ScissorUtil.stop(matrices);

        float by = barY(), fw = sw - createW() - r(8f), cy = by + barH() / 2f;
        rect(sx, by, fw, barH(), r(8f), a(ROW, alpha));
        RenderUtil.RECT.drawOutline(matrices, sx, by, fw, barH(), r(8f), OUTLINE * 0.4f,
                a(mix(new Color(255, 255, 255, 6), new Color(255, 255, 255, 46), focus), alpha));
        icon(PENCIL, sx + r(16f), cy, r(11f), a(mix(ICON_DIM, Color.WHITE, focus), alpha));
        float size = r(12.5f);
        if (name.isEmpty() && !typing) {
            text(MEDIUM, "Theme name..", sx + r(32f), cy, size, a(ICON_DIM, alpha));
        } else {
            text(MEDIUM, name.toString(), sx + r(32f), cy, size, a(ROW_TEXT_ON, alpha));
            if (typing && System.currentTimeMillis() % 1000 < 500) {
                float caretX = sx + r(32f) + MEDIUM.getWidth(name.toString(), size) + r(1f);
                rect(caretX, cy - r(6.5f), r(0.9f), r(13f), 0f, a(Color.WHITE, alpha));
            }
        }
        float bx = sx + sw - createW();
        createHover = ease(createHover, inside(mx, my, bx, by, createW(), barH()) ? 1f : 0f, 16f, dt);
        rect(bx, by, createW(), barH(), r(8f), a(mix(BADGE, Color.WHITE, createHover), alpha));
        textCenter(SEMIBOLD, "Create", bx + createW() / 2f, cy, r(12.5f), a(BADGE_TEXT, alpha));
    }

    private void rebuildColors(Theme theme) {
        colorsOf = theme;
        colors.clear();
        rowHover.clear();
        colors.addAll(editableColors(theme));
    }

    static Color solid(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }


    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        Theme current = UIColors.currentTheme();

        if (inside(mx, my, x, y, gridW(), h)) {
            for (int i = 0; i < shown.size(); i++) {
                float tx = x + (i % COLUMNS) * (tileW() + gridGap());
                float ty = y + (i / COLUMNS) * (tileH() + gridGap()) - scroll;
                if (inside(mx, my, tx, ty, tileW(), tileH())) {
                    ThemeEditor.getInstance().setCurrentTheme(shown.get(i));
                    return true;
                }
            }
            return false;
        }

        if (!current.isPreset() && inside(mx, my, trashX() - r(10f), y + r(2f), r(20f), r(20f))) {
            delete(current);
            return true;
        }

        if (!current.isPreset()) {
            float ry = colorsY(), sx = sideX(), sw = sideW();
            for (ThemeColor color : colors) {
                if (inside(mx, my, sx, ry, sw, colorRowH()) && my < barY() - r(12f)) {
                    color.sync();
                    ColorPicker.INSTANCE.open(color, sx + sw, ry + colorRowH());
                    return true;
                }
                ry += colorRowH() + colorGap();
            }
        }

        float fw = sideW() - createW() - r(8f);
        if (inside(mx, my, sideX(), barY(), fw, barH())) {
            typing = true;
            return true;
        }
        if (inside(mx, my, sideX() + sideW() - createW(), barY(), createW(), barH())) {
            create();
            return true;
        }
        return false;
    }

    private void create() {
        createTheme(name.toString());
        name.setLength(0);
        typing = false;
        scrollTarget = Float.MAX_VALUE;
    }

    private void delete(Theme theme) {
        deleteTheme(theme);
        tileAnims.remove(theme);
    }

    static void createTheme(String rawName) {
        ThemeEditor editor = ThemeEditor.getInstance();
        String raw = rawName.trim();
        String themeName = raw.isEmpty() ? "Theme " + (editor.getThemeSelectables().size() + 1) : raw;
        Theme theme = new Theme(ThemeManager.getInstance().safeFileName(themeName));
        theme.getElementColors().clear();
        for (Theme.ElementColor element : UIColors.currentTheme().getElementColors()) {
            theme.getElementColors().add(new Theme.ElementColor(element.getName(), element.getColor()));
        }
        editor.getThemeSelectables().add(new ThemeSelectable(theme));
        editor.setCurrentTheme(theme);
        ThemeManager.getInstance().save(theme);
    }

    static void deleteTheme(Theme theme) {
        ThemeEditor editor = ThemeEditor.getInstance();
        ColorPicker.INSTANCE.close();
        if (editor.getThemeSelectables().removeIf(selectable -> selectable.getTheme() == theme)) {
            ThemeManager.getInstance().remove(theme.getName());
        }
        List<ThemeSelectable> rest = editor.getThemeSelectables();
        editor.setCurrentTheme(rest.isEmpty() ? editor.getDefaultTheme() : rest.get(0).getTheme());
    }

    static List<ThemeColor> editableColors(Theme theme) {
        List<ThemeColor> list = new ArrayList<>();
        for (Theme.ElementColor element : theme.getElementColors()) {
            if (ThemeEditor.LOCKED_COLORS.contains(element.getName())) continue;
            list.add(new ThemeColor(theme, element));
        }
        return list;
    }

    static boolean nameChar(char chr) {
        return Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == ' ';
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my, x, y, gridW() + r(14f), h)) return false;
        scrollTarget -= (float) amount * r(46f);
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
        if (name.length() < 20 && nameChar(chr)) {
            name.append(chr);
        }
        return true;
    }

    static final class ThemeColor implements ColorPicker.Target {
        private final Theme theme;
        final Theme.ElementColor element;
        private final float[] hsva = {0f, 0f, 0f, 1f};

        ThemeColor(Theme theme, Theme.ElementColor element) {
            this.theme = theme;
            this.element = element;
            sync();
        }

        void sync() {
            Color c = element.getColor();
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            if (hsb[1] > 0.001f && hsb[2] > 0.001f) hsva[0] = hsb[0];
            hsva[1] = hsb[1];
            hsva[2] = hsb[2];
            hsva[3] = c.getAlpha() / 255f;
        }

        @Override
        public float[] hsva() {
            return hsva;
        }

        @Override
        public void apply() {
            Color c = Color.getHSBColor(hsva[0], hsva[1], hsva[2]);
            element.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(hsva[3] * 255f)));
        }

        @Override
        public Color color() {
            return element.getColor();
        }

        @Override
        public void commit() {
            ThemeManager.getInstance().save(theme);
        }
    }
}
