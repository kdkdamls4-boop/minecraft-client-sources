package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class EntityAimPoint {
   private EntityAimPoint() {
   }

   public static Vec3 getNearestPoint(Entity entity, double value) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null && entity != null) {
         Vec3 eyePosition = player.getEyePosition(1.0F);
         double distance = value * value;
         AABB boundingBox = entity.getBoundingBox();
         Vec3 center = boundingBox.getCenter();
         Vec3 position = null;
         double pOSITIVE_INFINITY = Double.POSITIVE_INFINITY;

         for (double minX = boundingBox.minX; minX <= boundingBox.maxX; minX += 0.1) {
            for (double minY = boundingBox.minY; minY <= boundingBox.maxY; minY += 0.1) {
               for (double minZ = boundingBox.minZ; minZ <= boundingBox.maxZ; minZ += 0.1) {
                  Vec3 position2 = new Vec3(minX, minY, minZ);
                  if (!(eyePosition.distanceToSqr(position2) > distance)) {
                     BlockHitResult blockHitResult = instance.level.clip(new ClipContext(eyePosition, position2, Block.COLLIDER, Fluid.NONE, player));
                     if (blockHitResult.getType() == Type.MISS || blockHitResult.getType() != Type.BLOCK) {
                        double distanceSquared = center.distanceToSqr(position2);
                        if (distanceSquared < pOSITIVE_INFINITY) {
                           pOSITIVE_INFINITY = distanceSquared;
                           position = position2;
                        }
                     }
                  }
               }
            }
         }

         return position == null ? boundingBox.getCenter() : position;
      } else {
         return entity != null ? entity.getBoundingBox().getCenter() : Vec3.ZERO;
      }
   }
}
