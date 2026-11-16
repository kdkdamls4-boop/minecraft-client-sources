package tokyo.ghoul.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.configs.ThemeManager;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.UIComponent;
import tokyo.ghoul.client.ui.clickgui.Panel;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeManagerWindow extends UIComponent {
    @Getter private static final ThemeManagerWindow instance = new ThemeManagerWindow();

    @Setter private boolean open;

    private final AnimationUtil openAnimation = new AnimationUtil();
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private final AnimationUtil presetScrollAnimation = new AnimationUtil();
    private final AnimationUtil pickerAnimation = new AnimationUtil();

    private float scroll, maxScroll;
    private float presetScroll, presetMaxScroll;

    private int editingSlot = -1;
    private final float[] hsv = new float[3];
    private boolean draggingSV, draggingH;
    private float pickerPopupX, pickerPopupY;

    private String themeName = "";
    private boolean nameFieldFocused = false;

    private static final float WIDTH = 140f;
    private static final float HEIGHT = 280f;
    private static final float HEADER_H = 32f;
    private static final float ROW_H = 16f;
    private static final float PRESET_D = 9f;
    private static final float PRESET_GAP = 1.5f;
    private static final float PRESET_ROW_H = PRESET_D + 4f;
    private static final float FIELD_H = 16f;
    private static final float BTN_H = 16f;
    private static final float TOP_CONTROLS_H = 2f + FIELD_H + 4f;
    private static final float FIXED_BOTTOM_H = 1f + 2f + PRESET_ROW_H + 3f;
    private static final float SV_SIZE = 60f;
    private static final float HUE_W = 6f;
    private static final float PICKER_PAD = 4f;

    private static final int[][] PRESETS = {
            { 0xFFAA44FF, 0xFF0D0D10 }, { 0xFF44AAFF, 0xFF0A0A18 },
            { 0xFF44FFAA, 0xFF0A1810 }, { 0xFFFF4444, 0xFF1A0808 },
            { 0xFFFF9944, 0xFF1A1008 }, { 0xFF44FFEE, 0xFF081A18 },
            { 0xFFFF44AA, 0xFF1A0810 }, { 0xFF8844FF, 0xFF100A1A },
            { 0xFFFFDD44, 0xFF1A1808 }, { 0xFF44FF44, 0xFF081A08 },
            { 0xFF4488FF, 0xFF080A1A },
    };

    private ThemeManagerWindow() {
        setScaleMultiplier(Panel.SCALE);
        setWidth(scaled(WIDTH));
        setHeight(scaled(HEIGHT));
    }

    private List<Theme.ElementColor> editableColors() {
        return new ArrayList<>(UIColors.currentTheme().getElementColors());
    }

    private float scrollAreaH(float height, float headerH) {
        return height - headerH - scaled(TOP_CONTROLS_H + FIXED_BOTTOM_H);
    }

    private void clampScroll(float contentH, float areaH) {
        maxScroll = Math.max(0f, contentH - areaH);
        scroll = maxScroll > 0f ? Math.min(Math.max(scroll, -maxScroll), 0f) : 0f;
    }

    private void clampPresetScroll(float areaW) {
        int count = PRESETS.length + ThemeEditor.getInstance().getThemeSelectables().size();
        float totalW = count * scaled(PRESET_D) + (count - 1) * scaled(PRESET_GAP);
        presetMaxScroll = Math.max(0f, totalW - areaW);
        presetScroll = presetMaxScroll > 0f ? Math.min(Math.max(presetScroll, -presetMaxScroll), 0f) : 0f;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 400, Easing.QUINT_OUT);

        float anim = (float) (openAnimation.getValue() * getAlpha());
        if (anim < 0.01f) return;

        PoseStack matrixStack = RenderUtil.matrices();

        float w = scaled(WIDTH);
        float h = scaled(HEIGHT);
        float headerH = scaled(HEADER_H);
        float round = scaled(8f);
        float cx = getX() - w * (1f - (float) openAnimation.getValue());
        float cy = getY();

        int ai = (int) (255f * anim);
        Vector4f allRound = new Vector4f(round, round, round, round);
        Vector4f topRound = new Vector4f(round, round, 0f, 0f);

        Color tintL = ColorUtil.interpolate(UIColors.secondary(), new Color(18, 20, 19), 0.5f);
        Color tintR = ColorUtil.interpolate(UIColors.primary(), new Color(22, 25, 23), 0.5f);
        RenderUtil.BLUR_RECT.draw(matrixStack, cx, cy, w, h, allRound,
                ColorUtil.setAlpha(tintL, (int) (232f * anim)),
                ColorUtil.setAlpha(tintR, (int) (232f * anim)),
                ColorUtil.setAlpha(tintL, (int) (232f * anim)),
                ColorUtil.setAlpha(tintR, (int) (232f * anim)), 0.18f);
        RenderUtil.RECT.draw(matrixStack, cx, cy, w, h, allRound, new Color(4, 6, 5, (int) (180f * anim)));
        RenderUtil.GRADIENT_RECT.draw(matrixStack, cx, cy, w, h, round,
                ColorUtil.setAlpha(tintL, (int) (20f * anim)),
                ColorUtil.setAlpha(tintR, (int) (20f * anim)),
                ColorUtil.setAlpha(tintL, (int) (14f * anim)),
                ColorUtil.setAlpha(tintR, (int) (14f * anim)));
        RenderUtil.RECT.draw(matrixStack, cx, cy, w, h, allRound, new Color(255, 255, 255, (int) (7f * anim)));

        String title = "Theme Editor";
        float titleSize = headerH * 0.52f;
        float titleWidth = Fonts.PS_REGULAR.getWidth(title, titleSize);
        Fonts.PS_REGULAR.drawText(matrixStack, title, cx + w / 2f - titleWidth / 2f, cy + headerH / 2f - titleSize / 2f, titleSize, ColorUtil.setAlpha(new Color(255, 255, 255), ai));

        float topFieldX = cx + scaled(4f);
        float topGap = scaled(3f);
        float topButtonW = scaled(48f);
        float topFieldW = w - scaled(8f) - topButtonW - topGap;
        float topY = cy + headerH + scaled(2f);
        float fieldH = scaled(FIELD_H);
        Color accent = ColorUtil.setAlpha(UIColors.primary(), (int) (55f * anim));
        RenderUtil.RECT.draw(matrixStack, topFieldX, topY, topFieldW, fieldH, scaled(3f), accent);
        String displayName = themeName.isEmpty() ? "Название" : themeName;
        Color nameColor = themeName.isEmpty()
                ? ColorUtil.setAlpha(new Color(200, 200, 200), (int) (160f * anim))
                : ColorUtil.setAlpha(new Color(255, 255, 255), ai);
        float nameSize = fieldH * (7f / FIELD_H);
        Fonts.PS_REGULAR.drawText(matrixStack, displayName, topFieldX + scaled(4f), topY + fieldH / 2f - nameSize / 2f, nameSize, nameColor);

        float topButtonX = topFieldX + topFieldW + topGap;
        float topButtonY = topY;
        float btnH = scaled(BTN_H);
        boolean btnHov = MouseUtil.isHovered(mouseX, mouseY, topButtonX, topButtonY, topButtonW, btnH);
        RenderUtil.RECT.draw(matrixStack, topButtonX, topButtonY, topButtonW, btnH, scaled(3f), btnHov ? ColorUtil.setAlpha(UIColors.primary(), (int) (90f * anim)) : accent);
        String btnText = "Создать";
        float btnTextSize = btnH * (7f / BTN_H);
        float btnTextW = Fonts.PS_REGULAR.getWidth(btnText, btnTextSize);
        Fonts.PS_REGULAR.drawText(matrixStack, btnText, topButtonX + topButtonW / 2f - btnTextW / 2f, topButtonY + btnH / 2f - btnTextSize / 2f, btnTextSize, ColorUtil.setAlpha(new Color(255, 255, 255), ai));

        float rowH = scaled(ROW_H);
        List<Theme.ElementColor> slots = editableColors();
        float scrollZoneY = cy + headerH + scaled(TOP_CONTROLS_H);
        float scrollZoneH = scrollAreaH(h, headerH);

        clampScroll(slots.size() * rowH + scaled(2f), scrollZoneH);
        clampPresetScroll(w - scaled(8f));
        scrollAnimation.update();
        scrollAnimation.run(scroll, 200, Easing.CUBIC_BOTH);
        presetScrollAnimation.update();
        presetScrollAnimation.run(presetScroll, 60, Easing.CUBIC_BOTH);
        pickerAnimation.update();
        pickerAnimation.run(editingSlot >= 0 ? 1.0 : 0.0, 250, Easing.QUINT_OUT);

        ScissorUtil.start(matrixStack, cx, scrollZoneY, w, scrollZoneH);
        float curY = scrollZoneY + scaled(2f) + (float) scrollAnimation.getValue();
        for (int i = 0; i < slots.size(); i++) {
            renderRow(matrixStack, i, slots.get(i), cx + scaled(3.5f), curY, w - scaled(7f), rowH, mouseX, mouseY, anim, ai);
            curY += rowH;
        }
        ScissorUtil.stop(matrixStack);

        float fixedY = cy + h - scaled(FIXED_BOTTOM_H);
        RenderUtil.RECT.draw(matrixStack, cx + scaled(4f), fixedY, w - scaled(8f), scaled(0.5f), 0f, ColorUtil.setAlpha(new Color(255, 255, 255), (int) (25f * anim)));
        fixedY += scaled(1f) + scaled(2f);

        float presetAreaX = cx + scaled(4f);
        float presetAreaW = w - scaled(8f);
        float presetRowH = scaled(PRESET_ROW_H);
        float pd = scaled(PRESET_D);
        float pgap = scaled(PRESET_GAP);
        ScissorUtil.start(matrixStack, presetAreaX, fixedY, presetAreaW, presetRowH);
        float circleX = presetAreaX + (float) presetScrollAnimation.getValue();
        float circleY = fixedY + (presetRowH - pd) / 2f;
        for (int[] preset : PRESETS) {
            RenderUtil.RECT.draw(matrixStack, circleX, circleY, pd, pd, pd / 2f - scaled(0.5f), ColorUtil.setAlpha(new Color(preset[0], true), ai));
            circleX += pd + pgap;
        }
        for (ThemeSelectable selectable : ThemeEditor.getInstance().getThemeSelectables()) {
            RenderUtil.RECT.draw(matrixStack, circleX, circleY, pd, pd, pd / 2f - scaled(0.5f), ColorUtil.setAlpha(selectable.getTheme().getPrimaryColor(), ai));
            circleX += pd + pgap;
        }
        ScissorUtil.stop(matrixStack);
        fixedY += presetRowH + scaled(2f);

        if (pickerAnimation.getValue() > 0.01f) {
            renderPickerPopup(matrixStack, pickerPopupX, pickerPopupY, mouseX, mouseY, (float) pickerAnimation.getValue() * anim);
        }
    }

    private void renderRow(PoseStack matrixStack, int slot, Theme.ElementColor elementColor, float rx, float ry, float rw, float rowH, int mouseX, int mouseY, float anim, int ai) {
        boolean active = editingSlot == slot;
        if (active) {
            RenderUtil.RECT.draw(matrixStack, rx, ry, rw, rowH, scaled(2f), ColorUtil.setAlpha(new Color(60, 60, 80), (int) (60f * anim)));
        }
        float textSize = rowH * (7f / ROW_H);
        Fonts.PS_REGULAR.drawText(matrixStack, displayName(elementColor.getName()), rx + scaled(3f), ry + rowH / 2f - textSize / 2f, textSize, ColorUtil.setAlpha(new Color(210, 210, 210), ai));
        float cd = scaled(8f);
        boolean locked = false;
        RenderUtil.RECT.draw(matrixStack, rx + rw - cd - scaled(3f), ry + rowH / 2f - cd / 2f, cd, cd, cd / 2f - scaled(1f), ColorUtil.setAlpha(elementColor.getColor(), locked ? ai / 2 : ai));
    }

    private String displayName(String name) {
        return switch (name) {
            case "Primary" -> "1 цвет";
            case "Secondary" -> "2 цвет";
            case "Blur" -> "Фон худа";
            case "Widget blur" -> "Фон модуля";
            case "Background blur" -> "Фон заднего плана";
            case "Text" -> "Текст";
            case "Inactive text" -> "Неактивный текст";
            case "Knob" -> "Круг слайдера";
            case "Inactive knob" -> "Неактивный переключатель";
            case "Positive" -> "Слайдер";
            case "Middle" -> "Переключатель";
            case "Negative" -> "Кнопка";
            default -> name;
        };
    }

    private void renderPickerPopup(PoseStack matrixStack, float px, float py, int mouseX, int mouseY, float anim) {
        int ai = (int) (255f * anim);
        float p = scaled(PICKER_PAD);
        float sv = scaled(SV_SIZE);
        float hueW = scaled(HUE_W);
        float pickerW = sv + scaled(PICKER_PAD) + hueW;
        float pickerH = sv;
        float small = scaled(2f);
        Vector4f smallRound = new Vector4f(small, small, small, small);

        RenderUtil.BLUR_RECT.draw(matrixStack, px - p, py - p, pickerW + p * 2f, pickerH + p * 2f, scaled(5f), UIColors.blur(ai));
        RenderUtil.RECT.draw(matrixStack, px - p, py - p, pickerW + p * 2f, pickerH + p * 2f, scaled(5f), ColorUtil.setAlpha(new Color(14, 14, 16), (int) (130f * anim)));

        RenderUtil.RECT.draw(matrixStack, px, py, sv, sv, small, ColorUtil.setAlpha(new Color(Color.HSBtoRGB(hsv[0], 1f, 1f)), ai));
        RenderUtil.GRADIENT_RECT.draw(matrixStack, px, py, sv, sv, smallRound,
                ColorUtil.setAlpha(new Color(255, 255, 255), ai), ColorUtil.setAlpha(new Color(255, 255, 255), ai),
                ColorUtil.setAlpha(new Color(255, 255, 255), 0), ColorUtil.setAlpha(new Color(255, 255, 255), 0));
        RenderUtil.GRADIENT_RECT.draw(matrixStack, px, py, sv, sv, smallRound,
                ColorUtil.setAlpha(new Color(0, 0, 0), 0), ColorUtil.setAlpha(new Color(0, 0, 0), 0),
                ColorUtil.setAlpha(new Color(0, 0, 0), ai), ColorUtil.setAlpha(new Color(0, 0, 0), ai));

        float scx = px + hsv[1] * sv;
        float scy = py + (1f - hsv[2]) * sv;
        RenderUtil.RECT.draw(matrixStack, scx - scaled(3f), scy - scaled(3f), scaled(6f), scaled(6f), scaled(3f), ColorUtil.setAlpha(new Color(0, 0, 0), (int) (180f * anim)));
        RenderUtil.RECT.draw(matrixStack, scx - scaled(2f), scy - scaled(2f), scaled(4f), scaled(4f), scaled(2f), ColorUtil.setAlpha(new Color(255, 255, 255), ai));

        float hueX = px + sv + scaled(4f);
        for (float i = 0f; i < SV_SIZE; i += 0.5f) {
            RenderUtil.RECT.draw(matrixStack, hueX, py + scaled(i), hueW, scaled(1f), 0f, ColorUtil.setAlpha(new Color(Color.HSBtoRGB(i / SV_SIZE, 1f, 1f)), ai));
        }
        float hcy = py + hsv[0] * sv;
        RenderUtil.RECT.draw(matrixStack, hueX - scaled(1.5f), hcy - scaled(2.5f), hueW + scaled(3f), scaled(5f), scaled(2f), ColorUtil.setAlpha(new Color(0, 0, 0), (int) (180f * anim)));
        RenderUtil.RECT.draw(matrixStack, hueX - scaled(0.5f), hcy - scaled(1.5f), hueW + scaled(1f), scaled(3f), scaled(1f), ColorUtil.setAlpha(new Color(255, 255, 255), ai));

        if (draggingSV) {
            hsv[1] = Math.max(0f, Math.min(1f, (mouseX - px) / sv));
            hsv[2] = 1f - Math.max(0f, Math.min(1f, (mouseY - py) / sv));
            applyHSV();
        } else if (draggingH) {
            hsv[0] = Math.max(0f, Math.min(1f, (mouseY - py) / sv));
            applyHSV();
        }
    }

    private void applyHSV() {
        if (editingSlot < 0) return;
        List<Theme.ElementColor> slots = editableColors();
        if (editingSlot >= slots.size()) return;
        slots.get(editingSlot).setColor(new Color(Color.HSBtoRGB(hsv[0], hsv[1], hsv[2])));
    }

    private void loadHSV(int slot) {
        Color color = editableColors().get(slot).getColor();
        Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), hsv);
    }

    private void save() {
        ThemeManager.getInstance().saveAll();
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!open || openAnimation.getValue() < 0.9f) return;

        float w = scaled(WIDTH);
        float h = scaled(HEIGHT);
        float headerH = scaled(HEADER_H);
        float cx = getX() - w * (1f - (float) openAnimation.getValue());
        float cy = getY();

        if (editingSlot >= 0 && pickerAnimation.getValue() > 0.5f) {
            float px = pickerPopupX, py = pickerPopupY;
            float sv = scaled(SV_SIZE);
            float hueW = scaled(HUE_W);
            float p = scaled(PICKER_PAD);
            float pickerW = sv + scaled(PICKER_PAD) + hueW;

            if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, px, py, sv, sv)) {
                draggingSV = true;
                return;
            }
            if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, px + sv + scaled(4f), py, hueW + scaled(4f), sv)) {
                draggingH = true;
                return;
            }
            if (!MouseUtil.isHovered(mouseX, mouseY, px - p, py - p, pickerW + p * 2f, sv + p * 2f)) {
                editingSlot = -1;
            }
        }

        List<Theme.ElementColor> slots = editableColors();
        float rowH = scaled(ROW_H);
        float scrollZoneY = cy + headerH + scaled(TOP_CONTROLS_H);
        float scrollZoneH = scrollAreaH(h, headerH);
        if (MouseUtil.isHovered(mouseX, mouseY, cx, scrollZoneY, w, scrollZoneH)) {
            float curY = scrollZoneY + scaled(2f) + (float) scrollAnimation.getValue();
            for (int i = 0; i < slots.size(); i++) {
                if (MouseUtil.isHovered(mouseX, mouseY, cx + scaled(3.5f), curY, w - scaled(7f), rowH) && button == 0) {
                    editingSlot = (editingSlot == i) ? -1 : i;
                    if (editingSlot >= 0) {
                        loadHSV(i);
                        pickerPopupX = cx + w + scaled(4f);
                        pickerPopupY = curY + rowH / 2f - scaled(SV_SIZE) / 2f;
                    }
                    return;
                }
                curY += rowH;
            }
        }

        float fixedY = cy + h - scaled(FIXED_BOTTOM_H) + scaled(1f) + scaled(2f);
        float presetAreaX = cx + scaled(4f);
        float presetAreaW = w - scaled(8f);
        float pd = scaled(PRESET_D);
        float pgap = scaled(PRESET_GAP);
        float circleX = presetAreaX + (float) presetScrollAnimation.getValue();
        float circleY = fixedY + (scaled(PRESET_ROW_H) - pd) / 2f;
        for (int[] preset : PRESETS) {
            if (button == 0
                    && circleX >= presetAreaX && circleX + pd <= presetAreaX + presetAreaW
                    && MouseUtil.isHovered(mouseX, mouseY, circleX, circleY, pd, pd)) {
                applyPreset(preset[0], preset[1]);
                return;
            }
            circleX += pd + pgap;
        }
        for (ThemeSelectable selectable : ThemeEditor.getInstance().getThemeSelectables()) {
            if (button == 0 && circleX >= presetAreaX && circleX + pd <= presetAreaX + presetAreaW
                    && MouseUtil.isHovered(mouseX, mouseY, circleX, circleY, pd, pd)) {
                ThemeEditor.getInstance().setCurrentTheme(selectable.getTheme());
                return;
            }
            circleX += pd + pgap;
        }
        float fieldX = cx + scaled(4f);
        float topGap = scaled(3f);
        float buttonW = scaled(48f);
        float fieldW = w - scaled(8f) - buttonW - topGap;
        float topY = cy + headerH + scaled(2f);
        nameFieldFocused = button == 0 && MouseUtil.isHovered(mouseX, mouseY, fieldX, topY, fieldW, scaled(FIELD_H));
        float buttonX = fieldX + fieldW + topGap;
        float buttonY = topY;

        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, buttonX, buttonY, buttonW, scaled(BTN_H))) {
            String name = themeName.trim().isEmpty()
                    ? "Theme " + (ThemeEditor.getInstance().getThemeSelectables().size() + 1)
                    : themeName.trim();

            Theme newTheme = new Theme(ThemeManager.getInstance().safeFileName(name));
            newTheme.getElementColors().clear();
            for (Theme.ElementColor elementColor : UIColors.currentTheme().getElementColors()) {
                newTheme.getElementColors().add(new Theme.ElementColor(elementColor.getName(), elementColor.getColor()));
            }
            ThemeEditor.getInstance().getThemeSelectables().add(new ThemeSelectable(newTheme));
            ThemeEditor.getInstance().setCurrentTheme(newTheme);
            save();

            themeName = "";
            nameFieldFocused = false;
        }
    }

    private void applyPreset(int first, int second) {
        Theme current = UIColors.currentTheme();
        setElementColor(current, "Primary", first);
        setElementColor(current, "Secondary", second);
        save();
    }

    private void setElementColor(Theme theme, String name, int argb) {
        for (Theme.ElementColor elementColor : theme.getElementColors()) {
            if (elementColor.getName().equalsIgnoreCase(name)) {
                elementColor.setColor(new Color(argb, true));
                return;
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSV || draggingH) save();
        draggingSV = false;
        draggingH = false;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) return;

        float w = scaled(WIDTH);
        float h = scaled(HEIGHT);
        float headerH = scaled(HEADER_H);
        float cx = getX() - w * (1f - (float) openAnimation.getValue());
        float cy = getY();

        float presetAreaTop = cy + h - scaled(FIXED_BOTTOM_H);
        if (MouseUtil.isHovered(mouseX, mouseY, cx, presetAreaTop, w, scaled(FIXED_BOTTOM_H))) {
            double delta = Math.abs(verticalAmount) > 0.001 ? verticalAmount : -horizontalAmount;
            presetScroll += (float) (delta * scaled(600f));
            clampPresetScroll(w - scaled(8f));
            return;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, cx, cy + headerH, w, scrollAreaH(h, headerH))) {
            scroll += (float) (verticalAmount * scaled(20f));
            clampScroll(editableColors().size() * scaled(ROW_H) + scaled(2f), scrollAreaH(h, headerH));
        }
    }

    public boolean charTyped(char chr) {
        if (!nameFieldFocused) return false;
        if (chr >= 32 && themeName.length() < 24) themeName += chr;
        return true;
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!nameFieldFocused) return;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !themeName.isEmpty()) {
            themeName = themeName.substring(0, themeName.length() - 1);
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) nameFieldFocused = false;
    }
}
