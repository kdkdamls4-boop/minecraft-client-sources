package tokyo.ghoul.api.utils.inventory;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.experimental.UtilityClass;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.combat.AutoTotemModule;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/** Низкоуровневые клики и использование предметов для InventoryActionManager. */
@UtilityClass
public class InventoryActions implements QuickImports {
    public static final int OFFHAND_BUTTON = 40;

    private int offhandRestoreSlot = -1;

    public boolean isValidClick(int slot, int button, ContainerInput type) {
        return switch (type) {
            case PICKUP, QUICK_MOVE, CLONE -> button >= 0 && button <= 2;
            case SWAP -> slot >= 0 && (button >= 0 && button < 9 || button == OFFHAND_BUTTON);
            case THROW -> button == 0 || button == 1;
            case QUICK_CRAFT -> button >= 0 && button <= 10 && button != 3 && button != 7;
            case PICKUP_ALL -> button == 0;
        };
    }

    public void click(int containerId, int slot, int button, ContainerInput type, Player player) {
        if (mc.gameMode != null && isValidClick(slot, button, type)) {
            mc.gameMode.handleContainerInput(containerId, slot, button, type, player);
        }
    }

    public void click(int slot, int button, ContainerInput type) {
        if (mc.player != null) {
            click(0, slot, button, type, mc.player);
        }
    }

    public void swapToOffhand(int slot) {
        click(mc.player.containerMenu.containerId, slot, OFFHAND_BUTTON, ContainerInput.SWAP, mc.player);
    }

