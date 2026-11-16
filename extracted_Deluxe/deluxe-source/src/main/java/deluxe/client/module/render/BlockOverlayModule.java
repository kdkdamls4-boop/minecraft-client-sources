package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.util.DepthSnapshot;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.util.RaycastUtils;
import deluxe.client.setting.ColorSetting;
import deluxe.client.render.ThemeColors;
import deluxe.client.RotationAngles;
import deluxe.client.setting.SectionSetting;
import deluxe.client.event.WorldRenderEvent;

import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

@ModuleInfo(name = "BlockOverlay", description = "Выделяет грань блока под прицелом, видно сквозь блоки.", category = ModuleCategory.RENDER)
public final class BlockOverlayModule extends Module {
   private static BlockOverlayModule blockOverlayModule;
   public BooleanSetting field12633;
   public LegacyNumberSetting field12634;
   public LegacyNumberSetting field12635;
   public BooleanSetting field12636;
   public LegacyNumberSetting field12637;
   public BooleanSetting field12638;
   public ColorSetting colorSetting;
   private Box field12640;
   private float field12641;
   private long field12642;
   private final EventListener<WorldRenderEvent> field12643 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         float local5973_2 = Math.max(1.0F, Math.min((float)(System.currentTimeMillis() - this.field12642), 100.0F)) / 16.666666F;
         this.field12642 = System.currentTimeMillis();
         BlockHitResult blockHitResult = mc.world
            .raycast(
               new RaycastContext(
                  mc.player.getCameraPosVec(worldRenderEvent.method0646()),
                  mc.player
                     .getCameraPosVec(worldRenderEvent.method0646())
                     .add(
                        new RotationAngles(RotationAngles.method4366(), RotationAngles.method4364())
                           .method4363()
                           .multiply(mc.player.getBlockInteractionRange())
                     ),
                  ShapeType.OUTLINE,
                  FluidHandling.NONE,
                  mc.player
               )
            );
         Box box = blockHitResult.getType() == Type.BLOCK && !mc.world.getBlockState(blockHitResult.getBlockPos()).isAir()
            ? (
               mc.world.getBlockState(blockHitResult.getBlockPos()).getOutlineShape(mc.world, blockHitResult.getBlockPos()).isEmpty()
                  ? new Box(blockHitResult.getBlockPos()).expand(0.002)
                  : mc.world
                     .getBlockState(blockHitResult.getBlockPos())
                     .getOutlineShape(mc.world, blockHitResult.getBlockPos())
                     .getBoundingBox()
                     .offset(blockHitResult.getBlockPos())
                     .expand(0.002)
            )
            : null;
         if (box != null && this.method3637(mc.player.getCameraPosVec(worldRenderEvent.method0646()), blockHitResult.getPos())) {
            box = null;
         }

         if (box != null) {
            box = this.method3636(box, blockHitResult.getSide());
         }

         if (box != null) {
            this.field12640 = this.field12640 != null && !(this.field12641 < 0.003921569F) && this.field12636.isChecked()
               ? new Box(
                  MathHelper.lerp(Math.min(1.0F, this.field12637.getValue() * local5973_2), this.field12640.minX, box.minX),
                  MathHelper.lerp(Math.min(1.0F, this.field12637.getValue() * local5973_2), this.field12640.minY, box.minY),
                  MathHelper.lerp(Math.min(1.0F, this.field12637.getValue() * local5973_2), this.field12640.minZ, box.minZ),
                  MathHelper.lerp(Math.min(1.0F, this.field12637.getValue() * local5973_2), this.field12640.maxX, box.maxX),
                  MathHelper.lerp(Math.min(1.0F, this.field12637.getValue() * local5973_2), this.field12640.maxY, box.maxY),
                  MathHelper.lerp(Math.min(1.0F, this.field12637.getValue() * local5973_2), this.field12640.maxZ, box.maxZ)
               )
               : box;
         }

