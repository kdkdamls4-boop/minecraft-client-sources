package tokyo.ghoul.client.ui.clickgui.module.settings;

import tokyo.ghoul.api.module.setting.ModeSetting;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class ModeComponent extends ExpandableComponent.ExpandableSettingComponent {
    private final ModeSetting setting;

    private final List<Bound> bounds = new ArrayList<>();
    private final Map<String, AnimationUtil> modeAnimations = new HashMap<>();

    public ModeComponent(ModeSetting setting) {
        super(setting);
        this.setting = setting;

        float tileSize = labelTextSize() * 0.9f;
        updateHeight(getDefaultHeight() + tileSize * 1.8f + 2f);

        for (String mode : setting.getModes()) {
            modeAnimations.put(mode, new AnimationUtil());
        }
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        PoseStack matrixStack = RenderUtil.matrices();

        float fontSize = labelTextSize();
        float scd = scaled(getDefaultHeight());
        float zavoz = offset();

        int fullAlpha = (int) (getAlpha() * 255f);

        String name = setting.getName();
        String valueText = setting.getValue();
        float valueWidth = Fonts.PS_REGULAR.getWidth(valueText, fontSize);
        float textY = getY() + scd / 2f - fontSize / 2f;

        Fonts.PS_REGULAR.drawText(matrixStack, name, getX() + zavoz, textY, fontSize, UIColors.textColor(fullAlpha));
        Fonts.PS_REGULAR.drawText(matrixStack, valueText, getX() + getWidth() - zavoz - valueWidth, textY, fontSize, UIColors.primary(fullAlpha));

        bounds.clear();
        float defX = getX() + zavoz;
        float currentX = defX;
        float currentY = getY() + scd;
        float tileSize = fontSize * 0.9f;
        float tileHeight = tileSize * 1.8f;
        float tilePadding = gap();

        for (String mode : setting.getModes()) {
            AnimationUtil modeAnim = modeAnimations.get(mode);

            modeAnim.update();
            modeAnim.run(setting.is(mode) ? 1.0 : 0.0, 500, Easing.EXPO_OUT);

            float textWidth = Fonts.PS_REGULAR.getWidth(mode, tileSize);
            float tileWidth = textWidth + tileSize;

            if (currentX + tileWidth + tilePadding > getX() + getWidth()) {
                currentX = defX;
                currentY += tileHeight + tilePadding;
            }

            bounds.add(new Bound(currentX, currentY, tileWidth, tileHeight, mode));

            Color theme = UIColors.primary();
            Color inactive = new Color((int) (theme.getRed() * 0.18f), (int) (theme.getGreen() * 0.18f), (int) (theme.getBlue() * 0.18f));
            Color rectColor = ColorUtil.setAlpha(ColorUtil.interpolate(UIColors.primary(), inactive, modeAnim.getValue()), fullAlpha);
            Color textColor = ColorUtil.setAlpha(ColorUtil.interpolate(Color.WHITE, new Color(205, 195, 210), modeAnim.getValue()), fullAlpha);

            RenderUtil.RECT.draw(matrixStack, currentX, currentY, tileWidth, tileHeight, tileHeight * 0.2f, rectColor);
            Fonts.PS_REGULAR.drawCenteredText(matrixStack, mode, currentX + tileWidth / 2f, currentY + tileHeight / 2f - tileSize / 2f, tileSize, textColor);

            currentX += tileWidth + tilePadding;
        }

        setHeight(currentY - getY() + tileHeight + gap());
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        for (Bound bound : bounds) {
            if (MouseUtil.isHovered(mouseX, mouseY, bound.x, bound.y, bound.width, bound.height)) {
                setting.setValue(bound.value);
            }
        }
    }

    private float getDefaultHeight() {
        return 15f;
    }

    private record Bound(float x, float y, float width, float height, String value) {}

    @Override public void keyPressed(int keyCode, int scanCode, int modifiers) {}
    @Override public void mouseReleased(double mouseX, double mouseY, int button) {}
    @Override public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {}
}
