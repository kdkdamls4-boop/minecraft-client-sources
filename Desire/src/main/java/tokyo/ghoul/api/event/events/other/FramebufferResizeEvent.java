package tokyo.ghoul.api.event.events.other;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class FramebufferResizeEvent extends Event<FramebufferResizeEvent> {
    @Getter private static final FramebufferResizeEvent instance = new FramebufferResizeEvent();
}
