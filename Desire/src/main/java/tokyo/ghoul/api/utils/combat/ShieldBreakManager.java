package tokyo.ghoul.api.utils.combat;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.player.InventoryUtil;
import tokyo.ghoul.api.utils.other.SlownessManager;

public class ShieldBreakManager implements QuickImports {
    public boolean shouldBreakShield(Player entity) {
        if (entity.isBlocking() && isLookingAtMe(entity)) {
            boolean slowness = SlownessManager.isEnabled();
            int invSlot = InventoryUtil.findAxeInInventory(false);
            int hotBarSlot = InventoryUtil.findAxeInInventory(true);

            if (hotBarSlot == -1 && invSlot != -1) {
                if (!slowness) {
                    return shieldBreakAction("Inventory", hotBarSlot, invSlot, entity);
                } else {
                    SlownessManager.applySlowness(10, () -> {
                        shieldBreakAction("Inventory", hotBarSlot, invSlot, entity);
                    });
                    return true;
                }
            }

            if (hotBarSlot != -1) {
                if (!slowness) {
                    shieldBreakAction("Hotbar", hotBarSlot, invSlot, entity);
                } else {
                    SlownessManager.applySlowness(150, () -> {
                        shieldBreakAction("Hotbar", hotBarSlot, invSlot, entity);
                    });
                    return true;
                }
            }
        }

        return false;
    }

    private boolean shieldBreakAction(String action, int hotBarSlot, int invSlot, Player entity) {
        int prevSlot = mc.player.getInventory().getSelectedSlot();

        switch (action) {
            case "Hotbar" -> {
                InventoryUtil.swapToSlot(hotBarSlot);
                mc.gameMode.attack(mc.player, entity);
                mc.player.swing(InteractionHand.MAIN_HAND);
                InventoryUtil.swapToSlot(prevSlot);
                return true;
            }

            case "Inventory" -> {
                int bestSlot = InventoryUtil.findBestSlotInHotBar();
                InventoryUtil.swapSlots(invSlot, bestSlot);
                InventoryUtil.swapToSlot(bestSlot);

                mc.gameMode.attack(mc.player, entity);
                mc.player.swing(InteractionHand.MAIN_HAND);

                InventoryUtil.swapSlots(invSlot, bestSlot);
                InventoryUtil.swapToSlot(prevSlot);
                return true;
            }
        }

        return false;
    }

    private boolean isLookingAtMe(Player target) {
        if (mc.player == null) return false;

        Vec3 lookVec = target.getViewVector(1.0F).normalize();
        Vec3 dirToMe = mc.player.getEyePosition().subtract(target.getEyePosition()).normalize();

        double dot = lookVec.dot(dirToMe);
        double angle = Math.toDegrees(Math.acos(dot));

        return angle < 100.0;
    }
}