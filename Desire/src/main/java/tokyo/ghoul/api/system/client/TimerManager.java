package tokyo.ghoul.api.system.client;

import lombok.Getter;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.utils.task.TaskPriority;
import tokyo.ghoul.api.utils.task.TaskProcessor;

@Getter
public class TimerManager {
    @Getter private static final TimerManager instance = new TimerManager();

    private final TaskProcessor<Float> taskProcessor = new TaskProcessor<>();
    private float speed = 1.0f;

    public float getTimerSpeed() {
        Float task = taskProcessor.fetchActiveTaskValue();
        return task != null ? task : speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public void resetSpeed() {
        this.speed = 1.0f;
    }

    public void addTimer(float timer, TaskPriority taskPriority, Module provider, int ticks) {
        taskProcessor.addTask(new TaskProcessor.Task<>(ticks, taskPriority.getPriority(), provider, timer));
    }
}
