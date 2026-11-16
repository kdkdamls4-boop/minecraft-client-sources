package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.inject.accessor.MinecraftAccessor;

@ModuleRegister(name = "NoDelays", description = "Убирает задержки действий", category = Category.PLAYER)
public class NoDelaysModule extends Module {
    @Getter private static final NoDelaysModule instance = new NoDelaysModule();

    private static final int DEFAULT_JUMP_DELAY = 10;
    private static final int DEFAULT_RIGHT_CLICK_DELAY = 4;
    private static final int DEFAULT_BLOCK_BREAK_DELAY = 5;

    private final BooleanSetting jump = new BooleanSetting("Jump").value(true);
    private final BooleanSetting rightClick = new BooleanSetting("Right Click").value(false);
    private final BooleanSetting experienceBottlesOnly = new BooleanSetting("Experience Bottles Only").value(false)
            .setVisible(rightClick::getValue);
    private final BooleanSetting blockBreak = new BooleanSetting("Block Break").value(false);

    public NoDelaysModule() {
        addSettings(jump, rightClick, experienceBottlesOnly, blockBreak);
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            if (jump.getValue()) {
                mc.player.noJumpDelay = 0;
            }
            if (rightClick.getValue()
                    && (!experienceBottlesOnly.getValue()
                    || mc.player.getMainHandItem().is(Items.EXPERIENCE_BOTTLE)
                    || mc.player.getOffhandItem().is(Items.EXPERIENCE_BOTTLE))) {
                ((MinecraftAccessor) (Object) mc).setRightClickDelay(0);
            }
            if (blockBreak.getValue() && mc.gameMode != null) {
                mc.gameMode.destroyDelay = 0;
            }
        }));
        addEvents(update);
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.noJumpDelay = DEFAULT_JUMP_DELAY;
        }
        ((MinecraftAccessor) (Object) mc).setRightClickDelay(DEFAULT_RIGHT_CLICK_DELAY);
        if (mc.gameMode != null) {
            mc.gameMode.destroyDelay = DEFAULT_BLOCK_BREAK_DELAY;
        }
        super.onDisable();
    }
}
