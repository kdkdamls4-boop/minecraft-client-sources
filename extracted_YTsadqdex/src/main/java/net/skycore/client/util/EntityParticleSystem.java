package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntityParticleSystem {
   private final WorldParticleRenderer renderer;
   private static final Identifier TEXTURE = Identifier.parse("skycore:icons/world_render/hex_orbiz.png");
   private final List<EntityParticleSystem.Particle> particles;
   private final List<EntityParticleSystem.Emitter> emitters = new ArrayList<>();
   private final List<WorldParticleRenderer.Sprite> sprites;
   private static final Identifier GLOW = Identifier.parse("skycore:icons/world_render/glow.png");
   private static final Identifier STAR = Identifier.parse("skycore:icons/world_render/star.png");

   public void clear() {
      this.emitters.clear();
      this.particles.clear();
      this.sprites.clear();
   }

   public void render(boolean condition, boolean condition2, float coordinate, float coordinate2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null) {
         float gameTimeDeltaPartialTick = instance.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         this.sprites.clear();
         Identifier texture = condition2 ? STAR : TEXTURE;

         for (EntityParticleSystem.Particle particle : this.particles) {
            float minimum = Math.min(particle.life, particle.age + gameTimeDeltaPartialTick);
            float computedValue = Math.clamp(Math.min(minimum / 3.0F, (particle.life - minimum) / (particle.life / 2)), 0.0F, 1.0F);
            computedValue = computedValue * computedValue * (3.0F - 2.0F * computedValue);
            Vec3 position = particle.previous.lerp(particle.position, gameTimeDeltaPartialTick);
            position = position.add(0.0, particle.size * 0.5, 0.0);
            float color = particle.size * coordinate;
            this.sprites
               .add(new WorldParticleRenderer.Sprite(texture, position, color, 0.0F, WriteHexByteHelper.withAlpha(particle.color, Math.round(computedValue * (condition2 ? 255 : 170)))));
            if (condition) {
               this.sprites
                  .add(new WorldParticleRenderer.Sprite(GLOW, position, color * 1.5F * coordinate2, 0.0F, WriteHexByteHelper.withAlpha(particle.color, Math.round(computedValue * 24.0F))));
               this.sprites
                  .add(new WorldParticleRenderer.Sprite(GLOW, position, color * 2.5F * coordinate2, 0.0F, WriteHexByteHelper.withAlpha(particle.color, Math.round(computedValue * 12.0F))));
            }
         }

         this.renderer.renderHexTotem(this.sprites);
      }
   }

   public EntityParticleSystem() {
      this.particles = new ArrayList<>();
      this.sprites = new ArrayList<>();
      this.renderer = new WorldParticleRenderer();
   }

   public void tick(boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      if (condition && instance.level != null) {
         for (EntityParticleSystem.Particle particle : this.particles) {
            particle.tick(instance);
         }

         this.particles.removeIf(particle2 -> {
            return particle2.age >= particle2.life;
         });
         ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();

         for (EntityParticleSystem.Emitter emitter : this.emitters) {
            Entity owner = emitter.owner;
            if (!owner.isRemoved() && owner.level() == instance.level) {
               for (int value = 0; value < 16; value++) {
                  double computedValue = threadLocalRandom.nextDouble() * 2.0 - 1.0;
                  double computedValue2 = threadLocalRandom.nextDouble() * 2.0 - 1.0;
                  double computedValue3 = threadLocalRandom.nextDouble() * 2.0 - 1.0;
                  if (!(computedValue * computedValue + computedValue2 * computedValue2 + computedValue3 * computedValue3 > 1.0) && this.particles.size() < 16384) {
                     Vec3 position = new Vec3(owner.getX(computedValue / 4.0), owner.getY(0.5 + computedValue2 / 4.0), owner.getZ(computedValue3 / 4.0));
                     this.particles.add(new EntityParticleSystem.Particle(position, new Vec3(computedValue, computedValue2 + 0.2, computedValue3), threadLocalRandom));
                  }
               }

               emitter.age++;
            } else {
               emitter.age = 30;
            }
         }

         this.emitters.removeIf(emitter2 -> {
            return emitter2.age >= 30;
         });
      } else {
         this.clear();
      }
   }

   public void emit(Entity entity) {
      var emitters2 = this.emitters;
      EntityParticleSystem.Emitter emitter = new EntityParticleSystem.Emitter(entity);
      emitters2.add(emitter);
   }

   static final class Emitter {
      int age;
      final Entity owner;

      public Emitter(Entity owner2) {
         this.owner = owner2;
      }
   }

   static final class Particle {
      Vec3 previous;
      boolean stopped;
      final int color;
      Vec3 position;
      final float size;
      int age;
      Vec3 velocity;
      final int life;

      public void tick(Minecraft minecraft) {
         this.previous = this.position;
         this.age++;
         if (!this.stopped) {
            Vec3 position2 = this.velocity.add(0.0, -0.008999999761581421, 0.0);
            AABB bounds = new AABB(
               this.position.x - this.size / 2.0F,
               this.position.y,
               this.position.z - this.size / 2.0F,
               this.position.x + this.size / 2.0F,
               this.position.y + this.size,
               this.position.z + this.size / 2.0F
            );
            Vec3 position3 = Entity.collideBoundingBox((Entity)null, position2, bounds, minecraft.level, List.of());
            this.position = this.position.add(position3);
            int y = position2.y < 0.0 && position2.y != position3.y ? 1 : 0;
            this.velocity = new Vec3(position2.x == position3.x ? position2.x : 0.0, position2.y == position3.y ? position2.y : 0.0, position2.z == position3.z ? position2.z : 0.0).scale(0.75);
            if (y != 0) {
               this.velocity = this.velocity.multiply(0.7, 1.0, 0.7);
               this.stopped = ((this.velocity.lengthSqr() <= 2.25E-4 ? 1 : 0) != 0);
            }
         }
      }

      public Particle(Vec3 previous2, Vec3 velocity2, ThreadLocalRandom size2) {
         this.position = this.previous = previous2;
         this.velocity = velocity2;
         this.life = 80 + size2.nextInt(40);
         this.size = size2.nextFloat(0.2F, 0.25F);
         int value = size2.nextInt(4) == 0 ? 1 : 0;
         float computedValue = (value != 0 ? 0.6F : 0.1F) + size2.nextFloat() * 0.2F;
         float computedValue2 = (value != 0 ? 0.6F : 0.4F) + size2.nextFloat() * 0.3F;
         float computedValue3 = size2.nextFloat() * 0.2F;
         this.color = 0xFF000000 | (int)(computedValue * 255.0F) << 16 | (int)(computedValue2 * 255.0F) << 8 | (int)(computedValue3 * 255.0F);
      }
   }
}
