package tokyo.ghoul.api.event.interfaces;

import tokyo.ghoul.api.event.Listener;

public interface Cacheable<T> {
    Listener<T>[] getCache();
}