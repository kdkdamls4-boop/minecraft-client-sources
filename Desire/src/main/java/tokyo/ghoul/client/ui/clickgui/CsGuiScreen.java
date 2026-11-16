package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsGuiPreview;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.clickgui.screen.ColorPicker;
import tokyo.ghoul.client.ui.clickgui.screen.ConfigsPage;
import tokyo.ghoul.client.ui.clickgui.screen.CsModuleCard;
import tokyo.ghoul.client.ui.clickgui.screen.CsThemesPage;
import tokyo.ghoul.client.ui.clickgui.screen.EventsPage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public class CsGuiScreen extends Screen implements ClickGui, QuickImports {
    private static final ClickGuiTab[] CATEGORIES = {
            ClickGuiTab.COMBAT, ClickGuiTab.MOVEMENT, ClickGuiTab.RENDER, ClickGuiTab.PLAYER, ClickGuiTab.OTHER
    };
    private static final ClickGuiTab[] NAV_LEFT = {null, ClickGuiTab.EVENTS, ClickGuiTab.COSMETICS};
    private static final ClickGuiTab[] NAV_RIGHT = {ClickGuiTab.CONFIGS, ClickGuiTab.THEME};
    private static final int COLUMNS = 3;

    private static final Color PANEL = new Color(24, 23, 23);
    private static final Color PANEL_RAISED = new Color(29, 28, 28);
    private static final Color WHITE_02 = new Color(255, 255, 255, 5);
    private static final Color WHITE_04 = new Color(255, 255, 255, 10);
    private static final Color WHITE_05 = new Color(255, 255, 255, 13);
    private static final Color WHITE_08 = new Color(255, 255, 255, 20);
    private static final Color WHITE_25 = new Color(255, 255, 255, 64);
    private static final Color WHITE_45 = new Color(255, 255, 255, 115);

    private static final CsGuiScreen instance = new CsGuiScreen();

    private boolean open;
    private boolean closing;
    private boolean closed;
    private float openAnim;
    private long lastFrame = System.nanoTime();

    private ClickGuiTab page;
    private ClickGuiTab category = ClickGuiTab.COMBAT;
    private float contentAnim = 1f;
    private float selectorX = Float.NaN, selectorW;
    private final float[] categoryHover = new float[CATEGORIES.length];
    private final float[] leftHover = new float[NAV_LEFT.length];
    private final float[] leftActive = new float[NAV_LEFT.length];
    private final float[] rightHover = new float[NAV_RIGHT.length];
    private final float[] rightActive = new float[NAV_RIGHT.length];

    private float offsetX, offsetY;
    private boolean dragging;
    private float grabX, grabY;

    private final Map<ClickGuiTab, List<CsModuleCard>> cards = new EnumMap<>(ClickGuiTab.class);
    private final Map<ClickGuiTab, float[]> scroll = new EnumMap<>(ClickGuiTab.class);
    private boolean cardsBuilt;
    private long lastScroll;
    private float scrollbarAnim;

    private final ConfigsPage configs = new ConfigsPage();
    private final CsThemesPage themes = new CsThemesPage();
    private final ClickGuiCosmetics cosmetics = new ClickGuiCosmetics();
    private final EventsPage events = new EventsPage();

    public CsGuiScreen() {
        super(Component.nullToEmpty(""));
    }

    public static CsGuiScreen getInstance() {
        return instance;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public ClickGuiTab getTab() {
        return page != null ? page : category;
    }

    @Override
    public boolean isTextInputFocused() {
        if (configs.isTyping() || themes.isTyping()) return true;
        for (CsModuleCard card : allCards()) if (card.capturesKeyboard()) return true;
        return false;
    }

    @Override
    public void onClose() {
        if (closed) return;
        beginClose();
    }

    private void beginClose() {
        ColorPicker.INSTANCE.close();
        releaseKeyboard();
        dragging = false;
        closing = true;
        open = false;
    }

    @Override
    protected void init() {
        closing = false;
        closed = false;
        open = true;
        openAnim = 0f;
        lastFrame = System.nanoTime();
        buildCards();
        configs.refresh();
        if (page == ClickGuiTab.THEME) themes.refresh();
        selectorX = Float.NaN;
        Minecraft.getInstance().mouseHandler.releaseMouse();
        super.init();
    }

    private void buildCards() {
        if (cardsBuilt) return;
        List<Module> modules = new ArrayList<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!module.isHidden()) modules.add(module);
        }
        modules.sort(java.util.Comparator.comparing(m -> m.getName().toLowerCase(Locale.ROOT)));
        for (ClickGuiTab tab : CATEGORIES) {
            cards.put(tab, new ArrayList<>());
            scroll.put(tab, new float[2]);
        }
        for (Module module : modules) {
            CsModuleCard card = new CsModuleCard(module);
            cards.get(card.tab()).add(card);
        }
        cardsBuilt = true;
    }

    private List<CsModuleCard> allCards() {
        List<CsModuleCard> all = new ArrayList<>();
        for (List<CsModuleCard> list : cards.values()) all.addAll(list);
        return all;
    }


    private float screenW() {
        return mc.getWindow().getGuiScaledWidth();
    }

    private float screenH() {
        return mc.getWindow().getGuiScaledHeight();
    }

    private static float panelW() { return r(924f); }
    private static float panelH() { return r(624f); }
    private static float margin() { return r(15f); }

    private float panelX() {
        return Math.max(0f, Math.min(screenW() - panelW(), (screenW() - panelW()) / 2f + offsetX));
    }

    private float panelY() {
        float y = Math.max(0f, Math.min(screenH() - panelH(), (screenH() - panelH()) / 2f + offsetY));
        return y + (1f - smooth(openAnim)) * r(12f);
    }

    private float barY() { return panelY() + margin(); }
    private static float barH() { return r(44f); }
    private static float button() { return r(44f); }
    private float leftX() { return panelX() + margin(); }
    private float rightX() { return panelX() + panelW() - margin() - NAV_RIGHT.length * button(); }
    private float dividerY() { return barY() + barH() + r(12f); }

    private float categoriesX() { return leftX() + NAV_LEFT.length * button() + r(12f); }
    private float categoriesW() { return rightX() - r(12f) - categoriesX(); }

    private static float catIcon() { return r(14f); }
    private static float catText() { return r(13.5f); }
    private static float catPad() { return r(14f); }
    private static float catGap() { return r(4f); }

    private static float categoryW(ClickGuiTab tab) {
        return catPad() * 2f + catIcon() + r(8f) + MEDIUM.getWidth(tab.getLabel(), catText());
    }

    private float[] categoryXs() {
        float total = -catGap();
        for (ClickGuiTab tab : CATEGORIES) total += categoryW(tab) + catGap();
        float[] xs = new float[CATEGORIES.length];
        float cx = categoriesX() + (categoriesW() - total) / 2f;
        for (int i = 0; i < CATEGORIES.length; i++) {
            xs[i] = cx;
            cx += categoryW(CATEGORIES[i]) + catGap();
        }
        return xs;
    }

    private float contentX() { return panelX() + margin(); }
    private float contentY() { return dividerY() + r(13f); }
    private static float cardGap() { return r(9f); }
    private static float pitch() { return CsModuleCard.width() + cardGap(); }
    private static float columnsW() { return pitch() * COLUMNS - cardGap(); }
    private float viewport() { return panelY() + panelH() - r(12f) - contentY(); }
    private float areaW() { return panelW() - margin() * 2f; }
    private float areaH() { return panelY() + panelH() - margin() - contentY(); }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RenderService.setGuiContext(true);
        try {
            super.extractRenderState(context, mouseX, mouseY, delta);
            renderContent(context, mouseX, mouseY);
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    private void renderContent(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        double mx = mouseX, my = mouseY;
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;

        openAnim = ease(openAnim, open && !closing ? 1f : 0f, closing ? 20f : 13f, dt);
        if (closing && openAnim <= 0.02f && !closed) {
            closed = true;
            mc.gui.setScreen(null);
            return;
        }
        contentAnim = Math.min(1f, contentAnim + dt / 0.3f);

        boolean cosmeticsLayer = page == ClickGuiTab.COSMETICS;
        if (cosmeticsLayer) EvaRenderer.getInstance().beginUnder();
        else CosmeticsGuiPreview.clearTiles();

        float alpha = smooth(openAnim);
        boolean interactive = !closing && !dragging && !ColorPicker.INSTANCE.isOpen();
        rect(0f, 0f, screenW(), screenH(), 0f, new Color(0, 0, 0, (int) (60 * alpha)));

        float px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        RenderUtil.RECT.drawGlassShadow(RenderUtil.matrices(), px, py, pw, ph, r(25f), r(10f),
                new Color(0, 0, 0, (int) (70 * alpha)));
        panel(px, py, pw, ph, r(25f), PANEL, alpha);

        drawTopBar(alpha, mx, my, dt, interactive);

        float appear = smooth(contentAnim);
        float contentAlpha = alpha * appear;
        float shift = (1f - appear) * r(16f);
        if (page == null) {
            drawModules(contentAlpha, shift, mx, my, dt, interactive);
        } else if (cosmeticsLayer) {
            var matrices = RenderUtil.matrices();
            ScissorUtil.start(matrices, contentX(), contentY(), areaW(), areaH());
            try {
                inCosmeticsArea(() -> {
                    cosmetics.renderLayered(context, px, py, contentAlpha, mouseX, mouseY);
                    return null;
                });
            } catch (Throwable t) {
                EvaRenderer.getInstance().endUnder();
                throw t;
            } finally {
                ScissorUtil.stop(matrices);
            }
        } else {
            double pmx = interactive ? mx : -1, pmy = interactive ? my : -1;
            float ay = contentY() + shift;
            if (page == ClickGuiTab.CONFIGS) {
                configs.render(contentX(), ay, areaW(), areaH(), "", contentAlpha, pmx, pmy, dt);
            } else if (page == ClickGuiTab.THEME) {
                themes.render(contentX(), ay, areaW(), areaH(), contentAlpha, pmx, pmy, dt);
            } else if (page == ClickGuiTab.EVENTS) {
                events.render(contentX(), ay, areaW(), areaH(), "", contentAlpha, pmx, pmy, dt);
            }
        }

        drawTooltips(alpha, mx, my, interactive);
        ColorPicker.INSTANCE.render(screenW(), screenH(), mx, my, alpha, dt);
    }

    private void drawTopBar(float alpha, double mx, double my, float dt, boolean interactive) {
        drawButtons(NAV_LEFT, leftX(), leftHover, leftActive, alpha, mx, my, dt, interactive);
        drawButtons(NAV_RIGHT, rightX(), rightHover, rightActive, alpha, mx, my, dt, interactive);
        drawCategories(alpha, mx, my, dt, interactive);
        rect(panelX() + margin(), dividerY(), panelW() - margin() * 2f, r(1f), 0f, a(WHITE_04, alpha));
    }

    private void drawButtons(ClickGuiTab[] tabs, float x, float[] hover, float[] active, float alpha,
                             double mx, double my, float dt, boolean interactive) {
        float y = barY(), b = button();
        rect(x, y, tabs.length * b, barH(), r(13f), a(WHITE_02, alpha));
        Color accent = UIColors.primary();
        float size = r(15f);
        for (int i = 0; i < tabs.length; i++) {
            ClickGuiTab tab = tabs[i];
            float bx = x + i * b, cx = bx + b / 2f, cy = y + barH() / 2f;
            boolean on = page == tab;
            boolean hovered = interactive && inside(mx, my, bx, y, b, barH());
            hover[i] = ease(hover[i], hovered ? 1f : 0f, 18f, dt);
            active[i] = ease(active[i], on ? 1f : 0f, 17f, dt);

            Color tint = a(mix(mix(WHITE_25, WHITE_45, hover[i]), accent, active[i]), alpha);
            if (tab == null) {
                icon(GEAR, cx, cy, size, tint);
            } else {
                float iw = Fonts.WFONT.getWidth(tab.getIconGlyph(), size);
                ClickGuiIcons.drawTabIcon(cx - iw / 2f, cy - size / 2f, size, tab, tint);
            }
            if (active[i] > 0.01f) {
                float iw = r(7f) * (0.4f + 0.6f * active[i]);
                rect(cx - iw / 2f, y + barH() - r(6f), iw, r(2f), r(1f), a(accent, alpha * active[i]));
            }
        }
    }

    private void drawCategories(float alpha, double mx, double my, float dt, boolean interactive) {
        float y = barY(), h = barH();
        rect(categoriesX(), y, categoriesW(), h, r(13f), a(WHITE_02, alpha));

        float[] xs = categoryXs();
        int current = 0;
        for (int i = 0; i < CATEGORIES.length; i++) if (CATEGORIES[i] == category) current = i;
        float targetX = xs[current], targetW = categoryW(category);
        if (Float.isNaN(selectorX)) {
            selectorX = targetX;
            selectorW = targetW;
        }
        selectorX = ease(selectorX, targetX, 17f, dt);
        selectorW = ease(selectorW, targetW, 17f, dt);

        Color accent = UIColors.primary();
        float modules = page == null ? 1f : 0.45f;
        float inset = r(6f);
        rect(selectorX, y + inset, selectorW, h - inset * 2f, r(9f), a(WHITE_05, alpha * modules));
        float line = Math.min(r(18f), selectorW * 0.4f);
        rect(selectorX + selectorW / 2f - line / 2f, y + h - r(6f), line, r(2f), r(1f), a(accent, alpha * modules));

        float cy = y + h / 2f, size = catIcon();
        for (int i = 0; i < CATEGORIES.length; i++) {
            ClickGuiTab tab = CATEGORIES[i];
            float ix = xs[i], iw = categoryW(tab);
            boolean hovered = interactive && inside(mx, my, ix, y, iw, h);
            boolean on = i == current && page == null;
            categoryHover[i] = ease(categoryHover[i], hovered || on ? 1f : 0f, 18f, dt);
            Color tint = on ? accent : mix(WHITE_25, WHITE_45, categoryHover[i]);
            Color label = on ? Color.WHITE : mix(new Color(255, 255, 255, 90), new Color(255, 255, 255, 170), categoryHover[i]);
            float gw = Fonts.WFONT.getWidth(tab.getIconGlyph(), size);
            float iconX = ix + catPad() + (size - gw) / 2f;
            ClickGuiIcons.drawTabIcon(iconX, cy - size / 2f, size, tab, a(tint, alpha));
            text(MEDIUM, tab.getLabel(), ix + catPad() + size + r(8f), cy, catText(), a(label, alpha));
        }
    }

    private record Placed(CsModuleCard card, float x, float y) {}

    private float layout(List<Placed> out) {
        float[] columns = new float[COLUMNS];
        for (CsModuleCard card : cards.get(category)) {
            int shortest = 0;
            for (int i = 1; i < columns.length; i++) {
                if (columns[i] < columns[shortest] - 0.001f) shortest = i;
            }
            out.add(new Placed(card, shortest * pitch(), columns[shortest]));
            columns[shortest] += card.height() + cardGap();
        }
        float tallest = 0f;
        for (float value : columns) tallest = Math.max(tallest, value);
        return Math.max(0f, tallest - cardGap());
    }

    private void drawModules(float alpha, float shift, double mx, double my, float dt, boolean interactive) {
        for (CsModuleCard card : cards.get(category)) card.animate(dt);
        List<Placed> placed = new ArrayList<>();
        float content = layout(placed);
        float viewport = viewport();
        float[] s = scroll.get(category);
        float max = Math.max(0f, content - viewport);
        s[1] = Math.max(0f, Math.min(max, s[1]));
        s[0] = ease(s[0], s[1], 17f, dt);

        float cx = contentX(), cy = contentY();
        boolean live = interactive && inside(mx, my, cx, cy, columnsW(), viewport);
        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, cx, cy, columnsW(), viewport);
        float top = cy + shift - Math.round(s[0]);
        for (Placed entry : placed) {
            entry.card().render(cx + entry.x(), top + entry.y(), alpha, mx, my, dt, live);
        }
        ScissorUtil.stop(matrices);

        if (placed.isEmpty()) {
            textCenter(MEDIUM, "No modules", cx + columnsW() / 2f, cy + r(40f), r(13.5f), a(DIM, alpha));
        }
        drawScrollbar(alpha, content, viewport, s[0], mx, my, dt);
    }

    private void drawScrollbar(float alpha, float content, float viewport, float value, double mx, double my, float dt) {
        float overflow = Math.max(0f, content - viewport);
        boolean recent = System.currentTimeMillis() - lastScroll < 900L;
        boolean nearby = inside(mx, my, panelX(), panelY(), panelW(), panelH());
        scrollbarAnim = ease(scrollbarAnim, overflow > 0f && (recent || nearby) ? 1f : 0.35f, 9f, dt);
        if (overflow <= 0f) return;
        float vis = scrollbarAnim;

        float x = panelX() + panelW() - r(8f), y = contentY();
        float w = r(2f), h = viewport;
        rect(x, y, w, h, r(1f), a(WHITE_04, alpha * vis));
        float thumb = Math.max(r(30f), h * viewport / content);
        float offset = (h - thumb) * (value / overflow);
        Color color = mix(WHITE_08, UIColors.primary(), (vis - 0.35f) / 0.65f * 0.55f);
        rect(x, y + offset, w, thumb, r(1f), a(color, alpha * vis));
    }

    private void drawTooltips(float alpha, double mx, double my, boolean interactive) {
        if (!interactive) return;
        float below = barY() + barH() + r(8f);
        for (int i = 0; i < NAV_LEFT.length; i++) {
            float bx = leftX() + i * button();
            if (inside(mx, my, bx, barY(), button(), barH())) {
                tooltip(label(NAV_LEFT[i]), bx + button() / 2f, below, alpha * leftHover[i]);
                return;
            }
        }
        for (int i = 0; i < NAV_RIGHT.length; i++) {
            float bx = rightX() + i * button();
            if (inside(mx, my, bx, barY(), button(), barH())) {
                tooltip(label(NAV_RIGHT[i]), bx + button() / 2f, below, alpha * rightHover[i]);
                return;
            }
        }
    }

    private static String label(ClickGuiTab tab) {
        return tab == null ? "Modules" : tab.getLabel();
    }

    private void tooltip(String label, float centerX, float top, float alpha) {
        if (alpha <= 0.01f) return;
        float size = r(12f), w = MEDIUM.getWidth(label, size) + r(16f), h = r(22f);
        float lx = Math.max(panelX() + r(6f), Math.min(panelX() + panelW() - r(6f) - w, centerX - w / 2f));
        float ly = top - (1f - alpha) * r(4f);
        rect(lx, ly, w, h, r(6f), a(PANEL_RAISED, alpha));
        RenderUtil.RECT.drawOutline(RenderUtil.matrices(), lx, ly, w, h, r(6f), OUTLINE, a(WHITE_08, alpha));
        textCenter(MEDIUM, label, lx + w / 2f, ly + h / 2f, size, a(LABEL, alpha));
    }

    private <T> T inCosmeticsArea(java.util.function.Supplier<T> action) {
        ClickGuiLayout.setContentRect(contentX() - r(14f), contentY() - r(8f), areaW() + r(28f), areaH() + r(16f));
        try {
            return action.get();
        } finally {
            ClickGuiLayout.clearContentRect();
        }
    }


    private void switchTo(ClickGuiTab next) {
        releaseKeyboard();
        ColorPicker.INSTANCE.close();
        if (page == next) return;
        page = next;
        contentAnim = 0f;
        if (next == ClickGuiTab.CONFIGS) configs.refresh();
        if (next == ClickGuiTab.THEME) {
            themes.refresh();
            themes.resetScroll();
        }
        if (next == ClickGuiTab.COSMETICS) cosmetics.resetScroll();
        if (next == ClickGuiTab.EVENTS) events.resetScroll();
    }

    private void selectCategory(ClickGuiTab next) {
        if (page != null) switchTo(null);
        if (category != next) {
            releaseKeyboard();
            ColorPicker.INSTANCE.close();
            category = next;
            contentAnim = 0f;
        }
    }

    private void releaseKeyboard() {
        configs.releaseKeyboard();
        themes.releaseKeyboard();
        for (CsModuleCard card : allCards()) card.releaseKeyboard();
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (closing) return true;
        double mx = click.x(), my = click.y();
        int button = click.button();
        if (ColorPicker.INSTANCE.mouseClicked(mx, my)) return true;

        if (button >= 2) {
            for (CsModuleCard card : allCards()) {
                if (card.assignMouseBind(button)) return true;
            }
        }
        for (CsModuleCard card : allCards()) {
            if (!card.isBinding()) continue;
            if (button > 2) return card.mouseClicked(mx, my, button);
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                card.releaseKeyboard();
                return true;
            }
        }
        releaseKeyboard();

        for (int i = 0; i < NAV_LEFT.length; i++) {
            if (inside(mx, my, leftX() + i * button(), barY(), button(), barH())) {
                switchTo(NAV_LEFT[i]);
                return true;
            }
        }
        for (int i = 0; i < NAV_RIGHT.length; i++) {
            if (inside(mx, my, rightX() + i * button(), barY(), button(), barH())) {
                switchTo(NAV_RIGHT[i]);
                return true;
            }
        }
        float[] xs = categoryXs();
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (inside(mx, my, xs[i], barY(), categoryW(CATEGORIES[i]), barH())) {
                selectCategory(CATEGORIES[i]);
                return true;
            }
        }

        if (page == null) {
            if (inside(mx, my, contentX(), contentY(), columnsW(), viewport())) {
                for (CsModuleCard card : cards.get(category)) {
                    if (card.mouseClicked(mx, my, button)) return true;
                }
            }
        } else if (inside(mx, my, contentX(), contentY(), areaW(), areaH())) {
            if (page == ClickGuiTab.CONFIGS) {
                if (configs.mouseClicked(mx, my, button)) return true;
            } else if (page == ClickGuiTab.THEME) {
                if (themes.mouseClicked(mx, my, button)) return true;
            } else if (page == ClickGuiTab.COSMETICS && button == 0) {
                if (inCosmeticsArea(() -> cosmetics.mouseClicked(mx, my, panelX(), panelY()))) return true;
            } else if (page == ClickGuiTab.EVENTS) {
                if (events.mouseClicked(mx, my, button)) return true;
            }
        }

        if (inside(mx, my, panelX(), panelY(), panelW(), panelH())) {
            if (button == 0) {
                dragging = true;
                grabX = (float) mx - panelX();
                grabY = (float) my - panelY();
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = false;
        ColorPicker.INSTANCE.mouseReleased();
        for (CsModuleCard card : allCards()) card.mouseReleased();
        if (page == ClickGuiTab.COSMETICS) cosmetics.mouseReleased(click.x(), click.y());
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (dragging) {
            float x = Math.max(0f, Math.min(screenW() - panelW(), (float) click.x() - grabX));
            float y = Math.max(0f, Math.min(screenH() - panelH(), (float) click.y() - grabY));
            offsetX = x - (screenW() - panelW()) / 2f;
            offsetY = y - (screenH() - panelH()) / 2f;
            return true;
        }
        if (page == ClickGuiTab.COSMETICS && cosmetics.mouseDragged(click.x(), click.y())) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (page == null) {
            if (inside(mouseX, mouseY, panelX(), panelY(), panelW(), panelH())) {
                scroll.get(category)[1] -= (float) verticalAmount * r(48f);
                lastScroll = System.currentTimeMillis();
                return true;
            }
        } else if (inside(mouseX, mouseY, contentX(), contentY(), areaW(), areaH())) {
            if (page == ClickGuiTab.CONFIGS) return configs.mouseScrolled(verticalAmount);
            if (page == ClickGuiTab.THEME) return themes.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.EVENTS) return events.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.COSMETICS) {
                return inCosmeticsArea(() -> cosmetics.mouseScrolled(mouseX, mouseY, verticalAmount, panelX(), panelY()));
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (configs.keyPressed(key) || themes.keyPressed(key)) return true;
        for (CsModuleCard card : allCards()) {
            if (card.keyPressed(key, input.scancode(), input.modifiers())) return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (ColorPicker.INSTANCE.isOpen()) {
                ColorPicker.INSTANCE.close();
                return true;
            }
            beginClose();
            mc.mouseHandler.grabMouse();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = (char) input.codepoint();
        if (configs.charTyped(chr) || themes.charTyped(chr)) return true;
        for (CsModuleCard card : allCards()) {
            if (card.charTyped(chr)) return true;
        }
        return super.charTyped(input);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) { }
    @Override public boolean isPauseScreen() { return false; }
}
