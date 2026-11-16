package net.skycore.client.module.combat;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.FreeLookController;
import net.skycore.client.combat.InputPredictor;
import net.skycore.client.combat.LookHelper;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationApplier;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.combat.ServerRotationManager;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PlayerUpdateEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class AimbotModule extends ClientModule {
   private final BooleanSetting otvodka;
   private final MultiSelectSetting targets;
   private static final String TARGET_FRIENDS = "Друзья";
   private final SliderSetting range = this.add(new SliderSetting("Дистанция", 40.0, 10.0, 80.0, 1.0));
   private static final String TARGET_MOBS = "Мобы";
   private static final String TARGET_NAKED = "Голые";
   private final ArrayDeque<Runnable> returnSteps;
   private static final String TARGET_PLAYERS = "Игроки";
   private static final int ROTATION_PRIORITY = 7;
   private static final float SNAP_SPEED = 360.0F;
   private int otvodkaGen;
   private boolean otvodkaActive;
   public static final AimbotModule INSTANCE = new AimbotModule();
   private static final String TARGET_ANIMALS = "Животные";

   private Rotation calculateBowRotation(LivingEntity entity) {
      Minecraft instance = Minecraft.getInstance();
      double x = Math.abs(instance.player.getX() - entity.getX());
      double z = Math.abs(instance.player.getZ() - entity.getZ());
      double computedValue = Math.hypot(x, z);
      int delta = Math.max((int)(computedValue / 1.5 + 2.0), 3);
      Vec3 position;
      if (entity instanceof Player player) {
         position = InputPredictor.simulateOtherPlayer(player, delta).boundingBox().getCenter();
      } else {
         position = entity.getBoundingBox().getCenter().add(entity.getDeltaMovement().scale(delta));
      }

      float useItem = Math.max(0, instance.player.getUseItem().getUseDuration(instance.player) - instance.player.getUseItemRemainingTicks());
      float computedValue2 = useItem / 20.0F;
      computedValue2 += computedValue2 / 3.0F;
      if (computedValue2 > 1.0F) {
         computedValue2 = 1.0F;
      }

      Rotation rotation = LookHelper.calculateAngle(position);
      float yaw = rotation.getYaw();
      float pitch = rotation.getPitch() - (float)(computedValue / 7.0);
      pitch -= 20.0F - computedValue2 * 20.0F;
      if (!instance.player.onGround()) {
         pitch += (float)(instance.player.getDeltaMovement().y * ((1.0F - computedValue2) * 20.0F));
      }

      yaw += Mth.clamp(instance.player.input.getMoveVector().x * 100.0F, -4.0F, 4.0F);
      return new Rotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
   }

   public void onPlayerTickStart() {
      Runnable runnable = this.returnSteps.pollFirst();
      if (runnable != null) {
         runnable.run();
      }
   }

   @Override
   protected void onDisable() {
      this.clearOtvodkaState();
      this.releaseRotationControl();
   }

   public boolean isOtvodkaActive() {
      if (this.isEnabled()) {
         if (this.otvodkaActive) {
            return true;
         }
      }

      return false;
   }

   public void clearOtvodkaState() {
      this.otvodkaGen++;
      this.otvodkaActive = false;
   }

   private void onStopUsingItem(PlayerUpdateEvent playerUpdateEvent) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null && instance.level != null && instance.getConnection() != null) {
            if (playerUpdateEvent.player() == instance.player) {
               if (instance.player.isUsingItem() && instance.player.getUseItem().is(Items.BOW)) {
                  LivingEntity target = this.findTarget();
                  if (target != null) {
                     float yRot = FreeLookController.isActive() ? FreeLookController.getFreeYaw() : instance.player.getYRot();
                     float xRot = FreeLookController.isActive() ? FreeLookController.getFreePitch() : instance.player.getXRot();
                     FreeLookController.setActive(true);
                     FreeLookController.setFreeYaw(yRot);
                     FreeLookController.setFreePitch(xRot);
                     Rotation rotation = this.calculateBowRotation(target);
                     float yRot2 = RotationApplier.aimModulo360(instance.player.getYRot(), rotation.getYaw());
                     float pitch = Mth.clamp(rotation.getPitch(), -90.0F, 90.0F);
                     Rotation rotation2 = new Rotation(yRot2, pitch);
                     RotationManager.update(rotation2, 360.0F, 360.0F, 360.0F, 360.0F, this.otvodka.get() ? 20 : 0, 7, false);
                     RotationApplier.applyBodyYaw(yRot2);
                     instance.player.setXRot(pitch);
                     instance.player
                        .connection
                        .send(
                           new PosRot(
                              instance.player.getX(), instance.player.getY(), instance.player.getZ(), yRot2, pitch, instance.player.onGround(), instance.player.horizontalCollision
                           )
                        );
                     if (this.otvodka.get()) {
                        this.startOtvodka(yRot, xRot);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean isValidTarget(LivingEntity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (entity != null && entity != instance.player && entity.isAlive() && !(entity instanceof ArmorStand) && entity.tickCount >= 3) {
         if (CriticalHelper.getStrictDistance(entity) > this.range.get()) {
            return false;
         }

         if (entity instanceof Player player) {
            if (AntiBotModule.isBot(player)) {
               return false;
            }

            if (player.getName().getString().equalsIgnoreCase(instance.player.getName().getString())) {
               return false;
            }

            boolean name = FriendManager.INSTANCE.isFriend(player.getName().getString());
            if (name && !this.targets.isSelected("Друзья")) {
               return false;
            }

            int armorValue = player.getArmorValue() == 0 ? 1 : 0;
            return armorValue != 0 && this.targets.isSelected("Голые") ? true : this.targets.isSelected("Игроки") || name && this.targets.isSelected("Друзья");
         } else if (entity instanceof AgeableMob) {
            return this.targets.isSelected("Животные");
         } else {
            return entity instanceof Monster ? this.targets.isSelected("Мобы") : false;
         }
      } else {
         return false;
      }
   }

   private void releaseRotationControl() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         RotationManager.INSTANCE.stopRotation();
         ServerRotationManager.INSTANCE.stopRotation();
         FreeLookController.setActive(false);
      } else {
         if (FreeLookController.isActive()) {
            float yRot = RotationApplier.aimModulo360(instance.player.getYRot(), FreeLookController.getFreeYaw());
            float freePitch = FreeLookController.getFreePitch();
            RotationApplier.applyBodyYaw(yRot);
            instance.player.setXRot(freePitch);
         }

         RotationManager.INSTANCE.stopRotation();
         ServerRotationManager.INSTANCE.stopRotation();
         FreeLookController.setActive(false);
         FreeLookController.setFreeYaw(instance.player.getYRot());
         FreeLookController.setFreePitch(instance.player.getXRot());
      }
   }

   private void startOtvodka(float yaw, float pitch) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         this.releaseRotationControl();
      } else {
         int computedValue = ++this.otvodkaGen;
         this.otvodkaActive = true;
         FreeLookController.setActive(true);
         FreeLookController.setFreeYaw(yaw);
         FreeLookController.setFreePitch(pitch);
         float yRot = instance.player.getYRot();
         float xRot = instance.player.getXRot();
         float computedValue2 = Mth.wrapDegrees(yaw - yRot);
         float computedValue3 = pitch - xRot;

         for (int value = 1; value <= 2; value++) {
            float computedValue4 = value / 2.0F;
            float computedValue5 = 1.0F - (1.0F - computedValue4) * (1.0F - computedValue4);
            float angle = yRot + computedValue2 * computedValue5;
            float clampedValue = Mth.clamp(xRot + computedValue3 * computedValue5, -90.0F, 90.0F);
            this.returnSteps.addLast(() -> {
               if (computedValue == this.otvodkaGen && instance.player != null) {
                  float yRot2 = instance.player.getYRot();
                  float angle2 = RotationApplier.aimModulo360(yRot2, angle);
                  RotationManager.update(new Rotation(angle2, clampedValue), 90.0F, 90.0F, 120.0F, 120.0F, 1, 7, false);
                  float yRot3 = instance.player.getYRot();
                  RotationApplier.applyBodyYaw(yRot3);
               }
            });
         }

         this.returnSteps.addLast(() -> {
            if (computedValue == this.otvodkaGen) {
               this.otvodkaActive = false;
               this.releaseRotationControl();
            }
         });
      }
   }

   private AimbotModule() {
      super("Aimbot", "Snap на цель в момент выстрела из лука (до отпускания стрелы)", ModuleCategory.COMBAT);
      this.targets = this.add(new MultiSelectSetting("Цели", List.of("Игроки", "Друзья", "Голые", "Животные", "Мобы"), Set.of("Игроки", "Голые")));
      this.otvodka = this.add(new BooleanSetting("Отводка", true));
      this.returnSteps = new ArrayDeque<>();
      SkycoreClient.events().subscribe(PlayerUpdateEvent.class, this::onStopUsingItem);
   }

   private LivingEntity findTarget() {
      Minecraft instance = Minecraft.getInstance();
      Double value = this.range.get();
      double value2 = value;
      ClientLevel level = instance.level;
      AABB boundingBox = instance.player.getBoundingBox().inflate(value2);
      Predicate<net.minecraft.world.entity.LivingEntity> predicate = this::isValidTarget;
      return level.getEntitiesOfClass(LivingEntity.class, boundingBox, predicate)
         .stream()
         .min(Comparator.comparingDouble(CriticalHelper::getStrictDistance))
         .orElse(null);
   }
}
