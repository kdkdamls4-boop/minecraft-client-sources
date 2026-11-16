package deluxe.client.module.render;

import deluxe.client.module.combat.AutoTotemModule;
import deluxe.client.util.Colors;
import deluxe.client.DeluxeClient;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.module.ModuleManager;
import deluxe.client.event.PacketEvent;
import deluxe.client.render.ThemeColors;
import deluxe.client.event.WorldRenderEvent;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents.AfterClientWorldChange;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@ModuleInfo(name = "TotemGhost", description = "Призрак модельки на месте лопнувшего тотема.", category = ModuleCategory.RENDER)
public final class TotemGhostModule extends Module {
   private static TotemGhostModule totemGhostModule;
   private static final RenderPipeline field08777 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/totem_ghost_fill"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline field08778 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/totem_ghost_lines"))
      .withVertexShader(Identifier.ofVanilla("core/position_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_color"))
      .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .build();
   private static final RenderPipeline field08779 = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
      .withLocation(DeluxeClient.id("pipeline/totem_ghost_glow"))
      .withVertexShader(Identifier.ofVanilla("core/position_tex_color"))
      .withFragmentShader(Identifier.ofVanilla("core/position_tex_color"))
      .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
      .withBlend(BlendFunction.LIGHTNING)
      .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
      .withDepthWrite(false)
      .withCull(false)
      .withSampler("Sampler0")
      .build();
   private static final RenderLayer field08780 = RenderLayer.of("totem_ghost_fill", RenderSetup.builder(field08777).expectedBufferSize(16384).build());
   private static final RenderLayer field08781 = RenderLayer.of("totem_ghost_lines", RenderSetup.builder(field08778).expectedBufferSize(4096).build());
   private static final RenderLayer field08782 = RenderLayer.of(
      "totem_ghost_glow", RenderSetup.builder(field08779).texture("Sampler0", DeluxeClient.id("textures/particles/glow.png")).expectedBufferSize(1024).build()
   );
   private static final float[][] field08783 = new float[][]{
      {-0.25F, 1.5F, -0.25F, 0.25F, 2.0F, 0.25F},
      {-0.25F, 0.75F, -0.125F, 0.25F, 1.5F, 0.125F},
      {0.25F, 0.75F, -0.125F, 0.5F, 1.5F, 0.125F},
      {-0.5F, 0.75F, -0.125F, -0.25F, 1.5F, 0.125F},
      {0.0F, 0.0F, -0.125F, 0.25F, 0.75F, 0.125F},
      {-0.25F, 0.0F, -0.125F, 0.0F, 0.75F, 0.125F}
   };
   private static final float[][] field08784 = new float[][]{
      {0.0F, 1.5F, 0.0F}, {0.0F, 0.75F, 0.0F}, {0.375F, 1.4F, 0.0F}, {-0.375F, 1.4F, 0.0F}, {0.125F, 0.75F, 0.0F}, {-0.125F, 0.75F, 0.0F}
   };
   private static final int[][] field08785 = new int[][]{{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
   private static final int[][] field08786 = new int[][]{{0, 1}, {1, 3}, {3, 2}, {2, 0}, {4, 5}, {5, 7}, {7, 6}, {6, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
   private final ArrayList<TotemGhostModule.Nested0444> field08787 = new ArrayList<>();
   private BufferAllocator field08788 = new BufferAllocator(131072);
   private Immediate field08789 = VertexConsumerProvider.immediate(this.field08788);
   private final EventListener<PacketEvent> field08790 = packetEvent -> {
      if (AutoTotemModule.method3667().field12780.isChecked()
         && packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof EntityStatusS2CPacket entityStatusS2CPacket
         && entityStatusS2CPacket.getStatus() == 35) {
         mc.execute(
            () -> {
               if (mc.world != null && entityStatusS2CPacket.getEntity(mc.world) != null && entityStatusS2CPacket.getEntity(mc.world) != mc.player) {
                  TotemGhostModule.Nested0444 local4062_2 = new TotemGhostModule.Nested0444(
                     entityStatusS2CPacket.getEntity(mc.world).getEntityPos().x,
                     entityStatusS2CPacket.getEntity(mc.world).getEntityPos().y,
                     entityStatusS2CPacket.getEntity(mc.world).getEntityPos().z,
                     entityStatusS2CPacket.getEntity(mc.world).getYaw()
                  );
                  if (entityStatusS2CPacket.getEntity(mc.world) instanceof LivingEntity livingEntity) {
                     local4062_2.field08939 = livingEntity.bodyYaw;
                     local4062_2.field08940 = MathHelper.clamp(MathHelper.wrapDegrees(livingEntity.headYaw - livingEntity.bodyYaw), -75.0F, 75.0F);
                     local4062_2.field08941[0] = livingEntity.getPitch();
                     local4062_2.field08941[2] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F + (float) Math.PI)
                           * 2.0F
                           * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                           * 0.5F
                     );
                     local4062_2.field08941[3] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F)
                           * 2.0F
                           * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                           * 0.5F
                     );
                     local4062_2.field08941[4] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F) * 1.4F * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                     );
                     local4062_2.field08941[5] = (float)Math.toDegrees(
                        MathHelper.cos(livingEntity.limbAnimator.getAnimationProgress() * 0.6662F + (float) Math.PI)
                           * 1.4F
                           * Math.min(livingEntity.limbAnimator.getSpeed(), 1.0F)
                     );
                  }

                  this.field08787.add(local4062_2);
               }
            }
         );
      }
   };
   private final EventListener<WorldRenderEvent> field08791 = worldRenderEvent -> {
      if (AutoTotemModule.method3667().field12780.isChecked() && mc.player != null && mc.world != null && mc.gameRenderer != null) {
         this.field08787.removeIf(local4054_0 -> (float)(System.currentTimeMillis() - local4054_0.field08938) >= 2000.0F);
         if (!this.field08787.isEmpty()) {
            for (TotemGhostModule.Nested0444 local4064_3 : this.field08787) {
               local4064_3.field08942 = (float)(System.currentTimeMillis() - local4064_3.field08938) / 2000.0F;
               local4064_3.field08943 = Math.min(local4064_3.field08942 * 8.0F, 1.0F)
                  * (local4064_3.field08942 > 0.55F ? 1.0F - (local4064_3.field08942 - 0.55F) / 0.45F : 1.0F);
               worldRenderEvent.method0647().push();
               worldRenderEvent.method0647()
                  .translate(
                     local4064_3.field08935 - mc.gameRenderer.getCamera().getCameraPos().x,
                     local4064_3.field08936
                        - mc.gameRenderer.getCamera().getCameraPos().y
                        + (1.0F - (1.0F - local4064_3.field08942) * (1.0F - local4064_3.field08942) * (1.0F - local4064_3.field08942)) * 2.0F,
                     local4064_3.field08937 - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               worldRenderEvent.method0647().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-local4064_3.field08939 + local4064_3.field08942 * 40.0F));
               worldRenderEvent.method0647().push();
               worldRenderEvent.method0647().translate(0.0F, 2.16F, 0.0F);
               worldRenderEvent.method0647().multiply(RotationAxis.POSITIVE_X.rotationDegrees(11.0F));
               worldRenderEvent.method0647().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-4.0F));
               local4064_3.field08945.set(worldRenderEvent.method0647().peek().getPositionMatrix());
               worldRenderEvent.method0647().pop();

               for (int index = 0; index < field08783.length; index++) {
                  if (local4064_3.field08941[index] != 0.0F || index == 0 && local4064_3.field08940 != 0.0F) {
                     worldRenderEvent.method0647().push();
                     worldRenderEvent.method0647().translate(field08784[index][0], field08784[index][1], field08784[index][2]);
                     if (index == 0 && local4064_3.field08940 != 0.0F) {
                        worldRenderEvent.method0647().multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-local4064_3.field08940));
                     }

                     if (local4064_3.field08941[index] != 0.0F) {
                        worldRenderEvent.method0647().multiply(RotationAxis.POSITIVE_X.rotationDegrees(local4064_3.field08941[index]));
                     }

                     worldRenderEvent.method0647().translate(-field08784[index][0], -field08784[index][1], -field08784[index][2]);
                     local4064_3.field08944[index].set(worldRenderEvent.method0647().peek().getPositionMatrix());
                     worldRenderEvent.method0647().pop();
                  } else {
                     local4064_3.field08944[index].set(worldRenderEvent.method0647().peek().getPositionMatrix());
                  }
               }

               worldRenderEvent.method0647().pop();
               worldRenderEvent.method0647().push();
               worldRenderEvent.method0647()
                  .translate(
                     local4064_3.field08935 - mc.gameRenderer.getCamera().getCameraPos().x,
                     local4064_3.field08936
                        - mc.gameRenderer.getCamera().getCameraPos().y
                        + (1.0F - (1.0F - local4064_3.field08942) * (1.0F - local4064_3.field08942) * (1.0F - local4064_3.field08942)) * 2.0F
                        + 1.1F,
                     local4064_3.field08937 - mc.gameRenderer.getCamera().getCameraPos().z
                  );
               worldRenderEvent.method0647().multiply(mc.gameRenderer.getCamera().getRotation());
               local4064_3.field08946.set(worldRenderEvent.method0647().peek().getPositionMatrix());
               worldRenderEvent.method0647().pop();
            }

            for (TotemGhostModule.Nested0444 local4064_3 : this.field08787) {
               if (!(local4064_3.field08943 <= 0.0F)) {
                  for (int index2 = 0; index2 < field08783.length; index2++) {
                     for (int[] intArray : field08785) {
                        for (int local4064_12 : intArray) {
                           this.field08789
                              .getBuffer(field08780)
                              .vertex(
                                 local4064_3.field08944[index2],
                                 this.method2608(field08783[index2], local4064_12, 0),
                                 this.method2608(field08783[index2], local4064_12, 1),
                                 this.method2608(field08783[index2], local4064_12, 2)
                              )
                              .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.25F));
                        }
                     }
                  }
               }
            }

            for (TotemGhostModule.Nested0444 local4064_3 : this.field08787) {
               this.method2613(
                  this.field08789.getBuffer(field08780),
                  local4064_3.field08945,
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.2F),
                  0,
                  0.3F,
                  0.395F,
                  0.0F
               );
               this.method2613(
                  this.field08789.getBuffer(field08780),
                  local4064_3.field08945,
                  0,
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.2F),
                  0.405F,
                  0.5F,
                  0.0F
               );
            }

            for (TotemGhostModule.Nested0444 local4064_3 : this.field08787) {
               this.method2613(
                  this.field08789.getBuffer(field08780),
                  local4064_3.field08945,
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F),
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F),
                  0.39F,
                  0.41F,
                  0.006F
               );
               this.method2613(
                  this.field08789.getBuffer(field08780),
                  local4064_3.field08945,
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F),
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F),
                  0.39F,
                  0.41F,
                  -0.006F
               );

               for (int index3 = 0; index3 < 96; index3++) {
                  this.field08789
                     .getBuffer(field08780)
                     .vertex(
                        local4064_3.field08945,
                        MathHelper.cos((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F,
                        -0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F
                     )
                     .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F));
                  this.field08789
                     .getBuffer(field08780)
                     .vertex(
                        local4064_3.field08945,
                        MathHelper.cos((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F,
                        -0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F
                     )
                     .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F));
                  this.field08789
                     .getBuffer(field08780)
                     .vertex(
                        local4064_3.field08945,
                        MathHelper.cos((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F,
                        0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * (index3 + 1) / 96.0F) * 0.41F
                     )
                     .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F));
                  this.field08789
                     .getBuffer(field08780)
                     .vertex(
                        local4064_3.field08945,
                        MathHelper.cos((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F,
                        0.006F,
                        MathHelper.sin((float) (Math.PI * 2) * index3 / 96.0F) * 0.41F
                     )
                     .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.92F));
               }
            }

            for (TotemGhostModule.Nested0444 local4064_3 : this.field08787) {
               if (!(local4064_3.field08943 <= 0.0F)) {
                  for (int index4 = 0; index4 < field08783.length; index4++) {
                     for (int[] intArray2 : field08786) {
                        this.field08789
                           .getBuffer(field08781)
                           .vertex(
                              local4064_3.field08944[index4],
                              this.method2608(field08783[index4], intArray2[0], 0),
                              this.method2608(field08783[index4], intArray2[0], 1),
                              this.method2608(field08783[index4], intArray2[0], 2)
                           )
                           .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.45F));
                        this.field08789
                           .getBuffer(field08781)
                           .vertex(
                              local4064_3.field08944[index4],
                              this.method2608(field08783[index4], intArray2[1], 0),
                              this.method2608(field08783[index4], intArray2[1], 1),
                              this.method2608(field08783[index4], intArray2[1], 2)
                           )
                           .color(Colors.method3727(this.method2609(), local4064_3.field08943 * 0.45F));
                     }
                  }
               }
            }

            for (TotemGhostModule.Nested0444 local4064_3 : this.field08787) {
               this.method2607(
                  this.field08789.getBuffer(field08782),
                  local4064_3.field08946,
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.43F),
                  1.5F
               );
               this.method2607(
                  this.field08789.getBuffer(field08782),
                  local4064_3.field08946,
                  Colors.method3727(this.method2609(), local4064_3.field08943 * 0.63F),
                  0.7F
               );
            }

            this.field08789.draw();
         }
      } else {
         this.field08787.clear();
      }
   };

   private void method2607(VertexConsumer vertexConsumer, Matrix4f matrix4f, int local4055_3, float local4055_4) {
      vertexConsumer.vertex(matrix4f, -local4055_4, -local4055_4, 0.0F).texture(0.0F, 1.0F).color(local4055_3);
      vertexConsumer.vertex(matrix4f, local4055_4, -local4055_4, 0.0F).texture(1.0F, 1.0F).color(local4055_3);
      vertexConsumer.vertex(matrix4f, local4055_4, local4055_4, 0.0F).texture(1.0F, 0.0F).color(local4055_3);
      vertexConsumer.vertex(matrix4f, -local4055_4, local4055_4, 0.0F).texture(0.0F, 0.0F).color(local4055_3);
   }

   private float method2608(float[] floatArray, int local4056_2, int local4056_3) {
      return floatArray[(local4056_2 >> local4056_3 & 1) * 3 + local4056_3];
   }

   private int method2609() {
      return ThemeColors.field13669;
   }

   public static TotemGhostModule method2610() {
      if (totemGhostModule == null) {
         totemGhostModule = new TotemGhostModule();
      }

      return totemGhostModule;
   }

   private TotemGhostModule() {
      ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((AfterClientWorldChange)(minecraftClient, clientWorld) -> this.field08787.clear());
      ModuleManager.unregisterModule(this);
      this.setEnabledWithoutCallbacks(true);
   }

   @Override
   protected void onDisable() {
      this.field08787.clear();
   }

   private void method2613(
      VertexConsumer vertexConsumer, Matrix4f matrix4f, int local4063_3, int local4063_4, float local4063_5, float local4063_6, float local4063_7
   ) {
      if (local4063_3 >>> 24 != 0 || local4063_4 >>> 24 != 0) {
         for (int index = 0; index < 96; index++) {
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * index / 96.0F) * local4063_5,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * index / 96.0F) * local4063_5
               )
               .color(local4063_3);
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * index / 96.0F) * local4063_6,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * index / 96.0F) * local4063_6
               )
               .color(local4063_4);
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_6,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_6
               )
               .color(local4063_4);
            vertexConsumer.vertex(
                  matrix4f,
                  MathHelper.cos((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_5,
                  local4063_7,
                  MathHelper.sin((float) (Math.PI * 2) * (index + 1) / 96.0F) * local4063_5
               )
               .color(local4063_3);
         }
      }
   }

   private static final class Nested0444 {
      private final double field08935;
      private final double field08936;
      private final double field08937;
      private final long field08938;
      private float field08939;
      private float field08940;
      private final float[] field08941 = new float[TotemGhostModule.field08783.length];
      private float field08942;
      private float field08943;
      private final Matrix4f[] field08944 = new Matrix4f[TotemGhostModule.field08783.length];
      private final Matrix4f field08945 = new Matrix4f();
      private final Matrix4f field08946 = new Matrix4f();

      private Nested0444(double local4065_1, double local4065_3, double local4065_5, float local4065_7) {
         this.field08935 = local4065_1;
         this.field08936 = local4065_3;
         this.field08937 = local4065_5;
         this.field08939 = local4065_7;
         this.field08938 = System.currentTimeMillis();

         for (int index = 0; index < TotemGhostModule.field08783.length; index++) {
            this.field08944[index] = new Matrix4f();
         }
      }
   }
}
