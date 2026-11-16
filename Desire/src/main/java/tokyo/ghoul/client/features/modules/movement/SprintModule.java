package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;

@ModuleRegister(name = "Sprint", description = "Автоматический бег", category = Category.MOVEMENT)
public class SprintModule extends Module {
    @Getter private static final SprintModule instance = new SprintModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Legit").values("Legit", "Packet", "None");

    public SprintModule() {
        addSettings(mode);
        setEnabled(true);
    }

    @Override
    public void onEvent() {
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(1, event -> {
            if (event.getDirectionalInput().isForwards()
                    && !event.getDirectionalInput().isBackwards()) {
                event.setSprint(true);
            }
        }));

        addEvents(sprintEvent);
    }

}
