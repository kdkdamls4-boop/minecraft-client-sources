package tokyo.ghoul.api.event.events.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.event.events.Event;

public class MouseLookEvent extends Event<MouseLookEvent.MouseLookEventData> {
    @Getter private static final MouseLookEvent instance = new MouseLookEvent();

    @Getter
    @Setter
    @AllArgsConstructor
    public static class MouseLookEventData {
        private double deltaX;
        private double deltaY;
    }
}
