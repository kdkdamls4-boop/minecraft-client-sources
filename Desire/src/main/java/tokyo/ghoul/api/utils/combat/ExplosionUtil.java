package tokyo.ghoul.api.utils.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.predict.PredictUtil;

@UtilityClass
public class ExplosionUtil implements QuickImports {
    private final ClientExplosion CLIENT_EXPLOSION = new ClientExplosion();

    public float getExplosionDamagePredict(Vec3 explosionPos, Player target, int predictTicks) {
        if (predictTicks <= 0) {
            return getExplosionDamage(explosionPos, target);
        }
        AABB predictedBox = predictBox(target, predictTicks);
        if (predictedBox == null) {
            return getExplosionDamage(explosionPos, target);
        }
        return getExplosionDamageWPredict(explosionPos, target, predictedBox);
    }

    public float getDamageOfGhostBlockPredict(Vec3 explosionPos, Player target, BlockPos bp, int predictTicks) {
        if (predictTicks <= 0) {
            return getDamageOfGhostBlock(explosionPos, target, bp);
        }
        AABB predictedBox = predictBox(target, predictTicks);
        if (predictedBox == null) {
            return getDamageOfGhostBlock(explosionPos, target, bp);
        }
        return getDamageOfGhostBlockWPredict(explosionPos, target, bp, predictedBox);
    }

    public float getExplosionDamage(Vec3 explosionPos, Player target) {
        if (mc.level == null || mc.player == null || target == null
                || mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0f;
        }

        DamageSource damageSource = Explosion.getDefaultDamageSource(mc.level, mc.player);
        CLIENT_EXPLOSION.update(explosionPos, mc.player);

        if (!new AABB(
                Mth.floor(explosionPos.x - 11),
                Mth.floor(explosionPos.y - 11),
                Mth.floor(explosionPos.z - 11),
                Mth.floor(explosionPos.x + 13),
                Mth.floor(explosionPos.y + 13),
                Mth.floor(explosionPos.z + 13)
        ).intersects(target.getBoundingBox())) {
            return 0f;
        }

        if (target.isInvulnerable()) {
            return 0f;
        }

        double distExposure = target.distanceToSqr(explosionPos) / 144.0;
        if (distExposure > 1.0) return 0f;

        double exposure = getExposure(explosionPos, target.getBoundingBox(), false, false);
        return applyExplosionDamage(target, distExposure, exposure, damageSource);
    }

    public float getExplosionDamageWPredict(Vec3 explosionPos, Player target, AABB predict) {
        if (mc.level == null || mc.player == null || target == null || predict == null
                || mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0f;
        }

        DamageSource damageSource = Explosion.getDefaultDamageSource(mc.level, mc.player);
        CLIENT_EXPLOSION.update(explosionPos, mc.player);

        if (!new AABB(
                Mth.floor(explosionPos.x - 11),
                Mth.floor(explosionPos.y - 11),
                Mth.floor(explosionPos.z - 11),
                Mth.floor(explosionPos.x + 13),
                Mth.floor(explosionPos.y + 13),
                Mth.floor(explosionPos.z + 13)
        ).intersects(predict)) {
            return 0f;
        }

        if (target.isInvulnerable()) {
            return 0f;
        }

        double distExposure = predict.getCenter().add(0, -0.9, 0).distanceToSqr(explosionPos) / 144.0;
        if (distExposure > 1.0) return 0f;

        double exposure = getExposure(explosionPos, predict, false, false);
        return applyExplosionDamage(target, distExposure, exposure, damageSource);
    }

    public float getDamageOfGhostBlock(Vec3 explosionPos, Player target, BlockPos bp) {
        if (mc.level == null || mc.player == null || target == null || bp == null
                || mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0f;
        }

        DamageSource damageSource = Explosion.getDefaultDamageSource(mc.level, mc.player);
        CLIENT_EXPLOSION.update(explosionPos, mc.player);

        double maxDist = 12;
        if (!new AABB(
                Mth.floor(explosionPos.x - maxDist - 1.0),
                Mth.floor(explosionPos.y - maxDist - 1.0),
                Mth.floor(explosionPos.z - maxDist - 1.0),
                Mth.floor(explosionPos.x + maxDist + 1.0),
                Mth.floor(explosionPos.y + maxDist + 1.0),
                Mth.floor(explosionPos.z + maxDist + 1.0)
        ).intersects(target.getBoundingBox())) {
            return 0f;
        }

        if (target.isInvulnerable()) {
            return 0f;
        }

        double distExposure = target.distanceToSqr(explosionPos) / 144.0;
        if (distExposure > 1.0) return 0f;

        double exposure = getExposureGhost(explosionPos, target, bp, false);
        return applyExplosionDamage(target, distExposure, exposure, damageSource);
    }

