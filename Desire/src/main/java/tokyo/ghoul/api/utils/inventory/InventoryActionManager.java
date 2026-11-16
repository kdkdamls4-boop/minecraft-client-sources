package tokyo.ghoul.api.utils.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.PostTickEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.move.MotionEvent;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.inject.accessor.ClientboundContainerSetContentPacketAccessor;
import tokyo.ghoul.inject.accessor.ClientboundContainerSetSlotPacketAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Очередь инвентарных действий.
 * - шаги выполняются по тикам, на время шагов с priority движение игрока останавливается;
 * - до/после кликов клиентский инвентарь откатывается к снимку, а серверные обновления этих слотов
 *   маскируются на MASK_MS, чтобы предметы не "моргали";
 * - для verify-последовательностей stateId меню подменяется, сервер в ответ шлёт полный контент,
 *   по которому видно, приняты ли клики; если нет — клики откатываются и пересинхронизация повторяется.
 */
public enum InventoryActionManager implements QuickImports {
    INSTANCE;

    private static final long MASK_MS = 1500L;
    private static final long VERIFY_TIMEOUT_MS = 800L;

    private final ConcurrentLinkedDeque<InventoryActionSequence> queue = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Runnable> postTickTasks = new ConcurrentLinkedDeque<>();

    private InventoryActionSequence current;
    private int freezeTicks;
    private int stepDelay;
    private boolean runPending;

    private ItemStack[] snapshot;
    private long[] snapshotExpiry;
    private long snapshotExpiryMax;
    private int snapshotContainer;

    private ItemStack[] touchedSnapshot;
    private long[] touchedExpiry;
    private long touchedExpiryMax;
    private int touchedContainer;

    private ItemStack[] deferredServer;
    private ItemStack[] deferredExpected;
    private int deferredContainer;

    private volatile Connection trackedConnection;
    private volatile boolean stateIdTracked;
    private volatile int contentCounter;

    private InventoryActionSequence verifySequence;
    private InventoryActionSequence retrySequence;
    private InventoryActionSequence packetSequence;
    private ItemStack[] verifyBefore;
    private ItemStack[] verifyAfter;
    private ItemStack carriedBefore;
    private ItemStack carriedAfter;
    private int originalStateId;
    private int fakeStateId;
    private int contentCounterAtVerify;
    private int verifyRetries;
    private long verifyDeadline;

    public void load() {
        // в desire слушатель с большим приоритетом вызывается раньше; менеджер должен идти последним
        TickEvent.getInstance().subscribe(new Listener<>(-4, event -> onTick()));
        PostTickEvent.getInstance().subscribe(new Listener<>(-4, event -> onPostTick()));
        MotionEvent.getInstance().subscribe(new Listener<>(-4, event -> onMovementPacket()));
        MovementInputEvent.getInstance().subscribe(new Listener<>(-4, this::onInput));
        SprintEvent.getInstance().subscribe(new Listener<>(-4, event -> {
            if (freezeTicks > 0) event.setSprint(false);
        }));
        PacketEvent.getInstance().subscribe(new Listener<>(-4, this::onPacket));
    }

    public InventoryActionSequence enqueue(Runnable clicks) {
        return enqueue(clicks, 1, true);
    }

    public InventoryActionSequence enqueue(Runnable clicks, int priority, boolean close) {
        InventoryActionSequence sequence = new InventoryActionSequence();
        queue.add(sequence);
        return sequence.click(clicks, priority, close);
    }

    public InventoryActionSequence enqueueFirst(Runnable clicks, int priority, boolean close) {
        InventoryActionSequence sequence = new InventoryActionSequence();
        queue.offerFirst(sequence);
        return sequence.click(clicks, priority, close);
    }

    /** Прерывает текущую последовательность и ставит новую первой (для тотема). */
    public InventoryActionSequence enqueueNow(Runnable clicks, int priority, boolean close) {
        InventoryActionSequence sequence = new InventoryActionSequence();
        sequence.click(clicks, priority, close);
        interrupt(sequence);
        return sequence;
    }

    private void interrupt(InventoryActionSequence sequence) {
        if (current != null && (current.hasNext() || current == verifySequence)) {
            if (!current.isRestoreState()) {
                restoreSnapshot();
            }
            if (current.hasNext()) {
                queue.offerFirst(current);
            }
            current = null;
        }

        freezeTicks = 0;
        stepDelay = 0;
        runPending = false;
        snapshot = null;
        queue.offerFirst(sequence);
    }

