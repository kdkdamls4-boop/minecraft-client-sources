package deluxe.client.util;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.module.combat.MaceHelperModule;
import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;
import deluxe.client.RotationAngles;
import deluxe.client.RotationSpeed;
import deluxe.client.module.misc.YTMODEModule;

import java.util.ArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class EntityUtils implements MinecraftAccess {
   private EntityUtils() {
   }

   public static boolean method2789(BlockState blockState, BlockPos blockPos) {
      if (mc.player == null) {
         return false;
      } else {
         BlockPos blockPos2 = BlockPos.ofFloored(mc.player.getEyePos());
         if (!blockPos2.equals(blockPos) && (!mc.player.isInPose(EntityPose.SWIMMING) || !blockPos2.up().equals(blockPos))) {
            Block block = blockState.getBlock();
            return block instanceof FenceBlock
               || block instanceof FenceGateBlock
               || block instanceof WallBlock
               || block instanceof PaneBlock
               || block instanceof SlabBlock
               || block instanceof StairsBlock
               || block instanceof TrapdoorBlock
               || block instanceof PointedDripstoneBlock;
         } else {
            return true;
         }
      }
   }

   public static float method2790(LivingEntity livingEntity) {
      if (mc.world != null) {
         for (ScoreboardObjective scoreboardObjective : mc.world.getScoreboard().getObjectives()) {
            ReadableScoreboardScore score = mc.world.getScoreboard().getScore(livingEntity, scoreboardObjective);
            if (score != null) {
               return score.getScore();
            }
         }
      }

      return livingEntity.getHealth() + livingEntity.getAbsorptionAmount();
   }

   public static boolean method2791(LivingEntity livingEntity, float local4402_1) {
      Box boundingBox = mc.player.getBoundingBox();
      Box box = livingEntity.getBoundingBox().stretch(local4402_1, 0.0, local4402_1);
      return boundingBox.intersects(box);
   }

   public static float method2792(float local4403_0, float local4403_1, float local4403_2, float local4403_3) {
      return MathUtils.method3827(local4403_2, local4403_3, Math.abs(local4403_0) * MathUtils.clampUnit(local4403_1));
   }

   public static boolean method2793(Vec3d vec3d, Vec3d vec3d2) {
      return mc.world != null && mc.player != null ? BlockView.raycast(vec3d, vec3d2, null, (local4423_2, blockPos) -> {
         BlockState blockState = mc.world.getBlockState(blockPos);
         if (method2789(blockState, blockPos)) {
            return null;
         }

         VoxelShape collisionShape = blockState.getCollisionShape(mc.world, blockPos);
         return !collisionShape.isEmpty() && collisionShape.raycast(vec3d, vec3d2, blockPos) != null ? Boolean.FALSE : null;
      }, local4413_0 -> Boolean.TRUE) : true;
   }

   private static boolean method2794(Vec3d vec3d, Vec3d vec3d2) {
      Vec3d vec3d3 = vec3d2.add(vec3d.subtract(vec3d2).normalize().multiply(0.05));
      return method2793(vec3d, vec3d3);
   }

   public static boolean method2795(ItemStack itemStack) {
      return itemStack.getItem() == Items.TOTEM_OF_UNDYING
         && !itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
            .copyNbt()
            .getCompoundOrEmpty("PublicBukkitValues")
            .getString("minecraft:don-item", "")
            .isEmpty();
   }

   public static float[] method2796(double local4407_0, double local4407_2, double local4407_4, double local4407_6, double local4407_8, double local4407_10) {
      double[] doubleArray = new double[]{local4407_0 - local4407_6, local4407_2 - local4407_8, local4407_4 - local4407_10};
      doubleArray = new double[]{
         doubleArray[0], doubleArray[1], doubleArray[2], Math.sqrt(doubleArray[0] * doubleArray[0] + doubleArray[2] * doubleArray[2])
      };
      return new float[]{
         (float)Math.atan2(doubleArray[2], doubleArray[0]) * (float) (180.0 / Math.PI) - 90.0F,
         (float)Math.atan2(doubleArray[1], doubleArray[3]) * (float) (-180.0 / Math.PI)
      };
   }

   public static float method2797(RotationAngles rotationAngles, float local4408_1, float local4408_2) {
      return mc.player == null
         ? 360.0F
         : Math.max(
            MouseSensitivity.method4408(),
            Math.abs(MathHelper.wrapDegrees(rotationAngles.method4362() - mc.player.getYaw()))
               / Math.max(1.0F, MathUtils.method3825(local4408_1, local4408_2))
         );
   }

   public static boolean method2798(LivingEntity livingEntity) {
      return method2791(livingEntity, 0.0F);
   }

   public static boolean method2799(Vec3d vec3d, Vec3d vec3d2, Box box, double local4410_3) {
      double local4410_5 = 0.0;
      double local4410_7 = local4410_3;
      double local4410_9 = vec3d.x;
      double local4410_11 = vec3d.y;
      double local4410_13 = vec3d.z;

      for (int index = 0; index < 3; index++) {
         double local4410_16 = index == 0 ? vec3d2.x : (index == 1 ? vec3d2.y : vec3d2.z);
         double local4410_18 = index == 0 ? local4410_9 : (index == 1 ? local4410_11 : local4410_13);
         double local4410_20 = index == 0 ? box.minX : (index == 1 ? box.minY : box.minZ);
         double local4410_22 = index == 0 ? box.maxX : (index == 1 ? box.maxY : box.maxZ);
         if (Math.abs(local4410_16) < 1.0E-9) {
            if (local4410_18 < local4410_20 || local4410_18 > local4410_22) {
               return false;
            }
         } else {
            double local4410_24 = 1.0 / local4410_16;
            double local4410_26 = (local4410_20 - local4410_18) * local4410_24;
            double local4410_28 = (local4410_22 - local4410_18) * local4410_24;
            if (local4410_26 > local4410_28) {
               double local4410_30 = local4410_26;
               local4410_26 = local4410_28;
               local4410_28 = local4410_30;
            }

            local4410_5 = Math.max(local4410_5, local4410_26);
            local4410_7 = Math.min(local4410_7, local4410_28);
            if (local4410_5 > local4410_7) {
               return false;
            }
         }
      }

      return local4410_7 >= 0.0 && local4410_5 <= local4410_3;
   }

   public static float method2800(float local4411_0, float local4411_1) {
      return Math.max(MouseSensitivity.method4408(), MouseSensitivity.method4410(MathUtils.method3825(local4411_0, local4411_1)));
   }

   public static float method2801(RotationAngles rotationAngles, float local4412_1, float local4412_2) {
      return mc.player == null
         ? 360.0F
         : Math.max(
            MouseSensitivity.method4408(),
            Math.abs(rotationAngles.method4365() - mc.player.getPitch()) / Math.max(1.0F, MathUtils.method3825(local4412_1, local4412_2))
         );
   }

   public static boolean method2803(Entity entity, double local4414_1, float local4414_3, float local4414_4) {
      if (entity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         return entity.getBoundingBox().expand(entity.getTargetingMargin()).contains(eyePos)
            || entity.getBoundingBox()
               .expand(entity.getTargetingMargin())
               .raycast(eyePos, eyePos.add(method2810(local4414_3, local4414_4).multiply(local4414_1)))
               .isPresent();
      } else {
         return false;
      }
   }

   public static String method2804(LivingEntity livingEntity) {
      if (YTMODEModule.method1426().isEnabled() && livingEntity.isInvisible()) {
         return "1000";
      } else {
         return Math.round(method2790(livingEntity)) == 1000 ? "?" : String.valueOf(Math.round(method2790(livingEntity)));
      }
   }

   public static float method2805(int local4416_0, int local4416_1) {
      return Math.max(1, Math.round((float)MathUtils.method3822(local4416_0, local4416_1))) * MouseSensitivity.method4408();
   }

   public static float method2806(double local4417_0, double local4417_2, double local4417_4) {
      return (float)Math.atan2(local4417_2, Math.sqrt(local4417_0 * local4417_0 + local4417_4 * local4417_4)) * (float) (-180.0 / Math.PI);
   }

   public static float method2807(Entity entity, float local4418_1, float local4418_2, float local4418_3) {
      return MathUtils.lerp(
         local4418_1, local4418_2, MathUtils.clampUnit((float)(method2816(entity) / Math.max(0.01F, local4418_3)))
      );
   }

   public static boolean method2808(LivingEntity livingEntity, double local4419_1) {
      if (mc.player != null && mc.world != null && livingEntity != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Box box = livingEntity.getBoundingBox().expand(livingEntity.getTargetingMargin());
         if (box.contains(eyePos)) {
            return true;
         }

         Vec3d vec3d = box.raycast(
               eyePos, eyePos.add(method2810(mc.player.getYaw(), mc.player.getPitch()).multiply(local4419_1))
            )
            .orElse(null);
         return vec3d != null && method2794(eyePos, vec3d);
      } else {
         return true;
      }
   }

   public static RotationSpeed method2809(RotationAngles rotationAngles, float local4420_1, float local4420_2, float local4420_3, float local4420_4) {
      return new RotationSpeed(
         MouseSensitivity.method4410(method2797(rotationAngles, local4420_1, local4420_2)),
         MouseSensitivity.method4410(method2801(rotationAngles, local4420_1, local4420_2)),
         local4420_3,
         local4420_4
      );
   }

   public static Vec3d method2810(float local4421_0, float local4421_1) {
      float local4421_2 = -local4421_0 * (float) (Math.PI / 180.0) - (float) Math.PI;
      float local4421_3 = -local4421_1 * (float) (Math.PI / 180.0);
      return new Vec3d(
         (float)Math.sin(local4421_2) * -((float)Math.cos(local4421_3)),
         (float)Math.sin(local4421_3),
         (float)Math.cos(local4421_2) * -((float)Math.cos(local4421_3))
      );
   }

   public static boolean method2811(Vec3d vec3d, Vec3d vec3d2) {
      return mc.world != null && mc.player != null
         ? mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType()
            == Type.MISS
         : true;
   }

   public static float method2813(double local4424_0, double local4424_2) {
      return (float)Math.atan2(local4424_2, local4424_0);
   }

   public static float[] method2814(Vec3d vec3d, Vec3d vec3d2) {
      double[] doubleArray = new double[]{
         vec3d.getX() - vec3d2.getX(), vec3d.getY() - vec3d2.getY(), vec3d.getZ() - vec3d2.getZ()
      };
      doubleArray = new double[]{doubleArray[0], doubleArray[1], doubleArray[2], Math.sqrt(doubleArray[0] * doubleArray[0] + doubleArray[2] * doubleArray[2])};
      return new float[]{
         (float)Math.atan2(doubleArray[2], doubleArray[0]) * (float) (180.0 / Math.PI) - 90.0F,
         (float)Math.atan2(doubleArray[1], doubleArray[3]) * (float) (-180.0 / Math.PI)
      };
   }

   public static RotationAngles method2815(RotationAngles rotationAngles) {
      AttackAuraModule attackAuraModule = AttackAuraModule.method1029();
      if (!attackAuraModule.isEnabled() || attackAuraModule.method1005() == null || !attackAuraModule.field02100.isSelected("Неполноценные")) {
         return rotationAngles;
      }

      if (mc.player == null || mc.world == null) {
         return rotationAngles;
      }

      if (!MaceHelperModule.method1776().method1783() && !attackAuraModule.method1038()) {
         LivingEntity livingEntity = attackAuraModule.method1005();
         Vec3d vec3d = mc.player.getEyePos();
         Box boundingBox = livingEntity.getBoundingBox();
         double local4426_5 = attackAuraModule.method1023();
         Vec3d vec3d2 = boundingBox.raycast(
               vec3d, vec3d.add(method2810(rotationAngles.method4362(), rotationAngles.method4365()).normalize().multiply(local4426_5 + 1.0))
            )
            .orElse(null);
         if (vec3d2 != null && !method2794(vec3d, vec3d2)) {
            ArrayList<Vec3d> local4426_8 = new ArrayList<>();
            Vec3d vec3d3 = Vec3d.ZERO;

            for (int index = 0; index <= 8; index++) {
               for (int index2 = 0; index2 <= 8; index2++) {
                  for (int index3 = 0; index3 <= 8; index3++) {
                     if (index == 0 || index == 8 || index2 == 0 || index2 == 8 || index3 == 0 || index3 == 8) {
                        Vec3d vec3d4 = new Vec3d(
                           MathHelper.lerp(index / 8.0, boundingBox.minX, boundingBox.maxX),
                           MathHelper.lerp(index2 / 8.0, boundingBox.minY, boundingBox.maxY),
                           MathHelper.lerp(index3 / 8.0, boundingBox.minZ, boundingBox.maxZ)
                        );
                        if (!(vec3d.squaredDistanceTo(vec3d4) > local4426_5 * local4426_5) && method2794(vec3d, vec3d4)) {
                           local4426_8.add(vec3d4);
                           vec3d3 = vec3d3.add(vec3d4);
                        }
                     }
                  }
               }
            }

            if (local4426_8.isEmpty()) {
               return rotationAngles;
            }

            Vec3d vec3d5 = vec3d3.multiply(1.0 / local4426_8.size());
            Vec3d vec3d6 = local4426_8.get(0);

            for (Vec3d vec3d7 : local4426_8) {
               if (vec3d7.squaredDistanceTo(vec3d5) < vec3d6.squaredDistanceTo(vec3d5)) {
                  vec3d6 = vec3d7;
               }
            }

            float[] floatArray = method2814(vec3d6, vec3d);
            return new RotationAngles(floatArray[0], floatArray[1]);
         } else {
            return rotationAngles;
         }
      } else {
         return rotationAngles;
      }
   }

   public static double method2816(Entity entity) {
      if (entity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Box boundingBox = entity.getBoundingBox();
         double local4427_3 = Math.max(Math.max(boundingBox.minX - eyePos.x, eyePos.x - boundingBox.maxX), 0.0);
         double local4427_5 = Math.max(Math.max(boundingBox.minY - eyePos.y, eyePos.y - boundingBox.maxY), 0.0);
         double local4427_7 = Math.max(Math.max(boundingBox.minZ - eyePos.z, eyePos.z - boundingBox.maxZ), 0.0);
         return Math.sqrt(local4427_3 * local4427_3 + local4427_5 * local4427_5 + local4427_7 * local4427_7);
      } else {
         return Double.MAX_VALUE;
      }
   }

   public static double method2817(LivingEntity livingEntity) {
      if (livingEntity != null && mc.player != null) {
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d vec3d = livingEntity.getEntityPos()
            .add(0.0, MathHelper.clamp(eyePos.y - livingEntity.getY(), 0.0, livingEntity.getHeight() * 0.8), 0.0)
            .subtract(eyePos);
         float local4428_3 = MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-vec3d.x, vec3d.z)) - RotationAngles.method4366());
         float local4428_4 = (float)(-Math.toDegrees(Math.atan2(vec3d.y, Math.hypot(vec3d.x, vec3d.z)))) - RotationAngles.method4364();
         return Math.sqrt(local4428_3 * local4428_3 + local4428_4 * local4428_4);
      } else {
         return 0.0;
      }
   }
}
