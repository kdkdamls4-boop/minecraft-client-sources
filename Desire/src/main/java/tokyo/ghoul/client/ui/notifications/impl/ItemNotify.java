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

/** Уведомление модуля с иконкой предмета: "<модуль> <сообщение>". */
public class ItemNotify extends Notify implements QuickImports {
    private static final int MAX_MESSAGE_LENGTH = 40;

    private final String title;
    private final String message;
    private final ItemStack stack;

    public ItemNotify(String title, ItemStack stack, String message) {
        this.title = title + " ";
        this.message = message.length() > MAX_MESSAGE_LENGTH ? message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…" : message;
        this.stack = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void render(PoseStack ms) {
        float anim = (float) getAnimation().getValue();
        if (anim <= 0.01f) return;

        float titleW = NotificationStyle.textWidth(title);
        float messageW = NotificationStyle.textWidth(message);
        float width = NotificationStyle.width(titleW + messageW);
        float height = NotificationStyle.height();
        float x = getX() - width / 2f;
        float y = getY();

        NotificationStyle.drawBackground(ms, x, y, width, height, anim);

        float item = NotificationStyle.iconSize();
        float itemX = NotificationStyle.iconCenterX(x) - item / 2f;
        float itemY = y + (height - item) / 2f;
        Identifier textureId = null;
        if (!stack.isEmpty()) {
            var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            textureId = Identifier.fromNamespaceAndPath(key.getNamespace(), "textures/item/" + key.getPath() + ".png");
        }
        if (textureId != null && mc.getResourceManager().getResource(textureId).isPresent()) {
            RenderUtil.TEXTURE_RECT.draw(ms, itemX, itemY, item, item, 0f,
                    RelicStyle.a(Color.WHITE, anim), 0f, 0f, 1f, 1f, textureId);
        } else {
            RenderUtil.RECT.draw(ms, itemX, itemY, item, item, 2f,
                    RelicStyle.a(new Color(50, 55, 65, 220), anim));
        }

        float textX = NotificationStyle.textX(x);
        NotificationStyle.drawText(ms, title, textX, y, RelicStyle.ACCENT, anim);
        NotificationStyle.drawText(ms, message, textX + titleW, y, RelicStyle.TEXT, anim);
    }
}
