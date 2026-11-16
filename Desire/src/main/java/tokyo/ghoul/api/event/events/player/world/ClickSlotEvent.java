package tokyo.ghoul.api.event.events.player.world;

import lombok.Getter;
import net.minecraft.world.inventory.ContainerInput;
import tokyo.ghoul.api.event.events.Event;

public class ClickSlotEvent extends Event<ClickSlotEvent.ClickSlotEventData> {
    @Getter private static final ClickSlotEvent instance = new ClickSlotEvent();

    public record ClickSlotEventData(ContainerInput slotActionType, int slot, int button, int id) {
    }
}
