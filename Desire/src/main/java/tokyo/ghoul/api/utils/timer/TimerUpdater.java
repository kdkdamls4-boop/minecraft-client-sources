package tokyo.ghoul.api.utils.timer;

import lombok.experimental.UtilityClass;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@UtilityClass
public class TimerUpdater implements QuickImports {
    private final List<TimeTask> timeTasks = new ArrayList<>();

    static {
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateTimeTasks(true);
        }));

        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            updateTimeTasks(false);
        }));
    }

    public void addTimeTask(TimeTask task) {
        synchronized (timeTasks) {
            timeTasks.add(task);
        }
    }

    public void updateTimeTasks(boolean ticksUpdate) {
        long now = System.currentTimeMillis();
        List<TimeTask> copy;
        synchronized (timeTasks) {
            if (timeTasks.isEmpty()) return;
            copy = new ArrayList<>(timeTasks);
        }

        for (TimeTask task : copy) {
            if (ticksUpdate) task.ticksExisted++;
            boolean shouldRun = task.mode == TimeTask.Mode.TICKS
                    ? task.ticksExisted >= task.time
                    : (now - task.time > task.duration);
            if (shouldRun && task.runnable != null) {
                task.runnable.run();
                task.runnable = null;
            }
        }

        synchronized (timeTasks) {
            Iterator<TimeTask> iterator = timeTasks.iterator();
            while (iterator.hasNext()) {
                TimeTask task = iterator.next();
                boolean finished = task.mode == TimeTask.Mode.TICKS
                        ? task.ticksExisted >= task.time
                        : (now - task.time > task.duration);
                if (finished || !task.condition) {
                    iterator.remove();
                }
            }
        }
    }

    public boolean timeTasksIsEmpty() {
        synchronized (timeTasks) {
            return timeTasks.isEmpty();
        }
    }

    public static class TimeTask {
        public long duration;
        public long time;
        public long ticksExisted;
        public Runnable runnable;
        public boolean condition;
        public Mode mode;

        public TimeTask(long duration, Runnable runnable, boolean condition) {
            this.mode = Mode.TIME;
            this.duration = duration;
            this.runnable = runnable;
            this.time = System.currentTimeMillis();
            this.condition = condition;
        }

        public TimeTask(Mode mode, int ticks, Runnable runnable, boolean condition) {
            this.mode = mode;
            this.runnable = runnable;
            this.time = ticks;
            this.condition = condition;
        }

        public enum Mode {
            TIME,
            TICKS
        }
    }
}
