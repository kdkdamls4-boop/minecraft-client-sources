package tokyo.ghoul.client.features.modules.player;

import lombok.Getter;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.world.PushEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;

@Getter
@ModuleRegister(name = "No Push", description = "Убирает отталкивание", category = Category.PLAYER)
public class NoPushModule extends Module {
    @Getter private static final NoPushModule instance = new NoPushModule();

    private final MultiBooleanSetting objects = new MultiBooleanSetting("Objects").value(
            new BooleanSetting("Blocks").value(true),
            new BooleanSetting("Players").value(true)
    );

    public NoPushModule() {
        addSettings(objects);
    }

    @Override
    public void onEvent() {
        EventListener pushEvent = PushEvent.getInstance().subscribe(new Listener<>(event -> {
            PushEvent.PushEventData.PushingSource source = event.source();

            if ((source == PushEvent.PushEventData.PushingSource.BLOCK && objects.isEnabled("Blocks"))
                    || (source == PushEvent.PushEventData.PushingSource.ENTITY && objects.isEnabled("Players"))) {
                PushEvent.getInstance().setCancel(true);
            }
        }));

        addEvents(pushEvent);
    }
}
