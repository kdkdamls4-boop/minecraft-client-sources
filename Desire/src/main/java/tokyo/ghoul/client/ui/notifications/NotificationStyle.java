package tokyo.ghoul.client.ui.notifications;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.RelicStyle;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

import java.awt.Color;

public final class NotificationStyle {
    private NotificationStyle() {
    }

    public static float u(float px) {
        return RelicStyle.u(px * 1.15f);
    }

    public static float height() {
        return u(76f);
    }

    public static float pitch() {
        return u(112f);
    }

    public static float textSize() {
        return u(39f);
    }

    public static float iconSize() {
        return u(40f);
    }

    public static float textWidth(String text) {
        return RelicStyle.width(Fonts.HUD_MEDIUM, text, textSize());
    }

    public static float width(float textWidth) {
        return u(105f) + textWidth + u(33f);
    }

    public static float textX(float x) {
        return x + u(105f);
    }

    public static void drawBackground(PoseStack ms, float x, float y, float width, float height, float alpha) {
        if (alpha <= 0.001f) return;

        boolean glass = NotifyWidget.getInstance().isLiquidGlass();
        RelicStyle.panel(ms, x, y, width, height, height * 0.36f, u(4f), RelicStyle.BORDER, RelicStyle.FILL, alpha, glass);
        RelicStyle.separator(ms, x + u(75f), y + height / 2f, u(12f), u(26f), alpha);
    }

    public static void drawIcon(PoseStack ms, Identifier texture, float x, float y, float w, float h, Color color, float alpha) {
        RelicStyle.icon(ms, texture, iconCenterX(x) - w / 2f, y + height() / 2f, w, h, color, alpha);
    }

    public static float iconCenterX(float x) {
        return x + u(38f);
    }

    public static void drawText(PoseStack ms, String text, float textX, float y, Color color, float alpha) {
        RelicStyle.text(ms, Fonts.HUD_MEDIUM, text, textX, y + height() / 2f, textSize(), color, alpha);
    }
}
