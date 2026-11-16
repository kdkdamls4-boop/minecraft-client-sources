package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.*;
import tokyo.ghoul.client.features.modules.render.targetesp.render.*;
import net.minecraft.client.renderer.RenderPipelines;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.MeshData.DrawState;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@ModuleRegister(name = "ChinaHat", description = "Шляпа над головой игрока", category = Category.RENDER)
public class ChinaHatModule extends Module {

   private static final Identifier f_16193 = Identifier.fromNamespaceAndPath("desire", "targetesp/images/fireflies/bloom.png");
   private final ModeSetting f_16195;
   private final MultiBooleanSetting f_16196;
   private final SliderSetting f_16197;
   private final SliderSetting f_16198;
   private final SliderSetting f_16199;
   private final SliderSetting f_16200;
   private final SliderSetting f_16201;
   private final SliderSetting f_16202;
   private final SliderSetting f_16203;
   private final SliderSetting f_16204;
   private final BooleanSetting f_16205;
   private final BooleanSetting f_16206;
   private final ColorSetting f_16207;
   private final RenderPipeline f_16208;
   private final RenderPipeline f_16209;
   private final Map<Integer, LinkedList<Vec3>[]> f_16213;
   private final Vector4f f_16214;
   private final Matrix4f f_16215;
   private final Matrix4f f_16216;
   private final Vector4f f_16218;
   private final Vector4f f_16219;
   private long f_16220;
   private float f_16222;
   private float f_16223;
   private float f_16224;
   private float f_16225;
   private float f_16226;
   private float f_16227;
   private static final float f_16316 = (float) (Math.PI * 2);
   private static final double f_16368 = Math.PI * 2;
   private static final float f_16378 = (float) (Math.PI * 2);
   private static final float f_16380 = (float) (Math.PI * 2);
   private static final float f_16396 = (float) (Math.PI * 2);
   private static final float f_16398 = (float) (Math.PI * 2);
   private static final float f_16403 = (float) (Math.PI * 2);
   private static final float f_16405 = (float) (Math.PI * 2);

   private void m_5643(BufferBuilder var1, int var2) {
      MeshData var3 = var1.build();
      if (var3 != null) {
         if (var2 == 0) {
            var3.close();
         } else {
            GpuBuffer var4 = null;

            try {
               DrawState var5 = var3.drawState();
               int var6 = var5.indexCount();
               AutoStorageIndexBuffer var7 = RenderSystem.getSequentialBuffer(var5.primitiveTopology());
               GpuBuffer var8 = var7.getBuffer(var6);
               IndexType var9 = var7.type();
               var4 = RenderSystem.getDevice().createBuffer(() -> "chinaHatGhost3Vertices", 32, var3.vertexBuffer());
               RenderTarget var10 = mc.gameRenderer.mainRenderTarget();
               CommandEncoder var11 = RenderSystem.getDevice().createCommandEncoder();
               AbstractTexture var12 = mc.getTextureManager().getTexture(f_16193);
               RenderPass var13 = var11.createRenderPass(
                  () -> "chinaHatGhost3", var10.getColorTextureView(), Optional.empty(), var10.getDepthTextureView(), OptionalDouble.empty()
               );

               try {
                  RenderSystem.bindDefaultUniforms(var13);
                  var13.setPipeline(this.f_16209);
                  var13.bindTexture("Sampler0", var12.getTextureView(), var12.getSampler());
                  var13.setVertexBuffer(0, var4.slice());
                  var13.setIndexBuffer(var8, var9);
                  var13.drawIndexed(var6, 1, 0, 0, 0);
               } catch (Throwable var21) {
                  if (var13 != null) {
                     try {
                        var13.close();
                     } catch (Throwable var20) {
                        var21.addSuppressed(var20);
                     }
                  }

                  throw var21;
               }

               if (var13 != null) {
                  var13.close();
               }
            } finally {
               if (var4 != null) {
                  var4.close();
               }

               var3.close();
            }
         }
      }
   }

   private void m_5644(BufferBuilder var1, Matrix4f var2, float var3, float var4, float var5, Vector4f var6) {
      var1.addVertex(var2, var3, var4, var5).setUv(0.0F, 0.0F).setColor(var6.x, var6.y, var6.z, var6.w);
   }

   private float m_5647(Player var1, ChinaHatModule.uDvg2RY38Z22x8A4 var2, boolean var3) {
      boolean var4 = var2.xRot != 0.0F || var2.swimShift > 0.0F || var1.isFallFlying() || var1.isVisuallySwimming() || var1.isVisuallyCrawling();
      float var5 = (var4 ? var1.getDimensions(Pose.STANDING).height() : var1.getBbHeight()) * var2.scale;
      return var3 ? var5 * 0.8333334F - 0.12F : var5 + this.f_16199.getValue();
   }

