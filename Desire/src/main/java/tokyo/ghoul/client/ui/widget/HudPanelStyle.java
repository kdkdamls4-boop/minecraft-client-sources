package tokyo.ghoul.client.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;

import java.awt.Color;

@UtilityClass
public class HudPanelStyle {
    public final String CHEVRON = "\u00BB";

    public void surface(PoseStack ms, float x, float y, float width, float height,
                        float corner, float shadowBlur, float alpha, boolean liquidGlass) {
        if (alpha <= 0.001f) return;

        RenderUtil.RECT.drawGlassShadow(ms, x, y, width, height, corner, shadowBlur,
                new Color(0, 0, 0, (int) (120f * alpha)));

        if (liquidGlass) {
            RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, corner,
                    new Color(255, 255, 255, (int) (255f * alpha)), 0f);
            RenderUtil.RECT.draw(ms, x, y, width, height, corner,
                    new Color(14, 15, 20, (int) (155f * alpha)));
        } else {
            RenderUtil.RECT.draw(ms, x, y, width, height, corner,
                    new Color(14, 15, 20, (int) (235f * alpha)));
        }
    }

    public void header(PoseStack ms, float x, float y, float width, float headerH, float corner, float alpha) {
        ScissorUtil.start(ms, x, y, width, headerH);
        RenderUtil.RECT.draw(ms, x, y, width, headerH + corner, corner,
                new Color(8, 9, 13, (int) (92f * alpha)));
        ScissorUtil.stop(ms);

        RenderUtil.RECT.draw(ms, x + 1f, y + headerH - 0.4f, width - 2f, 0.8f, 0f,
                new Color(255, 255, 255, (int) (22f * alpha)));
    }

    public float chevronWidth(float size) {
        return Fonts.HUD_GUILLEMET.getWidth(CHEVRON, size);
    }

    public float headerWidth(float padX, float iconSize, float gap, float chevronSize, float titleWidth) {
        return padX + iconSize + gap + chevronWidth(chevronSize) + gap + titleWidth + padX;
    }

    public void headerText(PoseStack ms, float iconX, float iconSize, float gap, float centerY,
                           float chevronSize, float titleSize, String title, float alpha) {
        float chevronX = iconX + iconSize + gap;
        float chevronY = centerY - chevronSize / 2f + chevronSize * 0.06f;
        Fonts.HUD_GUILLEMET.drawText(ms, CHEVRON, chevronX, chevronY, chevronSize,
                ColorUtil.setAlpha(UIColors.primary(), (int) (205f * alpha)));

        Fonts.HUD_SEMIBOLD.drawText(ms, title, chevronX + chevronWidth(chevronSize) + gap,
                centerY - titleSize / 2f + titleSize * 0.067f, titleSize,
                ColorUtil.setAlpha(Color.WHITE, (int) (240f * alpha)));
    }

    public float valueX(float panelX, float panelW, float padX, float iconSize, float gap, float valueWidth) {
        return panelX + panelW - padX - iconSize - gap - valueWidth;
    }

    public float rowTextY(float rowY, float rowH, float textSize) {
        return rowY + (rowH - textSize) / 2f + textSize * 0.07f;
    }
}
