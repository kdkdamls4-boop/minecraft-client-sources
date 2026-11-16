package tokyo.ghoul.client.ui.notifications.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import tokyo.ghoul.client.ui.notifications.NotificationStyle;
import tokyo.ghoul.client.ui.notifications.Notify;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.RelicStyle;

public class ModuleNotify extends Notify {
    private final String name;
    private final boolean enabled;

    public ModuleNotify(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        String prefix = "Функция ";
        String suffix = enabled ? " активирована" : " деактивирована";
        float prefixW = NotificationStyle.textWidth(prefix);
        float nameW = NotificationStyle.textWidth(name);
        float suffixW = NotificationStyle.textWidth(suffix);

        float width = NotificationStyle.width(prefixW + nameW + suffixW);
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);

        if (enabled) {
            NotificationStyle.drawIcon(ms, HudIcons.WM_PLUS, x, y, NotificationStyle.u(26f), NotificationStyle.u(26f), RelicStyle.ACCENT, anim);
        } else {
            NotificationStyle.drawIcon(ms, HudIcons.WM_CROSS, x, y, NotificationStyle.u(22f), NotificationStyle.u(22f), RelicStyle.ACCENT, anim);
        }

        float textX = NotificationStyle.textX(x);
        NotificationStyle.drawText(ms, prefix, textX, y, RelicStyle.TEXT, anim);
        NotificationStyle.drawText(ms, name, textX + prefixW, y, RelicStyle.ACCENT, anim);
        NotificationStyle.drawText(ms, suffix, textX + prefixW + nameW, y, RelicStyle.TEXT, anim);
    }
}
