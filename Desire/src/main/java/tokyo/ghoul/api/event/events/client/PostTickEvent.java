package tokyo.ghoul.api.event.events.client;

import lombok.Getter;
import tokyo.ghoul.api.event.events.Event;

public class PostTickEvent extends Event<PostTickEvent> {
    @Getter private static final PostTickEvent instance = new PostTickEvent();
}
