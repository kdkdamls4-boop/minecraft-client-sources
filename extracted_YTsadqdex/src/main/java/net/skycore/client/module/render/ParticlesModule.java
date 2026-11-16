package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.mixin.AbstractArrowIsInGroundAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.Setting;
import net.skycore.client.util.WorldParticleRenderer;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.EntityParticleSystem;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.WriteHexByteHelper;

public final class ParticlesModule extends ClientModule {
   private final EntityParticleSystem hexTotem;
   private static final int MAX_ATTACK = 70;
   private final NumberSetting moveCount;
   private static final int MAX_TRAIL = 100;
   private final NumberSetting size;
   private double lastPlayerY;
   private static final int MAX_WORLD = 90;
   private final List<ParticlesModule.Particle> worldParticles;
   private static final double BASE_VELOCITY = 0.05;
   private final ModeSetting totemIcon;
   private static final int MAX_TOTAL = 240;
   private final WorldParticleRenderer renderer;
   private double lastPlayerZ;
   private final NumberSetting totemSize;
   private final ModeSetting colorMode;
   private boolean positionInitialized;
   private final List<ParticlesModule.Particle> trailParticles;
   public static final ParticlesModule INSTANCE = new ParticlesModule();
   private final NumberSetting speed;
   private final BooleanSetting glow;
   private final NumberSetting idleRange;
   private final NumberSetting throwCount;
   private final MultiSelectSetting events = this.add(
      new MultiSelectSetting("Spawn On", List.of("Attack", "Throw", "Totem", "Move", "Idle"), Set.of("Attack", "Throw", "Totem", "Idle"))
   );
   private final NumberSetting totemGlowSize;
   private final List<WorldParticleRenderer.Sprite> spriteBuffer;
   private final BooleanSetting totemGlow;
   private double lastPlayerX;
   private final NumberSetting idleCount;
   private long lastFrameNanos;
   private final ColorSetting customColor;
   private final BooleanSetting rotation;
   private final NumberSetting attackCount;
   private final List<ParticlesModule.Particle> attackParticles;
   private final ModeSetting particleType = this.add(
      new ModeSetting("Particle Type", "Bloom", "Bloom", "Star", "Heart", "Dollar", "Snow", "Sparkle", "Pumpkin", "Random")
   );

   private static double random(double value, double value2) {
      return ThreadLocalRandom.current().nextDouble(value, value2);
   }

   @Override
   protected void onDisable() {
      this.clear();
   }

   private void collect(List<WorldParticleRenderer.Sprite> sprites, Vec3 position, List<ParticlesModule.Particle> particles, boolean condition) {
      for (ParticlesModule.Particle particle : particles) {
         if (!(position.distanceToSqr(particle.position) < 0.25)) {
            float value = particle.alpha(System.nanoTime());
            if (!(value <= 0.01F)) {
               if (condition) {
                  int color = Math.max(0, Math.min(110, Math.round(value * 110.0F)));
                  sprites.add(
                     new WorldParticleRenderer.Sprite(
                        Identifier.parse("skycore:icons/world_render/glow.png"),
                        particle.position,
                        particle.size * 2.0F,
                        particle.rotation,
                        WriteHexByteHelper.withAlpha(particle.color, color)
                     )
                  );
               } else {
                  sprites.add(
                     new WorldParticleRenderer.Sprite(
                        particle.texture, particle.position, particle.size, particle.rotation, WriteHexByteHelper.withAlpha(particle.color, Math.round(value * 255.0F))
                     )
                  );
               }
            }
         }
      }
   }

