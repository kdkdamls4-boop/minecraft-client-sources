package tokyo.ghoul.client.features.modules.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import tokyo.ghoul.api.system.configs.FriendManager;

public final class AutoCape {
    private AutoCape() {
    }

    public static boolean shouldApply(PlayerInfo info) {
        if (info == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;

        PlayerInfo self = mc.player.getPlayerInfo();
        if (self != null && self.equals(info)) return true;

        String name = info.getProfile().name();
        return name != null && FriendManager.getInstance().contains(name);
    }

    public static boolean hidesCapeFor(LivingEntity entity) {
        return entity != null && hidesCapeForChest(entity.getItemBySlot(EquipmentSlot.CHEST), entity.isFallFlying());
    }

    public static boolean hidesCapeForChest(ItemStack chest, boolean flying) {
        if (flying) return true;
        if (chest == null || chest.isEmpty()) return false;
        if (chest.is(Items.ELYTRA) || chest.has(DataComponents.GLIDER)) return true;
        Identifier itemId = BuiltInRegistries.ITEM.getKey(chest.getItem());
        if (itemId != null && itemId.getPath().contains("elytra")) return true;
        Equippable equippable = chest.get(DataComponents.EQUIPPABLE);
        if (equippable == null) return false;
        return equippable.assetId()
                .map(key -> key.identifier().getPath().contains("elytra"))
                .orElse(Boolean.FALSE);
    }

    public static PlayerSkin withCape(PlayerSkin original) {
        return original;
    }
}
