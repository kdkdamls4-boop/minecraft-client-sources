package tokyo.ghoul.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import tokyo.ghoul.api.event.events.Event;

public class AttackEvent extends Event<AttackEvent.AttackEventData> {
    @Getter private static final AttackEvent instance = new AttackEvent();

    public record AttackEventData(Entity entity) {}
}
