package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AntiBotModule;
import net.skycore.client.util.WorldParticleRenderer;
import net.skycore.client.util.CameraViewPoseHelper;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;

public final class CometsModule extends ClientModule {
   private final ColorSetting customColor;
   private final ModeSetting palette;
   private static final int TRAIL_LENGTH = 17;
   private double phase;
   private double previousPhase;
   private static final double TRAIL_STEP = 0.095;
   private Object previousLevel;
   private final WorldParticleRenderer renderer;
   private final List<LivingEntity> tracked;
   private final List<CometsModule.Fly> flies;
   private static final Identifier BLOOM = Identifier.parse("skycore:icons/world_render/bloom.png");
   private final Map<LivingEntity, double[]> headings;
   private final NumberSetting speed;
   private static final double FLIGHT_STEP = 0.105;
   private final NumberSetting size = this.add(new NumberSetting("Size", "Comet and trail size", 1.8, 0.4, 4.0, 0.1, "x"));
   private final MultiSelectSetting targets;

   public void renderWorld() {
      if (this.isEnabled() && !this.tracked.isEmpty() && !this.flies.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.level == this.previousLevel) {
            float deltaTracker = Mth.clamp(instance.getDeltaTracker().getGameTimeDeltaPartialTick(true), 0.0F, 1.0F);
            float value = this.size.get().floatValue();
            int value2 = this.palette.is("Custom") ? this.customColor.get() : SkycoreClient.themes().accent();
            long now = System.currentTimeMillis();
            double computedValue = Mth.lerp(deltaTracker, this.previousPhase, this.phase);
            double computedValue2 = 0.095 * this.speed.get();
            ArrayList elements = new ArrayList(this.tracked.size() * 130);

            for (LivingEntity entity : this.tracked) {
               if (entity.isAlive() && entity.level() == instance.level && (entity != instance.player || !instance.options.getCameraType().isFirstPerson())) {
                  double[] doubleValues = this.headings.get(entity);
                  if (doubleValues != null) {
                     Vec3 position = CameraViewPoseHelper.interpolatedPosition(entity, deltaTracker);
                     double height = Math.toRadians(Mth.lerp(deltaTracker, doubleValues[0], doubleValues[1]));

                     for (CometsModule.Fly fly : this.flies) {
                        double height2 = computedValue + fly.phase;
                        Vec3 bbHeight = trajectory(position, entity.getBbHeight(), height, height2, fly.index);
                        double sine = 0.9 + 0.1 * Math.sin(now / 210.0 + fly.phase);
                        Vec3 position2 = bbHeight;

                        for (int value3 = 1; value3 <= 17; value3++) {
                           Vec3 bbHeight2 = trajectory(position, entity.getBbHeight(), height, height2 - value3 * computedValue2, fly.index);

                           for (int value4 = 1; value4 <= 3; value4++) {
                              float computedValue3 = (value3 - 1 + value4 / 3.0F) / 17.0F;
                              float computedValue4 = (1.0F - computedValue3) * (1.0F - computedValue3);
                              elements.add(
                                 new WorldParticleRenderer.Sprite(
                                    BLOOM,
                                    position2.lerp(bbHeight2, value4 / 3.0),
                                    value * (0.055F * (1.0F - computedValue3) + 0.01F),
                                    0.0F,
                                    tint(value2, Math.round(175.0F * computedValue4))
                                 )
                              );
                           }

                           position2 = bbHeight2;
                        }

                        elements.add(new WorldParticleRenderer.Sprite(BLOOM, bbHeight, 0.22F * value, 0.0F, tint(value2, (int)(80.0 * sine))));
                        elements.add(new WorldParticleRenderer.Sprite(BLOOM, bbHeight, 0.105F * value, 0.0F, tint(value2, (int)(185.0 * sine))));
                        elements.add(new WorldParticleRenderer.Sprite(BLOOM, bbHeight, 0.035F * value, 0.0F, tint(-1, (int)(240.0 * sine))));
                     }
                  }
               }
            }

            this.renderer.render(elements, true);
         }
      }
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && instance.player != null) {
         if (instance.level != this.previousLevel) {
            this.reset();
            this.previousLevel = instance.level;
         }

         this.tracked.clear();
         if (this.targets.isSelected("Self")) {
            this.tracked.add(instance.player);
         }

         for (Entity entity : instance.level.entitiesForRendering()) {
            if (this.tracked.size() >= 16) {
               break;
            }

            if (entity != instance.player && this.accepts(entity, instance)) {
               this.tracked.add((LivingEntity)entity);
            }
         }

         this.headings.keySet().retainAll(this.tracked);

         for (LivingEntity entity2 : this.tracked) {
            double[] yRot = this.headings.computeIfAbsent(entity2, entity3 -> {
               double[] doubleValues = new double[2];
               doubleValues[0] = entity2.getYRot();
               doubleValues[1] = entity2.getYRot();
               return doubleValues;
            });
            yRot[0] = yRot[1];
            yRot[1] += Mth.wrapDegrees(entity2.getYRot() - yRot[1]) * 0.24;
         }

         if (this.flies.size() != 2) {
            this.flies.clear();
            this.flies.add(new CometsModule.Fly(0, 0.0));
            this.flies.add(new CometsModule.Fly(1, Math.PI));
         }

         this.previousPhase = this.phase;
         this.phase = this.phase + 0.105 * this.speed.get();
      } else {
         this.reset();
      }
   }

   public CometsModule() {
      super("Comets", "Two glowing comets flying around the shoulders and chest", ModuleCategory.RENDER);
      this.speed = this.add(new NumberSetting("Speed", "Flight speed", 1.5, 0.3, 4.0, 0.1, "x"));
      this.targets = this.add(new MultiSelectSetting("Targets", List.of("Self", "Players", "Friends", "Naked", "Animals", "Mobs"), Set.of("Self")));
      this.palette = this.add(new ModeSetting("Color Mode", "Sync", "Sync", "Custom"));
      this.customColor = this.add(new ColorSetting("Custom Color", -10177025, false));
      this.renderer = new WorldParticleRenderer();
      this.flies = new ArrayList<>();
      this.tracked = new ArrayList<>();
      this.headings = new IdentityHashMap<>();
      this.customColor.visibleWhen(() -> {
         return this.palette.is("Custom");
      });
   }

   private static int tint(int value, int value2) {
      int value3 = 0;
      value3 = value & 16777215;
      return value3 | Mth.clamp(value2, 0, 255) << 24;
   }

   @Override
   protected void onEnable() {
      this.reset();
   }

   private boolean accepts(Entity entity, Minecraft minecraft) {
      if (!(entity instanceof LivingEntity entity2 && entity2.isAlive() && !entity2.isInvulnerable() && !(entity instanceof ArmorStand))) {
         return false;
      } else {
         if (entity == minecraft.player) {
            return this.targets.isSelected("Self");
         }

         if (minecraft.player.distanceToSqr(entity) > 576.0) {
            return false;
         }

         if (entity instanceof Player player) {
            if (AntiBotModule.isBot(player)) {
               return false;
            } else if (FriendManager.INSTANCE.isFriend(player.getGameProfile().name())) {
               return this.targets.isSelected("Friends");
            } else {
               return player.getArmorValue() == 0 ? this.targets.isSelected("Naked") : this.targets.isSelected("Players");
            }
         } else {
            return !(entity instanceof Animal) && !(entity instanceof AmbientCreature)
               ? entity instanceof Mob && this.targets.isSelected("Mobs")
               : this.targets.isSelected("Animals");
         }
      }
   }

   @Override
   protected void onDisable() {
      this.reset();
   }

   private static Vec3 trajectory(Vec3 position, double value, double value2, double value3, int value4) {
      double cosine = Math.cos(value2);
      double sine = Math.sin(value2);
      Vec3 position2 = new Vec3(cosine, 0.0, sine);
      cosine = -Math.sin(value2);
      Vec3 position3 = new Vec3(cosine, 0.0, Math.cos(value2));
      double computedValue = Math.cos(value3) * 0.53;
      double computedValue2 = Math.sin(value3) * 0.57;
      double computedValue3 = value3 * 2.0 + value4 * 0.65;
      double cosine2 = 0.16 * Math.sin(computedValue3) + 0.09 * Math.cos(value3 + value4 * 0.4);
      Vec3 position4 = position.add(0.0, value * 0.75 + cosine2, 0.0);
      Vec3 position5 = position2.scale(computedValue);
      return position4.add(position5).add(position3.scale(computedValue2));
   }

   private void reset() {
      this.flies.clear();
      this.tracked.clear();
      this.headings.clear();
      this.previousLevel = null;
      this.phase = 0.0;
      this.previousPhase = 0.0;
   }

   public record Fly(int index, double phase) {
      public double phase() {
         return this.phase;
      }

      public int index() {
         return this.index;
      }
   }
}
