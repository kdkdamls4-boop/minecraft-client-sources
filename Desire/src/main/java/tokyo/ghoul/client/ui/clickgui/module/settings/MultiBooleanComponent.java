package tokyo.ghoul.client.ui.clickgui.module.settings;

import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.module.ExpandableComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MultiBooleanComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final MultiBooleanSetting setting;

    private final List<BooleanSetting> values = new ArrayList<>();
    private final Map<BooleanSetting, AnimationUtil> animations = new HashMap<>();
    private final List<Bound> bounds = new ArrayList<>();

    public MultiBooleanComponent(MultiBooleanSetting setting) {
        super(setting);
        this.setting = setting;

        float tileSize = labelTextSize() * 0.9f;
        updateHeight(getDefaultHeight() + tileSize * 1.8f + 2f);

        for (BooleanSetting value : setting.getValue()) {
            values.add(value);
            AnimationUtil anim = new AnimationUtil();
            anim.setValue(value.getValue() ? 1.0 : 0.0);
            animations.put(value, anim);
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = RenderUtil.matrices();

        float fontSize = labelTextSize();
        float scd = scaled(getDefaultHeight());
        float zavoz = offset();
        int fullAlpha = (int) (getAlpha() * 255f);

        int enabledCount = 0;
        int visibleCount = 0;
        for (BooleanSetting value : values) {
            if (!value.isVisible()) continue;
            visibleCount++;
            if (value.getValue()) enabledCount++;
        }

        String counter = enabledCount + " из " + visibleCount;
        float counterWidth = Fonts.PS_REGULAR.getWidth(counter, fontSize);
        Fonts.PS_REGULAR.drawWrap(matrixStack, setting.getName(), getX() + zavoz, getY() + scd / 2f - fontSize / 2f,
                getWidth() - zavoz * 2f - counterWidth - gap(), fontSize, UIColors.textColor(fullAlpha),
                scaled(16f), Duration.ofMillis(3000), Duration.ofMillis(500));
        Fonts.PS_REGULAR.drawText(matrixStack, counter, getX() + getWidth() - zavoz - counterWidth,
                getY() + scd / 2f - fontSize / 2f, fontSize, ColorUtil.setAlpha(Color.WHITE, fullAlpha));

        bounds.clear();
        float defX = getX() + zavoz;
        float currentX = defX;
        float currentY = getY() + scd;
        float tileSize = fontSize * 0.9f;
        float tileHeight = tileSize * 1.8f;
        float tilePadding = gap();

        for (BooleanSetting value : values) {
            if (!value.isVisible()) continue;

            AnimationUtil anim = animations.get(value);
            anim.update();
            anim.run(value.getValue() ? 1.0 : 0.0, 500, Easing.EXPO_OUT);
            float on = (float) anim.getValue();

            float textWidth = Fonts.PS_REGULAR.getWidth(value.getName(), tileSize);
            float tileWidth = textWidth + tileSize;

            if (currentX + tileWidth + tilePadding > getX() + getWidth()) {
                currentX = defX;
                currentY += tileHeight + tilePadding;
            }

            bounds.add(new Bound(currentX, currentY, tileWidth, tileHeight, value));

            Color theme = UIColors.primary();
            Color inactive = new Color((int) (theme.getRed() * 0.18f), (int) (theme.getGreen() * 0.18f), (int) (theme.getBlue() * 0.18f));
            Color rectColor = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.primary(), inactive, on), fullAlpha);
            Color textColor = ColorUtil.setAlpha(ColorUtil.interpolate(Color.WHITE, new Color(205, 195, 210), on), fullAlpha);

            RenderUtil.RECT.draw(matrixStack, currentX, currentY, tileWidth, tileHeight, tileHeight * 0.2f, rectColor);
            Fonts.PS_REGULAR.drawCenteredText(matrixStack, value.getName(), currentX + tileWidth / 2f, currentY + tileHeight / 2f - tileSize / 2f, tileSize, textColor);

            currentX += tileWidth + tilePadding;
        }

        setHeight(currentY - getY() + tileHeight + gap());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                bound.value.toggle();
            }
        }
    }

    private float getDefaultHeight() {
        return 15f;
    }

    private record Bound(float x, float y, float width, float height, BooleanSetting value) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