    public InventoryActionSequence enqueueUse(Runnable action) {
        return enqueueUse(action, 1, false);
    }

    public InventoryActionSequence enqueueUse(Runnable action, int priority, boolean close) {
        InventoryActionSequence sequence = new InventoryActionSequence();
        queue.add(sequence);
        return sequence.use(action, priority, close);
    }

    public boolean isBusy() {
        return current != null || verifyBefore != null || !queue.isEmpty() || !postTickTasks.isEmpty();
    }

    public void runPostTick(Runnable task) {
        postTickTasks.add(task);
    }

    public long maskDuration() {
        return MASK_MS;
    }

    public void reset() {
        queue.clear();
        postTickTasks.clear();
        current = null;
        freezeTicks = 0;
        stepDelay = 0;
        runPending = false;
        packetSequence = null;
        snapshot = null;
        snapshotExpiry = null;
        snapshotExpiryMax = 0L;
        touchedSnapshot = null;
        touchedExpiry = null;
        touchedExpiryMax = 0L;
        deferredServer = null;
        deferredExpected = null;
        clearVerify();
    }

    private void onTick() {
        if (mc.player == null || mc.level == null) {
            reset();
            return;
        }

        applyDeferred();
        boolean wasFrozen = freezeTicks > 0;
        if (freezeTicks > 0) {
            freezeTicks--;
        }

        packetSequence = null;
        if (verifyBefore != null) {
            checkVerify();
        }

        if (runPending) {
            runPending = false;
            runCurrent();
            return;
        }

        if (current == null) {
            current = queue.poll();
            if (current == null) return;

            if (!current.isRestoreState()) {
                takeSnapshot();
            }
        }

        if (current == verifySequence) return;

        if (stepDelay > 0) {
            stepDelay--;
            if (stepDelay > 0) return;
        }

        int priority = current.currentPriority();
        if (priority > 0 && !wasFrozen && !current.currentIsUse()) {
            // сначала останавливаем игрока, клики — на следующем тике или в пакете движения
            freezeTicks = priority;
            if (current.isRunAtPacket()) {
                packetSequence = current;
            }
        } else {
            if (priority > freezeTicks) {
                freezeTicks = priority;
            }
            if (current.currentIsUse()) {
                runCurrent();
            } else {
                runPending = true;
            }
        }
    }

    private void onPostTick() {
        Runnable task;
        while ((task = postTickTasks.poll()) != null) {
            task.run();
        }
    }

    private void onMovementPacket() {
        InventoryActionSequence sequence = packetSequence;
        if (sequence == null) return;
        packetSequence = null;
        if (sequence == current && mc.player != null) {
            runCurrent();
        }
    }

    private void onInput(MovementInputEvent.MovementInputEventData event) {
        if (freezeTicks <= 0) return;

        event.setDirectionalInput(DirectionalInput.NONE);
        event.setJump(false);
        event.setSneak(false);
        if (runPending) {
            runPending = false;
            runCurrent();
        }
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (!event.isReceive()) return;

        trackStateId(event);
        if (mc.player == null) return;

        boolean snapshotMasking = current != null && !current.isRestoreState() && snapshot != null;
        if (!snapshotMasking && deferredServer == null && !snapshotActive() && !touchedActive()) return;

        int containerId = mc.player.containerMenu.containerId;
        long now = System.currentTimeMillis();
        NonNullList<Slot> slots = mc.player.containerMenu.slots;

        if (event.packet() instanceof ClientboundContainerSetSlotPacket packet) {
            if (packet.getContainerId() != containerId) return;

            ItemStack replacement = maskSlot(packet.getSlot(), packet.getItem(), snapshotMasking, now, containerId, slots, false);
            if (replacement != null) {
                ((ClientboundContainerSetSlotPacketAccessor) (Object) packet).setItemStack(replacement);
            }
        } else if (event.packet() instanceof ClientboundContainerSetContentPacket packet) {
            if (packet.containerId() != containerId) return;

            List<ItemStack> items = packet.items();
            List<ItemStack> masked = new ArrayList<>(items.size());
            int replaced = 0;
            for (int i = 0; i < items.size(); i++) {
                ItemStack replacement = maskSlot(i, items.get(i), snapshotMasking, now, containerId, slots, true);
                if (replacement == null) {
                    masked.add(items.get(i));
                } else {
                    masked.add(replacement);
                    replaced++;
                }
            }
            if (replaced > 0) {
                ((ClientboundContainerSetContentPacketAccessor) (Object) packet).setItems(masked);
            }
        }
    }