   private void spawnIdleParticles(LocalPlayer player) {
      Minecraft.getInstance();
      if (this.worldParticles.size() < 90) {
         int value = this.idleRange.get().intValue();
         int computedValue = Math.min(this.idleCount.get().intValue(), 90 - this.worldParticles.size());
         Vec3 lookAngle = player.getLookAngle();
         Vec3 position = new Vec3(lookAngle.x, 0.0, lookAngle.z);
         if (position.lengthSqr() < 1.0E-6) {
            position = new Vec3(0.0, 0.0, 1.0);
         } else {
            position = position.normalize();
         }

         Vec3 position2 = new Vec3(-position.z, 0.0, position.x);

         for (int value2 = 0; value2 < computedValue; value2++) {
            double computedValue2 = random(Math.max(2.0, value * 0.18), value);
            double randomValue = random(-computedValue2 * 0.72, computedValue2 * 0.72);
            Vec3 position3 = position.scale(computedValue2).add(position2.scale(randomValue));
            double x = player.getX() + position3.x;
            double z = player.getZ() + position3.z;
            double y = player.getY() + random(0.25, 3.25);
            Vec3 position4 = new Vec3(x, y, z);

            for (int value3 = 0; value3 < 4 && solid(position4); value3++) {
               position4 = position4.add(0.0, 1.0, 0.0);
            }

            if (!solid(position4)) {
               Vec3 deltaMovement = player.getDeltaMovement();
               Vec3 position5 = new Vec3(deltaMovement.x + random(-0.5, 0.5), random(-0.06, 0.06), deltaMovement.z + random(-0.5, 0.5));
               this.spawnIdle(this.worldParticles, position4, position5, this.resolveColor(this.worldParticles.size()));
            }
         }
      }
   }

