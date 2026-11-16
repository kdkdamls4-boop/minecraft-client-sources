package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.setting.BindSetting;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.module.setting.Setting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.system.backend.KeyStorage;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.*;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.text;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.textCenter;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.textRight;

public abstract class LightSetting {
    protected final Setting<?> setting;
    protected float x, y, w;
    public float visible;

    protected LightSetting(Setting<?> setting) {
        this.setting = setting;
        this.visible = isShown() ? 1f : 0f;
    }

    public static LightSetting of(Setting<?> setting) {
        if (setting instanceof SliderSetting s) return new Slider(s);
        if (setting instanceof ModeSetting s) return new Mode(s);
        if (setting instanceof MultiBooleanSetting s) return new Multi(s);
        if (setting instanceof BooleanSetting s) return new Checkbox(s);
        if (setting instanceof BindSetting s) return new Bind(s);
        if (setting instanceof TextSetting s) return new Text(s);
        if (setting instanceof RunSetting s) return new Button(s);
        if (setting instanceof ColorSetting s) return new ColorValue(s);
        return null;
    }

    public String name() {
        return setting.getName();
    }

    public boolean isShown() {
        return setting == null || setting.isVisible();
    }

    public abstract float height(float width);

    public final void render(float x, float y, float w, float alpha, double mx, double my, float dt) {
        this.x = x;
        this.y = y;
        this.w = w;
        draw(alpha, mx, my, dt);
    }

    protected abstract void draw(float alpha, double mx, double my, float dt);

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public void mouseReleased() {}

    public boolean keyPressed(int key, int scancode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr) {
        return false;
    }

    public boolean capturesKeyboard() {
        return false;
    }

    public void releaseKeyboard() {}

    public boolean assignMouse(int button) {
        return false;
    }

    protected static float labelSize() {
        return r(18.5f);
    }

    protected float labelY() {
        return y + r(14f);
    }

    protected float right() {
        return x + w;
    }

    protected void label(float alpha, float reserved) {
        text(INTER, ellipsize(INTER, name(), labelSize(), w - reserved), x, labelY(), labelSize(), a(WHITE, alpha));
    }

    protected void value(String value, float alpha) {
        textRight(INTER, value, right(), labelY(), labelSize(), a(accent(), alpha));
    }

