package net.skycore.client.module.player;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.setting.BooleanSetting;

public final class AutoPenisBuilderModule extends ClientModule {
   private final SliderSetting distance;
   public static final AutoPenisBuilderModule INSTANCE = new AutoPenisBuilderModule();
   private BlockPos origin;
   private final SliderSetting delay;
   private final ModeSetting facing = this.add(new ModeSetting("Facing", "Auto", "Auto", "North", "South", "East", "West"));
   private Direction direction;
   private final BooleanSetting rotate = this.add(new BooleanSetting("Rotation", true));
   private long next;

   private int findBlock(Minecraft minecraft) {

      for (int value = 0; value < 9; value++) {
         if (minecraft.player.getInventory().getItem(value).getItem() instanceof BlockItem) {
            return value;
         }
      }

      return -1;
   }

   private AutoPenisBuilderModule() {
      super("Auto Penis Builder", "Automatically builds a five-block shape", ModuleCategory.PLAYER);
      this.distance = this.add(new SliderSetting("Distance", 2.0, 1.0, 4.0, 1.0));
      this.delay = this.add(new SliderSetting("Delay", 50.0, 0.0, 250.0, 10.0));
   }

   @Override
   protected void onEnable() {
      this.origin = null;
      this.next = 0L;
   }

   @Override
   protected void onDisable() {
      this.origin = null;
      RotationManager.INSTANCE.stopRotation();
   }

   private AutoPenisBuilderModule.Placement placement(Minecraft minecraft, BlockPos blockPos) {

      for (Direction direction2 : Direction.values()) {
         Direction opposite = direction2.getOpposite();
         BlockPos blockPos2 = blockPos.relative(opposite);
         BlockState blockState = minecraft.level.getBlockState(blockPos2);
         if (!blockState.isAir()) {
            if (!minecraft.level.getBlockState(blockPos2).canBeReplaced()) {
               Vec3 position = Vec3.atCenterOf(blockPos2);
               Vec3 unitVec3i = Vec3.atLowerCornerOf(direction2.getUnitVec3i());
               Vec3 position2 = position.add(unitVec3i.scale(0.5));
               return new AutoPenisBuilderModule.Placement(new BlockHitResult(position2, direction2, blockPos2, false));
            }
         }
      }

      return null;
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null && instance.gameMode != null && instance.gui.screen() == null) {
         if (this.origin == null) {
            this.direction = this.resolve(instance);
            this.origin = instance.player.blockPosition().relative(this.direction, this.distance.get().intValue());

            while (this.origin.getY() > instance.level.getMinY() && instance.level.getBlockState(this.origin.below()).canBeReplaced()) {
               this.origin = this.origin.below();
            }
         }

         var clockWise = List.of(
            this.origin,
            this.origin.relative(this.direction.getClockWise()),
            this.origin.relative(this.direction.getCounterClockWise()),
            this.origin.above(),
            this.origin.above(2)
         );
         BlockPos blockState = clockWise.stream().filter(blockPos -> instance.level.getBlockState(blockPos).canBeReplaced()).findFirst().orElse(null);
         if (blockState == null) {
            SendDonationHelper.displayInfo("AutoPenisBuilder: done");
            this.setEnabled(false);
         } else if (System.currentTimeMillis() >= this.next) {
            int block = this.findBlock(instance);
            if (block < 0) {
               SendDonationHelper.displayWarning("AutoPenisBuilder: no blocks available");
               this.setEnabled(false);
            } else {
               AutoPenisBuilderModule.Placement placement = this.placement(instance, blockState);
               if (placement != null) {
                  if (this.rotate.get()) {
                     Vec3 location = placement.hit.getLocation().subtract(instance.player.getEyePosition());
                     float angle = (float)Math.toDegrees(Math.atan2(location.z, location.x)) - 90.0F;
                     float angle2 = (float)(-Math.toDegrees(Math.atan2(location.y, Math.hypot(location.x, location.z))));
                     RotationManager.update(new Rotation(angle, angle2), 360.0F, 360.0F, 40.0F, 40.0F, 3, 50, false);
                  }

                  int selectedSlot = instance.player.getInventory().getSelectedSlot();
                  instance.player.getInventory().setSelectedSlot(block);
                  if (instance.gameMode.useItemOn(instance.player, InteractionHand.MAIN_HAND, placement.hit).consumesAction()) {
                     instance.player.swing(InteractionHand.MAIN_HAND);
                  }

                  instance.player.getInventory().setSelectedSlot(selectedSlot);
                  this.next = System.currentTimeMillis() + this.delay.get().longValue();
               }
            }
         }
      }
   }

   private Direction resolve(Minecraft minecraft) {
      if (this.facing.is("North")) {
         return Direction.NORTH;
      }

      if (this.facing.is("South")) {
         return Direction.SOUTH;
      }

      if (this.facing.is("East")) {
         return Direction.EAST;
      }

      if (this.facing.is("West")) {
         return Direction.WEST;
      }

      return minecraft.player.getDirection();
   }

   public record Placement(BlockHitResult hit) {
      public BlockHitResult hit() {
         return this.hit;
      }
   }
}
