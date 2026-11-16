package net.skycore.client.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.combat.RotationClaim;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.combat.AntiBotModule;
import org.joml.Vector2f;

public final class CrystalPlacementAimHelper {
   private static final Minecraft MC = Minecraft.getInstance();

   public static Vec3 crystalPlacementAim(BlockPos blockPos) {
      return nearestSupportFace(blockPos).point();
   }

   public static BlockHitResult trySupportPick(BlockPos blockPos, BlockPos blockPos2, Vec3 position, float value) {
      if (MC.player != null && blockPos != null && blockPos2 != null && position != null) {
         BlockHitResult blockHitResult = pickBody(value);
         if (blockHitResult == null) {
            return null;
         }

         BlockPos blockPos3 = blockHitResult.getBlockPos();
         if (blockPos3.equals(blockPos2)) {
            BlockPos blockPos4 = blockHitResult.getBlockPos();
            Direction direction = blockHitResult.getDirection();
            if (blockPos4.relative(direction).equals(blockPos)) {
               return blockHitResult;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static float eyeDistance(Entity entity) {
      if (MC.player != null && entity != null) {
         LocalPlayer player = MC.player;
         float bbWidth = entity.getBbWidth() / 2.0F;
         float x = (float)Mth.clamp(player.getX() - entity.getX(), -bbWidth, bbWidth);
         Vec3 eyePosition = player.getEyePosition();
         double y = eyePosition.y - entity.getY();
         float bbHeight = (float)Mth.clamp(y, 0.0, entity.getBbHeight());
         double z = player.getZ();
         double z2 = z - entity.getZ();
         double computedValue = -bbWidth;
         double value = bbWidth;
         float clampedValue = (float)Mth.clamp(z2, computedValue, value);
         Vec3 eyePosition2 = player.getEyePosition();
         Vec3 position = entity.position();
         double value2 = x;
         double value3 = bbHeight;
         double value4 = clampedValue;
         return (float)eyePosition2.distanceTo(position.add(value2, value3, value4));
      } else {
         return Float.MAX_VALUE;
      }
   }

   public static List<Player> friends(float value) {
      return players(value, true);
   }

   public static BlockHitResult pick(double value) {
      return pickBody(value);
   }

   public static boolean containsBlock(AABB bounds, Block block) {
      int value = 1;
      int value2 = 0;
      value = (byte)-1;
      value2 = (byte)0;
      value2 = Mth.floor(bounds.minX);
      int intermediate = -1;
      intermediate = (byte)-1;
      value = Mth.floor(bounds.minY);
      int roundedDown = Mth.floor(bounds.minZ);
      intermediate = Mth.floor(bounds.maxX);
      int roundedDown2 = Mth.floor(bounds.maxY);
      var blockPoss = BlockPos.betweenClosed(value2, value, roundedDown, intermediate, roundedDown2, Mth.floor(bounds.maxZ));
      Iterator iterator = blockPoss.iterator();

      while (iterator.hasNext()) {
         BlockPos element = (BlockPos)iterator.next();
         if (MC.level.getBlockState(element).is(block)) {
            return true;
         }
      }

      return false;
   }

   public static int findItem(Item item, boolean condition) {
      if (MC.player == null) {
         return -1;
      }

      for (int value = 0; value < (condition ? 9 : 36); value++) {
         Inventory inventory = MC.player.getInventory();
         ItemStack item2 = inventory.getItem(value);
         if (item2.is(item)) {
            return value;
         }
      }

      return -1;
   }

   public static boolean previousBlockRay(BlockPos blockPos, double value) {
      float intermediate = RotationClaim.previousYaw();
      if (!Float.isNaN(intermediate)) {
         float value2 = RotationClaim.previousYaw();
         float value3 = RotationClaim.previousPitch();
         if (!blockRay(blockPos, value2, value3, value)) {
            return false;
         }
      }

      return true;
   }

   public static boolean placementOk(InteractionResult interactionResult) {
      if (interactionResult != null) {
         if (interactionResult == InteractionResult.SUCCESS) {
            return true;
         }

         if (interactionResult.consumesAction()) {
            return true;
         }
      }

      return false;
   }

   private CrystalPlacementAimHelper() {
   }

   private static boolean eligible(Player player) {
      if (player != MC.player && !(player instanceof LocalPlayer)) {
         if (player.isAlive() && !player.isDeadOrDying()) {
            if (!player.isSpectator()) {
               return true;
            }
         }
      }

      return false;
   }

   public static List<Player> targets(float value) {
      return players(value, false);
   }

   private static boolean withinPlaceRange(BlockPos blockPos, float value) {
      Vec3 eyePosition = MC.player.getEyePosition();
      Vec3 position = Vec3.atCenterOf(blockPos);
      return eyePosition.distanceToSqr(position) <= (double)value * value;
   }

   private static List<Player> players(float distance, boolean condition) {
      ArrayList elements = new ArrayList();
      if (MC.level != null && MC.player != null) {
         for (Entity entity : MC.level.entitiesForRendering()) {
            if (entity instanceof Player player
               && eligible(player)
               && FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) == condition
               && (condition || !AntiBotModule.isBot(player))
               && eyeDistance(player) <= distance) {
               elements.add(player);
            }
         }

         return elements;
      } else {
         return elements;
      }
   }

   public static BlockHitResult pickBody(double value) {
      if (MC.player != null) {
         if (MC.level != null) {
            Vec3 eyePosition = MC.player.getEyePosition();
            float yRot = MC.player.getYRot();
            float xRot = MC.player.getXRot();
            Vec3 position = HitsEntityHelper.lookVector(xRot, yRot);
            Vec3 position2 = position.scale(value);
            Vec3 position3 = eyePosition.add(position2);
            ClientLevel level = MC.level;
            ClipContext clipContext = new ClipContext(eyePosition, position3, net.minecraft.world.level.ClipContext.Block.OUTLINE, Fluid.NONE, MC.player);
            return level.clip(clipContext) instanceof BlockHitResult blockHitResult ? blockHitResult : null;
         }
      }

      return null;
   }

   private static CrystalPlacementAimHelper.SupportFace nearestSupportFace(BlockPos blockPos) {
      Vec3 position = Vec3.atCenterOf(blockPos);
      Vec3 eyePosition = MC.player != null ? MC.player.getEyePosition() : position;
      CrystalPlacementAimHelper.SupportFace supportFace = new CrystalPlacementAimHelper.SupportFace(position.add(0.0, 0.5, 0.0), Direction.UP, Double.POSITIVE_INFINITY);

      for (Direction direction : Direction.values()) {
         Vec3 stepX = position.add(direction.getStepX() * 0.5, direction.getStepY() * 0.5, direction.getStepZ() * 0.5);
         double distanceSquared = eyePosition.distanceToSqr(stepX);
         if (distanceSquared < supportFace.distance()) {
            supportFace = new CrystalPlacementAimHelper.SupportFace(stepX, direction, distanceSquared);
         }
      }

      return supportFace;
   }

   public static Vector2f aim(Vec3 position, Vec3 position2) {
      return AimAngles.aim(position, position2);
   }

   public static boolean blockRay(BlockPos blockPos, float value, float value2, double value3) {
      if (MC.player != null) {
         if (HitsEntityHelper.hitsBlock(MC.player.getEyePosition(), value, value2, value3, blockPos)) {
            return true;
         }
      }

      return false;
   }

   public static boolean attackRay(float value, float value2, double value3, Entity entity) {
      if (MC.player != null) {
         if (HitsEntityHelper.hitsEntity(value, value2, value3, entity)) {
            return true;
         }
      }

      return false;
   }

   private static double placementReach(Vec3 position, float value) {
      double eyePosition = MC.player.getEyePosition().distanceTo(position) + 1.5;
      return Math.max(value + 2.0, eyePosition);
   }

   public static BlockHitResult crystalPlacementHit(BlockPos blockPos) {
      CrystalPlacementAimHelper.SupportFace supportFace = nearestSupportFace(blockPos);
      return new BlockHitResult(supportFace.point(), supportFace.direction(), blockPos, false);
   }

   public static BlockHitResult tryCrystalPick(BlockPos blockPos, float value) {
      if (MC.player != null && blockPos != null && withinPlaceRange(blockPos, value)) {
         Vec3 position = Vec3.atCenterOf(blockPos);
         double value2 = placementReach(position, value);
         BlockHitResult blockHitResult = pickBody(value2);
         if (blockHitResult != null && blockHitResult.getBlockPos().equals(blockPos)) {
            return crystalPlacementHit(blockPos);
         }

         float yRot = MC.player.getYRot();
         float xRot = MC.player.getXRot();
         if (!blockRay(blockPos, yRot, xRot, value)) {
            return null;
         }

         Vec3 eyePosition = MC.player.getEyePosition();
         Vec3 position2 = position.add(0.0, 0.49, 0.0);
         return MC.level.clip(new ClipContext(eyePosition, position2, net.minecraft.world.level.ClipContext.Block.OUTLINE, Fluid.NONE, MC.player)) instanceof BlockHitResult blockHitResult2
               && blockHitResult2.getBlockPos().equals(blockPos)
            ? crystalPlacementHit(blockPos)
            : null;
      } else {
         return null;
      }
   }

   public record SupportFace(Vec3 point, Direction direction, double distance) {
      public double distance() {
         return this.distance;
      }

      public Direction direction() {
         return this.direction;
      }

      public Vec3 point() {
         return this.point;
      }
   }
}