         this.field12641 = MathHelper.clamp(this.field12641 + (box == null ? -0.22F : 0.22F) * local5973_2, 0.0F, 1.0F);
         if (this.field12640 != null && !(this.field12641 < 0.003921569F)) {
            DepthSnapshot.method4230();
            worldRenderEvent.method0647().push();
            worldRenderEvent.method0647()
               .translate(
                  this.field12640.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                  this.field12640.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                  this.field12640.minZ - mc.gameRenderer.getCamera().getCameraPos().z
               );
            Matrix4f positionMatrix = worldRenderEvent.method0647().peek().getPositionMatrix();
            if (this.field12633.isChecked() && this.field12634.getValue() > 0.001F) {
               VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
               float[] floatArray = new float[]{0.0F, (float)(this.field12640.maxX - this.field12640.minX)};
               float[] floatArray2 = new float[]{0.0F, (float)(this.field12640.maxY - this.field12640.minY)};
               float[] floatArray3 = new float[]{0.0F, (float)(this.field12640.maxZ - this.field12640.minZ)};
               int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};
               int local5973_11 = floatArray[1] < 0.01F ? 0 : (floatArray2[1] < 0.01F ? 1 : (floatArray3[1] < 0.01F ? 2 : -1));

               for (int index = 0; index < intArrayArray.length; index++) {
                  if (local5973_11 < 0 || index == local5973_11 * 2) {
                     for (int local5973_16 : intArrayArray[index]) {
                        vertexConsumer.vertex(positionMatrix, floatArray[local5973_16 >> 2], floatArray2[local5973_16 >> 1 & 1], floatArray3[local5973_16 & 1])
                           .color(
                              Colors.method3727(
                                 this.field12638.isChecked() ? this.colorSetting.getValue() : ThemeColors.field13669,
                                 this.field12634.getValue() * this.field12641
                              )
                           );
                     }
                  }
               }
            }

            VertexRendering.drawOutline(
               worldRenderEvent.method0647(),
               mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
               VoxelShapes.cuboid(
                  new Box(
                     0.0,
                     0.0,
                     0.0,
                     this.field12640.maxX - this.field12640.minX,
                     this.field12640.maxY - this.field12640.minY,
                     this.field12640.maxZ - this.field12640.minZ
                  )
               ),
               0.0,
               0.0,
               0.0,
               Colors.method3727(this.field12638.isChecked() ? this.colorSetting.getValue() : ThemeColors.field13669, this.field12641),
               this.field12635.getValue()
            );
            mc.getBufferBuilders().getEntityVertexConsumers().draw();
            worldRenderEvent.method0647().pop();
         }
      }
   };

   private Box method3636(Box box, Direction direction) {
      return switch (direction) {
         case DOWN -> new Box(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ);
         case UP -> new Box(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ);
         case NORTH -> new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ);
         case SOUTH -> new Box(box.minX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ);
         case WEST -> new Box(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.maxZ);
         case EAST -> new Box(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
      };
   }

   private boolean method3637(Vec3d vec3d, Vec3d vec3d2) {
      return RaycastUtils.method3295(
            mc.player,
            vec3d,
            vec3d2,
            mc.player.getBoundingBox().stretch(vec3d2.subtract(vec3d)).expand(1.0, 1.0, 1.0),
            entity -> !entity.isSpectator() && entity.canHit(),
            vec3d.distanceTo(vec3d2)
         )
         != null;
   }

   private BlockOverlayModule() {
      this.addSetting(new SectionSetting("Бокс"));
      this.field12633 = this.addSetting(new BooleanSetting("Заливка", "Заливать грань цветом.", true));
      this.field12634 = this.addSetting(new LegacyNumberSetting("Прозрачность заливки", "Насколько плотно залиты грани.", 0.25, 0.0, 1.0, 0.05));
      this.field12635 = this.addSetting(new LegacyNumberSetting("Толщина линии", "Толщина линий оутлайна.", 2.0, 0.5, 6.0, 0.1));
      this.addSetting(new SectionSetting("Анимация"));
      this.field12636 = this.addSetting(new BooleanSetting("Анимация", "Бокс плавно переезжает по x, y, z на новый блок вместо мгновенного прыжка.", true));
      this.field12637 = this.addSetting(new LegacyNumberSetting("Скорость", "Как быстро бокс догоняет новый блок.", 0.35, 0.05, 1.0, 0.05));
      this.addSetting(new SectionSetting("Цвет"));
      this.field12638 = this.addSetting(new BooleanSetting("Свой цвет", "Использовать свой цвет вместо ACCENT.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет бокса."));
      this.field12634.withVisibility(() -> this.field12633.isChecked());
      this.field12637.withVisibility(() -> this.field12636.isChecked());
      this.colorSetting.withVisibility(() -> this.field12638.isChecked());
   }

   @Override
   protected void onDisable() {
      this.field12640 = null;
      this.field12641 = 0.0F;
   }

   public static BlockOverlayModule method3643() {
      if (blockOverlayModule == null) {
         blockOverlayModule = new BlockOverlayModule();
      }

      return blockOverlayModule;
   }
}
