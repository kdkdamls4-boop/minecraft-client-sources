package tokyo.ghoul.api.utils.other;

import tokyo.ghoul.api.utils.math.TimerUtil;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/** Цепочка действий с задержками в мс: каждое следующее ждёт свою задержку после предыдущего. */
public class ActionSequence {
    private final TimerUtil timer = new TimerUtil();
    private final List<DelayedAction> actions = new CopyOnWriteArrayList<>();
    private int index;
    private boolean paused;

    public ActionSequence addDelayed(int delay, Runnable action) {
        return addDelayed(delay, action, () -> true);
    }

    public ActionSequence addDelayed(int delay, Runnable action, BooleanSupplier condition) {
        actions.add(new DelayedAction(delay, action, condition));
        return this;
    }

    public ActionSequence clear() {
        actions.clear();
        timer.reset();
        index = 0;
        paused = false;
        return this;
    }

    public void tick() {
        if (actions.isEmpty() || paused || index >= actions.size()) return;

        DelayedAction action = actions.get(index);
        if (action.condition().getAsBoolean() && timer.finished((long) action.delay())) {
            action.action().run();
            index++;
            timer.reset();
        }
    }

    public boolean isFinished() {
        return index >= actions.size() && !paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    private record DelayedAction(int delay, Runnable action, BooleanSupplier condition) {
    }
}
