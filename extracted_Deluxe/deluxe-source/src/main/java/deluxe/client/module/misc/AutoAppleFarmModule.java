package deluxe.client.module.misc;

import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.mixin.MinecraftClientMixin3;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.render.FontText;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.render.ThemeColors;
import deluxe.client.util.AimPointUtils;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.util.Stopwatch;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(
   name = "AutoAppleFarm",
   description = "Сажает дуб, растит костной мукой, ломает крону мотыгой и ствол топором, чинит мотыгу опытом с аукциона.",
   category = ModuleCategory.MISC
)
public final class AutoAppleFarmModule extends Module {
   private static AutoAppleFarmModule autoAppleFarmModule;
   public ModeSetting modeSetting;
   public LegacyNumberSetting legacyNumberSetting;
   private AutoAppleFarmModule.Nested0377 field03330 = AutoAppleFarmModule.Nested0377.PLACE_SAPLING;
   private BlockPos field03331;
   private BlockPos field03332;
   private BlockPos field03333;
   private int field03334;
   private int field03335;
   private int field03336;
   private int field03337;
   private Boolean field03338;
   private long field03339;
   private BlockPos field03340;
   private int field03341;
   private int field03342;
   private int field03343;
   private Set<BlockPos> field03344 = new HashSet<>();
   private BlockPos field03345;
   private int field03346;
   private int field03347;
   private int field03348;
   private boolean field03349;
   private final Stopwatch field03350 = new Stopwatch();
   private final Stopwatch field03351 = new Stopwatch();
   private boolean field03352;
   private long field03353;
   private long field03354;
   private long field03355;
   private int field03356;
   private int field03357;
   private Set<Integer> field03358 = new HashSet<>();
   private long field03359;
   private final EventListener<PlayerTickEvent> field03360 = playerTickEvent -> {
      if (this.isEnabled()) {
         if (mc.player != null && mc.world != null) {
            if (mc.currentScreen instanceof GameMenuScreen) {
               this.method1251();
            } else {
               this.method1213();
               this.method1195();
               if (!this.method1245()) {
                  if (this.field03330 != AutoAppleFarmModule.Nested0377.PLACE_SAPLING || !this.method1255()) {
                     if (this.method1228()) {
                        switch (this.field03330) {
                           case PLACE_SAPLING:
                              this.method1204();
                              break;
                           case GROW:
                              this.method1190();
                              break;
                           case BREAK_LEAVES:
                              this.method1234();
                              break;
                           case BREAK_LOG:
                              this.method1193();
                        }
                     }
                  }
               }
            }
         }
      }
   };
   private final EventListener<HudRenderEvent> field03361 = event -> this.onHudRender(event);

