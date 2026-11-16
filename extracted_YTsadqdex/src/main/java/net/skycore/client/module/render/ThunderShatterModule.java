package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.CameraViewPoseHelper;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.ThunderPipelines;
import net.skycore.client.setting.BooleanSetting;

public final class ThunderShatterModule extends ClientModule {
   private final BooleanSetting shockwaves;
   private final ModeSetting palette = this.add(new ModeSetting("Palette", "Gold", "Gold", "Custom"));
   private final ColorSetting customColor = this.add(new ColorSetting("Glass Color", -11413, false));
   private final BooleanSetting sparks;
   private static final double GRAVITY = 9.3;
   private final List<ThunderShatterModule.Burst> bursts;
   private final NumberSetting force;
   private final ThunderPipelines thunder;
   private final NumberSetting duration;
   private static final int MAX_BURSTS = 4;
   private final BooleanSetting trails;
   public static final ThunderShatterModule INSTANCE = new ThunderShatterModule();
   private final NumberSetting amount = this.add(new NumberSetting("Shards", "", 14.0, 6.0, 26.0, 1.0, ""));
   private static final double BODY_LIGHTNING_SECONDS = 1.0;
   private final BooleanSetting flash;

   private static double smooth(double value) {
      value = clamp(value);
      return value * value * (3.0 - 2.0 * value);
   }

