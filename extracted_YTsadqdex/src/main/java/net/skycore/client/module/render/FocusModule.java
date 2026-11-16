package net.skycore.client.module.render;

import java.util.Optional;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.FocusBlurRenderer;

public final class FocusModule extends ClientModule {
   private final FocusBlurRenderer renderer = new FocusBlurRenderer(this);
   private static final long FOCUS_DELAY_NS = 1100000000L;
   private float focusRadius;
   private boolean focusReady;
   private long targetSince;
   private float focusDistance;
   private long targetKey = Long.MIN_VALUE;
   private int targetKind;

   private void clearTarget() {
      this.targetKind = 0;
      this.targetKey = Long.MIN_VALUE;
      this.targetSince = 0L;
      this.focusReady = false;
   }

   public boolean focusReady() {
      return this.focusReady;
   }

   public float focusRadius() {
      return this.focusRadius;
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   public FocusBlurRenderer renderer() {
      return this.renderer;
   }

   public static FocusModule enabled() {
      var optional = SkycoreClient.modules().get(FocusModule.class);
      var optional2 = optional.filter(ClientModule::isEnabled);
      return (FocusModule)optional2.orElse(null);
   }

   public FocusModule() {
      super("Focus", "Depth-of-field autofocus for the object under the crosshair", ModuleCategory.RENDER);
      this.focusRadius = 0.18F;
   }

   private float entityRadius(Entity entity, Vec3 position) {
      if (entity instanceof ItemEntity) {
         return 0.085F;
      }

      Vec3 position2;
      focusDecision: {
         Minecraft instance = Minecraft.getInstance();
         if (instance.gameRenderer.mainCamera() != null) {
            if (instance.gameRenderer.mainCamera().isInitialized()) {
               position2 = instance.gameRenderer.mainCamera().position();
               break focusDecision;
            }
         }

         if (instance.player != null) {
            position2 = instance.player.getEyePosition();
         } else {
            position2 = position;
         }
      }

      Vec3 position3 = position2;
      float distance = (float)position3.distanceTo(position);
      float maximum = Math.max(1.0F, distance);
      float bbWidth = entity.getBbWidth();
      float bbHeight = Math.max(bbWidth, entity.getBbHeight());
      float computedValue = 0.075F + bbHeight / maximum * 0.32F;
      return clamp(computedValue, 0.1F, 0.24F);
   }

   @Override
   protected void onDisable() {
      this.clearTarget();
      this.renderer.release();
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null && instance.gui.screen() == null) {
         HitResult hitResult = instance.hitResult;
         if (hitResult != null && hitResult.getType() != Type.MISS) {
            byte flag;
            long timestamp;
            float radius;
            if (hitResult instanceof EntityHitResult entityHitResult) {
               Entity entity = entityHitResult.getEntity();
               flag = 2;
               timestamp = entity.getId();
               radius = this.entityRadius(entity, hitResult.getLocation());
            } else {
               if (!(hitResult instanceof BlockHitResult blockHitResult)) {
                  this.clearTarget();
                  return;
               }

               flag = 1;
               timestamp = blockHitResult.getBlockPos().asLong();
               radius = 0.19F;
            }

            long nowNanos = System.nanoTime();
            if (flag != this.targetKind || timestamp != this.targetKey) {
               this.targetKind = flag;
               this.targetKey = timestamp;
               this.targetSince = nowNanos;
               this.focusReady = false;
            }

            Camera camera = instance.gameRenderer.mainCamera();
            Vec3 eyePosition = camera != null && camera.isInitialized() ? camera.position() : instance.player.getEyePosition();
            this.focusDistance = Math.max(0.1F, (float)eyePosition.distanceTo(hitResult.getLocation()));
            this.focusRadius = radius;
            this.focusReady = ((nowNanos - this.targetSince >= 1100000000L ? 1 : 0) != 0);
         } else {
            this.clearTarget();
         }
      } else {
         this.clearTarget();
      }
   }

   @Override
   protected void onEnable() {
      this.clearTarget();
      this.renderer.reset();
   }

   public float focusDistance() {
      return this.focusDistance;
   }
}
