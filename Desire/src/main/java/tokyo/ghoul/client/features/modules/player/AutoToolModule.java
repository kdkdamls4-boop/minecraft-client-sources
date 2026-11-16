package tokyo.ghoul.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.utils.inventory.InventoryActionManager;
import tokyo.ghoul.api.utils.inventory.InventoryActions;
import tokyo.ghoul.client.features.modules.combat.CrystalAuraModule;
import tokyo.ghoul.inject.accessor.MultiPlayerGameModeAccessor;

@ModuleRegister(name = "Auto Tool", description = "Автоматический выбор инструмента", category = Category.PLAYER)
public class AutoToolModule extends Module {
    @Getter private static final AutoToolModule instance = new AutoToolModule();

    private int previousSlot = -1;
    private int swappedInventorySlot = -1;

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(tick);
    }

    @Override
    public void onEnable() {
        if (mc.player != null && mc.gameMode != null) {
            restoreSlot(mc.player);
        }
        super.onEnable();
    }

    private void onTick() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;

        if (player.containerMenu != player.inventoryMenu) {
            restoreSlot(player);
            return;
        }

        BlockPos pos = breakingPos();
        if (pos == null) {
            if (!mc.options.keyAttack.isDown()) {
                restoreSlot(player);
                return;
            }
            if (!(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
            pos = hit.getBlockPos();
        }

        BlockState state = mc.level.getBlockState(pos);
        int best = -1;
        float bestSpeed = 1.0F;
        for (int i = 0; i < 36; i++) {
            float speed = getBreakSpeed(player.getInventory().getItem(i), state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = i;
            }
        }
        if (best == -1) return;

        int selected = player.getInventory().getSelectedSlot();
        if (best == selected) return;

        if (best > 8) {
            if (swappedInventorySlot == best) return;

            if (previousSlot != -1) {
                selectSlot(player, previousSlot);
                previousSlot = -1;
            }
            if (swappedInventorySlot != -1) {
                swapBackInventorySlot();
            }
            swapToMainHand(best);
            swappedInventorySlot = best;
            return;
        }

        if (swappedInventorySlot != -1) {
            swapBackInventorySlot();
        }
        if (previousSlot == -1) {
            previousSlot = selected;
        }
        selectSlot(player, best);
    }

    // слот, который ломает CrystalAura (аналог Nuker в исходнике)
    private static BlockPos breakingPos() {
        return CrystalAuraModule.getInstance().getMiningPos();
    }

    public float getBestBreakSpeed(BlockState state) {
        if (mc.player == null) return 1.0F;

        float best = 1.0F;
        for (int i = 0; i < 36; i++) {
            best = Math.max(best, getBreakSpeed(mc.player.getInventory().getItem(i), state));
        }
        return best;
    }

    private float getBreakSpeed(ItemStack stack, BlockState state) {
        if (stack.isEmpty()) return 0.0F;

        float speed = stack.getDestroySpeed(state);
        boolean leaves = state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES);
        if (leaves && (stack.is(Items.NETHERITE_SWORD) || stack.is(Items.DIAMOND_SWORD) || stack.is(Items.GOLDEN_SWORD)
                || stack.is(Items.IRON_SWORD) || stack.is(Items.STONE_SWORD) || stack.is(Items.WOODEN_SWORD))) {
            return Math.max(speed, 15.0F);
        }
        return speed;
    }

    private void restoreSlot(LocalPlayer player) {
        if (previousSlot != -1) {
            selectSlot(player, previousSlot);
        }
        previousSlot = -1;
        if (swappedInventorySlot != -1) {
            swapBackInventorySlot();
        }
    }

    private void swapBackInventorySlot() {
        swapToMainHand(swappedInventorySlot);
        swappedInventorySlot = -1;
    }

    // слоты 9..35 инвентаря совпадают с индексами слотов меню игрока
    private static void swapToMainHand(int slot) {
        InventoryActionManager.INSTANCE.enqueue(() -> InventoryActions.swapToHand(slot, InteractionHand.MAIN_HAND));
    }

    private static void selectSlot(LocalPlayer player, int slot) {
        player.getInventory().setSelectedSlot(slot);
        ((MultiPlayerGameModeAccessor) mc.gameMode).ghoul$ensureHasSentCarriedItem();
    }
}
