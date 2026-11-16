package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.SampleSurfaceHelper;

public final class JumpCirclesModule extends ClientModule {
   private final NumberSetting distortion;
   private final NumberSetting appearanceSpeed = this.add(
      new NumberSetting("Appearance Speed", "How fast jump circles unfold and fade", 1.0, 0.4, 3.0, 0.1, "x")
   );
   private final SampleSurfaceHelper renderer;
   private boolean wasGrounded;
   private Vec3 lastGroundFeet;
   private static final int MAX_CIRCLES = 8;
   private final Random random;
   private Object lastLevel;
   public static final JumpCirclesModule INSTANCE = new JumpCirclesModule();
   private final NumberSetting radius = this.add(new NumberSetting("Radius", "Maximum radius in blocks", 1.35, 0.5, 3.5, 0.05, " blocks"));
   private final List<SampleSurfaceHelper.Circle> circles;

   public void renderWorld() {
      if (this.isEnabled() && !this.circles.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.level == this.lastLevel && instance.player != null) {
            long nowNanos = System.nanoTime();
            double index = 1.25E9 / this.appearanceSpeed.get();
            ArrayList elements = new ArrayList(this.circles.size());

            for (SampleSurfaceHelper.Circle circle : this.circles) {
               float radius2 = (float)Math.min(1.0, (nowNanos - circle.startedNanos()) / index);
               if (!(radius2 >= 1.0F) && !(circle.position().distanceToSqr(instance.player.position()) > 6400.0)) {
                  float computedValue = smoother(Math.min(1.0F, radius2 / 0.055F));
                  float computedValue2 = 1.0F - smoother(Math.max(0.0F, Math.min(1.0F, (radius2 - 0.58F) / 0.42F)));
                  float radius3 = computedValue * computedValue2;
                  if (!(radius3 < 0.003F)) {
                     elements.add(new SampleSurfaceHelper.Instance(circle, this.radius.get().floatValue(), radius2, radius3));
                  }
               }
            }

            if (!elements.isEmpty()) {
               this.renderer.render(elements, SkycoreClient.themes().accent(), this.distortion.get().floatValue(), nowNanos);
            }
         } else {
            this.reset();
         }
      }
   }

   private JumpCirclesModule() {
      super("JumpCircles", "A hand-drawn theme-coloured jump ring with refracted ground", ModuleCategory.RENDER);
      this.distortion = this.add(new NumberSetting("Distortion Strength", "Refraction inside the hand-drawn circle", 0.45, 0.0, 1.0, 0.05, "x"));
      this.circles = new ArrayList<>();
      this.renderer = new SampleSurfaceHelper();
      this.random = new Random();
   }

   @Override
   protected void onDisable() {
      this.reset();
      this.renderer.release();
   }

   private static float smoother(float value) {
      return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
   }

   private void reset() {
      this.circles.clear();
      this.lastLevel = null;
      this.wasGrounded = false;
      this.lastGroundFeet = null;
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level == null || instance.player == null) {
         this.reset();
      } else if (this.lastLevel != instance.level) {
         this.reset();
         this.lastLevel = instance.level;
         this.wasGrounded = instance.player.onGround();
         if (this.wasGrounded) {
            this.lastGroundFeet = instance.player.position();
         }
      } else {
         boolean onGround = instance.player.onGround();
         if (this.wasGrounded && !onGround && this.lastGroundFeet != null && instance.player.getDeltaMovement().y > 0.055) {
            if (this.circles.size() >= 8) {
               this.circles.remove(0);
            }

            this.circles.add(new SampleSurfaceHelper.Circle(this.lastGroundFeet.add(0.0, 0.022, 0.0), System.nanoTime(), this.random.nextFloat() * 1000.0F));
         }

         if (onGround && instance.player.getDeltaMovement().y <= 0.02) {
            this.lastGroundFeet = instance.player.position();
         }

         this.wasGrounded = onGround;
         long nowNanos = System.nanoTime();
         double computedValue = 1.25E9 / this.appearanceSpeed.get();
         this.circles.removeIf(circle -> {
            return nowNanos - circle.startedNanos() >= computedValue;
         });
      }
   }

   @Override
   protected void onEnable() {
      Minecraft minecraft;
      byte flag;
      jumpDetection: {
         flag = -1;
         flag = 1;
         flag = -1;
         minecraft = Minecraft.getInstance();
         this.circles.clear();
         this.lastLevel = minecraft.level;
         if (minecraft.player != null) {
            if (minecraft.player.onGround()) {
               flag = 1;
               break jumpDetection;
            }
         }

         flag = 0;
      }

      this.wasGrounded = (flag != 0);
      Vec3 position;
      if (this.wasGrounded) {
         position = minecraft.player.position();
      } else {
         position = null;
      }

      this.lastGroundFeet = position;
   }
}
