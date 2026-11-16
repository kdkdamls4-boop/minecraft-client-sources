package net.skycore.client.combat;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.util.UseItemHelper;

public final class CrystalInventory implements MinecraftAccess {
   public static float[] rotations(Vec3 position) {
      if (mc.player == null) {
         return new float[]{0.0F, 0.0F};
      }

      double x = position.x - mc.player.getX();
      double y = position.y;
      double y2 = mc.player.getY();
      double eyeHeight = y - (y2 + mc.player.getEyeHeight(mc.player.getPose()));
      double z = position.z - mc.player.getZ();
      double computedValue = x * x + z * z;
      double squareRoot = Math.sqrt(computedValue);
      double angleRadians = Math.atan2(z, x);
      float computedValue2 = (float)Math.toDegrees(angleRadians) - 90.0F;
      double angleRadians2 = Math.atan2(eyeHeight, squareRoot);
      float computedValue3 = (float)(-Math.toDegrees(angleRadians2));
      return new float[]{computedValue2, computedValue3};
   }

   public static Entity getMouseOver(Entity entity, float value, float value2, double value3) {
      if (entity != null) {
         if (mc.player != null) {
            if (mc.level != null) {
               EntityHitResult entityHitResult = rayCastEntity(value3, value, value2, entity2 -> entity2 == entity);
               return entityHitResult != null && entityHitResult.getEntity() == entity ? entity : null;
            }
         }
      }

      return null;
   }

   public static int getHotbarCrystalSlot() {
      Class element = Items.END_CRYSTAL.getClass();
      return getItemSlot(element, true);
   }

   private CrystalInventory() {
   }

   public static void moveToOffhand(Item item) {
      int slot = UseItemHelper.getSlot(item);
      if (slot != -1) {
         if (mc.player != null) {
            UseItemHelper.moveItem(slot, 45);
         }
      }
   }

   public static boolean placeInAir(BlockHitResult blockHitResult, InteractionHand interactionHand) {
      if (mc.player != null) {
         if (mc.player.connection != null && blockHitResult != null) {
            ClientPacketListener elements = mc.player.connection;
            ServerboundUseItemOnPacket serverboundUseItemOnPacket = new ServerboundUseItemOnPacket(interactionHand, blockHitResult, 0);
            elements.send(serverboundUseItemOnPacket);
            mc.player.swing(interactionHand);
            return true;
         }
      }

      return false;
   }

   public static int getItemSlot(Class<?> element, boolean condition) {
      if (mc.player == null) {
         return -1;
      }

      Inventory inventory = mc.player.getInventory();
      int index = condition ? 0 : 9;
      int index2 = condition ? 9 : 36;

      for (int index3 = index; index3 < index2; index3++) {
         if (!inventory.getItem(index3).isEmpty() && element.isInstance(inventory.getItem(index3).getItem())) {
            return index3;
         }
      }

      return -1;
   }

   public static BlockHitResult airHit(BlockPos blockPos) {
      if (blockPos == null) {
         return null;
      }

      Vec3 position = Vec3.atCenterOf(blockPos);
      return new BlockHitResult(position, Direction.UP, blockPos, false);
   }

   private static Vec3 lookVector(float value, float value2) {
      float angleRadians = (float)Math.toRadians(value);
      double value3 = value2;
      float angleRadians2 = (float)Math.toRadians(value3);
      float cosine = Mth.cos(angleRadians2);
      double value4 = angleRadians;
      double computedValue = -Mth.sin(value4) * cosine;
      double sine = -Mth.sin(angleRadians2);
      double value5 = angleRadians;
      double computedValue2 = Mth.cos(value5) * cosine;
      return new Vec3(computedValue, sine, computedValue2);
   }

   public static EntityHitResult rayCastEntity(double value, float value2, float value3, Predicate<Entity> predicate) {
      if (mc.player != null && mc.level != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 position = lookVector(value2, value3);
         Vec3 position2 = eyePosition.add(position.scale(value));
         EntityHitResult entityHitResult = null;
         double index = value;

         for (Entity entity : mc.level.entitiesForRendering()) {
            if (predicate.test(entity) && !entity.isSpectator()) {
               AABB boundingBox = entity.getBoundingBox();
               var optional = boundingBox.clip(eyePosition, position2);
               if (optional.isPresent()) {
                  double value4 = eyePosition.distanceTo((Vec3)optional.get());
                  if (value4 < index) {
                     index = value4;
                     entityHitResult = new EntityHitResult(entity, (Vec3)optional.get());
                  }
               }
            }
         }

         return entityHitResult;
      } else {
         return null;
      }
   }
}
