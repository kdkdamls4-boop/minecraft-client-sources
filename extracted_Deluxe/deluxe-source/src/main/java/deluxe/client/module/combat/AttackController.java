package deluxe.client.module.combat;

import deluxe.client.util.EntityUtils;
import deluxe.client.util.InventoryUtils;

import deluxe.client.module.combat.AimAssistModule;
import deluxe.client.module.movement.AirStuckModule;
import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.module.combat.MaceHelperModule;
import deluxe.client.util.MathUtils;
import deluxe.client.MinecraftAccess;
import deluxe.client.util.Stopwatch;
import deluxe.client.module.combat.TriggerBotModule;

import java.util.Arrays;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Items;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public final class AttackController implements MinecraftAccess {
   private static final Stopwatch stopwatch = new Stopwatch();
   private static int field03222;
   private static long field03223;
   private static boolean field03224;
   private static int field03225;
   private static float field03226 = MathUtils.method3816().nextBoolean() ? 0.0F : 0.1F;

   public static Stopwatch method1145() {
      return stopwatch;
   }

   public static boolean method1146() {
      return method1169(0L);
   }

   public static boolean method1147(LivingEntity livingEntity, Runnable runnable, Runnable runnable2, Hand hand, boolean local1584_4) {
      if (livingEntity != null && mc.interactionManager != null) {
         if (runnable != null) {
            runnable.run();
         }

         mc.interactionManager.attackEntity(mc.player, livingEntity);
         if (local1584_4) {
            method1160();
         } else {
            method1152();
         }

         field03222 = 0;
         field03226 = MathUtils.method3816().nextBoolean() ? 0.0F : 0.1F;
         if (hand != null) {
            mc.player.swingHand(hand);
         }

         if (runnable2 != null) {
            runnable2.run();
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean method1148() {
      return field03223 % 7L == 3L;
   }

   public static boolean method1150(LivingEntity livingEntity, float local1587_1) {
      return livingEntity != null && mc.player != null ? mc.player.squaredDistanceTo(livingEntity) <= local1587_1 * local1587_1 : true;
   }

   public static void method1151() {
      field03222++;
   }

   public static void method1152() {
      field03223 = 0L;
   }

   public static float method1153() {
      return Math.min(field03222 / 10.0F, 1.0F);
   }

   public static long method1154() {
      return field03222 * 50L;
   }

   public static void method1155() {
      field03224 = false;
      field03225 = 0;
   }

   public static boolean method1156(
      LivingEntity livingEntity, boolean local1593_1, boolean local1593_2, boolean local1593_3, long local1593_4, float[] floatArray
   ) {
      float local1593_7 = floatArray.length > 2 ? floatArray[2] : floatArray[0];
      if (local1593_2 && livingEntity != null && !method1150(livingEntity, local1593_7)) {
         return false;
      }

      if (!method1169(local1593_4)) {
         return false;
      }

      boolean local1593_8 = method1167(local1593_3);
      if (local1593_8 && local1593_1 && !method1163(livingEntity, floatArray[0])) {
         local1593_8 = false;
      }

      return local1593_8;
   }

   private static int method1158() {
      return 3;
   }

   public static double method1159(int local1596_0) {
      if (mc.world == null) {
         return 1.0;
      }

      Vec3d eyePos = mc.player.getEyePos();
      double local1596_2 = local1596_0 * 2.0;
      double local1596_4 = 255.0;
      double local1596_6 = -64.0;
      float local1596_8 = mc.player.getWidth() / 2.0F - 0.01F;

      for (Vec3d vec3d : Arrays.asList(
         eyePos.add(-local1596_8, 0.0, -local1596_8),
         eyePos.add(local1596_8, 0.0, local1596_8),
         eyePos.add(local1596_8, 0.0, -local1596_8),
         eyePos.add(-local1596_8, 0.0, local1596_8)
      )) {
         double local1596_11 = mc.world
            .raycast(new RaycastContext(vec3d, vec3d.add(0.0, -local1596_0, 0.0), ShapeType.VISUAL, FluidHandling.ANY, mc.player))
            .getPos()
            .y;
         double local1596_13 = mc.world
            .raycast(new RaycastContext(vec3d, vec3d.add(0.0, local1596_0, 0.0), ShapeType.VISUAL, FluidHandling.ANY, mc.player))
            .getPos()
            .y;
         if (local1596_4 > local1596_13) {
            local1596_4 = local1596_13;
         }

         if (local1596_6 < local1596_11) {
            local1596_6 = local1596_11;
         }

         if (local1596_2 > local1596_4 - local1596_6) {
            local1596_2 = local1596_4 - local1596_6;
         }
      }

      return local1596_2 - mc.player.getHeight();
   }

   public static void method1160() {
      field03223++;
   }

   public static double method1161() {
      double local1599_0 = mc.player.fallDistance;
      if (mc.world != null
         && !mc.player.isOnGround()
         && mc.player.getVelocity().y < -0.0784000015258789
         && !mc.world.getBlockState(mc.player.getBlockPos()).getFluidState().isIn(FluidTags.WATER)
         && !mc.world.getBlockState(mc.player.getBlockPos().up()).getFluidState().isIn(FluidTags.WATER)) {
         local1599_0 = -mc.player.getVelocity().y;
      }

      return local1599_0;
   }

   public static boolean method1162(LivingEntity livingEntity, boolean local1600_1, boolean local1600_2, long local1600_3, float[] floatArray) {
      return method1156(livingEntity, local1600_1, true, local1600_2, local1600_3, floatArray);
   }

   public static boolean method1163(LivingEntity entity, double local1601_1) {
      return entity != null && EntityUtils.method2803(entity, local1601_1, mc.player.getYaw(), mc.player.getPitch());
   }

   public static void method1164(LivingEntity livingEntity, boolean local1602_1, boolean local1602_2, boolean local1602_3) {
      if (livingEntity == null || field03225 == 0 || !local1602_3 || livingEntity.hurtTime != 0) {
         method1155();
      }

      if (local1602_3
         && livingEntity != null
         && !(livingEntity instanceof PlayerEntity)
         && !mc.player.isUsingItem()
         && method1171(method1148() ? 250L : 150L)
         && mc.player.handSwinging) {
         if (!field03224 && field03225 == 0 && livingEntity.hurtTime == 0) {
            field03224 = true;
            field03225 = method1158();
         }

         if (field03224
            && field03225 > 0
            && (!local1602_2 || method1163(livingEntity, 6.0))
            && method1147(livingEntity, () -> {}, () -> {}, Hand.MAIN_HAND, local1602_1)) {
            field03225--;
         }
      }
   }

   private static LivingEntity method1165() {
      return AttackAuraModule.method1029().method1005() != null ? AttackAuraModule.method1029().method1005() : TriggerBotModule.method4593().method4599();
   }

   private static boolean method1166() {
      return AttackAuraModule.method1029().method1037() || TriggerBotModule.method4593().method4589() || CriticalHitUtils.method0962();
   }

   public static boolean method1167(boolean local1605_0) {
      if (method1166()
         && mc.player != null
         && !mc.player.isOnGround()
         && InventoryUtils.method0557(Items.MACE) != -1
         && !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getInventory().getStack(InventoryUtils.method0557(Items.MACE)))) {
         if (CriticalHitUtils.method0972(method1165())) {
            return false;
         }

         if (MaceItem.shouldDealAdditionalDamage(mc.player) && mc.player.getVelocity().y < 0.0) {
            return true;
         }
      }

      if (!local1605_0) {
         return true;
      }

      boolean local1605_1 = AttackAuraModule.method1029().isEnabled() && AttackAuraModule.method1029().method1019()
         || TriggerBotModule.method4593().isEnabled() && TriggerBotModule.method4593().method4583()
         || AimAssistModule.method0883().isEnabled() && AimAssistModule.method0883().method0881();
      FluidState fluidState = mc.world.getFluidState(BlockPos.ofFloored(mc.player.getEyePos()));
      boolean local1605_3 = !mc.player.hasStatusEffect(StatusEffects.LEVITATION)
         && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
         && !fluidState.isIn(FluidTags.WATER)
         && !fluidState.isIn(FluidTags.LAVA)
         && !mc.player.getAbilities().flying
         && !mc.player.isGliding()
         && !mc.player.isClimbing()
         && !mc.player.hasVehicle();
      return local1605_3 && mc.player.fallDistance > (AirStuckModule.method2785().method2787() ? 0.0 : method1161()) && !mc.player.isOnGround()
         || local1605_1 && mc.player.isOnGround() && !mc.player.input.playerInput.jump()
         || !local1605_3;
   }

   private static boolean method1168() {
      return MaceHelperModule.method1776().field05761.isChecked();
   }

   private AttackController() {
   }

   public static boolean method1169(long local1608_0) {
      if (mc.player == null) {
         return false;
      } else if (AttackAuraModule.method1029().method0998() || TriggerBotModule.method4593().method4587()) {
         return !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack());
      } else if (mc.player.fallDistance > 1.5) {
         return !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack()) && field03222 > 3L + local1608_0 / 50L;
      } else {
         return mc.player.getMainHandStack().isOf(Items.MACE)
            ? !mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())
               && mc.player.getAttackCooldownProgress(0.5F + (float)Math.max(0L, -local1608_0) / 50.0F) >= 0.9F
            : mc.player.getAttackCooldownProgress(0.5F + (float)Math.max(0L, -local1608_0) / 50.0F) >= 0.9F && field03222 >= 10L + local1608_0 / 50L;
      }
   }

   public static boolean method1170(LivingEntity livingEntity, float[] floatArray, String local1609_2) {
      if ("Быстрый".equals(local1609_2)) {
         if (livingEntity != null
            && method1156(livingEntity, false, false, false, -50L, floatArray)
            && !mc.player.isOnGround()
            && !mc.player.isSubmergedInWater()
            && mc.player.getVelocity().y <= 0.0030162615090425808) {
            return true;
         }
      } else if ("Легитный".equals(local1609_2)
         && livingEntity != null
         && method1156(livingEntity, false, false, false, -150L, floatArray)
         && !mc.player.isOnGround()
         && !mc.player.isSubmergedInWater()
         && mc.player.getVelocity().y <= 0.16477328182606651) {
         return true;
      }

      return false;
   }

   public static boolean method1171(long local1610_0) {
      return field03222 * 50L >= local1610_0;
   }
}
