package tokyo.ghoul.api.utils.inventory;

import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * "Легитные" действия: использование предметов и перекладывание брони идут через очередь
 * InventoryActionManager с задержками между свапом, использованием и обратным свапом.
 */
public enum DelayedInventoryActionManager implements QuickImports {
    INSTANCE;

    private static final int MAX_QUEUE = 16;
    private static final int MAX_BATCH = 3;

    private final ConcurrentLinkedDeque<Request> queue = new ConcurrentLinkedDeque<>();
    private int cooldown;

    public void load() {
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
    }

    public void addCooldown(int ticks) {
        if (ticks > cooldown) {
            cooldown = ticks;
        }
    }

    private void onUpdate() {
        if (mc.player == null || mc.level == null) {
            InventoryActionManager.INSTANCE.reset();
            queue.clear();
            cooldown = 0;
        } else if (cooldown > 0) {
            cooldown--;
        } else {
            process();
        }
    }

    public boolean isIdle() {
        return queue.isEmpty() && !InventoryActionManager.INSTANCE.isBusy();
    }

    public void use(Item item) {
        use(InteractionHand.MAIN_HAND, item);
    }

    public void use(Slot slot) {
        use(InteractionHand.MAIN_HAND, slot);
    }

    public void use(InteractionHand hand, Item item) {
        if (mc.player != null && mc.level != null) {
            request(hand, item);
        }
    }

    public void use(InteractionHand hand, Slot slot) {
        if (mc.player != null && mc.level != null) {
            request(hand, slot);
        }
    }

    private void request(InteractionHand hand, Object target) {
        if (mc.player == null || mc.level == null || mc.gameMode == null || target == null) return;
        if (queue.size() >= MAX_QUEUE) return;
        if (itemOf(target) == Items.FIREWORK_ROCKET && countQueued(Items.FIREWORK_ROCKET) >= 2) return;

        queue.offerLast(new Request(hand, target));
        if (cooldown == 0) {
            process();
        }
    }

    private Item itemOf(Object target) {
        if (target instanceof Item item) return item;
        return target instanceof Slot slot ? slot.getItem().getItem() : null;
    }

    private int countQueued(Item item) {
        int count = 0;
        for (Request request : queue) {
            if (itemOf(request.target()) == item) count++;
        }
        return count;
    }

    private void execute(InteractionHand hand, Object target) {
        if (mc.player == null) return;

        Slot fromSlot = null;
        Item item;
        ItemStack stack;
        if (target instanceof Item asItem) {
            item = asItem;
            Slot found = InventoryActions.findSlot(asItem);
            stack = found == null ? ItemStack.EMPTY : found.getItem();
        } else if (target instanceof Slot slot) {
            fromSlot = slot;
            stack = slot.getItem();
            if (stack == null || stack.isEmpty()) return;
            item = stack.getItem();
        } else {
            return;
        }

        if (item == Items.AIR) return;
        if (item != Items.FIREWORK_ROCKET && !stack.isEmpty() && mc.player.getCooldowns().isOnCooldown(stack)) return;

        Item inHand = hand == InteractionHand.OFF_HAND ? mc.player.getOffhandItem().getItem() : mc.player.getMainHandItem().getItem();
        if (fromSlot != null && fromSlot.container == mc.player.getInventory()) {
            useInventorySlot(fromSlot, hand);
        } else if (inHand == item) {
            if (InventoryActions.use(hand)) {
                mc.player.swing(hand);
            }
        } else {
            boolean offhand = hand == InteractionHand.OFF_HAND;
            int inventory = InventoryActions.findInventorySlot(item, false);
            int hotbar = InventoryActions.findInventorySlot(item, true);
            if (hotbar == -1 && inventory == -1) return;

            int selected = mc.player.getInventory().getSelectedSlot();
            int button = offhand ? InventoryActions.OFFHAND_BUTTON : selected;
            int containerId = mc.player.containerMenu.containerId;
            if (hotbar == -1) {
                swapUseSwap(containerId, inventory, button, hand);
            } else if (offhand) {
                swapUseSwap(containerId, hotbar + 36, button, hand);
            } else {
                carriedUse(hotbar, selected, hand);
            }
        }
    }

    private void useInventorySlot(Slot slot, InteractionHand hand) {
        int index = slot.getContainerSlot();
        int selected = mc.player.getInventory().getSelectedSlot();
        int button = hand == InteractionHand.OFF_HAND ? InventoryActions.OFFHAND_BUTTON : selected;
        int containerId = mc.player.containerMenu.containerId;

        if (index >= 0 && index < 9) {
            if (hand == InteractionHand.OFF_HAND) {
                swapUseSwap(containerId, slot.index, button, hand);
            } else {
                carriedUse(index, selected, hand);
            }
        } else if (index >= 9 && index < 36) {
            swapUseSwap(containerId, slot.index, button, hand);
        }
    }

    // свап в руку, тик паузы, использование, тик паузы, свап обратно
    private void swapUseSwap(int containerId, int slot, int button, InteractionHand hand) {
        InventoryActionManager.INSTANCE
                .enqueue(() -> mc.gameMode.handleContainerInput(containerId, slot, button, ContainerInput.SWAP, mc.player), 1, true)
                .delay(1, false)
                .use(() -> {
                    InventoryActions.useItem(hand);
                    mc.player.swing(hand);
                }, 1, false)
                .delay(1, false)
                .click(() -> mc.gameMode.handleContainerInput(containerId, slot, button, ContainerInput.SWAP, mc.player), 1, true)
                .restoreState(false);
    }