   public void onEntityEvent(ClientboundEntityEventPacket clientboundEntityEventPacket) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.level != null && instance.player != null && clientboundEntityEventPacket.getEventId() == 35) {
         if (clientboundEntityEventPacket.getEntity(instance.level) instanceof Player player && !(player.distanceToSqr(instance.player) > 4096.0)) {
            long nowNanos = System.nanoTime();
            Random random = new Random(nowNanos ^ player.getUUID().getLeastSignificantBits());
            Vec3 bbHeight = player.position().add(0.0, player.getBbHeight() * 0.53, 0.0);
            int value = this.amount.get().intValue();
            ArrayList elements = new ArrayList(value);

            for (int value2 = 0; value2 < value; value2++) {
               double computedValue = (value2 + random.nextDouble() * 0.38) * Math.PI * 2.0 / value;
               Vec3 position = new Vec3(Math.cos(computedValue), 0.0, Math.sin(computedValue));
               Vec3 position2 = position.scale((0.7 + random.nextDouble() * 0.65) * this.force.get())
                  .add(0.0, (0.35 + random.nextDouble() * 0.78) * this.force.get(), 0.0);
               elements.add(
                  new ThunderShatterModule.Shard(
                     bbHeight.add(position.scale(0.1 + random.nextDouble() * 0.11)), position2, computedValue, 0.043 + random.nextDouble() * 0.035, 3.0 + random.nextDouble() * 5.0
                  )
               );
            }

            if (this.bursts.size() >= 4) {
               this.bursts.remove(0);
            }

            this.bursts.add(new ThunderShatterModule.Burst(player, bbHeight, nowNanos, elements, new ArrayList<>(), random));
         }
      }
   }

   private void drawBurst(ThunderShatterModule.Burst burst, double coordinate, long timestamp, int alpha) {
      double value = this.force.get();
      float computedValue = (float)Math.max(0.0, 1.0 - smooth(coordinate / 0.46));
      if (this.flash.get() && coordinate < 0.18) {
         float index = (float)(Math.sin(Math.PI * coordinate / 0.18) * 0.88);
         double index2 = (0.18 + easeOut(coordinate / 0.18) * 0.83) * value;

         for (int value2 = 0; value2 < 8; value2++) {
            double computedValue2 = value2 / 8.0 * Math.PI * 2.0 + 0.13;
            Vec3 position = burst.center.add(Math.cos(computedValue2) * index2, Math.sin(computedValue2 * 2.0) * index2 * 0.3, Math.sin(computedValue2) * index2);
            this.thunder.bolt(burst.center, position, 0.11, burst.seed + value2 * 11.7, index, value2 % 3 == 0 ? -1307 : alpha);
         }

         double sine = Math.sin(Math.PI * coordinate / 0.18);

         for (int value3 = 0; value3 < 5; value3++) {
            double computedValue3 = value3 * Math.PI * 2.0 / 5.0;
            Vec3 position2 = new Vec3(Math.cos(computedValue3) * index2 * 0.42, 0.0, Math.sin(computedValue3) * index2 * 0.42);
            AddQuadHelper.addFacet(
               burst.center.add(0.0, index2 * 0.45, 0.0),
               burst.center.add(position2),
               burst.center.add(position2.scale(0.5)).add(0.0, -index2 * 0.25, 0.0),
               alpha(-1307, 0.1 * sine)
            );
         }
      }

      if (coordinate < 0.43) {
         for (int value4 = 0; value4 < burst.shards.size(); value4++) {
            ThunderShatterModule.Shard shard = burst.shards.get(value4);
            if (shard.landedAt == 0L) {
               if ((value4 & 1) == 0) {
                  this.thunder.bolt(burst.center, shard.position, 0.072, shard.seed * 31.0 + burst.seed, computedValue * 0.66F, alpha);
               }

               if (value4 % 3 == 0 && value4 + 1 < burst.shards.size()) {
                  ThunderShatterModule.Shard shard2 = burst.shards.get(value4 + 1);
                  if (shard2.landedAt == 0L) {
                     this.thunder.bolt(shard.position, shard2.position, 0.052, shard.seed * 17.0, computedValue * 0.43F, -1307);
                  }
               }
            }
         }
      }

      for (ThunderShatterModule.Shard shard3 : burst.shards) {
         double computedValue4 = shard3.landedAt == 0L ? 0.0 : (timestamp - shard3.landedAt) / 1.0E9;
         double computedValue5 = shard3.landedAt == 0L ? smooth(coordinate / 0.08) * (1.0 - smooth((coordinate - 0.62) / 0.38)) : 1.0 - smooth(computedValue4 / 0.24);
         if (!(computedValue5 <= 0.001)) {
            Vec3 position3 = shard3.landedAt == 0L && shard3.velocity.lengthSqr() > 0.01
               ? shard3.velocity.normalize()
               : new Vec3(Math.cos(shard3.angle), 0.15, Math.sin(shard3.angle)).normalize();
            Vec3 position4 = new Vec3(-Math.sin(shard3.angle), 0.0, Math.cos(shard3.angle));
            Vec3 position5 = shard3.position.add(position3.scale(shard3.size * 1.9));
            Vec3 position6 = shard3.position.add(position4.scale(shard3.size * 0.16));
            Vec3 position7 = shard3.position.subtract(position4.scale(shard3.size * 0.16));
            this.thunder.glass(position5, position6, position7, alpha, (float)computedValue5);
            if (this.trails.get() && shard3.landedAt == 0L) {
               this.thunder.bolt(shard3.position, shard3.position.subtract(position3.scale(shard3.size * 1.5)), 0.024, shard3.seed * 23.3, (float)(computedValue5 * 0.52), alpha);
            }

            if (this.sparks.get() && shard3.landedAt != 0L && computedValue4 < 0.18) {
               float index3 = (float)((1.0 - smooth(computedValue4 / 0.18)) * 0.78);

               for (int value5 = 0; value5 < 3; value5++) {
                  double computedValue6 = shard3.angle + value5 * Math.PI * 2.0 / 3.0;
                  Vec3 position8 = shard3.position.add(Math.cos(computedValue6) * 0.16, 0.025 + value5 * 0.016, Math.sin(computedValue6) * 0.16);
                  this.thunder.bolt(shard3.position.add(0.0, 0.018, 0.0), position8, 0.044, shard3.seed * 40.0 + value5 * 9.0, index3, value5 == 0 ? -1307 : alpha);
               }
            }
         }
      }

      if (this.sparks.get()) {
         for (ThunderShatterModule.Dust dust : burst.dust) {
            double computedValue7 = (timestamp - dust.started) / (dust.life * 1.0E9);
            double computedValue8 = 1.0 - smooth(computedValue7);
            double computedValue9 = 0.009 + 0.017 * (1.0 - computedValue7);
            AddQuadHelper.addBeam(dust.position.add(0.0, computedValue9, 0.0), dust.position.add(0.0, -computedValue9, 0.0), 0.0013, alpha(alpha, 0.62 * computedValue8));
         }
      }
   }

   private ThunderShatterModule() {
      super("Thunder Shatter", "Electric lightning and prismatic glass burst from a totem pop", ModuleCategory.RENDER);
      this.duration = this.add(new NumberSetting("Duration", "", 1550.0, 1000.0, 2700.0, 50.0, " ms"));
      this.force = this.add(new NumberSetting("Burst Power", "", 1.0, 0.5, 1.7, 0.1, "x"));
      this.flash = this.add(new BooleanSetting("Flash", true));
      this.shockwaves = this.add(new BooleanSetting("Shockwaves", true));
      this.trails = this.add(new BooleanSetting("Glass Trails", true));
      this.sparks = this.add(new BooleanSetting("Ground Scatter", true));
      this.bursts = new ArrayList<>();
      this.thunder = new ThunderPipelines();
      this.customColor.visibleWhen(() -> this.palette.is("Custom"));
      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         this.bursts.clear();
      });
   }

   private static double easeOut(double progress) {
      progress = clamp(progress);
      return 1.0 - Math.pow(1.0 - progress, 3.0);
   }

   private static double clamp(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }

   private void drawBodyLightning(ThunderShatterModule.Burst burst, double coordinate, float coordinate2, int coordinate3, Minecraft minecraft) {
      if (!(coordinate < 0.0) && !(coordinate >= 1.0) && !burst.owner.isRemoved() && burst.owner.isAlive() && burst.owner.level() == minecraft.level) {
         if (burst.owner != minecraft.player || !minecraft.options.getCameraType().isFirstPerson()) {
            double computedValue = smooth(coordinate / 0.085);
            double computedValue2 = 1.0 - smooth((coordinate - 0.72) / 0.28);
            float computedValue3 = (float)(computedValue * computedValue2);
            if (!(computedValue3 < 0.005F)) {
               Player owner = burst.owner;
               Vec3 position = CameraViewPoseHelper.interpolatedPosition(owner, coordinate2);
               double yRot = Math.toRadians(owner.getYRot(coordinate2));
               double bbHeight = Math.max(0.52, owner.getBbHeight());
               double bbWidth = Math.max(0.25, owner.getBbWidth() * 0.57 + 0.065);
               double computedValue4 = coordinate * Math.PI * 3.75 + yRot;

               for (int value = 0; value < 3; value++) {
                  double index = value * Math.PI * 2.0 / 3.0;
                  Vec3 position2 = null;

                  for (int value2 = 0; value2 <= 23; value2++) {
                     double computedValue5 = value2 / 23.0;
                     double sine = computedValue4 + index + computedValue5 * Math.PI * 3.25 + 0.095 * Math.sin(coordinate * 26.0 + computedValue5 * 13.0 + value);
                     double sine2 = bbWidth + 0.028 * Math.sin(computedValue5 * 24.0 - coordinate * 19.0 + value * 2.0);
                     double sine3 = 0.16 + (bbHeight - 0.3) * computedValue5 + 0.027 * Math.sin(sine * 2.0 + coordinate * 13.0);
                     Vec3 position3 = position.add(Math.cos(sine) * sine2, sine3, Math.sin(sine) * sine2);
                     if (position2 != null) {
                        float computedValue6 = (float)(smooth(computedValue5 / 0.13) * (1.0 - smooth((computedValue5 - 0.86) / 0.14)));
                        float computedValue7 = computedValue3 * computedValue6 * (value == 0 ? 0.95F : 0.73F);
                        this.thunder
                           .bolt(
                              position2,
                              position3,
                              value == 0 ? 0.082 : 0.069,
                              burst.seed + value * 43.7 + value2 * 1.79,
                              computedValue7,
                              value == 1 && value2 % 4 == 0 ? -1307 : coordinate3
                           );
                        if (value2 > 3 && value2 < 20 && (value2 + value * 3) % 7 == 0) {
                           Vec3 position4 = position3.add(Math.cos(sine) * 0.115, 0.09 + 0.035 * Math.sin(coordinate * 20.0 + value2), Math.sin(sine) * 0.115);
                           this.thunder.bolt(position3, position4, 0.045, burst.seed + 207.0 + value2 * 12.0 + value, computedValue7 * 0.52F, -1307);
                        }
                     }

                     position2 = position3;
                  }
               }
            }
         }
      }
   }

   private void advance(ThunderShatterModule.Burst burst, double index, long timestamp, Minecraft minecraft) {
      for (ThunderShatterModule.Shard shard : burst.shards) {
         if (shard.landedAt == 0L) {
            Vec3 position = shard.velocity.add(0.0, -9.3 * index, 0.0);
            Vec3 position2 = shard.position.add(position.scale(index));
            if (position.y < 0.0) {
               BlockHitResult blockHitResult = minecraft.level.clip(new ClipContext(shard.position, position2.add(0.0, -0.025, 0.0), Block.COLLIDER, Fluid.NONE, minecraft.player));
               if (blockHitResult.getType() == Type.BLOCK && blockHitResult.getDirection() == Direction.UP) {
                  shard.position = blockHitResult.getLocation().add(0.0, 0.014, 0.0);
                  shard.velocity = Vec3.ZERO;
                  shard.landedAt = timestamp;
                  if (this.sparks.get() && burst.dust.size() < 100) {
                     for (int value = 0; value < 3; value++) {
                        double computedValue = shard.angle + (value - 1) * 1.5 + burst.random.nextDouble() * 0.5;
                        Vec3 position3 = new Vec3(Math.cos(computedValue) * 0.35, 0.22 + burst.random.nextDouble() * 0.4, Math.sin(computedValue) * 0.35);
                        burst.dust.add(new ThunderShatterModule.Dust(shard.position, position3, timestamp, 0.34 + burst.random.nextDouble() * 0.24));
                     }
                  }
                  continue;
               }
            }

            shard.position = position2;
            shard.velocity = position.scale(0.997);
            shard.angle = shard.angle + shard.spin * index;
         }
      }

      for (ThunderShatterModule.Dust dust : burst.dust) {
         if (!(timestamp - dust.started > dust.life * 1.0E9)) {
            Vec3 position4 = dust.velocity.add(0.0, -6.696000000000001 * index, 0.0);
            Vec3 position5 = dust.position.add(position4.scale(index));
            if (position4.y < 0.0) {
               BlockHitResult blockHitResult2 = minecraft.level.clip(new ClipContext(dust.position, position5.add(0.0, -0.015, 0.0), Block.COLLIDER, Fluid.NONE, minecraft.player));
               if (blockHitResult2.getType() == Type.BLOCK && blockHitResult2.getDirection() == Direction.UP) {
                  position5 = blockHitResult2.getLocation().add(0.0, 0.009, 0.0);
                  position4 = new Vec3(position4.x * 0.58, -position4.y * 0.13, position4.z * 0.58);
               }
            }

            dust.position = position5;
            dust.velocity = position4;
         }
      }

      burst.dust.removeIf(dust2 -> {
         double computedValue2 = timestamp - dust2.started;
         return computedValue2 > dust2.life * 1.0E9;
      });
   }

   @Override
   protected void onDisable() {
      this.bursts.clear();
      this.thunder.release();
   }

   private static int alpha(int value, double value2) {
      double computedValue = clamp(value2) * 255.0;
      return (int)Math.round(computedValue) << 24 | value & 16777215;
   }

   public void renderWorld() {
      if (this.isEnabled() && !this.bursts.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null) {
            long nowNanos = System.nanoTime();
            double computedValue = this.duration.get() * 1000000.0;
            int computedValue2 = this.palette.is("Custom") ? this.customColor.get() : -11413;
            AddQuadHelper.beginFrame();
            this.thunder.beginFrame();
            float deltaTracker = Mth.clamp(instance.getDeltaTracker().getGameTimeDeltaPartialTick(true), 0.0F, 1.0F);

            for (int index = this.bursts.size() - 1; index >= 0; index--) {
               ThunderShatterModule.Burst burst = this.bursts.get(index);
               double computedValue3 = (nowNanos - burst.started) / computedValue;
               if (computedValue3 >= 1.0) {
                  this.bursts.remove(index);
               } else if (!(burst.center.distanceToSqr(instance.player.position()) > 4900.0)) {
                  double computedValue4 = Math.max(0.0, Math.min(0.05, (nowNanos - burst.lastFrame) / 1.0E9));
                  burst.lastFrame = nowNanos;
                  this.advance(burst, computedValue4, nowNanos, instance);
                  this.drawBurst(burst, computedValue3, nowNanos, computedValue2);
                  if (this.shockwaves.get()) {
                     this.drawBodyLightning(burst, (nowNanos - burst.started) / 1.0E9, deltaTracker, computedValue2, instance);
                  }
               }
            }

            AddQuadHelper.endFrame();
            this.thunder.render(nowNanos);
         } else {
            this.bursts.clear();
         }
      }
   }

   static final class Burst {
      final List<ThunderShatterModule.Shard> shards;
      final long started;
      final double seed;
      final List<ThunderShatterModule.Dust> dust;
      long lastFrame;
      final Random random;
      final Vec3 center;
      final Player owner;

      public Burst(Player owner2, Vec3 center2, long started2, List<ThunderShatterModule.Shard> shards2, List<ThunderShatterModule.Dust> dust2, Random random2) {
         this.owner = owner2;
         this.center = center2;
         this.started = started2;
         this.lastFrame = started2;
         this.shards = shards2;
         this.dust = dust2;
         this.random = random2;
         this.seed = random2.nextDouble() * 1000.0;
      }
   }

   static final class Dust {
      final double life;
      Vec3 position;
      final long started;
      Vec3 velocity;

      public Dust(Vec3 position2, Vec3 velocity2, long started2, double life2) {
         this.position = position2;
         this.velocity = velocity2;
         this.started = started2;
         this.life = life2;
      }
   }

   static final class Shard {
      double angle;
      Vec3 position;
      Vec3 velocity;
      final double size;
      final float seed;
      long landedAt;
      final double spin;

      public Shard(Vec3 position2, Vec3 velocity2, double angle2, double size2, double spin2) {
         this.position = position2;
         this.velocity = velocity2;
         this.angle = angle2;
         this.size = size2;
         this.spin = spin2;
         this.seed = (float)(angle2 * 1.319 + size2 * 23.47);
      }
   }
}
