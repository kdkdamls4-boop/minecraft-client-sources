package tokyo.ghoul.api.event.events.other;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class RotationUpdateEvent extends Event<RotationUpdateEvent> {
    @Getter private static final RotationUpdateEvent instance = new RotationUpdateEvent();
}
