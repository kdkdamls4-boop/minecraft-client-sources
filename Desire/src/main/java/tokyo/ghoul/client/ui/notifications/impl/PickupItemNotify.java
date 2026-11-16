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

public class PickupItemNotify extends Notify implements QuickImports {
    private final ItemStack itemStack;

    public PickupItemNotify(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        String name = itemStack.getHoverName().getString();
        String prefix = "Вы подобрали ";
        float prefixW = NotificationStyle.textWidth(prefix);
        float nameW = NotificationStyle.textWidth(name);
        float width = NotificationStyle.width(prefixW + nameW);
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);

        float item = NotificationStyle.iconSize();
        float itemX = NotificationStyle.iconCenterX(x) - item / 2f;
        float itemY = y + (height - item) / 2f;
        var key = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
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
        NotificationStyle.drawText(ms, name, textX + prefixW, y, RelicStyle.ACCENT, anim);
    }
}
