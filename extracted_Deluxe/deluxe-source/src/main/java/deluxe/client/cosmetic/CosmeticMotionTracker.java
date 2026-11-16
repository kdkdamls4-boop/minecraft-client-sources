package deluxe.client.cosmetic;

import deluxe.client.cosmetic.CosmeticCategory;
import deluxe.client.cosmetic.CosmeticEntry;
import deluxe.client.cosmetic.CosmeticModel;
import deluxe.client.cosmetic.CosmeticPlacement;
import deluxe.client.cosmetic.CosmeticStore;
import deluxe.client.module.misc.GlobalsModule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class CosmeticMotionTracker {
   private static final CosmeticMotionTracker cosmeticMotionTracker = new CosmeticMotionTracker();
   private final HashMap<UUID, CosmeticMotionTracker.Nested0486> field11802 = new HashMap<>();
   private final HashSet<UUID> field11803 = new HashSet<>();
   private Object field11804;

   private boolean method3363(CosmeticMotionTracker.Nested0486 local5517_1, double local5517_2) {
      local5517_1.field11862 = Math.min(0.85, local5517_1.field11862 + 0.08);
      local5517_1.field11855 = Math.max(local5517_2, local5517_1.field11855 - local5517_1.field11862);
      if (local5517_1.field11855 > local5517_2 + 0.001) {
         return true;
      }

      local5517_1.field11855 = local5517_2;
      local5517_1.field11862 = 0.0;
      return false;
   }

   private static Vec3d method3364(Vec3d vec3d, float local5519_1, boolean local5519_2) {
      double local5519_3 = Math.toRadians(local5519_1);
      return local5519_2
         ? new Vec3d(vec3d.x + Math.cos(local5519_3 - (Math.PI / 2)) * 1.25, vec3d.y, vec3d.z + Math.sin(local5519_3 - (Math.PI / 2)) * 1.25)
         : new Vec3d(vec3d.x + Math.sin(local5519_3) * 1.75, vec3d.y, vec3d.z - Math.cos(local5519_3) * 1.75);
   }

   public static CosmeticMotionTracker method3365() {
      return cosmeticMotionTracker;
   }

   private double method3366(MinecraftClient minecraftClient, Vec3d vec3d, Vec3d vec3d2) {
      HitResult hitResult = minecraftClient.world
         .raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, minecraftClient.player));
      if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() != Type.MISS) {
         BlockPos blockPos = blockHitResult.getBlockPos();
         BlockPos blockPos2 = blockPos.up();
         if (minecraftClient.world.getBlockState(blockPos).getCollisionShape(minecraftClient.world, blockPos).isEmpty()) {
            return Double.NaN;
         } else {
            return minecraftClient.world.getBlockState(blockPos2).getCollisionShape(minecraftClient.world, blockPos2).isEmpty()
                  && minecraftClient.world.getBlockState(blockPos2.up()).getCollisionShape(minecraftClient.world, blockPos2.up()).isEmpty()
               ? hitResult.getPos().y
               : Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   private Vec3d method3367(MinecraftClient minecraftClient, double local5522_2, double local5522_4, double local5522_6) {
      for (int index = 0; index <= 4; index++) {
         for (int index2 = -index; index2 <= index; index2++) {
            for (int index3 = -index; index3 <= index; index3++) {
               if (index <= 0 || Math.abs(index2) == index || Math.abs(index3) == index) {
                  double local5522_11 = index == 0 ? local5522_2 : Math.floor(local5522_2) + index2 + 0.5;
                  double local5522_13 = index == 0 ? local5522_4 : Math.floor(local5522_4) + index3 + 0.5;
                  double local5522_15 = this.method3372(minecraftClient, local5522_11, local5522_13, local5522_6);
                  if (!Double.isNaN(local5522_15)) {
                     return new Vec3d(local5522_11, local5522_15, local5522_13);
                  }
               }
            }
         }
      }

      return null;
   }

   public void method3368(MinecraftClient minecraftClient) {
      if (minecraftClient.player != null && minecraftClient.world != null) {
         if (this.field11804 != minecraftClient.world) {
            this.field11802.clear();
            this.field11804 = minecraftClient.world;
         }

         HashSet<UUID> local5523_2 = this.field11803;
         local5523_2.clear();

         for (AbstractClientPlayerEntity abstractClientPlayerEntity : minecraftClient.world.getPlayers()) {
            CosmeticEntry currentCosmeticEntry = abstractClientPlayerEntity == minecraftClient.player
               ? CosmeticStore.method3519(CosmeticCategory.PET)
               : (
                  !abstractClientPlayerEntity.isSpectator() && !abstractClientPlayerEntity.isInvisible() && GlobalsModule.method0643().method0639()
                     ? GlobalsModule.method0643().method0641(abstractClientPlayerEntity.getGameProfile().name(), CosmeticCategory.PET)
                     : null
               );
            if (currentCosmeticEntry != null && currentCosmeticEntry.placement() != null && currentCosmeticEntry.placement().isPet()) {
               local5523_2.add(abstractClientPlayerEntity.getUuid());
               CosmeticMotionTracker.Nested0486 local5523_6 = this.field11802.get(abstractClientPlayerEntity.getUuid());
               if (local5523_6 == null || local5523_6.cosmeticEntry.id() != currentCosmeticEntry.id()) {
                  Vec3d vec3d = method3364(abstractClientPlayerEntity.getEntityPos(), abstractClientPlayerEntity.bodyYaw, currentCosmeticEntry.placement().flying());
                  double local5523_8 = currentCosmeticEntry.placement().flying()
                     ? abstractClientPlayerEntity.getY() + 1.3
                     : this.method3369(minecraftClient, vec3d.x, vec3d.z, abstractClientPlayerEntity.getY());
                  local5523_6 = new CosmeticMotionTracker.Nested0486(
                     currentCosmeticEntry,
                     abstractClientPlayerEntity.getUuid().getLeastSignificantBits() ^ currentCosmeticEntry.id(),
                     vec3d.x,
                     local5523_8,
                     vec3d.z,
                     abstractClientPlayerEntity.bodyYaw
                  );
                  this.field11802.put(abstractClientPlayerEntity.getUuid(), local5523_6);
               }

               this.method3377(minecraftClient, abstractClientPlayerEntity, local5523_6);
            }
         }

         this.field11802.keySet().retainAll(local5523_2);
      } else {
         this.field11802.clear();
         this.field11804 = null;
      }
   }

   private double method3369(MinecraftClient minecraftClient, double local5524_2, double local5524_4, double local5524_6) {
      double local5524_8 = this.method3372(minecraftClient, local5524_2, local5524_4, local5524_6);
      return Double.isNaN(local5524_8) ? local5524_6 : local5524_8;
   }

   private boolean method3370(
      MinecraftClient minecraftClient, CosmeticMotionTracker.Nested0486 local5526_2, Vec3d vec3d, double local5526_4, boolean local5526_6
   ) {
      double local5526_7 = vec3d.x - local5526_2.field11854;
      double local5526_9 = vec3d.z - local5526_2.field11856;
      double local5526_11 = Math.sqrt(local5526_7 * local5526_7 + local5526_9 * local5526_9);
      boolean local5526_13 = false;
      if (!local5526_6) {
         double local5526_14 = this.method3375(minecraftClient, local5526_2.field11854, local5526_2.field11856, local5526_2.field11855);
         if (!Double.isNaN(local5526_14) && local5526_14 < local5526_2.field11855 - 0.02) {
            local5526_13 = this.method3363(local5526_2, local5526_14);
         } else if (!Double.isNaN(local5526_14)) {
            local5526_2.field11855 = local5526_14;
            local5526_2.field11862 = 0.0;
         }
      }

      if (local5526_11 > 15.0) {
         if (local5526_6) {
            local5526_2.method3378(vec3d.x, vec3d.y, vec3d.z);
         } else {
            Vec3d vec3d2 = this.method3367(minecraftClient, vec3d.x, vec3d.z, vec3d.y);
            if (vec3d2 != null) {
               local5526_2.method3378(vec3d2.x, vec3d2.y, vec3d2.z);
            }
         }

         return false;
      } else {
         boolean local5526_14 = false;
         if (local5526_11 > 0.12) {
            double local5526_15 = Math.min(local5526_4, local5526_11);
            double local5526_17 = local5526_7 / local5526_11;
            double local5526_19 = local5526_9 / local5526_11;
            if (local5526_6) {
               local5526_2.field11854 += local5526_17 * local5526_15;
               local5526_2.field11856 += local5526_19 * local5526_15;
               local5526_2.field11864 = local5526_2.field11864
                  + MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-local5526_7, local5526_9)) - local5526_2.field11864) * 0.28F;
               local5526_14 = true;
            } else {
               local5526_14 = this.method3374(
                  minecraftClient, local5526_2, local5526_2.field11854 + local5526_17 * local5526_15, local5526_2.field11856 + local5526_19 * local5526_15
               );
               if (!local5526_14) {
                  local5526_14 = this.method3374(
                     minecraftClient,
                     local5526_2,
                     local5526_2.field11854 - local5526_19 * local5526_15 * local5526_2.field11863,
                     local5526_2.field11856 + local5526_17 * local5526_15 * local5526_2.field11863
                  );
               }

               if (!local5526_14) {
                  local5526_2.field11863 = -local5526_2.field11863;
                  local5526_14 = this.method3374(
                     minecraftClient,
                     local5526_2,
                     local5526_2.field11854 - local5526_19 * local5526_15 * local5526_2.field11863,
                     local5526_2.field11856 + local5526_17 * local5526_15 * local5526_2.field11863
                  );
               }
            }
         }

         if (local5526_6) {
            local5526_2.field11862 = 0.0;
            local5526_2.field11855 = local5526_2.field11855 + MathHelper.clamp(vec3d.y - local5526_2.field11855, -0.32, 0.32);
         }

         if (!local5526_14 && !local5526_13 && local5526_11 > 0.12) {
            local5526_2.field11869 = Math.min(local5526_2.field11869, 5);
         }

         return local5526_14 || local5526_13;
      }
   }

   public void method3371(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, MinecraftClient minecraftClient) {
      if (minecraftClient.player != null && minecraftClient.world != null && matrixStack != null && vertexConsumerProvider != null) {
         float tickProgress = minecraftClient.getRenderTickCounter().getTickProgress(false);
         Vec3d cameraPos = minecraftClient.gameRenderer.getCamera().getCameraPos();

         for (CosmeticMotionTracker.Nested0486 local5527_7 : this.field11802.values()) {
            double local5527_8 = MathHelper.lerp(tickProgress, local5527_7.field11857, local5527_7.field11854) - cameraPos.x;
            double local5527_10 = MathHelper.lerp(tickProgress, local5527_7.field11858, local5527_7.field11855) - cameraPos.y;
            double local5527_12 = MathHelper.lerp(tickProgress, local5527_7.field11859, local5527_7.field11856) - cameraPos.z;
            CosmeticModel cosmeticModel = CosmeticModel.method1889(local5527_7.cosmeticEntry);
            if (cosmeticModel != null) {
               CosmeticPlacement cosmeticPlacement = local5527_7.cosmeticEntry.placement();
               float local5527_16 = local5527_7.field11865 + MathHelper.wrapDegrees(local5527_7.field11864 - local5527_7.field11865) * tickProgress;
               float local5527_17 = Math.max(0.05F, cosmeticPlacement.scale());
               matrixStack.push();
               matrixStack.translate(local5527_8, local5527_10, local5527_12);
               matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - local5527_16));
               matrixStack.translate(cosmeticPlacement.x(), cosmeticPlacement.y(), cosmeticPlacement.z());
               matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cosmeticPlacement.rotationX()));
               matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cosmeticPlacement.rotationY()));
               matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cosmeticPlacement.rotationZ()));
               matrixStack.scale(local5527_17, local5527_17, local5527_17);
               cosmeticModel.method1884(
                  matrixStack,
                  vertexConsumerProvider,
                  15728880,
                  (local5527_7.field11870 + tickProgress) / 20.0F,
                  MathHelper.lerp(tickProgress, local5527_7.field11868, local5527_7.field11867)
               );
               matrixStack.pop();
            }
         }
      }
   }

   private double method3372(MinecraftClient minecraftClient, double local5528_2, double local5528_4, double local5528_6) {
      return this.method3366(minecraftClient, new Vec3d(local5528_2, local5528_6 + 4.0, local5528_4), new Vec3d(local5528_2, local5528_6 - 12.0, local5528_4));
   }

   private static double method3373(Vec3d vec3d, Vec3d vec3d2) {
      double local5529_2 = vec3d.x - vec3d2.x;
      double local5529_4 = vec3d.z - vec3d2.z;
      return local5529_2 * local5529_2 + local5529_4 * local5529_4;
   }

   private boolean method3374(MinecraftClient minecraftClient, CosmeticMotionTracker.Nested0486 local5530_2, double local5530_3, double local5530_5) {
      double local5530_7 = this.method3372(minecraftClient, local5530_3, local5530_5, local5530_2.field11855);
      if (Double.isNaN(local5530_7) || local5530_7 < local5530_2.field11855 - 1.25) {
         local5530_7 = this.method3375(minecraftClient, local5530_3, local5530_5, local5530_2.field11855);
      }

      if (!Double.isNaN(local5530_7) && !(local5530_7 > local5530_2.field11855 + 1.25)) {
         double local5530_9 = local5530_3 - local5530_2.field11854;
         double local5530_11 = local5530_5 - local5530_2.field11856;
         local5530_2.field11854 = local5530_3;
         local5530_2.field11856 = local5530_5;
         if (local5530_7 < local5530_2.field11855 - 1.25) {
            this.method3363(local5530_2, local5530_7);
         } else {
            local5530_2.field11855 = local5530_7;
            local5530_2.field11862 = 0.0;
         }

         local5530_2.field11864 = local5530_2.field11864
            + MathHelper.wrapDegrees((float)Math.toDegrees(Math.atan2(-local5530_9, local5530_11)) - local5530_2.field11864) * 0.28F;
         return true;
      } else {
         return false;
      }
   }

   private double method3375(MinecraftClient minecraftClient, double local5531_2, double local5531_4, double local5531_6) {
      return this.method3366(minecraftClient, new Vec3d(local5531_2, local5531_6 + 0.35, local5531_4), new Vec3d(local5531_2, local5531_6 - 64.0, local5531_4));
   }

   private Vec3d method3376(CosmeticMotionTracker.Nested0486 local5532_1, Vec3d vec3d, double local5532_3, double local5532_5) {
      if (--local5532_1.field11869 <= 0
         || method3373(new Vec3d(local5532_1.field11860, vec3d.y, local5532_1.field11861), vec3d) > local5532_5 * local5532_5) {
         double local5532_7 = local5532_1.field11853.nextDouble() * Math.PI * 2.0;
         double local5532_9 = local5532_3 + local5532_1.field11853.nextDouble() * (local5532_5 - local5532_3);
         local5532_1.field11860 = vec3d.x + Math.cos(local5532_7) * local5532_9;
         local5532_1.field11861 = vec3d.z + Math.sin(local5532_7) * local5532_9;
         local5532_1.field11869 = 45 + local5532_1.field11853.nextInt(90);
      }

      return new Vec3d(local5532_1.field11860, vec3d.y, local5532_1.field11861);
   }

   private void method3377(MinecraftClient minecraftClient, AbstractClientPlayerEntity abstractClientPlayerEntity, CosmeticMotionTracker.Nested0486 local5533_3) {
      boolean local5533_4 = local5533_3.cosmeticEntry.placement().flying();
      Vec3d vec3d = abstractClientPlayerEntity.getEntityPos();
      boolean local5533_6 = local5533_3.field11879 != null && method3373(vec3d, local5533_3.field11879) > 4.0E-4;
      local5533_3.field11879 = vec3d;
      if (local5533_6) {
         local5533_3.field11866 = abstractClientPlayerEntity.bodyYaw;
      }

      Vec3d vec3d2 = method3364(vec3d, local5533_3.field11866, local5533_4);
      if (local5533_4) {
         vec3d2 = new Vec3d(vec3d2.x, vec3d.y + 1.3, vec3d2.z);
      }

      local5533_3.field11857 = local5533_3.field11854;
      local5533_3.field11858 = local5533_3.field11855;
      local5533_3.field11859 = local5533_3.field11856;
      local5533_3.field11865 = local5533_3.field11864;
      local5533_3.field11868 = local5533_3.field11867;
      local5533_3.field11870++;
      double local5533_8 = method3373(new Vec3d(local5533_3.field11854, local5533_3.field11855, local5533_3.field11856), vec3d);
      double local5533_10 = local5533_4 ? 4.0 : 7.0;
      double local5533_12 = local5533_4 ? 3.5 : 6.0;
      if (local5533_8 > local5533_10 * local5533_10) {
         local5533_3.field11877 = true;
      } else if (local5533_8 < local5533_12 * local5533_12) {
         local5533_3.field11877 = false;
      }

      Vec3d vec3d3 = local5533_3.field11877 ? vec3d2 : this.method3376(local5533_3, vec3d, local5533_4 ? 3.0 : 1.4, local5533_4 ? 4.0 : 6.5);
      if (local5533_4) {
         vec3d3 = new Vec3d(vec3d3.x, vec3d.y + 1.3, vec3d3.z);
      }

      boolean local5533_15 = this.method3370(minecraftClient, local5533_3, vec3d3, local5533_3.field11877 ? 0.22 : 0.075, local5533_4);
      local5533_3.field11867 = local5533_3.field11867 + ((local5533_15 ? 1.0F : 0.0F) - local5533_3.field11867) * 0.25F;
   }

   private static final class Nested0486 {
      private final CosmeticEntry cosmeticEntry;
      private final Random field11853;
      private double field11854;
      private double field11855;
      private double field11856;
      private double field11857;
      private double field11858;
      private double field11859;
      private double field11860;
      private double field11861;
      private double field11862;
      private double field11863;
      private float field11864;
      private float field11865;
      private float field11866;
      private float field11867;
      private float field11868;
      private int field11869;
      private int field11870;
      private int field11871;
      private final int field11872;
      private double field11873;
      private double field11874;
      private double field11875;
      private double field11876;
      private boolean field11877;
      private boolean field11878;
      private Vec3d field11879;

      private void method3378(double local5534_1, double local5534_3, double local5534_5) {
         this.field11854 = this.field11857 = this.field11860 = local5534_1;
         this.field11855 = this.field11858 = local5534_3;
         this.field11856 = this.field11859 = this.field11861 = local5534_5;
         this.field11862 = 0.0;
      }

      private void method3379() {
         this.field11857 = this.field11854;
         this.field11858 = this.field11855;
         this.field11859 = this.field11856;
         this.field11865 = this.field11864;
         this.field11868 = this.field11867;
      }

      private Nested0486(CosmeticEntry initialCosmeticEntry, long local5536_2, double local5536_4, double local5536_6, double local5536_8, float local5536_10) {
         this.cosmeticEntry = initialCosmeticEntry;
         this.field11853 = new Random(local5536_2);
         this.field11872 = (int)(local5536_2 & 2147483647L) % 4;
         this.field11854 = this.field11857 = this.field11860 = local5536_4;
         this.field11855 = this.field11858 = local5536_6;
         this.field11856 = this.field11859 = this.field11861 = local5536_8;
         this.field11864 = this.field11865 = this.field11866 = local5536_10;
         this.field11863 = this.field11853.nextBoolean() ? 1.0 : -1.0;
      }
   }
}
