package deluxe.client.module.misc;

import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.module.ModuleManager;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.GuiScale;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.FontText;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.render.Render2D;
import deluxe.client.util.Stopwatch;
import deluxe.client.event.WorldRenderEvent;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShapes;
import org.joml.Matrix4f;

@ModuleInfo(
   name = "WardenHelper",
   description = "Подсвечивает сундуки вардена и показывает их таймеры: отсчёт, «Активен», «Не активированный».",
   category = ModuleCategory.MISC
)
public final class WardenHelperModule extends Module {
   private static WardenHelperModule wardenHelperModule;
   private static final Pattern field04079 = Pattern.compile("(?:(\\d{1,2}):(\\d{2})|(\\d+)\\s*(?:сек|сек\\.|с|s)|(\\d+)\\s*(?:мин|мин\\.|m))");
   private final ConcurrentHashMap<BlockPos, Long> field04080 = new ConcurrentHashMap<>();
   private final Stopwatch stopwatch = new Stopwatch();
   private final EventListener<PlayerTickEvent> field04082 = playerTickEvent -> this.method1352();
   private final EventListener<HudRenderEvent> field04083 = event -> this.onHudRender(event);
   private final EventListener<WorldRenderEvent> field04084 = event2 -> this.onWorldRender(event2);

   private int method1351(long local1918_1) {
      return local1918_1 > System.currentTimeMillis() ? -43691 : (local1918_1 > 0L ? -11141291 : -171);
   }

