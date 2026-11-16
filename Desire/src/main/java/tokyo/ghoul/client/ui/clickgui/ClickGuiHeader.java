package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;

import java.awt.Color;

public class ClickGuiHeader {
    private static final Color MUTED = new Color(140, 140, 140);

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha,
                       float contentAlpha, ClickGuiTab tab, boolean themeView,
                       String searchText, boolean searching) {
        var matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);

        RenderUtil.RECT.draw(matrices,
                windowX + ClickGuiLayout.sidebarWidth(), windowY,
                ClickGuiLayout.scaled(0.5f), ClickGuiLayout.windowHeight(),
                0f, ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int) (full * 0.22f)));
        RenderUtil.RECT.draw(matrices,
                windowX + ClickGuiLayout.sidebarWidth(), windowY + ClickGuiLayout.headerHeight(),
                ClickGuiLayout.contentWidth(), ClickGuiLayout.scaled(0.5f),
                0f, ColorUtil.setAlpha(UIColors.inactiveTextColor(), (int) (full * 0.22f)));

        float leftX = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(16f);
        float midY = windowY + ClickGuiLayout.headerHeight() / 2f;
        float titleSize = ClickGuiLayout.scaled(12f);
        float titleY = midY - titleSize / 2f;
        String crumb = tab.getLabel() + " /";
        Color crumbColor = ColorUtil.setAlpha(MUTED, full);
        Color titleColor = ColorUtil.setAlpha(Color.WHITE, full);

        Fonts.SF_MEDIUM.drawText(matrices, crumb, leftX, titleY, titleSize, crumbColor);
        float crumbW = Fonts.SF_MEDIUM.getWidth(crumb, titleSize);

        float iconSize = titleSize;
        float iconX = leftX + crumbW + ClickGuiLayout.scaled(8f);
        ClickGuiIcons.drawTabIcon(iconX, titleY, iconSize, tab, titleColor);

        float titleX = iconX + iconSize + ClickGuiLayout.scaled(6f);
        Fonts.SF_MEDIUM.drawText(matrices, tab.getLabel(), titleX, titleY, titleSize, titleColor);

        float btn = buttonSize();
        float gap = ClickGuiLayout.scaled(6f);
        float searchW = searchHitW();
        float right = windowX + ClickGuiLayout.windowWidth() - ClickGuiLayout.scaled(14f);
        float searchX = right - searchW;
        float folderX = searchX - gap - btn;
        float gearX = folderX - gap - btn;
        float btnY = midY - btn / 2f;

        float glyph = ClickGuiLayout.scaled(11f);
        ClickGuiIcons.draw(gearX + (btn - glyph) / 2f, btnY + (btn - glyph) / 2f, glyph,
                ClickGuiIcons.SETTINGS, titleColor);
        ClickGuiIcons.draw(folderX + (btn - glyph) / 2f, btnY + (btn - glyph) / 2f, glyph,
                ClickGuiIcons.GRID, titleColor);

        String placeholder = "Search settings";
        if (searching && (searchText == null || searchText.isEmpty())) {
            long period = placeholder.length() * 140L + 900L;
            long elapsed = System.currentTimeMillis() % period;
            placeholder = placeholder.substring(0, (int) Math.min(placeholder.length(), elapsed / 140L));
        }
        boolean empty = searchText == null || searchText.isEmpty();
        String visible = empty ? placeholder : searchText;
        Color searchColor = empty
                ? ColorUtil.setAlpha(MUTED, full)
                : titleColor;

        float magSize = ClickGuiLayout.scaled(10f);
        float magX = searchX + ClickGuiLayout.scaled(10f);
        float magBaseline = (Fonts.SF_MEDIUM.getMetrics().baselineHeight()
                - Fonts.MINCED_ICONS.getMetrics().baselineHeight()) * magSize;
        Fonts.MINCED_ICONS.drawText(matrices, "l", magX, btnY + (btn - magSize) / 2f + magBaseline,
                magSize, titleColor);

        float searchTextSize = ClickGuiLayout.scaled(11f);
        float searchTextX = magX + Fonts.MINCED_ICONS.getWidth("l", magSize) + ClickGuiLayout.scaled(6f);
        Fonts.SF_MEDIUM.drawText(matrices, visible, searchTextX, midY - searchTextSize / 2f,
                searchTextSize, searchColor);

        if (searching) {
            float cursor = (float) ((Math.sin(System.currentTimeMillis() / 320.0) + 1.0) / 2.0);
            float textW = Fonts.SF_MEDIUM.getWidth(visible, searchTextSize);
            RenderUtil.RECT.draw(matrices,
                    searchTextX + textW + ClickGuiLayout.scaled(1f),
                    midY - ClickGuiLayout.scaled(5f),
                    ClickGuiLayout.scaled(0.6f), ClickGuiLayout.scaled(10f), 0f,
                    ColorUtil.setAlpha(Color.WHITE, (int) (cursor * alpha * 255f)));
        }
    }

    public boolean themeClicked(double mouseX, double mouseY, float windowX, float windowY) {
        return MouseUtil.isHovered(mouseX, mouseY, gearX(windowX), buttonY(windowY), buttonSize(), buttonSize());
    }

    public boolean configsClicked(double mouseX, double mouseY, float windowX, float windowY) {
        return MouseUtil.isHovered(mouseX, mouseY, folderX(windowX), buttonY(windowY), buttonSize(), buttonSize());
    }

    public static float buttonSize() {
        return ClickGuiLayout.scaled(26f);
    }

    public static float buttonY(float windowY) {
        return windowY + (ClickGuiLayout.headerHeight() - buttonSize()) / 2f;
    }

    public static float searchHitW() {
        return ClickGuiLayout.scaled(128f);
    }

    public static float searchHitH() {
        return buttonSize();
    }

    public static float searchHitX(float windowX) {
        return windowX + ClickGuiLayout.windowWidth() - ClickGuiLayout.scaled(14f) - searchHitW();
    }

    public static float searchHitY(float windowY) {
        return buttonY(windowY);
    }

    public static float folderX(float windowX) {
        return searchHitX(windowX) - ClickGuiLayout.scaled(6f) - buttonSize();
    }

    public static float gearX(float windowX) {
        return folderX(windowX) - ClickGuiLayout.scaled(6f) - buttonSize();
    }

    public static float brushX(float windowX) {
        return gearX(windowX);
    }

    public static float brushY(float windowY) {
        return buttonY(windowY);
    }

    public static float brushSize() {
        return buttonSize();
    }
}
