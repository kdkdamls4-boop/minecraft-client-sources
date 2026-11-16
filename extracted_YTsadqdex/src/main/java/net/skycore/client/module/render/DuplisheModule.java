package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.CameraViewPoseHelper;
import net.skycore.client.util.SpringRotation;
import net.skycore.client.util.ExtractRenderStateHelper;
import net.skycore.client.util.LegacyRigHelper;
import net.skycore.client.util.LoadMeshHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.TryPeckHelper;

public final class DuplisheModule extends ClientModule {
   private Vec3 origin;
   private int lastChirp;
   private long lastRotationFrame;
   private long started;
   private int lastFlap;
   private boolean watchingPlayer;
   private final List<SoundInstance> chirps;
   private boolean crashed;
   private Vec3 bikePosition;
   private float bikeStarted;
   private final TryPeckHelper defense;
   private final LoadMeshHelper renderer;
   private float bikeYaw;
   private final BooleanSetting prank = this.add(new BooleanSetting("Прикол", false));
   private final SpringRotation mamaRotation;
   private ClientLevel world;
   private float yaw;
   private LocalPlayer scenePlayer;

   public int displayedHealth(int value) {
      int value2 = 0;
      if (this.hasVisualDamage()) {
         if (this.crashed) {
            value2 = 0;
         } else {
            value2 = this.defense.displayedHealth(value);
         }
      } else {
         value2 = value;
      }

      return value2;
   }

   @Override
   protected void onDisable() {
      this.clear();
   }

   private boolean ready() {
      if (!this.isEnabled()) {
         return false;
      }

      Minecraft instance = Minecraft.getInstance();
      if (this.origin == null) {
         this.start();
      }

      if (this.origin == null) {
         return false;
      }

      if (instance.level == this.world) {
         if (instance.player == this.scenePlayer) {
            if (this.scenePlayer.isAlive() && !(this.seconds() >= 60.0F)) {
               return true;
            }
         }
      }

      this.setEnabled(false);
      return false;
   }

   @Override
   protected void onEnable() {
      this.clear();
      this.start();
   }

   private void clear() {
      this.chirps.forEach(Minecraft.getInstance().getSoundManager()::stop);
      this.chirps.clear();
      this.bikeStarted = -1.0F;
      this.bikePosition = null;
      this.crashed = false;
      this.world = null;
      this.origin = null;
      this.started = 0L;
      this.lastChirp = -1;
      this.lastFlap = -1;
      this.scenePlayer = null;
      this.defense.clear();
      this.watchingPlayer = false;
      this.lastRotationFrame = 0L;
   }

