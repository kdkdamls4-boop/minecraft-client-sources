package tokyo.ghoul.api.event.events.client;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class ResourceReloadEvent extends Event<ResourceReloadEvent> {
    @Getter private static final ResourceReloadEvent instance = new ResourceReloadEvent();
}