    public float getDamageIgnoringBlock(Vec3 explosionPos, Player target, BlockPos airPos) {
        if (mc.level == null || mc.player == null || target == null || airPos == null
                || mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0f;
        }

        double maxDist = 12.0;
        if (!new AABB(
                Mth.floor(explosionPos.x - maxDist - 1.0),
                Mth.floor(explosionPos.y - maxDist - 1.0),
                Mth.floor(explosionPos.z - maxDist - 1.0),
                Mth.floor(explosionPos.x + maxDist + 1.0),
                Mth.floor(explosionPos.y + maxDist + 1.0),
                Mth.floor(explosionPos.z + maxDist + 1.0)
        ).intersects(target.getBoundingBox())) {
            return 0f;
        }

        if (target.isInvulnerable()) {
            return 0f;
        }

        double distExposure = target.distanceToSqr(explosionPos) / 144.0;
        if (distExposure > 1.0) return 0f;

        double exposure = getExposureIgnoreBlock(explosionPos, target.getBoundingBox(), target, airPos);
        DamageSource damageSource = Explosion.getDefaultDamageSource(mc.level, mc.player);
        CLIENT_EXPLOSION.update(explosionPos, mc.player);
        return applyExplosionDamage(target, distExposure, exposure, damageSource);
    }

