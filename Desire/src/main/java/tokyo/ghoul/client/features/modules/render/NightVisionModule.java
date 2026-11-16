package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;

@ModuleRegister(name = "Night Vision", description = "Ночное зрение", category = Category.RENDER)
public class NightVisionModule extends Module {
    @Getter private static final NightVisionModule instance = new NightVisionModule();

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            add();
        }));

        addEvents(tickEvent);
    }

    private void remove() {
        if (mc.player == null) return;
        mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }

    private void add() {
        if (mc.player == null) return;
        mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
    }
}
