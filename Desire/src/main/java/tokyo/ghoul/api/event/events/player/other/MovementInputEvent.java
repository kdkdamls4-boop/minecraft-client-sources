package tokyo.ghoul.api.event.events.player.other;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Input;
import tokyo.ghoul.api.event.events.Event;
import tokyo.ghoul.api.utils.player.DirectionalInput;

public class MovementInputEvent extends Event<MovementInputEvent.MovementInputEventData> {
    @Getter private static final MovementInputEvent instance = new MovementInputEvent();

    @Getter
    @AllArgsConstructor
    public static class MovementInputEventData {
        private final Input playerInput;

        @Setter
        private boolean jump, sneak;

        @Setter
        private DirectionalInput directionalInput;
    }
}