   private boolean method1180(BlockPos blockPos) {
      return mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos)) <= mc.player.getBlockInteractionRange() - 0.1;
   }

   private BlockPos method1181() {
      BlockPos blockPos = null;
      double local1634_2 = Double.MAX_VALUE;

      for (int index = -1; index <= 1; index++) {
         for (int index2 = -1; index2 <= 1; index2++) {
            BlockPos blockPos2 = new BlockPos(
               (int)Math.floor(mc.player.getX()) + index,
               (int)Math.floor(mc.player.getY()) - 1,
               (int)Math.floor(mc.player.getZ()) + index2
            );
            if (this.method1205(blockPos2)
               && (mc.world.getBlockState(blockPos2.up()).isAir() || mc.world.getBlockState(blockPos2.up()).isOf(Blocks.OAK_SAPLING))
               && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) <= mc.player.getBlockInteractionRange()
               && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) < local1634_2) {
               local1634_2 = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2));
               blockPos = blockPos2.up().toImmutable();
            }
         }
      }

      return blockPos;
   }

   private float method1182() {
      return 2.0F + ((BossBarHudMixin)mc.inGameHud.getBossBarHud()).getEvents().size() * 19.0F;
   }

   private static String method1185(long local1638_0) {
      return String.format("%02d:%02d:%02d", local1638_0 / 1000L / 3600L, local1638_0 / 1000L % 3600L / 60L, local1638_0 / 1000L % 60L);
   }

   private void method1186(Vec3d vec3d, float local1639_2) {
      this.method1206(AimPointUtils.method4450(vec3d.subtract(mc.player.getEyePos())), local1639_2);
   }

   private boolean method1187(Block block) {
      for (int index = -5; index <= 5; index++) {
         for (int index2 = -5; index2 <= 5; index2++) {
            for (int index3 = -5; index3 <= 5; index3++) {
               if (mc.world
                  .getBlockState(
                     new BlockPos(
                        (int)Math.floor(mc.player.getX()) + index,
                        (int)Math.floor(mc.player.getY()) + index2,
                        (int)Math.floor(mc.player.getZ()) + index3
                     )
                  )
                  .isOf(block)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static AutoAppleFarmModule method1189() {
      if (autoAppleFarmModule == null) {
         autoAppleFarmModule = new AutoAppleFarmModule();
      }

      return autoAppleFarmModule;
   }

   private void method1190() {
      if (this.field03331 == null || !mc.world.getBlockState(this.field03331).isOf(Blocks.OAK_SAPLING)) {
         this.field03330 = AutoAppleFarmModule.Nested0377.BREAK_LEAVES;
      } else if (this.field03335 > 0) {
         this.field03335--;
      } else if (this.method1180(this.field03331)) {
         if (this.method1209() != -1) {
            boolean local1643_1 = this.method1238(this.method1209());
            this.method1236(Vec3d.ofCenter(this.field03331));
            if (local1643_1 && this.method1218(Vec3d.ofCenter(this.field03331))) {
               this.method1201(this.method1241(this.field03331, Direction.UP));
               this.field03335 = 1;
            }
         }
      }
   }

   private BlockPos method1191(Block block) {
      BlockPos blockPos = null;
      double local1644_3 = Double.MAX_VALUE;

      for (int index = -5; index <= 5; index++) {
         for (int index2 = -5; index2 <= 5; index2++) {
            for (int index3 = -5; index3 <= 5; index3++) {
               BlockPos blockPos2 = new BlockPos(
                  (int)Math.floor(mc.player.getX()) + index,
                  (int)Math.floor(mc.player.getY()) + index2,
                  (int)Math.floor(mc.player.getZ()) + index3
               );
               if (mc.world.getBlockState(blockPos2).isOf(block)
                  && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) <= mc.player.getBlockInteractionRange() - 0.1
                  && mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2)) < local1644_3) {
                  local1644_3 = mc.player.getEyePos().distanceTo(Vec3d.ofCenter(blockPos2));
                  blockPos = blockPos2;
               }
            }
         }
      }

      return blockPos;
   }

   private void method1192() {
      this.field03330 = AutoAppleFarmModule.Nested0377.PLACE_SAPLING;
      this.field03331 = null;
      this.field03332 = null;
      this.field03333 = null;
      this.field03334 = 0;
      this.field03335 = 0;
      this.field03336 = 0;
      this.field03337 = 0;
   }

   private void method1193() {
      if (this.method1191(Blocks.OAK_LOG) == null) {
         this.method1251();
         this.method1192();
         this.field03334 = 16;
      } else {
         this.method1219(this.method1191(Blocks.OAK_LOG), 16.0F);
      }
   }

   private int method1194() {
      int local1647_1 = -1;

      for (int index = 0; index < 9; index++) {
         if (mc.player.getInventory().getStack(index).isOf(Items.OAK_SAPLING)
            && (
               local1647_1 == -1
                  || mc.player.getInventory().getStack(index).getCount() > mc.player.getInventory().getStack(local1647_1).getCount()
            )) {
            local1647_1 = index;
         }
      }

      return local1647_1;
   }

   @Override
   protected void onDisable() {
      this.method1251();
      this.field03348 = 0;
      this.field03352 = false;
      this.field03341 = 0;
      this.field03342 = 0;
      this.field03340 = null;
      this.field03346 = 0;
      this.field03347 = 0;
      this.field03345 = null;
      this.field03355 = 0L;
      this.method1240();
      if (this.field03338 != null && mc.options != null) {
         mc.options.pauseOnLostFocus = this.field03338;
         this.field03338 = null;
      }
   }

   private void method1195() {
      if (mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         if (System.currentTimeMillis() >= this.field03359) {
            if (InventoryUtils.method0546(9, 35) != -1) {
               for (int index = 0; index < 9; index++) {
                  if (!this.method1215(index)) {
                     InventoryUtils.method0547(InventoryUtils.method0533(index), 0, SlotActionType.QUICK_MOVE, false);
                     this.field03359 = System.currentTimeMillis() + 250L;
                     return;
                  }
               }
            }
         }
      }
   }

   private boolean method1197() {
      return mc.player.currentScreenHandler instanceof GenericContainerScreenHandler;
   }

   private String method1198() {
      if (this.field03348 == 1) {
         return "BUYING XP";
      } else if (this.field03348 == 2) {
         return "REPAIRING";
      } else if (this.field03341 != 0) {
         return "RESTOCKING";
      } else if (this.field03346 != 0) {
         return "UNLOADING";
      } else {
         return !this.method1253() ? "NO ITEMS" : "WORKING";
      }
   }

   private void method1199() {
      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }
   }

   private boolean method1201(BlockHitResult blockHitResult) {
      if (!this.method1180(blockHitResult.getBlockPos())) {
         return false;
      }

      mc.crosshairTarget = blockHitResult;
      if (!mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, blockHitResult).isAccepted()) {
         return false;
      }

      mc.player.swingHand(Hand.MAIN_HAND);
      return true;
   }

   private void method1202(BlockPos blockPos) {
      mc.crosshairTarget = this.method1241(blockPos, this.method1229(blockPos));
      mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, this.method1241(blockPos, this.method1229(blockPos)));
      mc.player.swingHand(Hand.MAIN_HAND);
   }

   private void method1204() {
      this.method1251();
      if (this.field03334 > 0) {
         this.field03334--;
      } else if (this.field03333 != null) {
         if (this.field03336 > 0) {
            this.field03336--;
         } else if (mc.world.getBlockState(this.field03333).isOf(Blocks.OAK_SAPLING)) {
            this.field03331 = this.field03333.toImmutable();
            this.field03333 = null;
            this.field03337 = 0;
            this.field03330 = AutoAppleFarmModule.Nested0377.GROW;
         } else {
            this.field03333 = null;
            this.field03337++;
            this.field03334 = 2;
            if (this.field03337 >= 6) {
               this.field03337 = 0;
               this.field03334 = 20;
            }
         }
      } else {
         BlockPos blockPos = this.method1181();
         if (blockPos != null) {
            if (mc.world.getBlockState(blockPos).isOf(Blocks.OAK_LOG) || mc.world.getBlockState(blockPos).isOf(Blocks.OAK_LEAVES)) {
               this.field03330 = AutoAppleFarmModule.Nested0377.BREAK_LEAVES;
            } else if (mc.world.getBlockState(blockPos).isOf(Blocks.OAK_SAPLING)) {
               this.field03331 = blockPos.toImmutable();
               this.field03330 = AutoAppleFarmModule.Nested0377.GROW;
            } else if (mc.world.getBlockState(blockPos).isAir()) {
               if (this.method1205(blockPos.down()) && this.method1180(blockPos.down())) {
                  if (InventoryUtils.method0556(itemStack -> itemStack.isOf(Items.OAK_SAPLING), 0, 8) != -1) {
                     boolean local1658_2 = this.method1238(InventoryUtils.method0556(itemStack2 -> itemStack2.isOf(Items.OAK_SAPLING), 0, 8));
                     this.method1236(Vec3d.ofCenter(blockPos.down()).add(0.0, 0.5, 0.0));
                     if (local1658_2 && this.method1218(Vec3d.ofCenter(blockPos.down()).add(0.0, 0.5, 0.0))) {
                        if (!this.method1201(this.method1241(blockPos.down(), Direction.UP))) {
                           this.field03337++;
                           this.field03334 = 2;
                           if (this.field03337 >= 6) {
                              this.field03337 = 0;
                              this.field03334 = 20;
                           }
                        } else {
                           this.field03333 = blockPos.toImmutable();
                           this.field03336 = 2;
                           this.field03334 = 1;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private boolean method1205(BlockPos blockPos) {
      return mc.world.getBlockState(blockPos).isOf(Blocks.GRASS_BLOCK)
         || mc.world.getBlockState(blockPos).isOf(Blocks.DIRT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.COARSE_DIRT)
         || mc.world.getBlockState(blockPos).isOf(Blocks.ROOTED_DIRT);
   }

   private void method1206(RotationAngles rotationAngles, float local1660_2) {
      RotationManager.method0658().method0648(rotationAngles, new RotationSpeed(local1660_2, local1660_2, 45.0F, 45.0F), 300L);
   }

   private void method1207() {
      this.field03348 = 0;
      this.field03352 = false;
      if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE) && InventoryUtils.method0546(0, 35) != -1) {
         InventoryActionQueue.method2656().method2662(InventoryUtils.method0546(0, 35), 40, 1);
      }

      this.method1240();
   }

   private int method1208() {
      return this.field03349 ? this.method1235() : this.method1247();
   }

   private int method1209() {
      return this.modeSetting.isSelected("Озеленитель")
         ? this.method1247()
         : InventoryUtils.method0556(itemStack -> itemStack.isOf(Items.BONE_MEAL), 0, 8);
   }

   private int method1211() {
      int local1665_1 = 0;

      for (int index = 0; index < 36; index++) {
         if (mc.player.getInventory().getStack(index).isEmpty()) {
            local1665_1++;
         }
      }

      return local1665_1;
   }

   private void method1213() {
      for (ItemEntity itemEntity : mc.world
         .getEntitiesByClass(ItemEntity.class, mc.player.getBoundingBox().expand(10.0), itemEntity2 -> itemEntity2.getStack().isOf(Items.APPLE))) {
         if (this.field03358.add(itemEntity.getId())) {
            this.field03356 = this.field03356 + itemEntity.getStack().getCount();
         }
      }
   }

   private boolean method1215(int local1669_1) {
      if (mc.player.getInventory().getStack(local1669_1).isEmpty()) {
         return true;
      } else if (mc.player.getInventory().getStack(local1669_1).isIn(ItemTags.HOES)
         || mc.player.getInventory().getStack(local1669_1).isIn(ItemTags.AXES)) {
         return true;
      } else {
         return mc.player.getInventory().getStack(local1669_1).isOf(Items.BONE_MEAL)
            ? !this.modeSetting.isSelected("Озеленитель")
            : mc.player.getInventory().getStack(local1669_1).isOf(Items.OAK_SAPLING) && local1669_1 == this.method1194();
      }
   }

   private boolean method1216() {
      if (this.field03346 == 0) {
         if (InventoryUtils.method0546(0, 35) != -1 && this.method1211() > 4) {
            return false;
         }

         if (!this.method1232()) {
            return false;
         }

         this.field03345 = this.method1233();
         if (this.field03345 == null) {
            return false;
         }

         this.method1251();
         this.field03346 = 1;
         this.field03347 = 0;
         return true;
      } else if (this.field03346 == 1) {
         if (this.method1197()) {
            this.field03346 = 2;
            this.field03347 = 0;
            return true;
         }

         this.method1236(Vec3d.ofCenter(this.field03345));
         if (this.field03347 % 5 == 0) {
            this.method1202(this.field03345);
         }

         if (++this.field03347 > 60) {
            this.field03346 = 0;
         }

         return true;
      } else if (this.field03346 == 2) {
         if (!this.method1197()) {
            this.field03346 = 0;
            return true;
         }

         if (this.method1232() && this.field03347 <= 200) {
            if (this.field03347++ % 2 == 0 && this.method1249() != null) {
               InventoryUtils.method0544(this.method1249(), 0, SlotActionType.QUICK_MOVE, false);
            }

            return true;
         } else {
            this.field03346 = 3;
            return true;
         }
      } else {
         InventoryUtils.method0549(false);
         this.field03345 = null;
         this.field03346 = 0;
         return true;
      }
   }

   private boolean method1217() {
      if (this.field03341 == 0) {
         if (this.modeSetting.isSelected("Озеленитель")) {
            return false;
         }

         if (InventoryUtils.method0535(Items.BONE_MEAL) > 64) {
            return false;
         }

         this.field03340 = this.method1254(this.field03344, null);
         if (this.field03340 == null) {
            return false;
         }

         this.method1251();
         this.field03343 = InventoryUtils.method0535(Items.BONE_MEAL) + 64 * ThreadLocalRandom.current().nextInt(3, 6);
         this.field03341 = 1;
         this.field03342 = 0;
         return true;
      } else if (this.field03341 == 1) {
         if (this.method1197()) {
            this.field03341 = 2;
            this.field03342 = 0;
            return true;
         }

         this.method1236(Vec3d.ofCenter(this.field03340));
         if (this.field03342 % 5 == 0) {
            this.method1202(this.field03340);
         }

         if (++this.field03342 > 60) {
            this.field03344.add(this.field03340);
            this.field03341 = 0;
         }

         return true;
      } else if (this.field03341 == 2) {
         if (!this.method1197()) {
            this.field03341 = 0;
            return true;
         }

         if (InventoryUtils.method0535(Items.BONE_MEAL) >= this.field03343 || this.field03342 > 200) {
            this.field03341 = 3;
            return true;
         }

         if (this.method1239(Items.BONE_MEAL) == null) {
            if (InventoryUtils.method0535(Items.BONE_MEAL) <= 64) {
               this.field03344.add(this.field03340);
            }

            this.field03341 = 3;
            return true;
         } else {
            if (this.field03342++ % 2 == 0) {
               InventoryUtils.method0544(this.method1239(Items.BONE_MEAL), 0, SlotActionType.QUICK_MOVE, false);
            }

            return true;
         }
      } else {
         InventoryUtils.method0549(false);
         this.field03340 = null;
         this.field03341 = 0;
         return true;
      }
   }

   private boolean method1218(Vec3d vec3d) {
      return this.method1222(AimPointUtils.method4450(vec3d.subtract(mc.player.getEyePos())));
   }

   private void method1219(BlockPos blockPos, float local1673_2) {
      if (!this.method1180(blockPos)) {
         this.method1199();
      } else {
         this.method1186(Vec3d.ofCenter(blockPos), local1673_2);
         BlockHitResult blockHitResult = this.method1243();
         if (blockHitResult == null) {
            this.method1199();
         } else {
            int local1673_4;
            if (mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LEAVES)) {
               local1673_4 = this.method1247();
            } else {
               if (!mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LOG)) {
                  this.method1199();
                  return;
               }

               local1673_4 = this.method1235();
            }

            if (local1673_4 == -1) {
               this.method1199();
            } else if (!this.method1238(local1673_4)) {
               this.method1199();
            } else {
               this.field03332 = blockHitResult.getBlockPos().toImmutable();
               if (mc.currentScreen == null && mc.isWindowFocused()) {
                  mc.options.attackKey.setPressed(true);
               } else {
                  mc.options.attackKey.setPressed(false);
                  mc.interactionManager.updateBlockBreakingProgress(blockHitResult.getBlockPos(), blockHitResult.getSide());
                  mc.player.swingHand(Hand.MAIN_HAND);
               }
            }
         }
      }
   }

   private String method1221() {
      String local1675_1 = "";
      if (InventoryUtils.method0556(itemStack -> itemStack.isIn(ItemTags.HOES), 0, 8) == -1) {
         local1675_1 = local1675_1 + "мотыга, ";
      }

      if (InventoryUtils.method0556(itemStack2 -> itemStack2.isIn(ItemTags.AXES), 0, 8) == -1) {
         local1675_1 = local1675_1 + "топор, ";
      }

      if (InventoryUtils.method0556(itemStack3 -> itemStack3.isOf(Items.OAK_SAPLING), 0, 8) == -1) {
         local1675_1 = local1675_1 + "саженец дуба, ";
      }

      if (!this.modeSetting.isSelected("Озеленитель") && InventoryUtils.method0556(itemStack4 -> itemStack4.isOf(Items.BONE_MEAL), 0, 8) == -1) {
         local1675_1 = local1675_1 + "костная мука, ";
      }

      return local1675_1.isEmpty() ? "-" : local1675_1.substring(0, local1675_1.length() - 2);
   }

   private boolean method1222(RotationAngles rotationAngles) {
      return Math.abs(MathHelper.wrapDegrees(rotationAngles.method4362() - mc.player.getYaw())) <= 12.0F
         && Math.abs(rotationAngles.method4365() - mc.player.getPitch()) <= 12.0F;
   }

   private long method1224() {
      return this.field03355 > 0L ? System.currentTimeMillis() - this.field03355 : 0L;
   }

   private boolean method1227() {
      if (InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) == -1 && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
         if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            if (!genericContainerScreen.getTitle().getString().contains("Подозрительная цена") && !genericContainerScreen.getTitle().getString().contains("Подтверждение покупки")) {
               if (!AuctionHelperModule.method2310().method2331(genericContainerScreen.getTitle().getString())) {
                  return true;
               }

               if (this.field03352) {
                  if (!this.field03351.hasExceededMillis(3000L)) {
                     return true;
                  }

                  this.field03352 = false;
               }

               int local1681_2 = -1;
               long local1681_3 = Long.MAX_VALUE;

               for (int index = 0; index < mc.player.currentScreenHandler.slots.size() - 36 - 9; index++) {
                  if (mc.player.currentScreenHandler.getSlot(index).hasStack()
                     && mc.player.currentScreenHandler.getSlot(index).getStack().isOf(Items.EXPERIENCE_BOTTLE)
                     && !AuctionHelperModule.method2310()
                        .method2321(
                           mc.player.currentScreenHandler.getSlot(index).getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "")
                        )
                        .contains("не актуален")
                     && !AuctionHelperModule.method2310()
                        .method2321(
                           mc.player.currentScreenHandler.getSlot(index).getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", "")
                        )
                        .matches(".*\\[\\s*\\d+\\s*ур.*")
                     && !mc.player
                        .currentScreenHandler
                        .getSlot(index)
                        .getStack()
                        .getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT)
                        .lines()
                        .stream()
                        .anyMatch(
                           text -> AuctionHelperModule.method2310()
                              .method2321(text.getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                              .matches(".*содержит.*опыт.*")
                        )
                     && AutoMineModule.method3460().method3507(mc.player.currentScreenHandler.getSlot(index).getStack())
                     && AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack()) >= 0
                     && !(
                        AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack())
                           > this.legacyNumberSetting.getValue()
                     )
                     && AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack()) < local1681_3) {
                     local1681_2 = index;
                     local1681_3 = AuctionHelperModule.method2310().method2314(mc.player.currentScreenHandler.getSlot(index).getStack());
                  }
               }

               if (local1681_2 != -1) {
                  InventoryUtils.method0547(local1681_2, 0, SlotActionType.PICKUP, false);
                  this.field03352 = true;
                  this.field03351.reset();
                  this.field03350.reset();
                  return true;
               }

               if (this.field03350.hasExceededMillis(400L)) {
                  InventoryUtils.method0547(mc.player.currentScreenHandler.slots.size() - 36 - 5, 0, SlotActionType.PICKUP, false);
                  this.field03350.reset();
               }

               return true;
            } else if (mc.player
               .currentScreenHandler
               .slots
               .stream()
               .anyMatch(
                  slot -> slot.hasStack()
                     && AuctionHelperModule.method2310()
                        .method2321(slot.getStack().getName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))
                        .contains("не актуален")
               )) {
               mc.player.closeHandledScreen();
               mc.player.networkHandler.sendChatCommand("ah search опыт");
               this.field03352 = false;
               this.field03350.reset();
               return true;
            } else {
               InventoryUtils.method0547(0, 0, SlotActionType.QUICK_MOVE, false);
               this.field03350.reset();
               return true;
            }
         } else {
            if (this.field03350.hasExceededMillis(3000L)) {
               mc.player.networkHandler.sendChatCommand("ah search опыт");
               this.field03350.reset();
            }

            return true;
         }
      } else {
         if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         this.field03352 = false;
         this.field03348 = 2;
         this.field03350.reset();
         return true;
      }
   }

   private boolean method1228() {
      if (this.method1253()) {
         return true;
      }

      this.method1251();
      if (System.currentTimeMillis() < this.field03339) {
         return false;
      }

      this.field03339 = System.currentTimeMillis() + 8000L;
      ClientChat.method2256("&8[&bDeluxe Client&8] &fВ хотбаре не хватает: &c" + this.method1221());
      return false;
   }

   private Direction method1229(BlockPos blockPos) {
      if (Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).y)
            > Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x)
         && Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).y)
            > Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z)) {
         return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).y >= 0.0 ? Direction.UP : Direction.DOWN;
      } else if (Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x)
         >= Math.abs(mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z)) {
         return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).x >= 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return mc.player.getEyePos().subtract(Vec3d.ofCenter(blockPos)).z >= 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null && this.isEnabled()) {
         FontText.method1405(
            FontFace.SEMI, "AutoAppleFarm — Информация", event.method4462() / 2.0F, this.method1182(), 7.0F, ThemeColors.field13665
         );
         FontText.method1405(
            FontFace.SEMI,
            "Текущее состояние: " + this.method1198(),
            event.method4462() / 2.0F,
            this.method1182() + 10.0F,
            7.0F,
            ThemeColors.field13665
         );
         FontText.method1405(
            FontFace.SEMI,
            "Налутано яблок: " + this.field03356,
            event.method4462() / 2.0F,
            this.method1182() + 20.0F,
            7.0F,
            ThemeColors.field13665
         );
         FontText.method1405(
            FontFace.SEMI,
            "Время работы: " + method1185(this.method1224()),
            event.method4462() / 2.0F,
            this.method1182() + 30.0F,
            7.0F,
            ThemeColors.field13665
         );
      }
   }

   private boolean method1232() {
      return InventoryUtils.method0535(Items.OAK_LOG) > 0
         || InventoryUtils.method0535(Items.OAK_LEAVES) > 0
         || InventoryUtils.method0535(Items.STICK) > 0
         || InventoryUtils.method0535(Items.APPLE) > 0
         || InventoryUtils.method0535(Items.OAK_SAPLING) > 64;
   }

   private BlockPos method1233() {
      return this.method1254(null, Blocks.HOPPER);
   }

   @Override
   protected void onEnable() {
      if (mc.options != null) {
         this.field03338 = mc.options.pauseOnLostFocus;
         mc.options.pauseOnLostFocus = false;
      }

      this.field03355 = System.currentTimeMillis();
      this.field03356 = 0;
      this.field03357 = mc.player != null ? InventoryUtils.method0535(Items.APPLE) : 0;
      this.field03339 = 0L;
      this.field03353 = 0L;
      this.field03354 = 0L;
      this.field03359 = 0L;
      this.field03358.clear();
      this.field03344.clear();
      this.method1192();
   }

   private void method1234() {
      if (this.method1191(Blocks.OAK_LEAVES) != null) {
         this.method1219(this.method1191(Blocks.OAK_LEAVES), 32.0F);
      } else if (this.method1187(Blocks.OAK_LEAVES) && this.method1191(Blocks.OAK_LOG) != null) {
         this.method1219(this.method1191(Blocks.OAK_LOG), 16.0F);
      } else {
         this.method1251();
         this.field03330 = AutoAppleFarmModule.Nested0377.BREAK_LOG;
      }
   }

   private int method1235() {
      return InventoryUtils.method0556(itemStack -> itemStack.isIn(ItemTags.AXES), 0, 8);
   }

   private void method1236(Vec3d vec3d) {
      this.method1186(vec3d, 32.0F);
   }

   private boolean method1237(ItemStack itemStack) {
      return itemStack.isOf(Items.OAK_LEAVES) || itemStack.isOf(Items.OAK_LOG) || itemStack.isOf(Items.STICK) || itemStack.isOf(Items.APPLE);
   }

   private boolean method1238(int local1693_1) {
      if (local1693_1 < 0) {
         return false;
      }

      if (mc.player.getInventory().getSelectedSlot() != local1693_1) {
         if (System.currentTimeMillis() < this.field03353) {
            return false;
         }

         this.method1251();
         InventoryUtils.method0553(local1693_1, false);
         this.field03353 = System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(300L, 501L);
         this.field03354 = System.currentTimeMillis() + 150L;
         return false;
      } else {
         return System.currentTimeMillis() >= this.field03354;
      }
   }

   private Slot method1239(Item item) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id < genericContainerScreenHandler.getRows() * 9 && slot.hasStack() && slot.getStack().isOf(item)) {
               return slot;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void method1240() {
      RotationManager.method0658().method0653();
      if (mc.player != null) {
         mc.player.setYaw(RotationAngles.method4366());
         mc.player.setPitch(RotationAngles.method4364());
         mc.player.headYaw = RotationAngles.method4366();
         mc.player.bodyYaw = RotationAngles.method4366();
         mc.player.lastYaw = RotationAngles.method4366();
         mc.player.lastPitch = RotationAngles.method4364();
      }
   }

   private BlockHitResult method1241(BlockPos blockPos, Direction direction) {
      return new BlockHitResult(
         Vec3d.ofCenter(blockPos).add(direction.getOffsetX() * 0.5, direction.getOffsetY() * 0.5, direction.getOffsetZ() * 0.5),
         direction,
         blockPos,
         false
      );
   }

   private float method1242(ItemStack itemStack) {
      return !itemStack.isEmpty() && itemStack.isDamageable() && itemStack.getMaxDamage() > 0
         ? 1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage()
         : 1.0F;
   }

   private BlockHitResult method1243() {
      BlockHitResult blockHitResult = mc.world
         .raycast(
            new RaycastContext(
               mc.player.getEyePos(),
               mc.player
                  .getEyePos()
                  .add(
                     new RotationAngles(mc.player.getYaw(), mc.player.getPitch())
                        .method4363()
                        .multiply(mc.player.getBlockInteractionRange())
                  ),
               ShapeType.OUTLINE,
               FluidHandling.NONE,
               mc.player
            )
         );
      if (blockHitResult.getType() != Type.BLOCK) {
         return null;
      } else {
         return !mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LEAVES)
               && !mc.world.getBlockState(blockHitResult.getBlockPos()).isOf(Blocks.OAK_LOG)
            ? null
            : blockHitResult;
      }
   }

   private boolean method1244() {
      if (mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      if (!mc.player.currentScreenHandler.getCursorStack().isEmpty() && InventoryUtils.method0546(0, 35) != -1) {
         InventoryUtils.method0547(InventoryUtils.method0533(InventoryUtils.method0546(0, 35)), 0, SlotActionType.PICKUP, false);
         return true;
      }

      if (this.method1208() == -1 || this.method1242(mc.player.getInventory().getStack(this.method1208())) >= 0.9F) {
         this.method1207();
         return false;
      }

      if (!this.method1238(this.method1208())) {
         return true;
      }

      if (mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)) {
         this.method1248(new RotationAngles(mc.player.getYaw(), 90.0F));
         if (mc.player.getPitch() < 75.0F) {
            return true;
         }

         ((MinecraftClientMixin3)mc).setRightClickDelay(0);
         InventoryUtils.method0542(Hand.OFF_HAND);
         mc.player.swingHand(Hand.OFF_HAND);
         return true;
      } else if (InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) != -1) {
         if (!InventoryActionQueue.method2656().method2665()) {
            InventoryActionQueue.method2656().method2662(InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE), 40, 1);
         }

         return true;
      } else {
         this.field03348 = 1;
         this.field03352 = false;
         this.field03350.reset();
         return true;
      }
   }

   private boolean method1245() {
      if (this.field03348 != 0) {
         this.method1251();
         return this.field03348 == 1 ? this.method1227() : this.method1244();
      }

      if (this.method1247() != -1 && this.method1242(mc.player.getInventory().getStack(this.method1247())) < 0.15F) {
         this.field03349 = false;
      } else {
         if (this.method1235() == -1 || !(this.method1242(mc.player.getInventory().getStack(this.method1235())) < 0.15F)) {
            return false;
         }

         this.field03349 = true;
      }

      this.field03348 = InventoryUtils.method0557(Items.EXPERIENCE_BOTTLE) == -1 && !mc.player.getOffHandStack().isOf(Items.EXPERIENCE_BOTTLE)
         ? 1
         : 2;
      this.field03352 = false;
      this.field03350.reset();
      return true;
   }

   private AutoAppleFarmModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting(
            "Рост", "Чем растить саженец: костной мукой или зачарованием Озеленитель на мотыге.", "Костная мука", "Костная мука", "Озеленитель"
         )
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Цена стака опыта", "Дороже этого стак опыта на аукционе не покупать.", 200000.0, 10000.0, 1000000.0, 10000.0)
      );
   }

   private int method1247() {
      return InventoryUtils.method0556(itemStack -> itemStack.isIn(ItemTags.HOES), 0, 8);
   }

   private void method1248(RotationAngles rotationAngles) {
      this.method1206(rotationAngles, 32.0F);
   }

   private Slot method1249() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
         for (Slot slot : genericContainerScreenHandler.slots) {
            if (slot.id >= genericContainerScreenHandler.getRows() * 9 && slot.hasStack()) {
               if (this.method1237(slot.getStack())) {
                  return slot;
               }

               if (slot.getStack().isOf(Items.OAK_SAPLING) && InventoryUtils.method0535(Items.OAK_SAPLING) - slot.getStack().getCount() >= 64
                  )
                {
                  return slot;
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private void method1251() {
      if (mc.interactionManager != null && mc.interactionManager.isBreakingBlock()) {
         mc.interactionManager.cancelBlockBreaking();
      }

      if (mc.options != null) {
         mc.options.attackKey.setPressed(false);
      }

      this.field03332 = null;
   }

   private boolean method1253() {
      return InventoryUtils.method0556(itemStack -> itemStack.isIn(ItemTags.HOES), 0, 8) != -1
         && InventoryUtils.method0556(itemStack2 -> itemStack2.isIn(ItemTags.AXES), 0, 8) != -1
         && InventoryUtils.method0556(itemStack3 -> itemStack3.isOf(Items.OAK_SAPLING), 0, 8) != -1
         && (this.modeSetting.isSelected("Озеленитель") || InventoryUtils.method0556(itemStack4 -> itemStack4.isOf(Items.BONE_MEAL), 0, 8) != -1);
   }

   private BlockPos method1254(Set<BlockPos> local1710_1, Block block) {
      BlockPos blockPos = null;
      double local1710_4 = Double.MAX_VALUE;

      for (int index = -5; index <= 5; index++) {
         for (int index2 = -5; index2 <= 5; index2++) {
            for (int index3 = -5; index3 <= 5; index3++) {
               BlockPos blockPos2 = mc.player.getBlockPos().add(index, index2, index3);
               if ((mc.world.getBlockState(blockPos2).isOf(Blocks.CHEST) || mc.world.getBlockState(blockPos2).isOf(Blocks.TRAPPED_CHEST))
                  && (local1710_1 == null || !local1710_1.contains(blockPos2))
                  && (block == null || mc.world.getBlockState(blockPos2.down()).isOf(block))
                  && this.method1180(blockPos2)
                  && mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos2)) < local1710_4) {
                  local1710_4 = mc.player.getEyePos().squaredDistanceTo(Vec3d.ofCenter(blockPos2));
                  blockPos = blockPos2.toImmutable();
               }
            }
         }
      }

      return blockPos;
   }

   private boolean method1255() {
      return this.method1216() ? true : this.method1217();
   }

   private enum Nested0377 {
      PLACE_SAPLING,
      GROW,
      BREAK_LEAVES,
      BREAK_LOG;
   }
}
