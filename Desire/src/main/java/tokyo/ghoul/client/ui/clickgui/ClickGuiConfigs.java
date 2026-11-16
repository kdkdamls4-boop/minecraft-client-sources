package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.configs.ConfigManager;
import tokyo.ghoul.api.system.DiscordRpc;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class ClickGuiConfigs {
    private static final DateTimeFormatter CONFIG_TIME = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm").withZone(ZoneId.systemDefault());
    private final StringBuilder name = new StringBuilder();
    private boolean typing;
    private float scroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private List<String> configs = new ArrayList<>();

    public void refresh() {
        configs = new ArrayList<>(ConfigManager.getInstance().getConfigsNames());
        configs.removeIf(config -> config.equalsIgnoreCase("autoConfig"));
        configs.sort(String.CASE_INSENSITIVE_ORDER);
    }

    public void close() {
        typing = false;
        name.setLength(0);
    }

    public boolean isTyping() {
        return typing;
    }

    public void render(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        var matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);
        float x = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(20f);
        float y = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f);
        float width = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(40f);

        float fieldH = ClickGuiLayout.scaled(26f);
        float saveW = ClickGuiLayout.scaled(66f);
        float createW = ClickGuiLayout.scaled(66f);
        float fieldW = width - saveW - createW - ClickGuiLayout.scaled(14f);
        float fieldTextSize = ClickGuiLayout.scaled(11f);
        float fieldTextY = y + (fieldH - fieldTextSize) / 2f;

        RenderUtil.RECT.draw(matrices, x, y, fieldW, fieldH, ClickGuiLayout.scaled(3f), new Color(1, 3, 2, (int) (30f * alpha)));
        String placeholder = typing || !name.isEmpty() ? name.toString() : "Сначала новые";
        Color fieldColor = name.isEmpty() && !typing
                ? UIColors.inactiveTextColor((int) (0.55f * full))
                : UIColors.textColor(full);
        Fonts.PS_MEDIUM.drawText(matrices, placeholder, x + ClickGuiLayout.scaled(8f), fieldTextY, fieldTextSize, fieldColor);

        if (typing) {
            float cursor = (float) ((Math.sin(System.currentTimeMillis() / 320.0) + 1.0) / 2.0);
            float textW = Fonts.PS_MEDIUM.getWidth(placeholder, fieldTextSize);
            RenderUtil.RECT.draw(matrices,
                    x + ClickGuiLayout.scaled(8.5f) + textW,
                    fieldTextY + ClickGuiLayout.scaled(1f),
                    ClickGuiLayout.scaled(0.5f), ClickGuiLayout.scaled(7f), 0f,
                    ColorUtil.setAlpha(UIColors.textColor(), (int) (cursor * alpha * 255f)));
        }

        float saveX = x + width - saveW - createW - ClickGuiLayout.scaled(6f);
        boolean saveHover = MouseUtil.isHovered(mouseX, mouseY, saveX, y, saveW, fieldH);
        RenderUtil.RECT.draw(matrices, saveX, y, saveW, fieldH, ClickGuiLayout.scaled(3f), new Color(1, 3, 2, (int) (30f * alpha)));
        if (saveHover) {
            RenderUtil.RECT.draw(matrices, saveX, y, saveW, fieldH, ClickGuiLayout.scaled(4f),
                    ColorUtil.setAlpha(new Color(255, 255, 255), (int) (26f * alpha)));
        }
        float saveTextSize = ClickGuiLayout.scaled(11f);
        float saveTextW = Fonts.PS_BOLD.getWidth("Сохранить", saveTextSize);
        Fonts.PS_BOLD.drawText(matrices, "Сохранить", saveX + (saveW - saveTextW) / 2f,
                y + (fieldH - saveTextSize) / 2f, saveTextSize, ColorUtil.setAlpha(Color.WHITE, full));

        float createX = x + width - createW;
        RenderUtil.RECT.draw(matrices, createX, y, createW, fieldH, ClickGuiLayout.scaled(3f),
                ColorUtil.setAlpha(UIColors.primary(), (int) (115f * alpha)));
        float createTextW = Fonts.PS_BOLD.getWidth("Создать", saveTextSize);
        Fonts.PS_BOLD.drawText(matrices, "Создать", createX + (createW - createTextW) / 2f,
                y + (fieldH - saveTextSize) / 2f, saveTextSize, ColorUtil.setAlpha(Color.WHITE, full));

        float rowH = ClickGuiLayout.scaled(44f);
        float radius = ClickGuiLayout.scaled(3f);
        float listY = y + fieldH + ClickGuiLayout.scaled(10f);
        float listH = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.contentHeight() - listY - ClickGuiLayout.scaled(14f);
        scrollAnimation.update();
        scrollAnimation.run(scroll, 220, Easing.EXPO_OUT);

        ScissorUtil.start(matrices, x, listY, width, listH);
        float offset = (float) scrollAnimation.getValue();
        float row = listY + offset;
        for (String config : configs) {
            RenderUtil.RECT.draw(matrices, x, row, width, rowH, radius, new Color(1, 3, 2, (int) (34f * alpha)));

            float textSize = ClickGuiLayout.scaled(12f);
            Fonts.PS_MEDIUM.drawText(matrices, config, x + ClickGuiLayout.scaled(8f), row + ClickGuiLayout.scaled(6f),
                    textSize, UIColors.textColor(full));
            float metaSize = ClickGuiLayout.scaled(8.5f);
            String modified = CONFIG_TIME.format(Instant.ofEpochMilli(ConfigManager.getInstance().getLastModified(config)));
            String meta = "Изменён: " + modified + "   Автор: " + DiscordRpc.getDiscordUsername();
            Fonts.PS_REGULAR.drawText(matrices, meta, x + ClickGuiLayout.scaled(8f),
                    row + ClickGuiLayout.scaled(22f), metaSize, UIColors.inactiveTextColor((int) (0.8f * full)));

            float iconSize = ClickGuiLayout.scaled(12f);
            float delX = x + width - iconSize - ClickGuiLayout.scaled(8f);
            float loadX = delX - ClickGuiLayout.scaled(42f);
            Fonts.PS_MEDIUM.drawText(matrices, "Загрузить", loadX - ClickGuiLayout.scaled(28f), row + ClickGuiLayout.scaled(16f),
                    ClickGuiLayout.scaled(10f), UIColors.textColor(full));
            Fonts.PS_MEDIUM.drawText(matrices, "•••", delX - ClickGuiLayout.scaled(18f), row + ClickGuiLayout.scaled(14f),
                    ClickGuiLayout.scaled(12f), UIColors.inactiveTextColor(full));
            ClickGuiIcons.draw(delX, row + (rowH - iconSize) / 2f, iconSize, ClickGuiIcons.DELETE, UIColors.negativeColor(full));
            row += rowH + ClickGuiLayout.rowGap();
        }
        ScissorUtil.stop(matrices);

        float content = configs.size() * (rowH + ClickGuiLayout.rowGap());
        scroll = Mth.clamp(scroll, Math.min(listH - content, 0f), 0f);
    }

    public boolean mouseClicked(double mouseX, double mouseY, float windowX, float windowY) {
        float x = ClickGuiLayout.contentX(windowX) + ClickGuiLayout.scaled(20f);
        float y = ClickGuiLayout.contentY(windowY) + ClickGuiLayout.scaled(10f);
        float width = ClickGuiLayout.contentWidth() - ClickGuiLayout.scaled(40f);
        float fieldH = ClickGuiLayout.scaled(26f);
        float saveW = ClickGuiLayout.scaled(66f);
        float createW = ClickGuiLayout.scaled(66f);
        float fieldW = width - saveW - createW - ClickGuiLayout.scaled(14f);

        if (MouseUtil.isHovered(mouseX, mouseY, x, y, fieldW, fieldH)) {
            typing = true;
            return true;
        }

        float saveX = x + width - saveW - createW - ClickGuiLayout.scaled(6f);
        if (MouseUtil.isHovered(mouseX, mouseY, saveX, y, saveW, fieldH) && !name.isEmpty()) {
            ConfigManager.getInstance().save(name.toString());
            name.setLength(0);
            typing = false;
            refresh();
            return true;
        }

        float createX = x + width - createW;
        if (MouseUtil.isHovered(mouseX, mouseY, createX, y, createW, fieldH)) {
            typing = true;
            return true;
        }

        float rowH = ClickGuiLayout.scaled(44f);
        float iconSize = ClickGuiLayout.scaled(12f);
        float listY = y + fieldH + ClickGuiLayout.scaled(10f);
        float row = listY + (float) scrollAnimation.getValue();
        for (String config : configs) {
            if (MouseUtil.isHovered(mouseX, mouseY, x, row, width, rowH)) {
                float delX = x + width - iconSize - ClickGuiLayout.scaled(8f);
                float loadX = delX - ClickGuiLayout.scaled(42f);
                if (MouseUtil.isHovered(mouseX, mouseY, loadX - ClickGuiLayout.scaled(32f), row, ClickGuiLayout.scaled(52f), rowH)) {
                    ConfigManager.getInstance().load(config);
                    return true;
                }
                if (MouseUtil.isHovered(mouseX, mouseY, delX - ClickGuiLayout.scaled(4f), row, iconSize + ClickGuiLayout.scaled(8f), rowH)) {
                    ConfigManager.getInstance().remove(config);
                    refresh();
                    return true;
                }
            }
            row += rowH + ClickGuiLayout.rowGap();
        }
        typing = false;
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        if (!MouseUtil.isHovered(mouseX, mouseY, ClickGuiLayout.contentX(windowX), ClickGuiLayout.contentY(windowY),
                ClickGuiLayout.contentWidth(), ClickGuiLayout.contentHeight())) {
            return false;
        }
        scroll += (float) (amount * ClickGuiLayout.scaled(16f));
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!typing) return false;
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !name.isEmpty()) {
            name.deleteCharAt(name.length() - 1);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER && !name.isEmpty()) {
            ConfigManager.getInstance().save(name.toString());
            name.setLength(0);
            typing = false;
            refresh();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            typing = false;
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr) {
        if (!typing) return false;
        if (name.length() < 24 && !Character.isISOControl(chr)) {
            name.append(chr);
            return true;
        }
        return false;
    }
}
