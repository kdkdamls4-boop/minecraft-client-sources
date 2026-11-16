package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.other.NetworkUtil;

@ModuleRegister(name = "No Web", description = "Убирает замедление в паутине", category = Category.MOVEMENT)
public class NoWebModule extends Module {
    @Getter private static final NoWebModule instance = new NoWebModule();

    private final ModeSetting mode = new ModeSetting("Мод").values("ReallyWorld", "Break", "Custom").value("ReallyWorld");

    private final SliderSetting speed = new SliderSetting("Скорость").value(0.21F).range(0.1F, 1.0F).step(0.01F)
            .setVisible(() -> mode.is("Custom"));
    private final SliderSetting speedY = new SliderSetting("Скорость по Y").value(0.9F).range(0.1F, 2.0F).step(0.01F)
            .setVisible(() -> mode.is("Custom"));

    private static final double RW_SPEED_Y = 0.9F;
    private static final double RW_SPEED = 0.21F;

    @Getter @Setter private static boolean inWeb;

    public NoWebModule() {
        addSettings(mode, speed, speedY);
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(update);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null || !inWeb) return;

        if (mode.is("ReallyWorld")) {
            applyMotion(RW_SPEED_Y, RW_SPEED);
        } else if (mode.is("Custom")) {
            applyMotion(speedY.getValue(), speed.getValue());
        }
    }

    private void applyMotion(double vertical, double horizontal) {
        var motion = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(motion.x, 0.0, motion.z);
        if (mc.options.keyJump.isDown()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, vertical, mc.player.getDeltaMovement().z);
        }
        if (mc.options.keyShift.isDown()) {
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().x, -vertical, mc.player.getDeltaMovement().z);
        }
        setSpeed(horizontal);
    }

    public static boolean onWebInside(BlockPos pos) {
        NoWebModule module = instance;
        if (!module.isEnabled() || !module.mode.is("Break") || module.mc.player == null) return false;

        NetworkUtil.sendPacket(sequence -> new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP, sequence));
        return true;
    }

    private void setSpeed(double value) {
        float forward = mc.player.input.getMoveVector().y;
        float strafe = mc.player.input.getMoveVector().x;
        float yaw = mc.player.getYRot();
        if (forward == 0.0F && strafe == 0.0F) {
            mc.player.setDeltaMovement(0.0, mc.player.getDeltaMovement().y, 0.0);
            return;
        }

        if (forward != 0.0F) {
            if (strafe > 0.0F) {
                yaw += forward > 0.0F ? -45.0F : 45.0F;
            } else if (strafe < 0.0F) {
                yaw += forward > 0.0F ? 45.0F : -45.0F;
            }
            strafe = 0.0F;
            forward = forward > 0.0F ? 1.0F : -1.0F;
        }

        double rad = Math.toRadians(yaw + 90.0F);
        double x = forward * value * Math.cos(rad) + strafe * value * Math.sin(rad);
        double z = forward * value * Math.sin(rad) - strafe * value * Math.cos(rad);
        mc.player.setDeltaMovement(x, mc.player.getDeltaMovement().y, z);
    }
}
