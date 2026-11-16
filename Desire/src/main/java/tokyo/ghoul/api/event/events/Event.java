package tokyo.ghoul.api.event.events;

import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.event.Flora;

@Getter
@Setter
public class Event<T> extends Flora<T> {
    private boolean cancel = false;

    private static volatile Thread mutedThread;

    public static void setMutedThread(Thread thread) {
        mutedThread = thread;
    }

    @SuppressWarnings("unchecked")
    protected T getSelf() {
        return (T) this;
    }

    public boolean call() {
        if (Thread.currentThread() == mutedThread) return false;
        cancel = false;
        notify(getSelf());
        return cancel;
    }

    public boolean call(T any) {
        if (Thread.currentThread() == mutedThread) return false;
        cancel = false;
        notify(any);
        return cancel;
    }
}
