package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.GameLoopEvent;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.move.MoveEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.math.TimerUtil;
import tokyo.ghoul.api.utils.other.NetworkUtil;

import java.util.concurrent.CopyOnWriteArrayList;

@ModuleRegister(name = "Air Stuck", description = "Зависание игрока в воздухе", category = Category.MOVEMENT)
public class AirStuckModule extends Module {
    @Getter private static final AirStuckModule instance = new AirStuckModule();

    private static final String MODE_DEFAULT = "Обычный";
    private static final String MODE_FUNTIME = "FunTime";
    private static final String MODE_SPOOKYTIME = "SpookyTime";

    private final ModeSetting mode = new ModeSetting("Режим").value(MODE_DEFAULT).values(MODE_DEFAULT, MODE_FUNTIME, MODE_SPOOKYTIME);
    private final BooleanSetting alternativeMode = new BooleanSetting("Альтернативный режим").value(false)
            .setVisible(() -> mode.is(MODE_DEFAULT));
    private final BooleanSetting waitMoment = new BooleanSetting("Ждать момент").value(false);
    private final BooleanSetting autoDisable = new BooleanSetting("Авто выключение").value(false)
            .setVisible(() -> !mode.is(MODE_SPOOKYTIME));
    private final SliderSetting autoDisableTime = new SliderSetting("Время до выключения (сек)").value(5.0F).range(1.0F, 30.0F).step(1.0F)
            .setVisible(() -> !mode.is(MODE_SPOOKYTIME) && autoDisable.getValue());

    private final TimerUtil autoDisableTimer = new TimerUtil();
    private final TimerUtil spookyTimer = new TimerUtil();
    private final CopyOnWriteArrayList<Packet<?>> bufferedPackets = new CopyOnWriteArrayList<>();
    private long lagUntil;
    private Vec3 savedVelocity = Vec3.ZERO;
    private Vec3 savedPosition = Vec3.ZERO;
    @Getter private float savedYaw;
    @Getter private float savedPitch;
    @Getter private float savedBodyYaw;
    @Getter private boolean stateSaved;
    private boolean releasing;

    public AirStuckModule() {
        addSettings(mode, alternativeMode, waitMoment, autoDisable, autoDisableTime);
    }

    /** Для ElytraMotion: игрок заморожен модулем. */
    public boolean isFrozen() {
        return isEnabled();
    }

    @Override
    public void onEvent() {
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        EventListener update = GameLoopEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        EventListener move = MoveEvent.getInstance().subscribe(new Listener<>(this::onMove));
        addEvents(packet, update, move);
    }

    private void onWorldChange() {
        lagUntil = System.currentTimeMillis() + 67L;
        flushPackets();
        toggle();
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (event.isReceive() && (event.packet() instanceof ClientboundLoginPacket || event.packet() instanceof ClientboundRespawnPacket)) {
            mc.execute(() -> {
                if (isEnabled()) onWorldChange();
            });
            return;
        }

        if (mc.player == null || mc.level == null) {
            if (isEnabled()) {
                mc.execute(this::toggle);
            }
            return;
        }

        if (waitMoment.getValue() && mc.player.fallDistance < 0.1) return;
        if (mode.is(MODE_SPOOKYTIME) && releasing) return;

        if (mode.is(MODE_FUNTIME)) {
            if (event.isSend()) {
                bufferedPackets.add(event.packet());
                PacketEvent.getInstance().setCancel(true);
            }
        } else if (event.packet() instanceof ServerboundMovePlayerPacket) {
            PacketEvent.getInstance().setCancel(true);
        }
    }

    private void onUpdate() {
        if (System.currentTimeMillis() < lagUntil) {
            setEnabled(false);
        }

        if (autoDisable.getValue() && !mode.is(MODE_SPOOKYTIME) && autoDisableTimer.getElapsedTime() > (long) (autoDisableTime.getValue() * 1000.0F)) {
            setEnabled(false);
        }

        if (!mode.is(MODE_SPOOKYTIME)) return;

        if (!releasing) {
            if (spookyTimer.getElapsedTime() > 2500L) {
                flushPackets();
                stateSaved = false;
                if (savedPosition != null && mc.player != null) {
                    mc.player.setOnGroundWithMovement(mc.player.onGround(), savedPosition);
                }
                if (savedVelocity != null && mc.player != null) {
                    mc.player.setDeltaMovement(savedVelocity);
                }
                releasing = true;
                spookyTimer.reset();
            }
        } else if (spookyTimer.getElapsedTime() > 150L) {
            releasing = false;
            spookyTimer.reset();
        }
    }

    private void onMove(MoveEvent.MoveEventData event) {
        if (mc.player == null) return;
        if (waitMoment.getValue() && mc.player.fallDistance < 0.1) return;
        if (mode.is(MODE_SPOOKYTIME) && releasing) return;

        saveState();
        event.set(Vec3.ZERO);
        MoveEvent.getInstance().setCancel(true);
        if (mode.is(MODE_DEFAULT) && alternativeMode.getValue() && mc.player.tickCount % 10 == 0) {
            NetworkUtil.sendSilentPacket(new ServerboundMovePlayerPacket.StatusOnly(mc.player.onGround(), mc.player.horizontalCollision));
        }
        mc.player.setDeltaMovement(Vec3.ZERO);
    }

    private void saveState() {
        if (stateSaved) return;

        stateSaved = true;
        savedPosition = mc.player.getKnownMovement();
        mc.player.setOnGroundWithMovement(mc.player.onGround(), Vec3.ZERO);
        Vec3 velocity = mc.player.getDeltaMovement();
        savedVelocity = new Vec3(velocity.x, velocity.y, velocity.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
        savedYaw = mc.player.getYRot();
        savedPitch = mc.player.getXRot();
        savedBodyYaw = mc.player.yBodyRot;
    }

    private void flushPackets() {
        if (mc.getConnection() != null) {
            for (Packet<?> packet : bufferedPackets) {
                NetworkUtil.sendSilentPacket(packet);
            }
        }
        bufferedPackets.clear();
    }

    @Override
    public void onDisable() {
        stateSaved = false;
        releasing = false;
        bufferedPackets.clear();
        autoDisableTimer.reset();
        spookyTimer.reset();
        super.onDisable();
    }

    @Override
    public void onEnable() {
        stateSaved = false;
        releasing = false;
        flushPackets();
        if (savedPosition != null && mc.player != null) {
            mc.player.setOnGroundWithMovement(mc.player.onGround(), savedPosition);
        }
        if (savedVelocity != null && mc.player != null) {
            mc.player.setDeltaMovement(savedVelocity);
        }
        autoDisableTimer.reset();
        spookyTimer.reset();
        super.onEnable();
    }
}