    /** Предмет с зачарованием, лором, атрибутами или кастомным именем — такой бережём. */
    public boolean isValuable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isEnchanted() || stack.hasFoil()) return true;
        if (Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE))) return true;
        if (stack.has(DataComponents.CUSTOM_MODEL_DATA) || stack.has(DataComponents.CUSTOM_NAME)) return true;

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null && !lore.lines().isEmpty()) return true;

        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        return modifiers != null && !modifiers.modifiers().isEmpty();
    }

    private boolean isImportant(ItemStack stack) {
        Item item = stack.getItem();
        if (item == Items.NETHERITE_SWORD || item == Items.DIAMOND_SWORD || item == Items.IRON_SWORD) return true;
        if (item instanceof AxeItem || item == Items.MACE) return true;
        return item == Items.ELYTRA || item == Items.TOTEM_OF_UNDYING || item == Items.END_CRYSTAL
                || item == Items.OBSIDIAN || item == Items.CRYING_OBSIDIAN;
    }

    public int findEmptyHotbar(int exclude) {
        if (mc.player == null) return -1;
        NonNullList<ItemStack> items = mc.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < 9; i++) {
            if (i != exclude && items.get(i).isEmpty() && mc.player.getInventory().getSelectedSlot() != i) {
                return i;
            }
        }
        return -1;
    }

    public int findReplaceableHotbar(int exclude) {
        if (mc.player == null) return -1;
        NonNullList<ItemStack> items = mc.player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < 9; i++) {
            if (i != exclude && mc.player.getInventory().getSelectedSlot() != i && !isImportant(items.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public int findFreeHotbar() {
        int empty = findEmptyHotbar(-1);
        return empty != -1 ? empty : findReplaceableHotbar(-1);
    }

    /** Перекладывает предмет из слота меню from в слот to (обычно в броню). */
    public void moveSlot(int from, int to) {
        if (from < 9) {
            click(to, from, ContainerInput.SWAP);
        } else if (from >= 36 && from < 45) {
            click(to, from - 36, ContainerInput.SWAP);
        } else {
            int hotbar = findFreeHotbar();
            if (hotbar == -1) {
                click(from, 0, ContainerInput.PICKUP);
                click(to, 0, ContainerInput.PICKUP);
                if (!mc.player.containerMenu.getCarried().isEmpty()) {
                    click(from, 0, ContainerInput.PICKUP);
                }
            } else {
                click(from, hotbar, ContainerInput.SWAP);
                click(to, hotbar, ContainerInput.SWAP);
                click(from, hotbar, ContainerInput.SWAP);
            }
        }
    }

    public InventoryActionSequence moveSlotQueued(int from, int to) {
        return from != to && from != -1 ? InventoryActionManager.INSTANCE.enqueue(() -> moveSlot(from, to)) : null;
    }

    private Slot findSlot(Predicate<ItemStack> predicate, boolean inventoryOnly) {
        NonNullList<Slot> slots = mc.player.containerMenu.slots;
        int start = slots.size() - 37;
        for (Slot slot : slots) {
            if (predicate.test(slot.getItem()) && (!inventoryOnly || slot.index >= start)) {
                return slot;
            }
        }
        return null;
    }

    public Slot findSlot(Item item) {
        return findSlot(stack -> stack.getItem() == item, false);
    }

    public int findInventorySlot(Item item, boolean hotbar) {
        if (mc.player == null) return -1;
        if (hotbar) {
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == item) return i;
            }
        } else {
            for (int i = 9; i < 45; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == item) return i;
            }
        }
        return -1;
    }

    private int hotbarIndex(Slot slot) {
        if (slot == null || mc.player == null || slot.container != mc.player.getInventory()) return -1;
        int index = slot.getContainerSlot();
        return index >= 0 && index < 9 ? index : -1;
    }

    private void sendCarried(int slot) {
        if (mc.player != null && slot >= 0 && slot <= 8) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    public InteractionResult useItem(InteractionHand hand) {
        if (mc.gameMode == null || mc.player == null) return InteractionResult.FAIL;
        return mc.gameMode.useItem(mc.player, hand);
    }

    public boolean use(InteractionHand hand, boolean swing) {
        if (mc.gameMode == null || mc.player == null) return false;
        mc.gameMode.useItem(mc.player, hand);
        if (swing) {
            mc.player.swing(hand);
        }
        return false;
    }

    public boolean use(InteractionHand hand) {
        return use(hand, true);
    }

    public void swapToHand(int slot, InteractionHand hand) {
        if (slot == -1) return;
        int button = hand == InteractionHand.MAIN_HAND ? mc.player.getInventory().getSelectedSlot() : OFFHAND_BUTTON;
        click(slot, button, ContainerInput.SWAP);
    }

    /** Использовать предмет из слота: из хотбара — тихой сменой слота, иначе — свапом через очередь. */
    public boolean useFromSlot(Slot slot, InteractionHand hand) {
        if (mc.player == null) return false;

        int hotbar = hand == InteractionHand.MAIN_HAND ? hotbarIndex(slot) : -1;
        if (hotbar != -1) {
            int selected = mc.player.getInventory().getSelectedSlot();
            sendCarried(hotbar);
            use(hand);
            sendCarried(selected);
            return true;
        }

        InventoryActionManager.INSTANCE.enqueue(() -> {
            swapToHand(slot.index, hand);
            use(hand);
            swapToHand(slot.index, hand);
        });
        return true;
    }

    public void useStack(ItemStack stack) {
        if (mc.player == null || mc.player.getCooldowns().isOnCooldown(stack)) return;
        Slot slot = findSlot(stack.getItem());
        if (slot != null) {
            useFromSlot(slot, InteractionHand.MAIN_HAND);
        }
    }

    /** Использовать предмет через левую руку и вернуть прежний предмет при следующем вызове. */
    public void useViaOffhand(Item item) {
        if (mc.player == null || AutoTotemModule.isBusyStatic()) return;

        boolean hadOffhand = !mc.player.getOffhandItem().isEmpty();
        Slot slot = findSlot(item);
        if (slot == null) return;

        int index = slot.index;
        if (index >= 0) {
            swapToOffhand(index);
            use(InteractionHand.OFF_HAND, false);
            if (hadOffhand && offhandRestoreSlot == -1) {
                offhandRestoreSlot = index;
            }
        }

        if (mc.player.getOffhandItem().getItem() == item && offhandRestoreSlot != -1) {
            swapToOffhand(offhandRestoreSlot);
            offhandRestoreSlot = -1;
        }
    }

    /** Фейерверк из хотбара тихой сменой слота; если его там нет — перекладывается в 9-й слот. */
    public void useFirework() {
        Item rocket = Items.FIREWORK_ROCKET;
        if (mc.player.isUsingItem()) {
            InventoryActionManager.INSTANCE.enqueueUse(() -> useViaOffhand(rocket));
            return;
        }

        int selected = mc.player.getInventory().getSelectedSlot();
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).getItem() != rocket) continue;

            if (i != selected) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(i));
            }
            use(InteractionHand.MAIN_HAND, false);
            if (i != selected) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(selected));
            }
            return;
        }

        InventoryActionManager.INSTANCE.enqueue(() -> {
            for (int i = 0; i < 36; i++) {
                if (mc.player.getInventory().getItem(i).getItem() == rocket) {
                    click(0, i, 8, ContainerInput.SWAP, mc.player);
                    break;
                }
            }
        }, 1, true);
    }

    /** Клик PICKUP_ALL случайным предметом мимо слотов: сервер отвечает пересинхронизацией инвентаря. */
    public void sendFakeClick() {
        AbstractContainerMenu menu = mc.player.containerMenu;
        ItemStack stack = BuiltInRegistries.ITEM.byId(ThreadLocalRandom.current().nextInt(0, 100)).getDefaultInstance();
        HashedStack hash = HashedStack.create(stack, mc.player.connection.decoratedHashOpsGenenerator());
        mc.player.connection.send(new ServerboundContainerClickPacket(menu.containerId, menu.getStateId(), (short) 0, (byte) 0,
                ContainerInput.PICKUP_ALL, Int2ObjectMaps.singleton(0, hash), hash));
    }

    public boolean hasFreeInventorySlot() {
        for (int i = 9; i < 45; i++) {
            if (mc.player.containerMenu.getSlot(i).getItem().isEmpty()) return true;
        }
        return false;
    }
}
