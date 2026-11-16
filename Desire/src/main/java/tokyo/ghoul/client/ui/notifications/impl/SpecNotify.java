package tokyo.ghoul.client.ui.notifications.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import tokyo.ghoul.client.ui.notifications.NotificationStyle;
import tokyo.ghoul.client.ui.notifications.Notify;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.RelicStyle;

import java.awt.Color;

public class SpecNotify extends Notify {
    private final String name;

    public SpecNotify(String name) {
        this.name = name;
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        String suffix = " просит о наблюдении";
        float nameW = NotificationStyle.textWidth(name);
        float width = NotificationStyle.width(nameW + NotificationStyle.textWidth(suffix));
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);
        NotificationStyle.drawIcon(ms, HudIcons.TRIANGLE, x, y, NotificationStyle.iconSize(), NotificationStyle.iconSize(),
                new Color(255, 227, 45), anim);

        float textX = NotificationStyle.textX(x);
        NotificationStyle.drawText(ms, name, textX, y, RelicStyle.ACCENT, anim);
        NotificationStyle.drawText(ms, suffix, textX + nameW, y, RelicStyle.TEXT, anim);
    }
}
