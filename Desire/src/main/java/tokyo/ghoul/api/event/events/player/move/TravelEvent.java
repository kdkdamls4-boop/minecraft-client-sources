package tokyo.ghoul.api.event.events.player.move;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class TravelEvent extends Event<TravelEvent> {
    @Getter private static final TravelEvent instance = new TravelEvent();
}
