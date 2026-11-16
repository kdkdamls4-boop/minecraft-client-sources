package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.world.InteractionHand;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;

@Getter
@ModuleRegister(name = "No Slow", description = "Убирает замедление при использовании предметов", category = Category.MOVEMENT)
public class NoSlowModule extends Module {
    @Getter private static final NoSlowModule instance = new NoSlowModule();

    private final ModeSetting mode = new ModeSetting("Mode").value("HVH").values("HVH", "HolyWorld");
    private final BooleanSetting sprint = new BooleanSetting("Sprint").value(true)
            .setVisible(() -> mode.is("HVH"));

    public NoSlowModule() {
        addSettings(mode, sprint);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        addEvents(updateEvent);
    }

    private void onUpdate() {
        if (mc.player == null || !mc.player.isAlive()) return;

        if (mode.is("HVH") && mc.player.isUsingItem()) {
            InteractionHand hand = mc.player.getUsedItemHand();
            InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

            if (sprint.getValue()) {
                mc.player.setSprinting(canKeepSprint());
            }
            mc.gameMode.useItem(mc.player, other);
        }
    }

    private boolean canKeepSprint() {
        return mc.player.canSprint()
                && isWalking()
                && !mc.player.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                && !mc.player.isFallFlying()
                && (!mc.player.isShiftKeyDown() || mc.player.isInWater());
    }

    private boolean isWalking() {
        return mc.player.input != null && mc.player.input.hasForwardImpulse();
    }


    public float useSpeedMultiplier(float original) {
        if (!isEnabled()) return original;

        if (mode.is("HVH")) return 1.0f;
        if (mode.is("HolyWorld")) return mc.player != null && mc.player.getTicksUsingItem() % 2 == 0 ? 1.0f : original;
        return original;
    }
}
