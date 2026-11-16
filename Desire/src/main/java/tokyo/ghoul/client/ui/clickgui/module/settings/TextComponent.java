package tokyo.ghoul.client.ui.clickgui.module.settings;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.ClickGuiLayout;
import tokyo.ghoul.client.ui.clickgui.module.SettingComponent;

import java.awt.Color;

public class TextComponent extends SettingComponent implements QuickImports {
    private final TextSetting setting;
    private boolean focused;
    private final AnimationUtil focusAnimation = new AnimationUtil();

    public TextComponent(TextSetting setting) {
        super(setting);
        this.setting = setting;
        updateHeight(getDefaultHeight());
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateHeight(getDefaultHeight());

        focusAnimation.update();
        focusAnimation.run(focused ? 1.0 : 0.0, 300, Easing.EXPO_OUT);

        PoseStack matrices = RenderUtil.matrices();
        int fullAlpha = (int) (getAlpha() * 255f);
        float fontSize = labelTextSize();
        float piska = scaled(0.5f);

        Fonts.PS_REGULAR.drawText(matrices, setting.getName(), getX() + piska, getY() + piska, fontSize, UIColors.textColor(fullAlpha));

        float boxW = ClickGuiLayout.scaled(62f);
        float boxH = fontSize + ClickGuiLayout.scaled(5f);
        float boxX = getX() + getWidth() - boxW;
        float boxY = getY() - ClickGuiLayout.scaled(1f);
        float round = ClickGuiLayout.scaled(3f);

        Color boxBg = ColorUtil.setAlpha(new Color(20, 24, 28), (int) (180f * getAlpha()));
        RenderUtil.RECT.draw(matrices, boxX, boxY, boxW, boxH, round, boxBg);

        float focusAnim = (float) focusAnimation.getValue();
        if (focusAnim > 0.01f) {
            Color borderColor = ColorUtil.setAlpha(UIColors.primary(), (int) (200f * focusAnim * getAlpha()));
            RenderUtil.RECT.draw(matrices, boxX, boxY, boxW, boxH, round, borderColor);
            RenderUtil.RECT.draw(matrices, boxX + 1f, boxY + 1f, boxW - 2f, boxH - 2f, round - 0.5f, boxBg);
        }

        String text = setting.getText();
        boolean blink = focused && ((System.currentTimeMillis() / 500L) % 2 == 0);
        String displayText = text + (blink ? "|" : "");
        if (text.isEmpty() && !focused) {
            displayText = setting.getPlaceholder().isEmpty() ? "..." : setting.getPlaceholder();
        }

        Color textColor = (text.isEmpty() && !focused)
                ? ColorUtil.setAlpha(Color.GRAY, fullAlpha)
                : ColorUtil.setAlpha(Color.WHITE, fullAlpha);

        ScissorUtil.start(matrices, boxX + ClickGuiLayout.scaled(3f), boxY, boxW - ClickGuiLayout.scaled(6f), boxH);
        float textW = Fonts.PS_REGULAR.getWidth(displayText, fontSize * 0.9f);
        float drawTextX = boxX + ClickGuiLayout.scaled(3f);
        if (textW > boxW - ClickGuiLayout.scaled(6f)) {
            drawTextX = boxX + boxW - ClickGuiLayout.scaled(3f) - textW;
        }
        Fonts.PS_REGULAR.drawText(matrices, displayText, drawTextX, boxY + (boxH - fontSize * 0.9f) / 2f, fontSize * 0.9f, textColor);
        ScissorUtil.stop(matrices);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        float boxW = ClickGuiLayout.scaled(62f);
        float boxH = labelTextSize() + ClickGuiLayout.scaled(5f);
        float boxX = getX() + getWidth() - boxW;
        float boxY = getY() - ClickGuiLayout.scaled(1f);

        if (button == 0) {
            if (MouseUtil.isHovered(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
                focused = true;
            } else {
                focused = false;
            }
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!focused) return;
        if (Character.isISOControl(codePoint)) return;
        setting.setText(setting.getText() + codePoint);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            focused = false;
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            String text = setting.getText();
            if (!text.isEmpty()) {
                setting.setText(text.substring(0, text.length() - 1));
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            try {
                String clipboard = mc.keyboardHandler.getClipboard();
                if (clipboard != null && !clipboard.isEmpty()) {
                    setting.setText(setting.getText() + clipboard.trim());
                }
            } catch (Exception ignored) {}
        }
    }

    private float getDefaultHeight() {
        return 12f;
    }
}
