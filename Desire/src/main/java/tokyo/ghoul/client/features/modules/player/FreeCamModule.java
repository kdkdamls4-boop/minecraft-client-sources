package tokyo.ghoul.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.CameraType;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.move.MoveEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.api.utils.player.MoveUtil;

@ModuleRegister(name = "Free Cam", description = "Свободная камера", category = Category.PLAYER)
public class FreeCamModule extends Module {
    @Getter private static final FreeCamModule instance = new FreeCamModule();

    private final SliderSetting speedSetting = new SliderSetting("Скорость").value(2.0f).range(0.5f, 5.0f).step(0.1f);
    private final BooleanSetting freezeSetting = new BooleanSetting("Заморозка").value(false);
    private final BooleanSetting reloadChunksSetting = new BooleanSetting("Reload Chunks").value(true);
    private final BooleanSetting toggleOnLogSetting = new BooleanSetting("Toggle On Log").value(true);

    public Vec3 pos = Vec3.ZERO;
    public Vec3 prevPos = Vec3.ZERO;

    public FreeCamModule() {
        addSettings(speedSetting, freezeSetting, reloadChunksSetting, toggleOnLogSetting);
    }

    public static Vec3 interpolatedPos(float partialTicks) {
        FreeCamModule module = instance;
        if (!module.isEnabled()) return null;
        return new Vec3(
                Mth.lerp(partialTicks, module.prevPos.x, module.pos.x),
                Mth.lerp(partialTicks, module.prevPos.y, module.pos.y),
                Mth.lerp(partialTicks, module.prevPos.z, module.pos.z)
        );
    }

    @Override
    public void onEnable() {
        if (mc.gameRenderer != null && mc.gameRenderer.mainCamera() != null) {
            prevPos = pos = mc.gameRenderer.mainCamera().position();
        } else if (mc.player != null) {
            prevPos = pos = mc.player.getEyePosition();
        }
        reloadChunks();
    }

    @Override
    public void onDisable() {
        reloadChunks();
    }

    private void reloadChunks() {
    }

    @Override
    public void onEvent() {
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        EventListener move = MoveEvent.getInstance().subscribe(new Listener<>(this::onMove));
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(this::onInput));
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.options != null) mc.options.setCameraType(CameraType.FIRST_PERSON);
        }));
        addEvents(packet, move, input, update);
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (freezeSetting.getValue() && event.isSend() && event.packet() instanceof ServerboundMovePlayerPacket) {
            PacketEvent.getInstance().setCancel(true);
        }
        if (event.isReceive() && (event.packet() instanceof ClientboundRespawnPacket
                || event.packet() instanceof ClientboundLoginPacket)
                && toggleOnLogSetting.getValue()) {
            setEnabled(false);
        }
    }

    private void onMove(MoveEvent.MoveEventData event) {
        if (!freezeSetting.getValue()) return;
        event.setX(0);
        event.setY(0);
        event.setZ(0);
        MoveEvent.getInstance().setCancel(true);
    }

    private void onInput(MovementInputEvent.MovementInputEventData event) {
        float speed = speedSetting.getValue();
        var input = event.getPlayerInput();
        float forward = (input.forward() ? 1f : 0f) - (input.backward() ? 1f : 0f);
        float strafe = (input.left() ? 1f : 0f) - (input.right() ? 1f : 0f);
        double[] motion = MoveUtil.forward(speed);
        if (forward == 0f && strafe == 0f) {
            motion = new double[]{0, 0};
        } else {
            double dir = MoveUtil.direction(mc.player.getYRot(), forward, strafe);
            motion = new double[]{-Math.sin(dir) * speed, Math.cos(dir) * speed};
        }
        double y = input.jump() ? speed : input.shift() ? -speed : 0;
        prevPos = pos;
        pos = pos.add(motion[0], y, motion[1]);
        event.setDirectionalInput(DirectionalInput.NONE);
        event.setJump(false);
        event.setSneak(false);
    }
}
