package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

@ModuleRegister(name = "Swing Animation", description = "Анимация взмаха руки", category = Category.RENDER)
public class SwingAnimationModule extends Module {
    @Getter private static final SwingAnimationModule instance = new SwingAnimationModule();

    public final ModeSetting mode = new ModeSetting("Свинги").value("Мод 1").values(
            "Обычный", "Мод 1", "Мод 2", "Мод 3", "Мод 4", "Мод 5", "Мод 6", "Мод 7", "Мод 8",
            "Мод 9", "Мод 10", "Мод 11", "Мод 12", "Мод 13", "Мод 14", "Custom", "HMI");
    private final SliderSetting angle = new SliderSetting("Угол").value(100f).range(10f, 180f).step(1f)
            .setVisible(() -> mode.is("Мод 3") || mode.is("Мод 4"));
    private final SliderSetting itemScale = new SliderSetting("Размер предмета").value(1f).range(0.2f, 2f).step(0.05f)
            .setVisible(() -> !mode.is("HMI"));
    private final SliderSetting tiltPower = new SliderSetting("Сила наклона").value(5f).range(1f, 10f).step(1f)
            .setVisible(() -> !mode.is("HMI") && !mode.is("Custom"));
    private final BooleanSetting changeSpeed = new BooleanSetting("Менять скорость свинга").value(false)
            .setVisible(() -> !mode.is("HMI"));
    private final BooleanSetting cubicEase = new BooleanSetting("Кубическое ускорение").value(false)
            .setVisible(() -> !mode.is("HMI") && !mode.is("Custom"));
    private final SliderSetting speed = new SliderSetting("Скорость").value(7f).range(0f, 10f).step(1f)
            .setVisible(() -> !mode.is("HMI") && changeSpeed.getValue());
    public final BooleanSetting auraOnly = new BooleanSetting("Только с аурой").value(true);
    private final BooleanSetting onlyOnHit = new BooleanSetting("Только при ударе").value(false)
            .setVisible(() -> !mode.is("HMI"));
    private final ModeSetting useAnimation = new ModeSetting("Анимация использования").value("Обычная")
            .values("Обычная", "Укороченная", "Полная отмена");

    private final SliderSetting startX = custom("Начало X", 0f, -2f, 2f, 0.01f);
    private final SliderSetting startY = custom("Начало Y", 0.08f, -2f, 2f, 0.01f);
    private final SliderSetting startZ = custom("Начало Z", -0.05f, -2f, 2f, 0.01f);
    private final SliderSetting startPitch = custom("Начало Pitch", -65f, -180f, 180f, 1f);
    private final SliderSetting startYaw = custom("Начало Yaw", 24f, -180f, 180f, 1f);
    private final SliderSetting startRoll = custom("Начало Roll", -4f, -180f, 180f, 1f);
    private final SliderSetting endX = custom("Конец X", 0.04f, -2f, 2f, 0.01f);
    private final SliderSetting endY = custom("Конец Y", -0.92f, -2f, 2f, 0.01f);
    private final SliderSetting endZ = custom("Конец Z", -0.32f, -2f, 2f, 0.01f);
    private final SliderSetting endPitch = custom("Конец Pitch", -142f, -180f, 180f, 1f);
    private final SliderSetting endYaw = custom("Конец Yaw", 34f, -180f, 180f, 1f);
    private final SliderSetting endRoll = custom("Конец Roll", -16f, -180f, 180f, 1f);
    private final SliderSetting curve1X = custom("Кривая 1 X", 0.18f, 0f, 1f, 0.01f);
    private final SliderSetting curve1Y = custom("Кривая 1 Y", 0.02f, 0f, 1f, 0.01f);
    private final SliderSetting curve2X = custom("Кривая 2 X", 0.58f, 0f, 1f, 0.01f);
    private final SliderSetting curve2Y = custom("Кривая 2 Y", 1f, 0f, 1f, 0.01f);
    private final BooleanSetting customBack = new BooleanSetting("Возврат").value(true).setVisible(() -> mode.is("Custom"));
    private final SliderSetting customBackSpeed = new SliderSetting("Скорость возврата").value(8f).range(1f, 10f).step(1f)
            .setVisible(() -> mode.is("Custom") && customBack.getValue());

