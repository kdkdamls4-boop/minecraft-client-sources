package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.move.FireworkRocketEvent;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

import java.awt.Color;

@ModuleRegister(name = "Elytra Booster", description = "Ускорение полёта на элитрах", category = Category.MOVEMENT)
public class ElytraBoosterModule extends Module {
    @Getter private static final ElytraBoosterModule instance = new ElytraBoosterModule();

    public final ModeSetting mode = new ModeSetting("Mode").value("Moderate").values("Moderate", "Aggressive");
    private final BooleanSetting debug = new BooleanSetting("Debug").value(false);

    private final SliderSetting[] yawSettings = new SliderSetting[9];
    private final SliderSetting[] pitchSettings = new SliderSetting[9];
    private final SliderSetting[] aggYawSettings = new SliderSetting[9];
    private final SliderSetting[] aggPitchSettings = new SliderSetting[18];

    private long lastUseFireworkMs = System.currentTimeMillis();

    public ElytraBoosterModule() {
        String[] ranges5 = {"0 - 5", "5 - 10", "10 - 15", "15 - 20", "20 - 25", "25 - 30", "30 - 35", "35 - 40", "40 - 45"};
        for (int i = 0; i < 9; i++) {
            yawSettings[i] = new SliderSetting("yaw " + ranges5[i]).value(1.5f).range(1.5f, 2.5f).step(0.01f)
                    .setVisible(() -> mode.is("Moderate"));
            pitchSettings[i] = new SliderSetting("pitch " + ranges5[i]).value(1.5f).range(1.5f, 2.5f).step(0.01f)
                    .setVisible(() -> mode.is("Moderate"));
        }
        for (int i = 0; i < 45; i += 5) {
            aggYawSettings[i / 5] = new SliderSetting("AYaw " + i + "-" + (i + 5)).value(1.5f).range(1.5f, 2.3f).step(0.01f)
                    .setVisible(() -> mode.is("Aggressive"));
        }
        for (int i = 0; i < 90; i += 5) {
            aggPitchSettings[i / 5] = new SliderSetting("APitch " + i + "-" + (i + 5)).value(1.5f).range(1.5f, 2.3f).step(0.01f)
                    .setVisible(() -> mode.is("Aggressive"));
        }

        addSettings(mode, debug);
        addSettings(yawSettings);
        addSettings(pitchSettings);
        addSettings(aggYawSettings);
        addSettings(aggPitchSettings);
    }

