package tokyo.ghoul.client.ui.notifications.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.ui.notifications.NotificationStyle;
import tokyo.ghoul.client.ui.notifications.Notify;
import tokyo.ghoul.client.ui.widget.RelicStyle;

import java.awt.Color;

public class EquipmentNotify extends Notify {
    public enum Type {
        HEAD, CHEST, LEGGINGS, BOOTS
    }

    private final Type type;
    private final ItemStack stack;

    public EquipmentNotify(Type type, ItemStack stack) {
        this.type = type;
        this.stack = stack;
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        String armorElement = switch (type) {
            case HEAD -> "Вашей шляпе скоро пизда";
            case CHEST -> "Вашему грудаку скоро пизда";
            case LEGGINGS -> "Вашим поножам скоро пизда";
            case BOOTS -> "Вашим ботинкам скоро пизда";
        };

        float width = NotificationStyle.width(NotificationStyle.textWidth(armorElement));
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);

        float item = NotificationStyle.iconSize();
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier textureId = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/item/" + key.getPath() + ".png");
        RenderUtil.TEXTURE_RECT.draw(ms, NotificationStyle.iconCenterX(x) - item / 2f, y + (height - item) / 2f,
                item, item, 0f, RelicStyle.a(Color.WHITE, anim), 0f, 0f, 1f, 1f, textureId);

        NotificationStyle.drawText(ms, armorElement, NotificationStyle.textX(x), y, RelicStyle.TEXT, anim);
    }
}
