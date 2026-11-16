package tokyo.ghoul.client.ui.clickgui.module.settings;

import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.module.SettingComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public class SliderComponent extends SettingComponent {
    private final SliderSetting setting;
    private boolean dragging;
    private float currentWidth;
    private float previewValue;

    private final AnimationUtil dragAnimation = new AnimationUtil();

    public SliderComponent(SliderSetting setting) {
        super(setting);
        this.setting = setting;
        this.previewValue = setting.getValue();
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        dragAnimation.update();
        dragAnimation.run(dragging ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        PoseStack matrixStack = RenderUtil.matrices();

        float bigPenis = dragging ? previewValue : setting.getValue();
        float fontSize = fontSize();
        String valueText = String.valueOf(bigPenis);
        float valueWidth = Fonts.PS_REGULAR.getWidth(valueText, fontSize);
        float piska = scaled(0.5f);

        int fullAlpha = (int) (getAlpha() * 255f);

        float progress = (bigPenis - setting.getMin()) / (setting.getMax() - setting.getMin()) * sliderWidth();
        currentWidth = MathUtil.interpolate(currentWidth, progress, 0.2f);

        Fonts.PS_REGULAR.drawText(matrixStack, setting.getName(), getX() + piska, getY() + piska, fontSize, UIColors.textColor(fullAlpha));
        float valuePad = scaled(3f);
        float valueBoxW = valueWidth + valuePad * 2f;
        float valueBoxH = fontSize + scaled(3f);
        float valueBoxX = getX() + getWidth() - valueBoxW;
        RenderUtil.RECT.draw(matrixStack, valueBoxX, getY() - scaled(1.5f), valueBoxW, valueBoxH,
                scaled(2.5f), ColorUtil.setAlpha(UIColors.primary(), (int) (125f * getAlpha())));
        Fonts.PS_REGULAR.drawText(matrixStack, valueText, valueBoxX + valuePad, getY() + piska,
                fontSize, ColorUtil.setAlpha(Color.WHITE, fullAlpha));

        float sliderRound = sliderHeight() * 0.3f;
        float knobX = Mth.clamp(sliderX() + currentWidth - knobSize() / 2f, sliderX(), sliderX() + sliderWidth() - knobSize());

        Color knobColor = ColorUtil.setAlpha(Color.WHITE, fullAlpha);

        float hui = (knobSize() - sliderHeight()) / 2f;

        Color theme = UIColors.primary();
        Color inactiveTrack = new Color((int) (theme.getRed() * 0.22f), (int) (theme.getGreen() * 0.22f),
                (int) (theme.getBlue() * 0.22f), fullAlpha);
        RenderUtil.RECT.draw(matrixStack, sliderX(), sliderY(), sliderWidth(), sliderHeight(), sliderRound, inactiveTrack);
        RenderUtil.RECT.draw(matrixStack, sliderX(), sliderY(), currentWidth, sliderHeight(), sliderRound, UIColors.primary(fullAlpha));
        RenderUtil.RECT.draw(matrixStack, knobX, sliderY() - hui, knobSize(), knobSize(), knobSize() * 0.4f, knobColor);

        setHeight(sliderHeight() + (sliderY() - getY()) + knobSize() / 2f);

        if (dragging) {
            float newValue = (mouseX - getX()) / sliderWidth();
            newValue = setting.getMin() + newValue * (setting.getMax() - setting.getMin());
            newValue = Math.round(newValue / setting.getStep()) * setting.getStep();
            previewValue = MathUtil.round(Math.max(setting.getMin(), Math.min(setting.getMax(), newValue)), setting.getStep());
        }

        if (dragging && !MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.setValue(previewValue);
            dragging = false;
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            dragging = true;
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (dragging) {
            setting.setValue(previewValue);
        }
        dragging = false;
    }

    private float fontSize() { return labelTextSize(); }
    private float sliderWidth() { return getWidth(); }
    private float sliderHeight() { return scaled(2.5f); }
    private float knobSize() { return scaled(5f); }
    private float sliderY() { return getY() + fontSize() + knobSize() / 2f; }
    private float sliderX() { return getX(); }
    private float getDefaultHeight() { return fontSize() + gap() + knobSize(); }

    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
}