    private void carriedUse(int hotbar, int selected, InteractionHand hand) {
        InventoryActionManager.INSTANCE.enqueueUse(() -> {
            if (selected != hotbar) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbar));
            }
        }, 0, false).nextTick().use(() -> InventoryActions.use(hand), 0, false).nextTick().use(() -> {
            if (selected != hotbar) {
                mc.player.connection.send(new ServerboundSetCarriedItemPacket(selected));
            }
        }, 0, false);
    }

    /** Переложить предмет из слота меню from в слот to через хотбар, по шагу в тик. */
    public InventoryActionSequence moveSlot(int from, int to) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return null;
        if (from == to || from == -1 || to == -1) return null;
        if (InventoryActionManager.INSTANCE.isBusy()) return null;

        int containerId = mc.player.containerMenu.containerId;
        int hotbar = mc.player.getInventory().getSelectedSlot() % 8 + 1;
        if (from >= 36 && from < 45) {
            return InventoryActionManager.INSTANCE.enqueue(() ->
                    mc.gameMode.handleContainerInput(containerId, to, from - 36, ContainerInput.SWAP, mc.player));
        }

        return InventoryActionManager.INSTANCE
                .enqueue(() -> mc.gameMode.handleContainerInput(containerId, from, hotbar, ContainerInput.SWAP, mc.player))
                .nextTick()
                .clickClose(() -> mc.gameMode.handleContainerInput(containerId, to, hotbar, ContainerInput.SWAP, mc.player))
                .nextTick()
                .clickClose(() -> mc.gameMode.handleContainerInput(containerId, from, hotbar, ContainerInput.SWAP, mc.player));
    }

    private void process() {
        if (mc.gameMode == null || InventoryActionManager.INSTANCE.isBusy()) return;

        Request first = queue.peekFirst();
        if (first == null) return;

        if (!isBatchable(first)) {
            Request request = queue.pollFirst();
            if (request != null) {
                execute(request.hand(), request.target());
            }
            return;
        }

        List<Request> batch = new ArrayList<>();
        while (batch.size() < MAX_BATCH) {
            Request next = queue.peekFirst();
            if (next == null || !isBatchable(next)) break;
            queue.pollFirst();
            batch.add(next);
        }

        if (batch.size() == 1) {
            execute(batch.get(0).hand(), batch.get(0).target());
        } else if (!batch.isEmpty()) {
            executeBatch(batch);
        }
    }

    private boolean isBatchable(Request request) {
        return request.hand() == InteractionHand.MAIN_HAND && request.target() instanceof Item;
    }

    // несколько предметов: раскладка в хотбар, использование по очереди, раскладка обратно
    private void executeBatch(List<Request> batch) {
        int containerId = mc.player.containerMenu.containerId;
        int selected = mc.player.getInventory().getSelectedSlot();
        boolean[] usedHotbar = new boolean[9];
        List<int[]> swaps = new ArrayList<>();
        List<HotbarUse> uses = new ArrayList<>();

        for (Request request : batch) {
            Item item = (Item) request.target();
            int hotbar = InventoryActions.findInventorySlot(item, true);
            if (hotbar != -1) {
                usedHotbar[hotbar] = true;
                uses.add(new HotbarUse(hotbar));
                continue;
            }

            int inventory = InventoryActions.findInventorySlot(item, false);
            if (inventory == -1) continue;

            int existing = swapTarget(swaps, inventory);
            if (existing != -1) {
                uses.add(new HotbarUse(existing));
            } else {
                int free = nearestFree(usedHotbar, selected);
                if (free != -1) {
                    usedHotbar[free] = true;
                    swaps.add(new int[]{inventory, free});
                    uses.add(new HotbarUse(free));
                }
            }
        }

        if (uses.isEmpty()) return;

        int[] carried = new int[]{selected};
        int priority = swaps.isEmpty() ? 0 : 1;
        InventoryActionManager.INSTANCE.enqueue(() -> applySwaps(containerId, swaps), priority, !swaps.isEmpty())
                .nextTick()
                .use(() -> {
                    for (HotbarUse use : uses) {
                        selectCarried(carried, use.hotbarSlot());
                        if (InventoryActions.useItem(InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS) {
                            mc.player.swing(InteractionHand.MAIN_HAND);
                        }
                    }
                    selectCarried(carried, selected);
                }, priority, false)
                .nextTick()
                .click(() -> applySwaps(containerId, swaps), priority, !swaps.isEmpty());
    }

    private void applySwaps(int containerId, List<int[]> swaps) {
        if (mc.player == null || mc.gameMode == null) return;
        for (int[] swap : swaps) {
            mc.gameMode.handleContainerInput(containerId, swap[0], swap[1], ContainerInput.SWAP, mc.player);
        }
    }

    private int swapTarget(List<int[]> swaps, int inventory) {
        for (int[] swap : swaps) {
            if (swap[0] == inventory) return swap[1];
        }
        return -1;
    }

    private static int nearestFree(boolean[] used, int selected) {
        if (selected >= 0 && selected < 9 && !used[selected]) return selected;
        for (int offset = 1; offset < 9; offset++) {
            int right = selected + offset;
            if (right < 9 && !used[right]) return right;
            int left = selected - offset;
            if (left >= 0 && !used[left]) return left;
        }
        return -1;
    }

    private void selectCarried(int[] carried, int slot) {
        if (mc.player == null) return;
        if (slot >= 0 && slot <= 8 && carried[0] != slot) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            carried[0] = slot;
        }
    }

    private record Request(InteractionHand hand, Object target) {
    }

    private record HotbarUse(int hotbarSlot) {
    }
}