    public final BooleanSetting newShowLeftArm = new BooleanSetting("Левая рука").value(false).setVisible(() -> mode.is("HMI"));
    public final ModeSetting attackMode = new ModeSetting("Атака").value("Swings").values("Swings", "Forward", "Normal")
            .setVisible(() -> mode.is("HMI"));
    public final SliderSetting rightX = hmi("Правая X");
    public final SliderSetting rightY = hmi("Правая Y");
    public final SliderSetting rightZ = hmi("Правая Z");
    public final SliderSetting leftX = hmi("Левая X");
    public final SliderSetting leftY = hmi("Левая Y");
    public final SliderSetting leftZ = hmi("Левая Z");

    public SwingAnimationModule() {
        addSettings(mode, angle, itemScale, tiltPower, changeSpeed, cubicEase, speed, auraOnly, onlyOnHit, useAnimation,
                startX, startY, startZ, startPitch, startYaw, startRoll,
                endX, endY, endZ, endPitch, endYaw, endRoll,
                curve1X, curve1Y, curve2X, curve2Y, customBack, customBackSpeed,
                newShowLeftArm, attackMode, rightX, rightY, rightZ, leftX, leftY, leftZ);
    }

    private SliderSetting hmi(String name) {
        return new SliderSetting(name).value(0f).range(-2f, 2f).step(0.1f).setVisible(() -> mode.is("HMI"));
    }

    private SliderSetting custom(String name, float value, float min, float max, float step) {
        return new SliderSetting(name).value(value).range(min, max).step(step).setVisible(() -> mode.is("Custom"));
    }

    @Override
    public void onEvent() {
    }

    public boolean isHmi() {
        return isEnabled() && mode.is("HMI");
    }

    private static boolean auraHasTarget() {
        AuraModule aura = AuraModule.getInstance();
        return aura.isEnabled() && aura.target != null;
    }

    private boolean swingAllowed() {
        return !onlyOnHit.getValue() || mc.player != null && mc.player.swinging;
    }

    public boolean overridesVanillaSwing() {
        if (!isEnabled() || mode.is("Обычный")) return false;
        if (mode.is("HMI")) return true;
        return swingAllowed() && (!auraOnly.getValue() || auraHasTarget());
    }

