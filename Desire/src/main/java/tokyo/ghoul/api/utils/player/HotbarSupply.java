package tokyo.ghoul.api.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public final class HotbarSupply {
    private HotbarSupply() {
    }

    public static int ensure(Item item, boolean fromInventory, int targetSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return -1;

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        if (!fromInventory) return -1;
        if (mc.player.containerMenu != mc.player.inventoryMenu) return -1;

        int invSlot = InventoryUtil.searchItem(item, 9, 36);
        if (invSlot == -1) return -1;

        int slot = Math.clamp(targetSlot, 0, 8);
        SwapManager.getInstance().grimSwap(0, invSlot, slot, ContainerInput.SWAP);
        return mc.player.getInventory().getItem(slot).is(item) ? slot : -1;
    }

    public static int ensure(Item item, boolean fromInventory) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return -1;
        return ensure(item, fromInventory, pickTargetSlot(mc));
    }

    private static int pickTargetSlot(Minecraft mc) {
        int selected = mc.player.getInventory().getSelectedSlot();
        int fallback = -1;
        for (int i = 0; i < 9; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.isEmpty()) return i;
            if (stack.is(Items.END_CRYSTAL) || stack.is(Items.OBSIDIAN)) continue;
            if (i != selected) return i;
            fallback = i;
        }
        return fallback != -1 ? fallback : selected;
    }

    public static boolean has(Item item, boolean fromInventory) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return InventoryUtil.searchItem(item, 0, fromInventory ? 36 : 9) != -1;
    }
}
