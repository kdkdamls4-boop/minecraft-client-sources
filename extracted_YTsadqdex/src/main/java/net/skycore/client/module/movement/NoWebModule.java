package net.skycore.client.module.movement;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.ClientLevelGetDestroyingBlocksAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.MovementHelper;

public final class NoWebModule extends ClientModule {
   private static final String MODE_1_16_5 = "ReallyWorld 1.16.5";
   private BlockPos packetTarget;
   public static final NoWebModule INSTANCE = new NoWebModule();
   private final ModeSetting mode = this.add(new ModeSetting("Mode", "ReallyWorld 1.21.4", "ReallyWorld 1.16.5", "ReallyWorld 1.21.4", "Grim"));
   private static final float GRIM_SPEED_MULTIPLIER = 1.05F;
   private static final String MODE_GRIM = "Grim";
   private static final String MODE_1_21_4 = "ReallyWorld 1.21.4";
   private static boolean touchedWebThisTick;

   public static boolean isInWeb(LocalPlayer player) {
      if (player == null) {
         return false;
      }

      if (!touchedWebThisTick) {
         BlockState inBlockState = player.getInBlockState();
         if (!inBlockState.is(Blocks.COBWEB)) {
            return isEntityInWebSampled(player);
         }
      }

      return true;
   }

   public static boolean shouldIgnoreCobwebSlowdown() {
      if (INSTANCE.isEnabled()) {
         if (INSTANCE.mode.is("ReallyWorld 1.21.4")) {
            return true;
         }
      }

      return false;
   }

   private static LinkedHashSet<BlockPos> collectBodyCobwebs(Minecraft minecraft, LocalPlayer player) {
      LinkedHashSet entries = new LinkedHashSet();
      BlockPos blockPos = player.blockPosition();
      addIfCobweb(minecraft, entries, blockPos);
      BlockPos blockPos2 = blockPos.above();
      addIfCobweb(minecraft, entries, blockPos2);
      BlockPos blockPos3 = blockPos.below();
      addIfCobweb(minecraft, entries, blockPos3);
      AABB boundingBox = player.getBoundingBox();
      Iterator iterator = BlockPos.betweenClosed(boundingBox.inflate(0.5, 0.5, 0.5)).iterator();

      while (iterator.hasNext()) {
         BlockPos element = (BlockPos)iterator.next();
         addIfCobweb(minecraft, entries, element);
      }

      return entries;
   }