    public boolean applySwing(PoseStack pose, HumanoidArm arm, float swingProgress) {
        if (!isEnabled() || mode.is("HMI") || !swingAllowed()) return false;
        if (auraOnly.getValue() && !auraHasTarget()) return false;
        int side = arm == HumanoidArm.RIGHT ? 1 : -1;
        float progress = Mth.clamp(swingProgress, 0f, 1f);
        float eased = cubicEase.getValue() ? cubicOut(progress) : progress;
        float wave = (float) Math.sin(eased * Math.PI);
        float tilt = tiltPower.getValue();
        float scale = itemScale.getValue();
        boolean left = arm == HumanoidArm.LEFT;

        pose.scale(scale, scale, scale);
        if (mc.player == null || arm != mc.player.getMainArm()) return false;

        switch (mode.getValue()) {
            case "Мод 13" -> {
                pose.translate(0f, 0f, -wave * tilt / 10f);
                pose.mulPose(Axis.YP.rotationDegrees(side * wave * tilt * 5f));
            }
            case "Мод 14" -> {
                pose.translate(0.0, 0.1, 0.0);
                pose.mulPose(Axis.XP.rotationDegrees(eased * 360f));
                pose.translate(0.0, -0.1, 0.0);
            }
            case "Custom" -> applyCustom(pose, side, progress);
            case "Мод 1" -> {
                float sqrt = Mth.sqrt(eased);
                pose.mulPose(Axis.YP.rotationDegrees(side * (45f + Mth.sin(eased * eased * (float) Math.PI) / 4f * -20f)));
                pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.sin(sqrt * (float) Math.PI) * -20f));
                pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(sqrt * (float) Math.PI) * -tilt * 10f));
                pose.mulPose(Axis.YP.rotationDegrees(-side * 45));
            }
            case "Мод 2" -> pose.mulPose(Axis.XP.rotationDegrees(side * (eased * (float) Math.PI - tilt * 10f * wave)));
            case "Мод 3" -> {
                pose.translate(0.0, 0.15, -0.3F);
                pose.mulPose(Axis.YP.rotationDegrees(left ? -90f : 90f));
                pose.mulPose(Axis.ZP.rotationDegrees(left ? 60f : -60f));
                pose.mulPose(Axis.XP.rotationDegrees(-angle.getValue() - tilt * 10f * wave));
            }
            case "Мод 4" -> {
                pose.translate(0.0, 0.15, -0.3F);
                pose.mulPose(Axis.YP.rotationDegrees(left ? -70f : 70f));
                pose.mulPose(Axis.ZP.rotationDegrees(left ? 10f : -10f));
                pose.mulPose(Axis.XP.rotationDegrees(-angle.getValue() - tilt * 10f * wave));
            }
            case "Мод 5" -> {
                pose.translate(0.0, 0.15, -0.3F);
                pose.mulPose(Axis.YP.rotationDegrees(left ? -70f : 70f));
                pose.mulPose(Axis.ZP.rotationDegrees(left ? 10f : -10f));
                pose.mulPose(Axis.XP.rotationDegrees(-70f - tilt * 10f * wave));
            }
            case "Мод 12" -> {
                float invert = left ? -1f : 1f;
                float swingWave = Mth.sin(Mth.sqrt(eased) * (float) Math.PI);
                pose.translate(-invert * 0.56f, 0.72f, 0.72f);
                pose.translate(0.56f * invert, -0.52f - swingWave * tilt / 24f, -0.72f);
                pose.mulPose(Axis.YP.rotationDegrees(90f * invert));
                pose.mulPose(Axis.ZP.rotationDegrees(-30f * invert));
                pose.mulPose(Axis.XP.rotationDegrees(-90f));
            }
            case "Мод 6" -> {
                pose.translate(0.0, 0.15, -0.3F);
                pose.mulPose(Axis.YP.rotationDegrees(left ? -70f - 20f * wave : 70f + 20f * wave));
                pose.mulPose(Axis.ZP.rotationDegrees(left ? 10f + 60f * wave : -10f - 60f * wave));
                pose.mulPose(Axis.XP.rotationDegrees(-100f - tilt * 10f * wave));
            }
            case "Мод 7" -> {
                pose.translate(0.0, 0.15, -0.3F);
                pose.mulPose(Axis.YP.rotationDegrees(left ? -90f : 90f));
                pose.mulPose(Axis.ZP.rotationDegrees(left ? 60f : -60f - tilt * 10f * wave));
                pose.mulPose(Axis.XP.rotationDegrees(-50f));
            }
            case "Мод 8" -> {
                pose.translate(0.5f, -0.15f, -0.65f);
                pose.mulPose(Axis.YP.rotationDegrees(45f));
                float a = Mth.sin(eased * eased * (float) Math.PI);
                float b = Mth.sin(Mth.sqrt(eased) * (float) Math.PI);
                pose.mulPose(Axis.YP.rotationDegrees(a * -20f));
                pose.mulPose(Axis.ZP.rotationDegrees(b * -20f));
                pose.mulPose(Axis.XP.rotationDegrees(b * (-tilt * 10f)));
                pose.translate(-0.5f, 0.2f, 0f);
                pose.mulPose(Axis.YP.rotationDegrees(left ? -60 : 30));
                pose.mulPose(Axis.XP.rotationDegrees(-80f));
                pose.mulPose(Axis.YP.rotationDegrees(left ? -60 : 60));
            }
            case "Мод 9" -> {
                pose.mulPose(Axis.YP.rotationDegrees(side * 45f));
                pose.mulPose(Axis.XP.rotationDegrees(wave * -20f));
                pose.mulPose(Axis.ZP.rotationDegrees(side * wave * -20f));
                pose.mulPose(Axis.XP.rotationDegrees(wave * -80f));
                pose.translate(side * 0.4f, 0.2f, 0.2f);
                pose.translate(side * -0.5f, 0.08f, 0f);
                pose.mulPose(Axis.YP.rotationDegrees(side * 20f));
                pose.mulPose(Axis.XP.rotationDegrees(-80f));
                pose.mulPose(Axis.YP.rotationDegrees(side * 20f));
            }
            case "Мод 10" -> {
                pose.translate(0.5, -0.15F, -0.65F);
                float bounce = -0.2f * Mth.sin(eased * (float) Math.PI);
                pose.mulPose(Axis.YP.rotationDegrees(45f));
                pose.mulPose(Axis.YP.rotationDegrees(0f));
                pose.mulPose(Axis.ZP.rotationDegrees(0f));
                pose.mulPose(Axis.XP.rotationDegrees(0f));
                pose.mulPose(Axis.YP.rotationDegrees(30f));
                pose.mulPose(Axis.XP.rotationDegrees(-80f));
                pose.mulPose(Axis.YP.rotationDegrees(60f));
                long now = System.currentTimeMillis();
                int spin = (int) Math.min(255L, (now % 255L > 127L ? Math.abs(Math.abs(now) % 255L - 255L) : now % 255L) * 2L);
                float back = bounce > 0.5f ? 1f - bounce : bounce;
                pose.mulPose(Axis.ZP.rotationDegrees(0f));
                pose.mulPose(Axis.XP.rotationDegrees(90f));
                pose.mulPose(Axis.XP.rotationDegrees(-90f));
                pose.mulPose(Axis.XP.rotationDegrees(-10f));
                pose.mulPose(Axis.XP.rotationDegrees(-back * 10f));
                pose.mulPose(Axis.YP.rotationDegrees(10f));
                pose.mulPose(Axis.ZP.rotationDegrees(mc.player.swinging ? -spin / (110f - tilt * 10f) : 1f));
            }
            case "Мод 11" -> {
                float drop;
                if (eased < 0.8f) {
                    drop = -eased * 1.25f;
                } else {
                    float rest = (eased - 0.8f) * 5f;
                    drop = -(1f - rest);
                }
                pose.translate(0f, drop, 0f);
            }
            default -> { }
        }
        return !mode.is("Обычный");
    }

    public int swingDuration(boolean mainHand) {
        if (!isEnabled() || mode.is("HMI") || !changeSpeed.getValue() || !mainHand || !swingAllowed()) return -1;
        if (auraOnly.getValue() && !auraHasTarget()) return -1;
        return Math.max(1, 25 - speed.getValue().intValue() * 2);
    }

    public float eatBobbing(float vanilla) {
        if (!isEnabled()) return vanilla;
        return switch (useAnimation.getValue()) {
            case "Укороченная" -> 0.02f;
            case "Полная отмена" -> 0f;
            default -> vanilla;
        };
    }

    private void applyCustom(PoseStack pose, int side, float progress) {
        float t = customProgress(progress);
        float x = Mth.lerp(t, startX.getValue(), endX.getValue()) * side;
        float y = Mth.lerp(t, startY.getValue(), endY.getValue());
        float z = Mth.lerp(t, startZ.getValue(), endZ.getValue());
        float pitch = Mth.lerp(t, startPitch.getValue(), endPitch.getValue());
        float yaw = Mth.lerp(t, startYaw.getValue(), endYaw.getValue()) * side;
        float roll = Mth.lerp(t, startRoll.getValue(), endRoll.getValue()) * side;
        pose.translate(x, y, z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.ZP.rotationDegrees(roll));
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
    }

    private float customProgress(float progress) {
        if (customBack.getValue()) {
            float split = Mth.lerp((customBackSpeed.getValue() - 1f) / 9f, 0.45f, 0.78f);
            if (progress <= split) return bezierAt(Mth.clamp(progress / split, 0f, 1f));
            float back = 1f - Mth.clamp((progress - split) / (1f - split), 0f, 1f);
            return back * back * back;
        }
        return bezierAt(Mth.clamp(progress, 0f, 1f));
    }

    private float bezierAt(float x) {
        float lo = 0f, hi = 1f;
        for (int i = 0; i < 12; i++) {
            float mid = (lo + hi) * 0.5f;
            if (bezier(mid, 0f, curve1X.getValue(), curve2X.getValue(), 1f) < x) lo = mid;
            else hi = mid;
        }
        return Mth.clamp(bezier((lo + hi) * 0.5f, 0f, curve1Y.getValue(), curve2Y.getValue(), 1f), 0f, 1f);
    }

    private static float bezier(float t, float p0, float p1, float p2, float p3) {
        float u = 1f - t;
        return u * u * u * p0 + 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t * p3;
    }

    private static float cubicOut(float t) {
        t = Mth.clamp(t, 0f, 1f);
        float u = 1f - t;
        return 1f - u * u * u;
    }
}
