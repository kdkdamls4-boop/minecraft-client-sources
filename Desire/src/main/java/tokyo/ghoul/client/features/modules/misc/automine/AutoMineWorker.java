package tokyo.ghoul.client.features.modules.misc.automine;

import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.misc.AutoMineModule;
import tokyo.ghoul.client.services.AnarchyReconnectService;
import tokyo.ghoul.client.services.AnarchyReconnectService.Target;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoMineWorker implements QuickImports {

    public record Mine(String name, String warp, BlockPos min, BlockPos max) {
        static Mine of(String name, String warp, int x1, int y1, int z1, int x2, int y2, int z2) {
            return new Mine(name, warp,
                    new BlockPos(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2)),
                    new BlockPos(Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2)));
        }

        boolean contains(BlockPos pos) {
            return pos.getX() >= min.getX() && pos.getX() <= max.getX()
                    && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                    && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        }

        BlockPos topCenter() {
            return new BlockPos((min.getX() + max.getX()) / 2, max.getY() + 1, (min.getZ() + max.getZ()) / 2);
        }

        double horizontalDistance(Vec3 pos) {
            double dx = Math.max(Math.max(min.getX() - pos.x, 0.0), pos.x - (max.getX() + 1));
            double dz = Math.max(Math.max(min.getZ() - pos.z, 0.0), pos.z - (max.getZ() + 1));
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    public static final List<Mine> MINES = List.of(
            Mine.of("Обычная шахта", "warp mine", -154, 61, -188, -174, 52, -168),
            Mine.of("Эндер шахта", "warp mine2", -147, 62, -141, -127, 51, -161));


    private static final Set<Item> JUNK = Set.of(
            Items.GOLD_NUGGET, Items.NETHERRACK, Items.SMOOTH_STONE, Items.NETHER_BRICKS, Items.NETHER_BRICK);

    private static final Pattern DRILL = Pattern.compile("бур\\s+(iii|ii|i|\\d)\\b");

    private static final long WORLD_TIMEOUT_MS = 25_000L;
    private static final long WORLD_SETTLE_MS = 1_500L;
    private static final long WARP_TIMEOUT_MS = 6_000L;
    private static final long WARP_SETTLE_MS = 700L;
    private static final long RETRY_DELAY_MS = 3_000L;
    private static final double MINE_RADIUS = 96.0;
    private static final long BLOCK_TIMEOUT_MS = 7_000L;
    private static final long STUCK_MS = 3_000L;
    private static final long TUNNEL_STUCK_MS = 5_000L;
    private static final long REPLAN_MS = 4_000L;
    private static final int MAX_REPLANS = 3;
    private static final int MAX_PATH_NODES = 4_000;
    private static final int SCAN_INTERVAL = 20;
    private static final int DIG_DEPTH = 3;
    private static final double SPRINT_DISTANCE = 3.0;

    private enum Stage {
        START, RECONNECT, WAIT_WORLD, WARP, WAIT_WARP, MINE, FINISHED
    }

    private enum Phase {
        GO_START, DESCEND, TUNNEL
    }

    private final AnarchyReconnectService rct;
    private final boolean ownsRct;
    private final MineInput input = new MineInput();
    private final Random random = new Random();
    private final List<String> messages = new ArrayList<>();
    private final Map<ItemStack, Integer> drillCache = new WeakHashMap<>();

    private Stage stage = Stage.START;
    private long stageAtMs;
    private int anarchy;
    private int mineIndex;
    private int reconnectAttempts;
    private int warpAttempts;

    private Object levelRef;
    private long levelChangedAtMs;
    private Vec3 warpFrom;
    private long warpMovedAtMs;

    private final Set<BlockPos> ignored = new HashSet<>();
    private int remaining;
    private boolean unloaded;
    private int scanCooldown;
    private int dropCooldown;
    private boolean dumping;
    private BlockPos target;
    private long targetSinceMs;

    private Phase phase;
    private int startX;
    private int laneZ;
    private int dirX;
    private int descendFromY;
    private int descendToY;
    private boolean firstDescent;
    private int tunnelX;
    private long tunnelProgressAtMs;

    private BlockPos goal;
    private List<BlockPos> path = List.of();
    private int pathIndex;
    private long pathAtMs;
    private int replans;
    private Vec3 stuckFrom;
    private long stuckAtMs;

    private boolean hasRotation;
    private float targetYaw;
    private float targetPitch;
    private long lastFrameNs;
    private float smoothness = 10.0F;
    private long smoothnessAtMs;
    private float yawVelocity;
    private float pitchVelocity;
    private float yawCarry;
    private float pitchCarry;

    public AutoMineWorker(AnarchyReconnectService rct, boolean ownsRct) {
        this.rct = rct;
        this.ownsRct = ownsRct;
    }


    public void tick() {
        if (stage == Stage.FINISHED) return;
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) return;

        if (ownsRct) rct.tick();
        if (rct.isRunning()) {
            if (stage == Stage.WAIT_WORLD) stageAtMs = System.currentTimeMillis();
            idle(player);
            return;
        }

        switch (stage) {
            case START -> start(player);
            case RECONNECT -> reconnect(player);
            case WAIT_WORLD -> waitWorld(player);
            case WARP -> warp(player);
            case WAIT_WARP -> waitWarp(player);
            case MINE -> mine(player);
            default -> {
            }
        }
    }

    public void stop() {
        if (rct.isRunning() && stage != Stage.MINE) rct.stop();
        release();
        stage = Stage.FINISHED;
    }

    public boolean isFinished() {
        return stage == Stage.FINISHED;
    }

    public List<String> drainMessages() {
        if (messages.isEmpty()) return List.of();
        List<String> out = new ArrayList<>(messages);
        messages.clear();
        return out;
    }


    private void start(LocalPlayer player) {
        int slot = findPickaxe(player);
        if (slot == -1) {
            finish("Нет кирки в инвентаре");
            return;
        }
        int drill = drillLevel(player.getInventory().getItem(slot));
        log("Кирка: " + switch (drill) {
            case 0 -> "без бура";
            case 1 -> "Бур I (3x1)";
            default -> "Бур " + "I".repeat(Math.min(drill, 3)) + " (3x3)";
        });
        reconnectAttempts = 0;
        warpAttempts = 0;
        int current = ownsRct ? -1 : rct.currentHolyNumber();
        if (current > 0 && !AnarchyReconnectService.BLOCKED_HOLY_ANARCHIES.contains(current)) {
            anarchy = current;
            int nearby = nearbyMine(player);
            if (nearby != -1) {
                mineIndex = nearby;
                log("Уже на шахте (анархия " + anarchy + ") — копаю");
                startMine();
            } else {
                mineIndex = 0;
                log("Анархия " + anarchy + " — /" + currentMine().warp());
                setStage(Stage.WARP);
            }
            return;
        }
        anarchy = nextAllowed(AutoMineModule.getInstance().firstAnarchy() - 1, false);
        if (anarchy == -1) {
            finish("Все анархии из диапазона в списке пропуска");
            return;
        }
        mineIndex = 0;
        setStage(Stage.RECONNECT);
    }

    private int nearbyMine(LocalPlayer player) {
        Vec3 pos = player.position();
        for (int i = 0; i < MINES.size(); i++) {
            if (MINES.get(i).horizontalDistance(pos) < 2.0) return i;
        }
        return MINES.get(0).horizontalDistance(pos) < 48.0 ? 0 : -1;
    }

    private void reconnect(LocalPlayer player) {
        idle(player);
        if (reconnectAttempts > 0 && !elapsed(RETRY_DELAY_MS)) return;

        levelRef = null;
        rct.start(Target.AUTO, anarchy);
        if (!rct.isRunning()) {
            failReconnect("не удалось начать .rct " + anarchy);
            return;
        }
        log("Захожу на анархию " + anarchy);
        setStage(Stage.WAIT_WORLD);
    }

    private void waitWorld(LocalPlayer player) {
        idle(player);
        long now = System.currentTimeMillis();
        if (levelRef == null) {
            levelRef = mc.level;
            levelChangedAtMs = 0L;
        } else if (levelChangedAtMs == 0L && mc.level != levelRef) {
            levelChangedAtMs = now;
        }

        if (levelChangedAtMs != 0L && now - levelChangedAtMs >= WORLD_SETTLE_MS) {
            reconnectAttempts = 0;
            warpAttempts = 0;
            mineIndex = 0;
            setStage(Stage.WARP);
        } else if (elapsed(WORLD_TIMEOUT_MS)) {
            failReconnect("не дождался входа на анархию " + anarchy);
        }
    }

    private void warp(LocalPlayer player) {
        idle(player);
        if (warpAttempts > 0 && !elapsed(RETRY_DELAY_MS)) return;

        warpFrom = player.position();
        levelRef = mc.level;
        warpMovedAtMs = 0L;
        player.connection.sendCommand(currentMine().warp());
        setStage(Stage.WAIT_WARP);
    }

    private void waitWarp(LocalPlayer player) {
        idle(player);
        long now = System.currentTimeMillis();
        if (warpMovedAtMs == 0L && (mc.level != levelRef || player.position().distanceTo(warpFrom) > 3.0)) {
            warpMovedAtMs = now;
        }
        if (warpMovedAtMs != 0L && now - warpMovedAtMs >= WARP_SETTLE_MS) {
            startMine();
            return;
        }
        if (!elapsed(WARP_TIMEOUT_MS)) return;

        if (currentMine().horizontalDistance(player.position()) < MINE_RADIUS) {
            startMine();
            return;
        }
        warpAttempts++;
        if (warpAttempts >= 3) {
            failReconnect("/" + currentMine().warp() + " не сработал на анархии " + anarchy);
        } else {
            setStage(Stage.WARP);
        }
    }

    private void startMine() {
        ignored.clear();
        resetMining();
        scanCooldown = 0;
        phase = null;
        setStage(Stage.MINE);
    }

    private void mine(LocalPlayer player) {
        Mine mine = currentMine();
        if (player.isDeadOrDying()) {
            idle(player);
            return;
        }
        if (mine.horizontalDistance(player.position()) > MINE_RADIUS) {
            stopBreaking();
            warpAttempts = 0;
            setStage(Stage.WARP);
            return;
        }

        if (--scanCooldown <= 0) {
            scan(mine);
            scanCooldown = SCAN_INTERVAL;
        }
        if (unloaded) {
            walk(player, mine, mine.topCenter(), null);
            return;
        }
        if (remaining == 0) {
            finishMine(player, mine, "раскопана");
            return;
        }
        if (!holdPickaxe(player)) {
            finish("Кирка закончилась");
            return;
        }
        if (dropJunk(player)) return;

        if (phase == null) plan(player, mine);
        switch (phase) {
            case GO_START -> goStart(player, mine);
            case DESCEND -> descend(player, mine);
            case TUNNEL -> tunnel(player, mine);
        }
    }

    private void finishMine(LocalPlayer player, Mine mine, String reason) {
        stopBreaking();
        idle(player);
        mineIndex++;
        if (mineIndex < MINES.size()) {
            log(mine.name() + " " + reason + " — /" + currentMine().warp());
            warpAttempts = 0;
            setStage(Stage.WARP);
        } else {
            log(mine.name() + " " + reason + " — следующая анархия");
            nextAnarchy();
        }
    }

    private void nextAnarchy() {
        anarchy = nextAllowed(anarchy, AutoMineModule.getInstance().loop.getValue());
        if (anarchy == -1) {
            finish("Все анархии пройдены");
            return;
        }
        reconnectAttempts = 0;
        mineIndex = 0;
        setStage(Stage.RECONNECT);
    }

    private static int nextAllowed(int from, boolean loop) {
        AutoMineModule module = AutoMineModule.getInstance();
        int first = module.firstAnarchy();
        int last = module.lastAnarchy();
        int number = from;
        for (int i = 0; i <= last - first + 1; i++) {
            number++;
            if (number > last) {
                if (!loop) return -1;
                number = first;
            }
            if (!AnarchyReconnectService.BLOCKED_HOLY_ANARCHIES.contains(number)) return number;
        }
        return -1;
    }

    private void failReconnect(String reason) {
        reconnectAttempts++;
        if (reconnectAttempts > 2) {
            log(reason + " — пропускаю");
            nextAnarchy();
        } else {
            log(reason + " — повтор");
            setStage(Stage.RECONNECT);
        }
    }

    private void finish(String reason) {
        log(reason);
        release();
        stage = Stage.FINISHED;
    }


    private void plan(LocalPlayer player, Mine mine) {
        BlockPos min = mine.min();
        BlockPos max = mine.max();
        boolean fromMinX = Math.abs(player.getX() - min.getX()) <= Math.abs(player.getX() - (max.getX() + 1));
        boolean fromMinZ = Math.abs(player.getZ() - min.getZ()) <= Math.abs(player.getZ() - (max.getZ() + 1));
        startX = fromMinX ? min.getX() + 1 : max.getX() - 1;
        laneZ = fromMinZ ? min.getZ() + 1 : max.getZ() - 1;
        dirX = fromMinX ? 1 : -1;
        phase = Phase.GO_START;
        resetWalk();
        log(mine.name() + ": иду в угол " + startX + " " + laneZ);
    }

    private void goStart(LocalPlayer player, Mine mine) {
        BlockPos feet = player.blockPosition();
        if (player.onGround() && feet.getX() == startX && feet.getZ() == laneZ) {
            beginDescent(feet, true);
            return;
        }
        BlockPos column = new BlockPos(startX, surfaceFeet(mine, startX, laneZ), laneZ);
        walk(player, mine, column, pos -> pos.getX() == startX && pos.getZ() == laneZ);
    }

    private void beginDescent(BlockPos feet, boolean first) {
        phase = Phase.DESCEND;
        firstDescent = first;
        descendFromY = feet.getY();
        descendToY = Math.max(feet.getY() - DIG_DEPTH, currentMine().min().getY());
        stopBreaking();
    }

    private void descend(LocalPlayer player, Mine mine) {
        if (!player.onGround()) {
            idle(player);
            return;
        }
        BlockPos feet = player.blockPosition();
        BlockPos below = feet.below();
        boolean bottom = feet.getY() <= descendToY || !mine.contains(below) || !isMinable(below) && !passable(below);
        if (bottom) {
            if (feet.getY() >= descendFromY && !firstDescent) {
                finishMine(player, mine, "пройдена до дна");
                return;
            }
            startTunnel(feet);
            return;
        }
        if (!centerOn(player, feet)) return;
        if (!isMinable(below)) {
            idle(player);
            return;
        }
        Vec3 aim = new Vec3(below.getX() + 0.5 + jitter(0.15), below.getY() + 0.98, below.getZ() + 0.5 + jitter(0.15));
        breakAt(player, below, aim);
    }

    private void startTunnel(BlockPos feet) {
        phase = Phase.TUNNEL;
        laneZ = feet.getZ();
        tunnelX = feet.getX();
        tunnelProgressAtMs = System.currentTimeMillis();
        stopBreaking();
    }

    private void tunnel(LocalPlayer player, Mine mine) {
        if (!player.onGround()) {
            idle(player);
            return;
        }
        BlockPos feet = player.blockPosition();
        long now = System.currentTimeMillis();
        if (feet.getX() != tunnelX) {
            tunnelX = feet.getX();
            tunnelProgressAtMs = now;
        }

        BlockPos ore = findOre(player, mine, feet);
        if (ore != null) {
            tunnelProgressAtMs = now;
            breakAt(player, ore, Vec3.atCenterOf(ore));
            return;
        }

        BlockPos next = new BlockPos(feet.getX() + dirX, feet.getY(), laneZ);
        if (next.getX() < mine.min().getX() || next.getX() > mine.max().getX() || now - tunnelProgressAtMs > TUNNEL_STUCK_MS) {
            rowDone(player, mine, feet);
            return;
        }

        double faceX = next.getX() + 0.5 - dirX * 0.49;
        BlockPos head = next.above();
        if (isMinable(head)) {
            breakAt(player, head, new Vec3(faceX, head.getY() + 0.5 + jitter(0.2), laneZ + 0.5 + jitter(0.2)));
            return;
        }
        if (isMinable(next)) {
            breakAt(player, next, new Vec3(faceX, next.getY() + 0.5 + jitter(0.2), laneZ + 0.5 + jitter(0.2)));
            return;
        }

        stopBreaking();
        Vec3 point = new Vec3(next.getX() + 0.5, next.getY(), laneZ + 0.5);
        float yaw = rotationTo(player.getEyePosition(), point)[0];
        rotate(player, yaw, 15.0F);
        input.release();
        input.forward = Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) < 40.0F;
        install(player);
    }

    private void rowDone(LocalPlayer player, Mine mine, BlockPos feet) {
        stopBreaking();
        idle(player);
        dirX = -dirX;
        if (feet.getY() <= mine.min().getY()) {
            finishMine(player, mine, "пройдена до дна");
            return;
        }
        beginDescent(feet, false);
    }

    private BlockPos findOre(LocalPlayer player, Mine mine, BlockPos feet) {
        Vec3 eye = player.getEyePosition();
        double reach = player.blockInteractionRange() - 0.3;
        double reachSqr = reach * reach;
        if (target != null && isOre(target) && isMinable(target) && eye.distanceToSqr(Vec3.atCenterOf(target)) <= reachSqr) {
            return target;
        }

        int radius = (int) Math.ceil(reach);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(feet.offset(-radius, 0, -radius), feet.offset(radius, radius + 1, radius))) {
            if (!mine.contains(pos) || !isOre(pos) || !isMinable(pos)) continue;
            double distance = eye.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance > reachSqr || distance >= bestDistance) continue;
            if (!visible(eye, pos)) continue;
            bestDistance = distance;
            best = pos.immutable();
        }
        return best;
    }

    private boolean isOre(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.is(Blocks.ANCIENT_DEBRIS)) return true;
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return id.endsWith("_ore");
    }

    private int surfaceFeet(Mine mine, int x, int z) {
        for (int y = mine.max().getY(); y >= mine.min().getY(); y--) {
            if (!passable(new BlockPos(x, y, z))) return y + 1;
        }
        return mine.min().getY();
    }

    private boolean centerOn(LocalPlayer player, BlockPos feet) {
        double dx = feet.getX() + 0.5 - player.getX();
        double dz = feet.getZ() + 0.5 - player.getZ();
        if (dx * dx + dz * dz < 0.04) return true;
        stopBreaking();
        float yaw = rotationTo(player.getEyePosition(), new Vec3(feet.getX() + 0.5, player.getY(), feet.getZ() + 0.5))[0];
        rotate(player, yaw, 60.0F);
        input.release();
        input.sneak = true;
        input.forward = Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) < 30.0F;
        install(player);
        return false;
    }


    private void scan(Mine mine) {
        int count = 0;
        unloaded = false;
        for (BlockPos pos : BlockPos.betweenClosed(mine.min(), mine.max())) {
            if (!mc.level.isLoaded(pos)) {
                unloaded = true;
                continue;
            }
            if (isMinable(pos)) count++;
        }
        remaining = count;
    }

    private void breakAt(LocalPlayer player, BlockPos pos, Vec3 aim) {
        long now = System.currentTimeMillis();
        if (!pos.equals(target)) {
            stopBreaking();
            target = pos;
            targetSinceMs = now;
        }
        if (now - targetSinceMs > BLOCK_TIMEOUT_MS) {
            ignored.add(pos);
            stopBreaking();
            return;
        }

        input.release();
        install(player);
        float[] rotation = rotationTo(player.getEyePosition(), aim);
        rotate(player, rotation[0], rotation[1]);

        BlockHitResult hit = viewHit(player);
        if (hit == null || !hit.getBlockPos().equals(pos)) return;
        if (mc.gameMode.continueDestroyBlock(pos, hit.getDirection())) {
            player.swing(InteractionHand.MAIN_HAND);
        }
        if (mc.level.getBlockState(pos).isAir()) target = null;
    }

    private void stopBreaking() {
        if (target != null && mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        target = null;
    }

    private boolean isMinable(BlockPos pos) {
        if (ignored.contains(pos) || !mc.level.isLoaded(pos)) return false;
        BlockState state = mc.level.getBlockState(pos);
        return !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(mc.level, pos) >= 0.0F;
    }

    private boolean visible(Vec3 eye, BlockPos pos) {
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, Vec3.atCenterOf(pos),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private BlockHitResult viewHit(LocalPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(player.blockInteractionRange()));
        BlockHitResult hit = mc.level.clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.BLOCK ? hit : null;
    }

    private void resetMining() {
        stopBreaking();
        remaining = 0;
        unloaded = false;
        resetWalk();
    }

    private double jitter(double amplitude) {
        return (random.nextDouble() - 0.5) * 2.0 * amplitude;
    }


    private int findPickaxe(LocalPlayer player) {
        Inventory inventory = player.getInventory();
        BlockState stone = Blocks.STONE.defaultBlockState();
        int selected = inventory.getSelectedSlot();
        int best = -1;
        float bestScore = 0.0F;
        for (int i = -1; i < 36; i++) {
            int slot = i == -1 ? selected : i;
            if (i == slot && slot == selected) continue;
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(ItemTags.PICKAXES)) continue;
            if (stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 2) continue;
            float score = drillLevel(stack) * 1000.0F + stack.getDestroySpeed(stone);
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    private int drillLevel(ItemStack stack) {
        Integer cached = drillCache.get(stack);
        if (cached != null) return cached;
        int level = 0;
        try {
            Item.TooltipContext context = Item.TooltipContext.of(mc.level);
            for (Component line : stack.getTooltipLines(context, mc.player, TooltipFlag.NORMAL)) {
                String text = line.getString().replaceAll("§.", "").toLowerCase(Locale.ROOT);
                Matcher matcher = DRILL.matcher(text);
                if (!matcher.find()) continue;
                level = switch (matcher.group(1)) {
                    case "i" -> 1;
                    case "ii" -> 2;
                    case "iii" -> 3;
                    default -> Integer.parseInt(matcher.group(1));
                };
                break;
            }
        } catch (Exception ignored) {
        }
        drillCache.put(stack, level);
        return level;
    }

    private boolean dropJunk(LocalPlayer player) {
        if (dropCooldown > 0) {
            dropCooldown--;
            return false;
        }
        if (mc.gui.screen() != null) return false;
        Inventory inventory = player.getInventory();
        int junk = 0;
        int empty = 0;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) empty++;
            else if (JUNK.contains(stack.getItem())) junk++;
        }
        if (!dumping && (junk >= 18 || empty == 0 && junk > 0)) dumping = true;
        if (!dumping) {
            dropCooldown = 10;
            return false;
        }
        if (junk == 0) {
            dumping = false;
            return false;
        }
        stopBreaking();
        idle(player);
        int thrown = 0;
        for (int slot = 0; slot < 36 && thrown < 2; slot++) {
            if (!JUNK.contains(inventory.getItem(slot).getItem())) continue;
            int menuSlot = slot < 9 ? 36 + slot : slot;
            mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, menuSlot, 1, ContainerInput.THROW, player);
            thrown++;
        }
        return true;
    }

    private boolean holdPickaxe(LocalPlayer player) {
        int slot = findPickaxe(player);
        if (slot == -1) return false;
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        if (slot == selected) return true;
        stopBreaking();
        if (slot < 9) {
            inventory.setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        } else {
            mc.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, selected, ContainerInput.SWAP, player);
        }
        return true;
    }


    private void walk(LocalPlayer player, Mine mine, BlockPos want, Predicate<BlockPos> accept) {
        if (!want.equals(goal)) {
            resetWalk();
            goal = want;
        }
        stopBreaking();

        long now = System.currentTimeMillis();
        if ((pathIndex >= path.size() || now - pathAtMs > REPLAN_MS) && player.onGround()) {
            List<BlockPos> found = findPath(player, goal, accept != null ? accept : goal::equals);
            if (found == null) {
                if (++replans > MAX_REPLANS) {
                    resetWalk();
                    phase = null;
                    idle(player);
                    return;
                }
                found = List.of(goal);
            }
            path = found;
            pathIndex = 0;
            pathAtMs = now;
        }

        while (pathIndex < path.size() && reached(player, path.get(pathIndex))) pathIndex++;
        if (pathIndex >= path.size()) {
            idle(player);
            checkStuck(player, now);
            return;
        }

        BlockPos waypoint = path.get(pathIndex);
        Vec3 point = new Vec3(waypoint.getX() + 0.5, waypoint.getY(), waypoint.getZ() + 0.5);
        float yaw = rotationTo(player.getEyePosition(), point)[0];
        rotate(player, yaw, 12.0F);

        boolean facing = Math.abs(Mth.wrapDegrees(yaw - player.getYRot())) < 50.0F;
        input.release();
        input.forward = facing;
        input.sprint = facing && remainingDistance(player) > SPRINT_DISTANCE;
        input.jump = player.onGround() && (waypoint.getY() > player.getY() + 0.5 || facing && player.horizontalCollision);
        install(player);
        checkStuck(player, now);
    }

    private double remainingDistance(LocalPlayer player) {
        double distance = 0.0;
        Vec3 from = player.position();
        for (int i = pathIndex; i < path.size(); i++) {
            Vec3 to = Vec3.atBottomCenterOf(path.get(i));
            distance += from.distanceTo(to);
            from = to;
        }
        return distance;
    }

    private boolean reached(LocalPlayer player, BlockPos waypoint) {
        double dx = waypoint.getX() + 0.5 - player.getX();
        double dz = waypoint.getZ() + 0.5 - player.getZ();
        return dx * dx + dz * dz < 0.16 && player.getY() >= waypoint.getY() - 0.5;
    }

    private void checkStuck(LocalPlayer player, long now) {
        Vec3 pos = player.position();
        if (stuckFrom == null || pos.distanceTo(stuckFrom) > 0.6) {
            stuckFrom = pos;
            stuckAtMs = now;
            return;
        }
        if (now - stuckAtMs < STUCK_MS) return;
        stuckFrom = null;
        path = List.of();
        replans++;
    }

    private void resetWalk() {
        goal = null;
        path = List.of();
        pathIndex = 0;
        replans = 0;
        stuckFrom = null;
    }

    private List<BlockPos> findPath(LocalPlayer player, BlockPos target, Predicate<BlockPos> accept) {
        BlockPos start = player.blockPosition();
        Vec3 targetCenter = Vec3.atBottomCenterOf(target);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(Node::f));
        Map<BlockPos, Node> nodes = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        Node first = new Node(start, null, 0.0, heuristic(start, targetCenter));
        open.add(first);
        nodes.put(start, first);
        Node best = first;

        int expanded = 0;
        while (!open.isEmpty() && expanded++ < MAX_PATH_NODES) {
            Node current = open.poll();
            if (!closed.add(current.pos)) continue;
            if (accept.test(current.pos)) return build(current);
            if (current.h < best.h) best = current;

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos side = current.pos.relative(direction);
                if (standable(side)) {
                    offer(open, nodes, closed, current, side, 1.0, targetCenter);
                } else if (passable(side) && passable(side.above())) {
                    for (int drop = 1; drop <= 3; drop++) {
                        BlockPos down = side.below(drop);
                        if (standable(down)) {
                            offer(open, nodes, closed, current, down, 1.0 + drop * 0.5, targetCenter);
                            break;
                        }
                        if (!passable(down)) break;
                    }
                } else if (standable(side.above()) && passable(current.pos.above(2))) {
                    offer(open, nodes, closed, current, side.above(), 2.0, targetCenter);
                }
            }
        }
        return !open.isEmpty() && best != first ? build(best) : null;
    }

    private void offer(PriorityQueue<Node> open, Map<BlockPos, Node> nodes, Set<BlockPos> closed,
                       Node from, BlockPos pos, double cost, Vec3 target) {
        if (closed.contains(pos)) return;
        double g = from.g + cost;
        Node known = nodes.get(pos);
        if (known != null && known.g <= g) return;
        Node node = new Node(pos, from, g, heuristic(pos, target));
        nodes.put(pos, node);
        open.add(node);
    }

    private static double heuristic(BlockPos pos, Vec3 target) {
        return Math.sqrt(Vec3.atBottomCenterOf(pos).distanceToSqr(target));
    }

    private static List<BlockPos> build(Node node) {
        List<BlockPos> out = new ArrayList<>();
        for (Node at = node; at != null && at.parent != null; at = at.parent) out.add(0, at.pos);
        return out;
    }

    private boolean passable(BlockPos pos) {
        if (!mc.level.isLoaded(pos)) return false;
        BlockState state = mc.level.getBlockState(pos);
        return state.getFluidState().isEmpty() && state.getCollisionShape(mc.level, pos).isEmpty();
    }

    private boolean standable(BlockPos pos) {
        if (!passable(pos) || !passable(pos.above())) return false;
        BlockPos below = pos.below();
        return mc.level.isLoaded(below) && !mc.level.getBlockState(below).getCollisionShape(mc.level, below).isEmpty();
    }

    private record Node(BlockPos pos, Node parent, double g, double h) {
        double f() {
            return g + h;
        }
    }


    private void rotate(LocalPlayer player, float yaw, float pitch) {
        targetYaw = yaw;
        targetPitch = pitch;
        hasRotation = true;
        if (BotContext.isBackgroundContextActive()) integrate(player, 0.05F);
    }

    public void frame() {
        long now = System.nanoTime();
        float dt = lastFrameNs == 0L ? 0.0F : Math.min((now - lastFrameNs) / 1.0E9F, 0.1F);
        lastFrameNs = now;
        LocalPlayer player = mc.player;
        if (!hasRotation || player == null || stage == Stage.FINISHED || dt <= 0.0F) return;

        integrate(player, dt);
        player.yRotO = player.getYRot();
        player.xRotO = player.getXRot();
    }

    private void integrate(LocalPlayer player, float dt) {
        long nowMs = System.currentTimeMillis();
        if (nowMs - smoothnessAtMs > 400L) {
            smoothness = 8.0F + random.nextFloat() * 5.0F;
            smoothnessAtMs = nowMs;
        }
        float maxSpeed = AutoMineModule.getInstance().rotationSpeed.getValue() * 20.0F;
        float maxAccel = maxSpeed * 6.0F * dt;

        float yawDelta = Mth.wrapDegrees(targetYaw - player.getYRot());
        float pitchDelta = targetPitch - player.getXRot();
        yawVelocity = approach(yawVelocity, Mth.clamp(yawDelta * smoothness, -maxSpeed, maxSpeed), maxAccel);
        pitchVelocity = approach(pitchVelocity, Mth.clamp(pitchDelta * smoothness, -maxSpeed * 0.7F, maxSpeed * 0.7F), maxAccel);

        double sensitivity = mc.options.sensitivity().get() * 0.6 + 0.2;
        float gcd = (float) (sensitivity * sensitivity * sensitivity * 8.0 * 0.15);
        yawCarry += yawVelocity * dt;
        pitchCarry += pitchVelocity * dt;
        float yawStep = Math.round(yawCarry / gcd) * gcd;
        float pitchStep = Math.round(pitchCarry / gcd) * gcd;
        yawCarry -= yawStep;
        pitchCarry -= pitchStep;
        if (yawStep == 0.0F && pitchStep == 0.0F) return;

        player.setYRot(player.getYRot() + yawStep);
        player.setXRot(Mth.clamp(player.getXRot() + pitchStep, -90.0F, 90.0F));
        player.setYHeadRot(player.getYRot());
    }

    private static float approach(float current, float wanted, float maxChange) {
        return current + Mth.clamp(wanted - current, -maxChange, maxChange);
    }

    private void resetRotation() {
        hasRotation = false;
        yawVelocity = 0.0F;
        pitchVelocity = 0.0F;
        yawCarry = 0.0F;
        pitchCarry = 0.0F;
    }

    private static float[] rotationTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double dz = to.z - from.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        return new float[]{yaw, pitch};
    }

    private void idle(LocalPlayer player) {
        input.release();
        install(player);
    }

    private void install(LocalPlayer player) {
        if (player.input != input) player.input = input;
    }

    private void release() {
        stopBreaking();
        input.release();
        resetRotation();
        LocalPlayer player = mc.player;
        if (player != null && player.input == input) {
            player.input = BotContext.isBackgroundContextActive() ? new ClientInput() : new KeyboardInput(mc.options);
        }
    }


    private Mine currentMine() {
        return MINES.get(Math.min(mineIndex, MINES.size() - 1));
    }

    private void setStage(Stage next) {
        stage = next;
        stageAtMs = System.currentTimeMillis();
        if (next != Stage.MINE) resetRotation();
    }

    private boolean elapsed(long ms) {
        return System.currentTimeMillis() - stageAtMs >= ms;
    }

    private void log(String message) {
        messages.add(message);
    }
}
