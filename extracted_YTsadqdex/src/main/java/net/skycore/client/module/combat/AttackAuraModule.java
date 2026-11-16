package net.skycore.client.module.combat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionStop;
import net.skycore.client.combat.AuraRotation;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.FreeLookController;
import net.skycore.client.combat.InputPredictor;
import net.skycore.client.combat.RayTraceHelper;
import net.skycore.client.combat.RotationController;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.combat.ServerRotationManager;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.movement.AirStuckModule;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.ScoreboardModeHelper;
import net.skycore.client.setting.BooleanSetting;

public final class AttackAuraModule extends ClientModule {
   private final ModeSetting movementCorrection;
   private final BooleanSetting clientLook;
   private int lastRotateTick;
   private final AuraRotation rotations;
   private long lastAttackTime;
   private final BooleanSetting hitThroughDoors;
   private final ModeSetting sortMode;
   private final BooleanSetting onlyCriticals;
   private final BooleanSetting smartCriticals;
   private final ModeSetting rotationBehavior;
   private final BooleanSetting breakShield;
   private boolean canCrit;
   private final BooleanSetting releaseShield;
   private final BooleanSetting rayTrace;
   private int skycoreNextAttackTick;
   private final ModeSetting rotationMode = this.add(new ModeSetting("Rotation mode", "ReallyWorld", "ReallyWorld", "HvH", "Funtime", "HolyWorldLegit"));
   private boolean hitImminent;
   private final NumberSetting pursuitRange;
   private final BooleanSetting bypassWalls;
   private long attackCounter;
   private LivingEntity target;
   private final BooleanSetting disableOnDeath;
   private final BooleanSetting ignoreEating;
   private final NumberSetting attackRange;
   private static final long ATTACK_DELAY_MS = 473L;
   private final BooleanSetting tpsSync;
   private final MultiSelectSetting targets;
   private final BooleanSetting throughWalls;
   private int skycoreTick;

