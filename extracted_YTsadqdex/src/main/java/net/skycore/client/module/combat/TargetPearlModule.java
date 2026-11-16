package net.skycore.client.module.combat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.FreeLookController;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.util.MovementHelper;
import net.skycore.client.util.MovementInput;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class TargetPearlModule extends ClientModule implements MinecraftAccess {
   private boolean awaitingThrow;
   private boolean pearlRequested;
   private static final long TARGET_MEMORY_DURATION = 10000L;
   private final Map<Integer, Integer> recentThrowers;
   private static final int MAX_SIM_TICKS = 160;
   private final NumberSetting distance;
   public static final TargetPearlModule INSTANCE = new TargetPearlModule();
   public Entity rememberedTarget;
   private float pendingYaw;
   private static final int ROTATION_PRIORITY = 50;
   private boolean isThrowing;
   private int movementStopTicks;
   private static final int THROW_COOLDOWN = 2000;
   private long rotationHoldUntil;
   private static final float ROTATION_EPSILON = 4.0F;
   private long rememberedTargetTime;
   private static final double MAX_ACCEPTABLE_ERROR = 2.0;
   private final Map<Integer, Integer> pearlOwnerById;
   private Vec2 server;
   private static final int STOP_TICKS = 2;
   private static final double MAX_DISTANCE_FROM_TARGET = 1.5;
   private ThrownEnderpearl targetPearl;
   private float pendingPitch;
   private static final int THROW_ROTATION_TIMEOUT = 2;
   private Vec3 cachedLanding;
   private int rotationWaitTicks;
   private long lastPearlScan;
   private long lastThrowTime;
   private final BooleanSetting onlyTarget = new BooleanSetting("Target Only", false);
   private static final int PEARL_SCAN_INTERVAL = 35;
   private static final float ROTATION_SPEED = 360.0F;

   private Vec3 simulateTrajectory(float index, float index2) {
      Vec3 throwPos = this.getThrowPos(index, index2);
      Vec3 throwMotion = this.getThrowMotion(index, index2);

      for (int value = 0; value < 160; value++) {
         throwPos = throwPos.add(throwMotion);
         throwMotion = throwMotion.scale(0.99).add(0.0, -0.03, 0.0);
         if (throwPos.y <= 0.0) {
            return this.snapToBlockCenter(throwPos);
         }

         BlockPos blockPos = BlockPos.containing(throwPos);
         if (!mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty()) {
            return this.snapToBlockCenter(throwPos);
         }
      }

      return null;
   }

   private void cleanupPearlTracking() {
      if (mc.player != null) {
         int value = mc.player.tickCount;
         var entries = this.recentThrowers.entrySet();
         entries.removeIf(entry -> {
            return (Integer)entry.getValue() < value;
         });
         var entries2 = this.pearlOwnerById.entrySet();
         Predicate<java.util.Map.Entry> key = entry2 -> {
            ClientLevel level = mc.level;
            Integer key2 = (Integer)entry2.getKey();
            return level.getEntity(key2) == null;
         };
         entries2.removeIf(key);
      }
   }

   private boolean check() {
      if (this.isEnabled()) {
         if (this.isThrowing) {
            if (this.server != null) {
               return true;
            }
         }
      }

      return false;
   }

   private void assignPearlOwner(ThrownEnderpearl thrownEnderpearl) {
      int value = 1;
      if (thrownEnderpearl != null) {
         value = (byte)-1;
         if (mc.level != null) {
            Entity pearlShooterWithoutAssign = this.getPearlShooterWithoutAssign(thrownEnderpearl);
            if (pearlShooterWithoutAssign != null) {
               if (thrownEnderpearl.getOwner() == null) {
                  thrownEnderpearl.setOwner(pearlShooterWithoutAssign);
               }
            } else if (thrownEnderpearl.tickCount <= 8) {
               double x = thrownEnderpearl.getX();
               Entity y = this.findNearestThrower(x, thrownEnderpearl.getY(), thrownEnderpearl.getZ(), 4.0);
               if (y != null) {
                  thrownEnderpearl.setOwner(y);
                  var pearlOwnerById2 = this.pearlOwnerById;
                  value = thrownEnderpearl.getId();
                  Integer value2 = value;
                  pearlOwnerById2.put(value2, y.getId());
               }
            }
         }
      }
   }

   private Vec3 getThrowPos(float value, float value2) {
      double value3 = value;
      float angleRadians = (float)Math.toRadians(value3);
      double x = mc.player.getX() - Mth.cos(angleRadians) * 0.16;
      double y = mc.player.getY();
      double eyeHeight = y + mc.player.getEyeHeight(mc.player.getPose()) - 0.1;
      double z = mc.player.getZ() - Mth.sin(angleRadians) * 0.16;
      return new Vec3(x, eyeHeight, z);
   }

   private Entity getPearlShooterWithoutAssign(ThrownEnderpearl thrownEnderpearl) {
      Entity owner = thrownEnderpearl.getOwner();
      if (owner != null) {
         if (owner.isAlive()) {
            return owner;
         }
      }

      Integer id = this.pearlOwnerById.get(thrownEnderpearl.getId());
      if (id != null) {
         ClientLevel level = mc.level;
         int intermediate = id;
         return level.getEntity(intermediate);
      }

      int pearlOwnerEntityId = this.getPearlOwnerEntityId(thrownEnderpearl);
      Entity entity;
      if (pearlOwnerEntityId != 0) {
         entity = mc.level.getEntity(pearlOwnerEntityId);
      } else {
         entity = null;
      }

      return entity;
   }

   private LivingEntity resolveAuraTarget() {
      long now = System.currentTimeMillis();
      LivingEntity entity = SkycoreClient.modules().get(AttackAuraModule.class).map(AttackAuraModule::currentTarget).orElse(null);
      if (entity != null && entity.isAlive() && !entity.isRemoved()) {
         this.rememberedTarget = entity;
         this.rememberedTargetTime = now;
         return entity;
      }

      if (this.rememberedTarget != null) {
         if (!this.rememberedTarget.isAlive() || this.rememberedTarget.isRemoved()) {
            this.rememberedTarget = null;
            this.rememberedTargetTime = 0L;
            return null;
         }

         if (now - this.rememberedTargetTime < 10000L) {
            return (LivingEntity)this.rememberedTarget;
         }

         this.rememberedTarget = null;
         this.rememberedTargetTime = 0L;
      }

      return null;
   }

   private TargetPearlModule() {
      super("Target Pearl", "Throws a pearl toward the target's pearl landing position", ModuleCategory.COMBAT);
      this.distance = new NumberSetting("Min Distance", 10.0, 8.0, 20.0, 1.0);
      this.targetPearl = null;
      this.cachedLanding = null;
      this.lastPearlScan = 0L;
      this.lastThrowTime = 0L;
      this.rotationHoldUntil = 0L;
      this.isThrowing = false;
      this.server = null;
      this.awaitingThrow = false;
      this.pearlRequested = false;
      this.movementStopTicks = 0;
      this.rotationWaitTicks = 0;
      this.rememberedTarget = null;
      this.rememberedTargetTime = 0L;
      this.pearlOwnerById = new HashMap<>();
      this.recentThrowers = new HashMap<>();
      this.addSettings(this.onlyTarget, this.distance);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onMoveInput);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onPacket);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   private ThrownEnderpearl findBestPearl() {
      var entities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(130.0), entity -> entity instanceof ThrownEnderpearl);
      ThrownEnderpearl thrownEnderpearl = null;
      double mAX_VALUE = Double.MAX_VALUE;
      double value = this.distance.get().floatValue();
      LivingEntity entity2 = this.onlyTarget.get() ? this.resolveAuraTarget() : null;
      if (this.onlyTarget.get() && entity2 == null) {
         return null;
      }

      for (Entity entity3 : entities) {
         if (entity3 instanceof ThrownEnderpearl thrownEnderpearl2 && thrownEnderpearl2.isAlive()) {
            this.assignPearlOwner(thrownEnderpearl2);
            Entity pearlShooter = this.getPearlShooter(thrownEnderpearl2);
            if ((pearlShooter == null || !this.isSameEntity(pearlShooter, mc.player)) && (!this.onlyTarget.get() || this.isPearlFromEntity(thrownEnderpearl2, entity2))) {
               Vec3 position = this.predictLanding(thrownEnderpearl2);
               if (position != null) {
                  double distance2 = mc.player.position().distanceTo(position);
                  if (distance2 >= value && distance2 <= 120.0 && distance2 < mAX_VALUE) {
                     thrownEnderpearl = thrownEnderpearl2;
                     mAX_VALUE = distance2;
                  }
               }
            }
         }
      }

      return thrownEnderpearl;
   }

   private boolean isSameEntity(Entity entity, Entity entity2) {
      byte flag = 0;
      if (entity != null && entity2 != null) {
         if (entity != entity2) {
            if (entity.getId() != entity2.getId()) {
               UUID uUID = entity.getUUID();
               UUID uUID2 = entity2.getUUID();
               if (!uUID.equals(uUID2)) {
                  return false;
               }
            }
         }

         flag = 1;
      } else {
         flag = 0;
      }

      return (flag != 0);
   }

   private boolean applyThrowRotation(float angle, float value) {
      float clampedValue = Mth.clamp(value, -90.0F, 90.0F);
      Rotation rotation = new Rotation(angle, clampedValue);
      RotationManager.update(rotation, 360.0F, 360.0F, 360.0F, 360.0F, 2, 50, false);
      Rotation rotation2 = new Rotation(mc.player.getYRot(), mc.player.getXRot());
      return rotation2.getDelta(rotation) <= 4.0F;
   }

   private boolean hasObstacleBetween(Vec3 position, Vec3 position2) {
      Vec3 position3 = position2.subtract(position);
      double length = position3.length();
      Vec3 position4 = position3.normalize();
      int index = (int)(length / 0.5) + 1;

      for (int value = 1; value < index; value++) {
         double computedValue = value * 0.5;
         Vec3 position5 = position4.scale(computedValue);
         Vec3 position6 = position.add(position5);
         BlockPos blockPos = BlockPos.containing(position6);
         BlockState blockState = mc.level.getBlockState(blockPos);
         VoxelShape collisionShape = blockState.getCollisionShape(mc.level, blockPos);
         if (!collisionShape.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private Vec3 predictLanding(ThrownEnderpearl thrownEnderpearl) {
      Vec3 position = thrownEnderpearl.position();
      Vec3 deltaMovement = thrownEnderpearl.getDeltaMovement();

      for (int value = 0; value < 160; value++) {
         Vec3 position2 = position.add(deltaMovement);
         deltaMovement = deltaMovement.scale(0.99).add(0.0, -0.03, 0.0);
         if (position2.y <= 0.0) {
            return this.snapToBlockCenter(position2);
         }

         BlockPos blockPos = BlockPos.containing(position2);
         if (!mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty()) {
            return this.snapToBlockCenter(position2);
         }

         position = position2;
      }

      return null;
   }

   private float[] calculateYawPitch(Vec3 position) {
      Vec3 eyePosition = mc.player.getEyePosition();
      double x = position.x - eyePosition.x;
      double z = position.z - eyePosition.z;
      float computedValue = (float)Math.toDegrees(Math.atan2(z, x)) - 90.0F;
      double distance2 = eyePosition.distanceTo(position);
      double y = position.y - eyePosition.y;
      int value = y > 5.0 ? 1 : 0;
      float value2 = value != 0 ? 89.0F : 85.0F;
      float computedValue2 = distance2 > 60.0 ? -50.0F : -30.0F;
      if (value != 0) {
         computedValue2 = -80.0F;
      }

      float value3 = distance2 > 60.0 ? 0.5F : 0.42F;
      if (value != 0) {
         value3 = 0.3F;
      }

      float value4 = 0.0F;
      int mAX_VALUE = Integer.MAX_VALUE;
      double mAX_VALUE2 = Double.MAX_VALUE;
      double value5 = value != 0 ? 4.0 : 2.0;

      for (float index = value2; index >= computedValue2; index -= value3) {
         TargetPearlModule.SimulationResult simulationResult = this.simulateWithTicks(computedValue, index, position);
         if (simulationResult != null && simulationResult.error <= value5 && (simulationResult.ticks < mAX_VALUE || simulationResult.ticks == mAX_VALUE && simulationResult.error < mAX_VALUE2)) {
            mAX_VALUE = simulationResult.ticks;
            value4 = index;
            mAX_VALUE2 = simulationResult.error;
         }
      }

      return mAX_VALUE != Integer.MAX_VALUE ? new float[]{computedValue, Mth.clamp(value4, -90.0F, 90.0F)} : null;
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      if (this.isEnabled() && mc.level != null) {
         Packet packet = packetReceiveEvent.getPacket();
         int value = mc.player != null ? mc.player.tickCount : 0;
         if (packet instanceof ClientboundAnimatePacket clientboundAnimatePacket) {
            if (clientboundAnimatePacket.getAction() == 0) {
               this.recentThrowers.put(clientboundAnimatePacket.getId(), value + 12);
            }
         } else if (packet instanceof ClientboundAddEntityPacket clientboundAddEntityPacket && clientboundAddEntityPacket.getType() == EntityTypes.ENDER_PEARL) {
            int data = clientboundAddEntityPacket.getData();
            if (data != 0) {
               this.pearlOwnerById.put(clientboundAddEntityPacket.getId(), data);
               return;
            }

            for (Entry entry : this.recentThrowers.entrySet()) {
               if ((Integer)entry.getValue() >= value) {
                  this.pearlOwnerById.put(clientboundAddEntityPacket.getId(), (Integer)entry.getKey());
                  break;
               }
            }

            Entity x = this.findNearestThrower(clientboundAddEntityPacket.getX(), clientboundAddEntityPacket.getY(), clientboundAddEntityPacket.getZ(), 4.5);
            if (x != null) {
               this.pearlOwnerById.put(clientboundAddEntityPacket.getId(), x.getId());
            }
         }
      }
   }

   private void processPendingThrow() {
      if (this.canThrow() && this.cachedLanding != null) {
         boolean applyThrowRotation = this.applyThrowRotation(this.pendingYaw, this.pendingPitch);
         if (!applyThrowRotation) {
            this.rotationWaitTicks++;
            if (this.rotationWaitTicks < 10) {
               return;
            }
         }

         this.rotationWaitTicks = 0;
         boolean fallFlying = mc.player.isFallFlying();
         if (!this.pearlRequested) {
            if (!fallFlying) {
               this.movementStopTicks = 2;
               this.pearlRequested = true;
               return;
            }

            this.pearlRequested = true;
         }

         if (fallFlying || !MovementInput.hasPlayerMovement()) {
            if (!mc.player.getCooldowns().isOnCooldown(Items.ENDER_PEARL.getDefaultInstance()) && UseItemHelper.getItemSlot(Items.ENDER_PEARL) != -1) {
               UseItemHelper.inventorySwapClick(Items.ENDER_PEARL);
               this.lastThrowTime = System.currentTimeMillis();
            }

            this.finishThrowAndReleaseLook();
         }
      } else {
         this.cancelPendingThrow();
      }
   }

   private Vec3 snapToBlockCenter(Vec3 position) {
      double x = Mth.floor(position.x) + 0.5;
      double roundedDown = Mth.floor(position.y);
      double z = Mth.floor(position.z) + 0.5;
      return new Vec3(x, roundedDown, z);
   }

   private void onPreTick(PlayerTickEndEvent event) {
      int value = 1;
      if (this.isEnabled() && event != null && mc.player != null) {
         value = (byte)-1;
         if (mc.level != null) {
            if (this.movementStopTicks > 0) {
               value = this.movementStopTicks - 1;
               this.movementStopTicks = value;
            }

            if (this.awaitingThrow) {
               this.processPendingThrow();
            } else {
               this.tryThrow();
            }
         }
      }
   }

   private TargetPearlModule.SimulationResult simulateWithTicks(float index, float index2, Vec3 position) {
      Vec3 throwPos = this.getThrowPos(index, index2);
      Vec3 throwMotion = this.getThrowMotion(index, index2);

      for (int value = 0; value < 160; value++) {
         throwPos = throwPos.add(throwMotion);
         throwMotion = throwMotion.scale(0.99).add(0.0, -0.03, 0.0);
         if (throwPos.y <= 0.0) {
            return new TargetPearlModule.SimulationResult(this.snapToBlockCenter(throwPos).distanceTo(position), value + 1);
         }

         BlockPos blockPos = BlockPos.containing(throwPos);
         if (!mc.level.getBlockState(blockPos).getCollisionShape(mc.level, blockPos).isEmpty()) {
            return new TargetPearlModule.SimulationResult(this.snapToBlockCenter(throwPos).distanceTo(position), value + 1);
         }
      }

      return null;
   }

   private void onMoveInput(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled() && playerInputEvent != null) {
         if (mc.player != null && mc.level != null) {
            if (!mc.player.isFallFlying()) {
               if (this.movementStopTicks <= 0 && !this.pearlRequested) {
                  if (this.check()) {
                     MovementHelper.fixMovement(playerInputEvent, this.server.x);
                  }
               } else {
                  playerInputEvent.setMoveVector(Vec2.ZERO);
                  Input input = new Input(false, false, false, false, false, false, playerInputEvent.keyPresses().sprint());
                  playerInputEvent.setKeyPresses(input);
               }
            }
         }
      }
   }

   private boolean isValidLanding(Vec3 position) {
      BlockPos blockPos = BlockPos.containing(position);

      for (int value = 1; value <= 4; value++) {
         VoxelShape blockState = mc.level.getBlockState(blockPos.below(value)).getCollisionShape(mc.level, blockPos.below(value));
         if (!blockState.isEmpty()) {
            return true;
         }
      }

      return position.y <= 1.0;
   }

   private void cancelPendingThrow() {
      this.awaitingThrow = false;
      this.pearlRequested = false;
      this.movementStopTicks = 0;
      this.isThrowing = false;
      this.server = null;
      this.rotationHoldUntil = 0L;
      this.rotationWaitTicks = 0;
      this.releaseLookAfterThrow();
   }

   @Override
   public void onDisable() {
      this.awaitingThrow = false;
      this.pearlRequested = false;
      this.movementStopTicks = 0;
      this.isThrowing = false;
      this.targetPearl = null;
      this.cachedLanding = null;
      this.server = null;
      this.rotationHoldUntil = 0L;
      this.rememberedTarget = null;
      this.rememberedTargetTime = 0L;
      this.rotationWaitTicks = 0;
      this.pearlOwnerById.clear();
      this.recentThrowers.clear();
      RotationManager.INSTANCE.stopRotation();
      FreeLookController.setActive(false);
      super.onDisable();
   }

   private void finishThrowAndReleaseLook() {
      this.awaitingThrow = false;
      this.pearlRequested = false;
      this.isThrowing = false;
      this.server = null;
      this.rotationHoldUntil = 0L;
      this.rotationWaitTicks = 0;
      this.releaseLookAfterThrow();
   }

   @Override
   public boolean affectsGameplay() {
      return true;
   }

   private void updateTargetPearl() {
      this.cleanupPearlTracking();
      long now = System.currentTimeMillis();
      if (now - this.lastPearlScan < 35L) {
         if (this.targetPearl != null && this.targetPearl.isAlive() && this.cachedLanding != null) {
            return;
         }

         if (now - this.lastPearlScan < 17L) {
            return;
         }
      }

      this.lastPearlScan = now;
      this.cachedLanding = null;
      this.targetPearl = this.findBestPearl();
      if (this.targetPearl != null && this.targetPearl.isAlive()) {
         this.cachedLanding = this.predictLanding(this.targetPearl);
         if (this.cachedLanding != null && !this.isValidLanding(this.cachedLanding)) {
            this.cachedLanding = null;
         }
      }
   }

   private boolean isPearlFromEntity(ThrownEnderpearl thrownEnderpearl, Entity entity) {
      if (thrownEnderpearl != null && entity != null) {
         Entity pearlShooter = this.getPearlShooter(thrownEnderpearl);
         if (pearlShooter != null) {
            return this.isSameEntity(pearlShooter, entity);
         }

         int pearlOwnerEntityId = this.getPearlOwnerEntityId(thrownEnderpearl);
         if (pearlOwnerEntityId != 0 && pearlOwnerEntityId == entity.getId()) {
            return true;
         }

         UUID pearlOwnerUuid = this.getPearlOwnerUuid(thrownEnderpearl);
         return pearlOwnerUuid != null && pearlOwnerUuid.equals(entity.getUUID());
      } else {
         return false;
      }
   }

   public static boolean isRotationActive() {
      var optional = SkycoreClient.modules().get(TargetPearlModule.class);
      Function<net.skycore.client.module.combat.TargetPearlModule, java.lang.Boolean> function = TargetPearlModule::isManagingRotation;
      var optional2 = optional.map(function);
      Boolean condition = false;
      Boolean orElse = optional2.orElse(condition);
      return orElse;
   }

   private int getPearlOwnerEntityId(ThrownEnderpearl thrownEnderpearl) {
      Entity owner = thrownEnderpearl.getOwner();
      return owner != null ? owner.getId() : 0;
   }

   private Entity findNearestThrower(double distance2, double distance3, double distance4, double index) {
      Player player = null;
      double index2 = index * index;

      for (Player player2 : mc.level.players()) {
         if (player2 != mc.player) {
            double distanceSquared = player2.getEyePosition().distanceToSqr(distance2, distance3, distance4);
            double distanceSquared2 = player2.position().distanceToSqr(distance2, distance3, distance4);
            double minimum = Math.min(distanceSquared, distanceSquared2);
            if (minimum < index2) {
               index2 = minimum;
               player = player2;
            }
         }
      }

      return player;
   }

   private float[] findAnyWorkingRotation(Vec3 position) {
      Vec3 eyePosition = mc.player.getEyePosition();
      double x = position.x - eyePosition.x;
      double z = position.z - eyePosition.z;
      float computedValue = (float)Math.toDegrees(Math.atan2(z, x)) - 90.0F;
      double distance2 = eyePosition.distanceTo(position);
      double y = position.y - eyePosition.y;
      int value = y > 5.0 ? 1 : 0;
      float value2 = value != 0 ? 89.0F : 85.0F;
      float computedValue2 = distance2 > 60.0 ? -70.0F : -50.0F;
      if (value != 0) {
         computedValue2 = -85.0F;
      }

      float value3 = distance2 > 60.0 ? 1.2F : 1.5F;
      if (value != 0) {
         value3 = 1.0F;
      }

      float value4 = distance2 > 60.0 ? 30.0F : 20.0F;
      if (value != 0) {
         value4 = 40.0F;
      }

      float value5 = distance2 > 60.0 ? 3.0F : 2.5F;
      if (value != 0) {
         value5 = 2.0F;
      }

      double distance3 = distance2 > 60.0 ? 3.0 : 2.25;
      if (value != 0) {
         distance3 = 4.5;
      }

      for (float index = value2; index >= computedValue2; index -= value3) {
         Vec3 position2 = this.simulateTrajectory(computedValue, index);
         if (position2 != null && position.distanceTo(position2) <= distance3) {
            return new float[]{computedValue, Mth.clamp(index, -90.0F, 90.0F)};
         }
      }

      for (float index2 = -value4; index2 <= value4; index2 += value5) {
         if (index2 != 0.0F) {
            float index3 = computedValue + index2;

            for (float index4 = value2; index4 >= computedValue2; index4 -= value3) {
               Vec3 position3 = this.simulateTrajectory(index3, index4);
               if (position3 != null && position.distanceTo(position3) <= distance3) {
                  return new float[]{index3, Mth.clamp(index4, -90.0F, 90.0F)};
               }
            }
         }
      }

      if (value != 0) {
         for (float index5 = 89.0F; index5 >= 75.0F; index5--) {
            for (float index6 = -value4; index6 <= value4; index6 += value5) {
               float computedValue3 = computedValue + index6;
               Vec3 position4 = this.simulateTrajectory(computedValue3, index5);
               if (position4 != null && position.distanceTo(position4) <= distance3 * 1.5) {
                  return new float[]{computedValue3, Mth.clamp(index5, -90.0F, 90.0F)};
               }
            }
         }
      }

      for (float index7 = value2; index7 >= 70.0F; index7 -= 2.0F) {
         for (float index8 = -value4; index8 <= value4; index8 += value5 * 2.0F) {
            float computedValue4 = computedValue + index8;
            Vec3 position5 = this.simulateTrajectory(computedValue4, index7);
            double distance4 = value != 0 ? 6.0 : 3.75;
            if (position5 != null && position.distanceTo(position5) <= distance4) {
               return new float[]{computedValue4, Mth.clamp(index7, -90.0F, 90.0F)};
            }
         }
      }

      return null;
   }

   private boolean canThrow() {
      ItemCooldowns cooldowns = mc.player.getCooldowns();
      if (!cooldowns.isOnCooldown(Items.ENDER_PEARL.getDefaultInstance())) {
         if (UseItemHelper.getItemSlot(Items.ENDER_PEARL) != -1) {
            return true;
         }
      }

      return false;
   }

   private Vec3 getThrowMotion(float value, float value2) {
      double value3 = 1.5;
      double value4 = value;
      float angleRadians = (float)Math.toRadians(value4);
      double value5 = value2;
      float angleRadians2 = (float)Math.toRadians(value5);
      float sine = -Mth.sin(angleRadians);
      double value6 = angleRadians2;
      double computedValue = sine * Mth.cos(value6) * value3;
      double computedValue2 = -Mth.sin(angleRadians2) * value3;
      double value7 = angleRadians;
      float cosine = Mth.cos(value7);
      double value8 = angleRadians2;
      double computedValue3 = cosine * Mth.cos(value8) * value3;
      Vec3 deltaMovement = mc.player.getDeltaMovement();
      computedValue2 += deltaMovement.y;
      return new Vec3(computedValue, computedValue2, computedValue3);
   }

   private void releaseLookAfterThrow() {
      if (mc.player == null) {
         if (RotationManager.INSTANCE != null) {
            RotationManager.INSTANCE.stopRotation();
         }
      } else {
         float yRot = FreeLookController.isActive() ? FreeLookController.getFreeYaw() : mc.player.getYRot();
         float xRot = FreeLookController.isActive() ? FreeLookController.getFreePitch() : mc.player.getXRot();
         mc.player.setYRot(yRot);
         mc.player.setXRot(xRot);
         mc.player.yHeadRot = yRot;
         mc.player.yBodyRot = yRot;
         if (RotationManager.INSTANCE != null) {
            RotationManager.INSTANCE.stopRotation();
         }

         FreeLookController.setActive(false);
         FreeLookController.setFreeYaw(yRot);
         FreeLookController.setFreePitch(xRot);
      }
   }

   private void tryThrow() {
      if (System.currentTimeMillis() - this.lastThrowTime >= 2000L && this.canThrow()) {
         this.updateTargetPearl();
         if (this.cachedLanding != null) {
            float[] floatValues = this.calculateYawPitch(this.cachedLanding);
            if (floatValues == null) {
               floatValues = this.findAnyWorkingRotation(this.cachedLanding);
               if (floatValues == null) {
                  return;
               }
            }

            Vec3 position = this.simulateTrajectory(floatValues[0], floatValues[1]);
            if (position != null) {
               double distance2 = this.cachedLanding.distanceTo(position);
               Vec3 eyePosition = mc.player.getEyePosition();
               double distance3 = eyePosition.distanceTo(this.cachedLanding);
               double y = this.cachedLanding.y - eyePosition.y;
               int value = y > 5.0 ? 1 : 0;
               double value2 = distance3 > 60.0 ? 3.0 : 2.25;
               if (value != 0) {
                  value2 = 4.5;
               }

               if (this.hasObstacleBetween(eyePosition, this.cachedLanding)) {
                  value2 = value != 0 ? 6.0 : 3.75;
               }

               if (distance2 > value2) {
                  floatValues = this.findAnyWorkingRotation(this.cachedLanding);
                  if (floatValues == null) {
                     return;
                  }
               }
            }

            this.pendingYaw = floatValues[0];
            this.pendingPitch = floatValues[1];
            this.isThrowing = true;
            this.server = new Vec2(this.pendingYaw, this.pendingPitch);
            this.awaitingThrow = true;
            this.applyThrowRotation(this.pendingYaw, this.pendingPitch);
         }
      }
   }

   private Entity getPearlShooter(ThrownEnderpearl thrownEnderpearl) {
      int value = 1;
      value = (byte)-1;
      this.assignPearlOwner(thrownEnderpearl);
      Entity owner = thrownEnderpearl.getOwner();
      if (owner != null) {
         if (owner.isAlive()) {
            return owner;
         }
      }

      var pearlOwnerById2 = this.pearlOwnerById;
      value = thrownEnderpearl.getId();
      Integer value2 = (Integer)pearlOwnerById2.get(value);
      if (value2 != null) {
         int computedValue = -1;
         if (mc.level != null) {
            computedValue = (byte)1;
            ClientLevel level = mc.level;
            computedValue = value2;
            return level.getEntity(computedValue);
         }
      }

      int pearlOwnerEntityId = this.getPearlOwnerEntityId(thrownEnderpearl);
      Entity entity;
      if (pearlOwnerEntityId != 0 && mc.level != null) {
         entity = mc.level.getEntity(pearlOwnerEntityId);
      } else {
         entity = null;
      }

      return entity;
   }

   private UUID getPearlOwnerUuid(ThrownEnderpearl thrownEnderpearl) {
      Entity owner = thrownEnderpearl.getOwner();
      return owner != null ? owner.getUUID() : null;
   }

   public boolean isManagingRotation() {
      byte flag = 0;
      if (!this.isEnabled()) {
         flag = 0;
      } else {
         if (!this.awaitingThrow) {
            pearlSelection:
            if (!this.isThrowing) {
               if (this.rotationHoldUntil > 0L) {
                  if (System.currentTimeMillis() < this.rotationHoldUntil) {
                     break pearlSelection;
                  }
               }

               return false;
            }
         }

         flag = 1;
      }

      return flag != 0;
   }

   static final class SimulationResult {
      final int ticks;
      final double error;

      public SimulationResult(double error2, int ticks2) {
         this.error = error2;
         this.ticks = ticks2;
      }
   }
}