   private int m_5648(BufferBuilder var1, Matrix4f var2, float var3, float var4, float var5, float var6, int var7, float var8) {
      Vector4f var9 = ArgbUtil.m_2298(ArgbUtil.m_2306(var7, var8));
      Vector4f var10 = ArgbUtil.m_2298(ArgbUtil.m_2306(var7, 0.0F));
      int var11 = 0;

      for (int var12 = 0; var12 < 8; var12++) {
         float var13 = f_16378 * var12 / 8.0F;
         float var14 = f_16380 * (var12 + 1) / 8.0F;
         float var15 = Mth.cos(var13);
         float var16 = Mth.sin(var13);
         float var17 = Mth.cos(var14);
         float var18 = Mth.sin(var14);
         this.m_5644(var1, var2, var3, var4, var5, var9);
         this.m_5644(
            var1,
            var2,
            var3 + this.f_16218.x * var15 * var6 + this.f_16219.x * var16 * var6,
            var4 + this.f_16218.y * var15 * var6 + this.f_16219.y * var16 * var6,
            var5 + this.f_16218.z * var15 * var6 + this.f_16219.z * var16 * var6,
            var10
         );
         this.m_5644(
            var1,
            var2,
            var3 + this.f_16218.x * var17 * var6 + this.f_16219.x * var18 * var6,
            var4 + this.f_16218.y * var17 * var6 + this.f_16219.y * var18 * var6,
            var5 + this.f_16218.z * var17 * var6 + this.f_16219.z * var18 * var6,
            var10
         );
         this.m_5644(var1, var2, var3, var4, var5, var9);
         var11 += 4;
      }

      return var11;
   }

   private static boolean m_5652(Vector4f var0) {
      float var1 = Mth.sqrt(var0.x * var0.x + var0.y * var0.y + var0.z * var0.z);
      if (!(var1 < 1.0E-5F) && Float.isFinite(var1)) {
         var0.x /= var1;
         var0.y /= var1;
         var0.z /= var1;
         return true;
      } else {
         return false;
      }
   }

   private float m_5653(PoseStack var1, Vec3 var2, Player var3, float var4, float var5) {
      ChinaHatModule.uDvg2RY38Z22x8A4 var6 = this.m_5655(var3, var4);
      var1.pushPose();
      this.m_5688(var1, var2, var3, var4, var6, var5, this.m_5647(var3, var6, false));
      return var6.scale;
   }

   private int m_5654(
      BufferBuilder var1,
      Matrix4f var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      float var14,
      float var15,
      Vector4f var16
   ) {
      this.m_5657(var1, var2, var3, var4, var5, var6 - var8, var7 - var9, var10, var11, var12, var13, var14, var15, var16);
      this.m_5657(var1, var2, var3, var4, var5, var6 - var8, var7 + var9, var10, var11, var12, var13, var14, var15, var16);
      this.m_5657(var1, var2, var3, var4, var5, var6 + var8, var7 + var9, var10, var11, var12, var13, var14, var15, var16);
      this.m_5657(var1, var2, var3, var4, var5, var6 + var8, var7 - var9, var10, var11, var12, var13, var14, var15, var16);
      return 4;
   }

   private ChinaHatModule.uDvg2RY38Z22x8A4 m_5655(Player var1, float var2) {
      float var3 = Mth.rotLerp(var2, var1.yBodyRotO, var1.yBodyRot);
      float var4 = Mth.lerp(var2, var1.xRotO, var1.getXRot());
      float var5 = var1.getSwimAmount(var2);
      float var6 = var1.isBaby() ? 0.5F : 1.0F;
      float var7 = 0.0F;
      float var8 = 0.0F;
      if (var1.isFallFlying()) {
         float var9 = var1.getFallFlyingTicks() + var2;
         float var10 = Mth.clamp(var9 * var9 / 100.0F, 0.0F, 1.0F);
         if (!var1.isAutoSpinAttack()) {
            var7 = var10 * (var4 + 90.0F);
         }
      } else if (var5 > 0.0F) {
         float var11 = var1.isInWater() ? var4 + 90.0F : 90.0F;
         var7 = Mth.lerp(var5, 0.0F, var11);
         if (var1.isVisuallySwimming()) {
            var8 = var5;
         }
      }

      return new ChinaHatModule.uDvg2RY38Z22x8A4(var3, var7, var8, var6);
   }

