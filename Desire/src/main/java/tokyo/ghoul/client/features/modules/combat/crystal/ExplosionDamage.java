package tokyo.ghoul.client.features.modules.combat.crystal;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class ExplosionDamage implements QuickImports {
    public static final double CRYSTAL_POWER = 12.0;

    private static final double[] BOX_SAMPLES = {0.5, 0.05, 0.95};
    private static final double[] FACE_SAMPLES = {0.5, 0.1, 0.9, 0.3, 0.7};
    private static final AABB UNIT_BOX = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private static final CollisionContext EMPTY_CONTEXT = CollisionContext.empty();
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static final Int2IntOpenHashMap protectionCache = new Int2IntOpenHashMap();
    private static int protectionCacheTick = Integer.MIN_VALUE;
    private static final Map<ExposureKey, Float> exposureCache = new HashMap<>();
    private static int exposureCacheTick = Integer.MIN_VALUE;

    private static boolean emulateDamage;

    static {
        protectionCache.defaultReturnValue(-1);
    }

    private ExplosionDamage() {}

    public static void setEmulateDamage(boolean emulate) {
        emulateDamage = emulate;
    }


    public static float damage(Vec3 explosion, LivingEntity entity) {
        return entity == null ? 0.0F : damage(explosion, entity, entity.getBoundingBox());
    }

    public static float damage(Vec3 explosion, LivingEntity entity, AABB box) {
        return damage(explosion, entity, box, -1.0F);
    }

    public static float damage(Vec3 explosion, LivingEntity entity, AABB box, float exposure) {
        return damage(explosion, entity, box, exposure, CRYSTAL_POWER);
    }

    public static float damage(Vec3 explosion, LivingEntity entity, AABB box, float exposure, double power) {
        if (entity == null || box == null || mc.level == null) return 0.0F;
        if (entity.isDeadOrDying()) return 0.0F;

        float damage = rawDamage(explosion, entity, box, exposure, power);
        if (damage <= 0.0F) return 0.0F;

        if (entity instanceof Player && !emulateDamage) {
            damage = scaleByDifficulty(damage);
        }
        damage = applyArmor(entity, damage);
        damage = applyProtection(entity, damage);
        damage = applyResistance(entity, damage);
        return Math.max(0.0F, damage);
    }

    public static float damageWithChanges(Vec3 explosion, LivingEntity entity, AABB box, BlockPos placedBlock, BlockPos removedBlock) {
        if (placedBlock == null && removedBlock == null) {
            return damage(explosion, entity, box);
        }
        AABB placed = placedBlock == null ? null : new AABB(placedBlock).deflate(1.0E-4);
        List<BlockPos> removed = removedBlock == null ? List.of() : List.of(removedBlock);
        return damage(explosion, entity, box, exposure(explosion, box, placed, removed));
    }

    public static float damageWithBlock(Vec3 explosion, Entity entity, BlockPos blockPos) {
        AABB box = entity.getBoundingBox();
        float exposure = blockPos == null ? -1.0F : exposure(explosion, box, new AABB(blockPos).deflate(1.0E-4), List.of());
        return rawDamage(explosion, entity, box, exposure, CRYSTAL_POWER);
    }

    public static float maxDamageTo(Vec3 explosion, List<? extends LivingEntity> entities) {
        if (entities == null || entities.isEmpty()) return 0.0F;
        float max = 0.0F;
        for (LivingEntity entity : entities) {
            if (entity != null && entity != mc.player) {
                max = Math.max(max, damage(explosion, entity, entity.getBoundingBox()));
            }
        }
        return max;
    }

    private static float rawDamage(Vec3 explosion, Entity entity, AABB box, float exposure, double power) {
        double distance = feetCenter(box).distanceTo(explosion);
        if (distance > power) return 0.0F;

        double seen = exposure >= 0.0F ? exposure : cachedExposure(explosion, box, entity);
        double impact = (1.0 - distance / power) * seen;
        return (float) ((impact * impact + impact) / 2.0 * 7.0 * power + 1.0);
    }

    private static float scaleByDifficulty(float damage) {
        return switch (mc.level.getDifficulty()) {
            case PEACEFUL -> 0.0F;
            case EASY -> Math.min(damage / 2.0F + 1.0F, damage);
            case NORMAL -> damage;
            case HARD -> damage * 1.5F;
        };
    }

    private static float applyArmor(LivingEntity entity, float damage) {
        float armor = entity.getArmorValue();
        if (armor <= 0.0F) return damage;

        float toughness = (float) entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float breakdown = 2.0F + toughness / 4.0F;
        float effective = Mth.clamp(armor - damage / breakdown, armor * 0.2F, 20.0F);
        return damage * (1.0F - effective / 25.0F);
    }

    private static float applyProtection(LivingEntity entity, float damage) {
        return damage * (1.0F - protectionLevel(entity) / 25.0F);
    }

    private static float applyResistance(LivingEntity entity, float damage) {
        MobEffectInstance resistance = entity.getEffect(MobEffects.RESISTANCE);
        if (resistance == null) return damage;
        float reduction = Mth.clamp((resistance.getAmplifier() + 1) * 0.2F, 0.0F, 1.0F);
        return damage * (1.0F - reduction);
    }

    private static int protectionLevel(LivingEntity entity) {
        int tick = mc.player == null ? Integer.MIN_VALUE : mc.player.tickCount;
        if (tick != protectionCacheTick) {
            protectionCache.clear();
            protectionCacheTick = tick;
        }

        int cached = protectionCache.get(entity.getId());
        if (cached != -1) return cached;

        int level = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                level += CrystalUtil.enchantmentLevel(stack, Enchantments.PROTECTION);
                level += CrystalUtil.enchantmentLevel(stack, Enchantments.BLAST_PROTECTION) * 2;
            }
        }
        level = Mth.clamp(level, 0, 20);
        protectionCache.put(entity.getId(), level);
        return level;
    }


    private static float cachedExposure(Vec3 explosion, AABB box, Entity entity) {
        int tick = mc.player == null ? Integer.MIN_VALUE : mc.player.tickCount;
        if (tick != exposureCacheTick) {
            exposureCache.clear();
            exposureCacheTick = tick;
        }

        ExposureKey key = new ExposureKey(entity.getId(),
                (long) Math.floor(explosion.x), (long) Math.floor(explosion.y), (long) Math.floor(explosion.z), box);
        Float cached = exposureCache.get(key);
        if (cached != null) return cached;

        float exposure = exposure(explosion, box, null, List.of());
        exposureCache.put(key, exposure);
        return exposure;
    }

    private static float exposure(Vec3 explosion, AABB box, AABB extraBlock, List<BlockPos> ignored) {
        double stepX = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double stepY = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double stepZ = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double offsetX = (1.0 - Math.floor(1.0 / stepX) * stepX) / 2.0;
        double offsetZ = (1.0 - Math.floor(1.0 / stepZ) * stepZ) / 2.0;
        if (stepX < 0.0 || stepY < 0.0 || stepZ < 0.0) return 0.0F;

        int seen = 0;
        int total = 0;
        for (double x = 0.0; x <= 1.0; x += stepX) {
            for (double y = 0.0; y <= 1.0; y += stepY) {
                for (double z = 0.0; z <= 1.0; z += stepZ) {
                    Vec3 point = new Vec3(
                            Mth.lerp(x, box.minX, box.maxX) + offsetX,
                            Mth.lerp(y, box.minY, box.maxY),
                            Mth.lerp(z, box.minZ, box.maxZ) + offsetZ);
                    if (rayUnobstructed(point, explosion, extraBlock, ignored)) seen++;
                    total++;
                }
            }
        }
        return total == 0 ? 0.0F : (float) seen / total;
    }

    private static boolean rayUnobstructed(Vec3 from, Vec3 to, AABB extraBlock, List<BlockPos> ignored) {
        if (extraBlock != null && extraBlock.clip(from, to).isPresent()) return false;

        Vec3 start = from;
        for (int i = 0; i <= ignored.size(); i++) {
            BlockPos hit = CachedBlockView.INSTANCE.rayBlock(start, to, EMPTY_CONTEXT);
            if (hit == null) return true;
            if (!ignored.contains(hit)) return false;

            Vec3 exit = rayExit(start, to, hit);
            if (exit == null) return true;
            start = exit;
        }
        return false;
    }

    private static BlockPos firstOccluder(Vec3 from, Vec3 to, BlockPos target, List<BlockPos> ignored) {
        Vec3 start = from;
        BlockPos hit = null;
        for (int i = 0; i <= ignored.size(); i++) {
            hit = CachedBlockView.INSTANCE.rayBlock(start, to, EMPTY_CONTEXT, i == 0);
            if (hit == null || hit.equals(target)) return null;
            if (!ignored.contains(hit)) return hit;

            Vec3 exit = rayExit(start, to, hit);
            if (exit == null) return null;
            start = exit;
        }
        return hit;
    }

    static Vec3 rayExit(Vec3 from, Vec3 to, BlockPos pos) {
        Vec3 delta = to.subtract(from);
        double t = Double.POSITIVE_INFINITY;
        t = slabExit(t, delta.x, pos.getX() - from.x, pos.getX() + 1.0 - from.x);
        t = slabExit(t, delta.y, pos.getY() - from.y, pos.getY() + 1.0 - from.y);
        t = slabExit(t, delta.z, pos.getZ() - from.z, pos.getZ() + 1.0 - from.z);
        if (!Double.isFinite(t)) return null;

        double past = t + 1.0E-4;
        return past >= 1.0 ? null : from.add(delta.scale(past));
    }

    private static double slabExit(double current, double delta, double min, double max) {
        if (delta > 1.0E-8) {
            double t = max / delta;
            if (t >= 0.0 && t < current) return t;
        } else if (delta < -1.0E-8) {
            double t = min / delta;
            if (t >= 0.0 && t < current) return t;
        }
        return current;
    }


    public static Vec3 crystalAim(Entity crystal, Vec3 eye, float range, boolean matrix) {
        double rangeSq = range * range;
        Vec3 center = crystal.position().add(jitter(), 0.5 + jitter(), jitter());
        if (eye.distanceToSqr(center) <= rangeSq && isAimVisible(center, matrix)) return center;

        AABB box = crystal.getBoundingBox();
        Vec3 best = null;
        double bestDistance = Double.MAX_VALUE;

        for (double sx : BOX_SAMPLES) {
            double x = Mth.lerp(sx + sampleJitter(), box.minX, box.maxX);
            for (double sy : BOX_SAMPLES) {
                double y = Mth.lerp(sy + sampleJitter(), box.minY, box.maxY);
                for (double sz : BOX_SAMPLES) {
                    Vec3 point = new Vec3(x, y, Mth.lerp(sz + sampleJitter(), box.minZ, box.maxZ));
                    if (eye.distanceToSqr(point) > rangeSq) continue;

                    double distance = point.distanceToSqr(center);
                    if (distance < bestDistance && isAimVisible(point, matrix)) {
                        bestDistance = distance;
                        best = point;
                    }
                }
            }
        }
        return best;
    }

    public static Vec3 crystalAim(Entity crystal, Vec3 eye, float range) {
        return crystalAim(crystal, eye, range, false);
    }

    public static Vec3 crystalAimOrCenter(Entity crystal, Vec3 eye, float range) {
        Vec3 aim = crystalAim(crystal, eye, range);
        return aim != null ? aim : crystal.position().add(jitter(), 0.5 + jitter(), jitter());
    }

    private static boolean isAimVisible(Vec3 point, boolean matrix) {
        if (matrix) return MatrixPlacement.visibleFromAnyEye(point);
        if (mc.player == null || mc.level == null) return false;
        Vec3 eye = mc.player.getEyePosition();
        return mc.level.clip(new ClipContext(eye, point, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player))
                .getType() == HitResult.Type.MISS;
    }


    public static PlaceFace findPlaceFace(BlockPos pos, Vec3 eye, float range, boolean throughWalls) {
        return findPlaceFace(pos, eye, range, throughWalls, List.of());
    }

    public static PlaceFace findPlaceFace(BlockPos pos, Vec3 eye, float range, boolean throughWalls, List<BlockPos> ignored) {
        return searchPlaceFace(pos, eye, range, throughWalls, ignored).face();
    }

    public static BlockPos placeFaceOccluder(BlockPos pos, Vec3 eye, float range, boolean throughWalls) {
        return searchPlaceFace(pos, eye, range, throughWalls, List.of()).occluder();
    }

    private static FaceSearch searchPlaceFace(BlockPos pos, Vec3 eye, float range, boolean throughWalls, List<BlockPos> ignored) {
        PlaceFace best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos occluder = null;
        double occluderDistance = Double.MAX_VALUE;

        faces:
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side.getOpposite());
            if (ignored.contains(neighbor)) continue;

            BlockState state = mc.level.getBlockState(neighbor);
            if (state.canBeReplaced() || !isFaceFull(state, neighbor, side)) continue;

            VoxelShape shape = state.getCollisionShape(mc.level, neighbor);
            AABB bounds = shape.isEmpty() ? UNIT_BOX : shape.bounds();

            for (double u : FACE_SAMPLES) {
                for (double v : FACE_SAMPLES) {
                    Vec3 point = facePoint(bounds, neighbor, side, u, v);
                    if (!facesTowards(eye, point, side)) continue;

                    double distance = eye.distanceToSqr(point);
                    if (distance > range * range || distance >= bestDistance) continue;

                    BlockPos blocker = throughWalls ? null : firstOccluder(eye, point, neighbor, ignored);
                    if (blocker == null) {
                        bestDistance = distance;
                        best = new PlaceFace(neighbor, side, point);
                        continue faces;
                    }
                    if (distance < occluderDistance) {
                        occluderDistance = distance;
                        occluder = blocker;
                    }
                }
            }
        }
        return new FaceSearch(best, best == null ? occluder : null);
    }


    public static Vec3 crystalPlaceAim(BlockPos base, Vec3 eye, float range) {
        return searchCrystalPlaceAim(base, eye, range, List.of()).aim();
    }

    public static boolean hasCrystalPlaceAim(BlockPos base, Vec3 eye, float range) {
        return hasCrystalPlaceAim(base, eye, range, List.of());
    }

    public static boolean hasCrystalPlaceAim(BlockPos base, Vec3 eye, float range, List<BlockPos> ignored) {
        return searchCrystalPlaceAim(base, eye, range, ignored).aim() != null;
    }

    public static BlockPos crystalPlaceOccluder(BlockPos base, Vec3 eye, float range) {
        AimSearch search = searchCrystalPlaceAim(base, eye, range, List.of());
        return search.aim() != null ? null : search.occluder();
    }

    private static AimSearch searchCrystalPlaceAim(BlockPos base, Vec3 eye, float range, List<BlockPos> ignored) {
        double rangeSq = range * range;
        BlockPos occluder = null;
        double occluderDistance = Double.MAX_VALUE;

        for (Direction side : Direction.values()) {
            BlockPos outside = base.relative(side);
            if (!ignored.contains(outside) && !mc.level.getBlockState(outside).canBeReplaced()) continue;

            for (double u : FACE_SAMPLES) {
                for (double v : FACE_SAMPLES) {
                    Vec3 point = facePoint(UNIT_BOX, base, side, u, v);
                    double distance = eye.distanceToSqr(point);
                    if (distance > rangeSq) continue;

                    BlockPos blocker = firstOccluder(eye, point, base, ignored);
                    if (blocker == null) return new AimSearch(point, null);
                    if (distance < occluderDistance) {
                        occluderDistance = distance;
                        occluder = blocker;
                    }
                }
            }
        }
        return new AimSearch(null, occluder);
    }

    public static Visibility matrixVisibility(BlockPos base, Vec3 eye, float range, List<BlockPos> ignored) {
        double rangeSq = range * range;
        AABB box = crystalBox(base).deflate(0.05);
        List<Vec3> eyes = MatrixPlacement.eyePositions();
        BlockPos occluder = null;
        double occluderDistance = Double.MAX_VALUE;

        for (double sx : BOX_SAMPLES) {
            double x = Mth.lerp(sx, box.minX, box.maxX);
            for (double sy : BOX_SAMPLES) {
                double y = Mth.lerp(sy, box.minY, box.maxY);
                for (double sz : BOX_SAMPLES) {
                    Vec3 point = new Vec3(x, y, Mth.lerp(sz, box.minZ, box.maxZ));
                    double distance = eye.distanceToSqr(point);
                    if (distance > rangeSq) continue;

                    for (Vec3 from : eyes) {
                        BlockPos blocker = firstOccluder(from, point, null, ignored);
                        if (blocker == null) return new Visibility(true, null);
                        if (distance < occluderDistance) {
                            occluderDistance = distance;
                            occluder = blocker;
                        }
                    }
                }
            }
        }
        return new Visibility(false, occluder);
    }

    public static boolean matrixVisible(BlockPos base, Vec3 eye, float range, List<BlockPos> ignored) {
        return matrixVisibility(base, eye, range, ignored).visible();
    }


    private static Vec3 facePoint(AABB bounds, BlockPos pos, Direction side, double u, double v) {
        Direction.Axis axis = side.getAxis();
        double x = pos.getX() + Mth.lerp(axis == Direction.Axis.X ? 0.5 : u, bounds.minX, bounds.maxX);
        double y = pos.getY() + Mth.lerp(axis == Direction.Axis.Y ? 0.5 : (axis == Direction.Axis.X ? u : v), bounds.minY, bounds.maxY);
        double z = pos.getZ() + Mth.lerp(axis == Direction.Axis.Z ? 0.5 : v, bounds.minZ, bounds.maxZ);

        switch (side) {
            case UP -> y = pos.getY() + bounds.maxY;
            case DOWN -> y = pos.getY() + bounds.minY;
            case NORTH -> z = pos.getZ() + bounds.minZ;
            case SOUTH -> z = pos.getZ() + bounds.maxZ;
            case WEST -> x = pos.getX() + bounds.minX;
            case EAST -> x = pos.getX() + bounds.maxX;
        }
        return jitterOnFace(new Vec3(x, y, z), side);
    }

    public static Vec3 jitterOnFace(Vec3 point, Direction side) {
        Direction.Axis axis = side.getAxis();
        return new Vec3(
                axis == Direction.Axis.X ? point.x : point.x + jitter(),
                axis == Direction.Axis.Y ? point.y : point.y + jitter(),
                axis == Direction.Axis.Z ? point.z : point.z + jitter());
    }

    public static boolean facesTowards(Vec3 eye, Vec3 point, Direction side) {
        Vec3 toEye = eye.subtract(point);
        double length = toEye.length();
        if (length < 1.0E-6) return false;
        return toEye.dot(new Vec3(side.getStepX(), side.getStepY(), side.getStepZ())) / length >= 0.2;
    }

    public static Vec3 clampedAim(BlockPos pos, Vec3 eye) {
        return new Vec3(
                Mth.clamp(eye.x, pos.getX() + 0.04, pos.getX() + 1.0 - 0.04) + jitter(),
                Mth.clamp(eye.y, pos.getY() + 0.04, pos.getY() + 1.0 - 0.04) + jitter(),
                Mth.clamp(eye.z, pos.getZ() + 0.04, pos.getZ() + 1.0 - 0.04) + jitter());
    }

    public static boolean isFaceFull(BlockState state, BlockPos pos, Direction side) {
        return Block.isFaceFull(state.getCollisionShape(mc.level, pos), side);
    }

    public static Vec3 crystalPos(BlockPos base) {
        return new Vec3(base.getX() + 0.5, base.getY() + 1.0, base.getZ() + 0.5);
    }

    public static AABB crystalSpace(BlockPos pos) {
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 2.0, pos.getZ() + 1.0);
    }

    public static AABB crystalBox(BlockPos base) {
        return new AABB(base.getX() - 0.5, base.getY() + 1.0, base.getZ() - 0.5, base.getX() + 1.5, base.getY() + 3.0, base.getZ() + 1.5);
    }

    public static Vec3 feetCenter(AABB box) {
        return new Vec3((box.minX + box.maxX) / 2.0, box.minY, (box.minZ + box.maxZ) / 2.0);
    }

    public static double distanceSq(Vec3 point, AABB box) {
        return point.distanceToSqr(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ));
    }

    public static double distanceSq(Vec3 point, BlockPos pos) {
        return distanceSq(point, new AABB(pos));
    }

    public static double distanceSqToCrystal(Vec3 point, Vec3 crystal) {
        return distanceSq(point, new AABB(crystal.x - 1.0, crystal.y, crystal.z - 1.0, crystal.x + 1.0, crystal.y + 2.0, crystal.z + 1.0));
    }


    public static boolean isMinable(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock) return false;
        return state.getBlock().defaultDestroyTime() >= 0.0F;
    }

    public static boolean isBreakable(BlockState state) {
        return isMinable(state) && !state.is(Blocks.SWEET_BERRY_BUSH);
    }

    public static boolean isCrystalBase(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    public static boolean nearFluid(BlockPos pos) {
        if (!mc.level.getFluidState(pos).isEmpty() || !mc.level.getFluidState(pos.above()).isEmpty()) return true;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (!mc.level.getFluidState(pos.relative(side)).isEmpty()) return true;
        }
        return false;
    }

    public static boolean isUnsupportedFalling(BlockPos pos, BlockState state) {
        return mc.level != null && state.getBlock() instanceof FallingBlock && mc.level.getBlockState(pos.below()).canBeReplaced();
    }

    public static boolean obsidianObstructed(BlockPos pos) {
        return !mc.level.isUnobstructed(Blocks.OBSIDIAN.defaultBlockState(), pos, CollisionContext.empty());
    }

    public static boolean isObstructingEntity(Entity entity) {
        return entity.isAlive() && !entity.isSpectator();
    }

    public static boolean hasEntityInCrystalSpace(BlockPos pos) {
        return !mc.level.getEntitiesOfClass(Entity.class, crystalSpace(pos), ExplosionDamage::isObstructingEntity).isEmpty();
    }


    private static double jitter() {
        return random(-0.03F, 0.03F);
    }

    private static double sampleJitter() {
        return random(-0.04F, 0.04F);
    }

    static float random(float min, float max) {
        return min + ThreadLocalRandom.current().nextFloat() * (max - min);
    }


    public record Visibility(boolean visible, BlockPos occluder) {}

    public record PlaceFace(BlockPos neighbor, Direction side, Vec3 aim) {}

    private record AimSearch(Vec3 aim, BlockPos occluder) {}

    private record FaceSearch(PlaceFace face, BlockPos occluder) {}

    private record ExposureKey(int entityId, long x, long y, long z, AABB box) {}
}
