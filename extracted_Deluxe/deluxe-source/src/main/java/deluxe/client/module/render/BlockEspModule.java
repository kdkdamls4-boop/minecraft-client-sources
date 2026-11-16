package deluxe.client.module.render;

import deluxe.client.util.Colors;
import deluxe.client.DeluxeClient;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.event.WorldRenderEvent;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.DispenserBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ModuleInfo(name = "BlockEsp", description = "Подсвечивает сундуки, шалкеры, бочки и прочие хранилища сквозь стены.", category = ModuleCategory.RENDER)
public final class BlockEspModule extends Module {
   private static BlockEspModule blockEspModule;
   private static final RenderPipeline field10309 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/storage_esp"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final Vector4f field10310 = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f field10311 = new Vector3f();
   private static final Matrix4f field10312 = new Matrix4f();
   private static final int[][] field10313 = new int[][]{{0, 1, 5, 4}, {2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
   private static final float[] field10314 = new float[]{0.45F, 1.0F, 0.7F, 0.7F, 0.55F, 0.55F};
   private static final int[] field10315 = new int[]{-1526454, -5219088, -1020704, -3569072, -29632, -5195576, -9651984};
   private static final String[] field10316 = new String[]{"Сундуки", "Эндер-сундуки", "Шалкеры", "Бочки", "Печки", "Воронки", "Раздатчики"};
   public MultiSelectSetting multiSelectSetting;
   public LegacyNumberSetting field10318;
   public LegacyNumberSetting field10319;
   private final ArrayList<float[]> field10320 = new ArrayList<>();
   private final LinkedHashSet<Block> field10321 = new LinkedHashSet<>();
   private boolean field10322;
   private long field10323;
   private GpuBuffer field10324;
   private BufferAllocator field10325;
   private final EventListener<PlayerTickEvent> field10326 = playerTickEvent -> {
      if (mc.player != null && mc.world != null && System.currentTimeMillis() - this.field10323 >= 500L) {
         this.method3038();
         this.field10323 = System.currentTimeMillis();
         this.field10320.clear();
         int local4874_2 = (int)Math.ceil(this.field10318.getValue() / 16.0F) + 1;
         int local4874_3 = mc.player.getChunkPos().x;
         int local4874_4 = mc.player.getChunkPos().z;
         float local4874_5 = this.field10318.getValue() * this.field10318.getValue();

         for (int index = local4874_3 - local4874_2; index <= local4874_3 + local4874_2; index++) {
            for (int index2 = local4874_4 - local4874_2; index2 <= local4874_4 + local4874_2; index2++) {
               WorldChunk chunk = mc.world.getChunk(index, index2);

               for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                  int local4874_6 = blockEntity instanceof ChestBlockEntity
                     ? 0
                     : (
                        blockEntity instanceof EnderChestBlockEntity
                           ? 1
                           : (
                              blockEntity instanceof ShulkerBoxBlockEntity
                                 ? 2
                                 : (
                                    blockEntity instanceof BarrelBlockEntity
                                       ? 3
                                       : (
                                          blockEntity instanceof AbstractFurnaceBlockEntity
                                             ? 4
                                             : (blockEntity instanceof HopperBlockEntity ? 5 : (blockEntity instanceof DispenserBlockEntity ? 6 : -1))
                                       )
                                 )
                           )
                     );
                  if (local4874_6 != -1 && this.multiSelectSetting.matchesOption(field10316[local4874_6])) {
                     BlockPos pos = blockEntity.getPos();
                     if (!(mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > local4874_5)
                        && this.field10320.size() < 256) {
                        this.field10320
                           .add(new float[]{pos.getX(), pos.getY(), pos.getZ(), Float.intBitsToFloat(field10315[local4874_6])});
                     }
                  }
               }

               if (!this.field10321.isEmpty()) {
                  this.method3037(chunk, local4874_5);
               }
            }
         }
      }
   };
   private final EventListener<WorldRenderEvent> field10327 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && !this.field10320.isEmpty()) {
         if (mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
            float local4861_2 = (float)mc.gameRenderer.getCamera().getCameraPos().x;
            float local4861_3 = (float)mc.gameRenderer.getCamera().getCameraPos().y;
            float local4861_4 = (float)mc.gameRenderer.getCamera().getCameraPos().z;
            int local4861_5 = 0;
            float local4861_7 = 0.04F;
            if (this.field10325 == null) {
               this.field10325 = new BufferAllocator(12288 * VertexFormats.POSITION_COLOR.getVertexSize());
            }

            CloseableBuffer allocated = this.field10325.getAllocated();
            if (allocated != null) {
               allocated.close();
            }

            BufferBuilder bufferBuilder = new BufferBuilder(this.field10325, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (float[] floatArray : this.field10320) {
               float local4861_12 = floatArray[0] - local4861_2 + local4861_7;
               float local4861_13 = floatArray[1] - local4861_3 + local4861_7;
               float local4861_14 = floatArray[2] - local4861_4 + local4861_7;
               float local4861_15 = local4861_12 + 1.0F - local4861_7 * 2.0F;
               float local4861_16 = local4861_13 + 1.0F - local4861_7 * 2.0F;
               float local4861_17 = local4861_14 + 1.0F - local4861_7 * 2.0F;
               int local4861_18 = Float.floatToRawIntBits(floatArray[3]);

               for (int index3 = 0; index3 < 6; index3++) {
                  int local4861_6 = Colors.method3727(
                     Colors.method3723(-16777216, local4861_18, field10314[index3]), this.field10319.getValue()
                  );

                  for (int local4861_23 : field10313[index3]) {
                     bufferBuilder.vertex(
                           (local4861_23 & 4) == 0 ? local4861_12 : local4861_15,
                           (local4861_23 & 2) == 0 ? local4861_13 : local4861_16,
                           (local4861_23 & 1) == 0 ? local4861_14 : local4861_17
                        )
                        .color(local4861_6);
                  }

                  local4861_5++;
               }
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               if (this.field10324 != null && !this.field10324.isClosed() && this.field10324.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.field10324.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.field10324 != null && !this.field10324.isClosed()) {
                     this.field10324.close();
                  }

                  this.field10324 = RenderSystem.getDevice().createBuffer(() -> "deluxe_storage_esp_vertices", 40, builtBuffer.getBuffer());
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                  .write(worldRenderEvent.method0647().peek().getPositionMatrix(), field10310, field10311, field10312);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_storage_esp", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(field10309);
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, this.field10324);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local4861_5 * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local4861_5 * 6, 1);
               }
            }
         }
      }
   };

   public boolean method3035(Block block) {
      this.method3038();
      boolean local4860_2 = this.field10321.remove(block);
      if (local4860_2) {
         this.method3045();
      }

      return local4860_2;
   }

   @Override
   protected void onDisable() {
      if (this.field10325 != null) {
         this.field10325.close();
         this.field10325 = null;
      }

      if (this.field10324 != null && !this.field10324.isClosed()) {
         this.field10324.close();
      }

      this.field10324 = null;
   }

   private void method3037(WorldChunk worldChunk, float local4863_2) {
      ChunkSection[] sectionArray = worldChunk.getSectionArray();
      int local4863_4 = MathHelper.floor((mc.player.getY() - this.field10318.getValue()) / 16.0);
      int local4863_5 = MathHelper.floor((mc.player.getY() + this.field10318.getValue()) / 16.0);

      for (int index = 0; index < sectionArray.length && this.field10320.size() < 512; index++) {
         int local4863_7 = mc.world.sectionIndexToCoord(index);
         ChunkSection chunkSection = sectionArray[index];
         if (local4863_7 >= local4863_4
            && local4863_7 <= local4863_5
            && !chunkSection.isEmpty()
            && chunkSection.hasAny(blockState -> this.field10321.contains(blockState.getBlock()))) {
            int startX = worldChunk.getPos().getStartX();
            int local4863_10 = local4863_7 << 4;
            int startZ = worldChunk.getPos().getStartZ();

            for (int index2 = 0; index2 < 16; index2++) {
               for (int index3 = 0; index3 < 16; index3++) {
                  for (int index4 = 0; index4 < 16; index4++) {
                     Block block = chunkSection.getBlockState(index4, index2, index3).getBlock();
                     if (this.field10321.contains(block)
                        && !(
                           mc.player
                                 .squaredDistanceTo(startX + index4 + 0.5, local4863_10 + index2 + 0.5, startZ + index3 + 0.5)
                              > local4863_2
                        )) {
                        this.field10320
                           .add(
                              new float[]{
                                 startX + index4,
                                 local4863_10 + index2,
                                 startZ + index3,
                                 Float.intBitsToFloat(block.getDefaultMapColor().color | 0xFF000000)
                              }
                           );
                        if (this.field10320.size() >= 512) {
                           return;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void method3038() {
      if (!this.field10322) {
         this.field10322 = true;

         try {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("blockesp.json");
            if (!Files.exists(path)) {
               return;
            }

            for (JsonElement jsonElement : JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonArray()) {
               Identifier identifier = Identifier.tryParse(jsonElement.getAsString());
               if (identifier != null) {
                  Registries.BLOCK.getOptionalValue(identifier).ifPresent(this.field10321::add);
               }
            }
         } catch (Exception exception) {
         }
      }
   }

   public void method3040() {
      this.method3038();
      this.field10321.clear();
      this.method3045();
   }

   public boolean method3043(Block block) {
      this.method3038();
      boolean local4869_2 = this.field10321.add(block);
      if (local4869_2) {
         this.method3045();
      }

      return local4869_2;
   }

   public static BlockEspModule method3044() {
      if (blockEspModule == null) {
         blockEspModule = new BlockEspModule();
      }

      return blockEspModule;
   }

   private void method3045() {
      try {
         Path path = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("blockesp.json");
         Files.createDirectories(path.getParent());
         JsonArray jsonArray = new JsonArray();

         for (Block block : this.field10321) {
            jsonArray.add(Registries.BLOCK.getId(block).toString());
         }

         Files.writeString(path, jsonArray.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   public ArrayList<Block> method3046() {
      this.method3038();
      return new ArrayList<>(this.field10321);
   }

   private BlockEspModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Хранилища",
            "Какие блоки подсвечивать.",
            new String[]{"Сундуки", "Эндер-сундуки", "Шалкеры", "Бочки", "Печки", "Воронки", "Раздатчики"},
            "Сундуки",
            "Эндер-сундуки",
            "Шалкеры",
            "Бочки"
         )
      );
      this.field10318 = this.addSetting(new LegacyNumberSetting("Дистанция", "Радиус подсветки в блоках.", 48.0, 16.0, 128.0, 4.0));
      this.field10319 = this.addSetting(new LegacyNumberSetting("Плотность", "Прозрачность заливки.", 0.3, 0.05, 0.8, 0.05));
   }
}
