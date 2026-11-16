package tokyo.ghoul.api.event.events.player.move;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.events.Event;

public class TravelPostEvent extends Event<TravelPostEvent.TravelPostEventData> {
    @Getter private static final TravelPostEvent instance = new TravelPostEvent();

    @Getter
    @Setter
    @AllArgsConstructor
    public static class TravelPostEventData {
        private Vec3 velocity;
    }
}
