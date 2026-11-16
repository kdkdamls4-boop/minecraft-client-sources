package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.DiscordRpc;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.WebTexture;
import tokyo.ghoul.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.EnumMap;
import java.util.Map;

public class ClickGuiSidebar {
    private static final String PROFILE_DATE = "26.09.2099";

    private final Map<ClickGuiTab, AnimationUtil> selectAnimations = new EnumMap<>(ClickGuiTab.class);
    private final StringBuilder search = new StringBuilder();
    private boolean searching;

    public ClickGuiSidebar() {
        for (ClickGuiTab tab : ClickGuiTab.sidebarTabs()) {
            AnimationUtil animation = new AnimationUtil();
            animation.setValue(tab == ClickGuiTab.COMBAT ? 1.0 : 0.0);
            selectAnimations.put(tab, animation);
        }
    }

    public void clearSearch() {
        search.setLength(0);
        setSearching(false);
    }

    public String query() {
        return search.toString();
    }

    public boolean isSearching() {
        return searching;
    }

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, ClickGuiTab selected) {
        var matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);
        float padX = ClickGuiLayout.scaled(10f);
        float x = windowX + padX;
        float itemW = ClickGuiLayout.sidebarWidth() - padX * 2f;
        float itemH = ClickGuiLayout.scaled(26f);
        float itemGap = ClickGuiLayout.scaled(2f);
        float sectionGap = ClickGuiLayout.scaled(10f);
        float sectionTitleSize = ClickGuiLayout.scaled(9f);
        float sectionTitleH = ClickGuiLayout.scaled(14f);

        float logo = ClickGuiLayout.scaled(42f);
        float logoX = windowX + (ClickGuiLayout.sidebarWidth() - logo) / 2f;
        float logoY = windowY + ClickGuiLayout.scaled(10f);
        drawLogoGlow(logoX, logoY, logo, alpha);
        ClickGuiIcons.drawGradientLogo(logoX, logoY, logo, full);

        float dividerY = logoY + logo + ClickGuiLayout.scaled(6f);
        RenderUtil.RECT.draw(matrices, x, dividerY, itemW, ClickGuiLayout.scaled(0.5f), 0f,
                ColorUtil.setAlpha(new Color(255, 255, 255), (int) (18f * alpha)));

        float profileY = dividerY + ClickGuiLayout.scaled(10f);
        float avaSize = ClickGuiLayout.scaled(18f);
        String profileName = DiscordRpc.getDiscordUsername();
        Identifier avatar = WebTexture.get(DiscordRpc.getAvatarUrl());
        float avaRadius = avaSize / 2f;
        if (avatar != null) {
            RenderUtil.TEXTURE_RECT.draw(matrices, x, profileY, avaSize, avaSize, avaRadius,
                    ColorUtil.setAlpha(Color.WHITE, full), 0f, 0f, 1f, 1f, avatar);
        } else {
            RenderUtil.RECT.draw(matrices, x, profileY, avaSize, avaSize, avaRadius,
                    new Color(46, 47, 54, full));
        }

        float nameSize = ClickGuiLayout.scaled(11f);
        float dateSize = ClickGuiLayout.scaled(8f);
        float nameX = x + avaSize + ClickGuiLayout.scaled(7f);
        float nameY = profileY + (avaSize - nameSize) / 2f;
        float dateY = profileY + (avaSize - dateSize) / 2f;
        Fonts.SF_MEDIUM.drawText(matrices, profileName, nameX, nameY, nameSize, ColorUtil.setAlpha(Color.WHITE, full));
        float nameW = Fonts.SF_MEDIUM.getWidth(profileName, nameSize);
        Fonts.SF_REGULAR.drawText(matrices, PROFILE_DATE,
                nameX + nameW + ClickGuiLayout.scaled(5f),
                dateY,
                dateSize,
                ColorUtil.setAlpha(new Color(150, 150, 150), (int) (0.85f * full)));

        float y = profileY + avaSize + ClickGuiLayout.scaled(14f);
        for (ClickGuiTab tab : ClickGuiTab.sidebarTabs()) {
            if (ClickGuiTab.startsSection(tab)) {
                if (tab != ClickGuiTab.COMBAT) y += sectionGap;
                String section = ClickGuiTab.sectionTitle(tab);
                Fonts.SF_REGULAR.drawText(matrices, section, x + ClickGuiLayout.scaled(2f), y,
                        sectionTitleSize, ColorUtil.setAlpha(new Color(130, 130, 130), (int) (0.9f * full)));
                y += sectionTitleH;
            }

            AnimationUtil animation = selectAnimations.get(tab);
            animation.update();
            animation.run(tab == selected ? 1.0 : 0.0, 180, Easing.SINE_OUT);
            float select = (float) animation.getValue();

            if (select > 0.01f) {
                RenderUtil.RECT.draw(matrices, x, y, itemW, itemH, ClickGuiLayout.scaled(6f),
                        new Color(255, 255, 255, (int) (select * alpha * 22f)));
            }

            Color iconColor = select > 0.5f
                    ? ColorUtil.setAlpha(Color.WHITE, full)
                    : ColorUtil.setAlpha(new Color(150, 150, 150), full);
            Color textColor = select > 0.5f
                    ? ColorUtil.setAlpha(Color.WHITE, full)
                    : ColorUtil.setAlpha(new Color(160, 160, 160), full);

            float textSize = ClickGuiLayout.scaled(11f);
            float textY = y + (itemH - textSize) / 2f;
            float iconSize = textSize;
            float iconX = x + ClickGuiLayout.scaled(8f + tab.getIconShiftX());
            float iconY = textY + ClickGuiLayout.scaled(tab.getIconShiftY());
            ClickGuiIcons.drawTabIcon(iconX, iconY, iconSize, tab, iconColor);
            Fonts.SF_MEDIUM.drawText(matrices, tab.getLabel(), x + ClickGuiLayout.scaled(26f), textY, textSize, textColor);
            y += itemH + itemGap;
        }
    }

    public ClickGuiTab clickedTab(double mouseX, double mouseY, float windowX, float windowY) {
        float padX = ClickGuiLayout.scaled(10f);
        float x = windowX + padX;
        float itemW = ClickGuiLayout.sidebarWidth() - padX * 2f;
        float itemH = ClickGuiLayout.scaled(26f);
        float itemGap = ClickGuiLayout.scaled(2f);
        float sectionGap = ClickGuiLayout.scaled(10f);
        float sectionTitleH = ClickGuiLayout.scaled(14f);

        float logo = ClickGuiLayout.scaled(42f);
        float logoY = windowY + ClickGuiLayout.scaled(10f);
        float dividerY = logoY + logo + ClickGuiLayout.scaled(6f);
        float profileY = dividerY + ClickGuiLayout.scaled(10f);
        float avaSize = ClickGuiLayout.scaled(18f);
        float y = profileY + avaSize + ClickGuiLayout.scaled(14f);

        for (ClickGuiTab tab : ClickGuiTab.sidebarTabs()) {
            if (ClickGuiTab.startsSection(tab)) {
                if (tab != ClickGuiTab.COMBAT) y += sectionGap;
                y += sectionTitleH;
            }
            if (MouseUtil.isHovered(mouseX, mouseY, x, y, itemW, itemH)) {
                return tab;
            }
            y += itemH + itemGap;
        }
        return null;
    }

    public boolean clickedSearch(double mouseX, double mouseY, float windowX, float windowY) {
        if (MouseUtil.isHovered(mouseX, mouseY,
                ClickGuiHeader.searchHitX(windowX), ClickGuiHeader.searchHitY(windowY),
                ClickGuiHeader.searchHitW(), ClickGuiHeader.searchHitH())) {
            setSearching(true);
            return true;
        }
        return false;
    }

    public void setSearching(boolean value) {
        this.searching = value;
    }

    public void resetSearchFocus() {
        setSearching(false);
    }

    public boolean keyPressed(int keyCode) {
        if (!searching) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search.deleteCharAt(search.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            setSearching(false);
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (!searching) return false;
        if (search.length() < 16 && !Character.isISOControl(chr)) {
            search.append(chr);
            return true;
        }
        return false;
    }

    private static void drawLogoGlow(float x, float y, float size, float alpha) {
        float glowSize = size * 1.55f;
        RenderUtil.TEXTURE_RECT.draw(RenderUtil.matrices(), x + (size - glowSize) / 2f,
                y + (size - glowSize) / 2f, glowSize, glowSize, 0f,
                ColorUtil.setAlpha(UIColors.primary(), (int) (70f * alpha)),
                0f, 0f, 1f, 1f, FileUtil.getImage("particles/glow"));
    }
}