   private int m_5656(BufferBuilder var1, PoseStack var2, Vec3 var3, Player var4, float var5) {
      float var6 = this.m_5653(var2, var3, var4, var5, 0.0F);
      Matrix4f var7 = var2.last().pose();
      this.f_16216.set(var7).invert();
      this.f_16218.set(1.0F, 0.0F, 0.0F, 0.0F);
      this.f_16216.transform(this.f_16218);
      this.f_16219.set(0.0F, 1.0F, 0.0F, 0.0F);
      this.f_16216.transform(this.f_16219);
      if (m_5652(this.f_16218) && m_5652(this.f_16219)) {
         float var8 = this.f_16197.getValue() * var6;
         float var9 = this.f_16198.getValue() * var6;
         float var10 = this.f_16201.getValue();
         int var11 = this.m_5686(0);
         int var12 = this.f_16206.getValue() ? tokyo.ghoul.api.utils.color.UIColors.secondary().getRGB() : this.m_5686(48);
         double var13 = System.currentTimeMillis() * 0.004 * (this.f_16200.getValue() / 45.0F);
         LinkedList[] var15 = this.m_5678(var4.getId());
         int var16 = 0;

         for (int var17 = 0; var17 < var15.length; var17++) {
            double var18 = var17 * (f_16368 / var15.length);
            double var20 = Math.sin(var13 + var18) * var8;
            double var22 = Math.cos(var13 + var18) * var8;
            double var24 = var17 % 2 == 0 ? 1.0 : -1.0;
            double var26 = Math.sin((var13 + var18) * 0.8) * var24 * var9;
            LinkedList var28 = var15[var17];
            Vec3 var29 = new Vec3(var20, var26, var22);
            if (!var28.isEmpty()) {
               Vec3 var30 = (Vec3)var28.getFirst();
               double var31 = var30.distanceTo(var29);
               int var33 = Mth.clamp((int)Math.ceil(var31 / 0.04), 1, 6);

               for (int var34 = 1; var34 <= var33; var34++) {
                  var28.addFirst(var30.lerp(var29, (double)var34 / var33));
               }
            } else {
               var28.addFirst(var29);
            }

            while (var28.size() > 24) {
               var28.removeLast();
            }

            int var42 = 0;
            int var43 = var28.size();

            for (Vec3 var44 : (Iterable<Vec3>) (Iterable) var28) {
               float var45 = 1.0F - (float)var42 / var43;
               float var35 = (float)var44.x;
               float var36 = (float)var44.y;
               float var37 = (float)var44.z;
               int var38 = ArgbUtil.m_2302(var11, var12, var45);
               float var39 = var42 == 0 ? 0.26F : 0.16F;
               float var40 = var39 * var45 * Math.max(var10, 0.35F);
               float var41 = (float)(Math.pow(var45, 1.8) * var10);
               var16 += this.m_5648(var1, var7, var35, var36, var37, var40, var38, var41);
               if (var42 == 0) {
                  var16 += this.m_5648(var1, var7, var35, var36, var37, 0.12F, -1, var10);
               }

               var42++;
            }
         }

         var2.popPose();
         return var16;
      } else {
         var2.popPose();
         return 0;
      }
   }

   private int m_5657(
      BufferBuilder var1,
      Matrix4f var2,
      float var3,
      float var4,
      float var5,
      float var6,
      float var7,
      float var8,
      float var9,
      float var10,
      float var11,
      float var12,
      float var13,
      Vector4f var14
   ) {
      this.m_5644(var1, var2, var3 + var8 * var6 + var11 * var7, var4 + var9 * var6 + var12 * var7, var5 + var10 * var6 + var13 * var7, var14);
      return 1;
   }

   private boolean m_5658(float var1, float var2, float var3, float var4, float var5) {
      float var6 = this.f_16214.x - var1;
      float var7 = this.f_16214.y - var2;
      float var8 = this.f_16214.z - var3;
      float var9 = Mth.sqrt(var6 * var6 + var7 * var7 + var8 * var8);
      if (var9 < 1.0E-5F) {
         return false;
      }

      var6 /= var9;
      var7 /= var9;
      var8 /= var9;
      float var10 = var8;
      float var11 = 0.0F;
      float var12 = -var6;
      float var13 = Mth.sqrt(var10 * var10 + var12 * var12);
      if (var13 < 1.0E-5F) {
         var10 = var4;
         var11 = 0.0F;
         var12 = 0.0F;
      } else {
         float var14 = var4 / var13;
         var10 *= var14;
         var12 *= var14;
      }

      float var24 = var7 * var12 - var8 * var11;
      float var15 = var8 * var10 - var6 * var12;
      float var16 = var6 * var11 - var7 * var10;
      float var17 = Mth.sqrt(var24 * var24 + var15 * var15 + var16 * var16);
      if (var17 < 1.0E-5F) {
         return false;
      }

      float var18 = var5 / var17;
      this.f_16222 = var10;
      this.f_16223 = var11;
      this.f_16224 = var12;
      this.f_16225 = var24 * var18;
      this.f_16226 = var15 * var18;
      this.f_16227 = var16 * var18;
      return true;
   }

