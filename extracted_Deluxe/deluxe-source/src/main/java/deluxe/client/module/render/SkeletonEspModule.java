package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.DeluxeClient;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.ui.UiTheme;
import deluxe.client.setting.ColorSetting;
import deluxe.client.event.WorldRenderEvent;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.BufferAllocator.CloseableBuffer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

@ModuleInfo(name = "SkeletonEsp", description = "Рисует скелет игроков по позе их модели сквозь стены.", category = ModuleCategory.RENDER)
public final class SkeletonEspModule extends Module {
   private static SkeletonEspModule skeletonEspModule;
   private static final Vector4f field08443 = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
   private static final Vector3f field08444 = new Vector3f();
   private static final Matrix4f field08445 = new Matrix4f();
   private static final Matrix4f field08446 = new Matrix4f();
   private static final RenderPipeline field08447 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/skeleton_bones"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   public BooleanSetting booleanSetting;
   public ColorSetting field08449;
   public ColorSetting field08450;
   private GpuBuffer field08451;
   private BufferAllocator field08452;
   private final ArrayList<AbstractClientPlayerEntity> field08453 = new ArrayList<>();
   private final Vector3f field08454 = new Vector3f();
   private final Vector3f field08455 = new Vector3f();
   private final EventListener<WorldRenderEvent> field08456 = worldRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         ArrayList<AbstractClientPlayerEntity> local3893_2 = this.field08453;
         local3893_2.clear();

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity
               && (mc.options.getPerspective() != Perspective.FIRST_PERSON || abstractClientPlayerEntity != mc.player)) {
               local3893_2.add(abstractClientPlayerEntity);
            }
         }

         if (!local3893_2.isEmpty()) {
            if (this.field08452 == null) {
               this.field08452 = new BufferAllocator(520 * VertexFormats.POSITION_COLOR.getVertexSize());
            }

            CloseableBuffer allocated = this.field08452.getAllocated();
            if (allocated != null) {
               allocated.close();
            }

            BufferBuilder bufferBuilder = new BufferBuilder(this.field08452, DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int local3893_5 = 0;

            for (AbstractClientPlayerEntity abstractClientPlayerEntity2 : local3893_2) {
               local3893_5 += this.method2479(
                  worldRenderEvent.method0647(),
                  bufferBuilder,
                  abstractClientPlayerEntity2,
                  worldRenderEvent.method0646(),
                  FriendStore.method3431(abstractClientPlayerEntity2.getGameProfile().name())
                     ? this.field08450.getValue()
                     : (this.booleanSetting.isChecked() ? this.field08449.getValue() : UiTheme.field09686)
               );
            }

            this.method2477(bufferBuilder, local3893_5);
            local3893_2.clear();
         }
      }
   };

   @Override
   protected void onDisable() {
      if (this.field08451 != null && !this.field08451.isClosed()) {
         this.field08451.close();
      }

      this.field08451 = null;
      if (this.field08452 != null) {
         this.field08452.close();
         this.field08452 = null;
      }

      this.field08453.clear();
   }

   public static SkeletonEspModule method2471() {
      if (skeletonEspModule == null) {
         skeletonEspModule = new SkeletonEspModule();
      }

      return skeletonEspModule;
   }

   private int method2474(
      MatrixStack matrixStack,
      BufferBuilder bufferBuilder,
      ModelPart modelPart,
      float local3891_4,
      float local3891_5,
      float local3891_6,
      float local3891_7,
      int local3891_8
   ) {
      matrixStack.push();
      matrixStack.translate(local3891_4, local3891_5, local3891_6);
      if (modelPart.roll != 0.0F) {
         matrixStack.multiply(RotationAxis.POSITIVE_Z.rotation(modelPart.roll));
      }

      if (modelPart.yaw != 0.0F) {
         matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotation(modelPart.yaw));
      }

      if (modelPart.pitch != 0.0F) {
         matrixStack.multiply(RotationAxis.NEGATIVE_X.rotation(modelPart.pitch));
      }

      int local3891_9 = this.method2475(bufferBuilder, matrixStack.peek().getPositionMatrix(), 0.0F, 0.0F, 0.0F, 0.0F, local3891_7, 0.0F, local3891_8);
      matrixStack.pop();
      return local3891_9;
   }

   private int method2475(
      BufferBuilder bufferBuilder,
      Matrix4f matrix4f,
      float local3892_3,
      float local3892_4,
      float local3892_5,
      float local3892_6,
      float local3892_7,
      float local3892_8,
      int local3892_9
   ) {
      matrix4f.transformPosition(this.field08454.set(local3892_3, local3892_4, local3892_5));
      matrix4f.transformPosition(this.field08455.set(local3892_6, local3892_7, local3892_8));
      float local3892_10 = this.field08455.x - this.field08454.x;
      float local3892_11 = this.field08455.y - this.field08454.y;
      float local3892_12 = this.field08455.z - this.field08454.z;
      float local3892_13 = (this.field08454.x + this.field08455.x) * 0.5F;
      float local3892_14 = (this.field08454.y + this.field08455.y) * 0.5F;
      float local3892_15 = (this.field08454.z + this.field08455.z) * 0.5F;
      float local3892_16 = local3892_11 * local3892_15 - local3892_12 * local3892_14;
      float local3892_17 = local3892_12 * local3892_13 - local3892_10 * local3892_15;
      float local3892_18 = local3892_10 * local3892_14 - local3892_11 * local3892_13;
      float local3892_19 = MathHelper.sqrt(local3892_16 * local3892_16 + local3892_17 * local3892_17 + local3892_18 * local3892_18);
      if (local3892_19 < 1.0E-6F) {
         return 0;
      }

      float local3892_20 = MathHelper.sqrt(local3892_13 * local3892_13 + local3892_14 * local3892_14 + local3892_15 * local3892_15);
      float local3892_21 = Math.max(0.008F, local3892_20 * 0.0022F * MathHelper.clamp(1.35F - local3892_20 / 80.0F, 0.5F, 1.0F)) / local3892_19;
      this.method2478(
         bufferBuilder, local3892_16, local3892_17, local3892_18, local3892_21 * 1.9F, local3892_9 & 16777215 | (int)((local3892_9 >>> 24) * 0.3F) << 24
      );
      this.method2478(bufferBuilder, local3892_16, local3892_17, local3892_18, local3892_21, local3892_9);
      return 2;
   }

   private void method2477(BufferBuilder bufferBuilder, int local3894_2) {
      BuiltBuffer builtBuffer = bufferBuilder.endNullable();
      if (builtBuffer != null) {
         try (builtBuffer) {
            if (local3894_2 != 0 && mc.getFramebuffer() != null && mc.getFramebuffer().getColorAttachmentView() != null) {
               if (this.field08451 != null && !this.field08451.isClosed() && this.field08451.size() >= builtBuffer.getBuffer().remaining()) {
                  RenderSystem.getDevice()
                     .createCommandEncoder()
                     .writeToBuffer(this.field08451.slice(0L, builtBuffer.getBuffer().remaining()), builtBuffer.getBuffer());
               } else {
                  if (this.field08451 != null && !this.field08451.isClosed()) {
                     this.field08451.close();
                  }

                  this.field08451 = RenderSystem.getDevice().createBuffer(() -> "deluxe_skeleton_bones", 40, builtBuffer.getBuffer());
               }

               GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms().write(field08446, field08443, field08444, field08445);

               try (RenderPass renderPass = RenderSystem.getDevice()
                     .createCommandEncoder()
                     .createRenderPass(
                        () -> "deluxe_skeleton", mc.getFramebuffer().getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty()
                     )) {
                  renderPass.setPipeline(field08447);
                  renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                  renderPass.setUniform("Projection", RenderSystem.getProjectionMatrixBuffer());
                  renderPass.setVertexBuffer(0, this.field08451);
                  renderPass.setIndexBuffer(
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexBuffer(local3894_2 * 6),
                     RenderSystem.getSequentialBuffer(DrawMode.QUADS).getIndexType()
                  );
                  renderPass.drawIndexed(0, 0, local3894_2 * 6, 1);
                  return;
               }
            }
         }
      }
   }

   private SkeletonEspModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Свой цвет", "Красить кости своим цветом вместо ACCENT.", false));
      this.field08449 = this.addSetting(new ColorSetting("Цвет", "Цвет костей скелета."));
      this.field08450 = this.addSetting(new ColorSetting("Цвет друзей", "Цвет костей друзей.", new Color(64, 255, 128, 255)));
      this.field08449.withVisibility(() -> this.booleanSetting.isChecked());
   }

   private void method2478(BufferBuilder bufferBuilder, float local3896_2, float local3896_3, float local3896_4, float local3896_5, int local3896_6) {
      bufferBuilder.vertex(
            this.field08454.x - local3896_2 * local3896_5, this.field08454.y - local3896_3 * local3896_5, this.field08454.z - local3896_4 * local3896_5
         )
         .color(local3896_6);
      bufferBuilder.vertex(
            this.field08454.x + local3896_2 * local3896_5, this.field08454.y + local3896_3 * local3896_5, this.field08454.z + local3896_4 * local3896_5
         )
         .color(local3896_6);
      bufferBuilder.vertex(
            this.field08455.x + local3896_2 * local3896_5, this.field08455.y + local3896_3 * local3896_5, this.field08455.z + local3896_4 * local3896_5
         )
         .color(local3896_6);
      bufferBuilder.vertex(
            this.field08455.x - local3896_2 * local3896_5, this.field08455.y - local3896_3 * local3896_5, this.field08455.z - local3896_4 * local3896_5
         )
         .color(local3896_6);
   }

   private int method2479(MatrixStack matrixStack, BufferBuilder bufferBuilder, AbstractClientPlayerEntity abstractClientPlayerEntity, float local3897_4, int local3897_5) {
      PlayerEntityRenderer<AbstractClientPlayerEntity> local3897_6 = (PlayerEntityRenderer<AbstractClientPlayerEntity>)mc.getEntityRenderDispatcher()
         .<AbstractClientPlayerEntity>getRenderer(abstractClientPlayerEntity);
      PlayerEntityRenderState playerEntityRenderState = local3897_6.createRenderState();
      local3897_6.updateRenderState(abstractClientPlayerEntity, playerEntityRenderState, local3897_4);
      PlayerEntityModel model = local3897_6.getModel();
      model.setAngles(playerEntityRenderState);
      boolean isInSwimmingPose = abstractClientPlayerEntity.isInSwimmingPose();
      boolean isGliding = abstractClientPlayerEntity.isGliding();
      boolean local3897_11 = abstractClientPlayerEntity.isSneaking() && !abstractClientPlayerEntity.getAbilities().flying;
      float local3897_12 = local3897_11 ? 0.6F : 0.7F;
      float local3897_13 = local3897_11 ? 0.23F : 0.0F;
      float local3897_14 = local3897_11 ? 1.05F : 1.4F;
      float local3897_15 = local3897_11 ? 1.05F : 1.35F;
      matrixStack.push();
      Vec3d lerpedPos = abstractClientPlayerEntity.getLerpedPos(local3897_4);
      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      matrixStack.translate(lerpedPos.x - cameraPos.x, lerpedPos.y - cameraPos.y, lerpedPos.z - cameraPos.z);
      if (isInSwimmingPose) {
         matrixStack.translate(0.0F, 0.35F, 0.0F);
      }

      matrixStack.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(MathHelper.lerp(local3897_4, abstractClientPlayerEntity.lastBodyYaw, abstractClientPlayerEntity.bodyYaw) + 180.0F));
      if (isInSwimmingPose || isGliding) {
         matrixStack.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(90.0F + abstractClientPlayerEntity.getPitch(local3897_4)));
      }

      if (isInSwimmingPose) {
         matrixStack.translate(0.0F, -0.95F, 0.0F);
      }

      int local3897_18 = 0;
      local3897_18 += this.method2475(
         bufferBuilder, matrixStack.peek().getPositionMatrix(), 0.0F, local3897_12, local3897_13, 0.0F, local3897_14, 0.0F, local3897_5
      );
      local3897_18 += this.method2475(bufferBuilder, matrixStack.peek().getPositionMatrix(), -0.37F, local3897_15, 0.0F, 0.37F, local3897_15, 0.0F, local3897_5);
      local3897_18 += this.method2475(
         bufferBuilder, matrixStack.peek().getPositionMatrix(), -0.15F, local3897_12, local3897_13, 0.15F, local3897_12, local3897_13, local3897_5
      );
      local3897_18 += this.method2474(matrixStack, bufferBuilder, model.head, 0.0F, local3897_14, 0.0F, 0.15F, local3897_5);
      local3897_18 += this.method2474(matrixStack, bufferBuilder, model.rightLeg, 0.15F, local3897_12, local3897_13, -0.6F, local3897_5);
      local3897_18 += this.method2474(matrixStack, bufferBuilder, model.leftLeg, -0.15F, local3897_12, local3897_13, -0.6F, local3897_5);
      local3897_18 += this.method2474(matrixStack, bufferBuilder, model.rightArm, 0.37F, local3897_15, 0.0F, -0.55F, local3897_5);
      local3897_18 += this.method2474(matrixStack, bufferBuilder, model.leftArm, -0.37F, local3897_15, 0.0F, -0.55F, local3897_5);
      matrixStack.pop();
      return local3897_18;
   }
}
