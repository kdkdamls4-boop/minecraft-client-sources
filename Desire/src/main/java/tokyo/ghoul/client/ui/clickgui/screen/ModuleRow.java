package tokyo.ghoul.client.ui.clickgui.screen;

import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.setting.Setting;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class ModuleRow {
    private final Module module;
    private final List<CardSetting> settings = new ArrayList<>();
    private boolean expanded;
    private boolean binding;

    private float enableAnim;
    private float expandAnim;
    private float bindAnim;
    private float hover;
    private float iconHover;

    private float lastX, lastY, lastW;
    private String bindLabel;
    private int bindLabelFor = Integer.MIN_VALUE;

    public ModuleRow(Module module) {
        this.module = module;
        for (Setting<?> setting : module.getSettings()) {
            CardSetting card = CardSetting.of(setting);
            if (card != null) settings.add(card);
        }
        this.enableAnim = module.isEnabled() ? 1f : 0f;
    }

    public Module module() {
        return module;
    }

    public String name() {
        return module.getName();
    }

    public static float rowH() {
        return r(38f);
    }

    private static float inset() {
        return r(15f);
    }

    private static float contentW(float width) {
        return width - inset() * 2f;
    }

    private float settingsH(float width) {
        float h = r(2f);
        for (CardSetting setting : settings) h += setting.height(contentW(width)) * setting.visible;
        return h + r(8f);
    }

    public float height(float width) {
        return rowH() + settingsH(width) * smooth(expandAnim);
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    public void animate(float dt) {
        enableAnim = ease(enableAnim, module.isEnabled() ? 1f : 0f, 16f, dt);
        expandAnim = ease(expandAnim, expanded ? 1f : 0f, 14f, dt);
        if (Math.abs(expandAnim - (expanded ? 1f : 0f)) < 0.002f) expandAnim = expanded ? 1f : 0f;
        bindAnim = ease(bindAnim, binding ? 1f : 0f, 14f, dt);
        for (CardSetting setting : settings) {
            setting.visible = ease(setting.visible, setting.isShown() ? 1f : 0f, 14f, dt);
        }
    }

    private boolean hasBind() {
        return module.hasBind() && module.getBind() != -1 && module.getBind() != -999;
    }

    private String bindText() {
        if (binding) return "...";
        if (bindLabel == null || bindLabelFor != module.getBind()) {
            bindLabel = KeyStorage.getBind(module.getBind());
            bindLabelFor = module.getBind();
        }
        return bindLabel;
    }

    public void render(float x, float y, float w, float alpha, double mx, double my, float dt, boolean interactive) {
        lastX = x;
        lastY = y;
        lastW = w;
        float h = height(w);
        float e = enableAnim, b = bindAnim;
        float pulse = binding ? 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 160.0) : 0f;

        hover = ease(hover, interactive && inside(mx, my, x, y, w, rowH()) ? 1f : 0f, 16f, dt);
        iconHover = ease(iconHover, interactive && onIcon(mx, my) ? 1f : 0f, 18f, dt);

        Color fill = mix(mix(ROW, ROW_ON, e), ROW_HOVER, Math.max(hover * 0.45f, b * pulse));
        rect(x, y, w, h, r(7f), a(fill, alpha));

        float cy = y + rowH() / 2f;
        float right = x + w - r(13f);
        if (!settings.isEmpty()) {
            float size = r(17f);
            Color color = mix(mix(ICON_DIM, new Color(200, 200, 206), iconHover), ROW_TEXT_ON, expandAnim);
            icon(expanded ? SLIDERS_OPEN : SLIDERS, right - size / 2f, cy, size, a(color, alpha));
            right -= size + r(8f);
        }
        if (hasBind() || binding) {
            String label = bindText();
            right -= CardSetting.badgeW(label);
            CardSetting.badge(right, cy, label, true, alpha * (binding ? 0.55f + 0.45f * pulse : 1f));
            right -= r(8f);
        }

        float nameSize = r(16.5f);
        String name = ellipsize(MEDIUM, name(), nameSize, right - (x + inset()));
        Color nameColor = mix(mix(ROW_TEXT, ROW_TEXT_ON, e), Color.WHITE, b);
        text(MEDIUM, name, x + inset(), cy, nameSize, a(nameColor, alpha));

        if (expandAnim <= 0.002f) return;

        rect(x + inset(), y + rowH() - r(0.5f), contentW(w), r(1f), 0f,
                a(new Color(255, 255, 255, 10), alpha * expandAnim));

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x, y + rowH(), w, h - rowH());
        float settingsAlpha = alpha * smooth(Math.min(1f, expandAnim * 1.4f));
        float sy = y + rowH() + r(2f);
        float cw = contentW(w);
        for (CardSetting setting : settings) {
            if (setting.visible <= 0.01f) continue;
            boolean live = interactive && setting.visible > 0.95f;
            setting.render(x + inset(), sy, cw, settingsAlpha * setting.visible, live ? mx : -1, live ? my : -1, dt);
            sy += setting.height(cw) * setting.visible;
        }
        ScissorUtil.stop(matrices);
    }

    private boolean onIcon(double mx, double my) {
        float size = r(17f);
        return !settings.isEmpty() && inside(mx, my, lastX + lastW - r(13f) - size - r(4f), lastY, size + r(8f), rowH());
    }

    public boolean contains(double mx, double my) {
        return inside(mx, my, lastX, lastY, lastW, height(lastW));
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

        if (inside(mx, my, lastX, lastY, lastW, rowH())) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                binding = !binding;
            } else if (button == 1 || (button == 0 && onIcon(mx, my))) {
                if (!settings.isEmpty()) expanded = !expanded;
                if (!expanded) releaseKeyboard();
            } else if (button == 0) {
                binding = false;
                module.toggle();
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
