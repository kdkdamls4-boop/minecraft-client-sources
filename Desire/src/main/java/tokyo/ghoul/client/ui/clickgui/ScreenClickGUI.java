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
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsGuiPreview;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.client.ui.clickgui.screen.ColorPicker;
import tokyo.ghoul.client.ui.clickgui.screen.ConfigsPage;
import tokyo.ghoul.client.ui.clickgui.screen.DropdownPanel;
import tokyo.ghoul.client.ui.clickgui.screen.EventsPage;
import tokyo.ghoul.client.ui.clickgui.screen.ModuleRow;
import tokyo.ghoul.client.ui.clickgui.screen.ThemesPage;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public class ScreenClickGUI extends Screen implements ClickGui, QuickImports {
    private static final ClickGuiTab[] CATEGORIES = {
            ClickGuiTab.COMBAT, ClickGuiTab.MOVEMENT, ClickGuiTab.RENDER, ClickGuiTab.PLAYER, ClickGuiTab.OTHER
    };
    private static final ClickGuiTab[] PAGES = {
            ClickGuiTab.CONFIGS, ClickGuiTab.THEME, ClickGuiTab.COSMETICS, ClickGuiTab.EVENTS
    };
    private static final int LEFT_BUTTONS = 2;

    private static final ScreenClickGUI instance = new ScreenClickGUI();

    private boolean open;
    private boolean closing;
    private boolean closed;

    private ClickGuiTab page;
    private ClickGuiTab shownPage;
    private float pageAnim;
    private float pageContentAnim = 1f;

    private float openAnim;
    private long lastFrame = System.nanoTime();

    private final List<DropdownPanel> panels = new ArrayList<>();
    private boolean panelsBuilt;

    private final ConfigsPage configs = new ConfigsPage();
    private final ThemesPage themes = new ThemesPage();
    private final ClickGuiCosmetics cosmetics = new ClickGuiCosmetics();
    private final EventsPage events = new EventsPage();

    private final StringBuilder search = new StringBuilder();
    private boolean searchFocused;
    private float searchFocus;
    private float searchHover;
    private final float[] buttonHover = new float[PAGES.length];
    private final float[] buttonActive = new float[PAGES.length];

    public ScreenClickGUI() {
        super(Component.nullToEmpty(""));
    }

    public static ScreenClickGUI getInstance() {
        return instance;
    }

    public boolean isOpen() {
        return open;
    }

    public ClickGuiTab getTab() {
        return shownPage != null && pageAnim > 0.01f ? shownPage : ClickGuiTab.COMBAT;
    }

    public boolean isTextInputFocused() {
        if (searchFocused || configs.isTyping() || themes.isTyping()) return true;
        for (DropdownPanel panel : panels) {
            for (ModuleRow row : panel.rows()) if (row.capturesKeyboard()) return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (closed) return;
        beginClose();
    }

    private void beginClose() {
        searchFocused = false;
        ColorPicker.INSTANCE.close();
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
        buildPanels();
        configs.refresh();
        Minecraft.getInstance().mouseHandler.releaseMouse();
        super.init();
    }

    private void buildPanels() {
        if (panelsBuilt) return;
        List<Module> modules = new ArrayList<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (!module.isHidden()) modules.add(module);
        }
        modules.sort(java.util.Comparator.comparing(m -> m.getName().toLowerCase(Locale.ROOT)));
        for (ClickGuiTab tab : CATEGORIES) {
            List<ModuleRow> rows = new ArrayList<>();
            for (Module module : modules) {
                if (ClickGuiTab.of(module.getCategory()) == tab) rows.add(new ModuleRow(module));
            }
            panels.add(new DropdownPanel(tab, rows));
        }
        panelsBuilt = true;
    }

    private Iterable<ModuleRow> allRows() {
        List<ModuleRow> rows = new ArrayList<>();
        for (DropdownPanel panel : panels) rows.addAll(panel.rows());
        return rows;
    }


    private float screenW() {
        return mc.getWindow().getGuiScaledWidth();
    }

    private float screenH() {
        return mc.getWindow().getGuiScaledHeight();
    }

    private float panelGap() {
        return r(30f);
    }

    private float panelsW() {
        return CATEGORIES.length * DropdownPanel.width() + (CATEGORIES.length - 1) * panelGap();
    }

    private float panelX(int index) {
        return screenW() / 2f - panelsW() / 2f + index * (DropdownPanel.width() + panelGap());
    }

    private float searchGap() {
        return r(14f);
    }

    private float panelsY() {
        float block = DropdownPanel.height() + searchGap() + searchH();
        return Math.max(r(10f), (screenH() - block) / 2f - r(10f));
    }

    private float searchW() {
        return r(220f);
    }

    private float searchH() {
        return r(40f);
    }

    private float buttonGap() {
        return r(8f);
    }

    private float searchX() {
        return screenW() / 2f - searchW() / 2f;
    }

    private float searchY() {
        return panelsY() + DropdownPanel.height() + searchGap() + (1f - openAnim) * r(12f);
    }

    private float buttonX(int index) {
        float step = searchH() + buttonGap();
        if (index < LEFT_BUTTONS) return searchX() - (LEFT_BUTTONS - index) * step;
        return searchX() + searchW() + buttonGap() + (index - LEFT_BUTTONS) * step;
    }

    private float pageW() {
        return r(660f);
    }

    private float pageX() {
        return screenW() / 2f - pageW() / 2f;
    }

    private float pageY() {
        return panelsY() + (1f - smooth(pageAnim)) * r(14f);
    }

    private float areaX() {
        return pageX() + r(24f);
    }

    private float areaY() {
        return pageY() + DropdownPanel.headerH() + r(10f);
    }

    private float areaW() {
        return pageW() - r(48f);
    }

    private float areaH() {
        return DropdownPanel.height() - DropdownPanel.headerH() - r(32f);
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
        pageAnim = ease(pageAnim, page != null ? 1f : 0f, page != null ? 14f : 18f, dt);
        if (page == null && pageAnim < 0.01f) shownPage = null;
        pageContentAnim = Math.min(1f, pageContentAnim + dt / 0.35f);
        searchFocus = ease(searchFocus, searchFocused ? 1f : 0f, 16f, dt);

        boolean cosmeticsLayer = shownPage == ClickGuiTab.COSMETICS;
        if (cosmeticsLayer) EvaRenderer.getInstance().beginUnder();
        else CosmeticsGuiPreview.clearTiles();

        float alpha = smooth(openAnim);
        rect(0f, 0f, screenW(), screenH(), 0f, new Color(0, 0, 0, (int) (60 * alpha)));

        boolean pickerOpen = ColorPicker.INSTANCE.isOpen();
        float pagesT = smooth(pageAnim);
        float panelsAlpha = alpha * (1f - pagesT);
        if (panelsAlpha > 0.003f) {
            String query = search.toString();
            for (int i = 0; i < panels.size(); i++) {
                float local = smooth(Math.max(0f, Math.min(1f, (openAnim - i * 0.05f) / 0.8f)));
                float py = panelsY() + (1f - local) * r(18f) + pagesT * r(10f);
                DropdownPanel panel = panels.get(i);
                panel.filter(query);
                panel.render(panelX(i), py, panelsAlpha * local, mx, my, dt, page == null && !pickerOpen);
            }
        }

        if (shownPage != null) drawPage(context, alpha * pagesT, mx, my, dt, mouseX, mouseY, cosmeticsLayer);

        drawSearchBar(alpha, mx, my, dt);
        ColorPicker.INSTANCE.render(screenW(), screenH(), mx, my, alpha, dt);
    }

    private void drawPage(GuiGraphicsExtractor context, float alpha, double mx, double my, float dt,
                          int mouseX, int mouseY, boolean cosmeticsLayer) {
        float x = pageX(), y = pageY(), w = pageW(), h = DropdownPanel.height();
        glass(x, y, w, h, r(10f), alpha);

        float cy = y + DropdownPanel.headerH() / 2f + r(1f);
        float iconSize = r(15f);
        ClickGuiIcons.drawTabIcon(x + r(18f), cy - iconSize / 2f, iconSize, shownPage, a(HEADER_TEXT, alpha));
        rect(x + r(43f), cy - r(7f), r(1.2f), r(14f), r(0.6f), a(DIVIDER, alpha));
        text(MEDIUM, shownPage.getLabel(), x + r(53f), cy, r(16f), a(HEADER_TEXT, alpha));
        textRight(MEDIUM, "Esc", x + w - r(18f), cy, r(12f), a(DIM, alpha));

        float pageAlpha = alpha * smooth(Math.min(1f, pageContentAnim / 0.6f));
        if (shownPage == ClickGuiTab.CONFIGS) {
            configs.render(areaX(), areaY() + (1f - pageAlpha) * r(8f), areaW(), areaH(), search.toString(),
                    pageAlpha, mx, my, dt);
        } else if (shownPage == ClickGuiTab.THEME) {
            themes.render(areaX(), areaY() + (1f - pageAlpha) * r(8f), areaW(), areaH(), search.toString(),
                    pageAlpha, mx, my, dt);
        } else if (cosmeticsLayer) {
            var matrices = RenderUtil.matrices();
            ScissorUtil.start(matrices, areaX(), areaY(), areaW(), areaH());
            try {
                inCosmeticsArea(() -> {
                    cosmetics.renderLayered(context, x, y, pageAlpha, mouseX, mouseY);
                    return null;
                });
            } catch (Throwable t) {
                EvaRenderer.getInstance().endUnder();
                throw t;
            } finally {
                ScissorUtil.stop(matrices);
            }
        } else if (shownPage == ClickGuiTab.EVENTS) {
            events.render(areaX(), areaY() + (1f - pageAlpha) * r(8f), areaW(), areaH(), search.toString(),
                    pageAlpha, mx, my, dt);
        }
    }

    private <T> T inCosmeticsArea(java.util.function.Supplier<T> action) {
        ClickGuiLayout.setContentRect(areaX() - r(14f), areaY() - r(8f), areaW() + r(28f), areaH() + r(16f));
        try {
            return action.get();
        } finally {
            ClickGuiLayout.clearContentRect();
        }
    }

    private void drawSearchBar(float alpha, double mx, double my, float dt) {
        float sx = searchX(), sy = searchY(), sw = searchW(), sh = searchH(), cy = sy + sh / 2f;
        searchHover = ease(searchHover, inside(mx, my, sx, sy, sw, sh) ? 1f : 0f, 16f, dt);
        glass(sx, sy, sw, sh, r(10f), alpha);
        float outline = Math.max(searchFocus, searchHover * 0.4f);
        if (outline > 0.01f) {
            RenderUtil.RECT.drawOutline(RenderUtil.matrices(), sx, sy, sw, sh, r(10f), OUTLINE,
                    a(new Color(255, 255, 255, 60), alpha * outline));
        }

        float logo = r(18f);
        Color logoColor = a(mix(new Color(170, 170, 176), Color.WHITE, searchFocus), alpha);
        ClickGuiIcons.drawLogo(sx + r(14f), cy - logo / 2f, logo, logoColor, logoColor);
        rect(sx + r(42f), cy - r(7f), r(1.2f), r(14f), r(0.6f), a(DIVIDER, alpha));

        float size = r(14f), tx = sx + r(53f), maxW = sw - r(53f) - r(40f);
        String query = search.toString();
        if (query.isEmpty() && !searchFocused) {
            String hint = page == ClickGuiTab.CONFIGS ? "Search configs..."
                    : page == ClickGuiTab.THEME ? "Search themes..."
                    : page == ClickGuiTab.EVENTS ? "Search events..." : "Search...";
            text(MEDIUM, hint, tx, cy, size, a(ICON_DIM, alpha));
        } else {
            String shown = query;
            while (!shown.isEmpty() && MEDIUM.getWidth(shown, size) > maxW) shown = shown.substring(1);
            text(MEDIUM, shown, tx, cy, size, a(ROW_TEXT_ON, alpha));
            if (searchFocused && System.currentTimeMillis() % 1000 < 500) {
                float caretX = tx + MEDIUM.getWidth(shown, size) + r(1f);
                rect(caretX, cy - r(7f), r(0.9f), r(14f), 0f, a(Color.WHITE, alpha));
            }
        }
        icon(SEARCH, sx + sw - r(21f), cy, r(14f), a(mix(ICON_DIM, Color.WHITE, searchFocus), alpha));

        for (int i = 0; i < PAGES.length; i++) {
            ClickGuiTab t = PAGES[i];
            float bx = buttonX(i), bs = sh;
            boolean hovered = inside(mx, my, bx, sy, bs, bs);
            buttonHover[i] = ease(buttonHover[i], hovered ? 1f : 0f, 16f, dt);
            buttonActive[i] = ease(buttonActive[i], page == t ? 1f : 0f, 16f, dt);
            float act = buttonActive[i], hov = buttonHover[i];

            glass(bx, sy, bs, bs, r(10f), alpha);
            if (act > 0.01f) rect(bx, sy, bs, bs, r(10f), a(new Color(255, 255, 255, 22), alpha * act));
            if (hov > 0.01f) {
                RenderUtil.RECT.drawOutline(RenderUtil.matrices(), bx, sy, bs, bs, r(10f), OUTLINE * 2f,
                        a(new Color(255, 255, 255, 60), alpha * Math.max(hov * 0.5f, act)));
            }
            float iconSize = r(16f);
            Color iconColor = mix(mix(ICON_DIM, new Color(200, 200, 206), hov), Color.WHITE, act);
            ClickGuiIcons.drawTabIcon(bx + bs / 2f - iconSize / 2f, sy + bs / 2f - iconSize / 2f, iconSize, t,
                    a(iconColor, alpha));

            if (hov > 0.01f) {
                String label = t.getLabel();
                float ls = r(12f), lw = MEDIUM.getWidth(label, ls) + r(16f), lh = r(22f);
                float lx = bx + bs / 2f - lw / 2f, ly = sy + bs + r(8f) - (1f - hov) * r(4f);
                glass(lx, ly, lw, lh, r(6f), alpha * hov);
                textCenter(MEDIUM, label, bx + bs / 2f, ly + lh / 2f, ls, a(LABEL, alpha * hov));
            }
        }
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private void togglePage(ClickGuiTab next) {
        releaseKeyboard();
        ColorPicker.INSTANCE.close();
        if (page == next) {
            page = null;
            return;
        }
        if (shownPage != next) pageContentAnim = 0f;
        page = next;
        shownPage = next;
        if (next == ClickGuiTab.CONFIGS) configs.refresh();
        if (next == ClickGuiTab.THEME) {
            themes.refresh();
            themes.resetScroll();
        }
        if (next == ClickGuiTab.COSMETICS) cosmetics.resetScroll();
        if (next == ClickGuiTab.EVENTS) events.resetScroll();
    }

    private void closePage() {
        releaseKeyboard();
        ColorPicker.INSTANCE.close();
        page = null;
    }

    private void releaseKeyboard() {
        searchFocused = false;
        configs.releaseKeyboard();
        themes.releaseKeyboard();
        for (ModuleRow row : allRows()) row.releaseKeyboard();
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (closing) return true;
        double mx = click.x(), my = click.y();
        if (ColorPicker.INSTANCE.mouseClicked(mx, my)) return true;

        if (click.button() >= 2) {
            for (ModuleRow row : allRows()) {
                if (row.assignMouseBind(click.button())) return true;
            }
        }

        for (ModuleRow row : allRows()) {
            if (!row.isBinding()) continue;
            if (click.button() > 2) return row.mouseClicked(mx, my, click.button());
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                row.releaseKeyboard();
                return true;
            }
        }

        releaseKeyboard();
        if (inside(mx, my, searchX(), searchY(), searchW(), searchH())) {
            if (click.button() == 1) {
                search.setLength(0);
            }
            searchFocused = true;
            return true;
        }
        for (int i = 0; i < PAGES.length; i++) {
            if (inside(mx, my, buttonX(i), searchY(), searchH(), searchH())) {
                togglePage(PAGES[i]);
                return true;
            }
        }

        if (page == null) {
            for (DropdownPanel panel : panels) {
                if (panel.mouseClicked(mx, my, click.button())) return true;
            }
            return super.mouseClicked(click, doubled);
        }

        if (!inside(mx, my, pageX(), pageY(), pageW(), DropdownPanel.height())) {
            closePage();
            return true;
        }
        if (inside(mx, my, areaX(), areaY(), areaW(), areaH())) {
            if (page == ClickGuiTab.CONFIGS) {
                if (configs.mouseClicked(mx, my, click.button())) return true;
            } else if (page == ClickGuiTab.THEME) {
                if (themes.mouseClicked(mx, my, click.button())) return true;
            } else if (page == ClickGuiTab.COSMETICS && click.button() == 0) {
                if (inCosmeticsArea(() -> cosmetics.mouseClicked(mx, my, pageX(), pageY()))) return true;
            } else if (page == ClickGuiTab.EVENTS) {
                if (events.mouseClicked(mx, my, click.button())) return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        ColorPicker.INSTANCE.mouseReleased();
        for (ModuleRow row : allRows()) row.mouseReleased();
        if (page == ClickGuiTab.COSMETICS) cosmetics.mouseReleased(click.x(), click.y());
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        if (page == ClickGuiTab.COSMETICS && cosmetics.mouseDragged(click.x(), click.y())) return true;
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (page == null) {
            for (DropdownPanel panel : panels) {
                if (panel.mouseScrolled(mouseX, mouseY, verticalAmount)) return true;
            }
        } else if (inside(mouseX, mouseY, areaX(), areaY(), areaW(), areaH())) {
            if (page == ClickGuiTab.CONFIGS) return configs.mouseScrolled(verticalAmount);
            if (page == ClickGuiTab.THEME) return themes.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.EVENTS) return events.mouseScrolled(mouseX, mouseY, verticalAmount);
            if (page == ClickGuiTab.COSMETICS) {
                return inCosmeticsArea(() -> cosmetics.mouseScrolled(mouseX, mouseY, verticalAmount, pageX(), pageY()));
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) search.setLength(0);
                else search.deleteCharAt(search.length() - 1);
            } else if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
            } else if (key == GLFW.GLFW_KEY_V && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = GLFW.glfwGetClipboardString(mc.getWindow().handle());
                if (clip != null) {
                    for (char c : clip.toCharArray()) if (!Character.isISOControl(c) && search.length() < 24) search.append(c);
                }
            }
            return true;
        }
        if (configs.keyPressed(key) || themes.keyPressed(key)) return true;
        for (ModuleRow row : allRows()) {
            if (row.keyPressed(key, input.scancode(), input.modifiers())) return true;
        }
        if (key == GLFW.GLFW_KEY_F && (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0) {
            searchFocused = true;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (ColorPicker.INSTANCE.isOpen()) {
                ColorPicker.INSTANCE.close();
                return true;
            }
            if (page != null) {
                closePage();
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
            if (!Character.isISOControl(chr) && search.length() < 24) search.append(chr);
            return true;
        }
        if (configs.charTyped(chr) || themes.charTyped(chr)) return true;
        for (ModuleRow row : allRows()) {
            if (row.charTyped(chr)) return true;
        }
        return super.charTyped(input);
    }

    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) { }
    @Override public boolean isPauseScreen() { return false; }
}