    protected static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }


    public static final class Slider extends LightSetting {
        private final SliderSetting slider;
        private float shown;
        private float hover;
        private boolean dragging;

        Slider(SliderSetting slider) {
            super(slider);
            this.slider = slider;
            this.shown = current();
        }

        private float current() {
            Float v = slider.getValue();
            return v == null ? slider.getMin() : v;
        }

        @Override
        public float height(float width) {
            return r(56f);
        }

        private float trackY() {
            return y + r(39f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            if (dragging) setFromMouse(mx);
            float min = slider.getMin(), max = slider.getMax();
            shown = ease(shown, current(), 22f, dt);
            hover = ease(hover, dragging || inside(mx, my, x, trackY() - r(8f), w, r(16f)) ? 1f : 0f, 16f, dt);

            String value = format(current());
            value(value, alpha);
            label(alpha, INTER.getWidth(value, labelSize()) + r(12f));

            float t = max > min ? Math.max(0f, Math.min(1f, (shown - min) / (max - min))) : 0f;
            float th = r(4f), ty = trackY();
            rect(x, ty - th / 2f, w, th, th / 2f, a(tint(18), alpha));
            rect(x, ty - th / 2f, w * t, th, th / 2f, a(accent(), alpha));
            float k = r(12f) + r(2f) * hover;
            float kx = Math.max(x + k / 2f, Math.min(right() - k / 2f, x + w * t));
            rect(kx - k / 2f, ty - k / 2f, k, k, k / 2f, a(Color.WHITE, alpha));
        }

        private void setFromMouse(double mx) {
            float min = slider.getMin(), max = slider.getMax(), step = slider.getStep();
            float t = (float) Math.max(0.0, Math.min(1.0, (mx - x) / Math.max(1f, w)));
            float raw = min + (max - min) * t;
            float snapped = new BigDecimal(Math.round(raw / step) * (double) step)
                    .setScale(decimals(step), java.math.RoundingMode.HALF_UP).floatValue();
            slider.setValue(Math.max(min, Math.min(max, snapped)));
        }

        private static int decimals(float step) {
            String s = BigDecimal.valueOf(step).stripTrailingZeros().toPlainString();
            int dot = s.indexOf('.');
            return dot < 0 ? 0 : Math.min(3, s.length() - dot - 1);
        }

        private String format(float v) {
            int d = decimals(slider.getStep());
            return d == 0 ? String.valueOf(Math.round(v)) : String.format(Locale.ROOT, "%." + d + "f", v);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my, x - r(4f), trackY() - r(9f), w + r(8f), r(18f))) return false;
            dragging = true;
            setFromMouse(mx);
            return true;
        }

        @Override
        public void mouseReleased() {
            dragging = false;
        }
    }


    /** Options laid out as an equal-width segmented grid; shared by Mode and Multi. */
    abstract static class Segmented extends LightSetting {
        private float[] anim = new float[0];
        private float[] hover = new float[0];

        Segmented(Setting<?> setting) {
            super(setting);
        }

        abstract List<String> options();

        abstract boolean selected(int index);

        abstract void click(int index);

        abstract String summary();

        abstract Color container();

        static float segH() {
            return r(40f);
        }

        static float segFont() {
            return r(18f);
        }

        static float top() {
            return r(30f);
        }

        int columns(float width) {
            List<String> options = options();
            int n = options.size();
            if (n == 0) return 1;
            float widest = 0f;
            for (String option : options) widest = Math.max(widest, INTER.getWidth(option, segFont()) + r(24f));
            int fit = Math.max(1, Math.min(n, (int) (width / widest)));
            int rows = (n + fit - 1) / fit;
            return (n + rows - 1) / rows;
        }

        int rows(float width) {
            int n = options().size();
            return n == 0 ? 0 : (n + columns(width) - 1) / columns(width);
        }

        @Override
        public float height(float width) {
            return top() + rows(width) * segH() + r(18f);
        }

        float segX(int index) {
            return x + (index % columns(w)) * segW();
        }

        float segY(int index) {
            return y + top() + (index / columns(w)) * segH();
        }

        float segW() {
            return w / columns(w);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            List<String> options = options();
            if (anim.length != options.size()) {
                anim = new float[options.size()];
                hover = new float[options.size()];
                for (int i = 0; i < anim.length; i++) anim[i] = selected(i) ? 1f : 0f;
            }
            String summary = summary();
            value(summary, alpha);
            label(alpha, INTER.getWidth(summary, labelSize()) + r(12f));
            if (options.isEmpty()) return;

            rect(x, y + top(), w, rows(w) * segH(), r(8f), a(container(), alpha));
            float sw = segW(), sh = segH();
            for (int i = 0; i < options.size(); i++) {
                float sx = segX(i), sy = segY(i);
                anim[i] = ease(anim[i], selected(i) ? 1f : 0f, 18f, dt);
                hover[i] = ease(hover[i], inside(mx, my, sx, sy, sw, sh) ? 1f : 0f, 18f, dt);
                float on = anim[i], pad = r(2f);
                if (on > 0.01f) {
                    float bx = sx + pad, by = sy + pad, bw = sw - pad * 2f, bh = sh - pad * 2f;
                    rect(bx, by, bw, bh, r(7f), a(accent(34), alpha * on));
                    glowUp(bx, by, bw, bh, r(7f), a(accent(70), alpha * on));
                    glow(bx + bw / 2f, by + bh, bw * 0.9f, r(16f), a(accent(70), alpha * on));
                    openOutline(bx, by, bw, bh, r(7f), a(accent(), alpha * on));
                }
                Color color = mix(mix(GRAY, WHITE, hover[i] * 0.6f), accent(), on);
                String label = ellipsize(INTER, options.get(i), segFont(), sw - r(10f));
                textCenter(INTER, label, sx + sw / 2f, sy + sh / 2f, segFont(), a(color, alpha));
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) return false;
            for (int i = 0; i < options().size(); i++) {
                if (inside(mx, my, segX(i), segY(i), segW(), segH())) {
                    click(i);
                    return true;
                }
            }
            return false;
        }
    }


    public static final class Mode extends Segmented {
        private final ModeSetting mode;

        Mode(ModeSetting mode) {
            super(mode);
            this.mode = mode;
        }

        @Override
        List<String> options() {
            return mode.getModes();
        }

        @Override
        boolean selected(int index) {
            return options().get(index).equals(mode.getValue());
        }

        @Override
        void click(int index) {
            mode.setValue(options().get(index));
        }

        @Override
        String summary() {
            return mode.getValue() == null ? "" : mode.getValue();
        }

        @Override
        Color container() {
            return accent(30);
        }
    }


    public static final class Multi extends Segmented {
        private final MultiBooleanSetting multi;

        Multi(MultiBooleanSetting multi) {
            super(multi);
            this.multi = multi;
        }

        @Override
        List<String> options() {
            return multi.getAllNames();
        }

        @Override
        boolean selected(int index) {
            return Boolean.TRUE.equals(multi.getValue().get(index).getValue());
        }

        @Override
        void click(int index) {
            multi.getValue().get(index).toggle();
        }

        @Override
        String summary() {
            return multi.getList().size() + "/" + multi.getValue().size();
        }

        @Override
        Color container() {
            return tint(12);
        }
    }


    public static final class Checkbox extends LightSetting {
        private final BooleanSetting bool;
        private float anim;
        private float hover;

        Checkbox(BooleanSetting bool) {
            super(bool);
            this.bool = bool;
            this.anim = on() ? 1f : 0f;
        }

        private boolean on() {
            return Boolean.TRUE.equals(bool.getValue());
        }

        @Override
        public float height(float width) {
            return r(40f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            anim = ease(anim, on() ? 1f : 0f, 18f, dt);
            hover = ease(hover, inside(mx, my, x, y, w, height(w)) ? 1f : 0f, 18f, dt);
            float s = r(24f);
            label(alpha, s + r(12f));
            float bx = right() - s, by = labelY() - s / 2f;
            rect(bx, by, s, s, r(6f), a(mix(accent(46 + Math.round(16 * hover)), accent(), anim), alpha));
            float dot = r(9f) * anim;
            if (dot > 0.2f) rect(bx + s / 2f - dot / 2f, by + s / 2f - dot / 2f, dot, dot, dot / 2f, a(Color.WHITE, alpha));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my, x, y, w, height(w))) return false;
            bool.setValue(!on());
            return true;
        }
    }


    /** Shared look for key binds: "◎ Press key" when listening or unbound, key name otherwise. */
    abstract static class KeyRow extends LightSetting {
        protected boolean listening;
        private String keyLabel;
        private int keyLabelFor = Integer.MIN_VALUE;
        private float hover;

        KeyRow(Setting<?> setting) {
            super(setting);
        }

        abstract int key();

        abstract void set(int key);

        private boolean bound() {
            return key() != -999 && key() != -1;
        }

        @Override
        public float height(float width) {
            return r(44f);
        }

        private String keyText() {
            if (listening) return "Press key";
            if (!bound()) return "None";
            if (keyLabel == null || keyLabelFor != key()) {
                String name = KeyStorage.getBind(key());
                keyLabel = name == null ? "?" : name.toUpperCase(Locale.ROOT);
                keyLabelFor = key();
            }
            return keyLabel;
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            hover = ease(hover, inside(mx, my, x, y, w, height(w)) ? 1f : 0f, 18f, dt);
            String key = keyText();
            float size = labelSize(), ring = r(19f);
            float tw = INTER.getWidth(key, size);
            label(alpha, tw + ring + r(20f));

            float pulse = listening ? 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 160.0) : 1f;
            Color color = listening || bound() ? accent() : mix(MUTE, GRAY, hover);
            float tx = right() - tw, cy = labelY();
            text(INTER, key, tx, cy, size, a(color, alpha * pulse));
            float rx = tx - r(7f) - ring;
            icon(KEYBOARD, rx + ring / 2f, cy, ring, a(color, alpha * pulse));
        }

        @Override
        public boolean assignMouse(int button) {
            if (!listening || button < 2) return false;
            set(-100 + button);
            listening = false;
            return true;
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (assignMouse(button)) return true;
            if (!inside(mx, my, x, y, w, height(w))) return false;
            if (button == 1) {
                set(-999);
                listening = false;
            } else if (button == 0) {
                listening = !listening;
            }
            return true;
        }

        @Override
        public boolean keyPressed(int key, int scancode, int modifiers) {
            if (!listening) return false;
            listening = false;
            if (key == GLFW.GLFW_KEY_ESCAPE) return true;
            set(key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -999 : key);
            return true;
        }

        @Override
        public boolean capturesKeyboard() {
            return listening;
        }

        @Override
        public void releaseKeyboard() {
            listening = false;
        }
    }


    public static final class Bind extends KeyRow {
        private final BindSetting bind;

        Bind(BindSetting bind) {
            super(bind);
            this.bind = bind;
        }

        @Override
        int key() {
            Integer v = bind.getValue();
            return v == null ? -999 : v;
        }

        @Override
        void set(int key) {
            bind.setValue(key);
        }
    }


    /** The module's own toggle key, shown as the last row of the settings panel. */
    public static final class ModuleBind extends KeyRow {
        private final Module module;

        public ModuleBind(Module module) {
            super(null);
            this.module = module;
        }

        @Override
        public String name() {
            return "Bind";
        }

        @Override
        int key() {
            return module.getBind();
        }

        @Override
        void set(int key) {
            module.setBind(key);
        }

        public boolean isListening() {
            return listening;
        }

        public void listen() {
            listening = true;
        }
    }


    public static final class Text extends LightSetting {
        private final TextSetting textSetting;
        private boolean focused;
        private float focus;

        Text(TextSetting textSetting) {
            super(textSetting);
            this.textSetting = textSetting;
        }

        @Override
        public float height(float width) {
            return r(52f);
        }

        private float fieldW() {
            return Math.min(r(245f), w * 0.64f);
        }

        private float fieldH() {
            return r(40f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            focus = ease(focus, focused ? 1f : 0f, 16f, dt);
            float fw = fieldW(), fh = fieldH(), fx = right() - fw, cy = y + height(w) / 2f - r(2f), fy = cy - fh / 2f;
            text(INTER, ellipsize(INTER, name(), labelSize(), w - fw - r(12f)), x, cy, labelSize(), a(WHITE, alpha));

            rect(fx, fy, fw, fh, r(8f), a(accent(30 + Math.round(10 * focus)), alpha));
            outline(fx, fy, fw, fh, r(8f), a(accent(110), alpha * focus));

            float prefix = r(15f);
            text(INTER, "Aa", fx + r(14f), cy, prefix, a(GRAY, alpha));
            float size = r(18.5f), tx = fx + r(14f) + INTER.getWidth("Aa", prefix) + r(9f);
            float maxW = fx + fw - r(10f) - tx;
            String shown = textSetting.getText();
            while (!shown.isEmpty() && INTER.getWidth(shown, size) > maxW) shown = shown.substring(1);
            if (shown.isEmpty() && !focused) {
                String placeholder = textSetting.getPlaceholder().isEmpty() ? "Enter value" : textSetting.getPlaceholder();
                text(INTER, ellipsize(INTER, placeholder, size, maxW), tx, cy, size, a(GRAY, alpha));
            } else {
                text(INTER, shown, tx, cy, size, a(WHITE, alpha));
            }
            if (focused && System.currentTimeMillis() % 1000 < 500) {
                float caret = tx + INTER.getWidth(shown, size) + r(1f);
                rect(caret, cy - r(9f), r(1.5f), r(18f), 0f, a(accent(), alpha));
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            float cy = y + height(w) / 2f - r(2f);
            boolean hit = inside(mx, my, right() - fieldW(), cy - fieldH() / 2f, fieldW(), fieldH());
            focused = hit;
            return hit;
        }

        @Override
        public boolean keyPressed(int key, int scancode, int modifiers) {
            if (!focused) return false;
            String value = textSetting.getText();
            if (key == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
                textSetting.setText((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 ? "" : value.substring(0, value.length() - 1));
            } else if (key == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
                if (clip != null) textSetting.setText(value + clip.replaceAll("\\p{Cntrl}", ""));
            } else if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                focused = false;
            }
            return true;
        }

        @Override
        public boolean charTyped(char chr) {
            if (!focused) return false;
            if (!Character.isISOControl(chr) && textSetting.getText().length() < 64) {
                textSetting.setText(textSetting.getText() + chr);
            }
            return true;
        }

        @Override
        public boolean capturesKeyboard() {
            return focused;
        }

        @Override
        public void releaseKeyboard() {
            focused = false;
        }
    }


    public static final class Button extends LightSetting {
        private final RunSetting run;
        private float hover;
        private float flash;

        Button(RunSetting run) {
            super(run);
            this.run = run;
        }

        @Override
        public float height(float width) {
            return r(48f);
        }

        private float bw() {
            return r(100f);
        }

        private float bh() {
            return r(34f);
        }

        private float cy() {
            return y + height(w) / 2f - r(2f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            float bx = right() - bw(), by = cy() - bh() / 2f;
            hover = ease(hover, inside(mx, my, bx, by, bw(), bh()) ? 1f : 0f, 18f, dt);
            flash = ease(flash, 0f, 7f, dt);
            text(INTER, ellipsize(INTER, name(), labelSize(), w - bw() - r(12f)), x, cy(), labelSize(), a(WHITE, alpha));
            Color fill = mix(mix(accent(), Color.WHITE, hover * 0.25f), Color.WHITE, flash * 0.5f);
            rect(bx, by, bw(), bh(), r(8f), a(fill, alpha));
            textCenter(INTER, "Run", bx + bw() / 2f, cy(), r(18.5f), a(Color.WHITE, alpha));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my, right() - bw(), cy() - bh() / 2f, bw(), bh())) return false;
            flash = 1f;
            if (run.getValue() != null) run.getValue().run();
            return true;
        }
    }


    public static final class ColorValue extends LightSetting implements ColorPicker.Target {
        private final ColorSetting colorSetting;
        private final float[] hsva = {0f, 0f, 0f, 1f};
        private int synced;

        ColorValue(ColorSetting colorSetting) {
            super(colorSetting);
            this.colorSetting = colorSetting;
            syncFromSetting();
        }

        private Color stored() {
            Color c = colorSetting.getValue();
            return c == null ? Color.WHITE : c;
        }

        private void syncFromSetting() {
            Color c = stored();
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            if (hsb[1] > 0.001f && hsb[2] > 0.001f) hsva[0] = hsb[0];
            hsva[1] = hsb[1];
            hsva[2] = hsb[2];
            hsva[3] = c.getAlpha() / 255f;
            synced = c.getRGB();
        }

        @Override
        public float[] hsva() {
            return hsva;
        }

        @Override
        public void apply() {
            Color c = Color.getHSBColor(hsva[0], hsva[1], hsva[2]);
            Color value = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(hsva[3] * 255f));
            colorSetting.setValue(value);
            synced = value.getRGB();
        }

        @Override
        public Color color() {
            if (stored().getRGB() != synced) syncFromSetting();
            return stored();
        }

        @Override
        public float height(float width) {
            return r(40f);
        }

        private float swatch() {
            return r(22f);
        }

        private float chipW() {
            return swatch() + r(8f) + INTER.getWidth(hex(), labelSize());
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            label(alpha, chipW() + r(12f));
            Color c = color();
            float s = swatch(), sx = right() - s, sy = labelY() - s / 2f;
            text(INTER, hex(), right() - chipW(), labelY(), labelSize(), a(GRAY, alpha));
            rect(sx, sy, s, s, r(5f), a(new Color(c.getRed(), c.getGreen(), c.getBlue()), alpha));
            outline(sx, sy, s, s, r(5f), a(new Color(255, 255, 255, 40), alpha));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my, right() - chipW() - r(4f), labelY() - r(11f), chipW() + r(4f), r(22f))) return false;
            ColorPicker.INSTANCE.open(this, right(), labelY() + r(12f));
            return true;
        }
    }
}
