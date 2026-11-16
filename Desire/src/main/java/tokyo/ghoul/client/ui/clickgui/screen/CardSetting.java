package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
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
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;

import java.awt.Color;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public abstract class CardSetting {
    protected final Setting<?> setting;
    protected float x, y, w;
    float visible;

    protected CardSetting(Setting<?> setting) {
        this.setting = setting;
        this.visible = setting.isVisible() ? 1f : 0f;
    }

    public static CardSetting of(Setting<?> setting) {
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
        return setting.isVisible();
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

    protected static float labelSize() {
        return r(14.5f);
    }

    protected static float valueSize() {
        return r(13.5f);
    }

    protected float labelY() {
        return y + r(14f);
    }

    protected void label(float alpha, float reserved) {
        String text = ellipsize(MEDIUM, name(), labelSize(), w - reserved);
        text(MEDIUM, text, x, labelY(), labelSize(), a(SETTING_TEXT, alpha));
    }

    protected float right() {
        return x + w;
    }

    protected static void knob(float cx, float cy, float size, float alpha) {
        rect(cx - size / 2f - r(1f), cy - size / 2f - r(1f), size + r(2f), size + r(2f), size,
                a(new Color(0, 0, 0, 50), alpha));
        rect(cx - size / 2f, cy - size / 2f, size, size, size / 2f, a(Color.WHITE, alpha));
    }

    protected static float badgeW(String label) {
        return Math.max(r(19f), SEMIBOLD.getWidth(label, r(11.5f)) + r(10f));
    }

    protected static void badge(float bx, float cy, String label, boolean lit, float alpha) {
        float bh = r(19f), bw = badgeW(label);
        rect(bx, cy - bh / 2f, bw, bh, r(5f), a(lit ? BADGE : new Color(50, 50, 55), alpha));
        textCenter(SEMIBOLD, label, bx + bw / 2f, cy, r(11.5f), a(lit ? BADGE_TEXT : SETTING_TEXT, alpha));
    }


    public static final class Slider extends CardSetting {
        private final SliderSetting slider;
        private float shown;
        private boolean dragging;
        private float hover;

        Slider(SliderSetting slider) {
            super(slider);
            this.slider = slider;
            this.shown = value();
        }

        private float value() {
            Float v = slider.getValue();
            return v == null ? slider.getMin() : v;
        }

        @Override
        public float height(float width) {
            return r(40f);
        }

        private float trackY() {
            return y + r(31f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            if (dragging) setFromMouse(mx);
            float min = slider.getMin(), max = slider.getMax();
            shown = ease(shown, value(), 22f, dt);
            boolean hovered = dragging || inside(mx, my, x, trackY() - r(7f), w, r(14f));
            hover = ease(hover, hovered ? 1f : 0f, 16f, dt);

            String value = format(value());
            textRight(MEDIUM, value, right(), labelY(), valueSize(), a(SETTING_TEXT, alpha));
            label(alpha, MEDIUM.getWidth(value, valueSize()) + r(10f));

            float t = max > min ? Math.max(0f, Math.min(1f, (shown - min) / (max - min))) : 0f;
            float th = r(3f), ty = trackY();
            rect(x, ty - th / 2f, w, th, th / 2f, a(TRACK, alpha));
            rect(x, ty - th / 2f, w * t, th, th / 2f, a(FILL, alpha));
            knob(x + w * t, ty, r(10f) + r(2f) * hover, alpha);
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
            if (button != 0 || !inside(mx, my, x - r(4f), trackY() - r(8f), w + r(8f), r(16f))) return false;
            dragging = true;
            setFromMouse(mx);
            return true;
        }

        @Override
        public void mouseReleased() {
            dragging = false;
        }
    }


    public static final class Mode extends CardSetting {
        private final ModeSetting mode;
        private boolean open;
        private float openAnim;
        private float boxHover;
        private float[] hover = new float[0];

        Mode(ModeSetting mode) {
            super(mode);
            this.mode = mode;
        }

        private List<String> options() {
            return mode.getModes();
        }

        private static float headH() {
            return r(30f);
        }

        private static float optionH() {
            return r(25f);
        }

        private float listH() {
            return options().size() * optionH() + r(8f);
        }

        @Override
        public float height(float width) {
            return headH() + listH() * smooth(openAnim);
        }

        private float boxW() {
            float widest = 0f;
            for (String option : options()) widest = Math.max(widest, MEDIUM.getWidth(option, valueSize()));
            return Math.min(w * 0.58f, widest + r(34f));
        }

        private float boxX() {
            return right() - boxW();
        }

        private static float boxH() {
            return r(21f);
        }

        private static float textLift() {
            return r(1.5f);
        }

        private float boxCenterY() {
            return labelY() - textLift();
        }

        private float boxY() {
            return boxCenterY() - boxH() / 2f;
        }

        private float listY() {
            return y + headH();
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            List<String> options = options();
            if (hover.length != options.size()) hover = new float[options.size()];
            openAnim = ease(openAnim, open ? 1f : 0f, 16f, dt);
            if (Math.abs(openAnim - (open ? 1f : 0f)) < 0.002f) openAnim = open ? 1f : 0f;

            float bw = boxW(), bx = boxX(), by = boxY(), bh = boxH();
            boxHover = ease(boxHover, inside(mx, my, bx, by, bw, bh) ? 1f : 0f, 18f, dt);
            label(alpha, bw + r(10f));

            rect(bx, by, bw, bh, r(5f), a(mix(mix(new Color(46, 46, 50), new Color(58, 58, 63), boxHover),
                    new Color(62, 62, 67), openAnim), alpha));
            String value = mode.getValue() == null ? "" : mode.getValue();
            float pad = r(8f), arrow = r(10f);
            text(MEDIUM, ellipsize(MEDIUM, value, valueSize(), bw - pad * 2f - arrow - r(4f)), bx + pad, boxCenterY() + textLift(),
                    valueSize(), a(mix(LABEL, ROW_TEXT_ON, openAnim), alpha));
            var matrices = RenderUtil.matrices();
            float ax = bx + bw - pad - arrow / 2f, ay = boxCenterY();
            float t = smooth(openAnim);
            if (t < 0.99f) icon(CHEVRON_DOWN, ax, ay, arrow, a(ICON_DIM, alpha * (1f - t)));
            if (t > 0.01f) icon(CHEVRON_UP, ax, ay, arrow, a(ROW_TEXT_ON, alpha * t));

            if (openAnim <= 0.002f) return;
            float listAlpha = alpha * smooth(openAnim);
            float ly = listY(), lh = listH() * smooth(openAnim);
            ScissorUtil.start(matrices, x, ly, w, lh);
            rect(x, ly, w, listH(), r(6f), a(new Color(28, 28, 31), listAlpha));
            float oy = ly + r(4f);
            for (int i = 0; i < options.size(); i++) {
                String option = options.get(i);
                boolean selected = option.equals(mode.getValue());
                boolean hovered = open && inside(mx, my, x, oy, w, optionH()) && my < ly + lh;
                hover[i] = ease(hover[i], hovered ? 1f : 0f, 18f, dt);
                if (hover[i] > 0.01f || selected) {
                    rect(x + r(4f), oy, w - r(8f), optionH(), r(5f),
                            a(selected ? new Color(255, 255, 255, 16) : new Color(255, 255, 255, 10), listAlpha * (selected ? 1f : hover[i])));
                }
                Color color = selected ? ROW_TEXT_ON : mix(SETTING_TEXT, LABEL, hover[i]);
                float rowCy = oy + optionH() / 2f, inset = r(4f) + r(8f), check = r(10f);
                text(MEDIUM, ellipsize(MEDIUM, option, valueSize(), w - inset * 2f - check - r(6f)), x + inset,
                        rowCy + textLift(), valueSize(), a(color, listAlpha));
                if (selected) icon(CHECK, right() - inset - check / 2f, rowCy, check, a(ROW_TEXT_ON, listAlpha));
                oy += optionH();
            }
            ScissorUtil.stop(matrices);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            List<String> options = options();
            if (button != 0 && button != 1) return false;
            if (inside(mx, my, x, y, w, headH())) {
                if (button == 0) {
                    open = !open;
                } else if (!options.isEmpty()) {
                    int n = options.size();
                    mode.setValue(options.get((Math.max(0, options.indexOf(mode.getValue())) + 1) % n));
                }
                return true;
            }
            if (open && openAnim > 0.9f && button == 0) {
                float oy = listY() + r(4f);
                for (String option : options) {
                    if (inside(mx, my, x, oy, w, optionH())) {
                        mode.setValue(option);
                        open = false;
                        return true;
                    }
                    oy += optionH();
                }
            }
            return false;
        }

        private static float smooth(float t) {
            t = Math.max(0f, Math.min(1f, t));
            return t * t * (3f - 2f * t);
        }
    }


    public static final class Multi extends CardSetting {
        private final MultiBooleanSetting multi;
        private final List<float[]> chips = new ArrayList<>();
        private float[] anim = new float[0];
        private float[] hover = new float[0];

        Multi(MultiBooleanSetting multi) {
            super(multi);
            this.multi = multi;
        }

        private List<String> options() {
            return multi.getAllNames();
        }

        private boolean isSelected(int index) {
            return multi.getValue().get(index).getValue();
        }

        private static float chipH() {
            return r(19f);
        }

        private static float chipFont() {
            return r(12f);
        }

        private static float rowStep() {
            return r(24f);
        }

        private List<float[]> layout(float width) {
            List<float[]> out = new ArrayList<>();
            float cx = 0f, cy = 0f;
            for (String option : options()) {
                float cw = Math.min(width, MEDIUM.getWidth(option, chipFont()) + r(14f));
                if (cx > 0f && cx + cw > width) {
                    cx = 0f;
                    cy += rowStep();
                }
                out.add(new float[]{cx, cy, cw});
                cx += cw + r(5f);
            }
            return out;
        }

        @Override
        public float height(float width) {
            List<float[]> layout = layout(width);
            float lastY = layout.isEmpty() ? 0f : layout.get(layout.size() - 1)[1];
            return r(30f) + chipH() + lastY;
        }

        private float chipsTop() {
            return y + r(26f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            List<String> options = options();
            if (anim.length != options.size()) {
                anim = new float[options.size()];
                hover = new float[options.size()];
                for (int i = 0; i < anim.length; i++) anim[i] = isSelected(i) ? 1f : 0f;
            }
            String value = multi.getList().size() + " / " + multi.getValue().size();
            textRight(MEDIUM, value, right(), labelY(), valueSize(), a(SETTING_TEXT, alpha));
            label(alpha, MEDIUM.getWidth(value, valueSize()) + r(10f));

            chips.clear();
            chips.addAll(layout(w));
            for (int i = 0; i < options.size(); i++) {
                float[] chip = chips.get(i);
                float cx = x + chip[0], cy = chipsTop() + chip[1], cw = chip[2];
                anim[i] = ease(anim[i], isSelected(i) ? 1f : 0f, 18f, dt);
                hover[i] = ease(hover[i], inside(mx, my, cx, cy, cw, chipH()) ? 1f : 0f, 18f, dt);

                Color idle = mix(new Color(46, 46, 50), new Color(58, 58, 63), hover[i]);
                rect(cx, cy, cw, chipH(), r(5f), a(mix(idle, BADGE, anim[i]), alpha));
                Color textColor = mix(mix(SETTING_TEXT, LABEL, hover[i] * 0.6f), BADGE_TEXT, anim[i]);
                String text = ellipsize(MEDIUM, options.get(i), chipFont(), cw - r(8f));
                textCenter(MEDIUM, text, cx + cw / 2f, cy + chipH() / 2f, chipFont(), a(textColor, alpha));
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0) return false;
            for (int i = 0; i < chips.size() && i < options().size(); i++) {
                float[] chip = chips.get(i);
                if (inside(mx, my, x + chip[0], chipsTop() + chip[1], chip[2], chipH())) {
                    multi.getValue().get(i).toggle();
                    return true;
                }
            }
            return false;
        }
    }


    public static final class Checkbox extends CardSetting {
        private final BooleanSetting bool;
        private float anim;

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
            return r(28f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            anim = ease(anim, on() ? 1f : 0f, 18f, dt);
            float tw = r(25f), th = r(14f);
            label(alpha, tw + r(10f));

            float tx = right() - tw, ty = labelY() - th / 2f;
            rect(tx, ty, tw, th, th / 2f, a(mix(TRACK, FILL, anim), alpha));
            float k = r(10f);
            float kx = tx + r(2f) + (tw - k - r(4f)) * anim;
            rect(kx, labelY() - k / 2f, k, k, k / 2f, a(mix(new Color(150, 150, 156), new Color(34, 34, 38), anim), alpha));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my, x, y, w, height(w))) return false;
            bool.setValue(!on());
            return true;
        }
    }


    public static final class Bind extends CardSetting {
        private final BindSetting bind;
        private boolean listening;
        private String keyLabel;
        private int keyLabelFor = Integer.MIN_VALUE;

        Bind(BindSetting bind) {
            super(bind);
            this.bind = bind;
        }

        private int key() {
            Integer v = bind.getValue();
            return v == null ? -999 : v;
        }

        public boolean assignMouse(int button) {
            if (!listening || button < 2) return false;
            bind.setValue(-100 + button);
            listening = false;
            return true;
        }

        private boolean bound() {
            return key() != -999 && key() != -1;
        }

        @Override
        public float height(float width) {
            return r(28f);
        }

        private String keyText() {
            if (listening) return "...";
            if (!bound()) return "None";
            if (keyLabel == null || keyLabelFor != key()) {
                keyLabel = KeyStorage.getBind(key());
                keyLabelFor = key();
            }
            return keyLabel;
        }

        private float chipX() {
            return right() - badgeW(keyText());
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            String key = keyText();
            label(alpha, badgeW(key) + r(10f));
            float pulse = listening ? 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / 160.0) : 1f;
            badge(chipX(), labelY(), key, bound() || listening, alpha * pulse);
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (assignMouse(button)) return true;
            if (!inside(mx, my, x, y, w, height(w))) return false;
            if (button == 1) {
                bind.setValue(-999);
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
            bind.setValue(key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE ? -999 : key);
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


    public static final class Text extends CardSetting {
        private final TextSetting textSetting;
        private boolean focused;
        private float focus;

        Text(TextSetting textSetting) {
            super(textSetting);
            this.textSetting = textSetting;
        }

        @Override
        public float height(float width) {
            return r(28f);
        }

        private float fieldW() {
            return Math.min(r(96f), w * 0.55f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            focus = ease(focus, focused ? 1f : 0f, 16f, dt);
            label(alpha, fieldW() + r(10f));

            float fh = r(19f), fw = fieldW(), fx = right() - fw, fy = labelY() - fh / 2f;
            rect(fx, fy, fw, fh, r(5f), a(new Color(28, 28, 31), alpha));
            RenderUtil.RECT.drawOutline(RenderUtil.matrices(), fx, fy, fw, fh, r(5f), OUTLINE,
                    a(mix(new Color(255, 255, 255, 10), new Color(255, 255, 255, 70), focus), alpha));

            float size = r(12f);
            float tx = fx + r(7f), maxW = fw - r(14f);
            String shown = textSetting.getText();
            while (!shown.isEmpty() && MEDIUM.getWidth(shown, size) > maxW) shown = shown.substring(1);
            if (shown.isEmpty() && !focused) {
                String placeholder = textSetting.getPlaceholder().isEmpty() ? "Text.." : textSetting.getPlaceholder();
                text(MEDIUM, ellipsize(MEDIUM, placeholder, size, maxW), tx, labelY(), size, a(FAINT, alpha));
            } else {
                text(MEDIUM, shown, tx, labelY(), size, a(LABEL, alpha));
            }
            if (focused && System.currentTimeMillis() % 1000 < 500) {
                float cx = tx + MEDIUM.getWidth(shown, size) + r(1f);
                rect(cx, labelY() - r(5.5f), r(0.8f), r(11f), 0f, a(Color.WHITE, alpha));
            }
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            float fh = r(19f);
            boolean hit = inside(mx, my, right() - fieldW(), labelY() - fh / 2f, fieldW(), fh);
            focused = hit;
            return hit;
        }

        @Override
        public boolean keyPressed(int key, int scancode, int modifiers) {
            if (!focused) return false;
            String value = textSetting.getText();
            if (key == GLFW.GLFW_KEY_BACKSPACE && !value.isEmpty()) {
                textSetting.setText(value.substring(0, value.length() - 1));
            } else if (key == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                String clip = GLFW.glfwGetClipboardString(Minecraft.getInstance().getWindow().handle());
                if (clip != null) textSetting.setText((value + clip.replaceAll("\\p{Cntrl}", "")));
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


    public static final class Button extends CardSetting {
        private final RunSetting run;
        private float hover;
        private float flash;

        Button(RunSetting run) {
            super(run);
            this.run = run;
        }

        @Override
        public float height(float width) {
            return r(28f);
        }

        private float bw() {
            return r(56f);
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            float bh = r(19f), bx = right() - bw(), by = labelY() - bh / 2f;
            hover = ease(hover, inside(mx, my, bx, by, bw(), bh) ? 1f : 0f, 18f, dt);
            flash = ease(flash, 0f, 7f, dt);
            label(alpha, bw() + r(10f));

            Color fill = mix(mix(new Color(52, 52, 57), new Color(68, 68, 74), hover), BADGE, flash);
            rect(bx, by, bw(), bh, r(5f), a(fill, alpha));
            textCenter(MEDIUM, "Run", bx + bw() / 2f, labelY(), r(12f), a(mix(LABEL, BADGE_TEXT, flash), alpha));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            float bh = r(19f);
            if (button != 0 || !inside(mx, my, right() - bw(), labelY() - bh / 2f, bw(), bh)) return false;
            flash = 1f;
            if (run.getValue() != null) run.getValue().run();
            return true;
        }
    }


    public static final class ColorValue extends CardSetting implements ColorPicker.Target {
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
            return r(28f);
        }

        private float swatch() {
            return r(15f);
        }

        private float chipW() {
            return swatch() + r(6f) + MEDIUM.getWidth(hex(), valueSize());
        }

        private float chipX() {
            return right() - chipW();
        }

        @Override
        protected void draw(float alpha, double mx, double my, float dt) {
            label(alpha, chipW() + r(10f));
            Color c = color();
            Color solid = new Color(c.getRed(), c.getGreen(), c.getBlue());
            text(MEDIUM, hex(), chipX(), labelY(), valueSize(), a(SETTING_TEXT, alpha));
            float s = swatch(), sx = right() - s, sy = labelY() - s / 2f;
            rect(sx - r(1.5f), sy - r(1.5f), s + r(3f), s + r(3f), s, a(new Color(255, 255, 255, 30), alpha));
            rect(sx, sy, s, s, s / 2f, a(solid, alpha));
        }

        @Override
        public boolean mouseClicked(double mx, double my, int button) {
            if (button != 0 || !inside(mx, my, chipX() - r(4f), labelY() - r(10f), chipW() + r(4f), r(20f))) return false;
            ColorPicker.INSTANCE.open(this, right(), labelY() + r(10f));
            return true;
        }
    }
}
