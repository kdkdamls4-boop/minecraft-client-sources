package tokyo.ghoul.client.ui.clickgui.module;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tokyo.ghoul.api.module.setting.Setting;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.client.ui.UIComponent;

@Getter
@RequiredArgsConstructor
public abstract class SettingComponent extends UIComponent {
    private final Setting<?> setting;
    private final AnimationUtil visibleAnimation = new AnimationUtil();

    public void updateHeight(float value) {
        setHeight(scaled(value));
    }

    public float labelTextSize() {
        return scaled(6f);
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {}
    public void mouseReleased(double mouseX, double mouseY, int button) {}
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
    public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    public void charTyped(char codePoint, int modifiers) {}
}