   private void start() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && instance.player != null) {
         this.world = instance.level;
         this.scenePlayer = instance.player;
         this.yaw = instance.player.getYRot() + 180.0F;
         this.mamaRotation.reset(this.yaw);
         this.lastRotationFrame = System.nanoTime();
         Vec3 lookAngle = instance.player.getLookAngle().multiply(1.0, 0.0, 1.0).normalize();
         if (lookAngle.lengthSqr() < 0.01) {
            lookAngle = new Vec3(0.0, 0.0, 1.0);
         }

         Vec3 position = instance.player.position().add(lookAngle.scale(3.0));
         BlockHitResult blockHitResult = this.world.clip(new ClipContext(position.add(0.0, 2.0, 0.0), position.add(0.0, -5.0, 0.0), Block.COLLIDER, Fluid.NONE, instance.player));
         this.origin = blockHitResult.getType() == Type.MISS ? position : blockHitResult.getLocation().add(0.0, 0.015, 0.0);
         this.started = System.nanoTime();
      }
   }

   public void renderWorld() {
      if (this.ready()) {
         float yaw2 = this.seconds();
         float computedValue = smooth(yaw2 / 3.0F);
         float computedValue2 = smooth((yaw2 - 20.0F) / 8.0F);
         float computedValue3 = smooth((yaw2 - 18.0F) / 4.0F);
         float computedValue4 = smooth((yaw2 - 39.0F) / 4.0F);
         float computedValue5 = 1.0F - 0.32F * computedValue + 0.32F * computedValue4;
         float value = this.defense.peckAmount(yaw2);
         Vec3 position = this.bikePosition != null ? this.bikePosition.add(0.0, 0.62, 0.0) : this.at(-computedValue4 * 1.05, computedValue2 * 0.12, 0.0);
         float gameTimeDeltaPartialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
         Vec3 position2 = CameraViewPoseHelper.interpolatedPosition(this.scenePlayer, gameTimeDeltaPartialTick);
         Vec3 position3 = position2.subtract(position).multiply(1.0, 0.0, 1.0).normalize();
         Vec3 position4 = position2.subtract(this.origin);
         double value2 = this.watchingPlayer ? 1.85 : 1.6;
         this.watchingPlayer = yaw2 >= 37.0F
               && !this.scenePlayer.isSpectator()
               && position4.x * position4.x + position4.z * position4.z < value2 * value2
               && Math.abs(position4.y) <= (this.watchingPlayer ? 2.0 : 1.8);
         float angle = (this.watchingPlayer || value > 0.0F) && position3.lengthSqr() > 0.001 ? (float)Math.toDegrees(Math.atan2(-position3.x, position3.z)) : this.yaw;
         long nowNanos = System.nanoTime();
         float yaw3 = this.mamaRotation.update(angle, (nowNanos - this.lastRotationFrame) / 1.0E9);
         this.lastRotationFrame = nowNanos;
         if (this.bikePosition != null) {
            yaw3 = this.bikeYaw;
         }

         double angleRadians = Math.toRadians(yaw3);
         Vec3 position5 = new Vec3(-Math.sin(angleRadians), 0.0, Math.cos(angleRadians));
         this.renderer
            .renderScene(LegacyRigHelper.PENGUIN, position.add(position5.scale(value * 0.22)), yaw3, yaw2, new LoadMeshHelper.ScenePose(1.0F, computedValue5, 0.0F, -1));
         if (this.prank.get()) {
            this.renderer
               .renderPhotoFace("mama", position.add(position5.scale(0.32)).add(0.0, 0.52 * computedValue5, 0.0), yaw3, new LoadMeshHelper.ScenePose(0.72F, computedValue5, 0.0F, -1));
            if (this.bikePosition != null) {
               this.renderer.renderProp("pitbike", this.bikePosition, this.bikeYaw, new LoadMeshHelper.ScenePose(1.0F, 1.0F, 0.0F, -13619144));
            }
         }

         float computedValue6 = smooth((yaw2 - 3.0F) / 3.0F);
         double computedValue7 = 1.35 * (1.0F - computedValue6) + computedValue3 * 1.2;
         double computedValue8 = computedValue6 * (1.0F - computedValue3) * 1.05;
         double computedValue9 = yaw2 > 3.0F && yaw2 < 6.0F ? Math.abs(Math.sin(yaw2 * 9.0F)) * 0.045 : 0.0;
         Vec3 position6 = this.at(computedValue7, computedValue9, computedValue8);
         this.renderer.renderScene(LegacyRigHelper.ANGRY_BIRD, position6, this.yaw, yaw2, LoadMeshHelper.ScenePose.NORMAL);
         if (this.prank.get()) {
            double angleRadians2 = Math.toRadians(this.yaw);
            this.renderer
               .renderPhotoFace(
                  "red", position6.add(-Math.sin(angleRadians2) * 0.43, 0.38, Math.cos(angleRadians2) * 0.43), this.yaw, new LoadMeshHelper.ScenePose(0.62F, 1.0F, 0.0F, -1)
               );
         }

         float computedValue10 = yaw2 >= 6.0F && yaw2 < 20.0F ? (float)Math.sin(yaw2 * Math.PI * 16.0) * 0.85F : 0.0F;

         for (int index : new int[]{-1, 1}) {
            this.renderer
               .renderProp(
                  "egg",
                  this.at(computedValue7 + index * 0.38, computedValue9 + 0.5, computedValue8),
                  this.yaw,
                  new LoadMeshHelper.ScenePose(1.1F, 1.5F, index * (0.65F + computedValue10), -4446935)
               );
         }

         if (yaw2 >= 6.0F && yaw2 < 21.0F) {
            float index2 = smooth(yaw2 - 6.0F) * (1.0F - smooth((yaw2 - 19.0F) / 2.0F));

            for (int value3 = 0; value3 < 5; value3++) {
               float computedValue11 = ((yaw2 - 6.0F) * 0.32F + value3 * 0.2F) % 1.0F;
               float computedValue12 = index2 * (float)Math.sin(computedValue11 * Math.PI) * 0.38F;
               this.renderer
                  .renderProp(
                     "heart",
                     this.at(Math.sin(value3 * 2.4 + yaw2) * 0.7, 1.3 + computedValue11 * 1.4, 0.4),
                     Minecraft.getInstance().gameRenderer.mainCamera().yRot(),
                     new LoadMeshHelper.ScenePose(computedValue12, 1.0F, (float)Math.sin(yaw2 * 2.0F + value3) * 0.15F, value3 % 2 == 0 ? -44923 : -27462)
                  );
            }
         }

         if (computedValue2 > 0.0F) {
            this.renderer.renderProp("nest", this.origin, this.yaw, new LoadMeshHelper.ScenePose(computedValue2, 1.0F, 0.0F, -8826834));
         }

         for (int value4 = 0; value4 < 3; value4++) {
            float computedValue13 = smooth((yaw2 - (29 + value4 * 3)) / 2.0F);
            if (!(computedValue13 <= 0.0F)) {
               float computedValue14 = smooth((yaw2 - (44 + value4 * 2)) / 1.2F);
               double computedValue15 = (value4 - 1) * 0.32;
               double computedValue16 = value4 == 1 ? 0.2 : -0.1;
               if (computedValue14 < 1.0F) {
                  double computedValue17 = yaw2 >= 42 + value4 * 2 ? Math.sin(yaw2 * 28.0F) * 0.05 : 0.0;
                  this.renderer
                     .renderProp(
                        "egg",
                        this.at(computedValue15, 0.07 + (1.0F - computedValue13) * 0.35, computedValue16),
                        this.yaw,
                        new LoadMeshHelper.ScenePose(computedValue13 * (1.0F - computedValue14), 1.0F, (float)computedValue17, -5436)
                     );
               }

               if (computedValue14 > 0.0F) {
                  double computedValue18 = Math.max(0.0, Math.sin(yaw2 * 9.0F + value4 * 2)) * 0.055;
                  this.renderer
                     .renderScene(
                        LegacyRigHelper.PENGUIN,
                        this.at(computedValue15, 0.07 + computedValue18, computedValue16),
                        this.yaw + (float)Math.sin(yaw2 * 3.0F + value4) * 18.0F,
                        yaw2 + value4,
                        new LoadMeshHelper.ScenePose(0.27F * computedValue14, 1.0F, 0.0F, -7000)
                     );
               }
            }
         }
      }
   }

   private float seconds() {
      long nowNanos = System.nanoTime();
      return (float)((nowNanos - this.started) / 1.0E9);
   }

   public boolean hasVisualDamage() {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (this.origin != null && instance.level == this.world && instance.player == this.scenePlayer && this.scenePlayer.isAlive()) {
            if (this.seconds() < 60.0F) {
               return true;
            }
         }
      }

      return false;
   }

   public DuplisheModule() {
      super("Duplishe", "A one-minute bird family scene with a twig nest and chicks", ModuleCategory.RENDER);
      this.bikeStarted = -1.0F;
      this.renderer = new LoadMeshHelper();
      this.lastChirp = -1;
      this.lastFlap = -1;
      this.defense = new TryPeckHelper();
      this.mamaRotation = new SpringRotation();
      this.chirps = new ArrayList<>();
   }

   private static float smooth(float value) {
      float computedValue = Math.max(0.0F, Math.min(1.0F, value));
      return computedValue * computedValue * (3.0F - 2.0F * computedValue);
   }

   @Override
   protected void onTick() {
      if (this.ready()) {
         float value = this.seconds();
         if (value >= 6.0F && value < 20.0F) {
            int computedValue = (int)((value - 6.0F) * 8.0F);
            if (computedValue != this.lastFlap) {
               this.lastFlap = computedValue;
               this.play(SoundEvents.PARROT_FLY, this.at(0.0, 0.6, 1.05), 1.0F, 1.7F);
            }
         }

         Vec3 position = this.scenePlayer.position().subtract(this.origin);
         int eyePosition = this.world
                  .clip(new ClipContext(this.origin.add(0.0, 0.7, 0.0), this.scenePlayer.getEyePosition(), Block.COLLIDER, Fluid.NONE, this.scenePlayer))
                  .getType()
               == Type.MISS
            ? 1
            : 0;
         if (!this.prank.get()) {
            this.bikeStarted = -1.0F;
            this.bikePosition = null;
            this.crashed = false;
         } else if (!this.scenePlayer.isSpectator()
            && eyePosition != 0
            && value >= 45.2F
            && this.bikeStarted < 0.0F
            && position.x * position.x + position.z * position.z < 2.56
            && Math.abs(position.y) < 1.8) {
            this.bikeStarted = value;
            this.bikePosition = this.at(-1.05, 0.0, 0.0);
            this.play(SoundEvents.IRON_GOLEM_ATTACK, this.bikePosition, 1.0F, 0.6F);
         }

         if (this.bikeStarted >= 0.0F && !this.crashed) {
            Vec3 position2 = this.scenePlayer.position();
            Vec3 position3 = position2.subtract(this.bikePosition).multiply(1.0, 0.0, 1.0);
            double length = position3.length();
            if (length > 0.001) {
               this.bikeYaw = (float)Math.toDegrees(Math.atan2(-position3.x, position3.z));
            }

            if (value - this.bikeStarted > 0.65F) {
               this.bikePosition = this.bikePosition.add(position3.normalize().scale(Math.min(length, 0.32)));
               if (length < 0.65 && Math.abs(position2.y - this.bikePosition.y) < 1.8) {
                  this.crashed = true;
                  this.play(SoundEvents.PLAYER_HURT, position2, 1.0F, 0.7F);
                  Minecraft.getInstance().setScreenAndShow(new ExtractRenderStateHelper(this));
               }
            }
         }

         if (this.bikeStarted < 0.0F
            && !this.scenePlayer.isSpectator()
            && eyePosition != 0
            && this.defense.tryPeck(value, position.x * position.x + position.z * position.z, position.y)) {
            this.play(SoundEvents.PLAYER_HURT, this.scenePlayer.position(), 0.45F, 1.15F);
         }

         if (!(value < 44.0F)) {
            int computedValue2 = (int)((value - 44.0F) / 0.65F);
            if (computedValue2 != this.lastChirp) {
               this.lastChirp = computedValue2;
               Vec3 position4 = this.at((computedValue2 % 3 - 1) * 0.3, 0.3, 0.0);
               this.play(SoundEvents.CHICKEN_AMBIENT_BABY.value(), position4, 0.35F, 1.15F + computedValue2 % 3 * 0.1F);
            }
         }
      }
   }

   private Vec3 at(double value, double value2, double value3) {
      double angleRadians = Math.toRadians(180.0F - this.yaw);
      Vec3 origin2 = this.origin;
      double sine = value * Math.cos(angleRadians) + value3 * Math.sin(angleRadians);
      double computedValue = -value;
      double sine2 = computedValue * Math.sin(angleRadians);
      return origin2.add(sine, value2, sine2 + value3 * Math.cos(angleRadians));
   }

   public float peckAmount() {
      return this.hasVisualDamage() ? this.defense.peckAmount(this.seconds()) : 0.0F;
   }

   private void play(SoundEvent soundEvent, Vec3 position, float value, float value2) {
      SoundManager soundManager = Minecraft.getInstance().getSoundManager();
      var chirps2 = this.chirps;
      Predicate<net.minecraft.client.resources.sounds.SoundInstance> predicate = soundInstance -> {
         return !soundManager.isActive(soundInstance);
      };
      chirps2.removeIf(predicate);
      SimpleSoundInstance simpleSoundInstance;
      Identifier texture = soundEvent.location();
      SoundSource nEUTRAL = SoundSource.NEUTRAL;
      RandomSource randomSource = SoundInstance.createUnseededRandom();
      Attenuation attenuation;
      if (soundEvent == SoundEvents.PARROT_FLY) {
         attenuation = Attenuation.NONE;
      } else {
         attenuation = Attenuation.LINEAR;
      }

      simpleSoundInstance = new SimpleSoundInstance(texture, nEUTRAL, value, value2, randomSource, false, 0, attenuation, position.x, position.y, position.z, false);
      SimpleSoundInstance simpleSoundInstance2 = simpleSoundInstance;
      this.chirps.add(simpleSoundInstance2);
      soundManager.play(simpleSoundInstance2);
   }
}
