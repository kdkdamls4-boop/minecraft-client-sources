package tokyo.ghoul.client.ui.theme;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.UIComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;

@Getter
public class ThemeSelectable extends UIComponent {
    private final Theme theme;
    private final AnimationUtil hoverAnimation = new AnimationUtil();
    private final AnimationUtil enableAnimation = new AnimationUtil();

    public ThemeSelectable(Theme theme) {
        this.theme = theme;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = RenderUtil.matrices();

        boolean hovered = hovered(mouseX, mouseY);

        enableAnimation.update();
        enableAnimation.run(UIColors.currentTheme().getName().equals(theme.getName()) ? 1.0 : 0.7, 200, Easing.SINE_OUT);

        hoverAnimation.update();
        hoverAnimation.run(hovered ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

        int fullAlpha = (int) (getAlpha() * enableAnimation.getValue() * 255f);

        Color primary = ColorUtil.setAlpha(theme.getPrimaryColor(), fullAlpha);
        Color secondary = ColorUtil.setAlpha(theme.getSecondaryColor(), fullAlpha);

        float round = getHeight() * 0.2f;
        float fontSize = (float) (getHeight() * 0.4f + hoverAnimation.getValue());

        RenderUtil.GRADIENT_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), round, primary, secondary, primary, secondary);
        Fonts.PS_REGULAR.drawCenteredText(matrixStack, theme.getName(), getX() + getWidth() / 2f, getY() + getHeight() / 2f - fontSize / 2f, fontSize, ColorUtil.setAlpha(theme.getTextColor(), fullAlpha));
    }

    private boolean hovered(float mouseX, float mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight());
    }

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseClicked(double mouseX, double mouseY, int button) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