    public boolean intersectsEntityBox(BlockPos pos, Entity entity) {
        AABB blockBox = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0
        );
        return entity.getBoundingBox().intersects(blockBox);
    }

    public float getDamageOfGhostBlockWPredict(Vec3 explosionPos, Player target, BlockPos bp, AABB predictedBox) {
        if (mc.level == null || mc.player == null || target == null || bp == null || predictedBox == null
                || mc.level.getDifficulty() == Difficulty.PEACEFUL) {
            return 0f;
        }

        DamageSource damageSource = Explosion.getDefaultDamageSource(mc.level, mc.player);
        CLIENT_EXPLOSION.update(explosionPos, mc.player);

        double maxDist = 12;
        if (!new AABB(
                Mth.floor(explosionPos.x - maxDist - 1.0),
                Mth.floor(explosionPos.y - maxDist - 1.0),
                Mth.floor(explosionPos.z - maxDist - 1.0),
                Mth.floor(explosionPos.x + maxDist + 1.0),
                Mth.floor(explosionPos.y + maxDist + 1.0),
                Mth.floor(explosionPos.z + maxDist + 1.0)
        ).intersects(predictedBox)) {
            return 0f;
        }

        if (target.isInvulnerable()) {
            return 0f;
        }

        double distExposure = predictedBox.getCenter().add(0, -0.9, 0).distanceToSqr(explosionPos) / 144.0;
        if (distExposure > 1.0) return 0f;

        double exposure = getExposureGhostPredict(explosionPos, predictedBox, bp, false);
        return applyExplosionDamage(target, distExposure, exposure, damageSource);
    }

    private float applyExplosionDamage(Player target, double distExposure, double exposure, DamageSource damageSource) {
        double finalExposure = (1.0 - distExposure) * exposure;

        float toDamage = (float) Math.floor((finalExposure * finalExposure + finalExposure) / 2.0 * 7.0 * 12.0 + 1.0);

        if (mc.level.getDifficulty() == Difficulty.EASY) {
            toDamage = Math.min(toDamage / 2f + 1f, toDamage);
        } else if (mc.level.getDifficulty() == Difficulty.HARD) {
            toDamage = toDamage * 3f / 2f;
        }

        toDamage = CombatRules.getDamageAfterAbsorb(
                target,
                toDamage,
                damageSource,
                target.getArmorValue(),
                (float) target.getAttributeValue(Attributes.ARMOR_TOUGHNESS)
        );

        if (target.hasEffect(MobEffects.RESISTANCE)) {
            int resistance = 25 - (target.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
            float resistancePart = toDamage * resistance;
            toDamage = Math.max(resistancePart / 25f, 0f);
        }

        if (toDamage <= 0f) {
            return 0f;
        }

        float protAmount = getProtectionAmount(armorStacks(target));
        if (protAmount > 0f) {
            toDamage = CombatRules.getDamageAfterMagicAbsorb(toDamage, protAmount);
        }

        return Math.max(toDamage, 0f);
    }

    private float getExposureGhost(Vec3 source, Entity entity, BlockPos pos, boolean ignoreTerrain) {
        return calcExposureGhost(source, entity.getBoundingBox(), pos, ignoreTerrain, entity);
    }

    private float getExposureGhostPredict(Vec3 source, AABB box, BlockPos pos, boolean ignoreTerrain) {
        return calcExposureGhost(source, box, pos, ignoreTerrain, mc.player);
    }

    private float calcExposureGhost(Vec3 source, AABB box, BlockPos pos, boolean ignoreTerrain, Entity entity) {
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;

        if (d < 0.0 || e < 0.0 || f < 0.0) {
            return 0.0f;
        }

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n + g, o, p + h);
                    if (raycastGhost(new ClipContext(vec3d, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity), pos, ignoreTerrain)
                            .getType() == HitResult.Type.MISS) {
                        ++i;
                    }
                    ++j;
                }
            }
        }

        return (float) i / (float) j;
    }

    public float getExposure(Vec3 source, AABB box, boolean optimized, boolean ignoreTerrain) {
        if (!optimized) return getExposure(source, box, ignoreTerrain);

        int miss = 0;
        int hit = 0;

        for (int k = 0; k <= 1; k += 1) {
            for (int l = 0; l <= 1; l += 1) {
                for (int m = 0; m <= 1; m += 1) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n, o, p);
                    if (raycast(vec3d, source, ignoreTerrain) == HitResult.Type.MISS) {
                        ++miss;
                    }
                    ++hit;
                }
            }
        }
        return (float) miss / (float) hit;
    }

    public float getExposure(Vec3 source, AABB box, boolean ignoreTerrain) {
        double d = 0.4545454446934474;
        double e = 0.21739130885479366;
        double f = 0.4545454446934474;

        int i = 0;
        int j = 0;

        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n + 0.045454555306552624, o, p + 0.045454555306552624);
                    if (raycast(vec3d, source, ignoreTerrain) == HitResult.Type.MISS) {
                        ++i;
                    }
                    ++j;
                }
            }
        }

        return (float) i / (float) j;
    }

    private float getExposureIgnoreBlock(Vec3 source, AABB box, Entity entity, BlockPos airPos) {
        double d = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double e = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double f = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double g = (1.0 - Math.floor(1.0 / d) * d) / 2.0;
        double h = (1.0 - Math.floor(1.0 / f) * f) / 2.0;
        if (d < 0.0 || e < 0.0 || f < 0.0) return 0.0f;

        int i = 0;
        int j = 0;
        for (double k = 0.0; k <= 1.0; k += d) {
            for (double l = 0.0; l <= 1.0; l += e) {
                for (double m = 0.0; m <= 1.0; m += f) {
                    double n = Mth.lerp(k, box.minX, box.maxX);
                    double o = Mth.lerp(l, box.minY, box.maxY);
                    double p = Mth.lerp(m, box.minZ, box.maxZ);
                    Vec3 vec3d = new Vec3(n + g, o, p + h);
                    if (raycastIgnoreBlock(new ClipContext(vec3d, source, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity), airPos)
                            .getType() == HitResult.Type.MISS) {
                        ++i;
                    }
                    ++j;
                }
            }
        }
        return (float) i / (float) j;
    }

    private BlockHitResult raycastIgnoreBlock(ClipContext context, BlockPos airPos) {
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (inner, pos) -> {
            BlockState state = pos.equals(airPos) ? Blocks.AIR.defaultBlockState() : mc.level.getBlockState(pos);
            VoxelShape shape = state.getCollisionShape(mc.level, pos);
            return shape.clip(inner.getFrom(), inner.getTo(), pos);
        }, inner -> BlockHitResult.miss(
                inner.getTo(),
                Direction.getNearest(
                        (int) Math.signum(inner.getFrom().x - inner.getTo().x),
                        (int) Math.signum(inner.getFrom().y - inner.getTo().y),
                        (int) Math.signum(inner.getFrom().z - inner.getTo().z),
                        Direction.UP),
                BlockPos.containing(inner.getTo())
        ));
    }

    private BlockHitResult raycastGhost(ClipContext context, BlockPos bPos, boolean ignoreTerrain) {
        return BlockGetter.traverseBlocks(context.getFrom(), context.getTo(), context, (innerContext, pos) -> {
            Vec3 vec3d = innerContext.getFrom();
            Vec3 vec3d2 = innerContext.getTo();

            BlockState blockState;
            if (pos.equals(bPos)) {
                blockState = Blocks.OBSIDIAN.defaultBlockState();
            } else {
                blockState = mc.level.getBlockState(pos);
                if (ignoreTerrain && blockState.getBlock().getExplosionResistance() < 600f) {
                    blockState = Blocks.AIR.defaultBlockState();
                }
            }

            VoxelShape voxelShape = innerContext.getBlockShape(blockState, mc.level, pos);
            BlockHitResult blockHitResult = mc.level.clipWithInteractionOverride(vec3d, vec3d2, pos, voxelShape, blockState);
            BlockHitResult blockHitResult2 = Shapes.empty().clip(vec3d, vec3d2, pos);
            double d = blockHitResult == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult.getLocation());
            double e = blockHitResult2 == null ? Double.MAX_VALUE : innerContext.getFrom().distanceToSqr(blockHitResult2.getLocation());
            return d <= e ? blockHitResult : blockHitResult2;
        }, innerContext -> {
            Vec3 vec3d = innerContext.getFrom().subtract(innerContext.getTo());
            return BlockHitResult.miss(innerContext.getTo(), Direction.getNearest((int) Math.signum(vec3d.x), (int) Math.signum(vec3d.y), (int) Math.signum(vec3d.z), Direction.UP), BlockPos.containing(innerContext.getTo()));
        });
    }

    public HitResult.Type raycast(Vec3 start, Vec3 end, boolean ignoreTerrain) {
        return BlockGetter.traverseBlocks(start, end, null, (innerContext, blockPos) -> {
            BlockState blockState = mc.level.getBlockState(blockPos);
            if (blockState.getBlock().getExplosionResistance() < 600 && ignoreTerrain) return null;
            BlockHitResult hitResult = blockState.getCollisionShape(mc.level, blockPos).clip(start, end, blockPos);
            return hitResult == null ? null : hitResult.getType();
        }, innerContext -> HitResult.Type.MISS);
    }

    private java.util.List<ItemStack> armorStacks(LivingEntity entity) {
        return java.util.List.of(
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
                entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET));
    }

    public int getProtectionAmount(Iterable<ItemStack> equipment) {
        int value = 0;
        for (ItemStack stack : equipment) {
            value += getProtectionAmount(stack);
        }
        return value;
    }

    public int getProtectionAmount(ItemStack stack) {
        if (mc.level == null || stack.isEmpty()) return 0;

        Holder<Enchantment> blastProtection = getEnchantmentEntry(Enchantments.BLAST_PROTECTION);
        Holder<Enchantment> protection = getEnchantmentEntry(Enchantments.PROTECTION);

        int modifierBlast = EnchantmentHelper.getItemEnchantmentLevel(blastProtection, stack);
        int modifier = EnchantmentHelper.getItemEnchantmentLevel(protection, stack);
        return modifierBlast * 2 + modifier;
    }

    private Holder<Enchantment> getEnchantmentEntry(ResourceKey<Enchantment> key) {
        return mc.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .get(key)
                .orElseThrow();
    }

    private AABB predictBox(Player target, int predictTicks) {
        if (target == null) return null;
        if (predictTicks <= 0) return target.getBoundingBox();

        Vec3 predicted = PredictUtil.getPredicted(target, predictTicks);
        Vec3 delta = predicted.subtract(target.position());
        AABB box = target.getBoundingBox();
        return new AABB(box.minX + delta.x, box.minY + delta.y, box.minZ + delta.z,
                box.maxX + delta.x, box.maxY + delta.y, box.maxZ + delta.z);
    }

    private static final class ClientExplosion implements Explosion {
        private Vec3 pos = Vec3.ZERO;
        private Entity entity;

        private void update(Vec3 pos, Entity entity) {
            this.pos = pos;
            this.entity = entity;
        }

        @Override
        public ServerLevel level() {
            return null;
        }

        @Override
        public BlockInteraction getBlockInteraction() {
            return BlockInteraction.DESTROY;
        }

        @Override
        public LivingEntity getIndirectSourceEntity() {
            return entity instanceof LivingEntity living ? living : null;
        }

        @Override
        public Entity getDirectSourceEntity() {
            return entity;
        }

        @Override
        public float radius() {
            return 6.0f;
        }

        @Override
        public Vec3 center() {
            return pos;
        }

        @Override
        public boolean canTriggerBlocks() {
            return false;
        }

        @Override
        public boolean shouldAffectBlocklikeEntities() {
            return false;
        }
    }
}
