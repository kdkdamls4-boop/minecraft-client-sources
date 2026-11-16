package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class RenderWorldHelper {
   private final WorldParticleRenderer renderer;
   private static final int MAX_FIREFLIES = 25;
   private final Random random;
   private final List<RenderWorldHelper.Firefly> flies = new ArrayList<>();
   private int ticks;
   private Object world;
   private static final Identifier GLOW = Identifier.parse("skycore:icons/world_render/bloom.png");

   public void reset() {
      this.flies.clear();
      this.world = null;
      this.ticks = 0;
   }

   public void tick(Minecraft world2, boolean condition, boolean condition2) {
      if (world2.level != null && world2.player != null) {
         if (this.world != world2.level) {
            this.reset();
            this.world = world2.level;
         }

         this.ticks++;
         long nowNanos = System.nanoTime();
         this.flies.removeIf(firefly -> {
            long timestamp = nowNanos - firefly.birth();
            return timestamp >= firefly.lifetime();
         });
         if (!condition) {
            this.flies.clear();
         }

         if (world2.level.dimension().identifier().getPath().equals("overworld")) {
            BlockPos blockPos = world2.player.blockPosition();
            long overworldClockTime = world2.level.getOverworldClockTime() % 24000L;
            int value = overworldClockTime > 13000L && overworldClockTime < 23000L ? 1 : 0;
            String biome = world2.level.getBiome(blockPos).unwrapKey().map(resourceKey -> {
               Identifier texture = resourceKey.identifier();
               return texture.getPath();
            }).orElse("");
            int value2 = !biome.contains("forest")
                  && !biome.contains("swamp")
                  && !biome.contains("jungle")
                  && !biome.contains("plains")
                  && !biome.contains("meadow")
                  && !biome.contains("grove")
               ? 0
               : 1;
            if (condition && value != 0 && value2 != 0 && !world2.level.isRaining() && this.ticks % 5 == 0 && this.flies.size() < 25) {
               double x = world2.player.getX() + (this.random.nextDouble() - 0.5) * 13.0;
               double z = world2.player.getZ() + (this.random.nextDouble() - 0.5) * 13.0;
               int height = world2.level.getHeight(Types.MOTION_BLOCKING, (int)Math.floor(x), (int)Math.floor(z));
               if (Math.abs(height - world2.player.getY()) < 5.0) {
                  this.flies
                     .add(
                        new RenderWorldHelper.Firefly(
                           new Vec3(x, height + 0.4 + this.random.nextDouble() * 2.0, z),
                           nowNanos,
                           (long)((5.0 + this.random.nextDouble() * 4.0) * 1.0E9),
                           this.random.nextFloat() * 6.28318F
                        )
                     );
               }
            }

            if (condition2 && world2.level.isRaining() && this.ticks % 3 == 0) {
               double x2 = world2.player.getX() + (this.random.nextDouble() - 0.5) * 14.0;
               double z2 = world2.player.getZ() + (this.random.nextDouble() - 0.5) * 14.0;
               BlockPos blockPos2 = new BlockPos(
                  (int)Math.floor(x2), world2.level.getHeight(Types.MOTION_BLOCKING, (int)Math.floor(x2), (int)Math.floor(z2)), (int)Math.floor(z2)
               );
               if (world2.level.hasChunkAt(blockPos2) && !world2.level.dimensionType().hasCeiling()) {
                  world2.level.addParticle(ParticleTypes.SPLASH, x2, blockPos2.getY() + 0.02, z2, 0.0, 0.015, 0.0);
               }
            }
         }
      } else {
         this.reset();
      }
   }

   public void renderWorld() {
      if (!this.flies.isEmpty()) {
         long nowNanos = System.nanoTime();
         ArrayList elements = new ArrayList(this.flies.size() * 2);

         for (RenderWorldHelper.Firefly firefly : this.flies) {
            double computedValue = (double)(nowNanos - firefly.birth()) / firefly.lifetime();
            if (!(computedValue < 0.0) && !(computedValue >= 1.0)) {
               double computedValue2 = (nowNanos - firefly.birth()) / 1.0E9;
               double computedValue3 = 0.6 + 0.4 * Math.sin(computedValue2 * 7.2 + firefly.phase());
               double computedValue4 = Math.min(1.0, Math.min(computedValue * 6.0, (1.0 - computedValue) * 5.0));
               int computedValue5 = Math.max(0, Math.min(180, (int)(computedValue4 * computedValue3 * 155.0)));
               Vec3 position = firefly.start()
                  .add(
                     Math.sin(computedValue2 * 0.84 + firefly.phase()) * 0.22,
                     Math.sin(computedValue2 * 0.63 + firefly.phase() * 2.0) * 0.18,
                     Math.cos(computedValue2 * 0.77 + firefly.phase()) * 0.22
                  );
               elements.add(new WorldParticleRenderer.Sprite(GLOW, position, 0.115F, 0.0F, computedValue5 << 24 | 16771454));
               elements.add(new WorldParticleRenderer.Sprite(GLOW, position, 0.039F, 0.0F, Math.min(255, computedValue5 + 40) << 24 | 16775386));
            }
         }

         this.renderer.render(elements, true);
      }
   }

   public RenderWorldHelper() {
      this.random = new Random();
      this.renderer = new WorldParticleRenderer();
   }

   public record Firefly(Vec3 start, long birth, long lifetime, float phase) {
      public float phase() {
         return this.phase;
      }

      public long birth() {
         return this.birth;
      }

      public Vec3 start() {
         return this.start;
      }

      public long lifetime() {
         return this.lifetime;
      }
   }
}
