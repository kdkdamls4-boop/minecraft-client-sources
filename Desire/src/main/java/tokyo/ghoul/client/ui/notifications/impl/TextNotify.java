package tokyo.ghoul.client.ui.notifications.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import tokyo.ghoul.client.ui.notifications.NotificationStyle;
import tokyo.ghoul.client.ui.notifications.Notify;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.RelicStyle;

public class TextNotify extends Notify {
    private final String message;

    public TextNotify(String message) {
        this.message = message;
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        float width = NotificationStyle.width(NotificationStyle.textWidth(message));
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);
        NotificationStyle.drawIcon(ms, HudIcons.WM_BELL, x, y, NotificationStyle.u(29f), NotificationStyle.u(30f), RelicStyle.ACCENT, anim);
        NotificationStyle.drawText(ms, message, NotificationStyle.textX(x), y, RelicStyle.TEXT, anim);
    }
}
