package tokyo.ghoul.api.event.interfaces;

import tokyo.ghoul.api.event.EventListener;

public interface Subscribable<L, T> {
    EventListener subscribe(L listener);
    void unsubscribe(L listener);
}
