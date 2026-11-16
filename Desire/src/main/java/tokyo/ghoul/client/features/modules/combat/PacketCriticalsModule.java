package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;

@ModuleRegister(name = "Packet Criticals", description = "Критические удары через пакеты", category = Category.COMBAT)
public class PacketCriticalsModule extends Module {
    @Getter private static final PacketCriticalsModule instance = new PacketCriticalsModule();

    private static final int WATER_CRIT_INTENT_TICKS = 8;
    private static final int WATER_CRIT_CONTACT_TICKS = 10;
    private static final double WATER_CRIT_MIN_UPWARD_VELOCITY = 0.05;

    private final BooleanSetting cobweb = new BooleanSetting("Cobweb").value(true);
    private final BooleanSetting slowFalling = new BooleanSetting("Slow falling").value(true);
    private final BooleanSetting levitation = new BooleanSetting("Levitation").value(true);
    private final BooleanSetting water = new BooleanSetting("Water").value(true);
    private final BooleanSetting onlyAura = new BooleanSetting("Only Aura").value(false);

    private int lastWaterContactAge = Integer.MIN_VALUE;
    private int lastWaterCritIntentAge = Integer.MIN_VALUE;

    public PacketCriticalsModule() {
        addSettings(cobweb, slowFalling, levitation, water, onlyAura);
    }

    @Override
    public void onEvent() {
    }

    @Override
    public void onDisable() {
        resetWaterState();
        super.onDisable();
    }

    public void onAttack() {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.connection == null) {
            resetWaterState();
            return;
        }
        if (!isEnabled()) return;

        if (onlyAura.getValue()) {
            AuraModule aura = AuraModule.getInstance();
            if (!aura.isEnabled() || aura.target == null) return;
        }

        if (cobweb.getValue() && isInCobweb()) {
            spoofCobwebCritical();
        } else if (levitation.getValue() && player.hasEffect(MobEffects.LEVITATION)) {
            spoofAirCritical();
        }
    }

    public boolean canBypassCriticalRequirement() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || mc.level == null) {
            resetWaterState();
            return false;
        }

        updateWaterCritState();

        if (cobweb.getValue() && isInCobweb()) return true;
        if (levitation.getValue() && player.hasEffect(MobEffects.LEVITATION)) return true;
        if (water.getValue() && isTryingWaterCrit()) return isWaterCritWindow();

        return slowFalling.getValue()
                && player.hasEffect(MobEffects.SLOW_FALLING)
                && player.getDeltaMovement().y < 0.0
                && player.fallDistance > 0.0f;
    }

    private void spoofCobwebCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean horizontalCollision = mc.player.horizontalCollision;
        sendPosition(x, y + 0.003, z, horizontalCollision);
        sendPosition(x, y, z, horizontalCollision);
    }

    private void spoofAirCritical() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean horizontalCollision = mc.player.horizontalCollision;
        sendPosition(x, y + 0.0625, z, horizontalCollision);
        sendPosition(x, y, z, horizontalCollision);
        sendPosition(x, y + 1.1E-5, z, horizontalCollision);
        sendPosition(x, y, z, horizontalCollision);
    }

    private void sendPosition(double x, double y, double z, boolean horizontalCollision) {
        mc.player.connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, false, horizontalCollision));
    }

    private boolean isInCobweb() {
        return BlockPos.betweenClosedStream(mc.player.getBoundingBox().deflate(1.0E-3))
                .anyMatch(pos -> mc.level.getBlockState(pos).is(Blocks.COBWEB));
    }

    private void updateWaterCritState() {
        if (!isNearWaterSurface()) return;
        lastWaterContactAge = mc.player.tickCount;
        if (isWaterCritIntentState()) {
            lastWaterCritIntentAge = mc.player.tickCount;
        }
    }

    private boolean isWaterCritIntentState() {
        return mc.options != null
                && mc.options.keyJump.isDown()
                && !mc.player.onGround()
                && !mc.player.isUnderWater()
                && mc.player.getDeltaMovement().y > WATER_CRIT_MIN_UPWARD_VELOCITY;
    }

    private boolean isTryingWaterCrit() {
        return mc.options != null
                && mc.options.keyJump.isDown()
                && mc.player.tickCount - lastWaterCritIntentAge <= WATER_CRIT_INTENT_TICKS
                && mc.player.tickCount - lastWaterContactAge <= WATER_CRIT_CONTACT_TICKS;
    }

    private boolean isWaterCritWindow() {
        return !mc.player.onGround()
                && !mc.player.isInWater()
                && !mc.player.isUnderWater()
                && mc.player.fallDistance > 0.0f
                && mc.player.getDeltaMovement().y < 0.0;
    }

    private boolean isNearWaterSurface() {
        LocalPlayer player = mc.player;
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - 0.4, player.getZ());
        return player.isInWater()
                || player.isUnderWater()
                || mc.level.getBlockState(below).is(Blocks.WATER);
    }

    private void resetWaterState() {
        lastWaterContactAge = Integer.MIN_VALUE;
        lastWaterCritIntentAge = Integer.MIN_VALUE;
    }
}