    private void trackStateId(PacketEvent.PacketEventData event) {
        int stateId;
        if (event.packet() instanceof ClientboundContainerSetContentPacket packet && packet.containerId() == 0) {
            contentCounter++;
            stateId = packet.stateId();
        } else if (event.packet() instanceof ClientboundContainerSetSlotPacket packet && packet.getContainerId() == 0) {
            stateId = packet.getStateId();
        } else {
            return;
        }

        trackedConnection = mc.getConnection() != null ? mc.getConnection().getConnection() : null;
        stateIdTracked = stateId != 0;
    }

    private ItemStack maskSlot(int slot, ItemStack serverItem, boolean snapshotMasking, long now, int containerId,
                               List<Slot> slots, boolean fullContent) {
        if (slot < 0) return null;

        ItemStack replacement = null;
        boolean fromTouched = false;
        if (snapshot != null && containerId == snapshotContainer && slot < snapshot.length
                && shouldMaskSnapshot(slot, serverItem, snapshotMasking, now, slots)) {
            replacement = snapshot[slot];
        } else if (fullContent && touchedSnapshot != null && containerId == touchedContainer && slot < touchedSnapshot.length
                && shouldMaskTouched(slot, serverItem, now, slots)) {
            replacement = touchedSnapshot[slot];
            fromTouched = true;
        }

        boolean defer = !snapshotMasking && !fromTouched && replacement != null && !ItemStack.matches(serverItem, replacement);
        deferServerItem(slot, defer ? serverItem.copy() : null, replacement, containerId, slots);
        return replacement == null ? null : replacement.copy();
    }

    private void deferServerItem(int slot, ItemStack serverItem, ItemStack expected, int containerId, List<Slot> slots) {
        if (serverItem == null) {
            if (deferredServer != null && deferredContainer == containerId && slot < deferredServer.length) {
                deferredServer[slot] = null;
            }
            return;
        }

        if (deferredServer == null || deferredServer.length != slots.size() || deferredContainer != containerId) {
            deferredServer = new ItemStack[slots.size()];
            deferredExpected = new ItemStack[slots.size()];
            deferredContainer = containerId;
        }
        deferredServer[slot] = serverItem;
        deferredExpected[slot] = expected;
    }

    // замаскированное серверное значение применяется, когда маска истекла и слот всё ещё такой, как мы ждали
    private void applyDeferred() {
        if (deferredServer == null) return;
        if (current != null && !current.isRestoreState()) return;

        int containerId = mc.player.containerMenu.containerId;
        if (containerId != deferredContainer) {
            deferredServer = null;
            deferredExpected = null;
            return;
        }

        NonNullList<Slot> slots = mc.player.containerMenu.slots;
        long now = System.currentTimeMillis();
        boolean pending = false;
        for (int i = 0; i < deferredServer.length && i < slots.size(); i++) {
            ItemStack serverItem = deferredServer[i];
            if (serverItem == null) continue;

            if (!isMaskExpired(i, now, containerId)) {
                pending = true;
            } else {
                deferredServer[i] = null;
                if (ItemStack.matches(slots.get(i).getItem(), deferredExpected[i])) {
                    slots.get(i).set(serverItem);
                }
            }
        }

        if (!pending) {
            deferredServer = null;
            deferredExpected = null;
        }
    }

    private boolean isMaskExpired(int slot, long now, int containerId) {
        if (snapshot != null && snapshotContainer == containerId && snapshotExpiry != null
                && slot < snapshotExpiry.length && now <= snapshotExpiry[slot]) {
            return false;
        }
        return touchedSnapshot == null || touchedContainer != containerId || touchedExpiry == null
                || slot >= touchedExpiry.length || now > touchedExpiry[slot];
    }

    private boolean snapshotActive() {
        return snapshot != null && snapshotExpiry != null && System.currentTimeMillis() <= snapshotExpiryMax;
    }

    private boolean touchedActive() {
        return touchedSnapshot != null && System.currentTimeMillis() <= touchedExpiryMax;
    }

