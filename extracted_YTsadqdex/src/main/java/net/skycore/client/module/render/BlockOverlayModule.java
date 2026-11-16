package net.skycore.client.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.CurrentHelper;
import net.skycore.client.util.RenderSkyHelper;

public final class BlockOverlayModule extends ClientModule {
   private final NumberSetting shaderScale;
   private final CurrentHelper animation;
   private RenderSkyHelper renderer;
   private final ModeSetting mode = this.add(new ModeSetting("Mode", "Default", "Default", "Shader"));

   private static int withAlpha(int value, double value2) {
      double computedValue = Math.max(0.0, Math.min(1.0, value2)) * 255.0;
      int roundedValue = (int)Math.round(computedValue);
      return roundedValue << 24 | value & 16777215;
   }

   private void release() {
      this.animation.clear();
      if (this.renderer != null) {
         this.renderer.close();
         this.renderer = null;
      }
   }

   @Override
   protected void onDisable() {
      this.release();
   }

   @Override
   protected void onTick() {
      if (Minecraft.getInstance().level == null) {
         this.release();
      }
   }

   public BlockOverlayModule() {
      super("Block Overlay", "Block selection overlay", ModuleCategory.RENDER);
      this.shaderScale = this.add(new NumberSetting("Shader Scale", "", 1.0, 0.25, 3.0, 0.05, "x"));
      this.animation = new CurrentHelper();
      this.shaderScale.visibleWhen(() -> {
         return this.mode.is("Shader");
      });
   }

   public void render(CameraRenderState cameraRenderState) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null
            && instance.player != null
            && instance.hitResult instanceof BlockHitResult blockHitResult
            && blockHitResult.getType() == Type.BLOCK
            && !instance.level.isEmptyBlock(blockHitResult.getBlockPos())) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            VoxelShape shape = instance.level.getBlockState(blockPos).getShape(instance.level, blockPos);
            AABB bounds = shape.isEmpty() ? new AABB(blockPos) : shape.bounds().move(blockPos);
            AABB bounds2 = this.animation.update(blockPos, bounds, System.currentTimeMillis());
            int alpha = SkycoreClient.themes().accent();
            if (this.mode.is("Shader")) {
               if (this.renderer == null) {
                  this.renderer = new RenderSkyHelper();
               }

               this.renderer.renderBlock(bounds2, cameraRenderState, alpha, this.shaderScale.get().floatValue());
            } else {
               int value = withAlpha(alpha, 0.55);
               int value2 = withAlpha(alpha, 0.1);
               AddQuadHelper.beginFrame();
               AddQuadHelper.addFilledBox(bounds2.inflate(0.001), value2);
               AddQuadHelper.addScreenSpaceBoxOutline(bounds2.inflate(0.002), value);
               AddQuadHelper.endFrame();
            }
         } else {
            this.animation.clear();
         }
      }
   }
}
