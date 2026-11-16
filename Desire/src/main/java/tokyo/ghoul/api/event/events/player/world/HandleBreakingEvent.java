package tokyo.ghoul.api.event.events.player.world;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class HandleBreakingEvent extends Event<HandleBreakingEvent> {
    @Getter private static final HandleBreakingEvent instance = new HandleBreakingEvent();
}
