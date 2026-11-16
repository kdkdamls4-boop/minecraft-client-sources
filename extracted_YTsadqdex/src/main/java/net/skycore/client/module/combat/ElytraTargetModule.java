package net.skycore.client.module.combat;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.movement.SuperFireworkModule;
import net.skycore.client.util.AddModelHelper;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.FlightRawHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.WriteHexByteHelper;

public final class ElytraTargetModule extends ClientModule {
   public final BooleanSetting slowdown;
   public final SliderSetting attackRange = this.add(new SliderSetting("Дистанция атаки", 3.0, 1.0, 5.0, 0.1));
   public static final float DETECT_RANGE = 100.0F;
   private LivingEntity visualTarget;
   private Vec3 previousVisualPos;
   public final ModeSetting marker;
   public static final float BPS_THRESHOLD = 10.0F;
   public final SliderSetting jarPredictStrength;
   private Vec3 currentVisualPos;
   public static final ElytraTargetModule INSTANCE = new ElytraTargetModule();
   private int visualTick;
   private boolean responsePending;
   public static Vec3 predictPos = Vec3.ZERO;
   public final ModeSetting predictionMode = this.add(new ModeSetting("Режим предикта", "По взгляду", "По взгляду", "По движению"));
   public final BooleanSetting drawPredict;

