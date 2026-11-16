package tokyo.ghoul.api.utils.inventory;

import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.List;

/**
 * Цепочка инвентарных шагов. Каждый шаг выполняется InventoryActionManager в свой тик,
 * между шагами можно вставлять задержку, а priority > 0 заставляет менеджер сначала
 * остановить движение игрока на столько тиков.
 */
public class InventoryActionSequence implements QuickImports {
    private final List<Step> steps = new ArrayList<>();
    private int cursor;
    private int pendingDelay;
    private boolean pendingDelayStop;
    private boolean restoreState = true;
    private boolean verify;
    private boolean runAtPacket;
    private boolean cancelled;
    private int[] touchedSlots;

    /** Шаг с кликами по инвентарю. */
    public InventoryActionSequence click(Runnable clicks, int priority, boolean close) {
        steps.add(new Step(clicks, pendingDelay, priority, close, false, pendingDelayStop));
        pendingDelay = 0;
        pendingDelayStop = false;
        return this;
    }

    /** Шаг использования предмета (не трогает инвентарь сервера). */
    public InventoryActionSequence use(Runnable action, int priority, boolean close) {
        steps.add(new Step(action, pendingDelay, priority, close, true, pendingDelayStop));
        pendingDelay = 0;
        pendingDelayStop = false;
        return this;
    }

    public InventoryActionSequence use(Runnable action) {
        return use(action, 1, false);
    }

    public InventoryActionSequence clickClose(Runnable clicks) {
        return click(clicks, 1, true);
    }

    public InventoryActionSequence delay(int ticks) {
        return delay(ticks, false);
    }

    /** Задержка перед следующим шагом; stopMovement — держать движение остановленным на её время. */
    public InventoryActionSequence delay(int ticks, boolean stopMovement) {
        pendingDelay += ticks;
        if (stopMovement) pendingDelayStop = true;
        return this;
    }

    public InventoryActionSequence nextTick() {
        return delay(1, false);
    }

    /** Проверять, что сервер принял клики (трюк со stateId). */
    public InventoryActionSequence verify() {
        verify = true;
        return this;
    }

    /** Выполнять шаг в момент отправки пакета движения, а не в начале тика. */
    public InventoryActionSequence atMovementPacket() {
        runAtPacket = true;
        return this;
    }

    public InventoryActionSequence restoreState(boolean restore) {
        restoreState = restore;
        return this;
    }

    /** Слоты, которые нужно замаскировать от серверных обновлений после выполнения. */
    public InventoryActionSequence touch(int... slots) {
        touchedSlots = slots;
        return this;
    }

    void cancel() {
        cancelled = true;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isVerify() {
        return verify;
    }

    public boolean isRunAtPacket() {
        return runAtPacket;
    }

    public boolean isRestoreState() {
        return restoreState;
    }

    public int[] getTouchedSlots() {
        return touchedSlots;
    }

    public boolean hasNext() {
        return cursor < steps.size();
    }

    public int currentPriority() {
        return hasNext() ? steps.get(cursor).priority() : 0;
    }

    public boolean currentIsUse() {
        return hasNext() && steps.get(cursor).use();
    }

    public boolean currentDelayStop() {
        return hasNext() && steps.get(cursor).delayStop();
    }

    /** Выполняет текущий шаг и все следующие без задержки того же типа. Возвращает задержку до следующего. */
    int runStep() {
        if (!hasNext()) return 0;

        Step step = steps.get(cursor++);
        execute(step);
        while (hasNext() && steps.get(cursor).delay() == 0 && steps.get(cursor).use() == step.use()) {
            step = steps.get(cursor++);
            execute(step);
        }
        return hasNext() ? steps.get(cursor).delay() : 0;
    }

    private void execute(Step step) {
        step.action().run();
        if (step.close() && mc.player != null) {
            mc.player.connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId));
        }
    }

    private record Step(Runnable action, int delay, int priority, boolean close, boolean use, boolean delayStop) {
    }
}