   public ChinaHatModule() {
      this.f_16195 = new ModeSetting("Режим").value("Обычный").values("Обычный", "Градиент", "Контур", "Призраки 1", "Призраки 2", "Призраки 3");
      this.f_16196 = new MultiBooleanSetting("Отображать").value(new BooleanSetting("Себя").value(true), new BooleanSetting("Друзей").value(false), new BooleanSetting("Игроков").value(false));
      this.f_16197 = new SliderSetting("Радиус").value(0.65F).range(0.3F, 1.4F).step(0.05F);
      this.f_16198 = new SliderSetting("Высота").value(0.35F).range(0.1F, 0.9F).step(0.05F)
         .setVisible(() -> !this.f_16195.is("Призраки 3"));
      this.f_16199 = new SliderSetting("Отступ").value(0.08F).range(-0.2F, 0.4F).step(0.01F);
      this.f_16200 = new SliderSetting("Скорость").value(45.0F).range(0.0F, 180.0F).step(1.0F);
      this.f_16201 = new SliderSetting("Прозрачность").value(0.75F).range(0.15F, 1.0F).step(0.05F);
      this.f_16202 = new SliderSetting("Размер").value(0.3F).range(0.05F, 1.0F).step(0.01F).setVisible(() -> this.f_16195.is("Призраки 3"));
      this.f_16203 = new SliderSetting("Длина трейла").value(25.0F).range(5.0F, 100.0F).step(1.0F).setVisible(() -> this.f_16195.is("Призраки 3"));
      this.f_16204 = new SliderSetting("Смещение цвета").value(0.0F).range(0.0F, 360.0F).step(1.0F).setVisible(() -> this.f_16195.is("Призраки 3"));
      this.f_16205 = new BooleanSetting("Всегда показывать").value(false).setVisible(() -> this.f_16195.is("Призраки 3"));
      this.f_16206 = new BooleanSetting("Авто цвет под тему").value(true).setVisible(() -> !this.f_16195.is("Градиент"));
      this.f_16207 = new ColorSetting("Цвет").value(new java.awt.Color(-38006, true))
         .setVisible(() -> !this.f_16206.getValue() && !this.f_16195.is("Градиент"));
      this.f_16208 = PipelineBuilder.m_2718(PipelineBuilder.f_7671)
         .m_2726("desire/china_hat")
         .m_2734(EspResources.m_9312("target_esp_crystal"))
         .m_2740(EspResources.m_9312("target_esp_crystal"))
         .m_2719(CompareOp.ALWAYS_PASS)
         .m_2737(false)
         .m_2725(false)
         .m_2732(BlendFunction.TRANSLUCENT)
         .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
         .m_2722();
      this.f_16209 = PipelineBuilder.m_2718(PipelineBuilder.f_7671)
         .m_2726("desire/china_hat_ghost3")
         .m_2734(EspResources.m_9312("dead_chams"))
         .m_2740(EspResources.m_9312("dead_chams"))
         .m_2719(CompareOp.GREATER_THAN_OR_EQUAL)
         .m_2737(false)
         .m_2725(false)
         .m_2732(BlendFunction.ADDITIVE)
         .m_2739(DefaultVertexFormat.POSITION_TEX_COLOR, PrimitiveTopology.QUADS)
         .m_2741("Sampler0")
         .m_2722();
      this.f_16213 = new HashMap<>();
      this.f_16214 = new Vector4f();
      this.f_16215 = new Matrix4f();
      this.f_16216 = new Matrix4f();
      this.f_16218 = new Vector4f();
      this.f_16219 = new Vector4f();
      this.f_16220 = -1L;
      addSettings(
         this.f_16195,
         this.f_16196,
         this.f_16197,
         this.f_16198,
         this.f_16199,
         this.f_16200,
         this.f_16201,
         this.f_16202,
         this.f_16203,
         this.f_16204,
         this.f_16205,
         this.f_16206,
         this.f_16207
      );
   }

