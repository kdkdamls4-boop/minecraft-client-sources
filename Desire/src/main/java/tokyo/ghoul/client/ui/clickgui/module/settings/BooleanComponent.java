package tokyo.ghoul.client.ui.clickgui.module.settings;

import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.module.SettingComponent;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.*;
import java.time.Duration;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class BooleanComponent extends SettingComponent {
    private final BooleanSetting setting;
    private final AnimationUtil toggleAnimation = new AnimationUtil();

    public BooleanComponent(BooleanSetting setting) {
        this(setting, false);
    }

    public BooleanComponent(BooleanSetting setting, boolean inMenu) {
        super(setting);
        this.setting = setting;
        updateHeight(15f);
        toggleAnimation.setValue(setting.getValue() ? 1.0 : 0.0);
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(15f);

        toggleAnimation.update();
        toggleAnimation.run(setting.getValue() ? 1.0 : 0.0, 100, Easing.SINE_OUT);

        PoseStack matrixStack = RenderUtil.matrices();
        float fontSize = labelTextSize();
        int fullAlpha = (int) (getAlpha() * 255f);
        float anim = (float) toggleAnimation.getValue();

        float checkHeight = getHeight() * 0.55f;
        float checkWidth = checkHeight * 1.9f;
        float checkX = getX() + getWidth() - checkWidth;
        float checkY = getY() + getHeight() / 2f - checkHeight / 2f;
        float checkRound = checkHeight * 0.5f;

        float baseKnob = checkHeight * 0.78f;
        float knobGap = (checkHeight - baseKnob) * 0.8f;
        float knobSize = baseKnob - knobGap;
        float knobPad = knobGap * 1.3f;
        float knobY = getY() + getHeight() / 2f - knobSize / 2f;
        float knobX = checkX + knobPad + ((checkWidth - knobSize - knobPad * 2f) * anim);
        float knobRound = knobSize * 0.5f;

        Fonts.PS_MEDIUM.drawWrap(matrixStack, setting.getName(),
                getX(), getY() + getHeight() / 2f - fontSize / 2f,
                getWidth() - checkWidth - knobGap, fontSize,
                UIColors.textColor(fullAlpha), scaled(16f),
                Duration.ofMillis(3000), Duration.ofMillis(500));

        Color track = ColorUtil.interpolate(UIColors.primary(fullAlpha), UIColors.widgetBlur(fullAlpha), anim);
        RenderUtil.RECT.draw(matrixStack, checkX, checkY, checkWidth, checkHeight, checkRound, track);
        RenderUtil.RECT.draw(matrixStack, knobX, knobY, knobSize, knobSize, knobRound,
                ColorUtil.setAlpha(Color.WHITE, fullAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            setting.toggle();
        }
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
