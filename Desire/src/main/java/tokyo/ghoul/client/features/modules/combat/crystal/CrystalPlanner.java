package tokyo.ghoul.client.features.modules.combat.crystal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CrystalPlanner implements QuickImports {
    private static final Plan AWAIT_PLAN = new Plan(Action.AWAIT, null, null, null, 0.0F, 0.0F, null);
    private static final ExplosionDamage.Visibility VISIBLE = new ExplosionDamage.Visibility(true, null);

    private static final float LETHAL_BONUS = 1000.0F;
    private static final float ABOVE_FEET_PENALTY = 3.0F;
    private static final float COMMITTED_SELF_FACTOR = 1.15F;

    private CrystalPlanner() {}


    public static Plan findExplosion(Player target, AABB targetBox, Vec3 eye, Settings settings) {
        if (mc.player == null || mc.level == null || target == null) return null;

        boolean checkVisibility = !settings.explodeThroughWalls();
        double rangeSq = settings.explodeRange() * settings.explodeRange();
        AABB selfBox = mc.player.getBoundingBox();
        Plan best = null;
        float bestScore = -Float.MAX_VALUE;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof EndCrystal crystal)) continue;

            BlockPos base = crystal.blockPosition();
            if (ExplosionDamage.distanceSq(eye, crystal.getBoundingBox()) > rangeSq) continue;

            boolean matrix = settings.matrixSearch();
            if (matrix && !MatrixPlacement.withinCrystalReach(crystal.position())) continue;
            if ((checkVisibility || matrix) && ExplosionDamage.crystalAim(crystal, eye, settings.explodeRange(), matrix) == null) continue;

            Vec3 explosion = crystal.position();
            float enemy = ExplosionDamage.damage(explosion, target, targetBox);
            float self = ExplosionDamage.damage(explosion, mc.player, selfBox);
            if (enemy < settings.minEnemyDamage()) continue;
            if (!isSelfDamageAcceptable(self, enemy, settings, base.below())) continue;
            if (ExplosionDamage.maxDamageTo(explosion, settings.friends()) > settings.maxSelfDamage()) continue;

            float score = score(enemy, self, target, targetBox, explosion);
            if (score > bestScore) {
                bestScore = score;
                best = new Plan(Action.EXPLODE, base, null, crystal, enemy, self, null);
            }
        }
        return best;
    }


    public static Plan findPlacement(Player target, AABB targetBox, Vec3 eye, Settings settings, DigContext dig) {
        if (mc.player == null || mc.level == null || target == null) return null;

        Search search = new Search(target, targetBox, eye, settings, dig);
        int radius = Mth.ceil(settings.placeRange());
        BlockPos origin = mc.player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    search.visit(cursor);
                }
            }
        }
        return search.finish();
    }


    public static boolean isSelfDamageAcceptable(float self, float enemy, Settings settings, BlockPos base) {
        boolean committed = base != null && base.equals(settings.committedBase());
        return isSelfDamageAcceptable(self, committed ? enemy * COMMITTED_SELF_FACTOR : enemy, settings);
    }

    private static boolean isSelfDamageAcceptable(float self, float enemy, Settings settings) {
        if (self >= settings.selfHealthLimit()) return false;
        return self <= settings.maxSelfDamage() || self < enemy;
    }

    public static float score(float enemy, float self, LivingEntity target, AABB targetBox, Vec3 explosion) {
        float score = enemy - 2.0F * self;
        if (isAboveFeet(explosion, target, targetBox)) score -= ABOVE_FEET_PENALTY;
        return enemy >= target.getHealth() + target.getAbsorptionAmount() ? score + LETHAL_BONUS : score;
    }

    private static boolean isAboveFeet(Vec3 explosion, LivingEntity target, AABB targetBox) {
        boolean rising = !target.onGround() && target.getDeltaMovement().y > 0.08;
        return explosion.y > targetBox.minY + 0.5 && !rising;
    }


    private static Occupancy occupancy(BlockPos pos) {
        Occupancy result = Occupancy.FREE;
        for (Entity entity : mc.level.getEntitiesOfClass(Entity.class, ExplosionDamage.crystalSpace(pos), ExplosionDamage::isObstructingEntity)) {
            if (!(entity instanceof ItemEntity item) || item.onGround()) return Occupancy.BLOCKED;
            result = Occupancy.FALLING_ITEM;
        }
        return result;
    }

    private static ItemEntity groundedItem(BlockPos pos) {
        ItemEntity found = null;
        for (Entity entity : mc.level.getEntitiesOfClass(Entity.class, ExplosionDamage.crystalSpace(pos), ExplosionDamage::isObstructingEntity)) {
            if (!(entity instanceof ItemEntity item)) return null;
            if (item.onGround()) found = item;
        }
        return found;
    }

    private static boolean hasNonItemEntity(BlockPos pos) {
        return !mc.level.getEntitiesOfClass(Entity.class, ExplosionDamage.crystalSpace(pos),
                entity -> ExplosionDamage.isObstructingEntity(entity) && !(entity instanceof ItemEntity)).isEmpty();
    }


    private static int fluidAmount(BlockPos pos) {
        return mc.level.getFluidState(pos).getAmount();
    }

    private static boolean isOpened(BlockPos pos, List<BlockPos> dug, BlockPos cell, boolean cellAir) {
        return dug.contains(pos) || cellAir && pos.equals(cell);
    }

    private static Flood floodCheck(BlockPos base, List<BlockPos> dug) {
        BlockPos cell = base.above();
        boolean cellAir = !dug.contains(cell) && mc.level.getBlockState(cell).isAir();
        Map<BlockPos, Integer> levels = null;
        ArrayDeque<BlockPos> queue = null;

        for (BlockPos pos : dug) {
            int level = fluidAmount(pos);
            BlockPos above = pos.above();
            if (!dug.contains(above) && fluidAmount(above) > 0) level = 8;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(side);
                if (!dug.contains(neighbor)) level = Math.max(level, fluidAmount(neighbor) - 1);
            }
            if (level > 0) {
                if (levels == null) {
                    levels = new HashMap<>();
                    queue = new ArrayDeque<>();
                }
                spread(levels, queue, pos, level);
            }
        }

        if (levels == null) return Flood.DRY;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            int level = levels.get(pos);
            BlockPos below = pos.below();
            boolean fallsDown = isOpened(below, dug, cell, cellAir);
            if (fallsDown) spread(levels, queue, below, 8);

            if (!fallsDown && level > 1) {
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    BlockPos neighbor = pos.relative(side);
                    if (isOpened(neighbor, dug, cell, cellAir)) spread(levels, queue, neighbor, level - 1);
                }
            }
        }
        return levels.containsKey(cell) ? Flood.CELL : Flood.STREAM;
    }

    private static void spread(Map<BlockPos, Integer> levels, ArrayDeque<BlockPos> queue, BlockPos pos, int level) {
        Integer current = levels.get(pos);
        if (current == null || current < level) {
            levels.put(pos, level);
            queue.add(pos);
        }
    }


    private static final class Search {
        private final Player target;
        private final AABB targetBox;
        private final AABB selfBox;
        private final Vec3 eye;
        private final Settings settings;
        private final DigContext dig;
        private final int targetFeetY;
        private final double placeRangeSq;
        private final double explodeRangeSq;

        private final Best crystalReachable = new Best();
        private final Best obsidianReachable = new Best();
        private final Best crystalUnreachable = new Best();
        private final Best obsidianUnreachable = new Best();
        private final Best preferred = new Best();
        private final Fallbacks fallbacks = new Fallbacks();

        private ItemEntity blockingItem;
        private BlockPos blockingItemBase;
        private float blockingItemScore = -Float.MAX_VALUE;
        private boolean blockingItemPreferred;
        private float chosenScore = -Float.MAX_VALUE;
        private DigCandidate preferredDig;
        private DigCandidate bestDig;

        private Search(Player target, AABB targetBox, Vec3 eye, Settings settings, DigContext dig) {
            this.target = target;
            this.targetBox = targetBox;
            this.selfBox = mc.player.getBoundingBox();
            this.eye = eye;
            this.settings = settings;
            this.dig = dig;
            this.targetFeetY = Mth.floor(targetBox.minY);
            this.placeRangeSq = settings.placeRange() * settings.placeRange();
            this.explodeRangeSq = settings.explodeRange() * settings.explodeRange();
        }

        private void visit(BlockPos.MutableBlockPos pos) {
            if (!settings.canPlaceCrystal()) return;
            if (ExplosionDamage.distanceSq(eye, pos) > placeRangeSq) return;
            if (dig != null && dig.blacklist() != null && dig.blacklist().contains(pos)) return;
            if (dig != null && dig.pending() != null && (dig.pending().contains(pos) || dig.pending().contains(pos.above()))) return;

            Vec3 crystal = ExplosionDamage.crystalPos(pos);
            float ceiling = ExplosionDamage.damage(crystal, target, targetBox, 1.0F);
            if (ceiling < settings.minEnemyDamage()) return;

            boolean reachable = ExplosionDamage.distanceSqToCrystal(eye, crystal) <= explodeRangeSq
                    && (!settings.matrixSearch() || MatrixPlacement.withinCrystalReach(crystal));
            boolean canDig = reachable && pos.getY() < targetFeetY;
            BlockState state = mc.level.getBlockState(pos);

            if (ExplosionDamage.isCrystalBase(state)) {
                BlockPos base = pos.immutable();
                considerCrystal(base, crystal, reachable);
                if (canDig) considerDig(base, state, crystal);
            } else if (state.canBeReplaced()) {
                BlockPos base = pos.immutable();
                considerObsidian(base, crystal, reachable);
                if (canDig) considerDig(base, state, crystal);
            } else if (reachable && canDig && settings.canPlaceObsidian() && ExplosionDamage.isBreakable(state)) {
                considerDig(pos.immutable(), state, crystal);
            }
        }

        private void considerCrystal(BlockPos base, Vec3 crystal, boolean reachable) {
            BlockPos above = base.above();
            if (!mc.level.getBlockState(above).isAir()) return;

            Occupancy occupancy = occupancy(above);
            if (occupancy == Occupancy.BLOCKED) {
                considerBlockingItem(base, groundedItem(above), crystal);
                return;
            }

            boolean hidden = !settings.placeThroughWalls() && !ExplosionDamage.hasCrystalPlaceAim(base, eye, settings.placeRange());
            ExplosionDamage.Visibility visibility = hidden ? null : visibility(base);
            if (visibility != null && visibility.visible()) {
                float enemy = ExplosionDamage.damage(crystal, target, targetBox);
                float self = ExplosionDamage.damage(crystal, mc.player, selfBox);
                consider(Action.PLACE_CRYSTAL, base, crystal, enemy, self, occupancy, reachable);
            } else if (reachable) {
                considerOccluderDig(base, crystal, false, visibility);
            }
        }

        private void considerObsidian(BlockPos base, Vec3 crystal, boolean reachable) {
            if (!settings.canPlaceObsidian()) return;

            BlockPos above = base.above();
            if (!mc.level.getBlockState(above).isAir()) return;
            if (ExplosionDamage.obsidianObstructed(base)) return;

            Occupancy occupancy = occupancy(above);
            if (occupancy == Occupancy.BLOCKED) {
                considerBlockingItem(base, groundedItem(above), crystal);
                return;
            }

            ExplosionDamage.Visibility visibility = visibility(base);
            if (!visibility.visible()
                    || ExplosionDamage.findPlaceFace(base, eye, settings.placeRange(), settings.placeThroughWalls()) == null) {
                if (reachable) considerOccluderDig(base, crystal, true, visibility);
                return;
            }

            float enemy = ExplosionDamage.damageWithChanges(crystal, target, targetBox, base, null);
            float self = ExplosionDamage.damageWithChanges(crystal, mc.player, selfBox, base, null);
            consider(Action.PLACE_OBSIDIAN, base, crystal, enemy, self, occupancy, reachable);
        }

        private void consider(Action action, BlockPos base, Vec3 crystal, float enemy, float self, Occupancy occupancy, boolean reachable) {
            if (isRejectedByDamage(base, crystal, enemy, self)) return;

            float score = score(enemy, self, target, targetBox, crystal);
            Plan plan = new Plan(action, base, base, null, enemy, self, null);
            Best best = isPreferredBase(base) ? preferred : bucket(action, reachable);

            if (occupancy == Occupancy.FALLING_ITEM) {
                if (dig == null || dig.awaitTicks() < 2) best.offerAwait(score);
                return;
            }

            if (reachable && occupancy == Occupancy.FREE) fallbacks.offer(plan, crystal, score);
            best.offer(plan, score);
        }

        private Best bucket(Action action, boolean reachable) {
            if (action == Action.PLACE_CRYSTAL) return reachable ? crystalReachable : crystalUnreachable;
            return reachable ? obsidianReachable : obsidianUnreachable;
        }

        private boolean isRejectedByDamage(BlockPos base, Vec3 crystal, float enemy, float self) {
            if (enemy < settings.minEnemyDamage()) return true;
            if (!isSelfDamageAcceptable(self, enemy, settings, base)) return true;
            return ExplosionDamage.maxDamageTo(crystal, settings.friends()) > settings.maxSelfDamage();
        }

        private boolean isPreferredBase(BlockPos pos) {
            return dig != null && pos.equals(dig.preferredBase());
        }

        private ExplosionDamage.Visibility visibility(BlockPos base) {
            return settings.matrixSearch()
                    ? ExplosionDamage.matrixVisibility(base, eye, settings.placeRange(), List.of())
                    : VISIBLE;
        }

        private void considerBlockingItem(BlockPos base, ItemEntity item, Vec3 crystal) {
            if (item == null) return;

            float enemy = ExplosionDamage.damage(crystal, target, targetBox);
            float self = ExplosionDamage.damage(crystal, mc.player, selfBox);
            if (!isSelfDamageAcceptable(self, enemy, settings) || enemy < settings.minEnemyDamage()) return;

            float score = score(enemy, self, target, targetBox, crystal);
            boolean preferredBase = isPreferredBase(base);
            if (blockingItem != null && blockingItemPreferred && !preferredBase) return;
            if (blockingItem != null && blockingItemPreferred == preferredBase && score <= blockingItemScore) return;

            blockingItem = item;
            blockingItemBase = base;
            blockingItemScore = score;
            blockingItemPreferred = preferredBase;
        }


        private void considerDig(BlockPos base, BlockState baseState, Vec3 crystal) {
            BlockPos above = base.above();
            BlockState aboveState = mc.level.getBlockState(above);
            boolean needsObsidian = !ExplosionDamage.isCrystalBase(baseState);
            if (needsObsidian && !settings.canPlaceObsidian()) return;

            List<BlockPos> blocks = new ArrayList<>(2);
            if (!aboveState.isAir()) blocks.add(above);
            if (needsObsidian && !baseState.canBeReplaced()) blocks.add(base);
            if (blocks.isEmpty()) return;
            if (hasNonItemEntity(above)) return;

            float ticks = 0.0F;
            boolean active = false;
            for (int i = 0; i < blocks.size(); i++) {
                BlockPos block = blocks.get(i);
                BlockState state = block.equals(above) ? aboveState : baseState;
                if (!canDig(block, state, blocks.subList(0, i))) return;

                ticks += remainingBreakTicks(block, state);
                if (block.equals(dig.activeBlock())) active = true;
            }

            Flood flood = floodCheck(base, blocks);
            if (flood == Flood.CELL) return;
            if (needsObsidian && ExplosionDamage.findPlaceFace(base, eye, settings.placeRange(), settings.placeThroughWalls(), blocks) == null) return;

            boolean hidden = !needsObsidian && !settings.placeThroughWalls()
                    && !ExplosionDamage.hasCrystalPlaceAim(base, eye, settings.placeRange(), blocks);
            if (hidden || !isMatrixVisible(base, blocks)) return;

            considerDigPlan(blocks, base, ticks, active, crystal, needsObsidian ? base : null,
                    aboveState.isAir() ? null : above, flood == Flood.STREAM);
        }

        private void considerOccluderDig(BlockPos base, Vec3 crystal, boolean forObsidian, ExplosionDamage.Visibility visibility) {
            if (!settings.matrixSearch() || dig == null || !dig.allowed()) return;

            float range = settings.placeRange();
            BlockPos occluder = null;
            if (forObsidian) {
                occluder = ExplosionDamage.placeFaceOccluder(base, eye, range, settings.placeThroughWalls());
            } else if (!settings.placeThroughWalls()) {
                occluder = ExplosionDamage.crystalPlaceOccluder(base, eye, range);
            }
            if (occluder == null) occluder = (visibility != null ? visibility : visibility(base)).occluder();
            if (occluder == null || occluder.equals(base) || occluder.equals(base.above())) return;

            BlockState state = mc.level.getBlockState(occluder);
            if (!canDig(occluder, state, List.of())) return;

            List<BlockPos> blocks = List.of(occluder);
            Flood flood = floodCheck(base, blocks);
            if (flood == Flood.CELL) return;

            boolean reachable = ExplosionDamage.matrixVisible(base, eye, range, blocks) && (forObsidian
                    ? ExplosionDamage.findPlaceFace(base, eye, range, settings.placeThroughWalls(), blocks) != null
                    : settings.placeThroughWalls() || ExplosionDamage.hasCrystalPlaceAim(base, eye, range, blocks));
            if (!reachable) return;

            considerDigPlan(blocks, base, remainingBreakTicks(occluder, state), occluder.equals(dig.activeBlock()),
                    crystal, forObsidian ? base : null, occluder, flood == Flood.STREAM);
        }

        private void considerDigPlan(List<BlockPos> blocks, BlockPos base, float ticks, boolean active, Vec3 crystal,
                                     BlockPos placed, BlockPos removed, boolean stream) {
            if (ticks * dig.targetStep() > 1.0F) return;

            float enemy = ExplosionDamage.damageWithChanges(crystal, target, targetBox, placed, removed);
            float self = ExplosionDamage.damageWithChanges(crystal, mc.player, selfBox, placed, removed);
            if (isRejectedByDamage(base, crystal, enemy, self)) return;

            float score = score(enemy, self, target, targetBox, crystal);
            DigCandidate candidate = new DigCandidate(blocks, base, ticks, active, enemy, self, score, placed != null, stream);
            if (isPreferredBase(base)) {
                if (candidate.isBetterThan(preferredDig)) preferredDig = candidate;
            } else if (candidate.isBetterThan(bestDig)) {
                bestDig = candidate;
            }
        }

        private boolean canDig(BlockPos pos, BlockState state, List<BlockPos> alreadyDug) {
            if (dig == null || !dig.allowed()) return false;
            if (dig.pending() != null && dig.pending().contains(pos)) return false;
            if (!ExplosionDamage.isBreakable(state) || ExplosionDamage.isUnsupportedFalling(pos, state)) return false;
            if (dig.blacklist() != null && dig.blacklist().contains(pos)) return false;
            if (dig.keep() != null && dig.keep().contains(pos)) return false;
            if (CrystalUtil.breakTicks(state, pos) > 400.0F) return false;

            if (settings.digThroughWalls()) return ExplosionDamage.distanceSq(eye, pos) <= placeRangeSq;
            return ExplosionDamage.hasCrystalPlaceAim(pos, eye, settings.placeRange(), alreadyDug);
        }

        private boolean isMatrixVisible(BlockPos base, List<BlockPos> ignored) {
            return !settings.matrixSearch() || ExplosionDamage.matrixVisible(base, eye, settings.placeRange(), ignored);
        }

        private float remainingBreakTicks(BlockPos pos, BlockState state) {
            float ticks = CrystalUtil.breakTicks(state, pos);
            if (pos.equals(dig.activeBlock())) ticks *= 1.0F - Mth.clamp(dig.activeProgress(), 0.0F, 1.0F);
            return ticks;
        }


        private Plan finish() {
            Plan plan = bestPlan();
            if (plan == null) plan = crystalUnreachable.plan();
            if (plan == null) plan = obsidianUnreachable.plan();

            if (shouldClearItem(plan)) {
                Plan alternative = itemAlternative();
                if (alternative != null) plan = alternative;
            }
            return plan;
        }

        private Plan bestPlan() {
            Plan placement = bestPlacement();
            DigCandidate digCandidate = preferredDig != null ? preferredDig : bestDig;
            if (digCandidate == null) return placement;
            if (placement != null && !digCandidate.isMuchSaferThan(placement)) return placement;

            chosenScore = digCandidate.score();
            return digCandidate.toPlan();
        }

        private Plan bestPlacement() {
            float preferredScore = preferred.score();
            if (preferredScore != -Float.MAX_VALUE) {
                chosenScore = preferredScore;
                return preferred.plan();
            }

            float crystalScore = crystalReachable.score();
            float obsidianScore = obsidianReachable.score();
            if (crystalScore == -Float.MAX_VALUE && obsidianScore == -Float.MAX_VALUE) return null;

            float obsidianAdjusted = obsidianScore - 3.0F;
            chosenScore = Math.max(crystalScore, obsidianAdjusted);
            return crystalScore >= obsidianAdjusted ? crystalReachable.plan() : obsidianReachable.plan();
        }

        private boolean shouldClearItem(Plan plan) {
            if (blockingItem == null) return false;
            if (blockingItemPreferred || plan == null) return true;
            if (plan.action() == Action.DIG || plan.action() == Action.AWAIT) return true;
            return blockingItemScore > chosenScore + 5.0F;
        }

        private Plan itemAlternative() {
            Plan plan = fallbacks.find(blockingItem, blockingItemBase);
            if (plan == null) return null;
            return new Plan(plan.action(), plan.pos(), plan.base(), null, plan.enemyDamage(), plan.selfDamage(), blockingItemBase);
        }
    }

    private static final class Best {
        private Plan plan;
        private float score = -Float.MAX_VALUE;
        private float awaitScore = -Float.MAX_VALUE;

        private void offer(Plan candidate, float candidateScore) {
            if (candidateScore > score) {
                score = candidateScore;
                plan = candidate;
            }
        }

        private void offerAwait(float candidateScore) {
            if (candidateScore > awaitScore) awaitScore = candidateScore;
        }

        private float score() {
            return Math.max(score, awaitScore);
        }

        private Plan plan() {
            return awaitScore > score ? AWAIT_PLAN : plan;
        }
    }

    private static final class Fallbacks {
        private static final int LIMIT = 8;

        private final List<Plan> plans = new ArrayList<>(LIMIT + 1);
        private final List<Vec3> crystals = new ArrayList<>(LIMIT + 1);
        private final List<Float> scores = new ArrayList<>(LIMIT + 1);

        private void offer(Plan plan, Vec3 crystal, float score) {
            int index = 0;
            while (index < scores.size() && scores.get(index) >= score) index++;
            if (index >= LIMIT) return;

            plans.add(index, plan);
            crystals.add(index, crystal);
            scores.add(index, score);
            if (plans.size() > LIMIT) {
                plans.remove(LIMIT);
                crystals.remove(LIMIT);
                scores.remove(LIMIT);
            }
        }

        private Plan find(ItemEntity item, BlockPos itemBase) {
            Plan crystal = find(item, itemBase, Action.PLACE_CRYSTAL);
            return crystal != null ? crystal : find(item, itemBase, Action.PLACE_OBSIDIAN);
        }

        private Plan find(ItemEntity item, BlockPos itemBase, Action action) {
            AABB itemBox = item.getBoundingBox();
            for (int i = 0; i < plans.size(); i++) {
                Plan plan = plans.get(i);
                if (plan.action() != action) continue;

                BlockPos pos = plan.pos();
                boolean sameColumn = pos.getX() == itemBase.getX() && pos.getZ() == itemBase.getZ()
                        && Math.abs(pos.getY() - itemBase.getY()) <= 2;
                if (sameColumn) continue;

                AABB space = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, pos.getY() + 3.0, pos.getZ() + 1.0);
                if (space.intersects(itemBox)) continue;

                BlockPos placed = action == Action.PLACE_OBSIDIAN ? pos : null;
                if (ExplosionDamage.damageWithBlock(crystals.get(i), item, placed) >= 5.0F) return plan;
            }
            return null;
        }
    }

    private record DigCandidate(List<BlockPos> blocks, BlockPos base, float ticks, boolean active, float enemyDamage,
                                float selfDamage, float baseScore, boolean needsObsidian, boolean stream) {
        private float score() {
            return baseScore
                    - ticks * 0.25F
                    - 3.0F * (blocks.size() - 1)
                    - (needsObsidian ? 3.0F : 0.0F)
                    - (stream ? 6.0F : 0.0F)
                    + (active ? 8.0F : 0.0F);
        }

        private boolean isBetterThan(DigCandidate other) {
            return other == null || score() > other.score();
        }

        private boolean isMuchSaferThan(Plan plan) {
            return selfDamage + 2.0F < plan.selfDamage();
        }

        private Plan toPlan() {
            return new Plan(Action.DIG, blocks.get(0), base, null, enemyDamage, selfDamage, null);
        }
    }

    private enum Occupancy {FREE, FALLING_ITEM, BLOCKED}

    private enum Flood {DRY, STREAM, CELL}

    public enum Action {EXPLODE, PLACE_CRYSTAL, PLACE_OBSIDIAN, DIG, AWAIT}

    public record Plan(Action action, BlockPos pos, BlockPos base, EndCrystal crystal, float enemyDamage, float selfDamage, BlockPos clears) {}

    public record Settings(float placeRange, float explodeRange, float minEnemyDamage, float maxSelfDamage, float selfHealthLimit,
                           boolean canPlaceCrystal, boolean canPlaceObsidian, boolean placeThroughWalls, boolean digThroughWalls,
                           boolean explodeThroughWalls, List<? extends LivingEntity> friends, boolean matrixSearch,
                           BlockPos committedBase) {}

    public record DigContext(boolean allowed, Set<BlockPos> blacklist, Set<BlockPos> keep, BlockPos activeBlock,
                             float activeProgress, float targetStep, BlockPos preferredBase, Set<BlockPos> pending,
                             int awaitTicks) {}
}
