package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.setting.Setting;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.client.ui.clickgui.ClickGuiIcons;
import tokyo.ghoul.client.ui.clickgui.ClickGuiTab;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class CsModuleCard {
    private static final Color WHITE_02 = white(0.02f);
    private static final Color WHITE_035 = white(0.035f);
    private static final Color WHITE_04 = white(0.04f);
    private static final Color WHITE_07 = white(0.07f);
    private static final Color WHITE_12 = white(0.12f);
    private static final Identifier KEYBOARD_SMALL =
            Identifier.fromNamespaceAndPath("desire", "textures/menu/gui/keyboard_small.svg");
    private static final Color WHITE_25 = white(0.25f);
    private static final Color WHITE_45 = white(0.45f);
    private static final Color PANEL = new Color(24, 23, 23);

    private final Module module;
    private final ClickGuiTab tab;
    private final List<CardSetting> settings = new ArrayList<>();
    private boolean expanded;
    private boolean binding;

    private float enableAnim;
    private float expandAnim;
    private float hover;
    private float badgeHover;

    private float lastX, lastY;
    private String bindLabel;
    private int bindLabelFor = Integer.MIN_VALUE;

    public CsModuleCard(Module module) {
        this.module = module;
        this.tab = ClickGuiTab.of(module.getCategory());
        for (Setting<?> setting : module.getSettings()) {
            CardSetting card = CardSetting.of(setting);
            if (card != null) settings.add(card);
        }
        this.enableAnim = module.isEnabled() ? 1f : 0f;
    }

    public Module module() {
        return module;
    }

    public ClickGuiTab tab() {
        return tab;
    }

    public String name() {
        return module.getName();
    }

    static Color white(float opacity) {
        return new Color(255, 255, 255, Math.round(opacity * 255f));
    }

    static Color accent() {
        return UIColors.primary();
    }


    public static float width() {
        return r(292f);
    }

    public static float headerH() {
        return r(58f);
    }

    private static float inset() {
        return r(13f);
    }

    private static float contentW() {
        return width() - inset() * 2f;
    }

    private float settingsH() {
        float h = r(4f);
        for (CardSetting setting : settings) h += setting.height(contentW()) * setting.visible;
        return h + r(12f);
    }

    public float height() {
        return headerH() + settingsH() * smooth(expandAnim);
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    public void animate(float dt) {
        enableAnim = ease(enableAnim, module.isEnabled() ? 1f : 0f, 16f, dt);
        expandAnim = ease(expandAnim, expanded ? 1f : 0f, 13f, dt);
        if (Math.abs(expandAnim - (expanded ? 1f : 0f)) < 0.002f) expandAnim = expanded ? 1f : 0f;
        for (CardSetting setting : settings) {
            setting.visible = ease(setting.visible, setting.isShown() ? 1f : 0f, 14f, dt);
        }
    }

    private float headerCenter() { return lastY + r(22f); }

    private float toggleX() { return lastX + width() - r(13f) - toggleW(); }
    private float toggleY() { return headerCenter() - toggleH() / 2f; }
    private static float toggleW() { return r(27f); }
    private static float toggleH() { return r(19f); }

    private static float bindTextSize() { return r(12f); }
    private static float keyboardW() { return r(21f); }
    private static float keyboardH() { return r(14f); }

    private float badgeW() {
        String label = binding ? "..." : bindText();
        float textW = Math.max(SEMIBOLD.getWidth(label, bindTextSize()), SEMIBOLD.getWidth("...", bindTextSize()));
        return r(9f) + textW + r(6f) + keyboardW() + r(9f);
    }

    private float badgeX() { return toggleX() - r(8f) - badgeW(); }
    private float badgeY() { return headerCenter() - badgeH() / 2f; }
    private static float badgeH() { return r(23f); }

    private boolean hasBind() {
        return module.hasBind() && module.getBind() != -1 && module.getBind() != -999;
    }

    private String bindText() {
        if (binding) return (System.currentTimeMillis() / 350L) % 2L == 0L ? "..." : "";
        if (!hasBind()) return "-";
        if (bindLabel == null || bindLabelFor != module.getBind()) {
            String name = KeyStorage.getBind(module.getBind());
            name = name == null ? "?" : name.toUpperCase(Locale.ROOT);
            if (name.startsWith("MOUSE")) name = "M" + name.replaceAll("\\D", "");
            bindLabel = name.length() > 3 ? name.substring(0, 3) : name;
            bindLabelFor = module.getBind();
        }
        return bindLabel;
    }

    private String subtitle() {
        int count = settings.size();
        String suffix = count == 0 ? "No settings" : count + (count == 1 ? " setting" : " settings");
        return tab.getLabel() + "  ·  " + suffix;
    }


    public void render(float x, float y, float alpha, double mx, double my, float dt, boolean interactive) {
        lastX = x;
        lastY = y;
        float w = width(), h = height();
        float e = smooth(enableAnim);

        hover = ease(hover, interactive && inside(mx, my, x, y, w, h) ? 1f : 0f, 16f, dt);
        badgeHover = ease(badgeHover,
                binding || (interactive && inside(mx, my, badgeX(), badgeY(), badgeW(), badgeH())) ? 1f : 0f, 18f, dt);

        rect(x, y, w, h, r(15f), a(mix(WHITE_02, WHITE_035, hover), alpha));

        Color accent = accent();
        float titleSize = r(15f), titleTop = headerCenter() - titleSize / 2f;
        Color iconColor = mix(a(accent, 0.55f), accent, e);
        ClickGuiIcons.drawTabIcon(x + r(13f), titleTop, titleSize, tab, a(iconColor, alpha));

        float titleX = x + r(13f) + Fonts.WFONT.getWidth(tab.getIconGlyph(), titleSize) + r(7f);
        String title = ellipsize(MEDIUM, name(), titleSize, badgeX() - titleX - r(6f));
        text(MEDIUM, title, titleX, headerCenter(), titleSize, a(mix(new Color(200, 200, 204), Color.WHITE, e), alpha));
        text(REGULAR, subtitle(), x + r(13f), y + r(41f), r(12f), a(WHITE_25, alpha));

        drawBadge(alpha);
        drawToggle(alpha, e, interactive && inside(mx, my, toggleX(), toggleY(), toggleW(), toggleH()));

        if (expandAnim <= 0.002f) return;

        rect(x + inset(), y + headerH() - r(0.5f), contentW(), r(1f), 0f, a(white(0.04f), alpha * expandAnim));

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x, y + headerH(), w, h - headerH());
        float settingsAlpha = alpha * smooth(Math.max(0f, Math.min(1f, (expandAnim - 0.25f) / 0.6f)));
        float sy = y + headerH() + r(4f);
        float cw = contentW();
        for (CardSetting setting : settings) {
            if (setting.visible <= 0.01f) continue;
            boolean live = interactive && setting.visible > 0.95f && expandAnim > 0.95f;
            setting.render(x + inset(), sy, cw, settingsAlpha * setting.visible, live ? mx : -1, live ? my : -1, dt);
            sy += setting.height(cw) * setting.visible;
        }
        ScissorUtil.stop(matrices);
    }

    private void drawBadge(float alpha) {
        float bx = badgeX(), by = badgeY(), bw = badgeW(), bh = badgeH(), k = badgeHover;
        Color accent = accent();
        Color background = binding ? mix(WHITE_07, accent, k) : mix(WHITE_07, WHITE_12, k);
        rect(bx, by, bw, bh, r(7f), a(background, alpha));

        Color tint = binding ? mix(WHITE_45, PANEL, k) : mix(white(0.55f), white(0.85f), k);
        float cy = by + bh / 2f;
        text(SEMIBOLD, bindText(), bx + r(9f), cy, bindTextSize(), a(tint, alpha));
        ClickGuiIcons.draw(bx + bw - r(9f) - keyboardW(), cy - keyboardH() / 2f, keyboardW(), keyboardH(),
                KEYBOARD_SMALL, a(tint, alpha));
    }

    private void drawToggle(float alpha, float e, boolean hovered) {
        float tx = toggleX(), ty = toggleY(), tw = toggleW(), th = toggleH();
        rect(tx, ty, tw, th, th / 2f, a(mix(WHITE_04, accent(), e), alpha));
        float knob = r(13f), insetK = r(3f);
        float travel = tw - knob - insetK * 2f;
        rect(tx + insetK + travel * e, ty + insetK, knob, knob, knob / 2f,
                a(hovered ? Color.WHITE : white(0.94f), alpha));
    }


    public boolean contains(double mx, double my) {
        return inside(mx, my, lastX, lastY, width(), height());
    }

    public boolean assignMouseBind(int button) {
        for (CardSetting setting : settings) {
            if (setting instanceof CardSetting.Bind bind && bind.assignMouse(button)) return true;
        }
        return false;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (binding && button > 2) {
            module.setBind(-100 + button);
            binding = false;
            return true;
        }
        if (!contains(mx, my)) return false;

        if (inside(mx, my, toggleX(), toggleY(), toggleW(), toggleH()) && button == 0) {
            binding = false;
            module.toggle();
            return true;
        }
        if (inside(mx, my, badgeX(), badgeY(), badgeW(), badgeH())) {
            if (button == 0) {
                binding = !binding;
            } else if (button == 1) {
                binding = false;
                module.setBind(-999);
            }
            return true;
        }
        if (inside(mx, my, lastX, lastY, width(), headerH())) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                binding = !binding;
            } else if (button == 0 || button == 1) {
                binding = false;
                if (settings.isEmpty()) {
                    if (button == 0) module.toggle();
                } else {
                    expanded = !expanded;
                    if (!expanded) releaseKeyboard();
                }
            }
            return true;
        }
        if (expandAnim > 0.95f) {
            for (CardSetting setting : settings) {
                if (setting.visible > 0.95f && setting.mouseClicked(mx, my, button)) return true;
            }
        }
        return true;
    }

    public void mouseReleased() {
        for (CardSetting setting : settings) setting.mouseReleased();
    }

    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (binding) {
            binding = false;
            if (key == GLFW.GLFW_KEY_ESCAPE) return true;
            module.setBind(key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -999 : key);
            return true;
        }
        for (CardSetting setting : settings) {
            if (setting.capturesKeyboard() && setting.keyPressed(key, scancode, modifiers)) return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        for (CardSetting setting : settings) {
            if (setting.capturesKeyboard() && setting.charTyped(chr)) return true;
        }
        return false;
    }

    public boolean capturesKeyboard() {
        if (binding) return true;
        for (CardSetting setting : settings) {
            if (setting.capturesKeyboard()) return true;
        }
        return false;
    }

    public boolean isBinding() {
        return binding;
    }

    public void releaseKeyboard() {
        binding = false;
        for (CardSetting setting : settings) setting.releaseKeyboard();
    }
}
