package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.RenderLinesHelper;

public final class CubesModule extends ClientModule {
   private static final long FADE_MS = 500L;
   private Object lastLevel;
   public static final CubesModule INSTANCE = new CubesModule();
   private final RenderLinesHelper renderer;
   private final NumberSetting size;
   private final ColorSetting customColor;
   private final ModeSetting colorMode;
   private final NumberSetting amount = this.add(new NumberSetting("Amount", 100.0, 1.0, 100.0, 1.0));
   private final List<CubesModule.Particle> particles;
   private static final int MAX_PARTICLES = 100;

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         if (instance.level != this.lastLevel) {
            this.particles.clear();
            this.lastLevel = instance.level;
         }

         long nowNanos = System.nanoTime();
         this.particles.removeIf(particle -> {
            return particle.expired(nowNanos);
         });
         int computedValue = Math.min(100, this.amount.get().intValue());

         while (this.particles.size() > computedValue) {
            this.particles.remove(0);
         }

         for (CubesModule.Particle particle2 : this.particles) {
            particle2.tick();
         }

         if (this.particles.size() < computedValue) {
            ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
            Vec3 position = instance.player.position().add(threadLocalRandom.nextDouble(-20.0, 20.0), threadLocalRandom.nextDouble(0.0, 5.0), threadLocalRandom.nextDouble(-20.0, 20.0));
            Vec3 position2 = new Vec3(threadLocalRandom.nextDouble(-1.0, 1.0) * 0.04, threadLocalRandom.nextDouble(0.0, 2.0) * 0.04, threadLocalRandom.nextDouble(-1.0, 1.0) * 0.04);
            Vec3 position3 = new Vec3(threadLocalRandom.nextDouble(-1.0, 1.0) * 0.04, threadLocalRandom.nextDouble(-1.0, 1.0) * 0.04, threadLocalRandom.nextDouble(-1.0, 1.0) * 0.04);
            long timestamp = threadLocalRandom.nextLong(1500L, 4501L);
            float randomValue = (float)threadLocalRandom.nextDouble(0.1, 0.3);
            this.particles.add(new CubesModule.Particle(position, position2, position3, randomValue, timestamp, nowNanos));
         }
      } else {
         this.reset();
      }
   }

   public void renderWorld() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null && instance.level == this.lastLevel) {
            float deltaTracker = Mth.clamp(instance.getDeltaTracker().getGameTimeDeltaPartialTick(true), 0.0F, 1.0F);
            long nowNanos = System.nanoTime();
            ArrayList elements = new ArrayList(this.particles.size());

            for (CubesModule.Particle particle : this.particles) {
               float angle = particle.alpha(nowNanos);
               if (!(angle <= 0.0F)) {
                  elements.add(
                     new RenderLinesHelper.Cube(
                        particle.previous.lerp(particle.position, deltaTracker),
                        particle.previousRotation.lerp(particle.rotation, deltaTracker),
                        particle.size * this.size.get().floatValue(),
                        angle
                     )
                  );
               }
            }

            int index = this.colorMode.is("Custom") ? this.customColor.get() : SkycoreClient.themes().accent();
            this.renderer.render(elements, index);
         }
      }
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private void reset() {
      this.particles.clear();
      this.lastLevel = null;
   }

   private CubesModule() {
      super("Cubes", "Floating glowing wireframe cubes", ModuleCategory.RENDER);
      this.size = this.add(new NumberSetting("Size", "Cube size multiplier", 1.0, 0.25, 3.0, 0.05, "x"));
      this.colorMode = this.add(new ModeSetting("Color", "Sync", "Sync", "Custom"));
      this.customColor = this.add(new ColorSetting("Custom Color", -1, false));
      this.particles = new ArrayList<>(100);
      this.renderer = new RenderLinesHelper();
      this.customColor.visibleWhen(() -> {
         return this.colorMode.is("Custom");
      });
   }

   static final class Particle {
      private Vec3 rotation;
      private Vec3 spin;
      private Vec3 previousRotation = Vec3.ZERO;
      private final float size;
      private final long lifetimeMs;
      private Vec3 previous;
      private final long birthNanos;
      private Vec3 position;
      private Vec3 velocity;

      private float alpha(long timestamp) {
         long timestamp2 = this.elapsed(timestamp);
         if (timestamp2 >= this.lifetimeMs) {
            return 0.0F;
         }

         float elapsed = (float)timestamp2 / 500.0F;
         return Math.min(1.0F, Math.min(elapsed, (float)(this.lifetimeMs - timestamp2) / 500.0F));
      }

      private Particle(Vec3 previous2, Vec3 velocity2, Vec3 spin2, float size2, long lifetimeMs2, long birthNanos2) {
         this.rotation = Vec3.ZERO;
         this.previous = previous2;
         this.position = previous2;
         this.velocity = velocity2;
         this.spin = spin2;
         this.size = size2;
         this.lifetimeMs = lifetimeMs2;
         this.birthNanos = birthNanos2;
      }

      private boolean expired(long timestamp) {
         return this.elapsed(timestamp) >= this.lifetimeMs;
      }

      private void tick() {
         this.previous = this.position;
         this.previousRotation = this.rotation;
         Vec3 position2 = this.position.add(this.velocity);
         this.position = position2;
         this.rotation = this.rotation.add(this.spin);
         this.velocity = this.velocity.scale(0.98);
         this.spin = this.spin.scale(0.98);
      }

      private long elapsed(long timestamp) {
         long timestamp2 = (timestamp - this.birthNanos) / 1000000L;
         return Math.max(0L, timestamp2);
      }
   }
}
