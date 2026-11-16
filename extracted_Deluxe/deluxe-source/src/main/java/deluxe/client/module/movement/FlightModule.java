package deluxe.client.module.movement;

import deluxe.mixin.ClientPlayerInteractionManagerMixin2;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.render.ThemeColors;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.module.movement.SimulatedPlayerInput;
import deluxe.client.util.Stopwatch;
import deluxe.client.event.WorldRenderEvent;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(name = "Flight", description = "Полёт обычным движением с постоянной скоростью.", category = ModuleCategory.MOVEMENT)
public final class FlightModule extends Module {
   private static FlightModule flightModule;
   public ModeSetting modeSetting;
   public NumberSetting numberSetting;
   private final HashMap<BlockPos, Long> field09839 = new HashMap<>();
   private final Stopwatch stopwatch = new Stopwatch();
   private int field09841 = -1;
   private final EventListener<PlayerTickEvent> field09842 = playerTickEvent -> {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.modeSetting.isSelected("Обычный")) {
            Vec3d vec3d = Vec3d.ZERO;
            if (mc.options.forwardKey.isPressed()) {
               vec3d = vec3d.add(Vec3d.fromPolar(0.0F, mc.player.getYaw()));
            }

            if (mc.options.backKey.isPressed()) {
               vec3d = vec3d.subtract(Vec3d.fromPolar(0.0F, mc.player.getYaw()));
            }

            if (mc.options.rightKey.isPressed()) {
               vec3d = vec3d.add(Vec3d.fromPolar(0.0F, mc.player.getYaw() + 90.0F));
            }

            if (mc.options.leftKey.isPressed()) {
               vec3d = vec3d.subtract(Vec3d.fromPolar(0.0F, mc.player.getYaw() + 90.0F));
            }

            if (mc.options.jumpKey.isPressed()) {
               vec3d = vec3d.add(0.0, 1.0, 0.0);
            }

            if (mc.options.sneakKey.isPressed()) {
               vec3d = vec3d.subtract(0.0, 1.0, 0.0);
            }

            mc.player.setVelocity(vec3d.lengthSquared() > 0.0 ? vec3d.normalize().multiply(this.numberSetting.floatValue()) : Vec3d.ZERO);
         }
      }
   };
   private final EventListener<PacketEvent> field09843 = packetEvent -> {
      if (mc.player != null) {
         if (packetEvent.getPacket() instanceof OpenScreenS2CPacket openScreenS2CPacket
            && this.modeSetting.isSelected("FunTime")
            && openScreenS2CPacket.getName().getString().contains("Блок дамагер")) {
            mc.execute(
               () -> {
                  if (mc.player != null) {
                     mc.player
                        .networkHandler
                        .sendPacket(
                           new ClickSlotC2SPacket(
                              openScreenS2CPacket.getSyncId(), 0, (short)14, (byte)0, SlotActionType.QUICK_MOVE, Int2ObjectMaps.emptyMap(), ItemStackHash.EMPTY
                           )
                        );
                     mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(openScreenS2CPacket.getSyncId()));
                     this.stopwatch.delayStart(-5000L);
                  }
               }
            );
            packetEvent.method4423();
         }
      }
   };
   private final EventListener<WorldRenderEvent> field09844 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && !this.field09839.isEmpty()) {
         this.field09839
            .entrySet()
            .removeIf(local4536_0 -> System.currentTimeMillis() - local4536_0.getValue() > 200L || mc.world.getBlockState(local4536_0.getKey()).isAir());

         for (Entry<BlockPos, Long> local4543_3 : this.field09839.entrySet()) {
            float local4543_4 = 1.0F - MathHelper.clamp((float)(System.currentTimeMillis() - local4543_3.getValue()) / 200.0F, 0.0F, 1.0F);
            Box box = new Box(local4543_3.getKey());
            worldRenderEvent.method0647().push();
            worldRenderEvent.method0647()
               .translate(
                  box.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                  box.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                  box.minZ - mc.gameRenderer.getCamera().getCameraPos().z
               );
            Matrix4f positionMatrix = worldRenderEvent.method0647().peek().getPositionMatrix();
            VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
            float[] floatArray = new float[]{0.0F, 1.0F};
            float[] floatArray2 = new float[]{0.0F, 1.0F};
            float[] floatArray3 = new float[]{0.0F, 1.0F};
            int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

            for (int[] intArray : intArrayArray) {
               for (int local4543_19 : intArray) {
                  vertexConsumer.vertex(positionMatrix, floatArray[local4543_19 >> 2], floatArray2[local4543_19 >> 1 & 1], floatArray3[local4543_19 & 1])
                     .color(Colors.method3727(ThemeColors.field13669, 0.16F * local4543_4));
               }
            }

            VertexRendering.drawOutline(
               worldRenderEvent.method0647(),
               mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
               VoxelShapes.cuboid(new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)),
               0.0,
               0.0,
               0.0,
               Colors.method3727(ThemeColors.field13669, local4543_4),
               1.5F
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.method0647().pop();
         }
      }
   };

   private FlightModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим", "Чем держаться в воздухе.", "Обычный", "Обычный"));
      this.numberSetting = this.addSetting(
         new NumberSetting("Скорость", "Блоков за тик полёта.")
            .withRange(0.1F, 3.0F)
            .withStep(0.1F)
            .setValue(1.0F)
            .withVisibility(() -> this.modeSetting.isSelected("Обычный"))
      );
   }

   @Override
   protected void onDisable() {
      this.field09839.clear();
      if (this.field09841 != -1) {
         InventoryUtils.method0553(this.field09841, true);
         this.field09841 = -1;
      }
   }

   public boolean method2893(BlockState blockState) {
      return !blockState.isReplaceable()
         && !blockState.onUse(mc.world, mc.player, new BlockHitResult(BlockPos.ORIGIN.toCenterPos(), Direction.SOUTH, BlockPos.ORIGIN, false))
            .isAccepted();
   }

   public BlockHitResult method2894(BlockPos blockPos, Direction direction) {
      if (mc.player != null && mc.world != null) {
         BlockPos blockPos2 = blockPos.offset(direction).toImmutable();
         Vec3d eyePos = mc.player.getEyePos();
         Box box = new Box(blockPos2).contract(0.02);
         Direction opposite = direction.getOpposite();
         Vec3d vec3d = new Vec3d(
            opposite.getOffsetX() == 0
               ? MathHelper.clamp(eyePos.x, box.minX, box.maxX)
               : (opposite.getOffsetX() > 0 ? box.maxX : box.minX),
            opposite.getOffsetY() == 0
               ? MathHelper.clamp(eyePos.y, box.minY, box.maxY)
               : (opposite.getOffsetY() > 0 ? box.maxY : box.minY),
            opposite.getOffsetZ() == 0
               ? MathHelper.clamp(eyePos.z, box.minZ, box.maxZ)
               : (opposite.getOffsetZ() > 0 ? box.maxZ : box.minZ)
         );
         BlockHitResult blockHitResult = Box.raycast(List.of(Box.from(Vec3d.ZERO)), eyePos, vec3d, blockPos2);
         return blockHitResult != null && direction.equals(blockHitResult.getSide().getOpposite()) && eyePos.distanceTo(vec3d) < 4.45
            ? new BlockHitResult(
               blockHitResult.getPos().add(Vec3d.of(direction.getVector()).multiply(1.0E-12)),
               blockHitResult.getSide(),
               blockHitResult.getBlockPos(),
               blockHitResult.isInsideBlock()
            )
            : null;
      } else {
         return null;
      }
   }

   private void method2895(BlockPos blockPos, BlockState blockState) {
      if (this.field09841 != -1) {
         InventoryUtils.method0553(this.field09841, true);
         this.field09841 = -1;
      } else {
         Box box = new Box(blockPos).contract(0.01);
         if (blockState.getOutlineShape(mc.world, BlockPos.ORIGIN).isEmpty() && this.method2896(box)) {
            if (!InventoryActionQueue.method2656().method2665() && !mc.player.getBoundingBox().contains(blockPos)) {
               if (BlockPos.ofFloored(SimulatedPlayerInput.method1491(2).field04606).getY() <= blockPos.getY() && this.stopwatch.hasExceededMillis(2000L)) {
                  int local4541_4 = InventoryUtils.method0539(Items.JIGSAW);
                  if (local4541_4 != -1) {
                     this.stopwatch.reset();
                     this.field09841 = InventoryUtils.method0554();
                     RotationManager.method0658()
                        .method0660(
                           new RotationAngles(mc.player.getYaw(), 90.0F),
                           new RotationSpeed(180.0F, 180.0F, 20.0F, 20.0F),
                           RotationManager.ReturnMode.Smooth
                        );
                     InventoryUtils.method0553(local4541_4, true);
                     ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
                        .invokeStartPrediction(
                           mc.world,
                           local4534_1 -> new PlayerInteractBlockC2SPacket(
                              Hand.MAIN_HAND, new BlockHitResult(blockPos.toCenterPos().add(0.0, 0.5, 0.0), Direction.UP, blockPos, false), local4534_1
                           )
                        );
                     this.field09839.put(blockPos, System.currentTimeMillis());
                     mc.world.setBlockState(blockPos, Blocks.JIGSAW.getDefaultState());
                     ((ClientPlayerInteractionManagerMixin2)mc.interactionManager)
                        .invokeStartPrediction(
                           mc.world,
                           local4547_2 -> new PlayerInteractBlockC2SPacket(
                              Hand.MAIN_HAND, new BlockHitResult(box.getCenter(), Direction.UP, blockPos, false), local4547_2
                           )
                        );
                  }
               }
            }
         }
      }
   }

   private boolean method2896(Box box) {
      if (mc.player.getBoundingBox().intersects(box)) {
         return false;
      }

      for (Entity entity : mc.world.getEntities()) {
         if (entity != mc.player && entity.getBoundingBox().intersects(box)) {
            return false;
         }
      }

      return true;
   }

   @Override
   protected void onEnable() {
      this.field09839.clear();
   }

   public static FlightModule method2902() {
      if (flightModule == null) {
         flightModule = new FlightModule();
      }

      return flightModule;
   }
}