   private void preTick() {
      if (this.isEnabled()) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null) {
            if (!instance.isFallFlying()) {
               this.responsePending = false;
            }
         }
      }
   }

   private ElytraTargetModule() {
      super("ElytraTarget", "Преследует игрока на элитрах", ModuleCategory.COMBAT);
      this.jarPredictStrength = this.add(new SliderSetting("Сила предикта", 3.0, 1.5, 10.0, 0.5));
      this.drawPredict = this.add(new BooleanSetting("Рендерить предикт", true));
      this.slowdown = this.add(new BooleanSetting("Замедление", true));
      this.marker = this.add(new ModeSetting("Отображение", "Модель игрока", "Квадрат", "Модель игрока"));
      this.previousVisualPos = Vec3.ZERO;
      this.currentVisualPos = Vec3.ZERO;
      this.visualTick = Integer.MIN_VALUE;
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, playerTickEndEvent -> this.preTick());
   }

   public Vec3 onGlidingVelocity(Vec3 position) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (this.isEnabled() && instance != null) {
         AttackAuraModule attackAuraModule = aura();
         LivingEntity entity = attackAuraModule != null ? attackAuraModule.currentTarget() : null;
         if (this.responsePending) {
            float delta = 1.7F;
            if (SuperFireworkModule.INSTANCE.shouldBoost()) {
               delta = SuperFireworkModule.INSTANCE.responseSpeed() + 0.17F;
            }

            if (this.slowdown.get() && entity != null && instance.getEyePosition().distanceTo(entity.getEyePosition().add(entity.getDeltaMovement())) < 2.0) {
               delta = (float)entity.getDeltaMovement().length();
            }

            Vec3 xRot = FlightRawHelper.flightStep(instance, Vec3.directionFromRotation(instance.getXRot(), instance.getYRot()).scale(delta), 1.0);
            this.responsePending = false;
            return xRot;
         } else {
            if (entity != null) {
               Vec3 deltaMovement = FlightRawHelper.flightStep(instance, instance.getDeltaMovement(), 3.2);
               Vec3 position2 = new Vec3(entity.getDeltaMovement().x * 1.0E-5F, entity.getDeltaMovement().y * 1.0E-5F, entity.getDeltaMovement().z * 1.0E-5F);
               if (instance.getEyePosition().distanceTo(entity.getEyePosition().add(entity.getDeltaMovement())) < 4.5
                  && entity.isFallFlying()
                  && Math.hypot(entity.getDeltaMovement().x, entity.getDeltaMovement().z) != 0.0) {
                  return deltaMovement.add(position2);
               }
            }

            return position;
         }
      } else {
         return position;
      }
   }

   public Vec3 predict(LivingEntity entity, Vec3 position) {
      if (!this.isPredicting(entity)) {
         return position;
      } else if (this.predictionMode.is("По взгляду")) {
         float intermediate = FlightRawHelper.pitch(entity);
         float y = FlightRawHelper.yaw(entity);
         Vec3 position2 = entity.calculateViewVector(intermediate, y);
         return position.add(position2.scale(this.jarPredictStrength.get().floatValue()));
      } else {
         return position.add(FlightRawHelper.predictOffset(entity, this.jarPredictStrength.get().floatValue()));
      }
   }

   public static AttackAuraModule aura() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(AttackAuraModule.class);
      return (AttackAuraModule)optional.orElse(null);
   }

   @Override
   protected void onDisable() {
      this.responsePending = false;
      predictPos = Vec3.ZERO;
      this.resetVisualPosition();
   }

   public void onFirework(LivingEntity entity) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (this.isEnabled() && instance != null) {
         AttackAuraModule attackAuraModule = aura();
         LivingEntity entity2 = attackAuraModule != null ? attackAuraModule.currentTarget() : null;
         Vec3 eyePosition = entity2 != null ? FlightRawHelper.closest(instance.getEyePosition(), entity2.getBoundingBox()) : Vec3.ZERO;
         if (entity == instance && (entity2 == null || !instance.isFallFlying() || entity2.isFallFlying() || !(instance.getEyePosition().distanceTo(eyePosition) < 2.6F))) {
            this.responsePending = true;
         }
      }
   }

   public void renderPredictCube() {
      Minecraft instance = Minecraft.getInstance();
      AttackAuraModule attackAuraModule = aura();
      LivingEntity entity = attackAuraModule != null ? attackAuraModule.currentTarget() : null;
      if (this.isEnabled()
         && this.drawPredict.get()
         && instance.player != null
         && instance.level != null
         && attackAuraModule != null
         && attackAuraModule.isEnabled()
         && instance.player.isFallFlying()
         && entity != null
         && entity.isFallFlying()) {
         Vec3 position = this.predictDonor(entity);
         predictPos = position;
         float gameTimeDeltaPartialTick = instance.getDeltaTracker().getGameTimeDeltaPartialTick(true);
         AddQuadHelper.beginFrame();
         if (this.marker.is("Модель игрока")) {
            int count = vibrantThemeColor(SkycoreClient.themes().accent());
            AddModelHelper.addModel(
               entity,
               this.smoothVisualPosition(entity, position, gameTimeDeltaPartialTick, instance.player.tickCount),
               gameTimeDeltaPartialTick,
               count,
               WriteHexByteHelper.lerp(count, -1, 0.88F),
               (entity.tickCount + gameTimeDeltaPartialTick) / 20.0F
            );
         } else {
            this.resetVisualPosition();
            AddQuadHelper.addBoxOutline(AABB.ofSize(position, 0.75, 0.75, 0.75), -1, 1.0F);
         }

         AddQuadHelper.endFrame();
      } else {
         predictPos = Vec3.ZERO;
         this.resetVisualPosition();
      }
   }

   private Vec3 smoothVisualPosition(LivingEntity visualTarget2, Vec3 currentVisualPos2, float value, int visualTick2) {
      if (this.visualTarget == visualTarget2 && this.visualTick != Integer.MIN_VALUE) {
         if (this.visualTick != visualTick2) {
            this.previousVisualPos = this.currentVisualPos;
            this.currentVisualPos = currentVisualPos2;
            this.visualTick = visualTick2;
         } else {
            this.currentVisualPos = currentVisualPos2;
         }
      } else {
         this.visualTarget = visualTarget2;
         this.currentVisualPos = currentVisualPos2;
         this.previousVisualPos = currentVisualPos2;
         this.visualTick = visualTick2;
      }

      Vec3 previousVisualPos2 = this.previousVisualPos;
      Vec3 currentVisualPos3 = this.currentVisualPos;
      double clampedValue = Mth.clamp(value, 0.0F, 1.0F);
      return previousVisualPos2.lerp(currentVisualPos3, clampedValue);
   }

   private static int vibrantThemeColor(int value) {
      float[] floatValues = WriteHexByteHelper.hsv(value);
      float value2 = floatValues[0];
      float clampedValue = Mth.clamp(floatValues[1] * 1.15F, 0.0F, 1.0F);
      float value3 = floatValues[2];
      float maximum = Math.max(value3, 0.9F);
      return WriteHexByteHelper.fromHsv(value2, clampedValue, maximum, 255);
   }

   public boolean isPredicting(LivingEntity entity) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (this.isOvertakeActive() && player != null && player.isFallFlying() && entity != null) {
         if (entity.isFallFlying()) {
            Vec3 position = entity.position();
            Vec3 position2 = position.subtract(entity.xo, entity.yo, entity.zo);
            if (FlightRawHelper.exceedsPredictThreshold(position2.length(), 10.0)) {
               return true;
            }
         }
      }

      return false;
   }

   private void resetVisualPosition() {
      this.visualTarget = null;
      Vec3 zERO = Vec3.ZERO;
      this.currentVisualPos = Vec3.ZERO;
      this.previousVisualPos = zERO;
      this.visualTick = Integer.MIN_VALUE;
   }

   public boolean isOvertakeActive() {
      return this.isEnabled();
   }

   public Vec3 predictDonor(LivingEntity entity) {
      float height;
      Vec3 position;
      float intermediate;
      targetValidation: {
         LocalPlayer instance = Minecraft.getInstance().player;
         position = entity.position();
         height = entity.getBbHeight() / 2.0F;
         if (instance != null) {
            if (instance.isFallFlying()) {
               intermediate = 0.5F;
               break targetValidation;
            }
         }

         intermediate = 0.0F;
      }

      double computedValue = height + intermediate;
      return this.predict(entity, position.add(0.0, computedValue, 0.0));
   }
}
