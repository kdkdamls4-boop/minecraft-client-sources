package tokyo.ghoul.client.ui.notifications.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.ui.notifications.NotificationStyle;
import tokyo.ghoul.client.ui.notifications.Notify;
import tokyo.ghoul.client.ui.widget.RelicStyle;

import java.awt.Color;

public class TotemNotify extends Notify {
    private static final Identifier TOTEM_TEXTURE = Identifier.fromNamespaceAndPath("minecraft", "textures/item/totem_of_undying.png");

    private final String name;
    private final boolean enchanted;

    public TotemNotify(String name, boolean enchanted) {
        this.name = name;
        this.enchanted = enchanted;
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        String suffix = " потерял тотем, зачарован:";
        float nameW = NotificationStyle.textWidth(name);
        float suffixW = NotificationStyle.textWidth(suffix);
        float dot = NotificationStyle.u(22f), dotGap = NotificationStyle.u(14f);

        float width = NotificationStyle.width(nameW + suffixW + dotGap + dot);
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);

        float item = NotificationStyle.iconSize();
        RenderUtil.TEXTURE_RECT.draw(ms, NotificationStyle.iconCenterX(x) - item / 2f, y + (height - item) / 2f,
                item, item, 0f, RelicStyle.a(Color.WHITE, anim), 0f, 0f, 1f, 1f, TOTEM_TEXTURE);

        float textX = NotificationStyle.textX(x);
        NotificationStyle.drawText(ms, name, textX, y, RelicStyle.ACCENT, anim);
        NotificationStyle.drawText(ms, suffix, textX + nameW, y, RelicStyle.TEXT, anim);

        Color dotColor = enchanted ? new Color(0, 255, 0) : new Color(255, 0, 0);
        RenderUtil.RECT.draw(ms, textX + nameW + suffixW + dotGap, y + (height - dot) / 2f, dot, dot, dot / 2f,
                RelicStyle.a(dotColor, anim));
    }
}