   public boolean canPlayerCritical() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         return false;
      }

      if (!player.hasEffect(MobEffects.LEVITATION)) {
         if (!player.hasEffect(MobEffects.BLINDNESS)) {
            if (!player.isInWater()) {
               if (!player.isEyeInFluid(FluidTags.WATER) && !player.isEyeInFluid(FluidTags.LAVA) && !player.getAbilities().flying) {
                  if (!player.isFallFlying() && !player.onClimbable()) {
                     if (!player.isPassenger() && !player.onGround() && player.getDeltaMovement().y < 0.0 && player.fallDistance > 0.0) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean onlyCriticalsEnabled() {
      return this.onlyCriticals.get();
   }

   public long attackCounter() {
      return this.attackCounter;
   }

   private double getMaxAimRange() {
      LocalPlayer instance = Minecraft.getInstance().player;
      double value = this.attackDistance();
      return instance != null && instance.isFallFlying() && ElytraTargetModule.INSTANCE.isEnabled() ? Math.max(value, this.pursuitRange.get().doubleValue()) : value;
   }

   private void breakShieldPlayer(LocalPlayer player, Player player2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.gameMode != null && player2.isBlocking()) {
         int axeSlot = this.findAxeSlot(player);
         if (axeSlot >= 0) {
            int selectedSlot = player.getInventory().getSelectedSlot();
            if (axeSlot < 9) {
               player.getInventory().setSelectedSlot(axeSlot);
               instance.gameMode.attack(player, player2);
               player.swing(InteractionHand.MAIN_HAND);
               player.getInventory().setSelectedSlot(selectedSlot);
            } else {
               instance.gameMode.handleContainerInput(player.inventoryMenu.containerId, axeSlot, selectedSlot, ContainerInput.SWAP, player);
               instance.gameMode.attack(player, player2);
               player.swing(InteractionHand.MAIN_HAND);
               instance.gameMode.handleContainerInput(player.inventoryMenu.containerId, axeSlot, selectedSlot, ContainerInput.SWAP, player);
            }
         }
      }
   }

   public AttackAuraModule() {
      super("Attack Aura", "Attacks nearby targets with silent aim", ModuleCategory.COMBAT);
      this.rotationBehavior = this.add(new ModeSetting("Rotation behavior", "Smooth", "Smooth", "Snaps"));
      this.targets = this.add(new MultiSelectSetting("Targets", List.of("Players", "Friends", "Naked", "Animals", "Mobs"), Set.of("Players", "Naked")));
      this.movementCorrection = this.add(new ModeSetting("Movement correction", "Free", "Free", "Focused", "Target"));
      this.attackRange = this.add(new NumberSetting("Attack range", "", 3.0, 2.5, 6.0, 0.1, " blocks"));
      this.pursuitRange = this.add(new NumberSetting("Pursuit range", "", 1.5, 0.0, 3.0, 0.1, " blocks"));
      this.sortMode = this.add(new ModeSetting("Sort Mode", "Distance", "Distance", "Health", "Armor", "All"));
      this.disableOnDeath = this.add(new BooleanSetting("Disable on death", true));
      this.ignoreEating = this.add(new BooleanSetting("Don't attack while eating", false));
      this.tpsSync = this.add(new BooleanSetting("TPS Sync", false));
      this.onlyCriticals = this.add(new BooleanSetting("Only crits", true));
      this.smartCriticals = this.add(new BooleanSetting("Smart crits", false));
      this.releaseShield = this.add(new BooleanSetting("Release shield", true));
      this.breakShield = this.add(new BooleanSetting("Break Shield", true));
      this.bypassWalls = this.add(new BooleanSetting("RW wall bypass", false));
      this.rayTrace = this.add(new BooleanSetting("Ray trace", true));
      this.throughWalls = this.add(new BooleanSetting("Through walls", true));
      this.hitThroughDoors = this.add(new BooleanSetting("Hit through doors", false));
      this.clientLook = this.add(new BooleanSetting("Client Look", false));
      this.rotations = new AuraRotation();
      this.canCrit = true;
      this.lastRotateTick = -1;
      this.rotationBehavior.visibleWhen(() -> {
         if (!this.rotationMode.is("ReallyWorld")) {
            if (!this.rotationMode.is("Funtime")) {
               return false;
            }
         }

         return true;
      });
      this.bypassWalls.visibleWhen(() -> {
         return this.rotationMode.is("ReallyWorld");
      });
      this.hitThroughDoors.visibleWhen(() -> {
         Boolean get = this.rayTrace.get();
         if (get) {
            Boolean get2 = this.throughWalls.get();
            if (!get2) {
               return true;
            }
         }

         return false;
      });
      this.smartCriticals.visibleWhen(this.onlyCriticals::get);
      this.clientLook.visibleWhen(() -> {
         return !this.rotationMode.is("HolyWorldLegit");
      });
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onPlayerInput);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   public LivingEntity currentTarget() {
      return this.isEnabled() ? this.target : null;
   }

   private boolean shouldBlockAttack(LocalPlayer player) {
      if (ElytraTargetModule.INSTANCE.isEnabled() && player.isFallFlying()) {
         if (player.isUsingItem()) {
            if (!player.isBlocking()) {
               Boolean get = this.ignoreEating.get();
               if (get) {
                  return true;
               }
            }
         }

         return false;
      } else {
         if (player.isUsingItem()) {
            Boolean get2 = this.ignoreEating.get();
            if (get2 && !player.isFallFlying()) {
               return true;
            }
         }

         return false;
      }
   }

   private LivingEntity findTarget() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         ArrayList<LivingEntity> livingEntitys = new ArrayList<>();

         for (Entity entity : instance.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity entity2 && this.isValidTarget(entity2)) {
               livingEntitys.add(entity2);
            }
         }

         if (livingEntitys.isEmpty()) {
            return null;
         }

         if (livingEntitys.size() > 1) {
            if (this.sortMode.is("All")) {
               livingEntitys.sort(Comparator.<LivingEntity>comparingDouble(entity3 -> {
                  return entity3 instanceof Player player2 ? -this.getEntityArmor(player2) : -entity3.getArmorValue();
               }).thenComparingDouble(this::getEntityHealth).thenComparingDouble(CriticalHelper::getStrictDistance));
            } else if (this.sortMode.is("Distance")) {
               livingEntitys.sort(Comparator.comparingDouble(CriticalHelper::getStrictDistance));
            } else if (this.sortMode.is("Armor")) {
               livingEntitys.sort(Comparator.comparingDouble(entity4 -> {
                  return entity4 instanceof Player player3 ? this.getEntityArmor(player3) : entity4.getArmorValue();
               }));
            } else if (this.sortMode.is("Health")) {
               livingEntitys.sort(Comparator.comparingDouble(this::getEntityHealth));
            }
         }

         return (LivingEntity)livingEntitys.get(0);
      } else {
         return null;
      }
   }

   private void updateRotation(boolean condition) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && this.target != null && this.isValidTarget(this.target)) {
         if (!TargetPearlModule.isRotationActive()) {
            this.rotations
               .tick(
                  this,
                  instance,
                  this.target,
                  this.rotationMode.get(),
                  this.rotationBehavior.get(),
                  condition,
                  Boolean.TRUE.equals(this.bypassWalls.get()),
                  Boolean.TRUE.equals(this.breakShield.get()),
                  this.attackDistance()
               );
            this.lastRotateTick = instance.tickCount;
         }
      }
   }

   private boolean funtimeCameraHitsTarget(LocalPlayer player) {
      if (this.target == null) {
         return false;
      } else if (!player.getInBlockState().is(Blocks.COBWEB) && !this.target.getInBlockState().is(Blocks.COBWEB)) {
         float yRot = FreeLookController.isActive() ? FreeLookController.getFreeYaw() : player.getYRot();
         float xRot = FreeLookController.isActive() ? FreeLookController.getFreePitch() : player.getXRot();
         int value = !this.throughWalls.get() ? 1 : 0;
         int value2 = value != 0 && this.hitThroughDoors.get() ? 1 : 0;
         return RayTraceHelper.rayTraceEntity(player, this.target, yRot, xRot, this.attackDistance() + 1.0, (value != 0), (value2 != 0));
      } else {
         return true;
      }
   }

   public ModeSetting rotationBehavior() {
      return this.rotationBehavior;
   }

   private static double direction(float value, double value2, double value3) {
      if (value2 < 0.0) {
         value += 180.0F;
      }

      float value4 = 1.0F;
      if (value2 < 0.0) {
         value4 = -0.5F;
      } else if (value2 > 0.0) {
         value4 = 0.5F;
      }

      if (value3 > 0.0) {
         value -= 90.0F * value4;
      }

      if (value3 < 0.0) {
         value += 90.0F * value4;
      }

      return Math.toRadians(value);
   }

   public float getAICooldown() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         return 0.93F;
      } else {
         ItemStack mainHandItem = player.getMainHandItem();
         if (!mainHandItem.isEmpty() && mainHandItem.getItem() != Items.AIR) {
            return !mainHandItem.is(ItemTags.AXES) && !mainHandItem.is(Items.WOODEN_SHOVEL) ? 0.93F : 0.95F;
         } else {
            return 0.9F;
         }
      }
   }

   private void updateBeforePhysics(PlayerTickEndEvent playerTickEndEvent) {
      this.skycoreTick++;
      if (this.isEnabled()) {
         if (!CrystalAuraModule.INSTANCE.isRotating()) {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = playerTickEndEvent.player();
            if (player == null || instance.level == null) {
               this.resetState();
            } else if (this.disableOnDeath.get() && !player.isAlive()) {
               this.setEnabled(false);
            } else if (this.isManualBlockPlacement(player, instance)) {
               this.hitImminent = false;
               if (this.target != null && this.isValidTarget(this.target)) {
                  RotationController.setClientLook(this.clientLookEnabled());
                  this.updateRotation(false);
               }
            } else {
               if (ElytraTargetModule.INSTANCE.isEnabled() && player.isFallFlying()) {
                  this.target = this.findSkycoreTarget();
               } else if (this.target == null || !this.isValidTarget(this.target)) {
                  this.target = this.findTarget();
               }

               this.refreshCanCrit(player);
               if (this.target == null) {
                  this.hitImminent = false;
                  RotationController.setClientLook(false);
                  this.releaseRotationControl();
               } else if (!player.isFallFlying()
                  && (!ElytraTargetModule.INSTANCE.isEnabled() || !player.isFallFlying())
                  && !this.throughWalls.get()
                  && !this.hitThroughDoors.get()
                  && !player.hasLineOfSight(this.target)) {
                  this.target = null;
                  this.hitImminent = false;
                  this.releaseRotationControl();
               } else {
                  int value = this.canAttack() && !this.shouldBlockAttack(player) ? 1 : 0;
                  RotationController.setClientLook(this.clientLookEnabled());
                  if (this.rotationMode.is("ReallyWorld")) {
                     this.updateRotation((value != 0));
                  } else {
                     this.onGameUpdate();
                  }

                  this.hitImminent = ((value != 0 && this.isWithinAttackRange(this.target) ? 1 : 0) != 0);
                  if (this.hitImminent) {
                     AutoMaceModule.prepareForHit(this.target);
                     if (this.updateAttack()) {
                        this.lastAttackTime = System.currentTimeMillis();
                     }
                  }
               }
            }
         }
      }
   }

   private void moveToPosition(PlayerInputEvent playerInputEvent, Vec3 position, float value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         double x = position.x - instance.getX();
         double z = position.z - instance.getZ();
         double computedValue = Mth.wrapDegrees((float)(Math.toDegrees(Math.atan2(z, x)) - 90.0));
         float value2 = 0.0F;
         float value3 = 0.0F;
         double mAX_VALUE = Double.MAX_VALUE;

         for (float index = -1.0F; index <= 1.0F; index++) {
            for (float index2 = -1.0F; index2 <= 1.0F; index2++) {
               if (index != 0.0F || index2 != 0.0F) {
                  double computedValue2 = Math.toDegrees(direction(value, index, index2));
                  double computedValue3 = Math.abs(computedValue - Mth.wrapDegrees((float)computedValue2));
                  if (computedValue3 < mAX_VALUE) {
                     mAX_VALUE = computedValue3;
                     value2 = index;
                     value3 = index2;
                  }
               }
            }
         }

         playerInputEvent.setMoveVector(new Vec2(value3, value2));
         Input input = playerInputEvent.keyPresses();
         playerInputEvent.setKeyPresses(new Input(value2 > 0.0F, value2 < 0.0F, value3 > 0.0F, value3 < 0.0F, input.jump(), input.shift(), input.sprint()));
      }
   }

   public double attackDistance() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (player.isFallFlying() && ElytraTargetModule.INSTANCE.isEnabled()) {
            return ElytraTargetModule.INSTANCE.attackRange.get();
         }
      }

      float killAuraDistance = AirStuckModule.INSTANCE.getKillAuraDistance();
      double result;
      if (killAuraDistance > 0.0F) {
         result = killAuraDistance;
      } else {
         result = this.attackRange.get();
      }

      return result;
   }

   @Override
   protected void onTick() {
   }

   public ModeSetting rotationMode() {
      return this.rotationMode;
   }

   private int findAxeSlot(LocalPlayer player) {

      for (int value = 0; value < 36; value++) {
         Inventory inventory = player.getInventory();
         ItemStack item = inventory.getItem(value);
         if (!item.isEmpty()) {
            if (item.is(ItemTags.AXES)) {
               return value;
            }
         }
      }

      return -1;
   }

   public void onProfileLoaded() {
      if (this.isEnabled()) {
         this.resetState();
      }
   }

   private void releaseRotationControl() {
      this.rotations.reset();
      ServerRotationManager.INSTANCE.stopRotation();
      RotationManager.INSTANCE.stopRotation();
      RotationController.releaseToFreeLook();
   }

   private double skycoreHealth(LivingEntity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (entity instanceof Player && instance.getCurrentServer() != null) {
         if (ScoreboardModeHelper.scoreboardMode()) {
            Scoreboard scoreboard = instance.level.getScoreboard();
            Objective displayObjective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
            if (displayObjective != null) {
               String scoreboardName = entity.getScoreboardName();
               ScoreAccess orCreatePlayerScore = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(scoreboardName), displayObjective);
               return orCreatePlayerScore.get();
            }
         }
      }

      return entity.getHealth();
   }

   private boolean passesRayCheck(LocalPlayer player) {
      int inBlockState = !player.getInBlockState().is(Blocks.COBWEB) && (this.target == null || !this.target.getInBlockState().is(Blocks.COBWEB)) ? 0 : 1;
      if (inBlockState != 0) {
         return true;
      }

      float yRot = RotationController.isActive() ? RotationController.getServerYaw() : player.getYRot();
      float xRot = RotationController.isActive() ? RotationController.getServerPitch() : player.getXRot();
      int value = !this.throughWalls.get() ? 1 : 0;
      int value2 = value != 0 && this.hitThroughDoors.get() ? 1 : 0;
      boolean condition = RayTraceHelper.rayTraceEntity(player, this.target, yRot, xRot, this.attackDistance() + 1.0, (value != 0), (value2 != 0));
      return condition || this.isWallBypassAllowed(player);
   }

   private double getEntityArmor(Player player) {
      return player.getArmorValue();
   }

   private boolean isValidTarget(LivingEntity entity) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null || entity == instance) {
         return false;
      }

      if (ElytraTargetModule.INSTANCE.isEnabled() && instance.isFallFlying()) {
         if (!entity.isAlive() || entity.distanceTo(instance) > 106.0F) {
            return false;
         }

         if (entity instanceof Player player) {
            if (AntiBotModule.isBot(player)) {
               return false;
            } else if (FriendManager.INSTANCE.isFriend(player.getGameProfile().name())) {
               return this.targets.isSelected("Friends");
            } else {
               return entity.getArmorValue() == 0 ? this.targets.isSelected("Naked") : this.targets.isSelected("Players");
            }
         } else {
            return entity instanceof Mob && (this.targets.isSelected("Mobs") || this.targets.isSelected("Animals"));
         }
      } else if (entity.tickCount >= 3 && !(CriticalHelper.getStrictDistance(entity) >= this.getMaxAimRange())) {
         if (entity instanceof Player player2) {
            if (AntiBotModule.isBot(player2)) {
               return false;
            }

            if (!this.targets.isSelected("Friends") && FriendManager.INSTANCE.isFriend(player2.getGameProfile().name())) {
               return false;
            }

            if (player2.getGameProfile().name().equalsIgnoreCase(instance.getGameProfile().name())) {
               return false;
            }

            if (player2.getArmorValue() == 0 && !this.targets.isSelected("Naked")) {
               return false;
            }

            if (!this.targets.isSelected("Players") && !this.targets.isSelected("Naked") && !this.targets.isSelected("Friends")) {
               return false;
            }
         }

         if (!(entity instanceof Player)) {
            int value = !(entity instanceof Animal) && !(entity instanceof AmbientCreature) ? 0 : 1;
            if (value != 0) {
               if (!this.targets.isSelected("Animals")) {
                  return false;
               }
            } else {
               if (!(entity instanceof Mob)) {
                  return false;
               }

               if (!this.targets.isSelected("Mobs")) {
                  return false;
               }
            }
         }

         return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStand);
      } else {
         return false;
      }
   }

   public void onSlowFallingClientTick() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (CriticalsModule.INSTANCE.usesSlowFallingMode(instance)) {
         this.updateBeforePhysics(new PlayerTickEndEvent(instance));
      }
   }

   private void refreshCanCrit(LocalPlayer player) {
      if (this.target == null) {
         this.canCrit = false;
      } else {
         if (AirStuckModule.INSTANCE.isFrozen()) {
            this.canCrit = true;
         } else {
            if (player.fallDistance > 0.1F) {
               InputPredictor inputPredictor = InputPredictor.simulateLocalPlayer(1);
               if (inputPredictor.onGround()) {
                  this.canCrit = false;
                  return;
               }
            }

            this.canCrit = true;
         }
      }
   }

   public boolean isValidAttackCondition() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return false;
      }

      if (!instance.hasEffect(MobEffects.LEVITATION)) {
         if (!instance.hasEffect(MobEffects.BLINDNESS) && !instance.isInWater()) {
            if (!instance.isEyeInFluid(FluidTags.WATER)) {
               if (!instance.isEyeInFluid(FluidTags.LAVA)) {
                  if (!instance.getInBlockState().is(Blocks.COBWEB) && !instance.getAbilities().flying && !instance.isFallFlying()) {
                     if (!instance.onClimbable() && !instance.isPassenger()) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private LivingEntity findSkycoreTarget() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         float distance = 106.0F;
         ArrayList<LivingEntity> livingEntitys = new ArrayList<>();

         for (Entity entity : instance.level.entitiesForRendering()) {
            if (entity instanceof LivingEntity entity2 && entity2 != player && entity2.isAlive() && !(entity2.distanceTo(player) > distance)) {
               if (entity2 instanceof Player player2) {
                  if (AntiBotModule.isBot(player2)) {
                     continue;
                  }

                  boolean gameProfile = FriendManager.INSTANCE.isFriend(player2.getGameProfile().name());
                  if (gameProfile
                     ? !this.targets.isSelected("Friends")
                     : (entity2.getArmorValue() == 0 ? !this.targets.isSelected("Naked") : !this.targets.isSelected("Players"))) {
                     continue;
                  }
               } else if (!(entity2 instanceof Mob) || !this.targets.isSelected("Mobs") && !this.targets.isSelected("Animals")) {
                  continue;
               }

               livingEntitys.add(entity2);
            }
         }

         Comparator<net.minecraft.world.entity.LivingEntity> comparator = Comparator.comparing(entity3 -> {
            int value = 0;
            value = entity3 != this.target ? 1 : 0;
            return Boolean.valueOf((value != 0));
         });
         if (this.sortMode.is("All")) {
            comparator = comparator.thenComparingDouble(LivingEntity::getArmorValue).thenComparingDouble(this::skycoreHealth).thenComparingDouble(entity4 -> {
               return entity4.distanceTo(player);
            });
         } else if (this.sortMode.is("Distance")) {
            comparator = comparator.thenComparingDouble(entity5 -> entity5.distanceTo(player));
         } else if (this.sortMode.is("Armor")) {
            comparator = comparator.thenComparingDouble(LivingEntity::getArmorValue);
         } else {
            comparator = comparator.thenComparingDouble(this::skycoreHealth);
         }

         return livingEntitys.stream().min(comparator.thenComparingInt(Entity::getId)).orElse(null);
      } else {
         return null;
      }
   }

   private void onPreTick(PlayerTickEndEvent event) {
      if (event != null && event.player() != null) {
         if (!CriticalsModule.INSTANCE.usesSlowFallingMode(event.player())) {
            this.updateBeforePhysics(event);
         }
      }
   }

   private boolean isWallBypassAllowed(LocalPlayer player) {
      if (this.target != null) {
         Boolean get = this.throughWalls.get();
         if (!get) {
            if (player.hasLineOfSight(this.target)) {
               return false;
            }

            if (this.rotationMode.is("ReallyWorld")) {
               Boolean get2 = this.bypassWalls.get();
               if (get2) {
                  return true;
               }
            }

            return false;
         }
      }

      return false;
   }

   private boolean updateAttack() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (!this.isEnabled()
         || player == null
         || instance.level == null
         || instance.gameMode == null
         || this.target == null
         || !this.isValidTarget(this.target)
         || !this.canAttack()
         || this.shouldBlockAttack(player)) {
         return false;
      }

      if (this.shouldApplyRayCheck(player) && !this.passesRayCheck(player)) {
         return false;
      }

      if (Boolean.TRUE.equals(this.releaseShield.get()) && player.isBlocking()) {
         instance.gameMode.releaseUsingItem(player);
      }

      if (Boolean.TRUE.equals(this.breakShield.get()) && this.target instanceof Player player2 && player2.isBlocking()) {
         this.breakShieldPlayer(player, player2);
      }

      instance.gameMode.attack(player, this.target);
      player.swing(InteractionHand.MAIN_HAND);
      this.lastAttackTime = System.currentTimeMillis();
      this.attackCounter++;
      this.skycoreNextAttackTick = this.skycoreTick + 1;
      this.hitImminent = false;
      this.rotations.onAttack();
      return true;
   }

   private boolean shouldApplyRayCheck(LocalPlayer player) {
      if (this.rayTrace.get() && player != null) {
         if (!player.isFallFlying()) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   private void fixMovement(PlayerInputEvent playerInputEvent, float value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         Vec2 vec2 = playerInputEvent.moveVector();
         float y = vec2.y;
         float x = vec2.x;
         if (y != 0.0F || x != 0.0F) {
            double computedValue = Mth.wrapDegrees((float)Math.toDegrees(direction(value, y, x)));
            float yRot = RotationController.isActive() ? RotationController.getServerYaw() : instance.getYRot();
            float value2 = 0.0F;
            float value3 = 0.0F;
            double mAX_VALUE = Double.MAX_VALUE;

            for (float index = -1.0F; index <= 1.0F; index++) {
               for (float index2 = -1.0F; index2 <= 1.0F; index2++) {
                  if (index2 != 0.0F || index != 0.0F) {
                     double computedValue2 = Mth.wrapDegrees((float)Math.toDegrees(direction(yRot, index, index2)));
                     double computedValue3 = Math.abs(computedValue - computedValue2);
                     if (computedValue3 < mAX_VALUE) {
                        mAX_VALUE = computedValue3;
                        value2 = index;
                        value3 = index2;
                     }
                  }
               }
            }

            playerInputEvent.setMoveVector(new Vec2(value3, value2));
            Input input = playerInputEvent.keyPresses();
            playerInputEvent.setKeyPresses(new Input(value2 > 0.0F, value2 < 0.0F, value3 > 0.0F, value3 < 0.0F, input.jump(), input.shift(), input.sprint()));
         }
      }
   }

   public long millisSinceAttack() {
      return System.currentTimeMillis() - this.lastAttackTime;
   }

   private void onPlayerInput(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null && instance.level != null) {
            if (this.target == null || !this.isValidTarget(this.target)) {
               this.target = this.findTarget();
            }

            if (this.target != null) {
               if (!TargetPearlModule.isRotationActive()) {
                  int value = !RotationManager.INSTANCE.isRotating() && (!this.rotationMode.is("Funtime") || !this.movementCorrection.is("Focused")) ? 0 : 1;
                  if (value != 0 && !CrystalAuraModule.INSTANCE.isRotating()) {
                     if (this.movementCorrection.is("Free")) {
                        if (!this.clientLookEnabled()) {
                           float freeYaw = this.isLookMatchedMoveCorrection(player) ? player.getYRot() : FreeLookController.getFreeYaw();
                           this.fixMovement(playerInputEvent, freeYaw);
                        }
                     } else if (this.movementCorrection.is("Focused")) {
                        Vec3 closestVec = CriticalHelper.getClosestVec(this.target);
                        float x = (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(closestVec.z, closestVec.x)) - 90.0);
                        this.fixMovement(playerInputEvent, x);
                     } else if (this.movementCorrection.is("Target")) {
                        if (!RotationManager.INSTANCE.isRotating()) {
                           this.updateRotation(this.canAttack());
                        }

                        if (!ActionStop.isStop()) {
                           float yRot = player.getYRot();
                           this.moveToPosition(playerInputEvent, this.target.position(), yRot);
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public void onGameUpdate() {
      if (this.isEnabled() && Minecraft.getInstance().player != null && this.target != null) {
         this.updateRotation(this.hitImminent);
         this.updateAttack();
      } else {
         this.releaseRotationControl();
      }
   }

   private boolean isWithinAttackRange(LivingEntity entity) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && entity != null) {
         if (ElytraTargetModule.INSTANCE.isEnabled()) {
            if (instance.isFallFlying()) {
               double distance = instance.distanceTo(entity);
               Double value = ElytraTargetModule.INSTANCE.attackRange.get();
               return distance <= value;
            }
         }

         return CriticalHelper.getStrictDistance(entity) < this.attackDistance();
      } else {
         return false;
      }
   }

   private boolean isBypassWallsBlockedByFluid(LocalPlayer player) {
      if (player.isInWater() || player.isInLava() || player.isEyeInFluid(FluidTags.WATER) || player.isEyeInFluid(FluidTags.LAVA)) {
         return true;
      }

      if (this.target == null) {
         return false;
      }

      if (!this.target.isInWater() && !this.target.isInLava() && !this.target.isEyeInFluid(FluidTags.WATER) && !this.target.isEyeInFluid(FluidTags.LAVA)) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level == null) {
            return false;
         }

         Vec3 eyePosition = player.getEyePosition();
         Vec3 eyePosition2 = this.target.getEyePosition();
         Vec3 position = eyePosition2.subtract(eyePosition);
         double length = position.length();
         if (length < 0.001) {
            return false;
         }

         Vec3 position2 = position.normalize();

         for (double index = 0.0; index < length; index += 0.5) {
            BlockPos blockPos = BlockPos.containing(eyePosition.add(position2.scale(index)));
            FluidState fluidState = instance.level.getFluidState(blockPos);
            if (fluidState.is(FluidTags.WATER) || fluidState.is(FluidTags.LAVA)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private boolean isLookMatchedMoveCorrection(LocalPlayer player) {
      if (!this.rotationMode.is("Holy World")) {
         if (!this.rotationMode.is("HolyWorldLegit")) {
            if (!player.isFallFlying()) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean packetCriticalsEnabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(CriticalsModule.class);
      return optional.map(ClientModule::isEnabled).orElse(false);
   }

   public boolean rwWallBypassEnabled() {
      return this.bypassWalls.get();
   }

   public boolean isHitImminent() {
      return this.isEnabled() && this.hitImminent;
   }

   @Override
   protected void onEnable() {
      this.lastAttackTime = 0L;
      this.canCrit = true;
      this.lastRotateTick = -1;
      this.rotations.reset();
   }

   private double getEntityHealth(LivingEntity entity) {
      float labelHealth = ScoreboardModeHelper.getLabelHealth(entity);
      if (entity instanceof Player player) {
         double entityArmor = this.getEntityArmor(player) / 20.0;
         return labelHealth * entityArmor;
      } else {
         return labelHealth;
      }
   }

   public boolean clientLookEnabled() {
      if (!this.rotationMode.is("HolyWorldLegit")) {
         Boolean get = this.clientLook.get();
         if (!get) {
            return false;
         }
      }

      return true;
   }

   public boolean canAttack() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         return false;
      } else if (ElytraTargetModule.INSTANCE.isEnabled() && player.isFallFlying() && this.target != null) {
         return this.isWithinAttackRange(this.target) && player.getAttackStrengthScale(1.5F) > 0.92F && this.skycoreTick >= this.skycoreNextAttackTick;
      } else {
         float value = this.tpsSync.get() ? 1.0F : 1.5F;
         int attackStrengthScale = this.millisSinceAttack() >= 473L && player.getAttackStrengthScale(value) > this.getAICooldown() ? 1 : 0;
         int value2 = !instance.options.keyJump.isDown() && player.onGround() ? 0 : 1;
         if (CriticalHelper.isJumpBlockedByCeiling()) {
            return (attackStrengthScale != 0);
         } else if (AirStuckModule.INSTANCE.isFrozen()) {
            return (attackStrengthScale != 0);
         } else {
            return this.packetCriticalsEnabled()
               ? attackStrengthScale != 0
                  && (
                     !this.onlyCriticals.get()
                        || !this.isValidAttackCondition()
                        || this.smartCriticals.get() && value2 == 0
                        || this.canPlayerCritical()
                        || CriticalsModule.INSTANCE.allowsAuraCritical(player)
                  )
               : attackStrengthScale != 0
                  && (
                     !this.onlyCriticals.get()
                        || !this.isValidAttackCondition()
                        || this.smartCriticals.get() && value2 == 0
                        || this.canPlayerCritical() && this.canCrit
                  );
         }
      }
   }

   private void bypassBlocks(LocalPlayer player, LivingEntity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && player.connection != null) {
         Vec3 eyePosition = player.getEyePosition();
         Vec3 eyePosition2 = entity.getEyePosition();
         Vec3 position = eyePosition2.subtract(eyePosition);
         double length = position.length();
         if (!(length < 0.001)) {
            Vec3 position2 = position.normalize();

            for (double index = 0.0; index < length; index += 0.5) {
               BlockPos blockPos = BlockPos.containing(eyePosition.add(position2.scale(index)));
               if (!instance.level.getBlockState(blockPos).isAir()) {
                  player.connection.send(new ServerboundPlayerActionPacket(Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
                  player.connection.send(new ServerboundPlayerActionPacket(Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
               }
            }
         }
      }
   }

   private boolean isManualBlockPlacement(LocalPlayer player, Minecraft minecraft) {
      if (minecraft.gui.screen() == null) {
         if (minecraft.options != null) {
            if (minecraft.options.keyUse.isDown()) {
               if (player.isFallFlying()) {
                  return false;
               }

               if (!(player.getMainHandItem().getItem() instanceof BlockItem)) {
                  return false;
               }

               if (minecraft.hitResult != null) {
                  Type type = minecraft.hitResult.getType();
                  if (type == Type.BLOCK) {
                     return true;
                  }
               }

               return false;
            }
         }
      }

      return false;
   }

   private void resetState() {
      this.target = null;
      this.hitImminent = false;
      this.canCrit = false;
      this.lastRotateTick = -1;
      this.releaseRotationControl();
   }
}
