package tokyo.ghoul.api.utils.player;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.*;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.other.NetworkUtil;
import tokyo.ghoul.api.utils.other.SlownessManager;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.SmoothRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.features.modules.movement.InventoryMoveModule;
import tokyo.ghoul.api.utils.timer.TimerUpdater;

@UtilityClass
public class InventoryUtil implements QuickImports {
    private int swappedTo = -1;
    public void dropSlot(int slot) {
        if (mc.player == null || mc.gameMode == null) return;

        int syncId = mc.player.containerMenu.containerId;
        mc.gameMode.handleContainerInput(syncId, slot, 1, ContainerInput.THROW, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic())
            NetworkUtil.sendPacket(new ServerboundContainerClosePacket(syncId));
    }

    public void swapToOffhand(int slot) {
        if (slot == -1) return;
        if (mc.player == null || mc.gameMode == null) return;

        int syncId = mc.player.containerMenu.containerId;
        mc.gameMode.handleContainerInput(syncId, slot, 40, ContainerInput.SWAP, mc.player);
        mc.player.connection.send(new ServerboundContainerClosePacket(syncId));
    }

    public void swapWithBypassGrim(Runnable runnable) {
        if (mc.player == null || mc.player.connection == null) return;

        mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(
                false, false, false, false, false, false, false)));
        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            mc.player.connection.send(new ServerboundPlayerCommandPacket(
                    mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        }
        runnable.run();
        NetworkUtil.sendSilentPacket(new ServerboundContainerClosePacket(0));
        mc.player.connection.send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));
    }

    public void swapWithBypassPolar(Runnable runnable) {
        SlownessManager.applySlowness(100L, () -> {
            if (mc.player != null && mc.player.isUsingItem()) mc.player.releaseUsingItem();
            runnable.run();
            NetworkUtil.sendSilentPacket(new ServerboundContainerClosePacket(0));
        });
    }


    public int searchItem(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    public int searchItem(Item item, int start, int end) {
        if (mc.player == null) return -1;
        int bound = Math.min(end, 41);
        for (int i = start; i < bound; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    public int searchItemStack(java.util.function.Predicate<ItemStack> predicate) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 41; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && predicate.test(stack)) return i;
        }
        return -1;
    }

    public int findBestElytraSlot() {
        int bestSlot = -1;
        double bestScore = -1.0;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            if (!stack.is(Items.ELYTRA)) continue;

            int protLevel = enchantLevel(stack, Enchantments.PROTECTION);
            int unbLevel = enchantLevel(stack, Enchantments.UNBREAKING);
            int mendLevel = enchantLevel(stack, Enchantments.MENDING);

            int maxDurability = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            double durabilityRatio = maxDurability > 0 ? (maxDurability - currentDamage) / (double) maxDurability : 1.0;

            double score = protLevel * 100 + unbLevel * 10 + (mendLevel > 0 ? 1 : 0) + durabilityRatio * 10;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    public int findBestChestplateSlot() {
        int bestSlot = -1;
        double bestScore = -1.0;

        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            int priority = getChestplatePriority(stack.getItem());
            if (priority == 0) continue;

            int protLevel = enchantLevel(stack, Enchantments.PROTECTION);
            int unbLevel = enchantLevel(stack, Enchantments.UNBREAKING);
            int mendLevel = enchantLevel(stack, Enchantments.MENDING);

            int maxDurability = stack.getMaxDamage();
            int currentDamage = stack.getDamageValue();
            double durabilityRatio = maxDurability > 0 ? (maxDurability - currentDamage) / (double) maxDurability : 1.0;

            double score = priority * 10000 + protLevel * 100 + unbLevel * 10 + (mendLevel > 0 ? 1 : 0) + durabilityRatio * 10;

            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static int getChestplatePriority(Item item) {
        if (item == Items.NETHERITE_CHESTPLATE) return 6;
        if (item == Items.DIAMOND_CHESTPLATE) return 5;
        if (item == Items.IRON_CHESTPLATE) return 4;
        if (item == Items.CHAINMAIL_CHESTPLATE) return 3;
        if (item == Items.GOLDEN_CHESTPLATE) return 2;
        if (item == Items.LEATHER_CHESTPLATE) return 1;
        return 0;
    }

    private static int enchantLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (mc.level == null) return 0;
        Holder<Enchantment> holder = mc.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        return enchantments.getLevel(holder);
    }

    public void basicThrow(Item item) {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getCooldowns().isOnCooldown(item.getDefaultInstance())) return;

        for (InteractionHand hand : InteractionHand.values()) {
            if (mc.player.getItemInHand(hand).is(item)) {
                mc.gameMode.useItem(mc.player, hand);
                return;
            }
        }

        int hbSlot = searchItem(item, 0, 9);
        int invSlot = searchItem(item, 9, 36);

        if (hbSlot != -1) {
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            NetworkUtil.sendSilentPacket(new ServerboundSetCarriedItemPacket(hbSlot));
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            NetworkUtil.sendSilentPacket(new ServerboundSetCarriedItemPacket(prevSlot));
            return;
        }

        if (invSlot != -1) {
            swappedTo = mc.player.getInventory().getSelectedSlot() == 8
                    ? mc.player.getInventory().getSelectedSlot() - 1
                    : mc.player.getInventory().getSelectedSlot() + 1;
            int prevSlot = mc.player.getInventory().getSelectedSlot();
            int swapHotbarButton = swappedTo;
            NetworkUtil.sendSilentPacket(new ServerboundSetCarriedItemPacket(swappedTo));
            swapManager.grimSwap(0, invSlot, swapHotbarButton, ContainerInput.SWAP);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            NetworkUtil.sendSilentPacket(new ServerboundSetCarriedItemPacket(prevSlot));
            TimerUpdater.addTimeTask(new TimerUpdater.TimeTask(TimerUpdater.TimeTask.Mode.TICKS, 3, () -> {
                swapManager.grimSwap(0, invSlot, swapHotbarButton, ContainerInput.SWAP);
                swappedTo = -1;
            }, true));
        }
    }

    public void swapAndUseHvH(Item item) {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(item))) return;
        swapAndUseHvH(stack -> stack.is(item));
    }

    public boolean swapAndUseHvH(java.util.function.Predicate<ItemStack> match) {
        if (mc.player == null || mc.gameMode == null) return false;

        int slot = searchStack(match, 9, 41);
        int slotHotbar = searchStack(match, 0, 9);
        int previousSlot = mc.player.getInventory().getSelectedSlot();

        if (match.test(mc.player.getMainHandItem())) {
            if (mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem())) return false;
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            return true;
        }
        if (match.test(mc.player.getOffhandItem())) {
            if (mc.player.getCooldowns().isOnCooldown(mc.player.getOffhandItem())) return false;
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            return true;
        }
        int found = slotHotbar != -1 ? slotHotbar : slot;
        if (found == -1 || mc.player.getCooldowns().isOnCooldown(mc.player.getInventory().getItem(found))) return false;

        if (slotHotbar != -1) {
            setCarriedSlot(slotHotbar);
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            setCarriedSlot(previousSlot);
        }

        if (slotHotbar == -1 && slot != -1) {
            int slotCorrectable = -1;
            for (int slotNone = 0; slotNone < 8; slotNone++) {
                ItemStack stack = mc.player.getInventory().getItem(slotNone);
                if (stack.isEmpty()) slotCorrectable = slotNone;
                if (stack.getUseAnimation() == ItemUseAnimation.NONE) slotCorrectable = slotNone;
            }

            mc.player.setSprinting(false);

            if (slotCorrectable == -1) {
                mc.gameMode.handleContainerInput(0, slot, 8, ContainerInput.SWAP, mc.player);
                mc.player.connection.send(new ServerboundContainerClosePacket(0));
                setCarriedSlot(8);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                setCarriedSlot(previousSlot);
            } else {
                mc.gameMode.handleContainerInput(0, slot, slotCorrectable, ContainerInput.SWAP, mc.player);
                mc.player.connection.send(new ServerboundContainerClosePacket(0));
                setCarriedSlot(slotCorrectable);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                setCarriedSlot(previousSlot);
                mc.gameMode.handleContainerInput(0, slot, slotCorrectable, ContainerInput.SWAP, mc.player);
                mc.player.connection.send(new ServerboundContainerClosePacket(0));
            }
        }
        return true;
    }

    private int searchStack(java.util.function.Predicate<ItemStack> match, int start, int end) {
        if (mc.player == null) return -1;
        int bound = Math.min(end, 41);
        for (int i = start; i < bound; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (!stack.isEmpty() && match.test(stack)) return i;
        }
        return -1;
    }

    public void swapAndUseLegit(Item item) {
        if (mc.player == null || mc.gameMode == null) return;
        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(item))) return;

        int slot = searchItem(item, 9, 41);
        int slotHotbar = searchItem(item, 0, 9);
        int previousSlot = mc.player.getInventory().getSelectedSlot();

        if (mc.player.getMainHandItem().is(item)) {
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            return;
        }
        if (mc.player.getOffhandItem().is(item)) {
            mc.gameMode.useItem(mc.player, InteractionHand.OFF_HAND);
            return;
        }

        if (slotHotbar != -1) {
            setCarriedSlot(slotHotbar);
            NetworkUtil.sendPacket(sequence -> new ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND, sequence, mc.player.getYRot(), mc.player.getXRot()));
            setCarriedSlot(previousSlot);
        }

        if (slotHotbar == -1 && slot != -1) {
            int slotCorrectable = -1;
            for (int slotNone = 0; slotNone < 8; slotNone++) {
                ItemStack stack = mc.player.getInventory().getItem(slotNone);
                if (stack.isEmpty()) slotCorrectable = slotNone;
                if (stack.getUseAnimation() == ItemUseAnimation.NONE) slotCorrectable = slotNone;
            }

            mc.player.setSprinting(false);

            int useSlot = slotCorrectable == -1 ? 8 : slotCorrectable;
            mc.gameMode.handleContainerInput(0, slot, useSlot, ContainerInput.SWAP, mc.player);
            mc.player.connection.send(new ServerboundContainerClosePacket(0));
            setCarriedSlot(useSlot);
            NetworkUtil.sendPacket(sequence -> new ServerboundUseItemPacket(
                    InteractionHand.MAIN_HAND, sequence, mc.player.getYRot(), mc.player.getXRot()));
            setCarriedSlot(previousSlot);
            mc.gameMode.handleContainerInput(0, slot, useSlot, ContainerInput.SWAP, mc.player);
            mc.player.connection.send(new ServerboundContainerClosePacket(0));
        }
    }

    private void setCarriedSlot(int slot) {
        if (mc.player == null) return;
        CarriedItemSync.send(slot);
        mc.player.getInventory().setSelectedSlot(slot);
    }

    public void swapSlots(int from, int to) {
        if (mc.player == null || mc.gameMode == null) return;

        int syncId = mc.player.containerMenu.containerId;
        int swapSlot = (to >= 0 && to <= 8) ? to : (from >= 0 && from <= 8) ? from : -1;
        if (swapSlot == -1) return;

        mc.gameMode.handleContainerInput(syncId, (swapSlot == to) ? from : to, swapSlot, ContainerInput.SWAP, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic()) mc.player.connection.send(new ServerboundContainerClosePacket(syncId));
    }
    public void swapSlotsFull(int from, int to) {
        if (mc.player == null || mc.gameMode == null) return;

        int syncId = mc.player.containerMenu.containerId;

        mc.gameMode.handleContainerInput(syncId, from, to, ContainerInput.SWAP, mc.player);
        if (!InventoryMoveModule.getInstance().isBasic()) mc.player.connection.send(new ServerboundContainerClosePacket(syncId));
    }

    public void useItem(InteractionHand hand) {
        if (mc.player == null) return;

        RotationManager rotationManager = RotationManager.getInstance();

        float currentYaw = rotationManager.getCurrentRotationPlan() != null ? rotationManager.getRotation().getYRot() : mc.player.getYRot();
        float currentPitch = rotationManager.getCurrentRotationPlan() != null ? rotationManager.getRotation().getXRot() : mc.player.getXRot();

        NetworkUtil.sendPacket(s -> new ServerboundUseItemPacket(hand, s, currentYaw, currentPitch));
        mc.player.swing(hand);
    }

    public void swapToSlot(int slot) {
        swapToSlot(slot, true);
    }

    public void swapToSlot(int slot, boolean client) {
        if (mc.player == null) return;

        CarriedItemSync.send(slot);
        if (client) mc.player.getInventory().setSelectedSlot(slot);
    }

    public int findItem(Item item, boolean inHotbar) {
        if (mc.player == null) return -1;

        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();
        int firstSlot = inHotbar ? 0 : 9;
        int lastSlot = inHotbar ? 9 : 36;
        int finalSlot = -1;

        for (int i = firstSlot; i < lastSlot; i++) {
            if (main.get(i).getItem() == item) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    public int findItem(Item input) {
        if (mc.player == null) return -1;

        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();
        int slot = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.getItem() == input) {
                slot = i;
                break;
            }
        }

        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public boolean hasElytraEquipped() {
        return mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
    }

    public int findAxeInInventory(boolean inHotbar) {
        if (mc.player == null) return -1;

        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();
        int firstSlot = inHotbar ? 0 : 9;
        int lastSlot = inHotbar ? 9 : 36;

        for (int i = firstSlot; i < lastSlot; i++) {
            if (main.get(i).getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    public int findBestSlotInHotBar() {
        int emptySlot = findEmptySlot();
        return emptySlot != -1 ? emptySlot : findNonSwordSlot();
    }

    public int findEmptySlot() {
        if (mc.player == null) return -1;

        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();
        int currentItem = mc.player.getInventory().getSelectedSlot();

        for (int i = 0; i < 9; i++) {
            if (main.get(i).isEmpty() && currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    private static int findNonSwordSlot() {
        if (mc.player == null) return -1;

        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();
        int currentItem = mc.player.getInventory().getSelectedSlot();

        for (int i = 0; i < 9; i++) {
            ItemStack stack = main.get(i);
            if (!(stack.is(net.minecraft.tags.ItemTags.SWORDS)) && currentItem != i) {
                return i;
            }
        }
        return -1;
    }

    public int countNonEnchantedTotems() {
        if (mc.player == null) return -1;
        int count = 0;
        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.is(Items.TOTEM_OF_UNDYING) && !stack.isEnchanted()) {
                count++;
            }
        }
        return count;
    }

    public int findBestTotemSlot(boolean saveEnchanted) {
        NonNullList<ItemStack> main = mc.player.getInventory().getNonEquipmentItems();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = main.get(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                if (!saveEnchanted || !stack.isEnchanted()) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    @Getter
    @RequiredArgsConstructor
    public static class ItemUsage {
        private final Item item;
        private final Module provider;
        
        private boolean forceUse = false;
        @Setter private boolean useRotation = true;
        private Rotation customRotation = new Rotation(Float.MIN_VALUE, Float.MIN_VALUE);

        private int targetSlot = -1;
        private int previousSlot = -1;
        private int stateDelay = 0;

        private Runnable pendingAction = null;
        private UsageState usageState = UsageState.IDLE;

        public void onDisable() {
            usageState = UsageState.IDLE;
            stateDelay = 0;
        }

        public void updateCustomRotation(Rotation rotation) {
            customRotation = rotation;
        }

        public void applyRotation() {
            if (!useRotation) return;
            float yaw = customRotation.getYRot() != Float.MIN_VALUE ? customRotation.getYRot() : mc.player.getYRot();
            float pitch = customRotation.getXRot() != Float.MIN_VALUE ? customRotation.getXRot() : mc.player.getXRot();

            RotationStrategy configurable = new RotationStrategy(new SmoothRotation(), AuraModule.isMoveFix());
            RotationManager.getInstance().addRotation(new Rotation(yaw, pitch), configurable, TaskPriority.REQUIRED, provider);
        }

        public void handleUse(boolean isLegit) {
            if (mc.gui.screen() == null && pendingAction != null) {
                pendingAction.run();
            }

            if (mc.player.getCooldowns().isOnCooldown(item.getDefaultInstance())) return;

            if (isLegit) {
                pendingAction = () -> executeLegitMode(-1);
            } else {
                executePacketMode();
            }

            forceUse = false;
        }


        public void handleUse(int bind, boolean isLegit) {
            if (!isLegit) {
                pendingAction = null;
            }

            if (mc.gui.screen() == null && pendingAction != null) {
                pendingAction.run();
            }

            if (!KeyStorage.isPressed(bind) && mc.gui.screen() == null) {
                forceUse = false;
                return;
            }

            if (forceUse || mc.gui.screen() != null || mc.player.getCooldowns().isOnCooldown(item.getDefaultInstance())) return;

            if (isLegit) {
                pendingAction = () -> executeLegitMode(bind);
            } else {
                executePacketMode();
            }
        }

        public void executePacketMode() {
            int invSlot = InventoryUtil.findItem(item, false);
            int hbSlot = InventoryUtil.findItem(item, true);

            if (mc.player.getOffhandItem().is(item)) {
                applyRotation();
                InventoryUtil.useItem(InteractionHand.OFF_HAND);
                forceUse = true;
                return;
            }

            if (mc.player.getMainHandItem().is(item)) {
                applyRotation();
                InventoryUtil.useItem(InteractionHand.MAIN_HAND);
                forceUse = true;
                return;
            }

            int oldSlot = mc.player.getInventory().getSelectedSlot();
            int bestSlot = InventoryUtil.findBestSlotInHotBar();

            if (hbSlot != -1) {
                applyRotation();
                InventoryUtil.swapToSlot(hbSlot);
                InventoryUtil.useItem(InteractionHand.MAIN_HAND);
                InventoryUtil.swapToSlot(oldSlot);
                forceUse = true;
            } else if (invSlot != -1) {
                Runnable runnable = () -> {
                    applyRotation();
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    InventoryUtil.swapToSlot(bestSlot);
                    InventoryUtil.useItem(InteractionHand.MAIN_HAND);
                    InventoryUtil.swapToSlot(oldSlot);
                    InventoryUtil.swapSlots(invSlot, bestSlot);
                    forceUse = true;
                };

                if (SlownessManager.isEnabled()) {
                    SlownessManager.applySlowness(10, runnable);
                } else {
                    runnable.run();
                }
            }
        }

        public void executeLegitMode(int bind) {
            switch (usageState) {
                case IDLE:
                    if (bind != -1 && !KeyStorage.isPressed(bind)) return;

                    targetSlot = InventoryUtil.findItem(item, true);
                    previousSlot = mc.player.getInventory().getSelectedSlot();

                    if (targetSlot == -1) {
                        targetSlot = InventoryUtil.findItem(item, false);
                        if (targetSlot == -1) return;
                        usageState = UsageState.SWAP_ITEM;
                        stateDelay = 1;
                    } else {
                        mc.player.getInventory().setSelectedSlot(targetSlot);
                        usageState = UsageState.USE_ITEM;
                        stateDelay = 1;
                    }
                    break;

                case SWAP_ITEM:
                    if (stateDelay-- > 0) return;

                    int hotbarSlot = InventoryUtil.findBestSlotInHotBar();
                    if (hotbarSlot != -1) {
                        Runnable toUseItem = () -> {
                            InventoryUtil.swapSlots(targetSlot, hotbarSlot);
                            targetSlot = hotbarSlot;
                            mc.player.getInventory().setSelectedSlot(targetSlot);
                            InventoryUtil.swapToSlot(targetSlot);
                            usageState = UsageState.USE_ITEM;
                            stateDelay = 1;
                        };
                        if (SlownessManager.isEnabled()) {
                            SlownessManager.applySlowness(10, toUseItem);
                        } else {
                            toUseItem.run();
                        }
                    } else {
                        usageState = UsageState.IDLE;
                    }
                    break;

                case USE_ITEM:
                    if (stateDelay-- > 0) return;

                    applyRotation();
                    InventoryUtil.useItem(InteractionHand.MAIN_HAND);
                    usageState = UsageState.RESTORE_SLOT;
                    stateDelay = 1;
                    break;

                case RESTORE_SLOT:
                    if (stateDelay-- > 0) return;

                    mc.player.getInventory().setSelectedSlot(previousSlot);
                    usageState = UsageState.IDLE;
                    pendingAction = null;
                    forceUse = true;
                    break;
            }
        }

        public enum UsageState {
            IDLE,
            SWAP_ITEM,
            USE_ITEM,
            RESTORE_SLOT
        }
    }
}