   private int m_5660(BufferBuilder var1, Matrix4f var2, float var3, float var4, float var5, float var6, int var7) {
      Vector4f var8 = ArgbUtil.m_2298(var7);
      Vector4f var9 = ArgbUtil.m_2298(ArgbUtil.m_2306(-15394268, Math.max(0.45F, var8.w)));
      float var10 = var6;
      float var11 = var6 * 1.25F;
      float[] var12 = new float[]{
         0.0F,
         var11 * 0.5F,
         var10 * 0.22F,
         var11 * 0.42F,
         var10 * 0.38F,
         var11 * 0.22F,
         var10 * 0.44F,
         var11 * 0.02F,
         var10 * 0.42F,
         -var11 * 0.18F,
         var10 * 0.36F,
         -var11 * 0.4F,
         var10 * 0.2F,
         -var11 * 0.28F,
         var10 * 0.06F,
         -var11 * 0.48F,
         0.0F,
         -var11 * 0.34F,
         -var10 * 0.06F,
         -var11 * 0.48F,
         -var10 * 0.2F,
         -var11 * 0.28F,
         -var10 * 0.36F,
         -var11 * 0.4F,
         -var10 * 0.42F,
         -var11 * 0.18F,
         -var10 * 0.44F,
         var11 * 0.02F,
         -var10 * 0.38F,
         var11 * 0.22F,
         -var10 * 0.22F,
         var11 * 0.42F
      };
      float var13 = 0.0F;
      float var14 = 0.0F;
      float var15 = 0.0F;
      float var16 = 0.0F;
      float var17 = 0.0F;
      float var18 = 0.0F;
      if (!this.m_5658(var3, var4, var5, 1.0F, 1.0F)) {
         var13 = 1.0F;
         var17 = 1.0F;
      } else {
         var13 = this.f_16222;
         var14 = this.f_16223;
         var15 = this.f_16224;
         var16 = this.f_16225;
         var17 = this.f_16226;
         var18 = this.f_16227;
      }

      int var19 = var12.length / 2;
      int var20 = 0;
      float var21 = 0.0F;
      float var22 = var11 * 0.05F;

      for (int var23 = 0; var23 < var19; var23++) {
         int var24 = (var23 + 1) % var19;
         var20 += this.m_5657(var1, var2, var3, var4, var5, var21, var22, var13, var14, var15, var16, var17, var18, var8);
         var20 += this.m_5657(var1, var2, var3, var4, var5, var12[var23 * 2], var12[var23 * 2 + 1], var13, var14, var15, var16, var17, var18, var8);
         var20 += this.m_5657(var1, var2, var3, var4, var5, var12[var24 * 2], var12[var24 * 2 + 1], var13, var14, var15, var16, var17, var18, var8);
         var20 += this.m_5657(var1, var2, var3, var4, var5, var21, var22, var13, var14, var15, var16, var17, var18, var8);
      }

      var20 += this.m_5654(
         var1, var2, var3, var4, var5, -var10 * 0.13F, var11 * 0.14F, var10 * 0.07F, var11 * 0.11F, var13, var14, var15, var16, var17, var18, var9
      );
      return var20
         + this.m_5654(
            var1, var2, var3, var4, var5, var10 * 0.13F, var11 * 0.14F, var10 * 0.07F, var11 * 0.11F, var13, var14, var15, var16, var17, var18, var9
         );
   }

   private static Vec3 m_5661(Player var0, float var1) {
      return new Vec3(Mth.lerp(var1, var0.xo, var0.getX()), Mth.lerp(var1, var0.yo, var0.getY()), Mth.lerp(var1, var0.zo, var0.getZ()));
   }

   @Override
   public void onDisable() {
      this.f_16220 = -1L;
      this.f_16213.clear();
      super.onDisable();
   }

   private int m_5664(BufferBuilder var1, Matrix4f var2, float var3, float var4, float var5) {
      int var6 = 0;
      boolean var7 = this.f_16195.is("Градиент");
      Vector4f var8 = ArgbUtil.m_2298(ArgbUtil.m_2306(this.m_5686(0), var5 * (var7 ? 0.95F : 0.9F)));

      for (int var9 = 0; var9 < 64; var9++) {
         float var10 = f_16396 * var9 / 64.0F;
         float var11 = f_16398 * (var9 + 1) / 64.0F;
         float var12 = Mth.cos(var10) * var3;
         float var13 = Mth.sin(var10) * var3;
         float var14 = Mth.cos(var11) * var3;
         float var15 = Mth.sin(var11) * var3;
         Vector4f var16 = ArgbUtil.m_2298(ArgbUtil.m_2306(this.m_5686(var9), var5));
         Vector4f var17 = ArgbUtil.m_2298(ArgbUtil.m_2306(this.m_5686(var9 + 1), var5));
         this.m_5644(var1, var2, 0.0F, var4, 0.0F, var8);
         this.m_5644(var1, var2, var12, 0.0F, var13, var16);
         this.m_5644(var1, var2, var14, 0.0F, var15, var17);
         this.m_5644(var1, var2, 0.0F, var4, 0.0F, var8);
         var6 += 4;
      }

      return var6;
   }