   private void method1352() {
      if (mc.player != null && mc.world != null && this.stopwatch.hasExceededMillis(250L)) {
         this.stopwatch.reset();

         for (int index = mc.player.getChunkPos().x - 6; index <= mc.player.getChunkPos().x + 6; index++) {
            for (int index2 = mc.player.getChunkPos().z - 6; index2 <= mc.player.getChunkPos().z + 6; index2++) {
               if (mc.world.isChunkLoaded(index, index2)) {
                  for (BlockEntity blockEntity : mc.world.getChunk(index, index2).getBlockEntities().values()) {
                     if (blockEntity instanceof ChestBlockEntity && !(mc.player.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > 6400.0)) {
                        if (this.method1358(blockEntity.getPos()) >= 0) {
                           this.field04080.put(blockEntity.getPos().toImmutable(), System.currentTimeMillis() + this.method1358(blockEntity.getPos()) * 1000L);
                        } else {
                           this.field04080.putIfAbsent(blockEntity.getPos().toImmutable(), 0L);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         for (Entry<BlockPos, Long> local1921_3 : this.field04080.entrySet()) {
            if (!(mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local1921_3.getKey())) > 80.0)
               && mc.world.getBlockEntity(local1921_3.getKey()) instanceof ChestBlockEntity) {
               Box box = new Box(local1921_3.getKey()).contract(0.0625, 0.0625, 0.0625);
               event.method0647().push();
               event.method0647()
                  .translate(
                     box.minX - mc.gameRenderer.getCamera().getCameraPos().x,
                     box.minY - mc.gameRenderer.getCamera().getCameraPos().y,
                     box.minZ - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               Matrix4f positionMatrix = event.method0647().peek().getPositionMatrix();
               VertexConsumer vertexConsumer = mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.debugQuads());
               float[] floatArray = new float[]{0.0F, (float)(box.maxX - box.minX)};
               float[] floatArray2 = new float[]{0.0F, (float)(box.maxY - box.minY)};
               float[] floatArray3 = new float[]{0.0F, (float)(box.maxZ - box.minZ)};
               int[][] intArrayArray = new int[][]{{0, 1, 3, 2}, {4, 6, 7, 5}, {0, 4, 5, 1}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 5, 7, 3}};

               for (int[] intArray : intArrayArray) {
                  for (int local1921_18 : intArray) {
                     vertexConsumer.vertex(positionMatrix, floatArray[local1921_18 >> 2], floatArray2[local1921_18 >> 1 & 1], floatArray3[local1921_18 & 1])
                        .color(Colors.method3727(this.method1351(local1921_3.getValue()), 0.18F));
                  }
               }

               VertexRendering.drawOutline(
                  event.method0647(),
                  mc.getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayers.lines()),
                  VoxelShapes.cuboid(
                     new Box(0.0, 0.0, 0.0, box.maxX - box.minX, box.maxY - box.minY, box.maxZ - box.minZ)
                  ),
                  0.0,
                  0.0,
                  0.0,
                  Colors.method3727(this.method1351(local1921_3.getValue()), 0.55F),
                  1.0F
               );
               mc.getBufferBuilders().getEntityVertexConsumers().draw();
               event.method0647().pop();
            }
         }
      }
   }

   public static WardenHelperModule method1355() {
      if (wardenHelperModule == null) {
         wardenHelperModule = new WardenHelperModule();
      }

      return wardenHelperModule;
   }

   private int method1358(BlockPos blockPos) {
      for (Entity entity : mc.world
         .getOtherEntities(
            mc.player,
            new Box(blockPos).expand(3.0, 4.0, 3.0),
            entity2 -> entity2 instanceof ArmorStandEntity armorStandEntity && armorStandEntity.hasCustomName() || entity2 instanceof TextDisplayEntity
         )) {
         Matcher matcher = field04079.matcher(
            (entity instanceof TextDisplayEntity textDisplayEntity ? textDisplayEntity.getText() : entity.getName())
               .getString()
               .replaceAll("(?i)§[0-9a-fk-or]", "")
               .trim()
         );
         if (matcher.find()) {
            if (matcher.group(1) != null) {
               return Integer.parseInt(matcher.group(1)) * 60 + Integer.parseInt(matcher.group(2));
            }

            if (matcher.group(3) != null) {
               return Integer.parseInt(matcher.group(3));
            }

            return Integer.parseInt(matcher.group(4)) * 60;
         }
      }

      return -1;
   }

   private void onHudRender(HudRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         for (Entry<BlockPos, Long> local1927_3 : this.field04080.entrySet()) {
            if (!(mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local1927_3.getKey())) > 80.0)
               && mc.world.getBlockEntity(local1927_3.getKey()) instanceof ChestBlockEntity) {
               Vec3d vec3d = WorldProjection.method1125(
                  Vec3d.ofCenter(local1927_3.getKey()).add(0.0, 0.7, 0.0), event.method4462(), event.method4460()
               );
               if (vec3d != null) {
                  HudRenderContext.method4336(
                     (float)vec3d.x * GuiScale.method0797() / GuiScale.method0794(),
                     (float)vec3d.y * GuiScale.method0797() / GuiScale.method0794(),
                     (float)Math.max(0.55F, 1.0 - mc.player.getEntityPos().distanceTo(Vec3d.ofCenter(local1927_3.getKey())) * 0.008F)
                  );
                  Render2D.method4715(
                     (float)vec3d.x * GuiScale.method0797() / GuiScale.method0794()
                        - (FontText.method1396(FontFace.SEMI_BOLD, this.method1360(local1927_3.getValue()), 4.0F) + 4.0F) / 2.0F,
                     (float)vec3d.y * GuiScale.method0797() / GuiScale.method0794() - 4.5F,
                     FontText.method1396(FontFace.SEMI_BOLD, this.method1360(local1927_3.getValue()), 4.0F) + 4.0F,
                     9.0F,
                     0.0F,
                     Colors.method3728(ThemeColors.field13661, 133)
                  );
                  FontText.method1397(
                     FontFace.SEMI_BOLD,
                     this.method1360(local1927_3.getValue()),
                     (float)vec3d.x * GuiScale.method0797() / GuiScale.method0794()
                        - FontText.method1396(FontFace.SEMI_BOLD, this.method1360(local1927_3.getValue()), 4.0F) / 2.0F,
                     (float)vec3d.y * GuiScale.method0797() / GuiScale.method0794() - 2.45F,
                     4.0F,
                     this.method1351(local1927_3.getValue())
                  );
                  HudRenderContext.method4337();
               }
            }
         }
      }
   }

   private String method1360(long local1928_1) {
      if (local1928_1 > System.currentTimeMillis()) {
         return (local1928_1 - System.currentTimeMillis()) / 60000L + ":" + String.format("%02d", (local1928_1 - System.currentTimeMillis()) / 1000L % 60L);
      } else {
         return local1928_1 > 0L ? "Активен" : "Активирован";
      }
   }

   private WardenHelperModule() {
      ModuleManager.unregisterModule(this);
   }
}
