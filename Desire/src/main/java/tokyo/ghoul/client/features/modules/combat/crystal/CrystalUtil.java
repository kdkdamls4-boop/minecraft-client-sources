package tokyo.ghoul.client.features.modules.combat.crystal;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.combat.AntiBotModule;
import tokyo.ghoul.client.features.modules.player.AutoToolModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CrystalUtil implements QuickImports {
    private CrystalUtil() {}

    public static int enchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (stack.isEmpty()) return 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(enchantment)) return entry.getIntValue();
        }
        return 0;
    }

    public static int hotbarSlot(Item item) {
        if (mc.player == null) return -1;
        for (int slot = 0; slot < 9; slot++) {
            if (mc.player.getInventory().getItem(slot).getItem() == item) return slot;
        }
        return -1;
    }

    public static ItemStack miningTool(BlockState state) {
        if (mc.player == null) return ItemStack.EMPTY;
        ItemStack held = mc.player.getInventory().getSelectedItem();
        if (!AutoToolModule.getInstance().isEnabled()) return held;

        ItemStack best = held;
        float bestSpeed = held.getDestroySpeed(state);
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = mc.player.getInventory().getItem(slot);
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                best = stack;
            }
        }
        return best;
    }

    public static float breakTicks(BlockState state, BlockPos pos) {
        if (mc.player == null || mc.level == null) return Float.MAX_VALUE;

        float progress = state.getDestroyProgress(mc.player, mc.level, pos);
        if (progress <= 0.0F) return Float.MAX_VALUE;

        float heldSpeed = mc.player.getInventory().getSelectedItem().getDestroySpeed(state);
        float toolSpeed = miningTool(state).getDestroySpeed(state);
        if (heldSpeed > 0.0F && toolSpeed > 0.0F) {
            progress *= toolSpeed / heldSpeed;
        }
        return 1.0F / progress;
    }


    public static boolean isValidOpponent(Player player) {
        return player != mc.player && !(player instanceof LocalPlayer) && player.isAlive()
                && !player.isDeadOrDying() && !player.isSpectator();
    }

    private static boolean isFriend(Player player) {
        return FriendManager.getInstance().contains(player.getName().getString());
    }

    public static List<Player> enemies(float range) {
        List<Player> result = new ArrayList<>();
        if (mc.player == null || mc.level == null) return result;

        boolean antiBot = AntiBotModule.getInstance().isEnabled();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || !isValidOpponent(player) || isFriend(player)) continue;
            if (antiBot && AntiBotModule.isBot(player)) continue;
            if (eyeDistance(player) <= range) result.add(player);
        }
        return result;
    }

    public static List<Player> friends(float range) {
        List<Player> result = new ArrayList<>();
        if (mc.player == null || mc.level == null) return result;

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity instanceof Player player && isValidOpponent(player) && isFriend(player) && eyeDistance(player) <= range) {
                result.add(player);
            }
        }
        return result;
    }

    public static double eyeDistance(Entity entity) {
        return Math.sqrt(ExplosionDamage.distanceSq(mc.player.getEyePosition(), entity.getBoundingBox()));
    }

    public static int latencyTicks() {
        if (mc.isLocalServer() || mc.getConnection() == null || mc.player == null) return 0;
        PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info == null ? 0 : Math.max(0, info.getLatency()) / 50;
    }


    public static Vector2f rotationTo(Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        double horizontal = Math.hypot(delta.x, delta.z);
        float yaw = (float) (Mth.atan2(delta.z, delta.x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Mth.atan2(delta.y, horizontal) * 180.0 / Math.PI));
        return new Vector2f(Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F));
    }

    public static Vec3 direction(float yaw, float pitch) {
        float yawRad = -yaw * Mth.DEG_TO_RAD - (float) Math.PI;
        float pitchRad = -pitch * Mth.DEG_TO_RAD;
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = -Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    public static boolean rayHitsBox(Vec3 from, float yaw, float pitch, double range, boolean checkWalls, AABB box) {
        if (mc.level == null) return false;
        if (box.contains(from)) return true;

        Vec3 to = from.add(direction(yaw, pitch).scale(range));
        Optional<Vec3> hit = box.clip(from, to);
        if (hit.isEmpty()) return false;
        if (!checkWalls) return true;

        BlockHitResult block = mc.level.clip(new ClipContext(from, hit.get(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (block.getType() != HitResult.Type.BLOCK) return true;
        return from.distanceTo(block.getLocation()) >= from.distanceTo(hit.get());
    }
}
