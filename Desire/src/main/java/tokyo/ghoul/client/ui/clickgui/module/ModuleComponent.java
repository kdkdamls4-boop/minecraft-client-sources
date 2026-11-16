package tokyo.ghoul.client.ui.clickgui.module;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.setting.*;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.ClickGuiIcons;
import tokyo.ghoul.client.ui.clickgui.ClickGuiLayout;
import tokyo.ghoul.client.ui.clickgui.module.settings.*;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ModuleComponent extends ExpandableComponent {
    private final List<SettingComponent> settings = new ArrayList<>();
    private final Module module;

    private boolean bind;
    @Setter private float round;
    @Setter private int index;

    private final AnimationUtil enableAnimation = new AnimationUtil();

    public ModuleComponent(Module module) {
        this.module = module;

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                settings.add(new BooleanComponent(bool));
            } else if (setting instanceof MultiBooleanSetting multi) {
                settings.add(new MultiBooleanComponent(multi));
            } else if (setting instanceof ModeSetting mode) {
                settings.add(new ModeComponent(mode));
            } else if (setting instanceof SliderSetting slider) {
                settings.add(new SliderComponent(slider));
            } else if (setting instanceof ColorSetting color) {
                settings.add(new ColorComponent(color));
            } else if (setting instanceof RunSetting run) {
                settings.add(new ButtonComponent(run));
            } else if (setting instanceof BindSetting bindSetting) {
                settings.add(new BindComponent(bindSetting));
            } else if (setting instanceof TextSetting textSetting) {
                settings.add(new TextComponent(textSetting));
            }
        }

        enableAnimation.setValue(module.isEnabled() ? 1.0 : 0.0);
    }

    public float headerHeight() {
        return ClickGuiLayout.scaled(29f);
    }

    public float getDefaultHeight() {
        return headerHeight();
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrices = RenderUtil.matrices();
        updateOpen();
        enableAnimation.update();
        enableAnimation.run(module.isEnabled() ? 1.0 : 0.0, 180, Easing.SINE_OUT);
        int fullAlpha = (int) (getAlpha() * 255f);
        float header = headerHeight();
        float pad = ClickGuiLayout.scaled(7f);
        float enabled = (float) enableAnimation.getValue();

        float stroke = ClickGuiLayout.scaled(0.75f);
        float radius = ClickGuiLayout.scaled(4f);
        RenderUtil.RECT.draw(matrices, getX(), getY(), getWidth(), getHeight(), radius,
                new Color(2, 4, 3, (int) (15 * getAlpha())));

        float openAnimBg = getAnim();
        if (openAnimBg > 0.01f && getHeight() > header) {
            int bodyAlpha = (int) (getAlpha() * 255f * openAnimBg);
            float bodyY = getY() + header;
            float bodyH = getY() + getHeight() - stroke - bodyY;
            Color bodyTint = ColorUtil.setAlpha(new Color(1, 3, 2),
                    (int) (26 * getAlpha() * openAnimBg));
            RenderUtil.RECT.draw(matrices, getX() + stroke, bodyY, getWidth() - stroke * 2f, bodyH, radius - stroke, bodyTint);
        }

        float titleSize = ClickGuiLayout.scaled(12f);
        float titleY = getY() + (header - titleSize) / 2f;
        String title = bind ? bindLabel() : module.getName();
        Color on = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
        Color off = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
        Color titleColor = ColorUtil.interpolate(on, off, enabled);

        Fonts.PS_MEDIUM.drawText(matrices, title, getX() + pad, titleY, titleSize, titleColor);

        float trackW = ClickGuiLayout.scaled(18f);
        float trackH = ClickGuiLayout.scaled(7.5f);
        float trackX = getX() + getWidth() - ClickGuiLayout.scaled(7f) - trackW;
        float trackY = getY() + (header - trackH) / 2f;
        Color accent = UIColors.primary(fullAlpha);
        RenderUtil.RECT.draw(matrices, trackX, trackY, trackW, trackH, trackH / 2f,
                enabled > 0.01f
                        ? ColorUtil.setAlpha(accent, (int) (fullAlpha * (0.55f + enabled * 0.45f)))
                        : ColorUtil.setAlpha(new Color(28, 34, 40), (int) (fullAlpha * 0.72f)));
        float knob = ClickGuiLayout.scaled(6f);
        RenderUtil.RECT.draw(matrices,
                trackX + ClickGuiLayout.scaled(1f) + (trackW - knob - ClickGuiLayout.scaled(2f)) * enabled,
                trackY + (trackH - knob) / 2f, knob, knob, knob / 2f,
                ColorUtil.setAlpha(Color.WHITE, fullAlpha));

        if (!settings.isEmpty()) {
            float gearSize = ClickGuiLayout.scaled(12f);
            Color gearColor = ColorUtil.setAlpha(Color.WHITE, fullAlpha);
            ClickGuiIcons.draw(trackX - ClickGuiLayout.scaled(21f), getY() + (header - gearSize) / 2f, gearSize,
                    ClickGuiIcons.SETTINGS, gearColor);
        }

        if (isOpen() || getAnim() > 0.01f) {
            layoutSettings(context, mouseX, mouseY, delta, header);
        } else {
            setHeight(header);
        }
    }

    private void layoutSettings(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta, float header) {
        float openAnim = getAnim();
        float y = getY() + header;
        float x = getX() + ClickGuiLayout.scaled(8f);
        float width = getWidth() - ClickGuiLayout.scaled(16f);
        boolean firstVisible = true;
        for (SettingComponent setting : settings) {
            setting.getVisibleAnimation().update();
            setting.getVisibleAnimation().run(setting.getSetting().isVisible() ? 1.0 : 0.0, 120, Easing.SINE_OUT);
            float visible = (float) setting.getVisibleAnimation().getValue() * openAnim;
            if (visible <= 0.01f) continue;
            if (firstVisible) {
                y += ClickGuiLayout.scaled(5f) * visible;
                firstVisible = false;
            }
            setting.setX(x);
            setting.setY(y);
            setting.setWidth(width);
            setting.setAlpha(visible * getAlpha());
            setting.render(context, mouseX, mouseY, delta);
            y += (setting.getHeight() + gap()) * visible;
        }

        setHeight(Math.max(header, y - getY() + ClickGuiLayout.scaled(4f)));
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bind) {
            boolean clear = keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE;
            if (clear) module.setBind(-999);
            else module.setBind(keyCode);
            bind = false;
            return;
        }
        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.4f) continue;
            setting.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public void charTyped(char codePoint, int modifiers) {
        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.4f) continue;
            setting.charTyped(codePoint, modifiers);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (bind && button > 2) {
            module.setBind(-100 + button);
            bind = false;
            return;
        }
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), headerHeight())) {
            if (button == 0) module.toggle();
            if (button == 1 && !settings.isEmpty()) toggleOpen();
            if (button == 2) bind = !bind;
            return;
        }

        for (SettingComponent setting : settings) {
            if (setting.getAlpha() < 0.4f) continue;
            setting.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        for (SettingComponent setting : settings) {
            setting.mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    }

    private String bindLabel() {
        if (!module.hasBind()) return "Binding...";
        return "Bind - " + KeyStorage.getBind(module.getBind());
    }
}