    @Override
    public void onEnable() {
        lastUseFireworkMs = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    public void onEvent() {
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isSend() || !(event.packet() instanceof ServerboundUseItemPacket) || mc.player == null) return;
            boolean holdingFirework = mc.player.getMainHandItem().is(Items.FIREWORK_ROCKET)
                    || mc.player.getOffhandItem().is(Items.FIREWORK_ROCKET);
            if (holdingFirework) lastUseFireworkMs = System.currentTimeMillis();
        }));
        EventListener keys = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!isEnabled() || mc.player == null || event.mouse()) return;
            if (event.action() != GLFW.GLFW_PRESS) return;
            if (mc.gui.screen() != null) return;
            SliderSetting[] activeYaw = mode.is("Moderate") ? yawSettings : aggYawSettings;
            SliderSetting[] activePitch = mode.is("Moderate") ? pitchSettings : aggPitchSettings;
            int key = event.key();
            if (key == GLFW.GLFW_KEY_LEFT_BRACKET) change(activeYaw[currentYawIndex()], -0.1f);
            else if (key == GLFW.GLFW_KEY_RIGHT_BRACKET) change(activeYaw[currentYawIndex()], 0.1f);
            else if (key == GLFW.GLFW_KEY_SEMICOLON) change(activePitch[currentPitchIndex()], -0.1f);
            else if (key == GLFW.GLFW_KEY_APOSTROPHE) change(activePitch[currentPitchIndex()], 0.1f);
        }));
        EventListener firework = FireworkRocketEvent.getInstance().subscribe(new Listener<>(this::onFirework));
        EventListener render = Render2DEvent.getInstance().subscribe(new Listener<>(this::onRender));
        addEvents(packet, keys, firework, render);
    }

    private void onFirework(FireworkRocketEvent event) {
        if (mc.player == null) return;

        AuraModule aura = AuraModule.getInstance();
        LivingEntity target = aura.isEnabled() ? aura.target : null;

        if (mode.is("Moderate")) {
            float yaw = convertYawToRange(Mth.wrapDegrees(mc.player.getYRot()));
            float pitch = convertYawToRange(Math.abs(mc.player.getXRot()));

            float speed = getSpeedForYaw(yaw);
            float ySpeed = getSpeedForPitch(pitch);
            if (ySpeed > speed) speed = ySpeed;

            if (aura.isEnabled() && target != null && !target.isFallFlying() && !aura.blockPos) {
                speed = 1.52f;
                ySpeed = 1.52f;
                if (mc.player.getXRot() > 89.0f && mc.player.getXRot() < 90.0f) {
                    ySpeed = 2.3f;
                }
                event.setCancel(true);
            }

            if (aura.blockPos && aura.isAttacking) {
                event.setSpeed(-speed);
                event.setYSpeed(-ySpeed);
                aura.isAttacking = false;
            }

            event.setSpeed(speed);
            event.setYSpeed(ySpeed);
            lastUseFireworkMs = System.currentTimeMillis();
            return;
        }

        float yaw = convertYawToRange(Mth.wrapDegrees(mc.player.getYRot()));
        float pitch = Math.abs(mc.player.getXRot());

        float speed = getAggSpeedForYaw(yaw);
        float ySpeed = getAggSpeedForPitch(pitch);
        if (ySpeed > speed) speed = ySpeed;

        if (aura.isEnabled() && target != null && !target.isFallFlying() && !aura.blockPos) {
            speed = 1.52f;
            ySpeed = 1.52f;
            if (mc.player.getXRot() > 89.0f && mc.player.getXRot() < 90.0f) {
                ySpeed = 2.3f;
            }
            event.setCancel(true);
        }

        if (aura.blockPos && aura.isAttacking) {
            event.setSpeed(-speed);
            event.setYSpeed(-ySpeed);
            aura.isAttacking = false;
        }

        event.setSpeed(speed);
        event.setYSpeed(ySpeed);
        lastUseFireworkMs = System.currentTimeMillis();
    }

    private void onRender(Render2DEvent.Render2DEventData event) {
        if (!debug.getValue() || mc.player == null) return;
        float centerX = mc.getWindow().getGuiScaledWidth() / 2f;
        float centerY = mc.getWindow().getGuiScaledHeight() / 2f + 16;
        float speed;
        String activeYawBand;
        String activePitchBand;
        if (mode.is("Moderate")) {
            float yaw = convertYawToRange(Mth.wrapDegrees(mc.player.getYRot()));
            float pitch = convertYawToRange(Math.abs(mc.player.getXRot()));
            speed = getSpeedForYaw(yaw);
            float ySpeed = getSpeedForPitch(pitch);
            if (ySpeed > speed) speed = ySpeed;
            int yawIdx = (int) (yaw / 5.0f);
            int pitchIdx = (int) (pitch / 5.0f);
            if (yawIdx >= 9) yawIdx = 8;
            if (pitchIdx >= 9) pitchIdx = 8;
            if (yawIdx < 0) yawIdx = 0;
            if (pitchIdx < 0) pitchIdx = 0;
            activeYawBand = (yawIdx * 5) + " - " + ((yawIdx + 1) * 5);
            activePitchBand = (pitchIdx * 5) + " - " + ((pitchIdx + 1) * 5);
        } else {
            float yaw = convertYawToRange(Mth.wrapDegrees(mc.player.getYRot()));
            float pitch = Math.abs(mc.player.getXRot());
            speed = getAggSpeedForYaw(yaw);
            float ySpeed = getAggSpeedForPitch(pitch);
            if (ySpeed > speed) speed = ySpeed;
            int yawIdx = (int) (yaw / 5.0f);
            int pitchIdx = (int) (pitch / 5.0f);
            if (yawIdx >= 9) yawIdx = 8;
            if (pitchIdx >= 18) pitchIdx = 17;
            if (yawIdx < 0) yawIdx = 0;
            if (pitchIdx < 0) pitchIdx = 0;
            activeYawBand = (yawIdx * 5) + " - " + ((yawIdx + 1) * 5);
            activePitchBand = (pitchIdx * 5) + " - " + ((pitchIdx + 1) * 5);
        }
        Fonts.SF_SEMIBOLD.drawCenteredText(event.matrixStack(), "Speed: " + String.format("%.2f", speed).replace(",", "."), centerX, centerY, 8.75F, Color.WHITE);
        Fonts.SF_SEMIBOLD.drawCenteredText(event.matrixStack(), "Yaw: " + activeYawBand, centerX, centerY + 10, 8.75F, Color.WHITE);
        Fonts.SF_SEMIBOLD.drawCenteredText(event.matrixStack(), "Pitch: " + activePitchBand, centerX, centerY + 20, 8.75F, Color.WHITE);
    }

    private void change(SliderSetting setting, float delta) {
        setting.setValue(setting.getValue() + delta);
    }

    private float getSpeedForYaw(float angle) {
        int index = (int) (angle / 5.0f);
        if (index >= 9) index = 8;
        if (index < 0) index = 0;
        return yawSettings[index].getValue();
    }

    private float getSpeedForPitch(float angle) {
        int index = (int) (angle / 5.0f);
        if (index >= 9) index = 8;
        if (index < 0) index = 0;
        return pitchSettings[index].getValue();
    }

    private float getAggSpeedForYaw(float angle) {
        int index = (int) (angle / 5.0f);
        if (index >= 9) index = 8;
        if (index < 0) index = 0;
        return aggYawSettings[index].getValue();
    }

    private float getAggSpeedForPitch(float angle) {
        int index = (int) (angle / 5.0f);
        if (index >= 18) index = 17;
        if (index < 0) index = 0;
        return aggPitchSettings[index].getValue();
    }

    private int currentYawIndex() {
        float yaw = convertYawToRange(Mth.wrapDegrees(mc.player.getYRot()));
        int index = (int) (yaw / 5.0f);
        if (index >= 9) index = 8;
        if (index < 0) index = 0;
        return index;
    }

    private int currentPitchIndex() {
        if (mode.is("Aggressive")) {
            float pitch = Math.abs(mc.player.getXRot());
            int index = (int) (pitch / 5.0f);
            if (index >= 18) index = 17;
            if (index < 0) index = 0;
            return index;
        }
        float pitch = convertYawToRange(Math.abs(mc.player.getXRot()));
        int index = (int) (pitch / 5.0f);
        if (index >= 9) index = 8;
        if (index < 0) index = 0;
        return index;
    }

    private float convertYawToRange(float angle) {
        float value = Math.abs(angle);
        if (value > 90.0f) value = 180.0f - value;
        if (value > 45.0f) value = 90.0f - value;
        return value;
    }

    public enum Type { DEFAULT, LIMIT }

    public static Vec3 predictElytra(LivingEntity entity, float minusTicks, Type type) {
        if (!(entity instanceof Player player)) return entity.position();
        Vec3 basePos = player.position();
        Vec3 velocity = new Vec3(player.getX() - player.xo, player.getY() - player.yo, player.getZ() - player.zo);
        float ticks = 0;
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            ticks = (float) (net.minecraft.client.Minecraft.getInstance().player.getEyePosition().distanceTo(entity.getBoundingBox().getCenter()) * minusTicks);
        }
        ticks += 3;
        if (type == Type.LIMIT) ticks = Math.clamp(ticks, 0, 2.7F);

        float deltaYaw = Mth.wrapDegrees(player.getYRot() - player.yRotO);
        float deltaPitch = Mth.wrapDegrees(player.getXRot() - player.xRotO);
        Vec3 predictedPos = basePos;
        int intTicks = (int) ticks;
        float frac = ticks - intTicks;
        for (int i = 0; i < intTicks; i++) {
            double speed = velocity.length();
            if (speed > 0.001) {
                double velYaw = Math.atan2(-velocity.x, velocity.z);
                double velPitch = -Math.atan2(velocity.y, Math.hypot(velocity.x, velocity.z));
                velYaw += Math.toRadians(deltaYaw);
                velPitch += Math.toRadians(deltaPitch);
                velPitch = Mth.clamp(velPitch, -Math.PI / 2.0, Math.PI / 2.0);
                velocity = new Vec3(
                        -Math.sin(velYaw) * Math.cos(velPitch) * speed,
                        -Math.sin(velPitch) * speed,
                        Math.cos(velYaw) * Math.cos(velPitch) * speed
                );
            }
            predictedPos = predictedPos.add(velocity);
        }
        if (frac > 0) {
            double speed = velocity.length();
            if (speed > 0.001) {
                double velYaw = Math.atan2(-velocity.x, velocity.z);
                double velPitch = -Math.atan2(velocity.y, Math.hypot(velocity.x, velocity.z));
                velYaw += Math.toRadians(deltaYaw * frac);
                velPitch += Math.toRadians(deltaPitch * frac);
                velPitch = Mth.clamp(velPitch, -Math.PI / 2.0, Math.PI / 2.0);
                velocity = new Vec3(
                        -Math.sin(velYaw) * Math.cos(velPitch) * speed,
                        -Math.sin(velPitch) * speed,
                        Math.cos(velYaw) * Math.cos(velPitch) * speed
                );
            }
            predictedPos = predictedPos.add(velocity.scale(frac));
        }
        return predictedPos;
    }

    public static Vec3 realPredict(LivingEntity entity) {
        Vec3 predict = predictElytra(entity, 0, Type.DEFAULT);
        float minusTicks = 0;
        if (net.minecraft.client.Minecraft.getInstance().player != null) {
            minusTicks = Math.clamp((float) net.minecraft.client.Minecraft.getInstance().player.getEyePosition().distanceTo(predict) / 100, 0, 12);
        }
        minusTicks = Math.max(minusTicks, 0);
        return predictElytra(entity, minusTicks, Type.DEFAULT);
    }
}
