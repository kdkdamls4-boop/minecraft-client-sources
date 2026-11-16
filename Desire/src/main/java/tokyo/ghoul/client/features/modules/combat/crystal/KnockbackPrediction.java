package tokyo.ghoul.client.features.modules.combat.crystal;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

public final class KnockbackPrediction implements QuickImports {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private KnockbackPrediction() {}

    public static float yawTowards(Vec3 attackerPos, LivingEntity target) {
        return (float) (Mth.atan2(target.getZ() - attackerPos.z, target.getX() - attackerPos.x) * (180.0F / (float) Math.PI)) - 90.0F;
    }

    public static Vec3 apply(LivingEntity target, Vec3 velocity, boolean onGround, Vec3 attackerPos, float attackerYaw, float strength) {
        double resistance = knockbackResistance(target);
        Vec3 result = knockback(velocity, 0.4F, attackerPos.x - target.getX(), attackerPos.z - target.getZ(), onGround, resistance);
        if (strength > 0.0F) {
            double dirX = Mth.sin(attackerYaw * Mth.DEG_TO_RAD);
            double dirZ = -Mth.cos(attackerYaw * Mth.DEG_TO_RAD);
            result = knockback(result, strength, dirX, dirZ, onGround, resistance);
        }
        return result;
    }

    public static Vec3 knockback(Vec3 velocity, double strength, double dirX, double dirZ, boolean onGround, double resistance) {
        double power = strength * (1.0 - resistance);
        if (power <= 0.0 || dirX * dirX + dirZ * dirZ < 1.0E-5) return velocity;

        Vec3 push = new Vec3(dirX, 0.0, dirZ).normalize().scale(power);
        return new Vec3(
                velocity.x / 2.0 - push.x,
                onGround ? Math.min(0.4, velocity.y / 2.0 + power) : velocity.y,
                velocity.z / 2.0 - push.z);
    }

    public static float currentStrength() {
        return strength(mc.player.isSprinting() && mc.player.getAttackStrengthScale(0.5F) > 0.9F);
    }

    public static float strength(boolean sprintHit) {
        float strength = (float) mc.player.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        strength += CrystalUtil.enchantmentLevel(mc.player.getMainHandItem(), Enchantments.KNOCKBACK);
        return strength / 2.0F + (sprintHit ? 0.5F : 0.0F);
    }

    private static double knockbackResistance(LivingEntity entity) {
        double[] fromArmor = {0.0};
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            entity.getItemBySlot(slot).forEachModifier(slot, (attribute, modifier) -> {
                if (attribute.equals(Attributes.KNOCKBACK_RESISTANCE) && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                    fromArmor[0] += modifier.amount();
                }
            });
        }
        return Mth.clamp(Math.max(entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), fromArmor[0]), 0.0, 1.0);
    }
}
