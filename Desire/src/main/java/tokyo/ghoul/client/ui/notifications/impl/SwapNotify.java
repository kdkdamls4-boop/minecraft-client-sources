package tokyo.ghoul.client.ui.notifications.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.ui.notifications.NotificationStyle;
import tokyo.ghoul.client.ui.notifications.Notify;
import tokyo.ghoul.client.ui.widget.RelicStyle;

import java.awt.Color;

public class SwapNotify extends Notify implements QuickImports {
    private static final int MAX_NAME_LENGTH = 24;

    private final String fromName;
    private final String toName;
    private final ItemStack toStack;

    public SwapNotify(ItemStack from, ItemStack to) {
        this.fromName = from == null || from.isEmpty() ? null : shorten(from.getHoverName().getString());
        this.toName = shorten(to.getHoverName().getString());
        this.toStack = to.copy();
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        String prefix = fromName != null ? "Свапнул " : "Взял ";
        String middle = fromName != null ? " на " : "";
        String from = fromName != null ? fromName : "";

        float prefixW = NotificationStyle.textWidth(prefix);
        float fromW = NotificationStyle.textWidth(from);
        float middleW = NotificationStyle.textWidth(middle);
        float toW = NotificationStyle.textWidth(toName);
        float width = NotificationStyle.width(prefixW + fromW + middleW + toW);
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);

        float item = NotificationStyle.iconSize();
        float itemX = NotificationStyle.iconCenterX(x) - item / 2f;
        float itemY = y + (height - item) / 2f;
        var key = BuiltInRegistries.ITEM.getKey(toStack.getItem());
        Identifier textureId = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/item/" + key.getPath() + ".png");
        if (mc.getResourceManager().getResource(textureId).isPresent()) {
            RenderUtil.TEXTURE_RECT.draw(ms, itemX, itemY, item, item, 0f,
                    RelicStyle.a(Color.WHITE, anim), 0f, 0f, 1f, 1f, textureId);
        } else {
            RenderUtil.RECT.draw(ms, itemX, itemY, item, item, 2f,
                    RelicStyle.a(new Color(50, 55, 65, 220), anim));
        }

        float textX = NotificationStyle.textX(x);
        NotificationStyle.drawText(ms, prefix, textX, y, RelicStyle.TEXT, anim);
        textX += prefixW;
        if (fromName != null) {
            NotificationStyle.drawText(ms, from, textX, y, RelicStyle.ACCENT, anim);
            textX += fromW;
            NotificationStyle.drawText(ms, middle, textX, y, RelicStyle.TEXT, anim);
            textX += middleW;
        }
        NotificationStyle.drawText(ms, toName, textX, y, RelicStyle.ACCENT, anim);
    }

    private static String shorten(String name) {
        return name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH - 1) + "…" : name;
    }
}
