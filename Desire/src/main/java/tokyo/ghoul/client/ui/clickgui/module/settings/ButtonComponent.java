package tokyo.ghoul.client.ui.clickgui.module.settings;

import org.joml.Vector4f;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.module.SettingComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ButtonComponent extends SettingComponent {
    private final RunSetting setting;

    private final AnimationUtil hoverAnimation = new AnimationUtil();

    public ButtonComponent(RunSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        PoseStack matrixStack = RenderUtil.matrices();
        int fullAlpha = (int) (getAlpha() * 255f);

        hoverAnimation.update();
        hoverAnimation.run(hovered(mouseX, mouseY) ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        Color buttonColor1 = ColorUtil.interpolate(UIColors.gradient(0, fullAlpha), UIColors.widgetBlur(fullAlpha), hoverAnimation.getValue());
        Color buttonColor2 = ColorUtil.interpolate(UIColors.gradient(90, fullAlpha), UIColors.widgetBlur(fullAlpha), hoverAnimation.getValue());

        float fontSize = labelTextSize() + scaled((float) hoverAnimation.getValue());
        float round = getWidth() * 0.04f;
        RenderUtil.GRADIENT_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), new Vector4f(round), buttonColor1, buttonColor2, buttonColor1, buttonColor2);
        Fonts.PS_REGULAR.drawCenteredText(matrixStack, setting.getName(), getX() + getWidth() / 2f, getY() + getHeight() / 2f - fontSize / 2f, fontSize, ColorUtil.setAlpha(UIColors.textColor(), fullAlpha));
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (hovered(mouseX, mouseY)) {
            if (setting.getValue() != null) {
                setting.getValue().run();
            }
        }
    }

    private boolean hovered(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getDefaultHeight()));
    }

    private float getDefaultHeight() {
        return 15f;
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
