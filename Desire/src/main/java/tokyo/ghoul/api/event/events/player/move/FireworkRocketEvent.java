package tokyo.ghoul.api.event.events.player.move;

import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.event.events.Event;

@Getter
@Setter
public class FireworkRocketEvent extends Event<FireworkRocketEvent> {
    @Getter private static final FireworkRocketEvent instance = new FireworkRocketEvent();

    private float speed;
    private float ySpeed;
}