    private boolean shouldMaskSnapshot(int slot, ItemStack serverItem, boolean snapshotMasking, long now, List<Slot> slots) {
        if (snapshotExpiry == null || slot >= slots.size() || now > snapshotExpiry[slot]) {
            return false;
        }
        if (snapshotMasking) {
            return true;
        }
        if (!ItemStack.matches(snapshot[slot], slots.get(slot).getItem())) {
            snapshotExpiry[slot] = 0L;
            return false;
        }
        if (ItemStack.isSameItem(serverItem, snapshot[slot])) {
            snapshotExpiry[slot] = 0L;
            return false;
        }
        if (serverItem.isEmpty() && snapshot[slot].getCount() == 1 && !isHeldSlot(slot)) {
            snapshotExpiry[slot] = 0L;
            return false;
        }
        return true;
    }

    private boolean shouldMaskTouched(int slot, ItemStack serverItem, long now, List<Slot> slots) {
        ItemStack expected = touchedSnapshot[slot];
        if (expected == null || slot >= slots.size() || now > touchedExpiry[slot]) {
            return false;
        }
        if (!ItemStack.matches(expected, slots.get(slot).getItem())) {
            touchedExpiry[slot] = 0L;
            return false;
        }
        if (ItemStack.isSameItem(serverItem, expected)) {
            touchedExpiry[slot] = 0L;
            return false;
        }
        return true;
    }

    private void rememberTouched(InventoryActionSequence sequence) {
        int[] touched = sequence.getTouchedSlots();
        if (touched == null || mc.player == null) return;

        NonNullList<Slot> slots = mc.player.containerMenu.slots;
        int containerId = mc.player.containerMenu.containerId;
        if (touchedSnapshot == null || touchedSnapshot.length != slots.size() || touchedContainer != containerId) {
            touchedSnapshot = new ItemStack[slots.size()];
            touchedExpiry = new long[slots.size()];
            touchedContainer = containerId;
        }

        long expiry = System.currentTimeMillis() + MASK_MS;
        for (int slot : touched) {
            if (slot < 0 || slot >= slots.size()) continue;
            touchedSnapshot[slot] = slots.get(slot).getItem().copy();
            touchedExpiry[slot] = expiry;
            if (snapshotExpiry != null && slot < snapshotExpiry.length) {
                snapshotExpiry[slot] = 0L;
            }
        }
        if (expiry > touchedExpiryMax) {
            touchedExpiryMax = expiry;
        }
    }

    private boolean isHeldSlot(int slot) {
        if (mc.player == null) return false;
        return slot == 45 || slot == 36 + mc.player.getInventory().getSelectedSlot();
    }

