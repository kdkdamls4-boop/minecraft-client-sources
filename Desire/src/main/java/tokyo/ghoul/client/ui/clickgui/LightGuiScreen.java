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
import tokyo.ghoul.api.module.setting.Setting;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsGuiPreview;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.clickgui.screen.AutoBuyPage;
import tokyo.ghoul.client.ui.clickgui.screen.ColorPicker;
import tokyo.ghoul.client.ui.clickgui.screen.ConfigsPage;
import tokyo.ghoul.client.ui.clickgui.screen.CsThemesPage;
import tokyo.ghoul.client.ui.clickgui.screen.EventsPage;
import tokyo.ghoul.client.ui.clickgui.screen.LightSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.*;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.text;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.textCenter;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.textRight;

public class LightGuiScreen extends Screen implements ClickGui, QuickImports {
    private static final ClickGuiTab[] CATEGORIES = {
            ClickGuiTab.COMBAT, ClickGuiTab.MOVEMENT, ClickGuiTab.RENDER, ClickGuiTab.PLAYER, ClickGuiTab.OTHER
    };
    private static final ClickGuiTab[] PAGES = {
            ClickGuiTab.AUTOBUY, ClickGuiTab.CONFIGS, ClickGuiTab.COSMETICS, ClickGuiTab.EVENTS, ClickGuiTab.THEME
    };
    private static final int COLUMNS = 2;
    private static final int SEARCH_LIMIT = 24;

    private static final LightGuiScreen instance = new LightGuiScreen();

    private boolean open;
    private boolean closing;
    private boolean closed;
    private float openAnim;
    private long lastFrame = System.nanoTime();

    private ClickGuiTab page;
    private ClickGuiTab category = ClickGuiTab.COMBAT;
    private float contentAnim = 1f;
    private float settingsAnim = 1f;
    private final float[] categoryHover = new float[CATEGORIES.length];
    private final float[] categoryActive = new float[CATEGORIES.length];
    private final float[] pageHover = new float[PAGES.length];
    private final float[] pageActive = new float[PAGES.length];

    private float offsetX, offsetY;
    private boolean dragging;
    private float grabX, grabY;

    private final Map<ClickGuiTab, List<Card>> cards = new EnumMap<>(ClickGuiTab.class);
    private final Map<ClickGuiTab, Card> selection = new EnumMap<>(ClickGuiTab.class);
    private final Map<ClickGuiTab, float[]> scroll = new EnumMap<>(ClickGuiTab.class);
    private final float[] searchScroll = new float[2];
    private final float[] settingsScroll = new float[2];
    private Card selected;
    private boolean cardsBuilt;

    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused;
    private float searchFocus;
    private float searchHover;

    private final ConfigsPage configs = new ConfigsPage();
    private final CsThemesPage themes = new CsThemesPage();
    private final ClickGuiCosmetics cosmetics = new ClickGuiCosmetics();
    private final EventsPage events = new EventsPage();
    private final AutoBuyPage autobuy = new AutoBuyPage();

    public LightGuiScreen() {
        super(Component.nullToEmpty(""));
    }

