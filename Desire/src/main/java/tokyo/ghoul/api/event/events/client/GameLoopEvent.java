package tokyo.ghoul.api.event.events.client;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class GameLoopEvent extends Event<GameLoopEvent> {
    @Getter private static final GameLoopEvent instance = new GameLoopEvent();
}