   private static void trimList(List<ParticlesModule.Particle> particles, int value) {

      while (true) {
         if (particles.size() <= value) {
            return;
         }

         particles.removeFirst();
      }
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         if (this.events.isSelected("Idle")) {
            this.spawnIdleParticles(instance.player);
         }

         if (this.events.isSelected("Move")) {
            this.spawnMoveParticles(instance.player);
         }

         if (this.events.isSelected("Throw")) {
            this.spawnThrowParticles(instance.player);
         }

         this.hexTotem.tick(this.events.isSelected("Totem"));
         this.trim();
      } else {
         this.clear();
      }
   }

   private boolean hasGeneralEvents() {
      if (!this.events.isSelected("Attack") && !this.events.isSelected("Throw")) {
         if (!this.events.isSelected("Move")) {
            if (!this.events.isSelected("Idle")) {
               return false;
            }
         }
      }

      return true;
   }

   private void updateList(List<ParticlesModule.Particle> particles, double index, long timestamp) {

      for (ParticlesModule.Particle particle : particles) {
         Double value = this.speed.get();
         double value2 = value;
         boolean get = this.rotation.get();
         particle.update(index, timestamp, value2, get);
      }
   }

   private static boolean solid(Vec3 position) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level == null) {
         return false;
      }

      BlockPos blockPos = BlockPos.containing(position);
      BlockState blockState = instance.level.getBlockState(blockPos);
      return !blockState.getCollisionShape(instance.level, blockPos).isEmpty();
   }

   private void spawnIdle(List<ParticlesModule.Particle> particles, Vec3 position, Vec3 position2, int value) {
      if (this.totalCount() < 240) {
         float computedValue = 0.05F + this.size.get().floatValue() * 0.2F;
         Identifier texture = this.resolveTexture();
         Vec3 position3 = position.add(0.0, computedValue, 0.0);
         Vec3 position4 = position2.scale(0.05);
         particles.add(new ParticlesModule.Particle(texture, position3, position4, value, computedValue, false, 0L, true));
      }
   }

   public void render() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null) {
            long nowNanos = System.nanoTime();
            double computedValue = this.lastFrameNanos == 0L ? 0.016666666666666666 : Math.min(0.05, (nowNanos - this.lastFrameNanos) / 1.0E9);
            this.lastFrameNanos = nowNanos;
            this.updateList(this.attackParticles, computedValue, nowNanos);
            this.updateList(this.worldParticles, computedValue, nowNanos);
            this.updateList(this.trailParticles, computedValue, nowNanos);
            this.updateIdleVisibility(instance.player, computedValue);
            this.attackParticles.removeIf(particle -> {
               return particle.expired(nowNanos, 2500L);
            });
            this.worldParticles.removeIf(particle2 -> {
               boolean condition;
               if (particle2.idleParticle) {
                  condition = particle2.idleExpired(nowNanos);
               } else {
                  condition = particle2.expired(nowNanos, 2000L);
               }

               return condition;
            });
            this.trailParticles.removeIf(particle3 -> {
               return particle3.expired(nowNanos, 2000L);
            });
            Vec3 position = instance.gameRenderer.mainCamera().position();
            if (this.glow.get()) {
               this.spriteBuffer.clear();
               this.collect(this.spriteBuffer, position, this.attackParticles, true);
               this.collect(this.spriteBuffer, position, this.worldParticles, true);
               this.collect(this.spriteBuffer, position, this.trailParticles, true);
               this.renderer.render(this.spriteBuffer, true);
            }

            this.spriteBuffer.clear();
            this.collect(this.spriteBuffer, position, this.attackParticles, false);
            this.collect(this.spriteBuffer, position, this.worldParticles, false);
            this.collect(this.spriteBuffer, position, this.trailParticles, false);
            this.renderer.render(this.spriteBuffer, true);
            this.hexTotem.render(this.totemGlow.get(), this.totemIcon.is("Star"), this.totemSize.get().floatValue(), this.totemGlowSize.get().floatValue());
         } else {
            this.clear();
         }
      }
   }

   private boolean hasPlayerMoved(LocalPlayer lastPlayerX2) {
      if (!this.positionInitialized) {
         this.lastPlayerX = lastPlayerX2.getX();
         this.lastPlayerY = lastPlayerX2.getY();
         this.lastPlayerZ = lastPlayerX2.getZ();
         this.positionInitialized = true;
         return false;
      } else {
         int x = !(Math.abs(lastPlayerX2.getX() - this.lastPlayerX) > 0.001)
               && !(Math.abs(lastPlayerX2.getY() - this.lastPlayerY) > 0.001)
               && !(Math.abs(lastPlayerX2.getZ() - this.lastPlayerZ) > 0.001)
            ? 0
            : 1;
         this.lastPlayerX = lastPlayerX2.getX();
         this.lastPlayerY = lastPlayerX2.getY();
         this.lastPlayerZ = lastPlayerX2.getZ();
         return (x != 0);
      }
   }

   public void onAttack(Entity entity) {
      if (this.isEnabled() && this.events.isSelected("Attack") && entity != null && !entity.isRemoved()) {
         int index = Math.min(this.attackCount.get().intValue(), 70 - this.attackParticles.size());

         for (int value = 0; value < index; value++) {
            double randomValue = random(0.0, Math.PI * 2);
            double randomValue2 = random(2.8, 4.2);
            double randomValue3 = random(0.75, 1.9);
            Vec3 position = new Vec3(
               entity.getX() + random(-0.28, 0.28), entity.getY() + random(entity.getBbHeight() * 0.35, entity.getBbHeight() * 0.95), entity.getZ() + random(-0.28, 0.28)
            );
            Vec3 position2 = new Vec3(Math.cos(randomValue) * randomValue2, randomValue3, Math.sin(randomValue) * randomValue2);
            this.spawn(
               this.attackParticles, position, position2, this.resolveColor(this.attackParticles.size()), true, ThreadLocalRandom.current().nextLong(500L, 601L)
            );
         }
      }
   }

   private int totalCount() {
      int value = 0;
      value = (byte)0;
      value = this.attackParticles.size();
      return value + this.worldParticles.size() + this.trailParticles.size();
   }

   private void updateIdleVisibility(LocalPlayer player, double value) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 lookAngle = player.getLookAngle();
      Vec3 position = new Vec3(lookAngle.x, 0.0, lookAngle.z);
      if (position.lengthSqr() < 1.0E-6) {
         position = new Vec3(0.0, 0.0, 1.0);
      } else {
         position = position.normalize();
      }

      float minimum = (float)Math.min(1.0, value * 4.5);

      for (ParticlesModule.Particle particle : this.worldParticles) {
         if (particle.idleParticle) {
            Vec3 position2 = particle.position.subtract(eyePosition);
            Vec3 position3 = new Vec3(position2.x, 0.0, position2.z);
            int computedValue = !(position3.lengthSqr() < 1.0E-6) && !(position3.normalize().dot(position) > -0.05) ? 0 : 1;
            long timestamp = (System.nanoTime() - particle.bornNanos) / 1000000L;
            if (timestamp >= 6500L) {
               computedValue = 0;
            }

            particle.visibleTarget = computedValue != 0 ? 1.0F : 0.0F;
            particle.visibilityAlpha = particle.visibilityAlpha + (particle.visibleTarget - particle.visibilityAlpha) * minimum;
         }
      }
   }

   private void spawnMoveParticles(LocalPlayer player) {
      Minecraft instance = Minecraft.getInstance();
      if (this.hasPlayerMoved(player) && !instance.options.getCameraType().isFirstPerson() && this.trailParticles.size() < 100) {
         int index = Math.min(this.moveCount.get().intValue(), 100 - this.trailParticles.size());
         Vec3 deltaMovement = player.getDeltaMovement();

         for (int value = 0; value < index; value++) {
            Vec3 position = new Vec3(player.getX() + random(-0.5, 0.5), player.getY() + random(0.0, player.getBbHeight()), player.getZ() + random(-0.5, 0.5));
            Vec3 position2 = new Vec3(deltaMovement.x + random(-0.25, 0.25), random(-0.15, 0.15), deltaMovement.z + random(-0.25, 0.25));
            this.spawn(this.trailParticles, position, position2, this.resolveColor(this.trailParticles.size()), false, 0L);
         }
      }
   }

   private void clear() {
      this.attackParticles.clear();
      this.worldParticles.clear();
      this.trailParticles.clear();
      this.hexTotem.clear();
      this.spriteBuffer.clear();
      this.positionInitialized = false;
      this.lastFrameNanos = 0L;
   }

   private int resolveColor(int value) {
      int value2 = 0;
      if (this.colorMode.is("Custom")) {
         value2 = this.customColor.get();
      } else {
         value2 = SkycoreClient.themes().accent();
      }

      int value3 = value2;
      int computedValue = value3 | 0xFF000000;
      return (value & 3) == 0 ? WriteHexByteHelper.lerp(computedValue, -1, 0.2F) : computedValue;
   }

   private void spawnThrowParticles(LocalPlayer player) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && this.trailParticles.size() < 100) {
         double distance = 4096.0;
         int index = this.throwCount.get().intValue();

         for (Entity entity : instance.level.entitiesForRendering()) {
            if ((entity instanceof ThrownEnderpearl || entity instanceof AbstractArrow)
               && !(entity instanceof AbstractArrow abstractArrow && ((AbstractArrowIsInGroundAccessor)abstractArrow).skycore$isInGround())
               && !(entity.position().distanceToSqr(player.position()) > distance)
               && (entity.xo != entity.getX() || entity.yo != entity.getY() || entity.zo != entity.getZ())) {
               int index2 = Math.min(index, 100 - this.trailParticles.size());
               Vec3 bbHeight = entity.position().add(0.0, entity.getBbHeight() * 0.5, 0.0);

               for (int value = 0; value < index2; value++) {
                  this.spawn(
                     this.trailParticles,
                     bbHeight.add(random(-0.5, 0.5), random(-0.5, 0.5), random(-0.5, 0.5)),
                     new Vec3(random(-0.06, 0.06), random(-0.06, 0.06), random(-0.06, 0.06)),
                     this.resolveColor(this.trailParticles.size()),
                     false,
                     0L
                  );
               }

               if (this.trailParticles.size() >= 100) {
                  break;
               }
            }
         }
      }
   }

   private void spawn(List<ParticlesModule.Particle> particles, Vec3 position, Vec3 position2, int value, boolean condition, long timestamp) {
      if (this.totalCount() < 240) {
         float computedValue = 0.05F + this.size.get().floatValue() * 0.2F;
         Identifier texture = this.resolveTexture();
         Vec3 position3 = position.add(0.0, computedValue, 0.0);
         Vec3 position4 = position2.scale(0.05);
         ParticlesModule.Particle particle = new ParticlesModule.Particle(texture, position3, position4, value, computedValue, condition, timestamp, false);
         particles.add(particle);
      }
   }

   public void onEntityEvent(ClientboundEntityEventPacket clientboundEntityEventPacket) {
      if (this.isEnabled()) {
         if (this.events.isSelected("Totem")) {
            if (clientboundEntityEventPacket.getEventId() != 35) {
               return;
            }

            Minecraft instance = Minecraft.getInstance();
            if (instance.level == null) {
               return;
            }

            Entity entity = clientboundEntityEventPacket.getEntity(instance.level);
            if (entity == null) {
               return;
            }

            this.hexTotem.emit(entity);
            return;
         }
      }
   }

   private Identifier resolveTexture() {
      String text = this.particleType.get();
      if ("Random".equalsIgnoreCase(text)) {
         String[] stringValues = new String[]{"Bloom", "Star", "Heart", "Dollar", "Snow", "Sparkle", "Pumpkin"};
         text = stringValues[ThreadLocalRandom.current().nextInt(stringValues.length)];
      }
      String text2 = switch (text) {
         case "Bloom" -> "glow.png";
         case "Heart" -> "heart.png";
         case "Dollar" -> "dollar.png";
         case "Snow" -> "snowflake.png";
         case "Sparkle" -> "sparkle.png";
         case "Pumpkin" -> "pumpkin.png";
         default -> "star.png";
      };
      return Identifier.parse("skycore:icons/world_render/" + text2);
   }

   private void trim() {
      trimList(this.attackParticles, 70);
      trimList(this.worldParticles, 90);
      trimList(this.trailParticles, 100);

      while (this.totalCount() > 240) {
         if (!this.worldParticles.isEmpty()) {
            this.worldParticles.removeFirst();
         } else if (!this.trailParticles.isEmpty()) {
            this.trailParticles.removeFirst();
         } else {
            if (this.attackParticles.isEmpty()) {
               break;
            }

            this.attackParticles.removeFirst();
         }
      }
   }

   private ParticlesModule() {
      super("Particles", "Particles on attacks, projectiles, totems, movement and in the world", ModuleCategory.RENDER);
      this.speed = this.add(new NumberSetting("Speed", "", 1.5, 0.1, 3.0, 0.1, "x"));
      this.size = this.add(new NumberSetting("Size", "", 0.2, 0.05, 0.6, 0.01, ""));
      this.attackCount = this.add(new NumberSetting("Attack Count", 30.0, 5.0, 50.0, 1.0));
      this.moveCount = this.add(new NumberSetting("Move Count", 2.0, 1.0, 6.0, 1.0));
      this.throwCount = this.add(new NumberSetting("Throw Count", 6.0, 1.0, 16.0, 1.0));
      this.idleCount = this.add(new NumberSetting("Idle Count", 5.0, 1.0, 25.0, 1.0));
      this.idleRange = this.add(new NumberSetting("Idle Range", "", 16.0, 4.0, 32.0, 1.0, "m"));
      this.rotation = this.add(new BooleanSetting("Rotation", false));
      this.glow = this.add(new BooleanSetting("Glow", true));
      this.totemGlow = this.add(new BooleanSetting("Totem Glow", false));
      this.totemGlowSize = this.add(new NumberSetting("Totem Glow Size", "", 1.0, 0.25, 3.0, 0.05, "x"));
      this.totemSize = this.add(new NumberSetting("Totem Size", "", 1.0, 0.25, 2.0, 0.05, "x"));
      this.totemIcon = this.add(new ModeSetting("Totem Icon", "Star", "Star", "Bloom"));
      this.colorMode = this.add(new ModeSetting("Color", "Sync", "Sync", "Custom"));
      this.customColor = this.add(new ColorSetting("Custom Color", -1, false));
      this.attackParticles = new ArrayList<>();
      this.worldParticles = new ArrayList<>();
      this.trailParticles = new ArrayList<>();
      this.hexTotem = new EntityParticleSystem();
      this.spriteBuffer = new ArrayList<>(240);
      this.renderer = new WorldParticleRenderer();
      this.attackCount.visibleWhen(() -> {
         return this.events.isSelected("Attack");
      });
      this.totemGlow.visibleWhen(() -> {
         return this.events.isSelected("Totem");
      });
      this.totemGlowSize.visibleWhen(() -> {
         if (this.events.isSelected("Totem")) {
            if (this.totemGlow.get()) {
               return true;
            }
         }

         return false;
      });
      this.totemSize.visibleWhen(() -> {
         return this.events.isSelected("Totem");
      });
      this.totemIcon.visibleWhen(() -> {
         return this.events.isSelected("Totem");
      });

      for (Setting setting : List.of(this.particleType, this.speed, this.size, this.rotation, this.glow, this.colorMode)) {
         setting.visibleWhen(this::hasGeneralEvents);
      }

      this.moveCount.visibleWhen(() -> {
         return this.events.isSelected("Move");
      });
      this.throwCount.visibleWhen(() -> {
         return this.events.isSelected("Throw");
      });
      this.idleCount.visibleWhen(() -> {
         return this.events.isSelected("Idle");
      });
      this.idleRange.visibleWhen(() -> {
         return this.events.isSelected("Idle");
      });
      this.customColor.visibleWhen(() -> {
         if (this.hasGeneralEvents()) {
            if (this.colorMode.is("Custom")) {
               return true;
            }
         }

         return false;
      });
      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         this.clear();
      });
   }

   static final class Particle {
      private Vec3 position;
      private final boolean idleParticle;
      private final long bornNanos = System.nanoTime();
      private final int color;
      private final Identifier texture;
      private final boolean attackPhysics;
      private float visibleTarget = 1.0F;
      private float rotation;
      private final long bounceLifeMs;
      private final float size;
      private float rotate;
      private float visibilityAlpha;
      private Vec3 velocity;
      private long firstGroundNanos;

      private float alpha(long timestamp) {
         float computedValue = (float)(timestamp - this.bornNanos) / 1000000.0F;
         if (this.idleParticle) {
            float minimum = Math.min(1.0F, computedValue / 420.0F);
            minimum = 1.0F - (1.0F - minimum) * (1.0F - minimum);
            return minimum * this.visibilityAlpha;
         }

         if (this.attackPhysics) {
            float minimum2 = Math.min(1.0F, computedValue / 180.0F);
            minimum2 = 1.0F - (1.0F - minimum2) * (1.0F - minimum2);
            if (this.firstGroundNanos != 0L) {
               float computedValue2 = (float)(timestamp - this.firstGroundNanos) / 1000000.0F;
               float computedValue3 = Math.max(0.0F, 1.0F - computedValue2 / Math.max(1.0F, (float)this.bounceLifeMs));
               return minimum2 * computedValue3;
            } else {
               return minimum2;
            }
         } else {
            return computedValue <= 1200.0F ? 1.0F : Math.max(0.0F, 1.0F - (computedValue - 1200.0F) / 800.0F);
         }
      }

      private boolean idleExpired(long timestamp) {
         long timestamp2 = (timestamp - this.bornNanos) / 1000000L;
         if (this.visibleTarget <= 0.0F) {
            if (this.visibilityAlpha <= 0.025F && timestamp2 > 350L) {
               return true;
            }
         }

         return false;
      }

      private boolean expired(long timestamp, long timestamp2) {
         if (this.attackPhysics && this.firstGroundNanos != 0L) {
            long timestamp3 = (timestamp - this.firstGroundNanos) / 1000000L;
            return timestamp3 >= this.bounceLifeMs;
         } else {
            return (timestamp - this.bornNanos) / 1000000L >= timestamp2;
         }
      }

      private void updateFlyingPhysics(double value, double value2) {
         Vec3 position2 = this.velocity.scale(value * value2);
         Vec3 position3 = this.position.add(0.0, 0.0, position2.z);
         if (ParticlesModule.solid(position3)) {
            this.velocity = new Vec3(this.velocity.x, this.velocity.y, -this.velocity.z);
         } else {
            this.position = position3;
         }

         Vec3 position4 = this.position.add(0.0, position2.y, 0.0);
         if (ParticlesModule.solid(position4)) {
            this.velocity = new Vec3(this.velocity.x, -this.velocity.y, this.velocity.z);
         } else {
            this.position = position4;
         }

         Vec3 position5 = this.position.add(position2.x, 0.0, 0.0);
         if (ParticlesModule.solid(position5)) {
            this.velocity = new Vec3(-this.velocity.x, this.velocity.y, this.velocity.z);
         } else {
            this.position = position5;
         }

         this.velocity = this.velocity.scale(Math.pow(0.999, value));
      }

      private void update(double value, long timestamp, double value2, boolean condition) {
         if (condition) {
            this.rotation = this.rotate % 1000.0F / 2.5F;
            float computedValue = this.rotate + (float)(value * 60.0);
            this.rotate = computedValue;
         } else {
            this.rotation = 0.0F;
         }

         double computedValue2 = value * 60.0;
         if (!(computedValue2 <= 0.0)) {
            if (this.attackPhysics) {
               this.updateAttackPhysics(computedValue2, timestamp, value2);
            } else {
               this.updateFlyingPhysics(computedValue2, value2);
            }
         }
      }

      private void updateAttackPhysics(double index, long firstGroundNanos2, double index2) {
         double index3 = Math.max(1.0, Math.ceil(index * index2));
         double index4 = index / index3;

         for (int value = 0; value < (int)index3; value++) {
            this.velocity = this.velocity.add(0.0, -0.012 * index4, 0.0);
            Vec3 position2 = this.velocity.scale(index4 * index2);
            Vec3 position3 = this.position.add(position2.x, 0.0, 0.0);
            if (ParticlesModule.solid(position3)) {
               this.velocity = new Vec3(-this.velocity.x * 0.62, this.velocity.y, this.velocity.z);
            } else {
               this.position = position3;
            }

            Vec3 position4 = this.position.add(0.0, position2.y, 0.0);
            if (ParticlesModule.solid(position4)) {
               if (this.velocity.y < 0.0 && this.firstGroundNanos == 0L) {
                  this.firstGroundNanos = firstGroundNanos2;
               }

               this.velocity = new Vec3(this.velocity.x * 0.78, -this.velocity.y * 0.58, this.velocity.z * 0.78);
               if (Math.abs(this.velocity.y) < 0.018) {
                  this.velocity = new Vec3(this.velocity.x, 0.0, this.velocity.z);
               }
            } else {
               this.position = position4;
            }

            Vec3 position5 = this.position.add(0.0, 0.0, position2.z);
            if (ParticlesModule.solid(position5)) {
               this.velocity = new Vec3(this.velocity.x, this.velocity.y, -this.velocity.z * 0.62);
            } else {
               this.position = position5;
            }

            this.velocity = new Vec3(this.velocity.x * 0.985, this.velocity.y, this.velocity.z * 0.985);
         }
      }

      private Particle(Identifier texture2, Vec3 position2, Vec3 velocity2, int color2, float size2, boolean attackPhysics2, long bounceLifeMs2, boolean idleParticle2) {
         this.texture = texture2;
         this.position = position2;
         this.velocity = velocity2;
         this.color = color2;
         this.size = size2;
         this.attackPhysics = attackPhysics2;
         this.bounceLifeMs = bounceLifeMs2;
         this.idleParticle = idleParticle2;
         this.visibilityAlpha = idleParticle2 ? 0.0F : 1.0F;
         this.rotation = (float)ParticlesModule.random(0.0, 360.0);
      }
   }
}
