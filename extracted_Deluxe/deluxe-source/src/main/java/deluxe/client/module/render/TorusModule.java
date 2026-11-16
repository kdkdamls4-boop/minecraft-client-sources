package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.DeluxeClient;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.render.WorldProjection;
import deluxe.client.event.WorldRenderEvent;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "Torus", description = "Хромированное кольцо ударной волны из повреждённых сущностей.", category = ModuleCategory.RENDER)
public final class TorusModule extends Module {
   private static TorusModule torusModule;
   private static final RenderPipeline field14302 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/torus"))
      .withVertexShader(DeluxeClient.id("core/torus"))
      .withFragmentShader(DeluxeClient.id("core/torus"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("TorusParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field14303 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/torus_through_walls"))
      .withVertexShader(DeluxeClient.id("core/torus"))
      .withFragmentShader(DeluxeClient.id("core/torus"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("TorusParams", UniformType.UNIFORM_BUFFER)
      .build();
   public BooleanSetting booleanSetting;
   public NumberSetting field14305;
   public NumberSetting field14306;
   public NumberSetting field14307;
   private final ConcurrentLinkedQueue<Integer> field14308 = new ConcurrentLinkedQueue<>();
   private final ArrayList<TorusModule.Nested0519> field14310 = new ArrayList<>();
   private SimpleFramebuffer field14311;
   private GpuBuffer field14312;
   private GpuBuffer field14313;
   private BufferAllocator field14314;
   private final Vector3f field14315 = new Vector3f();
   private final Vector3f field14316 = new Vector3f();
   private static final int[][] field14317 = new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, 1}};
   private static final double[] field14318 = new double[11];
   private static final double[] field14319 = new double[11];
   private static final double[] field14320 = new double[31];
   private static final double[] field14321 = new double[31];
   private final EventListener<PacketEvent> field14322 = packetEvent -> {
      if (packetEvent.isIncoming()
         && mc.player != null
         && packetEvent.getPacket() instanceof EntityDamageS2CPacket entityDamageS2CPacket
         && entityDamageS2CPacket.entityId() != mc.player.getId()) {
         if (entityDamageS2CPacket.sourceCauseId() == mc.player.getId() || entityDamageS2CPacket.sourceDirectId() == mc.player.getId()) {
            this.field14308.add(entityDamageS2CPacket.entityId());
         }
      }
   };
   private final EventListener<WorldRenderEvent> field14323 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null && mc.gameRenderer != null) {
         this.field14310.removeIf(local6963_0 -> (float)(System.currentTimeMillis() - local6963_0.spawnAtMs()) > local6963_0.lifetimeMs());

         Integer local6970_2;
         while ((local6970_2 = this.field14308.poll()) != null) {
            Entity entityById = mc.world.getEntityById(local6970_2);
            if (entityById != null) {
               Vec3d vec3d = entityById == mc.player
                  ? mc.player.getEyePos().add(mc.player.getRotationVector().multiply(1.2))
                  : (
                     mc.crosshairTarget instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entityById
                        ? entityHitResult.getPos()
                        : entityById.getEyePos()
                  );
               this.field14310
                  .add(
                     new TorusModule.Nested0519(
                        vec3d,
                        RotationAxis.POSITIVE_Y
                           .rotationDegrees(
                              (float)(-(Math.toDegrees(Math.atan2(vec3d.z - mc.player.getZ(), vec3d.x - mc.player.getX())) - 90.0))
                           )
                           .mul(
                              RotationAxis.POSITIVE_X
                                 .rotationDegrees(
                                    (float)(
                                       -Math.toDegrees(
                                          Math.atan2(
                                             vec3d.y - mc.player.getEyePos().y,
                                             Math.hypot(vec3d.x - mc.player.getX(), vec3d.z - mc.player.getZ())
                                          )
                                       )
                                    )
                                 )
                           ),
                        System.currentTimeMillis(),
                        this.field14306.floatValue(),
                        this.field14307.floatValue()
                     )
                  );
            }
         }

         if (!this.field14310.isEmpty()
            && mc.getFramebuffer() != null
            && mc.getFramebuffer().getColorAttachmentView() != null
            && mc.getFramebuffer().textureWidth > 0
            && mc.getFramebuffer().textureHeight > 0) {
            boolean local6970_2x = false;
            int local6970_3 = 0;

            for (int index = this.field14310.size(); local6970_3 < index; local6970_3++) {
               if (this.method4167(this.field14310.get(local6970_3))) {
                  local6970_2x = true;
                  break;
               }
            }

            if (local6970_2x) {
               if (this.field14311 == null) {
                  this.field14311 = new SimpleFramebuffer(
                     "deluxe_torus_scene", mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight, false
                  );
               }

               if (this.field14311.textureWidth != mc.getFramebuffer().textureWidth
                  || this.field14311.textureHeight != mc.getFramebuffer().textureHeight) {
                  this.field14311.resize(mc.getFramebuffer().textureWidth, mc.getFramebuffer().textureHeight);
               }

               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .copyTextureToTexture(
                     mc.getFramebuffer().getColorAttachment(),
                     this.field14311.getColorAttachment(),
                     0,
                     0,
                     0,
                     0,
                     0,
                     mc.getFramebuffer().textureWidth,
                     mc.getFramebuffer().textureHeight
                  );
               local6970_3 = 0;
               if (this.field14314 == null) {
                  this.field14314 = new BufferAllocator(19200 * VertexFormats.POSITION_COLOR_NORMAL.getVertexSize());
               }

               CloseableBuffer allocated = this.field14314.getAllocated();
               if (allocated != null) {
                  allocated.close();
               }

               BufferBuilder bufferBuilder = new BufferBuilder(this.field14314, DrawMode.QUADS, VertexFormats.POSITION_COLOR_NORMAL);
               Vector3f vector3f = this.field14315;
               Vector3f vector3f2 = this.field14316;
               int[][] intArrayArray = field14317;
               double local6970_9 = mc.gameRenderer.getCamera().getCameraPos().x;
               double local6970_11 = mc.gameRenderer.getCamera().getCameraPos().y;
               double local6970_13 = mc.gameRenderer.getCamera().getCameraPos().z;

               for (TorusModule.Nested0519 local6970_16 : this.field14310) {
                  if (this.method4167(local6970_16)) {
                     float local6970_17 = Math.min(1.0F, (float)(System.currentTimeMillis() - local6970_16.spawnAtMs()) / local6970_16.lifetimeMs());
                     float local6970_18 = 0.6F * local6970_16.sizeMul()
                        + 2.0F * local6970_16.sizeMul() * (1.0F - (1.0F - local6970_17) * (1.0F - local6970_17) * (1.0F - local6970_17));
                     float local6970_19 = 0.4F * local6970_16.sizeMul() * (1.0F - local6970_17 * local6970_17 * local6970_17);
                     int local6970_20 = (int)(255.0F * (local6970_17 < 0.7F ? 1.0F : 1.0F - (local6970_17 - 0.7F) / 0.3F));

                     for (int index2 = 0; index2 < 10; index2++) {
                        for (int index3 = 0; index3 < 30; index3++) {
                           for (int[] intArray : intArrayArray) {
                              double local6970_27 = field14318[index2 + intArray[0]];
                              double local6970_29 = field14319[index2 + intArray[0]];
                              double local6970_31 = field14320[index3 + intArray[1]];
                              double local6970_33 = field14321[index3 + intArray[1]];
                              vector3f2.set((float)(local6970_27 * local6970_31), (float)(local6970_27 * local6970_33), (float)local6970_29);
                              vector3f.set(
                                 (float)((local6970_18 + local6970_19 * local6970_27) * local6970_31),
                                 (float)((local6970_18 + local6970_19 * local6970_27) * local6970_33),
                                 (float)(local6970_19 * local6970_29)
                              );
                              local6970_16.facing().transform(vector3f2);
                              local6970_16.facing().transform(vector3f);
                              bufferBuilder.vertex(
                                    (float)(local6970_16.pos().x - local6970_9 + vector3f.x),
                                    (float)(local6970_16.pos().y - local6970_11 + vector3f.y),
                                    (float)(local6970_16.pos().z - local6970_13 + vector3f.z)
                                 )
                                 .color(255, 255, 255, local6970_20)
                                 .normal(vector3f2.x, vector3f2.y, vector3f2.z);
                           }

                           local6970_3++;
                        }
                     }
                  }
               }

               if (this.field14313 == null || this.field14313.isClosed()) {
                  this.field14313 = RenderSystem.getDevice().createBuffer(() -> "deluxe_torus_params", 136, 160L);
               }

               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(160);
                  worldRenderEvent.method0647().peek().getPositionMatrix().get(0, byteBuffer);
                  WorldProjection.method1120().get(64, byteBuffer);
                  byteBuffer.putFloat(128, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                  byteBuffer.putFloat(132, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                  byteBuffer.putFloat(136, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                  byteBuffer.putFloat(140, this.field14305.floatValue() / 100.0F);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field14313.slice(0L, 160L), byteBuffer);
               } catch (Throwable throwable) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                     }
                  }

                  throw throwable;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }

               try (BuiltBuffer local6970_15x = bufferBuilder.end()) {
                  if (this.field14312 != null && !this.field14312.isClosed() && this.field14312.size() >= local6970_15x.getBuffer().remaining()) {
                     RenderSystem.getDevice()
                        .createCommandEncoder()
                        .writeToBuffer(this.field14312.slice(0L, local6970_15x.getBuffer().remaining()), local6970_15x.getBuffer());
                  } else {
                     if (this.field14312 != null && !this.field14312.isClosed()) {
                        this.field14312.close();
                     }

                     this.field14312 = RenderSystem.getDevice().createBuffer(() -> "deluxe_torus_vertices", 40, local6970_15x.getBuffer());
                  }

                  try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "deluxe_torus",
                           mc.getFramebuffer().getColorAttachmentView(),
                           OptionalInt.empty(),
                           this.booleanSetting.isChecked() ? mc.getFramebuffer().getDepthAttachmentView() : null,
                           OptionalDouble.empty()
                        )) {
                     renderPass.setPipeline(this.booleanSetting.isChecked() ? field14302 : field14303);
                     renderPass.bindTexture("Sampler0", this.field14311.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                     renderPass.setUniform("TorusParams", this.field14313);
                     renderPass.setVertexBuffer(0, this.field14312);
                     renderPass.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local6970_3 * 6),
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     renderPass.drawIndexed(0, 0, local6970_3 * 6, 1);
                  }
               }
            }
         }
      }
   };

   private TorusModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Глубина", "Не позволяет показывать Torus сквозь блоки.", false));
      this.field14305 = this.addSetting(
         new NumberSetting("Частота шума", "Сила эффекта шума на поверхности.").withRange(0.0F, 100.0F).withStep(1.0F).setValue(50.0F)
      );
      this.field14306 = this.addSetting(
         new NumberSetting("Время жизни", "Как долго тор остаётся видимым.").withRange(500.0F, 5000.0F).withStep(100.0F).setValue(1000.0F)
      );
      this.field14307 = this.addSetting(
         new NumberSetting("Размер", "Общий множитель размера тора.").withRange(0.2F, 2.0F).withStep(0.1F).setValue(0.5F)
      );
   }

   static {
      for (int index4 = 0; index4 <= 10; index4++) {
         field14318[index4] = Math.cos(index4 * (Math.PI / 5));
         field14319[index4] = Math.sin(index4 * (Math.PI / 5));
      }

      for (int index5 = 0; index5 <= 30; index5++) {
         field14320[index5] = Math.cos(index5 * (Math.PI / 15));
         field14321[index5] = Math.sin(index5 * (Math.PI / 15));
      }
   }

   private boolean method4167(TorusModule.Nested0519 local6965_1) {
      return WorldProjection.method1124(local6965_1.pos().x, local6965_1.pos().y, local6965_1.pos().z, 3.0F * local6965_1.sizeMul());
   }

   @Override
   protected void onDisable() {
      this.field14308.clear();
      this.field14310.clear();
      if (this.field14311 != null) {
         this.field14311.delete();
         this.field14311 = null;
      }

      if (this.field14312 != null && !this.field14312.isClosed()) {
         this.field14312.close();
      }

      this.field14312 = null;
      if (this.field14313 != null && !this.field14313.isClosed()) {
         this.field14313.close();
      }

      this.field14313 = null;
      if (this.field14314 != null) {
         this.field14314.close();
         this.field14314 = null;
      }
   }

   public static TorusModule method4172() {
      if (torusModule == null) {
         torusModule = new TorusModule();
      }

      return torusModule;
   }

   private record Nested0519(Vec3d pos, Quaternionf facing, long spawnAtMs, float lifetimeMs, float sizeMul) {
   }
}
