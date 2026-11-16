package tokyo.ghoul.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.events.Event;

public class EntityVelocityEvent extends Event<EntityVelocityEvent.EntityVelocityEventData> {
    @Getter private static final EntityVelocityEvent instance = new EntityVelocityEvent();

    @Getter
    @AllArgsConstructor
    public static class EntityVelocityEventData {
        @Setter private Vec3 velocity;
    }
}
