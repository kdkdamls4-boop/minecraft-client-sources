package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.DeluxeClient;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.render.WorldProjection;
import deluxe.client.ui.UiTheme;
import deluxe.client.setting.ColorSetting;
import deluxe.client.setting.SectionSetting;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Random;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "CustomWorld", description = "Насыщенность, освещение, время, погода и шейдерное небо.", category = ModuleCategory.RENDER)
public final class CustomWorldModule extends Module {
   private static CustomWorldModule customWorldModule;
   private static final RenderPipeline field07399 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/sky_plasma"))
      .withVertexShader(DeluxeClient.id("core/sky"))
      .withFragmentShader(DeluxeClient.id("core/plasma"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("SkyParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field07400 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/sky_space"))
      .withVertexShader(DeluxeClient.id("core/sky"))
      .withFragmentShader(DeluxeClient.id("core/space"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("SkyParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field07401 = method2224("sky_galaxy");
   private static final RenderPipeline field07402 = method2224("sky_overcast");
   private static final RenderPipeline field07403 = method2224("sky_purple");
   private static final RenderPipeline field07404 = method2224("sky_rays");
   private static final RenderPipeline field07405 = method2224("sky_nord");
   private static final RenderPipeline field07406 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/sky_aurora_layer"))
      .withVertexShader(DeluxeClient.id("core/sky_post"))
      .withFragmentShader(DeluxeClient.id("core/sky_aurora_layer"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field07407 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/sky_aurora"))
      .withVertexShader(DeluxeClient.id("core/sky_post"))
      .withFragmentShader(DeluxeClient.id("core/sky_aurora"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   public LegacyNumberSetting field07408;
   public LegacyNumberSetting field07409;
   public LegacyNumberSetting field07410;
   public BooleanSetting field07411;
   public LegacyNumberSetting field07412;
   public ModeSetting field07413;
   public BooleanSetting field07414;
   public ColorSetting field07415;
   public ModeSetting field07416;
   public BooleanSetting field07417;
   public BooleanSetting field07418;
   public BooleanSetting field07419;
   public LegacyNumberSetting field07420;
   public LegacyNumberSetting field07421;
   public LegacyNumberSetting field07422;
   public LegacyNumberSetting field07423;
   public BooleanSetting field07424;
   public LegacyNumberSetting field07425;
   public BooleanSetting field07426;
   public ColorSetting field07427;
   public LegacyNumberSetting field07428;
   public LegacyNumberSetting field07429;
   public BooleanSetting field07430;
   public BooleanSetting field07431;
   public LegacyNumberSetting field07432;
   public LegacyNumberSetting field07433;
   public LegacyNumberSetting field07434;
   public LegacyNumberSetting field07435;
   public BooleanSetting field07436;
   public LegacyNumberSetting field07437;
   public LegacyNumberSetting field07438;
   public BooleanSetting field07439;
   public BooleanSetting field07440;
   public ColorSetting field07441;
   public BooleanSetting field07442;
   public ModeSetting field07443;
   public LegacyNumberSetting field07444;
   public ColorSetting field07445;
   public LegacyNumberSetting field07446;
   private static final RenderPipeline field07448 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/custom_world_fog"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderLayer field07449 = RenderLayer.of(
      "custom_world_fog",
      RenderSetup.builder(field07448)
         .texture("Sampler0", DeluxeClient.id("textures/glow.png"))
         .expectedBufferSize(320 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())
         .build()
   );
   private final Immediate field07450 = VertexConsumerProvider.immediate(new BufferAllocator(320 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()));
   private final float[] field07451 = new float[400];
   private final Quaternionf field07452 = new Quaternionf();
   private final Vector3f field07453 = new Vector3f();
   private float field07454 = -1.0F;
   private static final RenderPipeline field07455 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/haze"))
      .withVertexShader(DeluxeClient.id("core/sky_post"))
      .withFragmentShader(DeluxeClient.id("core/haze"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler1")
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final RenderPipeline field07456 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/haze_blit"))
      .withVertexShader(DeluxeClient.id("core/sky_post"))
      .withFragmentShader(DeluxeClient.id("core/haze_blit"))
      .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withSampler("Sampler1")
      .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
      .build();
   private final Matrix4f field07457 = new Matrix4f();
   private SimpleFramebuffer field07458;
   private GpuBuffer field07459;
   private GpuBuffer field07460;
   public static final float[] field07461 = new float[]{0.45F, 0.6F, 0.9F};
   private GpuBuffer field07462;
   private GpuBuffer field07463;
   private GpuBuffer field07464;
   private SimpleFramebuffer field07465;

   private boolean method2177() {
      return this.field07416.isSelected("Галактика")
         || this.field07416.isSelected("Пасмурно")
         || this.field07416.isSelected("Фиолетовый")
         || this.field07416.isSelected("Аврора")
         || this.field07416.isSelected("Сияние")
         || this.field07416.isSelected("Норд");
   }

   public void method2183(Matrix4f matrix4f, Matrix4f matrix4f2) {
      if (!this.method2185()) {
         this.method2219();
      } else {
         Framebuffer framebuffer = mc.getFramebuffer();
         if (framebuffer != null
            && framebuffer.getColorAttachmentView() != null
            && framebuffer.getDepthAttachmentView() != null
            && framebuffer.textureWidth > 0
            && framebuffer.textureHeight > 0) {
            if (this.field07462 == null || this.field07462.isClosed()) {
               try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
                  BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
                  bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
                  bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
                  bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

                  try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                     this.field07462 = RenderSystem.getDevice().createBuffer(() -> "deluxe_sky_quad", 32, builtBuffer.getBuffer());
                  }
               }
            }

            if (this.field07465 != null && !this.field07416.isSelected("Аврора")) {
               this.field07465.delete();
               this.field07465 = null;
            }

            if (!this.field07416.isSelected("Плазма") && !this.field07416.isSelected("Космос")) {
               if (this.field07464 == null || this.field07464.isClosed()) {
                  this.field07464 = RenderSystem.getDevice().createBuffer(() -> "deluxe_sky_post_params", 136, 128L);
               }

               MemoryStack memoryStack = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer = memoryStack.calloc(128);
                  new Matrix4f(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer);
                  byteBuffer.putFloat(64, (UiTheme.field09686 >> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(68, (UiTheme.field09686 >> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(72, (UiTheme.field09686 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(76, this.field07416.isSelected("Фиолетовый") ? 1.0F : 0.5F);
                  byteBuffer.putFloat(80, (UiTheme.field09688 >> 16 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(84, (UiTheme.field09688 >> 8 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(88, (UiTheme.field09688 & 0xFF) / 255.0F);
                  byteBuffer.putFloat(92, 0.46F);
                  byteBuffer.putFloat(96, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
                  byteBuffer.putFloat(100, this.field07410.getValue());
                  byteBuffer.putFloat(104, (float)framebuffer.textureWidth / framebuffer.textureHeight);
                  byteBuffer.putFloat(108, framebuffer.textureHeight);
                  byteBuffer.putFloat(112, (float)mc.gameRenderer.getCamera().getCameraPos().x);
                  byteBuffer.putFloat(116, (float)mc.gameRenderer.getCamera().getCameraPos().y);
                  byteBuffer.putFloat(120, (float)mc.gameRenderer.getCamera().getCameraPos().z);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field07464.slice(0L, 128L), byteBuffer);
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

               if (this.field07416.isSelected("Аврора")) {
                  int local3417_4x = Math.max(1, framebuffer.textureWidth / 2);
                  int local3417_5 = Math.max(1, framebuffer.textureHeight / 2);
                  if (this.field07465 == null) {
                     this.field07465 = new SimpleFramebuffer("deluxe_sky_aurora_layer", local3417_4x, local3417_5, false);
                  }

                  if (this.field07465.textureWidth != local3417_4x || this.field07465.textureHeight != local3417_5) {
                     this.field07465.resize(local3417_4x, local3417_5);
                  }

                  try (RenderPass renderPass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "deluxe_sky_aurora_layer", this.field07465.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                        )) {
                     renderPass.setPipeline(field07406);
                     renderPass.setUniform("SkyPostParams", this.field07464);
                     renderPass.setVertexBuffer(0, this.field07462);
                     renderPass.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     renderPass.drawIndexed(0, 0, 6, 1);
                  }

                  try (RenderPass renderPass2 = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "deluxe_sky_aurora",
                           framebuffer.getColorAttachmentView(),
                           OptionalInt.empty(),
                           framebuffer.getDepthAttachmentView(),
                           OptionalDouble.empty()
                        )) {
                     renderPass2.setPipeline(field07407);
                     renderPass2.bindTexture("Sampler0", this.field07465.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                     renderPass2.setUniform("SkyPostParams", this.field07464);
                     renderPass2.setVertexBuffer(0, this.field07462);
                     renderPass2.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     renderPass2.drawIndexed(0, 0, 6, 1);
                  }
               } else {
                  try (RenderPass local3417_4x = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(
                           () -> "deluxe_sky_post",
                           framebuffer.getColorAttachmentView(),
                           OptionalInt.empty(),
                           framebuffer.getDepthAttachmentView(),
                           OptionalDouble.empty()
                        )) {
                     local3417_4x.setPipeline(
                        this.field07416.isSelected("Галактика")
                           ? field07401
                           : (
                              this.field07416.isSelected("Пасмурно")
                                 ? field07402
                                 : (this.field07416.isSelected("Сияние") ? field07404 : (this.field07416.isSelected("Норд") ? field07405 : field07403))
                           )
                     );
                     local3417_4x.setUniform("SkyPostParams", this.field07464);
                     local3417_4x.setVertexBuffer(0, this.field07462);
                     local3417_4x.setIndexBuffer(
                        RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                     );
                     local3417_4x.drawIndexed(0, 0, 6, 1);
                  }
               }
            } else {
               if (this.field07463 == null || this.field07463.isClosed()) {
                  this.field07463 = RenderSystem.getDevice().createBuffer(() -> "deluxe_sky_params", 136, 96L);
               }

               MemoryStack memoryStack2 = MemoryStack.stackPush();

               try {
                  ByteBuffer byteBuffer2 = memoryStack2.calloc(96);
                  new Matrix4f(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer2);
                  byteBuffer2.putFloat(64, (UiTheme.field09686 >> 16 & 0xFF) / 255.0F);
                  byteBuffer2.putFloat(68, (UiTheme.field09686 >> 8 & 0xFF) / 255.0F);
                  byteBuffer2.putFloat(72, (UiTheme.field09686 & 0xFF) / 255.0F);
                  byteBuffer2.putFloat(76, 1.0F);
                  byteBuffer2.putFloat(80, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F);
                  byteBuffer2.putFloat(84, 1.0F);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field07463.slice(0L, 96L), byteBuffer2);
               } catch (Throwable throwable3) {
                  if (memoryStack2 != null) {
                     try {
                        memoryStack2.close();
                     } catch (Throwable throwable4) {
                        throwable3.addSuppressed(throwable4);
                     }
                  }

                  throw throwable3;
               }

               if (memoryStack2 != null) {
                  memoryStack2.close();
               }

               try (RenderPass local3417_4x = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_sky",
                        framebuffer.getColorAttachmentView(),
                        OptionalInt.empty(),
                        framebuffer.getDepthAttachmentView(),
                        OptionalDouble.empty()
                     )) {
                  local3417_4x.setPipeline(this.field07416.isSelected("Космос") ? field07400 : field07399);
                  local3417_4x.setUniform("SkyParams", this.field07463);
                  local3417_4x.setVertexBuffer(0, this.field07462);
                  local3417_4x.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  local3417_4x.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      }
   }

   public boolean method2185() {
      return this.isEnabled() && !this.field07416.isSelected("Выкл");
   }

   private void method2193(float local3427_1) {
      Random random = new Random(42L);
      float local3427_3 = local3427_1 * 0.2F;
      float local3427_4 = local3427_1 * 0.95F;

      for (int index = 0; index < 80; index++) {
         float local3427_6 = local3427_3 + random.nextFloat() * (local3427_4 - local3427_3);
         float local3427_7 = random.nextFloat() * (float) (Math.PI * 2);
         int local3427_8 = index * 5;
         this.field07451[local3427_8] = MathHelper.sin(local3427_7) * local3427_6;
         this.field07451[local3427_8 + 1] = (random.nextFloat() - 0.4F) * local3427_1 * 0.4F;
         this.field07451[local3427_8 + 2] = MathHelper.cos(local3427_7) * local3427_6;
         this.field07451[local3427_8 + 3] = local3427_1 * (0.3F + random.nextFloat() * 0.5F);
         this.field07451[local3427_8 + 4] = (local3427_6 - local3427_3) / (local3427_4 - local3427_3);
      }

      this.field07454 = local3427_1;
   }

   private void method2194(
      VertexConsumer vertexConsumer,
      Matrix4f matrix4f,
      int local3428_3,
      float local3428_4,
      float local3428_5,
      float local3428_6,
      float local3428_7,
      int local3428_8
   ) {
      this.field07452.transform(this.field07453.set(local3428_4, local3428_5, 0.0F));
      vertexConsumer.vertex(
            matrix4f,
            this.field07451[local3428_3] + this.field07453.x,
            this.field07451[local3428_3 + 1] + this.field07453.y,
            this.field07451[local3428_3 + 2] + this.field07453.z
         )
         .texture(local3428_6, local3428_7)
         .color(local3428_8);
   }

   private CustomWorldModule() {
      this.field07408 = this.addSetting(new LegacyNumberSetting("Насыщенность", "Насыщенность цветов мира.", 1.0, 0.0, 2.0, 0.05));
      this.field07409 = this.addSetting(new LegacyNumberSetting("Освещение", "Яркость освещения мира.", 1.0, 0.0, 2.0, 0.05));
      this.addSetting(new SectionSetting("Туман"));
      this.field07442 = this.addSetting(new BooleanSetting("Туман", "Свой туман вокруг игрока.", false));
      this.field07443 = this.addSetting(
         new ModeSetting("Тип тумана", "Облака — объёмные клубы вокруг, Дымка — плотная пелена, съедающая даль.", "Облака", "Облака", "Дымка")
      );
      this.field07443.withVisibility(() -> this.field07442.isChecked());
      this.field07444 = this.addSetting(new LegacyNumberSetting("Дальность тумана", "Дальность отрисовки тумана в блоках.", 80.0, 10.0, 255.0, 5.0));
      this.field07445 = this.addSetting(new ColorSetting("Цвет тумана", "Цвет тумана.", new Color(214, 216, 222)));
      this.field07446 = this.addSetting(new LegacyNumberSetting("Объём тумана", "Плотность тумана.", 0.3, 0.05, 1.0, 0.05));
      this.field07444.withVisibility(() -> this.field07442.isChecked());
      this.field07445.withVisibility(() -> this.field07442.isChecked());
      this.field07446.withVisibility(() -> this.field07442.isChecked());
      this.addSetting(new SectionSetting("Время и погода"));
      this.field07412 = this.addSetting(
         new LegacyNumberSetting(
            "Время суток",
            "Время суток в тиках: 0 рассвет, 6000 полдень, 12000 закат, 18000 полночь. Сдвинь — время зафиксируется само.",
            6000.0,
            0.0,
            24000.0,
            100.0
         )
      );
      this.field07411 = this.addSetting(new BooleanSetting("Фиксировать время", "Зафиксировать время суток слайдером.", false));
      this.field07413 = this.addSetting(new ModeSetting("Погода", "Зафиксировать погоду.", "Выкл", "Выкл", "Ясно", "Дождь", "Гроза", "Снег"));
      this.field07414 = this.addSetting(new BooleanSetting("Свой цвет осадков", "Красить дождь и снег своим цветом.", false));
      this.field07415 = this.addSetting(new ColorSetting("Цвет осадков", "Цвет дождя и снега."));
      this.addSetting(new SectionSetting("Небо и осадки"));
      this.field07416 = this.addSetting(
         new ModeSetting(
            "Шейдерное небо", "Заменить небо шейдером.", "Выкл", "Выкл", "Плазма", "Космос", "Галактика", "Пасмурно", "Фиолетовый", "Аврора", "Сияние", "Норд"
         )
      );
      this.field07410 = this.addSetting(new LegacyNumberSetting("Сила неба", "Насколько плотно шейдер неба закрашивает мир.", 0.65, 0.05, 1.0, 0.05));
      this.field07417 = new BooleanSetting("Шейдерный дождь", "Заменить дождь шейдером.", false);
      this.addSetting(new SectionSetting("Дождь"));
      this.field07430 = this.addSetting(new BooleanSetting("Дождь в мире", "Капли на камере и брызги по земле.", false));
      this.field07431 = this.addSetting(new BooleanSetting("Только в дождь", "Показывать только когда идёт дождь и над головой открытое небо.", true));
      this.field07432 = this.addSetting(new LegacyNumberSetting("Струи дождя", "Сила падающих струй дождя в воздухе.", 0.7, 0.0, 1.0, 0.05));
      this.field07433 = this.addSetting(new LegacyNumberSetting("Плотность дождя", "Сколько струй в воздухе.", 1.0, 0.1, 2.0, 0.05));
      this.field07434 = this.addSetting(new LegacyNumberSetting("Скорость дождя", "Скорость падения и наклон струй.", 1.0, 0.3, 2.0, 0.05));
      this.field07435 = this.addSetting(new LegacyNumberSetting("Длина струй", "Во сколько раз струя вытягивается вдоль падения.", 10.0, 1.0, 24.0, 0.5));
      this.field07436 = this.addSetting(new BooleanSetting("Скрыть ванильный дождь", "Убрать ванильные полосы дождя, оставив только шейдерные.", true));
      this.field07437 = this.addSetting(new LegacyNumberSetting("Капли на экране", "Сила капель на камере.", 0.6, 0.0, 1.0, 0.05));
      this.field07438 = this.addSetting(new LegacyNumberSetting("Брызги на земле", "Сила брызг по горизонтальным поверхностям.", 0.6, 0.0, 1.0, 0.05));
      this.field07439 = this.addSetting(new BooleanSetting("Учитывать крышу", "Не брызгать под крышей и в помещениях.", true));
      this.field07440 = this.addSetting(new BooleanSetting("Свой цвет дождя", "Использовать свой цвет вместо цвета горизонта.", false));
      this.field07441 = this.addSetting(new ColorSetting("Цвет дождя", "Цвет капель и брызг.", new Color(179, 199, 217)));
      this.addSetting(new SectionSetting("Мокрый мир"));
      this.field07418 = this.addSetting(new BooleanSetting("Мокрые поверхности", "Мокрые блики и экранные отражения на поверхностях.", false));
      this.field07419 = this.addSetting(new BooleanSetting("Только под дождём", "Сила эффекта следует за уровнем дождя.", false));
      this.field07420 = this.addSetting(new LegacyNumberSetting("Влажность", "Насколько поверхности выглядят мокрыми.", 0.6, 0.0, 1.0, 0.05));
      this.field07421 = this.addSetting(new LegacyNumberSetting("Отражения", "Сила экранных отражений.", 0.7, 0.0, 1.0, 0.05));
      this.field07422 = this.addSetting(new LegacyNumberSetting("Блик", "Сила солнечного блика на мокрой поверхности.", 0.5, 0.0, 2.0, 0.05));
      this.field07423 = this.addSetting(new LegacyNumberSetting("Рябь", "Рябь по лужам.", 0.35, 0.0, 1.0, 0.05));
      this.field07424 = this.addSetting(new BooleanSetting("Только пол", "Мочить только горизонтальные поверхности.", true));
      this.field07425 = this.addSetting(new LegacyNumberSetting("Отражать небо", "Насколько промахнувшийся луч берёт цвет неба с экрана.", 0.8, 0.0, 1.0, 0.05));
      this.field07426 = this.addSetting(new BooleanSetting("Свой цвет отражений", "Использовать свой цвет вместо цвета горизонта.", false));
      this.field07427 = this.addSetting(new ColorSetting("Цвет отражений", "Цвет отражений там, где луч не попал в геометрию."));
      this.field07428 = this.addSetting(new LegacyNumberSetting("Толщина попадания", "Допуск толщины геометрии при попадании луча.", 0.6, 0.1, 3.0, 0.1));
      this.field07429 = this.addSetting(new LegacyNumberSetting("Дальность отражений", "Максимальная длина луча отражения в блоках.", 24.0, 4.0, 96.0, 1.0));
      this.field07410.withVisibility(() -> this.method2177());
      this.field07412.onChange(() -> this.field07411.setValue(true));
      this.field07415.withVisibility(() -> this.field07414.isChecked());
      this.field07419.withVisibility(() -> this.field07418.isChecked());
      this.field07420.withVisibility(() -> this.field07418.isChecked());
      this.field07421.withVisibility(() -> this.field07418.isChecked());
      this.field07422.withVisibility(() -> this.field07418.isChecked());
      this.field07423.withVisibility(() -> this.field07418.isChecked());
      this.field07424.withVisibility(() -> this.field07418.isChecked());
      this.field07425.withVisibility(() -> this.field07418.isChecked());
      this.field07426.withVisibility(() -> this.field07418.isChecked());
      this.field07427.withVisibility(() -> this.field07418.isChecked() && this.field07426.isChecked());
      this.field07428.withVisibility(() -> this.field07418.isChecked());
      this.field07429.withVisibility(() -> this.field07418.isChecked());
      this.field07431.withVisibility(() -> this.field07430.isChecked());
      this.field07432.withVisibility(() -> this.field07430.isChecked());
      this.field07433.withVisibility(() -> this.field07430.isChecked() && this.field07432.getValue() > 0.0F);
      this.field07434.withVisibility(() -> this.field07430.isChecked() && this.field07432.getValue() > 0.0F);
      this.field07435.withVisibility(() -> this.field07430.isChecked() && this.field07432.getValue() > 0.0F);
      this.field07436.withVisibility(() -> this.field07430.isChecked());
      this.field07437.withVisibility(() -> this.field07430.isChecked());
      this.field07438.withVisibility(() -> this.field07430.isChecked());
      this.field07439.withVisibility(() -> this.field07430.isChecked());
      this.field07440.withVisibility(() -> this.field07430.isChecked());
      this.field07441.withVisibility(() -> this.field07430.isChecked() && this.field07440.isChecked());
   }

   public static CustomWorldModule method2204() {
      if (customWorldModule == null) {
         customWorldModule = new CustomWorldModule();
      }

      return customWorldModule;
   }

   private void method2207() {
      if (this.field07459 != null && !this.field07459.isClosed()) {
         this.field07459.close();
      }

      this.field07459 = null;
      if (this.field07460 != null && !this.field07460.isClosed()) {
         this.field07460.close();
      }

      this.field07460 = null;
      if (this.field07458 != null) {
         this.field07458.delete();
         this.field07458 = null;
      }
   }

   private void method2211(Matrix4f matrix4f, Matrix4f matrix4f2) {
      Framebuffer framebuffer = mc.getFramebuffer();
      if (framebuffer != null
         && framebuffer.getColorAttachmentView() != null
         && framebuffer.getDepthAttachmentView() != null
         && framebuffer.textureWidth > 0
         && framebuffer.textureHeight > 0) {
         if (this.field07459 == null || this.field07459.isClosed()) {
            try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
               BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION);
               bufferBuilder.vertex(-1.0F, -1.0F, 0.0F);
               bufferBuilder.vertex(-1.0F, 1.0F, 0.0F);
               bufferBuilder.vertex(1.0F, 1.0F, 0.0F);
               bufferBuilder.vertex(1.0F, -1.0F, 0.0F);

               try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                  this.field07459 = RenderSystem.getDevice().createBuffer(() -> "deluxe_haze_quad", 32, builtBuffer.getBuffer());
               }
            }
         }

         if (this.field07460 == null || this.field07460.isClosed()) {
            this.field07460 = RenderSystem.getDevice().createBuffer(() -> "deluxe_haze_params", 136, 128L);
         }

         int value = this.field07445.getValue();
         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = memoryStack.calloc(128);
            this.field07457.set(matrix4f2).mul(matrix4f).invert().get(0, byteBuffer);
            byteBuffer.putFloat(64, (value >> 16 & 0xFF) / 255.0F);
            byteBuffer.putFloat(68, (value >> 8 & 0xFF) / 255.0F);
            byteBuffer.putFloat(72, (value & 0xFF) / 255.0F);
            byteBuffer.putFloat(76, (value >>> 24 & 0xFF) / 255.0F);
            byteBuffer.putFloat(80, this.field07444.getValue());
            byteBuffer.putFloat(84, this.field07444.getValue() * 0.3F);
            byteBuffer.putFloat(88, this.field07446.getValue());
            byteBuffer.putFloat(92, (float)(System.currentTimeMillis() % 1000000L) / 1000.0F);
            byteBuffer.putFloat(112, (float)mc.gameRenderer.getCamera().getCameraPos().x);
            byteBuffer.putFloat(116, (float)mc.gameRenderer.getCamera().getCameraPos().y);
            byteBuffer.putFloat(120, (float)mc.gameRenderer.getCamera().getCameraPos().z);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field07460.slice(0L, 128L), byteBuffer);
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

         int local3447_5x = Math.max(1, framebuffer.textureWidth / 2);
         int local3447_6 = Math.max(1, framebuffer.textureHeight / 2);
         if (this.field07458 == null) {
            this.field07458 = new SimpleFramebuffer("deluxe_haze_layer", local3447_5x, local3447_6, false);
         }

         if (this.field07458.textureWidth != local3447_5x || this.field07458.textureHeight != local3447_6) {
            this.field07458.resize(local3447_5x, local3447_6);
         }

         try (RenderPass renderPass = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "deluxe_haze", this.field07458.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
            renderPass.setPipeline(field07455);
            renderPass.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.setUniform("SkyPostParams", this.field07460);
            renderPass.setVertexBuffer(0, this.field07459);
            renderPass.setIndexBuffer(
               RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
            );
            renderPass.drawIndexed(0, 0, 6, 1);
         }

         try (RenderPass renderPass2 = RenderSystem.getDevice()
               .createCommandEncoder()
               .createRenderPass(() -> "deluxe_haze_blit", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
            renderPass2.setPipeline(field07456);
            renderPass2.bindTexture("Sampler0", this.field07458.getColorAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass2.bindTexture("Sampler1", framebuffer.getDepthAttachmentView(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass2.setUniform("SkyPostParams", this.field07460);
            renderPass2.setVertexBuffer(0, this.field07459);
            renderPass2.setIndexBuffer(
               RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
            );
            renderPass2.drawIndexed(0, 0, 6, 1);
         }
      }
   }

   public void method2213(Matrix4f matrix4f, Matrix4f matrix4f2) {
      if (this.method2225() && mc.world != null) {
         if (this.method2216()) {
            this.method2211(matrix4f, matrix4f2);
         } else {
            this.method2207();
            float value = this.field07444.getValue();
            if (this.field07454 != value) {
               this.method2193(value);
            }

            int local3449_4 = this.field07445.getValue() & 16777215;
            float value2 = this.field07446.getValue();
            this.field07452.set(mc.gameRenderer.getCamera().getRotation()).rotateY((float) Math.PI);
            Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
            VertexConsumer vertexConsumer = this.field07450.getBuffer(field07449);

            for (int index = 0; index < 80; index++) {
               int local3449_9 = index * 5;
               float local3449_10 = this.field07451[local3449_9 + 4];
               int local3449_11 = MathHelper.clamp((int)(value2 * (0.03F + local3449_10 * local3449_10 * 0.12F) * 255.0F), 0, 255);
               if (local3449_11 > 1) {
                  float local3449_12 = this.field07451[local3449_9 + 3] * 0.5F;
                  if (WorldProjection.method1124(
                     cameraPos.x + this.field07451[local3449_9],
                     cameraPos.y + this.field07451[local3449_9 + 1],
                     cameraPos.z + this.field07451[local3449_9 + 2],
                     local3449_12 * 1.42F
                  )) {
                     int local3449_13 = local3449_11 << 24 | local3449_4;
                     this.method2194(vertexConsumer, matrix4f, local3449_9, -local3449_12, -local3449_12, 0.0F, 0.0F, local3449_13);
                     this.method2194(vertexConsumer, matrix4f, local3449_9, -local3449_12, local3449_12, 0.0F, 1.0F, local3449_13);
                     this.method2194(vertexConsumer, matrix4f, local3449_9, local3449_12, local3449_12, 1.0F, 1.0F, local3449_13);
                     this.method2194(vertexConsumer, matrix4f, local3449_9, local3449_12, -local3449_12, 1.0F, 0.0F, local3449_13);
                  }
               }
            }

            this.field07450.draw();
         }
      } else {
         this.method2207();
      }
   }

   public boolean method2216() {
      return this.method2225() && this.field07443.isSelected("Дымка");
   }

   public void method2219() {
      if (this.field07462 != null && !this.field07462.isClosed()) {
         this.field07462.close();
      }

      this.field07462 = null;
      if (this.field07463 != null && !this.field07463.isClosed()) {
         this.field07463.close();
      }

      this.field07463 = null;
      if (this.field07464 != null && !this.field07464.isClosed()) {
         this.field07464.close();
      }

      this.field07464 = null;
      if (this.field07465 != null) {
         this.field07465.delete();
         this.field07465 = null;
      }
   }

   public static int method2220() {
      int local3456_0 = Math.round(Math.min(Math.max(field07461[0], 0.0F), 1.0F) * 255.0F);
      int local3456_1 = Math.round(Math.min(Math.max(field07461[1], 0.0F), 1.0F) * 255.0F);
      int local3456_2 = Math.round(Math.min(Math.max(field07461[2], 0.0F), 1.0F) * 255.0F);
      return 0xFF000000 | local3456_0 << 16 | local3456_1 << 8 | local3456_2;
   }

   private static RenderPipeline method2224(String local3460_0) {
      return RenderPipeline.builder()
         .withLocation(DeluxeClient.id("pipeline/" + local3460_0))
         .withVertexShader(DeluxeClient.id("core/sky_post"))
         .withFragmentShader(DeluxeClient.id("core/" + local3460_0))
         .withVertexFormat(VertexFormats.POSITION, DrawMode.QUADS)
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .withUniform("SkyPostParams", UniformType.UNIFORM_BUFFER)
         .build();
   }

   public boolean method2225() {
      return this.isEnabled() && this.field07442.isChecked();
   }
}
