package tokyo.ghoul.client.features.modules.combat.crystal;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.Arrays;

public final class CachedBlockView implements BlockGetter, QuickImports {
    public static final CachedBlockView INSTANCE = new CachedBlockView();

    private static final int SIZE = 8192;
    private static final long SHAPE_SALT = -7046029254386353131L;

    private final long[] stateKeys = new long[SIZE];
    private final BlockState[] states = new BlockState[SIZE];
    private final long[] shapeKeys = new long[SIZE];
    private final VoxelShape[] shapes = new VoxelShape[SIZE];
    private final int[] shapeGenerations = new int[SIZE];

    private int cachedTick = Integer.MIN_VALUE;
    private Object cachedLevel;
    private int generation;
    private CollisionContext lastContext;

    private Vec3 rayFrom;
    private Vec3 rayTo;
    private CollisionContext rayContext;
    private boolean skipInside;

    private CachedBlockView() {}

    public void invalidate() {
        Arrays.fill(states, null);
        generation++;
    }

    public BlockPos rayBlock(Vec3 from, Vec3 to, CollisionContext context, boolean skipInside) {
        if (mc.level == null) return null;

        refresh();
        if (lastContext != context) {
            lastContext = context;
            generation++;
        }

        this.rayFrom = from;
        this.rayTo = to;
        this.rayContext = context;
        this.skipInside = skipInside;
        return BlockGetter.traverseBlocks(from, to, this, CachedBlockView::firstSolid, view -> null);
    }

    public BlockPos rayBlock(Vec3 from, Vec3 to, CollisionContext context) {
        return rayBlock(from, to, context, false);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (mc.level == null) return Blocks.AIR.defaultBlockState();

        refresh();
        long key = pos.asLong();
        int slot = (int) HashCommon.mix(key) & SIZE - 1;
        BlockState cached = states[slot];
        if (cached != null && stateKeys[slot] == key) return cached;

        BlockState state = mc.level.getBlockState(pos);
        stateKeys[slot] = key;
        states[slot] = state;
        return state;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return mc.level == null ? null : mc.level.getBlockEntity(pos);
    }

    @Override
    public int getHeight() {
        return mc.level == null ? 0 : mc.level.getHeight();
    }

    @Override
    public int getMinY() {
        return mc.level == null ? 0 : mc.level.getMinY();
    }

    private void refresh() {
        int tick = mc.player == null ? Integer.MIN_VALUE : mc.player.tickCount;
        if (tick != cachedTick || cachedLevel != mc.level) {
            cachedTick = tick;
            cachedLevel = mc.level;
            invalidate();
        }
    }

    private static BlockPos firstSolid(CachedBlockView view, BlockPos pos) {
        VoxelShape shape = view.collisionShape(pos);
        if (shape.isEmpty()) return null;

        if (shape == Shapes.block()) {
            return view.hitsCube(pos) ? pos.immutable() : null;
        }

        BlockHitResult hit = shape.clip(view.rayFrom, view.rayTo, pos);
        if (hit == null) return null;
        return view.skipInside && hit.isInside() ? null : pos.immutable();
    }

    private VoxelShape collisionShape(BlockPos pos) {
        long key = pos.asLong();
        int slot = (int) HashCommon.mix(key ^ SHAPE_SALT) & SIZE - 1;
        if (shapeGenerations[slot] == generation && shapeKeys[slot] == key && shapes[slot] != null) {
            return shapes[slot];
        }

        VoxelShape shape = getBlockState(pos).getCollisionShape(this, pos, rayContext);
        shapeKeys[slot] = key;
        shapes[slot] = shape;
        shapeGenerations[slot] = generation;
        return shape;
    }

    private boolean hitsCube(BlockPos pos) {
        Vec3 from = rayFrom;
        double dx = rayTo.x - from.x;
        double dy = rayTo.y - from.y;
        double dz = rayTo.z - from.z;
        if (dx * dx + dy * dy + dz * dz < 1.0E-7) return false;

        double minX = pos.getX(), minY = pos.getY(), minZ = pos.getZ();
        if (insideUnit(from.x + dx * 0.001 - minX) && insideUnit(from.y + dy * 0.001 - minY) && insideUnit(from.z + dz * 0.001 - minZ)) {
            return !skipInside;
        }

        double maxX = minX + 1.0, maxY = minY + 1.0, maxZ = minZ + 1.0;

        if (dx > 1.0E-7 ? crossesFace(minX, from.x, dx, from.y, dy, minY, maxY, from.z, dz, minZ, maxZ)
                : dx < -1.0E-7 && crossesFace(maxX, from.x, dx, from.y, dy, minY, maxY, from.z, dz, minZ, maxZ)) {
            return true;
        }
        if (dy > 1.0E-7 ? crossesFace(minY, from.y, dy, from.z, dz, minZ, maxZ, from.x, dx, minX, maxX)
                : dy < -1.0E-7 && crossesFace(maxY, from.y, dy, from.z, dz, minZ, maxZ, from.x, dx, minX, maxX)) {
            return true;
        }
        return dz > 1.0E-7 ? crossesFace(minZ, from.z, dz, from.x, dx, minX, maxX, from.y, dy, minY, maxY)
                : dz < -1.0E-7 && crossesFace(maxZ, from.z, dz, from.x, dx, minX, maxX, from.y, dy, minY, maxY);
    }

    private static boolean insideUnit(double value) {
        return Mth.floor(Mth.clamp(value, -1.0, 1.0)) == 0;
    }

    private static boolean crossesFace(double plane, double start, double delta,
                                       double startA, double deltaA, double minA, double maxA,
                                       double startB, double deltaB, double minB, double maxB) {
        double t = (plane - start) / delta;
        double a = startA + t * deltaA;
        double b = startB + t * deltaB;
        return 0.0 < t && t < 1.0
                && minA - 1.0E-7 < a && a < maxA + 1.0E-7
                && minB - 1.0E-7 < b && b < maxB + 1.0E-7;
    }
}
