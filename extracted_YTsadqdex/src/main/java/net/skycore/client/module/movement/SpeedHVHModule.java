package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.BooleanSetting;

public final class SpeedHVHModule extends ClientModule {
   public static final SpeedHVHModule INSTANCE = new SpeedHVHModule();
   private final ModeSetting mode = this.add(new ModeSetting("Мод", "Ваниль", "Ваниль", "Моушн"));
   private final BooleanSetting buffOnly = this.add(new BooleanSetting("Только под бафом", true));
   private final ModeSetting buffLevel = this.add(new ModeSetting("Уровень бафа", "Любой", "Любой", "Скорость 1", "Скорость 2", "Скорость 3"));
   private final SliderSetting buffPower;
   private final BooleanSetting jumpBoost;
   private final SliderSetting acceleration;
   private final SliderSetting speed = this.add(new SliderSetting("Скорость", 0.4F, 0.2F, 1.0, 0.01F));

   private static void setSpeed(LocalPlayer speed2, Vec2 speed3, double speed4) {
      float yRot = speed2.getYRot();
      if (speed3.y < 0.0F) {
         yRot += 180.0F;
      }

      float y = speed3.y < 0.0F ? -0.5F : (speed3.y > 0.0F ? 0.5F : 1.0F);
      if (speed3.x > 0.0F) {
         yRot -= 90.0F * y;
      }

      if (speed3.x < 0.0F) {
         yRot += 90.0F * y;
      }

      double angleRadians = Math.toRadians(yRot);
      Vec3 deltaMovement = speed2.getDeltaMovement();
      speed2.setDeltaMovement(-Math.sin(angleRadians) * speed4, deltaMovement.y, Math.cos(angleRadians) * speed4);
   }

   private SpeedHVHModule() {
      super("SpeedHVH", "Спиды ёпта", ModuleCategory.MOVEMENT);
      this.buffPower = this.add(new SliderSetting("Мощность бафа", 0.15F, 0.0, 0.5, 0.01F));
      this.acceleration = this.add(new SliderSetting("Ускорение", 0.25, 0.05F, 0.6F, 0.01F));
      this.jumpBoost = this.add(new BooleanSetting("Подпрыгивание", true));
      this.buffLevel.visibleWhen(this.buffOnly::get);
      this.acceleration.visibleWhen(() -> {
         return this.mode.is("Моушн");
      });
      this.jumpBoost.visibleWhen(() -> {
         return this.mode.is("Моушн");
      });
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onMoving);
   }

   private int requiredBuffLevel() {
      if (this.buffLevel.is("Скорость 2")) {
         return 2;
      }

      return this.buffLevel.is("Скорость 3") ? 3 : 1;
   }

   private int getBuffLevel(LocalPlayer player) {
      int value = 0;
      MobEffectInstance effect = player.getEffect(MobEffects.SPEED);
      if (effect == null) {
         value = 0;
      } else {
         value = effect.getAmplifier() + 1;
      }

      return value;
   }

   private float targetSpeed(LocalPlayer player) {
      float value = this.speed.get().floatValue();
      Double value2 = this.buffPower.get();
      float buffLevel2 = value * (1.0F + value2.floatValue() * this.getBuffLevel(player));
      MobEffectInstance effect = player.getEffect(MobEffects.SLOWNESS);
      if (effect != null) {
         float amplifier = 1.0F - 0.15F * (effect.getAmplifier() + 1);
         buffLevel2 *= Math.max(0.3F, amplifier);
      }

      return buffLevel2;
   }

   private boolean canRun(LocalPlayer player) {
      if (player != null) {
         Abilities abilities = player.getAbilities();
         if (!abilities.flying) {
            if (!player.isInWater() && !player.isInLava()) {
               Boolean get = this.buffOnly.get();
               if (!get) {
                  return true;
               }

               if (this.getBuffLevel(player) >= this.requiredBuffLevel()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void onMoving(PlayerTickEndEvent playerTickEndEvent) {
      if (this.isEnabled()) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (this.canRun(instance) && instance.input != null) {
            float speed2 = this.targetSpeed(instance);
            Vec2 moveVector = instance.input.getMoveVector();
            if (this.mode.is("Ваниль")) {
               setSpeed(instance, moveVector, speed2);
            } else if (moveVector.x != 0.0F || moveVector.y != 0.0F) {
               Vec3 deltaMovement = instance.getDeltaMovement();
               double speed3 = Math.hypot(deltaMovement.x, deltaMovement.z);
               if (speed3 < speed2) {
                  speed3 = Math.min(speed2, speed3 + this.acceleration.get().floatValue());
               }

               if (this.jumpBoost.get() && instance.onGround()) {
                  instance.jumpFromGround();
               }

               setSpeed(instance, moveVector, speed3);
            }
         }
      }
   }
}