    public static LightGuiScreen getInstance() {
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
        if (searchFocused || configs.isTyping() || themes.isTyping() || autobuy.isTyping()) return true;
        return selected != null && selected.capturesKeyboard();
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
        if (page == ClickGuiTab.AUTOBUY) autobuy.refresh();
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
            Card card = new Card(module);
            cards.get(card.tab).add(card);
        }
        for (ClickGuiTab tab : CATEGORIES) {
            List<Card> list = cards.get(tab);
            if (!list.isEmpty()) selection.put(tab, list.get(0));
        }
        selected = selection.get(category);
        cardsBuilt = true;
    }

    private List<Card> allCards() {
        List<Card> all = new ArrayList<>();
        for (List<Card> list : cards.values()) all.addAll(list);
        return all;
    }

    private boolean searching() {
        return !search.isEmpty();
    }

    private List<Card> visibleCards() {
        if (!searching()) return cards.get(category);
        String query = search.toString().toLowerCase(Locale.ROOT).trim();
        List<Card> out = new ArrayList<>();
        for (ClickGuiTab tab : CATEGORIES) {
            for (Card card : cards.get(tab)) {
                if (card.module.getName().toLowerCase(Locale.ROOT).contains(query)
                        || card.module.getName().replace(" ", "").toLowerCase(Locale.ROOT).contains(query.replace(" ", ""))) {
                    out.add(card);
                }
            }
        }
        return out;
    }

    private float[] gridScroll() {
        return searching() ? searchScroll : scroll.get(category);
    }


    private float screenW() { return mc.getWindow().getGuiScaledWidth(); }
    private float screenH() { return mc.getWindow().getGuiScaledHeight(); }

    private static float panelW() { return r(1020f); }
    private static float panelH() { return r(660f); }
    private static float margin() { return r(32f); }
    private static float radius() { return r(22f); }

    private float panelX() {
        return Math.max(0f, Math.min(screenW() - panelW(), (screenW() - panelW()) / 2f + offsetX));
    }

    private float panelY() {
        float y = Math.max(0f, Math.min(screenH() - panelH(), (screenH() - panelH()) / 2f + offsetY));
        return y + (1f - smooth(openAnim)) * r(14f);
    }

    private float headerCY() { return panelY() + r(42f); }
    private static float navW() { return r(52f); }
    private static float navH() { return r(50f); }
    private static float navGap() { return r(12f); }
    private float navY() { return headerCY() - navH() / 2f; }
    private float dividerY() { return panelY() + r(84f); }

    private float categoriesX() {
        float total = CATEGORIES.length * navW() + navGap() + PAGES.length * navW();
        return panelX() + r(300f);
    }

    private float pagesX() { return categoriesX() + CATEGORIES.length * navW() + navGap(); }

    private static float searchW() { return r(128f); }
    private static float searchH() { return r(44f); }
    private float searchX() { return panelX() + panelW() - margin() - searchW(); }
    private float searchY() { return headerCY() - searchH() / 2f; }

    private float bodyY() { return dividerY() + r(28f); }
    private float bodyBottom() { return panelY() + panelH() - r(24f); }

    private static float cardW() { return r(252f); }
    private static float cardH() { return r(112f); }
    private static float cardGap() { return r(15f); }
    private static float cardRowGap() { return r(18f); }
    private static float gridW() { return cardW() * COLUMNS + cardGap() * (COLUMNS - 1); }
    private float gridX() { return panelX() + margin(); }
    private float splitX() { return gridX() + gridW() + r(26f); }
    private float settingsX() { return splitX() + r(30f); }
    private float settingsW() { return panelX() + panelW() - margin() - settingsX(); }
    private float settingsListY() { return bodyY() + r(64f); }

    private float pageAreaX() { return panelX() + margin(); }
    private float pageAreaW() { return panelW() - margin() * 2f; }
    private float pageAreaH() { return bodyBottom() - bodyY(); }

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
        settingsAnim = Math.min(1f, settingsAnim + dt / 0.25f);
        searchFocus = ease(searchFocus, searchFocused ? 1f : 0f, 16f, dt);

        boolean cosmeticsLayer = page == ClickGuiTab.COSMETICS;
        boolean autobuyLayer = page == ClickGuiTab.AUTOBUY;
        if (cosmeticsLayer || autobuyLayer) EvaRenderer.getInstance().beginUnder();
        else CosmeticsGuiPreview.clearTiles();

        float alpha = smooth(openAnim);
        boolean interactive = !closing && !dragging && !ColorPicker.INSTANCE.isOpen();
        rect(0f, 0f, screenW(), screenH(), 0f, new Color(0, 0, 0, (int) (70 * alpha)));

        float px = panelX(), py = panelY(), pw = panelW(), ph = panelH();
        var matrices = RenderUtil.matrices();
        RenderUtil.RECT.drawGlassShadow(matrices, px, py, pw, ph, radius(), r(16f), new Color(0, 0, 0, (int) (110 * alpha)));
        RenderUtil.BLUR_RECT.draw(matrices, px, py, pw, ph, radius(), new Color(255, 255, 255, (int) (255 * alpha)), 0f);
        rect(px, py, pw, ph, radius(), a(bg(), alpha));
        outline(px, py, pw, ph, radius(), a(tint(14), alpha));

        drawHeader(alpha, mx, my, dt, interactive);

        float appear = smooth(contentAnim);
        float contentAlpha = alpha * appear;
        float shift = (1f - appear) * r(14f);
        if (page == null) {
            drawGrid(contentAlpha, shift, mx, my, dt, interactive);
            float split = splitX(), line = dividerY();
            RenderUtil.RECT.draw(matrices, split, line, px + pw - split, py + ph - line,
                    new org.joml.Vector4f(0f, 0f, 0f, radius()), a(tint(6), alpha));
            float glowH = (py + ph - line) * 0.6f;
            Color warm = accent(26), clear = accent(0);
            RenderUtil.GRADIENT_RECT.draw(matrices, split, py + ph - glowH, px + pw - split, glowH,
                    new org.joml.Vector4f(0f, 0f, 0f, radius()), a(clear, alpha), a(clear, alpha), a(warm, alpha), a(warm, alpha));
            RenderUtil.GRADIENT_RECT.draw(matrices, px, py + ph - glowH, split - px, glowH,
                    new org.joml.Vector4f(0f, 0f, radius(), 0f), a(clear, alpha), a(clear, alpha), a(warm, alpha), a(warm, alpha));
            rect(split, line, r(1f), py + ph - line, 0f, a(tint(20), alpha));
            drawSettings(contentAlpha, mx, my, dt, interactive);
        } else if (cosmeticsLayer) {
            ScissorUtil.start(matrices, pageAreaX(), bodyY(), pageAreaW(), pageAreaH());
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
            float ay = bodyY() + shift;
            if (page == ClickGuiTab.CONFIGS) {
                configs.render(pageAreaX(), ay, pageAreaW(), pageAreaH(), search.toString(), contentAlpha, pmx, pmy, dt);
            } else if (page == ClickGuiTab.THEME) {
                themes.render(pageAreaX(), ay, pageAreaW(), pageAreaH(), contentAlpha, pmx, pmy, dt);
            } else if (page == ClickGuiTab.EVENTS) {
                events.render(pageAreaX(), ay, pageAreaW(), pageAreaH(), search.toString(), contentAlpha, pmx, pmy, dt);
            }
        }
        if (autobuyLayer) {
            // панель уходит в under-слой, чтобы ванильные иконки предметов легли поверх неё
            try {
                double pmx = interactive ? mx : -1, pmy = interactive ? my : -1;
                autobuy.render(pageAreaX(), bodyY() + shift, pageAreaW(), pageAreaH(), search.toString(), contentAlpha, pmx, pmy, dt);
            } finally {
                EvaRenderer.getInstance().endUnder();
            }
            autobuy.renderItems(context, contentAlpha);
        }

        drawTooltips(alpha, mx, my, interactive);
        ColorPicker.INSTANCE.render(screenW(), screenH(), mx, my, alpha, dt);
    }


    private void drawHeader(float alpha, double mx, double my, float dt, boolean interactive) {
        float cy = headerCY();
        float logo = r(26f), lx = panelX() + margin();
        glow(lx + logo / 2f, cy, logo * 1.5f, logo * 1.5f, accent((int) (55 * alpha)));
        ClickGuiIcons.drawLogo(lx, cy - logo / 2f, logo, a(accent(), alpha), a(secondary(255), alpha));
        float size = r(20f), tx = lx + logo + r(10f);
        INTER.drawGradientText(RenderUtil.matrices(), "Desire Beta", tx, cy - size / 2f - lift(INTER, size), size,
                a(accent(), alpha), a(Color.WHITE, alpha), 6f);

        boolean modules = page == null && !searching();
        drawNavGroup(CATEGORIES, categoriesX(), categoryHover, categoryActive, alpha, mx, my, dt, interactive, modules);
        drawNavGroup(PAGES, pagesX(), pageHover, pageActive, alpha, mx, my, dt, interactive, true);
        drawSearch(alpha, mx, my, dt, interactive);

        rect(panelX(), dividerY(), panelW(), r(1f), 0f, a(tint(20), alpha));
    }

    private void drawNavGroup(ClickGuiTab[] tabs, float x, float[] hover, float[] active, float alpha,
                              double mx, double my, float dt, boolean interactive, boolean categoryGroup) {
        float y = navY(), w = tabs.length * navW(), h = navH();
        rect(x, y, w, h, r(12f), a(new Color(0, 0, 0, 110), alpha));
        float size = r(19f);
        for (int i = 0; i < tabs.length; i++) {
            ClickGuiTab tab = tabs[i];
            float bx = x + i * navW(), cx = bx + navW() / 2f, cy = y + h / 2f;
            boolean on = tab.isCategory() ? categoryGroup && page == null && tab == category : page == tab;
            boolean hovered = interactive && inside(mx, my, bx, y, navW(), h);
            hover[i] = ease(hover[i], hovered ? 1f : 0f, 18f, dt);
            active[i] = ease(active[i], on ? 1f : 0f, 16f, dt);

            Color tint = mix(mix(WHITE, Color.WHITE, hover[i]), accent(), active[i]);
            if (tab == ClickGuiTab.AUTOBUY) {
                icon(ClickGuiIcons.AUTOBUY, cx, cy - r(1f), size * 0.92f, a(tint, alpha));
            } else {
                float iw = Fonts.WFONT.getWidth(tab.getIconGlyph(), size);
                ClickGuiIcons.drawTabIcon(cx - iw / 2f, cy - size / 2f - r(1f), size, tab, a(tint, alpha));
            }
            if (active[i] > 0.01f) {
                float lw = r(22f) * (0.3f + 0.7f * active[i]);
                float ly = y + h - r(6f), lh = r(1.8f);
                glow(cx, ly + lh / 2f, lw * 1.9f, r(10f), a(accent(120), alpha * active[i]));
                rect(cx - lw / 2f, ly, lw, lh, lh / 2f, a(accent(), alpha * active[i]));
            }
        }
    }

    private void drawSearch(float alpha, double mx, double my, float dt, boolean interactive) {
        float x = searchX(), y = searchY(), w = searchW(), h = searchH(), cy = y + h / 2f;
        searchHover = ease(searchHover, interactive && inside(mx, my, x, y, w, h) ? 1f : 0f, 16f, dt);
        rect(x, y, w, h, r(12f), a(mix(tint(6), tint(11), searchHover), alpha));
        outline(x, y, w, h, r(12f), a(accent(120), alpha * searchFocus));

        float iconSize = r(17f);
        icon(SEARCH, x + r(25f), cy, iconSize, a(mix(GRAY, accent(), searchFocus), alpha));
        float size = r(19f), tx = x + r(42f), maxW = w - r(42f) - r(12f);
        String query = search.toString();
        if (query.isEmpty() && !searchFocused) {
            text(INTER, "Search", tx, cy, size, a(GRAY, alpha));
            return;
        }
        String shown = query;
        while (!shown.isEmpty() && INTER.getWidth(shown, size) > maxW) shown = shown.substring(1);
        text(INTER, shown, tx, cy, size, a(WHITE, alpha));
        if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
            float caret = tx + INTER.getWidth(shown, size) + r(1f);
            rect(caret, cy - r(7f), r(1.2f), r(14f), 0f, a(accent(), alpha));
        }
    }


    private float gridViewport() {
        return bodyBottom() - bodyY();
    }

    private float gridContent(int count) {
        int rows = (count + COLUMNS - 1) / COLUMNS;
        return Math.max(0f, rows * (cardH() + cardRowGap()) - cardRowGap());
    }

    private void drawGrid(float alpha, float shift, double mx, double my, float dt, boolean interactive) {
        List<Card> list = visibleCards();
        float viewport = gridViewport();
        float[] s = gridScroll();
        float content = gridContent(list.size());
        float max = Math.max(0f, content - viewport);
        s[1] = Math.max(0f, Math.min(max, s[1]));
        s[0] = ease(s[0], s[1], 17f, dt);

        float gx = gridX(), gy = bodyY();
        boolean live = interactive && inside(mx, my, gx, gy, gridW(), viewport);
        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, gx - r(2f), gy, gridW() + r(4f), viewport);
        float top = gy + shift - Math.round(s[0]);
        for (int i = 0; i < list.size(); i++) {
            Card card = list.get(i);
            float cx = gx + (i % COLUMNS) * (cardW() + cardGap());
            float cy = top + (i / COLUMNS) * (cardH() + cardRowGap());
            card.animate(dt, card == selected);
            if (cy > gy + viewport || cy + cardH() < gy) {
                card.place(cx, cy);
                continue;
            }
            card.render(cx, cy, alpha, mx, my, dt, live);
        }
        ScissorUtil.stop(matrices);

        if (list.isEmpty()) {
            String empty = searching() ? "Ничего не найдено" : "Нет модулей";
            textCenter(INTER, empty, gx + gridW() / 2f, gy + r(40f), r(15f), a(MUTE, alpha));
        }
        if (max > 0f) {
            float bx = gx + gridW() + r(8f), bw = r(2.5f);
            float thumb = Math.max(r(36f), viewport * viewport / content);
            float offset = (viewport - thumb) * (s[0] / max);
            rect(bx, gy + offset, bw, thumb, bw / 2f, a(tint(22), alpha));
        }
    }

    private void drawSettings(float alpha, double mx, double my, float dt, boolean interactive) {
        float x = settingsX(), w = settingsW(), top = bodyY();
        Card card = selected;
        if (card == null) {
            textCenter(INTER, "Выберите модуль", x + w / 2f, top + r(40f), r(15f), a(MUTE, alpha));
            return;
        }
        float appear = smooth(settingsAnim);
        alpha *= appear;
        float shift = (1f - appear) * r(10f);

        float cy = top + r(10f) + shift;
        float cell = r(7f), gap = r(3f);
        for (int i = 0; i < 4; i++) {
            float sx = x + (i % 2) * (cell + gap), sy = cy - cell - gap / 2f + (i / 2) * (cell + gap);
            rect(sx, sy, cell, cell, r(1.5f), a(accent(), alpha));
        }
        float size = r(20f), tx = x + cell * 2f + gap + r(10f);
        String path = card.tab.getLabel() + " / ";
        text(INTER, path, tx, cy, size, a(WHITE, alpha));
        float nx = tx + INTER.getWidth(path, size);
        text(INTER, ellipsize(INTER, card.module.getName(), size, x + w - nx), nx, cy, size, a(accent(), alpha));

        float iy = cy + r(29f), ring = r(15f);
        icon(INFO, x + ring / 2f, iy, ring, a(MUTE, alpha));
        text(INTER, ellipsize(INTER, card.description(), r(17f), w - ring - r(10f)), x + ring + r(10f), iy, r(17f),
                a(DARK, alpha));

        card.animateRows(dt);
        float listY = settingsListY(), viewport = bodyBottom() - listY;
        float content = card.settingsHeight(w);
        float max = Math.max(0f, content - viewport);
        settingsScroll[1] = Math.max(0f, Math.min(max, settingsScroll[1]));
        settingsScroll[0] = ease(settingsScroll[0], settingsScroll[1], 17f, dt);

        boolean live = interactive && settingsAnim > 0.9f && inside(mx, my, x, listY, w, viewport);
        double lmx = live ? mx : -1, lmy = live ? my : -1;
        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x - r(4f), listY, w + r(8f), viewport);
        float sy = listY + shift - Math.round(settingsScroll[0]);
        for (LightSetting setting : card.rows()) {
            if (setting.visible <= 0.01f) continue;
            float h = setting.height(w) * setting.visible;
            if (sy + h >= listY && sy <= listY + viewport) {
                boolean rowLive = setting.visible > 0.95f;
                setting.render(x, sy, w, alpha * setting.visible, rowLive ? lmx : -1, rowLive ? lmy : -1, dt);
            }
            sy += h;
        }
        ScissorUtil.stop(matrices);

        if (max > 0f) {
            float bx = x + w + r(9f), bw = r(2.5f);
            float thumb = Math.max(r(30f), viewport * viewport / content);
            float offset = (viewport - thumb) * (settingsScroll[0] / max);
            rect(bx, listY + offset, bw, thumb, bw / 2f, a(tint(22), alpha));
        }
    }

    private void drawTooltips(float alpha, double mx, double my, boolean interactive) {
        if (!interactive) return;
        float below = navY() + navH() + r(8f);
        for (int i = 0; i < CATEGORIES.length; i++) {
            float bx = categoriesX() + i * navW();
            if (inside(mx, my, bx, navY(), navW(), navH())) {
                tooltip(CATEGORIES[i].getLabel(), bx + navW() / 2f, below, alpha * categoryHover[i]);
                return;
            }
        }
        for (int i = 0; i < PAGES.length; i++) {
            float bx = pagesX() + i * navW();
            if (inside(mx, my, bx, navY(), navW(), navH())) {
                tooltip(PAGES[i].getLabel(), bx + navW() / 2f, below, alpha * pageHover[i]);
                return;
            }
        }
    }

    private void tooltip(String label, float centerX, float top, float alpha) {
        if (alpha <= 0.01f) return;
        float size = r(13f), w = INTER.getWidth(label, size) + r(18f), h = r(25f);
        float lx = centerX - w / 2f, ly = top - (1f - alpha) * r(4f);
        rect(lx, ly, w, h, r(7f), a(new Color(34, 32, 27), alpha));
        outline(lx, ly, w, h, r(7f), a(tint(20), alpha));
        textCenter(INTER, label, centerX, ly + h / 2f, size, a(WHITE, alpha));
    }

    private <T> T inCosmeticsArea(java.util.function.Supplier<T> action) {
        ClickGuiLayout.setContentRect(pageAreaX() - r(14f), bodyY() - r(8f), pageAreaW() + r(28f), pageAreaH() + r(16f));
        try {
            return action.get();
        } finally {
            ClickGuiLayout.clearContentRect();
        }
    }


    private void select(Card card) {
        if (card == null || card == selected) return;
        if (selected != null) selected.releaseKeyboard();
        ColorPicker.INSTANCE.close();
        selected = card;
        selection.put(card.tab, card);
        settingsScroll[0] = settingsScroll[1] = 0f;
        settingsAnim = 0f;
    }

    private void switchPage(ClickGuiTab next) {
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
        if (next == ClickGuiTab.AUTOBUY) {
            autobuy.refresh();
            autobuy.resetScroll();
        }
    }

    private void selectCategory(ClickGuiTab next) {
        boolean changed = page != null || category != next || searching();
        if (page != null) switchPage(null);
        search.setLength(0);
        searchFocused = false;
        if (!changed) return;
        releaseKeyboard();
        ColorPicker.INSTANCE.close();
        category = next;
        contentAnim = 0f;
        Card remembered = selection.get(next);
        if (remembered != null) select(remembered);
    }

    private void releaseKeyboard() {
        searchFocused = false;
        configs.releaseKeyboard();
        themes.releaseKeyboard();
        autobuy.releaseKeyboard();
        for (Card card : allCards()) card.releaseKeyboard();
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (closing) return true;
        double mx = click.x(), my = click.y();
        int button = click.button();
        if (ColorPicker.INSTANCE.mouseClicked(mx, my)) return true;

        if (button >= 2 && selected != null && selected.assignMouse(button)) return true;
        releaseKeyboard();

        if (inside(mx, my, searchX(), searchY(), searchW(), searchH())) {
            if (button == 1) search.setLength(0);
            searchFocused = true;
            return true;
        }
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (inside(mx, my, categoriesX() + i * navW(), navY(), navW(), navH())) {
                selectCategory(CATEGORIES[i]);
                return true;
            }
        }
        for (int i = 0; i < PAGES.length; i++) {
            if (inside(mx, my, pagesX() + i * navW(), navY(), navW(), navH())) {
                switchPage(page == PAGES[i] ? null : PAGES[i]);
                return true;
            }
        }

        if (page == null) {
            if (inside(mx, my, gridX(), bodyY(), gridW(), gridViewport())) {
                for (Card card : visibleCards()) {
                    if (!card.contains(mx, my)) continue;
                    card.click(mx, my, button);
                    return true;
                }
            }
            if (selected != null && inside(mx, my, settingsX() - r(4f), settingsListY(), settingsW() + r(8f),
                    bodyBottom() - settingsListY())) {
                if (selected.mouseClicked(mx, my, button)) return true;
            }
        } else if (inside(mx, my, pageAreaX(), bodyY(), pageAreaW(), pageAreaH())) {
            if (page == ClickGuiTab.CONFIGS) {
                if (configs.mouseClicked(mx, my, button)) return true;
            } else if (page == ClickGuiTab.THEME) {
                if (themes.mouseClicked(mx, my, button)) return true;
            } else if (page == ClickGuiTab.COSMETICS && button == 0) {
                if (inCosmeticsArea(() -> cosmetics.mouseClicked(mx, my, panelX(), panelY()))) return true;
            } else if (page == ClickGuiTab.EVENTS) {
                if (events.mouseClicked(mx, my, button)) return true;
            } else if (page == ClickGuiTab.AUTOBUY) {
                if (autobuy.mouseClicked(mx, my, button)) return true;
            }
        }

        if (inside(mx, my, panelX(), panelY(), panelW(), panelH())) {
            if (button == 0 && my < dividerY()) {
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
        for (Card card : allCards()) card.mouseReleased();
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
            if (inside(mouseX, mouseY, settingsX() - r(10f), bodyY(), settingsW() + r(20f), bodyBottom() - bodyY())) {
                settingsScroll[1] -= (float) verticalAmount * r(44f);
                return true;
            }
            if (inside(mouseX, mouseY, panelX(), panelY(), panelW(), panelH())) {
                gridScroll()[1] -= (float) verticalAmount * r(52f);
                return true;
            }
        } else if (inside(mouseX, mouseY, pageAreaX(), bodyY(), pageAreaW(), pageAreaH())) {
            if (page == ClickGuiTab.CONFIGS) return configs.mouseScrolled(verticalAmount);
            if (page == ClickGuiTab.THEME) return themes.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.EVENTS) return events.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.AUTOBUY) return autobuy.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.COSMETICS) {
                return inCosmeticsArea(() -> cosmetics.mouseScrolled(mouseX, mouseY, verticalAmount, panelX(), panelY()));
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                if (ctrl) search.setLength(0);
                else search.deleteCharAt(search.length() - 1);
                searchScroll[0] = searchScroll[1] = 0f;
            } else if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
            } else if (key == GLFW.GLFW_KEY_V && ctrl) {
                String clip = GLFW.glfwGetClipboardString(mc.getWindow().handle());
                if (clip != null) {
                    for (char c : clip.toCharArray()) {
                        if (!Character.isISOControl(c) && search.length() < SEARCH_LIMIT) search.append(c);
                    }
                }
            }
            return true;
        }
        if (configs.keyPressed(key) || themes.keyPressed(key)) return true;
        if (page == ClickGuiTab.AUTOBUY && autobuy.keyPressed(key)) return true;
        if (selected != null && selected.keyPressed(key, input.scancode(), input.modifiers())) return true;
        if (key == GLFW.GLFW_KEY_F && ctrl) {
            searchFocused = true;
            return true;
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
        if (searchFocused) {
            if (!Character.isISOControl(chr) && search.length() < SEARCH_LIMIT) {
                search.append(chr);
                searchScroll[0] = searchScroll[1] = 0f;
            }
            return true;
        }
        if (configs.charTyped(chr) || themes.charTyped(chr)) return true;
        if (page == ClickGuiTab.AUTOBUY && autobuy.charTyped(chr)) return true;
        if (selected != null && selected.charTyped(chr)) return true;
        return super.charTyped(input);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) { }
    @Override public boolean isPauseScreen() { return false; }


    private final class Card {
        private final Module module;
        private final ClickGuiTab tab;
        private final List<LightSetting> settings = new ArrayList<>();
        private final LightSetting.ModuleBind bind;
        private final List<LightSetting> rows = new ArrayList<>();

        private float enable;
        private float hover;
        private float selectAnim;
        private float gearHover;
        private float x, y;
        private String bindLabel;
        private int bindLabelFor = Integer.MIN_VALUE;

        Card(Module module) {
            this.module = module;
            this.tab = ClickGuiTab.of(module.getCategory());
            for (Setting<?> setting : module.getSettings()) {
                LightSetting row = LightSetting.of(setting);
                if (row != null) settings.add(row);
            }
            this.bind = new LightSetting.ModuleBind(module);
            rows.addAll(settings);
            rows.add(bind);
            this.enable = module.isEnabled() ? 1f : 0f;
        }

        List<LightSetting> rows() {
            return rows;
        }

        String description() {
            String text = module.getDescription();
            if (text != null && !text.isEmpty()) return text;
            int count = settings.size();
            return count == 0 ? "Нет настроек" : count + " " + plural(count);
        }

        private static String plural(int n) {
            int mod10 = n % 10, mod100 = n % 100;
            if (mod10 == 1 && mod100 != 11) return "настройка";
            if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) return "настройки";
            return "настроек";
        }

        float settingsHeight(float width) {
            float h = 0f;
            for (LightSetting row : rows) h += row.height(width) * row.visible;
            return h;
        }

        void animate(float dt, boolean isSelected) {
            enable = ease(enable, module.isEnabled() ? 1f : 0f, 16f, dt);
            selectAnim = ease(selectAnim, isSelected ? 1f : 0f, 16f, dt);
        }

        void animateRows(float dt) {
            for (LightSetting row : rows) row.visible = ease(row.visible, row.isShown() ? 1f : 0f, 14f, dt);
        }

        void place(float x, float y) {
            this.x = x;
            this.y = y;
        }

        private float titleCY() { return y + r(30f); }
        private float toggleW() { return r(27f); }
        private float toggleH() { return r(15f); }
        private float toggleX() { return x + cardW() - r(18f) - toggleW(); }
        private float gearSize() { return r(17f); }
        private float gearCX() { return toggleX() - r(12f) - gearSize() / 2f; }

        private boolean hasSettings() {
            return !settings.isEmpty();
        }

        private boolean bound() {
            return module.hasBind() && module.getBind() != -1 && module.getBind() != -999;
        }

        private String bindText() {
            if (bind.isListening()) return "...";
            if (bindLabel == null || bindLabelFor != module.getBind()) {
                String name = KeyStorage.getBind(module.getBind());
                name = name == null ? "?" : name.toUpperCase(Locale.ROOT);
                if (name.startsWith("MOUSE")) name = "M" + name.replaceAll("\\D", "");
                bindLabel = name.length() > 4 ? name.substring(0, 4) : name;
                bindLabelFor = module.getBind();
            }
            return bindLabel;
        }

        void render(float x, float y, float alpha, double mx, double my, float dt, boolean interactive) {
            place(x, y);
            float w = cardW(), h = cardH(), e = smooth(enable), sel = smooth(selectAnim);
            hover = ease(hover, interactive && inside(mx, my, x, y, w, h) ? 1f : 0f, 16f, dt);
            boolean gearHovered = interactive && hasSettings() && inside(mx, my, gearCX() - r(10f), titleCY() - r(10f), r(20f), r(20f));
            gearHover = ease(gearHover, gearHovered ? 1f : 0f, 18f, dt);

            Color top = mix(tint(8), tint(13), hover);
            Color bottom = mix(tint(4), tint(8), hover);
            RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), x, y, w, h, r(12f),
                    a(top, alpha), a(top, alpha), a(bottom, alpha), a(bottom, alpha));
            if (e > 0.01f) glowUp(x, y, w, h, r(12f), a(accent(24), alpha * e));

            float pad = r(20f), titleSize = r(20f), cy = titleCY();
            float limit = (hasSettings() ? gearCX() - gearSize() / 2f : toggleX()) - r(10f);
            boolean showBind = bound() || bind.isListening();
            String bindText = showBind ? bindText() : "";
            float badgeFont = r(14f), badgeW = showBind ? INTER.getWidth(bindText, badgeFont) + r(10f) : 0f;
            float badgeWidth = Math.max(r(21f), badgeW);
            String title = ellipsize(INTER, module.getName(), titleSize,
                    limit - x - pad - (showBind ? badgeWidth + r(7f) : 0f));
            text(INTER, title, x + pad, cy, titleSize, a(mix(WHITE, accent(), e), alpha));
            if (showBind) {
                float bx = x + pad + INTER.getWidth(title, titleSize) + r(8f), bh = r(21f);
                rect(bx, cy - bh / 2f, badgeWidth, bh, r(5f), a(accent(46), alpha));
                textCenter(INTER, bindText, bx + badgeWidth / 2f, cy, badgeFont, a(accent(), alpha));
            }

            if (hasSettings()) {
                Color gear = mix(mix(GRAY, WHITE, gearHover), accent(), sel * 0.85f);
                icon(GEAR_BOLD, gearCX(), cy, gearSize(), a(gear, alpha));
            }
            drawToggle(alpha, e, cy);

            Color desc = mix(GRAY, WHITE, e);
            float size = r(17.5f), line = r(21f), dy = y + r(62f);
            for (String row : wrap(INTER, description(), size, w - pad * 2f, 2)) {
                text(INTER, row, x + pad, dy, size, a(desc, alpha));
                dy += line;
            }
        }

        private void drawToggle(float alpha, float e, float cy) {
            float tx = toggleX(), tw = toggleW(), th = toggleH(), ty = cy - th / 2f;
            rect(tx, ty, tw, th, th / 3.2f, a(new Color(48, 46, 41), alpha));
            RenderUtil.RECT.drawOutline(RenderUtil.matrices(), tx, ty, tw, th, th / 3.2f, r(0.2f),
                    a(tint(16), alpha));
            float k = r(9f), inset = (th - k) / 2f;
            float kx = tx + inset + (tw - k - inset * 2f) * e;
            rect(kx, cy - k / 2f, k, k, k / 3.2f, a(mix(new Color(238, 236, 230), accent(), e), alpha));
        }

        boolean contains(double mx, double my) {
            return inside(mx, my, x, y, cardW(), cardH());
        }

        void click(double mx, double my, int button) {
            boolean onGear = hasSettings() && inside(mx, my, gearCX() - r(10f), titleCY() - r(10f), r(20f), r(20f));
            if (button == 0 && !onGear) {
                module.toggle();
            } else if (button == 0 || button == 1) {
                select(this);
            } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                select(this);
                bind.listen();
            }
        }

        boolean mouseClicked(double mx, double my, int button) {
            for (LightSetting row : rows) {
                if (row.visible > 0.95f && row.mouseClicked(mx, my, button)) return true;
            }
            return false;
        }

        boolean assignMouse(int button) {
            for (LightSetting row : rows) if (row.assignMouse(button)) return true;
            return false;
        }

        void mouseReleased() {
            for (LightSetting row : rows) row.mouseReleased();
        }

        boolean keyPressed(int key, int scancode, int modifiers) {
            for (LightSetting row : rows) {
                if (row.capturesKeyboard() && row.keyPressed(key, scancode, modifiers)) return true;
            }
            return false;
        }

        boolean charTyped(char chr) {
            for (LightSetting row : rows) {
                if (row.capturesKeyboard() && row.charTyped(chr)) return true;
            }
            return false;
        }

        boolean capturesKeyboard() {
            for (LightSetting row : rows) if (row.capturesKeyboard()) return true;
            return false;
        }

        void releaseKeyboard() {
            for (LightSetting row : rows) row.releaseKeyboard();
        }
    }
}