   private void tickReallyWorld1165() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null && isInWeb(player)) {
         Vec3 deltaMovement = player.getDeltaMovement();
         player.setDeltaMovement(deltaMovement.x, 0.0, deltaMovement.z);
         if (instance.options.keyJump.isDown()) {
            deltaMovement = player.getDeltaMovement();
            player.setDeltaMovement(deltaMovement.x, 0.9, deltaMovement.z);
         }

         if (instance.options.keyShift.isDown()) {
            deltaMovement = player.getDeltaMovement();
            player.setDeltaMovement(deltaMovement.x, -0.9, deltaMovement.z);
         }

         MovementHelper.setMotion(0.21);
      }
   }

   private static boolean isEntityInWebSampled(Entity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (entity != null && instance.level != null) {
         AABB boundingBox = entity.getBoundingBox();
         double computedValue = boundingBox.minX + 0.001;
         double computedValue2 = boundingBox.minZ + 0.001;
         double computedValue3 = boundingBox.maxX - 0.001;
         double computedValue4 = boundingBox.maxZ - 0.001;
         double computedValue5 = boundingBox.maxY - boundingBox.minY;
         double computedValue6 = computedValue5 * 0.119;
         double computedValue7 = boundingBox.minY + computedValue6 * 0.5;
         double computedValue8 = boundingBox.maxY - computedValue6 * 0.5;
         double computedValue9 = (computedValue3 - computedValue) / 4.0;
         double computedValue10 = (computedValue4 - computedValue2) / 4.0;
         double computedValue11 = (computedValue8 - computedValue7) / 8.0;

         for (double index = computedValue; index <= computedValue3 + 1.0E-4; index += computedValue9) {
            for (double index2 = computedValue7; index2 <= computedValue8 + 1.0E-4; index2 += computedValue11) {
               for (double index3 = computedValue2; index3 <= computedValue4 + 1.0E-4; index3 += computedValue10) {
                  if (instance.level.getBlockState(BlockPos.containing(index, index2, index3)).is(Blocks.COBWEB)) {
                     return true;
                  }
               }
            }
         }

         double[][] doubleArrayValues = new double[][]{
            {computedValue, computedValue7, computedValue2},
            {computedValue, computedValue7, computedValue4},
            {computedValue, computedValue8, computedValue2},
            {computedValue, computedValue8, computedValue4},
            {computedValue3, computedValue7, computedValue2},
            {computedValue3, computedValue7, computedValue4},
            {computedValue3, computedValue8, computedValue2},
            {computedValue3, computedValue8, computedValue4}
         };

         for (double[] doubleValues : doubleArrayValues) {
            if (instance.level.getBlockState(BlockPos.containing(doubleValues[0], doubleValues[1], doubleValues[2])).is(Blocks.COBWEB)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   protected void onDisable() {
      this.packetTarget = null;
      touchedWebThisTick = false;
   }

   public static void markWebTouch() {
      touchedWebThisTick = true;
   }

   private void onPreTick(PlayerTickEndEvent event) {
      if (this.isEnabled()) {
         if (this.mode.is("Grim")) {
            this.tickGrim();
         } else {
            if (this.mode.is("ReallyWorld 1.21.4")) {
               this.sendFinishedDiggingWhenActive(Minecraft.getInstance());
            }
         }
      }
   }

   @Override
   protected void onTick() {
      if (this.mode.is("Grim")) {
         this.tickGrim();
         touchedWebThisTick = false;
      } else {
         if (!this.mode.is("ReallyWorld 1.16.5")) {
            touchedWebThisTick = false;
         } else {
            this.tickReallyWorld1165();
            touchedWebThisTick = false;
         }
      }
   }

   private static void addIfCobweb(Minecraft minecraft, Set<BlockPos> blockPoss, BlockPos blockPos) {
      BlockState blockState = minecraft.level.getBlockState(blockPos);
      if (blockState.is(Blocks.COBWEB)) {
         blockPoss.add(blockPos.immutable());
      }
   }

   private NoWebModule() {
      super("No Web", "Removes cobweb slowdown", ModuleCategory.MOVEMENT);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   private static boolean isInGrimObstacle(LocalPlayer player) {
      Minecraft instance = Minecraft.getInstance();
      if (player != null) {
         if (instance.level != null) {
            if (isInWeb(player)) {
               return true;
            }

            BlockPos blockPos = player.blockPosition();
            if (!isWebOrBerries(instance, blockPos)) {
               if (!isWebOrBerries(instance, blockPos.above())) {
                  return false;
               }
            }

            return true;
         }
      }

      return false;
   }

   private void sendFinishedDiggingWhenActive(Minecraft minecraft) {
      LocalPlayer player = minecraft.player;
      if (player != null && minecraft.level != null && player.connection != null) {
         var bodyCobwebs = collectBodyCobwebs(minecraft, player);
         if (bodyCobwebs.isEmpty()) {
            this.packetTarget = null;
         } else {
            this.packetTarget = (BlockPos)bodyCobwebs.getFirst();

            for (BlockPos blockPos : bodyCobwebs) {
               try (BlockStatePredictionHandler blockStatePredictionHandler = ((ClientLevelGetDestroyingBlocksAccessor)minecraft.level)
                     .skycore$getBlockStatePredictionHandler()
                     .startPredicting()) {
                  player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP, blockStatePredictionHandler.currentSequence()));
               }
            }
         }
      } else {
         this.packetTarget = null;
      }
   }

   private void tickGrim() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && player.input != null && instance.level != null && isInGrimObstacle(player)) {
         int value = !instance.options.keyJump.isDown()
               && !instance.options.keyShift.isDown()
               && !instance.options.keyLeft.isDown()
               && !instance.options.keyDown.isDown()
               && !instance.options.keyRight.isDown()
            ? 0
            : 1;
         if (value != 0) {
            this.tickReallyWorld1165();
         } else {
            Vec2 moveVector = player.input.getMoveVector();
            double y = moveVector.y;
            double x = moveVector.x;
            if (y == 0.0 && x == 0.0) {
               Vec3 deltaMovement = player.getDeltaMovement();
               player.setDeltaMovement(deltaMovement.x, 0.0, deltaMovement.z);
            } else {
               float yRot = player.getYRot() * (float) (Math.PI / 180.0);
               double delta = -Math.sin(yRot) * y + Math.cos(yRot) * x;
               double delta2 = Math.cos(yRot) * y + Math.sin(yRot) * x;
               delta *= 0.24149998903274536;
               delta2 *= 0.24149998903274536;
               double delta3 = 0.0;
               if (instance.options.keyJump.isDown()) {
                  delta3 += 0.04199999809265137;
               }

               if (instance.options.keyShift.isDown()) {
                  delta3 -= 0.04199999809265137;
               }

               double squareRoot = Math.sqrt(delta * delta + delta2 * delta2);
               double value2 = 0.5564999747276307;
               if (squareRoot > value2) {
                  double computedValue = value2 / squareRoot;
                  delta *= computedValue;
                  delta2 *= computedValue;
               }

               player.setDeltaMovement(delta, delta3, delta2);
               if (player.horizontalCollision || player.verticalCollision) {
                  Vec3 deltaMovement2 = player.getDeltaMovement();
                  player.setDeltaMovement(deltaMovement2.x, deltaMovement2.y, deltaMovement2.z);
               }
            }
         }
      }
   }

   private static boolean isWebOrBerries(Minecraft minecraft, BlockPos blockPos) {
      if (!minecraft.level.getBlockState(blockPos).is(Blocks.COBWEB)) {
         if (!minecraft.level.getBlockState(blockPos).is(Blocks.SWEET_BERRY_BUSH)) {
            return false;
         }
      }

      return true;
   }
}
