package tokyo.ghoul.client.ui.clickgui;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Vector4f;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.WindowResizeEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.UIComponent;
import tokyo.ghoul.client.ui.clickgui.module.ModuleComponent;
import tokyo.ghoul.client.ui.clickgui.module.SettingComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Panel extends UIComponent {
    public static final float SCALE = 1f / 1.5f;

    public static final Color BLUR_BG = new Color(32, 32, 32);
    public static final Color BODY_BG = new Color(45, 45, 45);
    public static final Color HEADER_BG = new Color(20, 20, 20);
    public static final Color WINDOW_BG = new Color(16, 16, 18);

    private final Category category;
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();

    @Setter private int categoryIndex;

    private double scroll = 0f;
    private final AnimationUtil scrollAnimation = new AnimationUtil();

    public Panel(Category category) {
        this.category = category;

        setScaleMultiplier(SCALE);

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isHidden()) continue;
            if (module.getCategory() == category) {
                ModuleComponent moduleComponent = new ModuleComponent(module);
                moduleComponent.setScaleMultiplier(SCALE);
                moduleComponents.add(moduleComponent);
            }
        }

        int index = categoryIndex;
        for (ModuleComponent module : moduleComponents) {
            module.setIndex(index);
            index += 45;
        }

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
        }));
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        updateThings();
        renderThings(context, mouseX, mouseY, delta);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(moduleComponents -> {
            moduleComponents.keyPressed(keyCode, scanCode, modifiers);
        });
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (inPanel(mouseX, mouseY)) {
            for (ModuleComponent module : moduleComponents) {
                if (!MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) continue;

                module.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (inPanel(mouseX, mouseY)) {
            for (ModuleComponent module : moduleComponents) {
                if (!MouseUtil.isHovered(mouseX, mouseY, module.getX(), module.getY(), module.getWidth(), module.getHeight())) continue;

                module.mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) {
            scroll += verticalAmount * scaled(15f);
        }
    }

    private void updateThings() {
        scrollAnimation.update();
        scrollAnimation.run(scroll, 600, Easing.EXPO_OUT);

        setWidth(scaled(99f));
    }

    private void renderThings(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = RenderUtil.matrices();

        float fontSize = getHeaderHeight() * 0.425f;
        float moduleY = 0f;

        int fullAlpha = (int) (getAlpha() * 255f);
        float alphaRatio = getAlpha();

        calcModules(moduleY);

        float round = getRound();
        Vector4f allRound = new Vector4f(round, round, round, round);
        Vector4f topRound = new Vector4f(round, round, 0f, 0f);

        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), allRound, ColorUtil.setAlpha(BLUR_BG, fullAlpha));
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), allRound,
                ColorUtil.setAlpha(BODY_BG, (int) (150f * alphaRatio)));
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeaderHeight(), topRound,
                ColorUtil.setAlpha(HEADER_BG, (int) (200f * alphaRatio)));

        String icon = categoryIcon();
        float titleWidth = Fonts.PS_REGULAR.getWidth(category.getLabel(), fontSize);
        float iconWidth = Fonts.MINCED_ICONS.getWidth(icon, fontSize);
        float totalWidth = iconWidth + scaled(3f) + titleWidth;
        float startX = getX() + getWidth() / 2f - totalWidth / 2f - scaled(1f);
        float textY = getY() + getHeaderHeight() / 2f - fontSize / 2f;

        float baselineShift = (Fonts.PS_REGULAR.getMetrics().baselineHeight() - Fonts.MINCED_ICONS.getMetrics().baselineHeight()) * fontSize;
        Fonts.MINCED_ICONS.drawText(matrixStack, icon, startX, textY + baselineShift, fontSize, UIColors.primary(fullAlpha));
        Fonts.PS_REGULAR.drawText(matrixStack, category.getLabel(), startX + iconWidth + scaled(3f), textY, fontSize, UIColors.primary(fullAlpha));

        ScissorUtil.start(matrixStack, getX(), getY() + getHeaderHeight(), getWidth(), getHeight() - getHeaderHeight());
        for (ModuleComponent module : moduleComponents) {
            module.setAlpha(getAlpha());
            module.render(context, mouseX, mouseY, delta);
        }
        ScissorUtil.stop(matrixStack);
    }

    private void calcModules(float moduleY) {
        for (ModuleComponent module : moduleComponents) {
            float openAnim = module.getAnim();

            if (openAnim > 0f) {
                float settingOffset = 0.0f;

                for (SettingComponent setting : module.getSettings()) {
                    float visibleAnim = (float) setting.getVisibleAnimation().getValue();

                    if (visibleAnim > 0.0) {
                        settingOffset += (setting.getHeight() + gap()) * visibleAnim;
                    }
                }
                settingOffset *= openAnim;
                module.setHeight(module.getDefaultHeight() + (settingOffset + gap()) * openAnim);
            } else {
                module.setHeight(module.getDefaultHeight());
            }
            module.setWidth(getWidth() - offset() * 2f);
            module.setRound(getRound() * 0.5f);
            module.setX(getX() + offset());
            module.setY((float) (getY() + scrollAnimation.getValue() + moduleY + getHeaderHeight() + gap()));

            moduleY += module.getHeight() + gap();
        }

        float targetHeight = getHeaderHeight() + moduleY + gap();
        float screenCap = Minecraft.getInstance().getWindow().getGuiScaledHeight() - scaled(8f);
        setHeight(Math.max(getHeaderHeight() + gap() * 2f, Math.min(targetHeight, screenCap)));

        float maxScroll = Math.max(0f, moduleY - (getHeight() - getHeaderHeight() - gap()));
        scroll = Math.min(Math.max(scroll, -maxScroll), 0f);
    }

    public float getHeaderHeight() {
        return scaled(18f);
    }

    private String categoryIcon() {
        return switch (category) {
            case COMBAT -> "a";
            case MOVEMENT -> "b";
            case RENDER -> "c";
            case PLAYER -> "d";
            case OTHER -> "e";
            default -> "e";
        };
    }

    public float getRound() {
        return getHeaderHeight() / 2.2f;
    }

    public boolean inPanel(double mouseX, double mouseY) {
        return MouseUtil.isHovered(mouseX, mouseY, getX(), getY() + getHeaderHeight(), getWidth(), getHeight() - getHeaderHeight());
    }
}
