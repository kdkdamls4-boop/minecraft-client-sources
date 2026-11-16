package deluxe.client.module.render;

import deluxe.client.module.combat.AimAssistModule;
import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.DeluxeClient;
import deluxe.client.util.DepthSnapshot;
import deluxe.client.util.Easings;
import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.render.WorldProjection;
import deluxe.client.util.EntityUtils;
import deluxe.client.ui.UiTheme;
import deluxe.client.util.RaycastUtils;
import deluxe.client.ui.AnimatedValue;
import deluxe.client.render.ThemeColors;
import deluxe.client.module.combat.TriggerBotModule;
import deluxe.client.event.WorldRenderEvent;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

@ModuleInfo(name = "TargetEsp", description = "Подсвечивает цель AttackAura, AimAssist или TrigerBot призраками и кольцом.", category = ModuleCategory.RENDER)
public final class TargetEspModule extends Module {
   private static TargetEspModule targetEspModule;
   private static final RenderPipeline field06267 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/target_ghosts"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderPipeline field06268 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/target_ghosts_depth"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderPipeline field06269 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/target_crystals"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline field06270 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/target_crystals_depth"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline field06271 = RenderPipeline.builder()
      .withLocation(DeluxeClient.id("pipeline/target_scan"))
      .withVertexShader(DeluxeClient.id("core/scan"))
      .withFragmentShader(DeluxeClient.id("core/scan"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .withUniform("ScanParams", UniformType.UNIFORM_BUFFER)
      .build();
   private static final Vector4f field06272 = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f field06273 = new Vector3f();
   private static final Matrix4f field06274 = new Matrix4f();
   private static final Vector3f[] field06275 = new Vector3f[]{
      new Vector3f(0.0F, 1.5F, 0.0F),
      new Vector3f(0.0F, -1.5F, 0.0F),
      new Vector3f(1.0F, 0.0F, 0.0F),
      new Vector3f(-1.0F, 0.0F, 0.0F),
      new Vector3f(0.0F, 0.0F, 1.0F),
      new Vector3f(0.0F, 0.0F, -1.0F)
   };
   private static final int[][] field06276 = new int[][]{{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
   private static final float[] field06277 = new float[]{1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F};
   public ModeSetting field06278;
   public ModeSetting field06279;
   public ModeSetting field06280;
   public BooleanSetting field06281;
   public BooleanSetting field06282;
   private final AnimatedValue field06283 = new AnimatedValue(350L);
   private LivingEntity field06284;
   private float field06285;
   private long field06286 = System.currentTimeMillis();
   private float field06287;
   private float field06288;
   private long field06289 = System.currentTimeMillis();
   private float field06290;
   private LivingEntity field06291;
   private long field06292;
   private long field06293;
   private float[] field06294 = new float[192];
   private float[] field06295 = new float[192];
   private float[] field06296 = new float[64];
   private int field06297;
   private ArrayList<float[]> field06298 = new ArrayList<>();
   private BufferBuilder field06299;
   private BufferBuilder field06300;
   private Quaternionf field06301;
   private Vector3f field06302 = new Vector3f();
   private float field06303;
   private float field06304;
   private float field06305;
   private int field06306;
   private int field06307;
   private final HashMap<Integer, float[]> field06308 = new HashMap<>();
   private float field06309;
   private float field06310;
   private long field06311;
   private final HashMap<Integer, Vec3d> field06312 = new HashMap<>();
   private final AnimatedValue field06313 = new AnimatedValue(400L);
   private float field06314;
   private long field06315;
   private final float[] field06316 = new float[3];
   private long field06317 = System.currentTimeMillis();
   private GpuBuffer field06318;
   private final GpuBuffer[] field06319 = new GpuBuffer[8];
   private int field06320;
   private final BufferAllocator[] field06321 = new BufferAllocator[2];
   private final HashMap<String, Identifier> field06322 = new HashMap<>();
   private GpuBuffer field06323;
   private boolean field06324;
   private float field06325;
   private float field06326;
   private float field06327;
   private final Matrix4f field06328 = new Matrix4f();
   private GpuBufferSlice field06329;
   private ProjectionType field06330;
   private final EventListener<WorldRenderEvent> field06331 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         this.field06320 = 0;
         if (this.field06278.isSelected("Цепи") && this.field06281.isChecked()) {
            this.method1910(worldRenderEvent.method0647(), worldRenderEvent.method0646());
         }

         if (this.field06278.isSelected("Молнии") && this.field06282.isChecked()) {
            this.method1940(worldRenderEvent.method0647(), worldRenderEvent.method0646());
         }

         LivingEntity livingEntity = AttackAuraModule.method1029().isEnabled() && AttackAuraModule.method1029().method1005() != null
            ? AttackAuraModule.method1029().method1005()
            : (
               AimAssistModule.method0883().isEnabled() && AimAssistModule.method0883().method0903() != null
                  ? AimAssistModule.method0883().method0903()
                  : (TriggerBotModule.method4593().isEnabled() ? TriggerBotModule.method4593().method4599() : null)
            );
         if (livingEntity != null && livingEntity != this.field06284) {
            this.field06284 = livingEntity;
            this.field06283.method3870(0.0);
            this.field06310 = 0.0F;
            this.field06311 = System.currentTimeMillis();
            this.field06313.method3870(0.0);
            this.field06315 = System.currentTimeMillis();
         }

         this.field06283.method3869(livingEntity != null ? 1.0 : 0.0);
         if (this.field06284 != null) {
            if (this.field06284.isRemoved()) {
               this.field06284 = null;
            } else {
               float local2980_3 = (float)this.field06283.method3871();
               if (local2980_3 < 0.003921569F) {
                  if (livingEntity == null) {
                     this.field06284 = null;
                  }

                  this.field06285 = 0.0F;
               } else {
                  float local2980_4 = this.field06284.hurtTime / 10.0F;
                  if (this.field06278.isSelected("Призраки")) {
                     if (TagsModule.method4106().method4095()) {
                        this.field06324 = true;
                        this.field06325 = local2980_3;
                        this.field06326 = local2980_4;
                        this.field06327 = worldRenderEvent.method0646();
                        this.field06328.set(worldRenderEvent.method0647().peek().getPositionMatrix());
                        this.field06329 = RenderSystem.getProjectionMatrixBuffer();
                        this.field06330 = RenderSystem.getProjectionType();
                     } else if (this.field06279.isSelected("Второй")) {
                        this.method1915(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                     } else {
                        this.method1902(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                     }
                  }

                  if (this.field06278.isSelected("Молнии")) {
                     this.method1913(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                  }

                  if (this.field06278.isSelected("Круг")) {
                     this.method1926(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                  }

                  if (this.field06278.isSelected("Кристаллы")) {
                     this.method1925(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                  }

                  if (this.field06278.isSelected("Ромб")) {
                     this.method1934(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                  }

                  if (this.field06278.isSelected("Цепи")) {
                     this.method1906(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                  }

                  if (this.field06278.isSelected("Череп")) {
                     this.method1938(worldRenderEvent.method0647(), this.field06284, worldRenderEvent.method0646(), local2980_3, local2980_4);
                  }
               }
            }
         }
      }
   };

   private BufferAllocator method1901(int local2977_1, int local2977_2) {
      BufferAllocator bufferAllocator = this.field06321[local2977_1];
      if (bufferAllocator == null) {
         bufferAllocator = this.field06321[local2977_1] = new BufferAllocator(local2977_2);
      }

      CloseableBuffer allocated = bufferAllocator.getAllocated();
      if (allocated != null) {
         allocated.close();
      }

      return bufferAllocator;
   }

   private void method1902(MatrixStack matrixStack, LivingEntity entity, float local2978_3, float local2978_4, float local2978_5) {
      if (mc.getTextureManager().getTexture(Identifier.of("deluxe", "textures/particles/bloom.png")).getGlTextureView() != null
         && mc.getFramebuffer() != null
         && mc.getFramebuffer().getColorAttachmentView() != null) {
         Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
         double local2978_7 = MathHelper.lerp(local2978_3, entity.lastRenderX, entity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
         double local2978_9 = MathHelper.lerp(local2978_3, entity.lastRenderY, entity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y;
         double local2978_11 = MathHelper.lerp(local2978_3, entity.lastRenderZ, entity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
         this.field06310 = this.field06310 + 3.5F * (float)(System.currentTimeMillis() - this.field06311) / 1000.0F;
         this.field06311 = System.currentTimeMillis();
         int local2978_13 = 4;
         int local2978_14 = 15;
         float local2978_15 = 0.68F;
         float local2978_16 = 1.05F;
         float local2978_17 = 0.24F;
         float local2978_18 = 0.12F;
         float local2978_19 = 0.45F;
         float local2978_20 = -0.02F;
         float local2978_21 = 0.12F;
         float local2978_22 = 0.02F;
         float local2978_23 = 0.05F;
         float local2978_24 = (float)(0.5 * Math.asin(local2978_23 / 0.5F));
         int local2978_25 = 0;
         BufferAllocator bufferAllocator = this.method1901(1, local2978_13 * local2978_14 * 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
         BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         Vector3f vector3f = new Vector3f();

         for (int index = 0; index < local2978_13; index++) {
            float local2978_30 = index < 2 ? 1.0F : -1.0F;
            float local2978_31 = index % 2 == 0 ? -1.0F : 1.0F;
            float local2978_32 = index % 2 == 0 ? local2978_20 : -local2978_20;
            float local2978_33 = index % 2 == 0 ? local2978_19 : -local2978_19;

            for (int index2 = 0; index2 < local2978_14; index2++) {
               int local2978_35 = index % 2 == 0 ? index2 : local2978_14 - 1 - index2;
               float local2978_36 = this.field06310 + local2978_35 * local2978_24;
               float local2978_37 = 5.0F * MathHelper.lerp((float)local2978_35 / local2978_14, local2978_22, local2978_23);
               this.method1931(
                  bufferBuilder,
                  rotation,
                  vector3f,
                  local2978_7 + local2978_30 * (local2978_15 * MathHelper.sin(local2978_36 + local2978_21) + local2978_32),
                  local2978_9 + local2978_16 + local2978_17 * MathHelper.sin(this.field06310 + local2978_35 * local2978_18) + local2978_33,
                  local2978_11 + local2978_30 * local2978_31 * local2978_15 * MathHelper.cos(local2978_36 - local2978_21),
                  local2978_37,
                  0.0F,
                  Colors.method3727(this.method1914(index * 90, local2978_5), local2978_4)
               );
               local2978_25++;
            }
         }

         try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
            this.method1933(
               matrixStack, "textures/particles/bloom.png", builtBuffer, local2978_25, RaycastUtils.method3289(mc.player, entity), false
            );
         }
      }
   }

   private float method1903(float local2979_1, float local2979_2, float local2979_3) {
      return MathHelper.clamp((local2979_3 - local2979_1) / (local2979_2 - local2979_1), 0.0F, 1.0F)
         * MathHelper.clamp((local2979_3 - local2979_1) / (local2979_2 - local2979_1), 0.0F, 1.0F)
         * (3.0F - 2.0F * MathHelper.clamp((local2979_3 - local2979_1) / (local2979_2 - local2979_1), 0.0F, 1.0F));
   }

   private void method1906(MatrixStack matrixStack, LivingEntity entity, float local2982_3, float local2982_4, float local2982_5) {
      this.field06314 = (this.field06314 + 75.0F * (float)(System.currentTimeMillis() - this.field06315) / 1000.0F) % 720.0F;
      this.field06315 = System.currentTimeMillis();
      this.field06313.method3869(local2982_4 >= 0.95F ? 1.0 : 0.0);
      float local2982_6 = 20.0F * Math.min(1.0F + MathHelper.sin((float)Math.toRadians(this.field06314)), 1.0F);
      float local2982_7 = 20.0F * MathHelper.sin((float)Math.toRadians(this.field06314));
      float local2982_8 = entity.getWidth() * 1.5F;
      float local2982_9 = 1.0F;
      float local2982_10 = 22.5F;
      double local2982_11 = MathHelper.lerp(local2982_3, entity.lastRenderX, entity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
      double local2982_13 = MathHelper.lerp(local2982_3, entity.lastRenderY, entity.getY())
         + entity.getHeight() * 0.5F
         - local2982_9 / 2.0F
         - mc.gameRenderer.getCamera().getCameraPos().y;
      double local2982_15 = MathHelper.lerp(local2982_3, entity.lastRenderZ, entity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
      int local2982_17 = 0;
      Vector3f vector3f = new Vector3f();
      BufferAllocator bufferAllocator = this.method1901(0, 256 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferAllocator bufferAllocator2 = this.method1901(1, 256 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int index = 0; index < 2; index++) {
         float local2982_24 = index == 0 ? local2982_4 : (float)this.field06313.method3871();
         if (!(local2982_24 < 0.003921569F)) {
            float local2982_25 = local2982_8 * (1.2F - 0.5F * local2982_24);
            float local2982_26 = (index == 0 ? local2982_6 : -local2982_6) / 100.0F;
            float local2982_27 = (index == 0 ? -local2982_7 : local2982_7) / 100.0F;
            Quaternionf quaternionf = new Quaternionf()
               .rotateZ((float)Math.toRadians(index == 0 ? local2982_6 : -local2982_6))
               .rotateX((float)Math.toRadians(index == 0 ? local2982_7 : -local2982_7));

            for (float local2982_29 = local2982_10; local2982_29 <= 720.0F; local2982_29 += local2982_10) {
               int local2982_30 = Colors.method3727(this.method1914(local2982_29 * 3.0F, local2982_5), local2982_24);
               float local2982_31 = local2982_26 + MathHelper.sin((float)Math.toRadians(local2982_29 - local2982_10 + this.field06314 * 0.5F)) * local2982_25;
               float local2982_32 = local2982_27 + MathHelper.cos((float)Math.toRadians(local2982_29 - local2982_10 + this.field06314 * 0.5F)) * local2982_25;
               float local2982_33 = local2982_26 + MathHelper.sin((float)Math.toRadians(local2982_29 + this.field06314 * 0.5F)) * local2982_25;
               float local2982_34 = local2982_27 + MathHelper.cos((float)Math.toRadians(local2982_29 + this.field06314 * 0.5F)) * local2982_25;
               quaternionf.transform(vector3f.set(local2982_31, 0.0F, local2982_32));
               bufferBuilder.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture((local2982_29 - local2982_10) / 360.0F * 3.0F, 0.0F)
                  .color(local2982_30);
               quaternionf.transform(vector3f.set(local2982_33, 0.0F, local2982_34));
               bufferBuilder.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture(local2982_29 / 360.0F * 3.0F, 0.0F)
                  .color(local2982_30);
               quaternionf.transform(vector3f.set(local2982_33, local2982_9, local2982_34));
               bufferBuilder.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture(local2982_29 / 360.0F * 3.0F, 1.0F)
                  .color(local2982_30);
               quaternionf.transform(vector3f.set(local2982_31, local2982_9, local2982_32));
               bufferBuilder.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture((local2982_29 - local2982_10) / 360.0F * 3.0F, 1.0F)
                  .color(local2982_30);
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (local2982_31 + local2982_33) / 2.0F, local2982_31),
                     MathHelper.lerp(0.87F, local2982_9 / 2.0F, 0.0F),
                     MathHelper.lerp(0.87F, (local2982_32 + local2982_34) / 2.0F, local2982_32)
                  )
               );
               bufferBuilder2.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture(0.0F, 0.0F)
                  .color(Colors.method3727(local2982_30, 0.05F));
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (local2982_31 + local2982_33) / 2.0F, local2982_33),
                     MathHelper.lerp(0.87F, local2982_9 / 2.0F, 0.0F),
                     MathHelper.lerp(0.87F, (local2982_32 + local2982_34) / 2.0F, local2982_34)
                  )
               );
               bufferBuilder2.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture(1.0F, 0.0F)
                  .color(Colors.method3727(local2982_30, 0.05F));
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (local2982_31 + local2982_33) / 2.0F, local2982_33),
                     MathHelper.lerp(0.87F, local2982_9 / 2.0F, local2982_9),
                     MathHelper.lerp(0.87F, (local2982_32 + local2982_34) / 2.0F, local2982_34)
                  )
               );
               bufferBuilder2.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture(1.0F, 1.0F)
                  .color(Colors.method3727(local2982_30, 0.05F));
               quaternionf.transform(
                  vector3f.set(
                     MathHelper.lerp(0.87F, (local2982_31 + local2982_33) / 2.0F, local2982_31),
                     MathHelper.lerp(0.87F, local2982_9 / 2.0F, local2982_9),
                     MathHelper.lerp(0.87F, (local2982_32 + local2982_34) / 2.0F, local2982_32)
                  )
               );
               bufferBuilder2.vertex((float)(local2982_11 + vector3f.x), (float)(local2982_13 + vector3f.y), (float)(local2982_15 + vector3f.z))
                  .texture(0.0F, 1.0F)
                  .color(Colors.method3727(local2982_30, 0.05F));
               local2982_17++;
            }
         }
      }

      try (BuiltBuffer builtBuffer = bufferBuilder2.end()) {
         this.method1933(
            matrixStack, "textures/particles/glow.png", builtBuffer, local2982_17, RaycastUtils.method3289(mc.player, entity), false
         );
      }

      try (BuiltBuffer builtBuffer2 = bufferBuilder.end()) {
         this.method1933(
            matrixStack, "textures/particles/chain.png", builtBuffer2, local2982_17, RaycastUtils.method3289(mc.player, entity), true
         );
      }
   }

   private int method1907(
      BufferBuilder bufferBuilder,
      float local2983_2,
      float local2983_3,
      float local2983_4,
      LivingEntity livingEntity,
      float local2983_6,
      float local2983_7,
      float local2983_8,
      float local2983_9,
      float local2983_10,
      float local2983_11,
      int local2983_12,
      int local2983_13
   ) {
      int local2983_14 = 0;

      for (int index = 0; index < 360; index += 2) {
         float local2983_16 = MathHelper.cos((float)Math.toRadians(index));
         float local2983_17 = MathHelper.sin((float)Math.toRadians(index));
         float local2983_18 = MathHelper.cos((float)Math.toRadians(index + 2));
         float local2983_19 = MathHelper.sin((float)Math.toRadians(index + 2));
         bufferBuilder.vertex(
               local2983_2 + local2983_16 * (this.method1928(livingEntity, local2983_8 / local2983_6, local2983_7) + local2983_10),
               local2983_3 + local2983_8,
               local2983_4 + local2983_17 * (this.method1928(livingEntity, local2983_8 / local2983_6, local2983_7) + local2983_10)
            )
            .color(local2983_12);
         bufferBuilder.vertex(
               local2983_2 + local2983_18 * (this.method1928(livingEntity, local2983_8 / local2983_6, local2983_7) + local2983_10),
               local2983_3 + local2983_8,
               local2983_4 + local2983_19 * (this.method1928(livingEntity, local2983_8 / local2983_6, local2983_7) + local2983_10)
            )
            .color(local2983_12);
         bufferBuilder.vertex(
               local2983_2 + local2983_18 * (this.method1928(livingEntity, local2983_9 / local2983_6, local2983_7) + local2983_11),
               local2983_3 + local2983_9,
               local2983_4 + local2983_19 * (this.method1928(livingEntity, local2983_9 / local2983_6, local2983_7) + local2983_11)
            )
            .color(local2983_13);
         bufferBuilder.vertex(
               local2983_2 + local2983_16 * (this.method1928(livingEntity, local2983_9 / local2983_6, local2983_7) + local2983_11),
               local2983_3 + local2983_9,
               local2983_4 + local2983_17 * (this.method1928(livingEntity, local2983_9 / local2983_6, local2983_7) + local2983_11)
            )
            .color(local2983_13);
         local2983_14++;
      }

      return local2983_14;
   }

   private int method1908(float local2984_1, boolean local2984_2) {
      if (this.field06297 < 2) {
         return 0;
      }

      for (int index = 0; index < this.field06297; index++) {
         float local2984_4 = this.field06294[index * 3];
         float local2984_5 = this.field06294[index * 3 + 1];
         float local2984_6 = this.field06294[index * 3 + 2];
         float local2984_7 = 0.0F;
         float local2984_8 = 0.0F;
         float local2984_9 = 0.0F;
         float local2984_10 = 0.0F;
         float local2984_11 = 0.0F;
         float local2984_12 = 0.0F;
         boolean local2984_13 = index > 0;
         boolean local2984_14 = index < this.field06297 - 1;
         if (local2984_13) {
            float local2984_15 = local2984_4 - this.field06294[index * 3 - 3];
            float local2984_16 = local2984_5 - this.field06294[index * 3 - 2];
            float local2984_17 = local2984_6 - this.field06294[index * 3 - 1];
            local2984_7 = local2984_16 * local2984_6 - local2984_17 * local2984_5;
            local2984_8 = local2984_17 * local2984_4 - local2984_15 * local2984_6;
            local2984_9 = local2984_15 * local2984_5 - local2984_16 * local2984_4;
            float local2984_18 = MathHelper.sqrt(local2984_7 * local2984_7 + local2984_8 * local2984_8 + local2984_9 * local2984_9);
            if (local2984_18 < 1.0E-6F) {
               local2984_13 = false;
            } else {
               local2984_7 /= local2984_18;
               local2984_8 /= local2984_18;
               local2984_9 /= local2984_18;
            }
         }

         if (local2984_14) {
            float local2984_15 = this.field06294[index * 3 + 3] - local2984_4;
            float local2984_16 = this.field06294[index * 3 + 4] - local2984_5;
            float local2984_17 = this.field06294[index * 3 + 5] - local2984_6;
            local2984_10 = local2984_16 * local2984_6 - local2984_17 * local2984_5;
            local2984_11 = local2984_17 * local2984_4 - local2984_15 * local2984_6;
            local2984_12 = local2984_15 * local2984_5 - local2984_16 * local2984_4;
            float local2984_18 = MathHelper.sqrt(local2984_10 * local2984_10 + local2984_11 * local2984_11 + local2984_12 * local2984_12);
            if (local2984_18 < 1.0E-6F) {
               local2984_14 = false;
            } else {
               local2984_10 /= local2984_18;
               local2984_11 /= local2984_18;
               local2984_12 /= local2984_18;
            }
         }

         float local2984_18 = 1.0F;
         float local2984_15;
         float local2984_16;
         float local2984_17;
         if (local2984_13 && local2984_14) {
            if (local2984_7 * local2984_10 + local2984_8 * local2984_11 + local2984_9 * local2984_12 < 0.0F) {
               local2984_10 = -local2984_10;
               local2984_11 = -local2984_11;
               local2984_12 = -local2984_12;
            }

            local2984_15 = local2984_7 + local2984_10;
            local2984_16 = local2984_8 + local2984_11;
            local2984_17 = local2984_9 + local2984_12;
            float local2984_19 = MathHelper.sqrt(local2984_15 * local2984_15 + local2984_16 * local2984_16 + local2984_17 * local2984_17);
            if (local2984_19 < 1.0E-6F) {
               local2984_15 = local2984_7;
               local2984_16 = local2984_8;
               local2984_17 = local2984_9;
            } else {
               local2984_15 /= local2984_19;
               local2984_16 /= local2984_19;
               local2984_17 /= local2984_19;
               local2984_18 = 1.0F / Math.max(local2984_15 * local2984_7 + local2984_16 * local2984_8 + local2984_17 * local2984_9, 0.35F);
            }
         } else if (local2984_13) {
            local2984_15 = local2984_7;
            local2984_16 = local2984_8;
            local2984_17 = local2984_9;
         } else if (local2984_14) {
            local2984_15 = local2984_10;
            local2984_16 = local2984_11;
            local2984_17 = local2984_12;
         } else {
            local2984_15 = 0.0F;
            local2984_16 = 0.0F;
            local2984_17 = 0.0F;
         }

         this.field06295[index * 3] = local2984_15;
         this.field06295[index * 3 + 1] = local2984_16;
         this.field06295[index * 3 + 2] = local2984_17;
         this.field06296[index] = local2984_18;
      }

      float local2984_3 = this.field06303 * local2984_1;
      float local2984_4 = this.field06305 * local2984_1;

      for (int index2 = 0; index2 < this.field06297 - 1; index2++) {
         float local2984_6 = 1.0F;
         float local2984_7 = 1.0F;
         if (local2984_2) {
            float local2984_8 = MathHelper.clamp(((float)index2 / (this.field06297 - 1) - 0.35F) / 0.65F, 0.0F, 1.0F);
            local2984_6 = 1.0F - 0.75F * local2984_8 * local2984_8 * (3.0F - 2.0F * local2984_8);
            local2984_8 = MathHelper.clamp(((float)(index2 + 1) / (this.field06297 - 1) - 0.35F) / 0.65F, 0.0F, 1.0F);
            local2984_7 = 1.0F - 0.75F * local2984_8 * local2984_8 * (3.0F - 2.0F * local2984_8);
         }

         float local2984_9 = this.field06295[index2 * 3] * local2984_3 * this.field06296[index2] * local2984_6;
         float local2984_10 = this.field06295[index2 * 3 + 1] * local2984_3 * this.field06296[index2] * local2984_6;
         float local2984_11 = this.field06295[index2 * 3 + 2] * local2984_3 * this.field06296[index2] * local2984_6;
         float local2984_12 = this.field06295[index2 * 3 + 3] * local2984_3 * this.field06296[index2 + 1] * local2984_7;
         float local2984_13 = this.field06295[index2 * 3 + 4] * local2984_3 * this.field06296[index2 + 1] * local2984_7;
         float local2984_14 = this.field06295[index2 * 3 + 5] * local2984_3 * this.field06296[index2 + 1] * local2984_7;
         this.field06299
            .vertex(
               this.field06294[index2 * 3] - local2984_9,
               this.field06294[index2 * 3 + 1] - local2984_10,
               this.field06294[index2 * 3 + 2] - local2984_11
            )
            .color(this.field06306);
         this.field06299
            .vertex(
               this.field06294[index2 * 3 + 3] - local2984_12,
               this.field06294[index2 * 3 + 4] - local2984_13,
               this.field06294[index2 * 3 + 5] - local2984_14
            )
            .color(this.field06306);
         this.field06299
            .vertex(
               this.field06294[index2 * 3 + 3] + local2984_12,
               this.field06294[index2 * 3 + 4] + local2984_13,
               this.field06294[index2 * 3 + 5] + local2984_14
            )
            .color(this.field06306);
         this.field06299
            .vertex(
               this.field06294[index2 * 3] + local2984_9,
               this.field06294[index2 * 3 + 1] + local2984_10,
               this.field06294[index2 * 3 + 2] + local2984_11
            )
            .color(this.field06306);
         this.method1931(
            this.field06300,
            this.field06301,
            this.field06302,
            (this.field06294[index2 * 3] + this.field06294[index2 * 3 + 3]) * 0.5F,
            (this.field06294[index2 * 3 + 1] + this.field06294[index2 * 3 + 4]) * 0.5F,
            (this.field06294[index2 * 3 + 2] + this.field06294[index2 * 3 + 5]) * 0.5F,
            local2984_4 * (local2984_6 + local2984_7) * 0.5F,
            0.0F,
            this.field06307
         );
      }

      return this.field06297 - 1;
   }

   public void method1909() {
      if (this.field06324) {
         this.field06324 = false;
         if (this.field06284 != null && !this.field06284.isRemoved()) {
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix(this.field06329, this.field06330);
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.multiplyPositionMatrix(this.field06328);
            if (this.field06279.isSelected("Второй")) {
               this.method1915(matrixStack, this.field06284, this.field06327, this.field06325, this.field06326);
            } else {
               this.method1902(matrixStack, this.field06284, this.field06327, this.field06325, this.field06326);
            }

            RenderSystem.restoreProjectionMatrix();
         }
      }
   }

   private void method1910(MatrixStack matrixStack, float local2986_2) {
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof PersistentProjectileEntity entity2
            && entity2.getOwner() == mc.player
            && !(entity2.getVelocity().lengthSquared() < 1.0E-5)) {
            double local2986_6 = MathHelper.lerp(local2986_2, mc.player.lastRenderX, mc.player.getX())
               - mc.gameRenderer.getCamera().getCameraPos().x;
            double local2986_8 = MathHelper.lerp(local2986_2, mc.player.lastRenderY, mc.player.getY())
               + mc.player.getStandingEyeHeight() * 0.75F
               - mc.gameRenderer.getCamera().getCameraPos().y;
            double local2986_10 = MathHelper.lerp(local2986_2, mc.player.lastRenderZ, mc.player.getZ())
               - mc.gameRenderer.getCamera().getCameraPos().z;
            double local2986_12 = MathHelper.lerp(local2986_2, entity2.lastRenderX, entity2.getX())
               - mc.gameRenderer.getCamera().getCameraPos().x;
            double local2986_14 = MathHelper.lerp(local2986_2, entity2.lastRenderY, entity2.getY())
               - mc.gameRenderer.getCamera().getCameraPos().y;
            double local2986_16 = MathHelper.lerp(local2986_2, entity2.lastRenderZ, entity2.getZ())
               - mc.gameRenderer.getCamera().getCameraPos().z;
            float local2986_18 = (float)Math.sqrt(
               (local2986_12 - local2986_6) * (local2986_12 - local2986_6)
                  + (local2986_14 - local2986_8) * (local2986_14 - local2986_8)
                  + (local2986_16 - local2986_10) * (local2986_16 - local2986_10)
            );
            float local2986_19 = 1.0F;
            float local2986_20 = local2986_18 * 0.06F;
            int local2986_21 = 16;
            float[] floatArray = new float[local2986_21 + 1];
            float[] floatArray2 = new float[local2986_21 + 1];
            float[] floatArray3 = new float[local2986_21 + 1];
            float[] floatArray4 = new float[local2986_21 + 1];
            float[] floatArray5 = new float[local2986_21 + 1];
            float[] floatArray6 = new float[local2986_21 + 1];
            BufferAllocator bufferAllocator = this.method1901(1, local2986_21 * 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int index = 0; index <= local2986_21; index++) {
               floatArray[index] = (float)MathHelper.lerp((double)index / local2986_21, local2986_6, local2986_12);
               floatArray2[index] = (float)(
                  MathHelper.lerp((double)index / local2986_21, local2986_8, local2986_14)
                     - local2986_20 * 4.0F * ((float)index / local2986_21) * (1.0F - (float)index / local2986_21)
               );
               floatArray3[index] = (float)MathHelper.lerp((double)index / local2986_21, local2986_10, local2986_16);
            }

            for (int index2 = 0; index2 <= local2986_21; index2++) {
               float local2986_28 = floatArray[Math.min(index2 + 1, local2986_21)] - floatArray[Math.max(index2 - 1, 0)];
               float local2986_29 = floatArray2[Math.min(index2 + 1, local2986_21)] - floatArray2[Math.max(index2 - 1, 0)];
               float local2986_30 = floatArray3[Math.min(index2 + 1, local2986_21)] - floatArray3[Math.max(index2 - 1, 0)];
               floatArray4[index2] = local2986_29 * floatArray3[index2] - local2986_30 * floatArray2[index2];
               floatArray5[index2] = local2986_30 * floatArray[index2] - local2986_28 * floatArray3[index2];
               floatArray6[index2] = local2986_28 * floatArray2[index2] - local2986_29 * floatArray[index2];
               float local2986_31 = Math.max(
                  1.0E-5F,
                  (float)Math.sqrt(
                     floatArray4[index2] * floatArray4[index2]
                        + floatArray5[index2] * floatArray5[index2]
                        + floatArray6[index2] * floatArray6[index2]
                  )
               );
               floatArray4[index2] *= local2986_19 / 2.0F / local2986_31;
               floatArray5[index2] *= local2986_19 / 2.0F / local2986_31;
               floatArray6[index2] *= local2986_19 / 2.0F / local2986_31;
            }

            for (int index3 = 0; index3 < local2986_21; index3++) {
               bufferBuilder.vertex(
                     floatArray[index3] - floatArray4[index3],
                     floatArray2[index3] - floatArray5[index3],
                     floatArray3[index3] - floatArray6[index3]
                  )
                  .texture((float)index3 / local2986_21 * local2986_18 / (local2986_19 * 1.3333F), 0.0F)
                  .color(this.method1914(index3 * 24.0F, 0.0F));
               bufferBuilder.vertex(
                     floatArray[index3 + 1] - floatArray4[index3 + 1],
                     floatArray2[index3 + 1] - floatArray5[index3 + 1],
                     floatArray3[index3 + 1] - floatArray6[index3 + 1]
                  )
                  .texture((float)(index3 + 1) / local2986_21 * local2986_18 / (local2986_19 * 1.3333F), 0.0F)
                  .color(this.method1914((index3 + 1) * 24.0F, 0.0F));
               bufferBuilder.vertex(
                     floatArray[index3 + 1] + floatArray4[index3 + 1],
                     floatArray2[index3 + 1] + floatArray5[index3 + 1],
                     floatArray3[index3 + 1] + floatArray6[index3 + 1]
                  )
                  .texture((float)(index3 + 1) / local2986_21 * local2986_18 / (local2986_19 * 1.3333F), 1.0F)
                  .color(this.method1914((index3 + 1) * 24.0F, 0.0F));
               bufferBuilder.vertex(
                     floatArray[index3] + floatArray4[index3],
                     floatArray2[index3] + floatArray5[index3],
                     floatArray3[index3] + floatArray6[index3]
                  )
                  .texture((float)index3 / local2986_21 * local2986_18 / (local2986_19 * 1.3333F), 1.0F)
                  .color(this.method1914(index3 * 24.0F, 0.0F));
            }

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               this.method1933(
                  matrixStack, "textures/particles/chain.png", builtBuffer, local2986_21, RaycastUtils.method3289(mc.player, entity2), true
               );
            }
         }
      }
   }

   public TargetEspModule.Nested0418 method1911(PlayerEntity playerEntity) {
      if (this.isEnabled()
         && this.field06278.isSelected("Круг")
         && this.field06280.isSelected("Четвёртый")
         && this.field06284 == playerEntity
         && !(this.field06283.method3871() <= 0.001F)) {
         if (this.field06291 != playerEntity || System.currentTimeMillis() - this.field06293 > 250L) {
            this.field06291 = playerEntity;
            this.field06292 = System.currentTimeMillis();
         }

         this.field06293 = System.currentTimeMillis();
         float local2987_2 = (float)((System.currentTimeMillis() - this.field06292) % 1800L) / 1800.0F;
         return new TargetEspModule.Nested0418(
            this.method1914(0.0F, this.field06290),
            -49088,
            local2987_2,
            this.method1903(0.0F, 1.0F, MathHelper.clamp((float)this.field06283.method3871(), 0.0F, 1.0F))
               * this.method1903(0.0F, 0.08F, local2987_2)
               * (1.0F - this.method1903(0.88F, 1.0F, local2987_2)),
            MathHelper.clamp(this.field06290, 0.0F, 1.0F),
            1.0F
         );
      } else {
         return null;
      }
   }

   private void method1913(MatrixStack matrixStack, LivingEntity entity, float local2989_3, float local2989_4, float local2989_5) {
      long local2989_6 = System.currentTimeMillis();
      this.field06285 = this.field06285 + (float)Math.min(local2989_6 - this.field06286, 100L) / 1000.0F;
      this.field06286 = local2989_6;
      this.field06287 = entity.hurtTime > 0 ? entity.hurtTime / 10.0F : Math.max(0.0F, this.field06287 - 0.1F);
      int local2989_8 = (int)this.field06285;
      float local2989_9 = this.field06285 - local2989_8;
      float height = entity.getHeight();
      float local2989_11 = entity.getWidth() * 0.75F + 0.16F;
      float local2989_12 = (float)(
         MathHelper.lerp(local2989_3, entity.lastRenderX, entity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
      );
      float local2989_13 = (float)(
         MathHelper.lerp(local2989_3, entity.lastRenderY, entity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
      );
      float local2989_14 = (float)(
         MathHelper.lerp(local2989_3, entity.lastRenderZ, entity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
      );
      boolean local2989_15 = RaycastUtils.method3289(mc.player, entity);
      this.field06303 = height * 0.005F;
      this.field06305 = height * 0.09F;
      this.field06301 = mc.gameRenderer.getCamera().getRotation();
      int local2989_16 = 0;
      int local2989_17 = 0;
      BufferAllocator bufferAllocator = this.method1901(0, 2048 * VertexFormats.POSITION_COLOR.getVertexSize());
      BufferAllocator bufferAllocator2 = this.method1901(1, 2048 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      this.field06299 = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      this.field06300 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

      for (int index = 0; index < 3; index++) {
         float local2989_21 = this.field06285 * 1.3F + index / 3.0F;
         int local2989_22 = (int)Math.floor(local2989_21);
         float local2989_23 = local2989_21 - local2989_22;
         float local2989_24 = MathHelper.clamp(local2989_23 / 0.08F, 0.0F, 1.0F)
            * (1.0F - MathHelper.clamp((local2989_23 - 0.42F) / 0.3F, 0.0F, 1.0F))
            * MathHelper.clamp(0.85F + 0.15F * this.method1920(index, 900, local2989_8, local2989_9), 0.0F, 1.0F)
            * local2989_4;
         if (!(local2989_24 < 0.003921569F)) {
            this.field06306 = Colors.method3727(
               Colors.method3728(
                  Colors.method3723(
                     Colors.method3723(-1, -1, (float)(Math.sin(System.currentTimeMillis() / 700.0 + Math.toRadians(44.0)) + 1.0) / 2.0F),
                     -49088,
                     local2989_5
                  ),
                  255
               ),
               local2989_24
            );
            this.field06307 = Colors.method3727(this.field06306, 0.13F);
            float local2989_25 = this.method1939(index, 11, local2989_22) * (float) Math.PI;
            float local2989_26 = 0.6F + 0.5F * Math.abs(this.method1939(index, 12, local2989_22));
            float local2989_27 = this.method1939(index, 13, local2989_22) >= 0.0F ? 1.0F : -1.0F;
            float local2989_28 = height * (0.85F + 0.05F * this.method1939(index, 14, local2989_22));
            float local2989_29 = height * 0.67F;
            float local2989_30 = local2989_11 * (1.0F + 0.12F * this.method1939(index, 15, local2989_22));
            float local2989_31 = height * 0.16F * MathHelper.sin(this.field06285 * 1.7F + index * 2.1F);
            float local2989_32 = 0.0F;
            float local2989_33 = 0.0F;
            float local2989_34 = 0.0F;

            for (int index2 = 0; index2 <= 3; index2++) {
               float local2989_36 = index2 / 3.0F;
               float local2989_37 = local2989_25 + this.field06285 * 0.9F + local2989_27 * local2989_26 * (float) (Math.PI * 2) * local2989_36;
               float local2989_38 = local2989_12 + MathHelper.cos(local2989_37) * local2989_30;
               float local2989_39 = local2989_13 + local2989_28 - local2989_29 * local2989_36 + local2989_31;
               float local2989_40 = local2989_14 + MathHelper.sin(local2989_37) * local2989_30;
               if (index2 > 0) {
                  local2989_17 += this.method1927(
                     local2989_32,
                     local2989_33,
                     local2989_34,
                     local2989_38,
                     local2989_39,
                     local2989_40,
                     (index * 131 + index2) * 17 + local2989_22 * 101 | 1,
                     local2989_8,
                     local2989_9,
                     index2 == 3
                  );
               }

               local2989_32 = local2989_38;
               local2989_33 = local2989_39;
               local2989_34 = local2989_40;
            }
         }
      }

      local2989_16 = local2989_17;
      if (local2989_16 > 0) {
         try (BuiltBuffer builtBuffer = this.field06300.end()) {
            this.method1933(matrixStack, "textures/particles/glow.png", builtBuffer, local2989_16, local2989_15, false);
         }
      }

      if (local2989_17 > 0) {
         try (BuiltBuffer builtBuffer2 = this.field06299.end()) {
            this.method1933(matrixStack, null, builtBuffer2, local2989_17, local2989_15, false);
         }
      }
   }

   private int method1914(float local2990_1, float local2990_2) {
      return Colors.method3723(
         Colors.method3723(
            UiTheme.field09686,
            UiTheme.field09687,
            (float)(Math.sin(System.currentTimeMillis() / 700.0 + Math.toRadians(local2990_1)) + 1.0) / 2.0F
         ),
         -49088,
         local2990_2
      );
   }

   private void method1915(MatrixStack matrixStack, LivingEntity livingEntity, float local2991_3, float local2991_4, float local2991_5) {
      Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
      double local2991_7 = MathHelper.lerp(local2991_3, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
      double local2991_9 = MathHelper.lerp(local2991_3, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y;
      double local2991_11 = MathHelper.lerp(local2991_3, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
      int local2991_13 = 4;
      int local2991_14 = 15;
      float width = livingEntity.getWidth();
      float local2991_16 = livingEntity.getHeight() / 2.0F + 0.2F;
      float local2991_17 = 0.7F;
      float local2991_18 = 0.2F;
      float local2991_19 = 0.6F;
      double local2991_20 = System.currentTimeMillis() / 1000.0;
      double local2991_22 = local2991_20 * 3.0 % (Math.PI * 2);
      double local2991_24 = Math.toRadians(60.0) / (local2991_14 - 1);
      int local2991_26 = 0;
      BufferAllocator bufferAllocator = this.method1901(1, local2991_13 * local2991_14 * 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Vector3f vector3f = new Vector3f();

      for (int index = 0; index < local2991_13; index++) {
         double local2991_31 = local2991_20 + index * 15.0;
         double local2991_33 = index * (Math.PI / 2);

         for (int index2 = 0; index2 < local2991_14; index2++) {
            double local2991_36 = index2 * local2991_24;
            double local2991_38 = local2991_36 + local2991_22 + local2991_33;
            this.method1931(
               bufferBuilder,
               rotation,
               vector3f,
               local2991_7 + width * Math.cos(local2991_38),
               local2991_9 + local2991_16 + Math.sin(local2991_31 + local2991_36 + index) * local2991_17,
               local2991_11 + width * Math.sin(local2991_38),
               MathHelper.lerp((float)index2 / (local2991_14 - 1), local2991_18, local2991_19) / 2.0F,
               0.0F,
               Colors.method3727(this.method1914(index2 * 14, local2991_5), local2991_4)
            );
            local2991_26++;
         }
      }

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.method1933(matrixStack, "textures/particles/glow.png", builtBuffer, local2991_26, false, false);
      }
   }

   private void method1916(
      float local2992_1,
      float local2992_2,
      float local2992_3,
      float local2992_4,
      float local2992_5,
      float local2992_6,
      int local2992_7,
      int local2992_8,
      int local2992_9,
      int local2992_10,
      float local2992_11
   ) {
      float local2992_12 = local2992_4 - local2992_1;
      float local2992_13 = local2992_5 - local2992_2;
      float local2992_14 = local2992_6 - local2992_3;
      float local2992_15 = MathHelper.sqrt(local2992_12 * local2992_12 + local2992_13 * local2992_13 + local2992_14 * local2992_14);
      if (local2992_7 > 0 && !(local2992_15 < 1.0E-4F)) {
         float local2992_16 = local2992_12 / local2992_15;
         float local2992_17 = local2992_13 / local2992_15;
         float local2992_18 = local2992_14 / local2992_15;
         float local2992_19 = Math.abs(local2992_17) < 0.9F ? 0.0F : 1.0F;
         float local2992_20 = Math.abs(local2992_17) < 0.9F ? 1.0F : 0.0F;
         float local2992_21 = -local2992_18 * local2992_20;
         float local2992_22 = local2992_18 * local2992_19;
         float local2992_23 = local2992_16 * local2992_20 - local2992_17 * local2992_19;
         float local2992_24 = MathHelper.sqrt(local2992_21 * local2992_21 + local2992_22 * local2992_22 + local2992_23 * local2992_23);
         if (local2992_24 < 1.0E-5F) {
            local2992_24 = 1.0F;
         }

         local2992_21 /= local2992_24;
         local2992_22 /= local2992_24;
         local2992_23 /= local2992_24;
         float local2992_25 = local2992_17 * local2992_23 - local2992_18 * local2992_22;
         float local2992_26 = local2992_18 * local2992_21 - local2992_16 * local2992_23;
         float local2992_27 = local2992_16 * local2992_22 - local2992_17 * local2992_21;
         float local2992_28 = this.method1920(local2992_8, 0, local2992_10, local2992_11);
         float local2992_29 = this.method1920(local2992_8, 1, local2992_10, local2992_11);
         float local2992_30 = local2992_15 * 0.3F;
         float local2992_31 = (local2992_1 + local2992_4) * 0.5F + (local2992_21 * local2992_28 + local2992_25 * local2992_29) * local2992_30;
         float local2992_32 = (local2992_2 + local2992_5) * 0.5F + (local2992_22 * local2992_28 + local2992_26 * local2992_29) * local2992_30;
         float local2992_33 = (local2992_3 + local2992_6) * 0.5F + (local2992_23 * local2992_28 + local2992_27 * local2992_29) * local2992_30;
         this.method1916(
            local2992_1,
            local2992_2,
            local2992_3,
            local2992_31,
            local2992_32,
            local2992_33,
            local2992_7 - 1,
            local2992_8 * 2 + 7,
            local2992_9,
            local2992_10,
            local2992_11
         );
         this.method1916(
            local2992_31,
            local2992_32,
            local2992_33,
            local2992_4,
            local2992_5,
            local2992_6,
            local2992_7 - 1,
            local2992_8 * 2 + 9,
            local2992_9,
            local2992_10,
            local2992_11
         );
         if (local2992_9 < 2 && local2992_7 >= 2 && this.method1920(local2992_8, 2, local2992_10, local2992_11) > 0.45F) {
            float local2992_34 = this.method1920(local2992_8, 3, local2992_10, local2992_11);
            float local2992_35 = this.method1920(local2992_8, 4, local2992_10, local2992_11);
            float local2992_36 = local2992_16 * 0.4F + local2992_21 * local2992_34 + local2992_25 * local2992_35;
            float local2992_37 = local2992_17 * 0.4F + local2992_22 * local2992_34 + local2992_26 * local2992_35;
            float local2992_38 = local2992_18 * 0.4F + local2992_23 * local2992_34 + local2992_27 * local2992_35;
            float local2992_39 = MathHelper.sqrt(local2992_36 * local2992_36 + local2992_37 * local2992_37 + local2992_38 * local2992_38);
            if (local2992_39 < 1.0E-5F) {
               local2992_39 = 1.0F;
            }

            float local2992_40 = local2992_15 * 0.75F;
            this.field06298
               .add(
                  new float[]{
                     local2992_31,
                     local2992_32,
                     local2992_33,
                     local2992_31 + local2992_36 / local2992_39 * local2992_40,
                     local2992_32 + local2992_37 / local2992_39 * local2992_40,
                     local2992_33 + local2992_38 / local2992_39 * local2992_40,
                     Math.min(local2992_7 - 1, 3),
                     local2992_8 * 3 + 13,
                     local2992_9 + 1,
                     0.55F
                  }
               );
         }
      } else {
         this.method1929(local2992_4, local2992_5, local2992_6);
      }
   }

   private void method1917(MatrixStack matrixStack, LivingEntity livingEntity, float local2993_3, float local2993_4, float local2993_5) {
      if (mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null && DepthSnapshot.method4228() != null) {
         float local2993_6 = (float)(
            MathHelper.lerp(local2993_3, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
         );
         float local2993_7 = (float)(
            MathHelper.lerp(local2993_3, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
         );
         float local2993_8 = (float)(
            MathHelper.lerp(local2993_3, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
         );
         float local2993_9 = livingEntity.getHeight() + 0.3F;
         float local2993_10 = livingEntity.getWidth() * 0.9F + 0.3F;
         float local2993_11 = livingEntity.getHeight() - 0.25F;
         float local2993_12 = (float)(System.currentTimeMillis() % 2200L) / 2200.0F * (float) (Math.PI * 2);
         float local2993_13 = 0.5F + 0.5F * MathHelper.sin(local2993_12);
         float local2993_14 = MathHelper.cos(local2993_12) >= 0.0F ? 1.0F : -1.0F;
         float local2993_15 = Math.abs(MathHelper.cos(local2993_12));
         float local2993_16 = local2993_7 + 0.12F + local2993_11 * local2993_13;
         int local2993_17 = this.method1914(0.0F, local2993_5);
         if (this.field06323 == null || this.field06323.isClosed()) {
            this.field06323 = RenderSystem.getDevice().createBuffer(() -> "deluxe_target_scan_params", 136, 112L);
         }

         MemoryStack memoryStack = MemoryStack.stackPush();

         try {
            ByteBuffer byteBuffer = memoryStack.calloc(112);
            new Matrix4f(WorldProjection.method1120()).mul(matrixStack.peek().getPositionMatrix()).invert().get(0, byteBuffer);
            byteBuffer.putFloat(64, local2993_6);
            byteBuffer.putFloat(68, local2993_7);
            byteBuffer.putFloat(72, local2993_8);
            byteBuffer.putFloat(76, local2993_10);
            byteBuffer.putFloat(80, local2993_16);
            byteBuffer.putFloat(84, local2993_14);
            byteBuffer.putFloat(88, local2993_15);
            byteBuffer.putFloat(92, (float)(System.currentTimeMillis() % 100000L) / 1000.0F);
            byteBuffer.putFloat(96, local2993_7 + 0.06F);
            byteBuffer.putFloat(100, local2993_7 - 0.1F + local2993_9);
            byteBuffer.putFloat(104, local2993_4);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.field06323.slice(0L, 112L), byteBuffer);
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

         try (BufferAllocator local2993_18x = new BufferAllocator(4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(local2993_18x, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            bufferBuilder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(local2993_17);
            bufferBuilder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(local2993_17);
            bufferBuilder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(local2993_17);
            bufferBuilder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(local2993_17);

            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
               if (this.field06318 != null && !this.field06318.isClosed() && this.field06318.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.field06318.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.field06318 != null && !this.field06318.isClosed()) {
                     this.field06318.close();
                  }

                  this.field06318 = RenderSystem.getDevice().createBuffer(() -> "deluxe_target_ghosts_vertices", 40, builtBuffer.getBuffer());
               }

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_target_scan", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(field06271);
                  renderPass.bindTexture("Sampler0", DepthSnapshot.method4228(), RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                  renderPass.setUniform("ScanParams", this.field06323);
                  renderPass.setVertexBuffer(0, this.field06318);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(6), RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, 6, 1);
               }
            }
         }
      }
   }

   private float method1920(int local2996_1, int local2996_2, int local2996_3, float local2996_4) {
      return MathHelper.lerp(
         local2996_4 * local2996_4 * (3.0F - 2.0F * local2996_4),
         this.method1939(local2996_1, local2996_2, local2996_3),
         this.method1939(local2996_1, local2996_2, local2996_3 + 1)
      );
   }

   private void method1925(MatrixStack matrixStack, LivingEntity entity, float local3001_3, float local3001_4, float local3001_5) {
      long local3001_6 = System.currentTimeMillis();
      float local3001_8 = (1.2F - 0.5F * local3001_4) * entity.getWidth() * 1.5F;
      float local3001_9 = 0.1F;
      float local3001_10 = 1.0F;
      int local3001_11 = Colors.method3727(this.method1914(0.0F, local3001_5), local3001_4);
      matrixStack.push();
      matrixStack.translate(
         entity.getLerpedPos(local3001_3).x - mc.gameRenderer.getCamera().getCameraPos().x,
         entity.getLerpedPos(local3001_3).y - mc.gameRenderer.getCamera().getCameraPos().y,
         entity.getLerpedPos(local3001_3).z - mc.gameRenderer.getCamera().getCameraPos().z
      );
      BufferAllocator bufferAllocator = this.method1901(0, 18 * field06276.length * 4 * VertexFormats.POSITION_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      MatrixStack matrixStack2 = new MatrixStack();
      int local3001_16 = 0;

      for (int index = 0; index < 360; index += 20) {
         float local3001_18 = (float)Math.toRadians(
            index + (float)((local3001_6 + (long)(7333.0F * (index / 360.0F))) % 7333L) / 7333.0F * 360.0F
         );
         float local3001_19 = MathHelper.sin(local3001_18) * local3001_8;
         float local3001_20 = MathHelper.cos(local3001_18) * local3001_8;
         float local3001_21 = 0.1F + entity.getHeight() * (float)Easings.EASE_IN_OUT_QUAD.apply(Math.abs(MathHelper.sin(index)));
         matrixStack2.push();
         matrixStack2.translate(local3001_19, local3001_21, local3001_20);
         matrixStack2.multiply(
            new Quaternionf()
               .rotationTo(
                  new Vector3f(0.0F, 1.0F, 0.0F), new Vector3f(-local3001_19, entity.getHeight() * 0.5F - local3001_21, -local3001_20).normalize()
               )
         );
         matrixStack2.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(((float)(local3001_6 % 1200L) / 1200.0F * 360.0F + index * 13.7F) % 360.0F));
         matrixStack2.scale(local3001_9, local3001_9, local3001_9);

         for (int index2 = 0; index2 < field06276.length; index2++) {
            for (int index3 = 0; index3 < 4; index3++) {
               Vector3f vector3f = field06275[field06276[index2][Math.min(index3, 2)]];
               bufferBuilder.vertex(matrixStack2.peek().getPositionMatrix(), vector3f.x, vector3f.y, vector3f.z)
                  .color(
                     Colors.method3723(
                        local3001_11, Colors.method3728(0, Colors.method3725(local3001_11)), 1.0F - field06277[index2]
                     )
                  );
            }

            local3001_16++;
         }

         matrixStack2.pop();
      }

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.method1933(matrixStack, null, builtBuffer, local3001_16, RaycastUtils.method3289(mc.player, entity), false);
      }

      bufferAllocator = this.method1901(1, 72 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Vector3f local3001_14x = new Vector3f();

      for (int index4 = 0; index4 < 360; index4 += 20) {
         float local3001_16x = (float)Math.toRadians(
            index4 + (float)((local3001_6 + (long)(7333.0F * (index4 / 360.0F))) % 7333L) / 7333.0F * 360.0F
         );
         this.method1931(
            bufferBuilder,
            mc.gameRenderer.getCamera().getRotation(),
            local3001_14x,
            MathHelper.sin(local3001_16x) * local3001_8,
            0.1F + entity.getHeight() * (float)Easings.EASE_IN_OUT_QUAD.apply(Math.abs(MathHelper.sin(index4))),
            MathHelper.cos(local3001_16x) * local3001_8,
            local3001_10 / 2.0F,
            0.0F,
            Colors.method3727(local3001_11, 0.2F)
         );
      }

      try (BuiltBuffer builtBuffer2 = bufferBuilder.end()) {
         this.method1933(matrixStack, "textures/particles/glow.png", builtBuffer2, 18, RaycastUtils.method3289(mc.player, entity), false);
      }

      matrixStack.pop();
   }

   private void method1926(MatrixStack matrixStack, LivingEntity livingEntity, float local3002_3, float local3002_4, float local3002_5) {
      long local3002_6 = System.currentTimeMillis();
      this.field06288 = this.field06288 + (float)Math.min(local3002_6 - this.field06289, 100L) / 1000.0F;
      this.field06289 = local3002_6;
      this.field06290 = livingEntity.hurtTime > 0 ? livingEntity.hurtTime / 10.0F : Math.max(0.0F, this.field06290 - 0.1F);
      if (!this.field06280.isSelected("Четвёртый")) {
         if (this.field06280.isSelected("Третий")) {
            float height = livingEntity.getHeight();
            float local3002_9 = (float)(System.currentTimeMillis() % 3600L) / 3600.0F * (float) (Math.PI * 2);
            float local3002_10 = 0.5F + 0.5F * MathHelper.cos(local3002_9);
            float local3002_11 = MathHelper.sin(local3002_9);
            float local3002_12 = this.method1903(0.0F, 1.0F, this.method1903(0.0F, 1.0F, MathHelper.clamp(local3002_4, 0.0F, 1.0F)));
            float local3002_13 = MathHelper.lerp(local3002_10, 0.035F, height - 0.035F);
            float local3002_14 = MathHelper.clamp(
               local3002_13 + local3002_11 * Math.min(0.55F, height * 0.55F) * this.method1903(0.0F, 1.0F, MathHelper.clamp(local3002_4, 0.0F, 1.0F)),
               0.02F,
               height - 0.02F
            );
            float local3002_15 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
            );
            float local3002_16 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
            );
            float local3002_17 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
            );
            int local3002_18 = this.method1914(0.0F, this.field06290);
            int local3002_19 = Colors.method3723(local3002_18, -1, 0.72F * (1.0F - this.field06290));
            float local3002_20 = local3002_12 * (float)Math.sqrt(Math.abs(local3002_11));
            float local3002_21 = height * 0.019F;
            boolean local3002_22 = RaycastUtils.method3289(mc.player, livingEntity);
            float[] floatArray = new float[]{0.0F, 0.18F, 0.52F, 1.0F};
            int[] intArray = new int[]{125, 72, 24, 0};
            int local3002_25 = 0;
            BufferAllocator bufferAllocator = this.method1901(0, 16384 * VertexFormats.POSITION_COLOR.getVertexSize());
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int index = 0; index < floatArray.length - 1; index++) {
               local3002_25 += this.method1907(
                  bufferBuilder,
                  local3002_15,
                  local3002_17,
                  local3002_16,
                  livingEntity,
                  height,
                  local3002_12,
                  MathHelper.lerp(floatArray[index], local3002_13, local3002_14),
                  MathHelper.lerp(floatArray[index + 1], local3002_13, local3002_14),
                  0.0F,
                  0.0F,
                  Colors.method3728(local3002_18, (int)(intArray[index] * local3002_20)),
                  Colors.method3728(local3002_18, (int)(intArray[index + 1] * local3002_20))
               );
            }

            local3002_25 += this.method1907(
               bufferBuilder,
               local3002_15,
               local3002_17,
               local3002_16,
               livingEntity,
               height,
               local3002_12,
               Math.max(0.0F, local3002_13 - 0.075F),
               local3002_13,
               0.025F,
               0.035F,
               Colors.method3728(local3002_18, 0),
               Colors.method3728(local3002_18, (int)(72.0F * local3002_12))
            );
            local3002_25 += this.method1907(
               bufferBuilder,
               local3002_15,
               local3002_17,
               local3002_16,
               livingEntity,
               height,
               local3002_12,
               local3002_13,
               Math.min(height, local3002_13 + 0.075F),
               0.035F,
               0.025F,
               Colors.method3728(local3002_18, (int)(72.0F * local3002_12)),
               Colors.method3728(local3002_18, 0)
            );
            local3002_25 += this.method1907(
               bufferBuilder,
               local3002_15,
               local3002_17,
               local3002_16,
               livingEntity,
               height,
               local3002_12,
               local3002_13 - local3002_21,
               local3002_13 + local3002_21,
               0.045F,
               0.045F,
               Colors.method3728(local3002_19, (int)(245.0F * local3002_12)),
               Colors.method3728(local3002_19, (int)(245.0F * local3002_12))
            );

            for (int index2 = 1; index2 <= 5; index2++) {
               local3002_25 += this.method1907(
                  bufferBuilder,
                  local3002_15,
                  local3002_17,
                  local3002_16,
                  livingEntity,
                  height,
                  local3002_12,
                  MathHelper.lerp(index2 / 6.0F, local3002_13, local3002_14) - local3002_21,
                  MathHelper.lerp(index2 / 6.0F, local3002_13, local3002_14) + local3002_21,
                  0.012F,
                  0.012F,
                  Colors.method3728(local3002_18, (int)((1.0F - index2 / 6.0F) * 34.0F * local3002_20)),
                  Colors.method3728(local3002_18, (int)((1.0F - index2 / 6.0F) * 34.0F * local3002_20))
               );
            }

            if (local3002_25 > 0) {
               try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                  this.method1933(matrixStack, null, builtBuffer, local3002_25, local3002_22, false);
               }
            }
         } else if (this.field06280.isSelected("Второй")) {
            float local3002_8 = (float)(System.currentTimeMillis() % 1750L);
            float local3002_9 = local3002_8 > 875.0F ? -1.0F : 1.0F;
            float local3002_10 = local3002_9 < 0.0F ? local3002_8 / 875.0F - 1.0F : 1.0F - local3002_8 / 875.0F;
            float local3002_11 = local3002_10 < 0.5F ? 2.0F * local3002_10 * local3002_10 : 1.0F - (float)Math.pow(-2.0F * local3002_10 + 2.0F, 2.0) / 2.0F;
            float local3002_12 = livingEntity.getHeight() + 0.15F;
            float local3002_13 = livingEntity.getWidth() * 0.8F;
            float local3002_14 = livingEntity.getHeight() * 0.006F;
            float local3002_15 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
            );
            float local3002_16 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
            );
            float local3002_17 = (float)(
                  MathHelper.lerp(local3002_3, livingEntity.lastRenderY, livingEntity.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
               )
               + local3002_12 * local3002_11;
            float local3002_18 = local3002_12 * 0.8F * Math.min(local3002_11, 1.0F - local3002_11) * local3002_9;
            int local3002_19 = Colors.method3727(this.method1914(0.0F, this.field06290), local3002_4);
            int local3002_20 = Colors.method3727(local3002_19, 0.55F);
            int local3002_21 = Colors.method3728(local3002_19, 0);
            boolean local3002_22 = RaycastUtils.method3289(mc.player, livingEntity);
            int local3002_23 = 0;
            BufferAllocator bufferAllocator2 = this.method1901(0, 8192 * VertexFormats.POSITION_COLOR.getVertexSize());
            BufferBuilder bufferBuilder2 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (int index3 = 0; index3 < 360; index3 += 3) {
               float local3002_27 = MathHelper.cos((float)Math.toRadians(index3));
               float local3002_28 = MathHelper.sin((float)Math.toRadians(index3));
               float local3002_29 = MathHelper.cos((float)Math.toRadians(index3 + 3));
               float local3002_30 = MathHelper.sin((float)Math.toRadians(index3 + 3));
               bufferBuilder2.vertex(
                     local3002_15 + local3002_27 * (local3002_13 - local3002_14), local3002_17, local3002_16 + local3002_28 * (local3002_13 - local3002_14)
                  )
                  .color(local3002_19);
               bufferBuilder2.vertex(
                     local3002_15 + local3002_29 * (local3002_13 - local3002_14), local3002_17, local3002_16 + local3002_30 * (local3002_13 - local3002_14)
                  )
                  .color(local3002_19);
               bufferBuilder2.vertex(
                     local3002_15 + local3002_29 * (local3002_13 + local3002_14), local3002_17, local3002_16 + local3002_30 * (local3002_13 + local3002_14)
                  )
                  .color(local3002_19);
               bufferBuilder2.vertex(
                     local3002_15 + local3002_27 * (local3002_13 + local3002_14), local3002_17, local3002_16 + local3002_28 * (local3002_13 + local3002_14)
                  )
                  .color(local3002_19);
               local3002_23++;
               bufferBuilder2.vertex(local3002_15 + local3002_27 * local3002_13, local3002_17, local3002_16 + local3002_28 * local3002_13).color(local3002_20);
               bufferBuilder2.vertex(local3002_15 + local3002_29 * local3002_13, local3002_17, local3002_16 + local3002_30 * local3002_13).color(local3002_20);
               bufferBuilder2.vertex(local3002_15 + local3002_29 * local3002_13, local3002_17 + local3002_18, local3002_16 + local3002_30 * local3002_13)
                  .color(local3002_21);
               bufferBuilder2.vertex(local3002_15 + local3002_27 * local3002_13, local3002_17 + local3002_18, local3002_16 + local3002_28 * local3002_13)
                  .color(local3002_21);
               local3002_23++;
            }

            if (local3002_23 > 0) {
               try (BuiltBuffer builtBuffer2 = bufferBuilder2.end()) {
                  this.method1933(matrixStack, null, builtBuffer2, local3002_23, local3002_22, false);
               }
            }
         } else {
            float local3002_8 = this.field06288 * 2.4F;
            float local3002_9 = 0.5F + 0.5F * MathHelper.sin(local3002_8);
            float local3002_10 = -MathHelper.cos(local3002_8);
            float local3002_11 = this.field06288 * 1.6F;
            float local3002_12 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
            );
            float local3002_13 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
            );
            float local3002_14 = (float)(
               MathHelper.lerp(local3002_3, livingEntity.lastRenderY, livingEntity.getY())
                  + (livingEntity.getHeight() + 0.1F) * local3002_9
                  - mc.gameRenderer.getCamera().getCameraPos().y
            );
            float local3002_15 = livingEntity.getWidth() * 0.9F + 1.0F * (1.0F - local3002_4);
            float local3002_16 = livingEntity.getHeight() * 0.006F;
            int local3002_17 = Colors.method3727(this.method1914(0.0F, this.field06290), local3002_4);
            int local3002_18 = Colors.method3727(local3002_17, 0.28F);
            int local3002_19 = Colors.method3727(local3002_17, 0.03F);
            int local3002_20 = Colors.method3727(local3002_17, 0.12F);
            boolean local3002_21 = RaycastUtils.method3289(mc.player, livingEntity);
            int local3002_22 = 0;
            int local3002_23 = 0;
            BufferAllocator bufferAllocator3 = this.method1901(0, 8192 * VertexFormats.POSITION_COLOR.getVertexSize());
            BufferAllocator bufferAllocator4 = this.method1901(1, 48 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
            BufferBuilder bufferBuilder3 = new BufferBuilder(bufferAllocator3, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            BufferBuilder bufferBuilder4 = new BufferBuilder(bufferAllocator4, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            Vector3f vector3f = new Vector3f();

            for (int index4 = 0; index4 < 360; index4 += 3) {
               float local3002_30 = MathHelper.cos((float)Math.toRadians(index4));
               float local3002_31 = MathHelper.sin((float)Math.toRadians(index4));
               float local3002_32 = MathHelper.cos((float)Math.toRadians(index4 + 3));
               float local3002_33 = MathHelper.sin((float)Math.toRadians(index4 + 3));
               bufferBuilder3.vertex(
                     local3002_12 + local3002_30 * (local3002_15 - local3002_16), local3002_14, local3002_13 + local3002_31 * (local3002_15 - local3002_16)
                  )
                  .color(local3002_17);
               bufferBuilder3.vertex(
                     local3002_12 + local3002_32 * (local3002_15 - local3002_16), local3002_14, local3002_13 + local3002_33 * (local3002_15 - local3002_16)
                  )
                  .color(local3002_17);
               bufferBuilder3.vertex(
                     local3002_12 + local3002_32 * (local3002_15 + local3002_16), local3002_14, local3002_13 + local3002_33 * (local3002_15 + local3002_16)
                  )
                  .color(local3002_17);
               bufferBuilder3.vertex(
                     local3002_12 + local3002_30 * (local3002_15 + local3002_16), local3002_14, local3002_13 + local3002_31 * (local3002_15 + local3002_16)
                  )
                  .color(local3002_17);
               local3002_22++;
               float local3002_34 = (0.45F + 0.12F * MathHelper.sin((float)Math.toRadians(index4) * 3.0F - local3002_11)) * local3002_10;
               float local3002_35 = (0.45F + 0.12F * MathHelper.sin((float)Math.toRadians(index4 + 3) * 3.0F - local3002_11)) * local3002_10;
               bufferBuilder3.vertex(local3002_12 + local3002_30 * local3002_15, local3002_14, local3002_13 + local3002_31 * local3002_15).color(local3002_18);
               bufferBuilder3.vertex(local3002_12 + local3002_32 * local3002_15, local3002_14, local3002_13 + local3002_33 * local3002_15).color(local3002_18);
               bufferBuilder3.vertex(local3002_12 + local3002_32 * local3002_15, local3002_14 + local3002_35, local3002_13 + local3002_33 * local3002_15)
                  .color(local3002_19);
               bufferBuilder3.vertex(local3002_12 + local3002_30 * local3002_15, local3002_14 + local3002_34, local3002_13 + local3002_31 * local3002_15)
                  .color(local3002_19);
               local3002_22++;
            }

            for (int index5 = 0; index5 < 360; index5 += 7) {
               this.method1931(
                  bufferBuilder4,
                  mc.gameRenderer.getCamera().getRotation(),
                  vector3f,
                  local3002_12 + MathHelper.cos((float)Math.toRadians(index5)) * local3002_15,
                  local3002_14,
                  local3002_13 + MathHelper.sin((float)Math.toRadians(index5)) * local3002_15,
                  local3002_15 * 0.2F,
                  0.0F,
                  local3002_20
               );
               local3002_23++;
            }

            if (local3002_23 > 0) {
               try (BuiltBuffer builtBuffer3 = bufferBuilder4.end()) {
                  this.method1933(matrixStack, "textures/particles/glow.png", builtBuffer3, local3002_23, local3002_21, false);
               }
            }

            if (local3002_22 > 0) {
               try (BuiltBuffer builtBuffer4 = bufferBuilder3.end()) {
                  this.method1933(matrixStack, null, builtBuffer4, local3002_22, local3002_21, false);
               }
            }
         }
      }
   }

   private int method1927(
      float local3003_1,
      float local3003_2,
      float local3003_3,
      float local3003_4,
      float local3003_5,
      float local3003_6,
      int local3003_7,
      int local3003_8,
      float local3003_9,
      boolean local3003_10
   ) {
      this.field06298.clear();
      this.field06297 = 0;
      this.method1929(local3003_1, local3003_2, local3003_3);
      this.method1916(local3003_1, local3003_2, local3003_3, local3003_4, local3003_5, local3003_6, 4, local3003_7, 0, local3003_8, local3003_9);
      int local3003_11 = this.method1908(1.0F, local3003_10);

      for (int index = 0; index < this.field06298.size(); index++) {
         float[] floatArray = this.field06298.get(index);
         this.field06297 = 0;
         this.method1929(floatArray[0], floatArray[1], floatArray[2]);
         this.method1916(
            floatArray[0],
            floatArray[1],
            floatArray[2],
            floatArray[3],
            floatArray[4],
            floatArray[5],
            (int)floatArray[6],
            (int)floatArray[7],
            (int)floatArray[8],
            local3003_8,
            local3003_9
         );
         local3003_11 += this.method1908(floatArray[9], true);
      }

      this.field06298.clear();
      return local3003_11;
   }

   private float method1928(LivingEntity livingEntity, float local3004_2, float local3004_3) {
      return (
            MathHelper.clamp(local3004_2, 0.0F, 1.0F) < 0.48F
               ? MathHelper.lerp(
                  this.method1903(0.24F, 0.46F, MathHelper.clamp(local3004_2, 0.0F, 1.0F)),
                  (livingEntity.getWidth() * 0.5F + 0.1F) * 1.02F,
                  (livingEntity.getWidth() * 0.5F + 0.1F) * 1.46F
               )
               : (
                  MathHelper.clamp(local3004_2, 0.0F, 1.0F) > 0.68F
                     ? MathHelper.lerp(
                        this.method1903(0.68F, 0.84F, MathHelper.clamp(local3004_2, 0.0F, 1.0F)),
                        (livingEntity.getWidth() * 0.5F + 0.1F) * 1.46F,
                        (livingEntity.getWidth() * 0.5F + 0.1F) * 1.16F
                     )
                     : (livingEntity.getWidth() * 0.5F + 0.1F) * 1.46F
               )
         )
         * (0.86F + 0.14F * this.method1903(0.0F, 1.0F, local3004_3));
   }

   private void method1929(float local3005_1, float local3005_2, float local3005_3) {
      if (this.field06297 * 3 + 3 > this.field06294.length) {
         this.field06294 = Arrays.copyOf(this.field06294, this.field06294.length * 2);
         this.field06295 = Arrays.copyOf(this.field06295, this.field06295.length * 2);
         this.field06296 = Arrays.copyOf(this.field06296, this.field06296.length * 2);
      }

      this.field06294[this.field06297 * 3] = local3005_1;
      this.field06294[this.field06297 * 3 + 1] = local3005_2;
      this.field06294[this.field06297 * 3 + 2] = local3005_3;
      this.field06297++;
   }

   private void method1931(
      BufferBuilder bufferBuilder,
      Quaternionf quaternionf,
      Vector3f vector3f,
      double local3007_4,
      double local3007_6,
      double local3007_8,
      float local3007_10,
      float local3007_11,
      int local3007_12
   ) {
      quaternionf.transform(vector3f.set(-local3007_10, -local3007_10, -local3007_11));
      bufferBuilder.vertex((float)(local3007_4 + vector3f.x), (float)(local3007_6 + vector3f.y), (float)(local3007_8 + vector3f.z))
         .texture(0.0F, 0.0F)
         .color(local3007_12);
      quaternionf.transform(vector3f.set(-local3007_10, local3007_10, -local3007_11));
      bufferBuilder.vertex((float)(local3007_4 + vector3f.x), (float)(local3007_6 + vector3f.y), (float)(local3007_8 + vector3f.z))
         .texture(0.0F, 1.0F)
         .color(local3007_12);
      quaternionf.transform(vector3f.set(local3007_10, local3007_10, -local3007_11));
      bufferBuilder.vertex((float)(local3007_4 + vector3f.x), (float)(local3007_6 + vector3f.y), (float)(local3007_8 + vector3f.z))
         .texture(1.0F, 1.0F)
         .color(local3007_12);
      quaternionf.transform(vector3f.set(local3007_10, -local3007_10, -local3007_11));
      bufferBuilder.vertex((float)(local3007_4 + vector3f.x), (float)(local3007_6 + vector3f.y), (float)(local3007_8 + vector3f.z))
         .texture(1.0F, 0.0F)
         .color(local3007_12);
   }

   private void method1933(MatrixStack matrixStack, String local3010_2, BuiltBuffer builtBuffer, int local3010_4, boolean local3010_5, boolean local3010_6) {
      if (mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
         Identifier identifier = local3010_2 == null
            ? null
            : this.field06322.computeIfAbsent(local3010_2, local3015_0 -> Identifier.of("deluxe", local3015_0));
         if (identifier == null || mc.getTextureManager().getTexture(identifier).getGlTextureView() != null) {
            int local3010_8 = this.field06320++ % this.field06319.length;
            GpuBuffer gpuBuffer = this.field06319[local3010_8];
            if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= builtBuffer.getBuffer().remaining()) {
               RenderSystem.getDevice()
                  .createCommandEncoder()
                  .writeToBuffer(gpuBuffer.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
            } else {
               if (gpuBuffer != null && !gpuBuffer.isClosed()) {
                  gpuBuffer.close();
               }

               gpuBuffer = this.field06319[local3010_8] = RenderSystem.getDevice()
                  .createBuffer(() -> "deluxe_target_quads_vertices", 40, builtBuffer.getBuffer());
            }

            GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(matrixStack.peek().getPositionMatrix(), field06272, field06273, field06274);

            try (RenderPass renderPass = RenderSystem.getDevice()
                  .createCommandEncoder()
                  .createRenderPass(
                     () -> "deluxe_target_quads",
                     mc.getFramebuffer().getColorAttachmentView(),
                     OptionalInt.empty(),
                     local3010_5 ? DepthSnapshot.method4228() : null,
                     OptionalDouble.empty()
                  )) {
               renderPass.setPipeline(local3010_2 == null ? (local3010_5 ? field06270 : field06269) : (local3010_5 ? field06268 : field06267));
               if (identifier != null) {
                  renderPass.bindTexture(
                     "Sampler0",
                     mc.getTextureManager().getTexture(identifier).getGlTextureView(),
                     local3010_6 ? RenderSystem.getSamplerCache().getRepeated(FilterMode.LINEAR) : RenderSystem.getSamplerCache().get(FilterMode.LINEAR)
                  );
               }

               renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
               renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
               renderPass.setVertexBuffer(0, gpuBuffer);
               renderPass.setIndexBuffer(
                  RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local3010_4 * 6),
                  RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
               );
               renderPass.drawIndexed(0, 0, local3010_4 * 6, 1);
            }
         }
      }
   }

   private void method1934(MatrixStack matrixStack, LivingEntity livingEntity, float local3011_3, float local3011_4, float local3011_5) {
      this.field06309 = MathHelper.lerp(0.22F, this.field06309, livingEntity.hurtTime > 0 ? 0.0F : 1.0F);
      float local3011_6 = (2.0F - local3011_4 + 0.3F * this.field06309) / 2.0F;
      Quaternionf quaternionf = new Quaternionf(mc.gameRenderer.getCamera().getRotation())
         .rotateZ((float)Math.toRadians((float)(System.currentTimeMillis() % 3000L) / 3000.0F * 360.0F));
      matrixStack.push();
      matrixStack.translate(
         livingEntity.getLerpedPos(local3011_3).x - mc.gameRenderer.getCamera().getCameraPos().x,
         livingEntity.getLerpedPos(local3011_3).y + livingEntity.getHeight() * 0.5F - mc.gameRenderer.getCamera().getCameraPos().y,
         livingEntity.getLerpedPos(local3011_3).z - mc.gameRenderer.getCamera().getCameraPos().z
      );
      BufferAllocator bufferAllocator = this.method1901(1, 8 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
      BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
      Vector3f vector3f = new Vector3f();

      for (float local3011_14 : new float[]{1.0F, 0.5F}) {
         quaternionf.transform(vector3f.set(-local3011_6, -local3011_6, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(0.0F, 0.0F)
            .color(Colors.method3727(this.method1914(270.0F, local3011_5), local3011_4 * local3011_14));
         quaternionf.transform(vector3f.set(-local3011_6, local3011_6, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(0.0F, 1.0F)
            .color(Colors.method3727(this.method1914(0.0F, local3011_5), local3011_4 * local3011_14));
         quaternionf.transform(vector3f.set(local3011_6, local3011_6, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(1.0F, 1.0F)
            .color(Colors.method3727(this.method1914(180.0F, local3011_5), local3011_4 * local3011_14));
         quaternionf.transform(vector3f.set(local3011_6, -local3011_6, 0.0F));
         bufferBuilder.vertex(vector3f.x, vector3f.y, vector3f.z)
            .texture(1.0F, 0.0F)
            .color(Colors.method3727(this.method1914(90.0F, local3011_5), local3011_4 * local3011_14));
      }

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.method1933(matrixStack, "textures/particles/target.png", builtBuffer, 2, false, false);
      }

      matrixStack.pop();
   }

   @Override
   protected void onDisable() {
      this.field06284 = null;
      this.field06310 = 0.0F;
      this.field06314 = 0.0F;
      this.field06313.method3870(0.0);
      this.field06285 = 0.0F;
      this.field06286 = System.currentTimeMillis();
      this.field06287 = 0.0F;
      this.field06288 = 0.0F;
      this.field06289 = System.currentTimeMillis();
      this.field06290 = 0.0F;
      this.field06291 = null;
      this.field06292 = 0L;
      this.field06293 = 0L;
      this.field06309 = 0.0F;
      Arrays.fill(this.field06316, 0.0F);
      this.field06317 = System.currentTimeMillis();
      if (this.field06318 != null && !this.field06318.isClosed()) {
         this.field06318.close();
      }

      this.field06318 = null;

      for (int index = 0; index < this.field06319.length; index++) {
         if (this.field06319[index] != null && !this.field06319[index].isClosed()) {
            this.field06319[index].close();
         }

         this.field06319[index] = null;
      }

      this.field06320 = 0;

      for (int index2 = 0; index2 < this.field06321.length; index2++) {
         if (this.field06321[index2] != null) {
            this.field06321[index2].close();
         }

         this.field06321[index2] = null;
      }
   }

   public static TargetEspModule method1936() {
      if (targetEspModule == null) {
         targetEspModule = new TargetEspModule();
      }

      return targetEspModule;
   }

   private void method1938(MatrixStack matrixStack, LivingEntity livingEntity, float local3016_3, float local3016_4, float local3016_5) {
      long local3016_6 = System.currentTimeMillis();
      float local3016_8 = (float)Math.min(local3016_6 - this.field06317, 100L) / 1000.0F;
      this.field06317 = local3016_6;
      float local3016_9 = livingEntity instanceof PlayerEntity ? EntityUtils.method2790(livingEntity) : livingEntity.getHealth();
      int local3016_10 = local3016_9 < 8.0F ? 2 : (local3016_9 < 13.0F ? 1 : 0);

      for (int index = 0; index < this.field06316.length; index++) {
         this.field06316[index] = MathHelper.clamp(
            this.field06316[index] + (index == local3016_10 ? local3016_8 : -local3016_8) / 0.25F, 0.0F, 1.0F
         );
      }

      float local3016_11 = 0.42F;
      double local3016_12 = MathHelper.lerp(local3016_3, livingEntity.lastRenderX, livingEntity.getX()) - mc.gameRenderer.getCamera().getCameraPos().x;
      double local3016_14 = MathHelper.lerp(local3016_3, livingEntity.lastRenderY, livingEntity.getY())
         + livingEntity.getHeight() * 0.55F
         - mc.gameRenderer.getCamera().getCameraPos().y;
      double local3016_16 = MathHelper.lerp(local3016_3, livingEntity.lastRenderZ, livingEntity.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z;
      Quaternionf rotation = mc.gameRenderer.getCamera().getRotation();
      int local3016_19 = Colors.method3727(Colors.method3723(ThemeColors.field13669, -49088, local3016_5), local3016_4);
      Vector3f vector3f = new Vector3f();
      BufferBuilder bufferBuilder = new BufferBuilder(
         this.method1901(1, 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR
      );
      this.method1931(
         bufferBuilder,
         rotation,
         vector3f,
         local3016_12,
         local3016_14,
         local3016_16,
         local3016_11 * 1.6F,
         0.0F,
         Colors.method3727(local3016_19, 0.22F)
      );

      try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
         this.method1933(matrixStack, "textures/particles/glow.png", builtBuffer, 1, false, false);
      }

      String[] stringArray = new String[]{"textures/img.png", "textures/img_1.png", "textures/img_2.png"};

      for (int index2 = 0; index2 < stringArray.length; index2++) {
         if (!(this.field06316[index2] < 0.003921569F)) {
            int local3016_24 = Colors.method3727(local3016_19, this.field06316[index2]);
            BufferBuilder bufferBuilder2 = new BufferBuilder(
               this.method1901(1, 4 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize()), DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR
            );
            rotation.transform(vector3f.set(-local3016_11, -local3016_11, 0.0F));
            bufferBuilder2.vertex((float)(local3016_12 + vector3f.x), (float)(local3016_14 + vector3f.y), (float)(local3016_16 + vector3f.z))
               .texture(0.0F, 1.0F)
               .color(local3016_24);
            rotation.transform(vector3f.set(-local3016_11, local3016_11, 0.0F));
            bufferBuilder2.vertex((float)(local3016_12 + vector3f.x), (float)(local3016_14 + vector3f.y), (float)(local3016_16 + vector3f.z))
               .texture(0.0F, 0.0F)
               .color(local3016_24);
            rotation.transform(vector3f.set(local3016_11, local3016_11, 0.0F));
            bufferBuilder2.vertex((float)(local3016_12 + vector3f.x), (float)(local3016_14 + vector3f.y), (float)(local3016_16 + vector3f.z))
               .texture(1.0F, 0.0F)
               .color(local3016_24);
            rotation.transform(vector3f.set(local3016_11, -local3016_11, 0.0F));
            bufferBuilder2.vertex((float)(local3016_12 + vector3f.x), (float)(local3016_14 + vector3f.y), (float)(local3016_16 + vector3f.z))
               .texture(1.0F, 1.0F)
               .color(local3016_24);

            try (BuiltBuffer builtBuffer2 = bufferBuilder2.end()) {
               this.method1933(matrixStack, stringArray[index2], builtBuffer2, 1, false, false);
            }
         }
      }
   }

   private float method1939(int local3017_1, int local3017_2, int local3017_3) {
      int local3017_4 = local3017_1 * 668265261 + local3017_2 * 374761393 + local3017_3 * -1640531535;
      local3017_4 ^= local3017_4 >>> 15;
      local3017_4 *= -2048144789;
      local3017_4 ^= local3017_4 >>> 13;
      return (local3017_4 >>> 8) / 8388608.0F - 1.0F;
   }

   private TargetEspModule() {
      this.field06278 = this.addSetting(
         new ModeSetting("Мод", "Какой эффект рисовать на цели.", "Призраки", "Призраки", "Молнии", "Круг", "Кристаллы", "Ромб", "Цепи", "Череп")
      );
      this.field06279 = this.addSetting(new ModeSetting("Стиль призраков", "Какими рисовать призраков на цели.", "Второй", "Второй"));
      this.field06279.withVisibility(() -> false);
      this.field06280 = this.addSetting(new ModeSetting("Стиль круга", "Каким рисовать круг на цели.", "Второй", "Второй"));
      this.field06280.withVisibility(() -> false);
      this.field06281 = this.addSetting(
         new BooleanSetting("Трезубец", "Тянет цепь от руки к летящим трезубцу и стрелам.", true).withVisibility(() -> this.field06278.isSelected("Цепи"))
      );
      this.field06282 = this.addSetting(
         new BooleanSetting("Снаряды", "Тянет молнию от руки к летящим трезубцу и стрелам.", true).withVisibility(() -> this.field06278.isSelected("Молнии"))
      );
   }

   private void method1940(MatrixStack matrixStack, float local3019_2) {
      long local3019_3 = System.currentTimeMillis();
      float local3019_5 = (float)Math.min(local3019_3 - this.field06286, 100L) / 1000.0F;
      this.field06285 += local3019_5;
      this.field06286 = local3019_3;
      this.field06308.keySet().removeIf(local2998_0 -> mc.world.getEntityById(local2998_0) == null);
      int local3019_6 = (int)this.field06285;
      float local3019_7 = this.field06285 - local3019_6;
      float local3019_8 = this.field06285 * 1.7F - (int)(this.field06285 * 1.7F);
      float local3019_9 = (float)(
         MathHelper.lerp(local3019_2, mc.player.lastRenderX, mc.player.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
      );
      float local3019_10 = (float)(
         MathHelper.lerp(local3019_2, mc.player.lastRenderY, mc.player.getY())
            + mc.player.getStandingEyeHeight() * 0.75F
            - mc.gameRenderer.getCamera().getCameraPos().y
      );
      float local3019_11 = (float)(
         MathHelper.lerp(local3019_2, mc.player.lastRenderZ, mc.player.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
      );
      this.field06303 = 0.0037F;
      this.field06305 = 0.047F;
      this.field06301 = mc.gameRenderer.getCamera().getRotation();

      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof PersistentProjectileEntity entity2 && entity2.getOwner() == mc.player) {
            boolean local3019_15 = !entity2.isOnGround() && entity2.getVelocity().lengthSquared() > 1.0E-5;
            float[] floatArray = this.field06308.computeIfAbsent(entity2.getId(), local3009_0 -> new float[1]);
            floatArray[0] = MathHelper.clamp(floatArray[0] + (local3019_15 ? local3019_5 / 0.15F : -local3019_5 / 0.3F), 0.0F, 1.0F);
            if (!(floatArray[0] < 0.003921569F)) {
               float local3019_17 = (float)(
                  MathHelper.lerp(local3019_2, entity2.lastRenderX, entity2.getX()) - mc.gameRenderer.getCamera().getCameraPos().x
               );
               float local3019_18 = (float)(
                  MathHelper.lerp(local3019_2, entity2.lastRenderY, entity2.getY()) - mc.gameRenderer.getCamera().getCameraPos().y
               );
               float local3019_19 = (float)(
                  MathHelper.lerp(local3019_2, entity2.lastRenderZ, entity2.getZ()) - mc.gameRenderer.getCamera().getCameraPos().z
               );
               float local3019_20 = local3019_17 - local3019_9;
               float local3019_21 = local3019_18 - local3019_10;
               float local3019_22 = local3019_19 - local3019_11;
               float local3019_23 = MathHelper.sqrt(local3019_20 * local3019_20 + local3019_21 * local3019_21 + local3019_22 * local3019_22);
               if (!(local3019_23 < 1.0F)) {
                  float local3019_24 = local3019_20 / local3019_23;
                  float local3019_25 = local3019_21 / local3019_23;
                  float local3019_26 = local3019_22 / local3019_23;
                  float local3019_27 = Math.abs(local3019_25) < 0.9F ? 0.0F : 1.0F;
                  float local3019_28 = Math.abs(local3019_25) < 0.9F ? 1.0F : 0.0F;
                  float local3019_29 = -local3019_26 * local3019_28;
                  float local3019_30 = local3019_26 * local3019_27;
                  float local3019_31 = local3019_24 * local3019_28 - local3019_25 * local3019_27;
                  float local3019_32 = MathHelper.sqrt(local3019_29 * local3019_29 + local3019_30 * local3019_30 + local3019_31 * local3019_31);
                  if (local3019_32 < 1.0E-5F) {
                     local3019_32 = 1.0F;
                  }

                  local3019_29 /= local3019_32;
                  local3019_30 /= local3019_32;
                  local3019_31 /= local3019_32;
                  float local3019_33 = local3019_25 * local3019_31 - local3019_26 * local3019_30;
                  float local3019_34 = local3019_26 * local3019_29 - local3019_24 * local3019_31;
                  float local3019_35 = local3019_24 * local3019_30 - local3019_25 * local3019_29;
                  float local3019_36 = MathHelper.clamp(0.8F + 0.2F * this.method1920(entity2.getId(), 900, local3019_6, local3019_7), 0.0F, 1.0F);
                  int local3019_37 = MathHelper.clamp((int)Math.ceil(local3019_23 / 2.2F), 2, 24);
                  int local3019_38 = 0;
                  BufferAllocator bufferAllocator = this.method1901(0, 2048 * VertexFormats.POSITION_COLOR.getVertexSize());
                  BufferAllocator bufferAllocator2 = this.method1901(1, 2048 * VertexFormats.POSITION_TEXTURE_COLOR.getVertexSize());
                  this.field06299 = new BufferBuilder(bufferAllocator, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                  this.field06300 = new BufferBuilder(bufferAllocator2, DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
                  float local3019_42 = local3019_9;
                  float local3019_43 = local3019_10;
                  float local3019_44 = local3019_11;

                  for (int index = 1; index <= local3019_37; index++) {
                     float local3019_46 = (float)index / local3019_37;
                     float local3019_47 = MathHelper.sin((float) Math.PI * local3019_46);
                     float local3019_48 = local3019_46 * 7.0F + this.field06285 * 4.0F;
                     float local3019_49 = Math.min(0.3F, local3019_23 * 0.06F) * local3019_47;
                     float local3019_50 = local3019_9
                        + local3019_20 * local3019_46
                        + (local3019_29 * MathHelper.sin(local3019_48) + local3019_33 * MathHelper.cos(local3019_48)) * local3019_49;
                     float local3019_51 = local3019_10
                        + local3019_21 * local3019_46
                        + (local3019_30 * MathHelper.sin(local3019_48) + local3019_34 * MathHelper.cos(local3019_48)) * local3019_49
                        - Math.min(0.6F, local3019_23 * 0.035F) * local3019_47;
                     float local3019_52 = local3019_11
                        + local3019_22 * local3019_46
                        + (local3019_31 * MathHelper.sin(local3019_48) + local3019_35 * MathHelper.cos(local3019_48)) * local3019_49;
                     float local3019_53 = Math.abs((index - 0.5F) / local3019_37 - local3019_8);
                     local3019_53 = Math.min(local3019_53, 1.0F - local3019_53);
                     float local3019_54 = Math.max(0.0F, 1.0F - local3019_53 / 0.22F);
                     this.field06306 = Colors.method3727(
                        Colors.method3728(Colors.method3723(UiTheme.field09686, -1, 0.4F + 0.6F * local3019_54), 255),
                        (0.5F + 0.5F * local3019_54 * local3019_54) * local3019_36 * floatArray[0]
                     );
                     this.field06307 = Colors.method3727(this.field06306, 0.13F);
                     local3019_38 += this.method1927(
                        local3019_42,
                        local3019_43,
                        local3019_44,
                        local3019_50,
                        local3019_51,
                        local3019_52,
                        (entity2.getId() * 131 + index) * 17 | 1,
                        local3019_6,
                        local3019_7,
                        index == local3019_37
                     );
                     local3019_42 = local3019_50;
                     local3019_43 = local3019_51;
                     local3019_44 = local3019_52;
                  }

                  int local3019_39 = local3019_38;
                  this.method1931(
                     this.field06300,
                     this.field06301,
                     this.field06302,
                     local3019_17,
                     local3019_18,
                     local3019_19,
                     0.067F + 0.02F * MathHelper.sin(this.field06285 * 9.0F + entity2.getId()),
                     0.0F,
                     Colors.method3727(
                        Colors.method3728(Colors.method3723(UiTheme.field09686, -1, 0.7F), 255),
                        0.45F * local3019_36 * floatArray[0]
                     )
                  );
                  local3019_39++;
                  boolean local3019_45 = RaycastUtils.method3289(mc.player, entity2);

                  try (BuiltBuffer builtBuffer = this.field06300.end()) {
                     this.method1933(matrixStack, "textures/particles/glow.png", builtBuffer, local3019_39, local3019_45, false);
                  }

                  if (local3019_38 > 0) {
                     try (BuiltBuffer builtBuffer2 = this.field06299.end()) {
                        this.method1933(matrixStack, null, builtBuffer2, local3019_38, local3019_45, false);
                     }
                  }
               }
            }
         }
      }
   }

   public record Nested0418(int primaryColor, int secondaryColor, float phase, float alpha, float hit, float glow) {
   }
}