    private static ItemStack[] copySlots(List<Slot> slots) {
        ItemStack[] copy = new ItemStack[slots.size()];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = slots.get(i).getItem().copy();
        }
        return copy;
    }

    private void takeSnapshot() {
        if (mc.player == null) {
            snapshot = null;
            return;
        }
        snapshot = copySlots(mc.player.containerMenu.slots);
        snapshotContainer = mc.player.containerMenu.containerId;
        if (snapshotExpiry == null || snapshotExpiry.length != snapshot.length) {
            snapshotExpiry = new long[snapshot.length];
        }
    }

    // клиентский инвентарь возвращается к снимку, изменённые слоты маскируются до ответа сервера
    private void restoreSnapshot() {
        if (snapshot == null || mc.player == null || mc.player.containerMenu.containerId != snapshotContainer) return;

        NonNullList<Slot> slots = mc.player.containerMenu.slots;
        long expiry = System.currentTimeMillis() + MASK_MS;
        for (int i = 0; i < snapshot.length && i < slots.size(); i++) {
            if (!ItemStack.matches(snapshot[i], slots.get(i).getItem())) {
                slots.get(i).set(snapshot[i].copy());
                snapshotExpiry[i] = expiry;
                if (expiry > snapshotExpiryMax) {
                    snapshotExpiryMax = expiry;
                }
            }
        }
    }

    private void runCurrent() {
        boolean verify = current.isVerify() && verifyBefore == null && canVerify();
        if (verify) {
            beginVerify();
        }

        stepDelay = current.runStep();
        if (stepDelay > 0 && current.currentDelayStop() && stepDelay > freezeTicks) {
            freezeTicks = stepDelay;
        }

        if (!current.isRestoreState()) {
            restoreSnapshot();
        }
        if (verify) {
            endVerify();
        }

        if (!current.hasNext() && verifySequence != current) {
            finishCurrent();
        }
    }

    private void finishCurrent() {
        rememberTouched(current);
        current.cancel();
        current = null;
    }

    private boolean canVerify() {
        return stateIdTracked
                && mc.player.containerMenu == mc.player.inventoryMenu
                && mc.getConnection() != null
                && mc.getConnection().getConnection() == trackedConnection;
    }

    private void beginVerify() {
        InventoryMenu menu = mc.player.inventoryMenu;
        verifySequence = current;
        verifyBefore = copySlots(menu.slots);
        carriedBefore = menu.getCarried().copy();
        originalStateId = menu.getStateId();
        fakeStateId = originalStateId + 16384 & 32767;
        menu.stateId = fakeStateId;
        contentCounterAtVerify = contentCounter;
        verifyRetries = 0;
    }

    private void endVerify() {
        InventoryMenu menu = mc.player.inventoryMenu;
        verifyAfter = copySlots(menu.slots);
        carriedAfter = menu.getCarried().copy();
        if (!verifyChanged()) {
            if (menu.getStateId() == fakeStateId) {
                menu.stateId = originalStateId;
            }
            clearVerify();
        } else {
            verifyDeadline = System.currentTimeMillis() + VERIFY_TIMEOUT_MS;
        }
    }

    private boolean verifyChanged() {
        if (!ItemStack.matches(carriedBefore, carriedAfter)) {
            return true;
        }
        for (int i = 0; i < verifyBefore.length && i < verifyAfter.length; i++) {
            if (!ItemStack.matches(verifyBefore[i], verifyAfter[i])) {
                return true;
            }
        }
        return false;
    }

    // сервер прислал полный контент после фейкового stateId — клики приняты; иначе ретрай/откат
    private void checkVerify() {
        boolean answered = contentCounter != contentCounterAtVerify;
        if (!answered && System.currentTimeMillis() <= verifyDeadline) return;

        if (!answered && verifyRetries < 2) {
            retryVerify();
            return;
        }

        InventoryActionSequence sequence = verifySequence;
        if (!answered) {
            rollbackVerify();
        }
        clearVerify();

        if (!answered || !sequence.hasNext()) {
            if (sequence != current) {
                queue.remove(sequence);
                sequence.cancel();
            } else if (answered) {
                finishCurrent();
            } else {
                sequence.cancel();
                current = null;
                stepDelay = 0;
            }
        }
    }

    private void retryVerify() {
        verifyRetries++;
        verifyDeadline = Long.MAX_VALUE;

        InventoryActionSequence sequence = verifySequence;
        if (sequence == current) {
            current = null;
            stepDelay = 0;
            if (sequence.hasNext()) {
                queue.offerFirst(sequence);
            }
        }

        retrySequence = new InventoryActionSequence().click(this::resyncClick, 1, false);
        queue.offerFirst(retrySequence);
    }

    // клик мимо окна с фейковым stateId — сервер обязан ответить полным контентом
    private void resyncClick() {
        if (verifyBefore == null || mc.player == null) return;

        InventoryMenu menu = mc.player.inventoryMenu;
        if (mc.player.containerMenu == menu && menu.getCarried().isEmpty()) {
            menu.stateId = fakeStateId;
            mc.gameMode.handleContainerInput(menu.containerId, -999, 0, ContainerInput.PICKUP, mc.player);
            mc.player.connection.send(new ServerboundContainerClosePacket(menu.containerId));
        }
        verifyDeadline = System.currentTimeMillis() + VERIFY_TIMEOUT_MS;
    }

    private void rollbackVerify() {
        InventoryMenu menu = mc.player.inventoryMenu;
        NonNullList<Slot> slots = menu.slots;
        for (int i = 0; i < verifyBefore.length && i < verifyAfter.length && i < slots.size(); i++) {
            if (!ItemStack.matches(verifyBefore[i], verifyAfter[i]) && ItemStack.matches(slots.get(i).getItem(), verifyAfter[i])) {
                slots.get(i).set(verifyBefore[i].copy());
            }
        }
        if (!ItemStack.matches(carriedBefore, carriedAfter) && ItemStack.matches(menu.getCarried(), carriedAfter)) {
            menu.setCarried(carriedBefore.copy());
        }
        if (menu.getStateId() == fakeStateId) {
            menu.stateId = originalStateId;
        }
    }

    private void clearVerify() {
        if (retrySequence != null) {
            queue.remove(retrySequence);
            if (current == retrySequence) {
                current = null;
                stepDelay = 0;
                runPending = false;
            }
            retrySequence = null;
        }

        verifySequence = null;
        verifyBefore = null;
        verifyAfter = null;
        carriedBefore = null;
        carriedAfter = null;
    }
}