   private void m_5665(TargetEspFrame var1, float var2, Vec3 var3) {
      if (this.f_16220 < 0L) {
         this.f_16220 = System.currentTimeMillis();
      }

      Camera var4 = mc.gameRenderer.mainCamera();
      float var5 = this.f_16197.getValue();
      float var6 = this.f_16202.getValue();
      float var7 = this.f_16200.getValue();
      float var8 = this.f_16201.getValue();
      int var9 = this.f_16203.getValue().intValue();
      float var10 = this.f_16204.getValue();
      long var11 = System.currentTimeMillis();
      PoseStack var13 = var1.poseStack();
      BufferBuilder var14 = BufferPool.m_4161().m_4162(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      int var15 = 0;

      for (Player var17 : mc.level.players()) {
         if (this.m_5673(var17)) {
            ChinaHatModule.uDvg2RY38Z22x8A4 var18 = this.m_5655(var17, var2);
            this.m_5683(this.f_16215, var3, var17, var2, var18, 0.0F, this.m_5647(var17, var18, false));
            float var19 = var5 * var18.scale;
            float var20 = var6 * var18.scale;

            for (int var21 = 0; var21 < 2; var21++) {
               double var22 = var21 * Math.PI;

               for (int var24 = 0; var24 < var9; var24++) {
                  float var25 = (1.0F - (float)var24 / var9) * var8;
                  float var26 = var20 * (1.0F - (float)var24 / var9);
                  double var27 = (var11 - this.f_16220 - var24 * 20.0) / 1000.0;
                  double var29 = var27 * (var7 / 10.0F) + var22;
                  this.f_16214.set((float)(Math.sin(var29) * var19), 0.0F, (float)(Math.cos(var29) * var19), 1.0F);
                  this.f_16215.transform(this.f_16214);
                  float var31 = this.f_16214.x;
                  float var32 = this.f_16214.y;
                  float var33 = this.f_16214.z;
                  int var34 = (int)(var10 + var24 * 30);
                  int var35 = this.f_16206.getValue() ? ArgbUtil.m_2304(var34) : this.f_16207.getValue().getRGB();
                  int var36 = ArgbUtil.m_2306(var35, var25);
                  int var37 = ArgbUtil.m_2306(ArgbUtil.m_2302(var35, -1, 0.3F), var25);
                  var13.pushPose();
                  var13.translate(var31, var32, var33);
                  var13.mulPose(Axis.YP.rotationDegrees(-var4.yRot()));
                  var13.mulPose(Axis.XP.rotationDegrees(var4.xRot()));
                  Matrix4f var38 = var13.last().pose();
                  var15 += m_5675(var14, var38, var26, var36);
                  var15 += m_5675(var14, var38, var26 * 1.6F, var37);
                  var13.popPose();
               }
            }
         }
      }

      this.m_5643(var14, var15);
   }

   private int m_5666(BufferBuilder var1, Matrix4f var2, float var3, float var4, float var5) {
      int var6 = 0;
      float var7 = Math.max(0.01F, var3 - 0.030000001F);
      float var8 = 0.012F;

      for (int var9 = 0; var9 < 64; var9++) {
         float var10 = f_16403 * var9 / 64.0F;
         float var11 = f_16405 * (var9 + 1) / 64.0F;
         Vector4f var12 = ArgbUtil.m_2298(ArgbUtil.m_2306(this.m_5686(var9), var5));
         Vector4f var13 = ArgbUtil.m_2298(ArgbUtil.m_2306(this.m_5686(var9 + 1), var5));
         float var14 = Mth.cos(var10) * var3;
         float var15 = Mth.sin(var10) * var3;
         float var16 = Mth.cos(var11) * var3;
         float var17 = Mth.sin(var11) * var3;
         float var18 = Mth.cos(var10) * var7;
         float var19 = Mth.sin(var10) * var7;
         float var20 = Mth.cos(var11) * var7;
         float var21 = Mth.sin(var11) * var7;
         this.m_5644(var1, var2, var14, 0.0F, var15, var12);
         this.m_5644(var1, var2, var16, 0.0F, var17, var13);
         this.m_5644(var1, var2, var20, 0.0F, var21, var13);
         this.m_5644(var1, var2, var18, 0.0F, var19, var12);
         var6 += 4;
         if (var9 % 4 == 0) {
            float var22 = -Mth.sin(var10);
            float var23 = Mth.cos(var10);
            Vector4f var24 = ArgbUtil.m_2298(ArgbUtil.m_2306(this.m_5686(var9), var5));
            this.m_5644(var1, var2, var22 * var8, var4, var23 * var8, var24);
            this.m_5644(var1, var2, var14 + var22 * 0.012F, 0.0F, var15 + var23 * 0.012F, var12);
            this.m_5644(var1, var2, var14 - var22 * 0.012F, 0.0F, var15 - var23 * 0.012F, var12);
            this.m_5644(var1, var2, -var22 * var8, var4, -var23 * var8, var24);
            var6 += 4;
         }
      }

      return var6;
   }

   public void m_5668(TargetEspFrame var1) {
      if (mc.player != null && mc.level != null) {
         if ("Призраки".equals(this.f_16195.getValue())) {
            this.f_16195.setValue("Призраки 1");
         } else if ("Cyber".equals(this.f_16195.getValue())) {
            this.f_16195.setValue("Обычный");
         }

         float var2 = var1.deltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 var3 = var1.cameraPos();
         if (this.f_16195.is("Призраки 3")) {
            this.f_16213.clear();
            this.m_5665(var1, var2, var3);
         } else {
            this.f_16220 = -1L;
            float var4 = (float)Math.toRadians(((float)mc.level.getGameTime() + var2) * this.f_16200.getValue() * 0.05F);
            boolean var5 = this.f_16195.is("Призраки 2");
            if (!var5) {
               this.f_16213.clear();
            }

            BufferBuilder var6 = BufferPool.m_4161().m_4162(PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            int var7 = 0;
            HashSet var8 = var5 ? new HashSet() : null;
            PoseStack var9 = var1.poseStack();

            for (Player var11 : mc.level.players()) {
               if (this.m_5673(var11)) {
                  if (var5) {
                     var8.add(var11.getId());
                     var7 += this.m_5656(var6, var9, var3, var11, var2);
                  } else if (this.f_16195.is("Призраки 1")) {
                     var7 += this.m_5669(var6, var9, var3, var11, var2, var4);
                  } else {
                     var7 += this.m_5689(var6, var9, var3, var11, var2, var4);
                  }
               }
            }

            if (var8 != null) {
               this.f_16213.keySet().removeIf(var1x -> !var8.contains(var1x));
            }

            this.m_5680(var6, var7);
         }
      }
   }

   private int m_5669(BufferBuilder var1, PoseStack var2, Vec3 var3, Player var4, float var5, float var6) {
      float var7 = this.m_5653(var2, var3, var4, var5, var6);
      this.m_5670(var3, var4, var5, var6);
      Matrix4f var8 = var2.last().pose();
      float var9 = this.f_16197.getValue() * var7;
      float var10 = this.f_16198.getValue() * var7;
      float var11 = this.f_16201.getValue();
      float var12 = Mth.clamp(var9 * 0.5F, 0.26F, 0.42F);
      int var13 = 0;

      for (int var14 = 0; var14 < 6; var14++) {
         float var15 = f_16316 * var14 / 6.0F;
         float var16 = Mth.sin(var6 * 4.0F + var14 * 1.9F) * var10 * 0.35F;
         var13 += this.m_5660(
            var1, var8, Mth.cos(var15) * var9, var16, Mth.sin(var15) * var9, var12, ArgbUtil.m_2306(this.m_5686(var14 * 8), var11)
         );
      }

      var2.popPose();
      return var13;
   }

   private void m_5670(Vec3 var1, Player var2, float var3, float var4) {
      ChinaHatModule.uDvg2RY38Z22x8A4 var5 = this.m_5655(var2, var3);
      this.m_5683(this.f_16215, var1, var2, var3, var5, var4, this.m_5647(var2, var5, false));
      this.f_16215.invert();
      this.f_16214.set(0.0F, 0.0F, 0.0F, 1.0F);
      this.f_16215.transform(this.f_16214);
   }

   private boolean m_5673(Player var1) {
      if (var1 != mc.player) {
         return tokyo.ghoul.api.system.configs.FriendManager.getInstance().contains(var1.getName().getString()) ? this.f_16196.isEnabled("Друзей") : this.f_16196.isEnabled("Игроков");
      } else if (!this.f_16196.isEnabled("Себя")) {
         return false;
      } else {
         return !mc.options.getCameraType().isFirstPerson() ? true : this.f_16195.is("Призраки 3") && this.f_16205.getValue();
      }
   }

   private static int m_5675(BufferBuilder var0, Matrix4f var1, float var2, int var3) {
      Vector4f var4 = ArgbUtil.m_2298(var3);
      var0.addVertex(var1, -var2, -var2, 0.0F).setUv(0.0F, 0.0F).setColor(var4.x, var4.y, var4.z, var4.w);
      var0.addVertex(var1, -var2, var2, 0.0F).setUv(0.0F, 1.0F).setColor(var4.x, var4.y, var4.z, var4.w);
      var0.addVertex(var1, var2, var2, 0.0F).setUv(1.0F, 1.0F).setColor(var4.x, var4.y, var4.z, var4.w);
      var0.addVertex(var1, var2, -var2, 0.0F).setUv(1.0F, 0.0F).setColor(var4.x, var4.y, var4.z, var4.w);
      return 4;
   }

   private void m_5676(BufferBuilder var1, int var2, RenderPipeline var3, String var4) {
      MeshData var5 = var1.build();
      if (var5 != null) {
         if (var2 == 0) {
            var5.close();
         } else {
            GpuBuffer var6 = null;

            try {
               DrawState var7 = var5.drawState();
               int var8 = var7.indexCount();
               AutoStorageIndexBuffer var9 = RenderSystem.getSequentialBuffer(var7.primitiveTopology());
               GpuBuffer var10 = var9.getBuffer(var8);
               IndexType var11 = var9.type();
               var6 = RenderSystem.getDevice().createBuffer(() -> var4 + "", 32, var5.vertexBuffer());
               RenderTarget var12 = mc.gameRenderer.mainRenderTarget();
               CommandEncoder var13 = RenderSystem.getDevice().createCommandEncoder();
               RenderPass var14 = var13.createRenderPass(
                  () -> var4, var12.getColorTextureView(), Optional.empty(), var12.getDepthTextureView(), OptionalDouble.empty()
               );

               try {
                  RenderSystem.bindDefaultUniforms(var14);
                  var14.setPipeline(var3);
                  var14.setVertexBuffer(0, var6.slice());
                  var14.setIndexBuffer(var10, var11);
                  var14.drawIndexed(var8, 1, 0, 0, 0);
               } catch (Throwable var22) {
                  if (var14 != null) {
                     try {
                        var14.close();
                     } catch (Throwable var21) {
                        var22.addSuppressed(var21);
                     }
                  }

                  throw var22;
               }

               if (var14 != null) {
                  var14.close();
               }
            } finally {
               if (var6 != null) {
                  var6.close();
               }

               var5.close();
            }
         }
      }
   }

   private LinkedList<Vec3>[] m_5678(int var1) {
      return this.f_16213.computeIfAbsent(var1, var0 -> {
         LinkedList[] var1x = new LinkedList[5];

         for (int var2 = 0; var2 < var1x.length; var2++) {
            var1x[var2] = new LinkedList();
         }

         return var1x;
      });
   }

   private void m_5680(BufferBuilder var1, int var2) {
      this.m_5676(var1, var2, this.f_16208, "chinaHat");
   }

   private void m_5683(Matrix4f var1, Vec3 var2, Player var3, float var4, ChinaHatModule.uDvg2RY38Z22x8A4 var5, float var6, float var7) {
      Vec3 var8 = m_5661(var3, var4);
      var1.translation((float)(var8.x - var2.x), (float)(var8.y - var2.y), (float)(var8.z - var2.z));
      var1.rotate(Axis.YP.rotationDegrees(-var5.bodyYaw));
      if (var5.xRot != 0.0F) {
         var1.rotate(Axis.XP.rotationDegrees(var5.xRot));
      }

      if (var5.swimShift > 0.0F) {
         var1.translate(0.0F, -1.0F * var5.swimShift * var5.scale, 0.3F * var5.swimShift * var5.scale);
      }

      var1.translate(0.0F, var7, 0.0F);
      if (var6 != 0.0F) {
         var1.rotate(Axis.YP.rotation(var6));
      }
   }

   private int m_5686(int var1) {
      if (!this.f_16195.is("Градиент") && (!this.m_5690() || !this.f_16206.getValue())) {
         return this.f_16206.getValue() ? tokyo.ghoul.api.utils.color.UIColors.primary().getRGB() : this.f_16207.getValue().getRGB();
      } else {
         return ArgbUtil.m_2304(var1 * 3);
      }
   }

   private void m_5688(PoseStack var1, Vec3 var2, Player var3, float var4, ChinaHatModule.uDvg2RY38Z22x8A4 var5, float var6, float var7) {
      Vec3 var8 = m_5661(var3, var4);
      var1.translate(var8.x - var2.x, var8.y - var2.y, var8.z - var2.z);
      var1.mulPose(Axis.YP.rotationDegrees(-var5.bodyYaw));
      if (var5.xRot != 0.0F) {
         var1.mulPose(Axis.XP.rotationDegrees(var5.xRot));
      }

      if (var5.swimShift > 0.0F) {
         var1.translate(0.0F, -1.0F * var5.swimShift * var5.scale, 0.3F * var5.swimShift * var5.scale);
      }

      var1.translate(0.0F, var7, 0.0F);
      if (var6 != 0.0F) {
         var1.mulPose(Axis.YP.rotation(var6));
      }
   }

   private int m_5689(BufferBuilder var1, PoseStack var2, Vec3 var3, Player var4, float var5, float var6) {
      float var7 = this.m_5653(var2, var3, var4, var5, var6);
      Matrix4f var8 = var2.last().pose();
      float var9 = this.f_16197.getValue() * var7;
      float var10 = this.f_16198.getValue() * var7;
      float var11 = this.f_16201.getValue();
      int var12 = this.f_16195.is("Контур") ? this.m_5666(var1, var8, var9, var10, var11) : this.m_5664(var1, var8, var9, var10, var11);
      var2.popPose();
      return var12;
   }

   private boolean m_5690() {
      return this.f_16195.is("Призраки 1") || this.f_16195.is("Призраки 2") || this.f_16195.is("Призраки 3");
   }

   private record uDvg2RY38Z22x8A4(float bodyYaw, float xRot, float swimShift, float scale) {
   }

   @Override
   public void onEvent() {
   }

   @Getter private static final ChinaHatModule instance = new ChinaHatModule();
}
