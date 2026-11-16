package net.skycore.client.module.combat;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.AttackHelper;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.LookHelper;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.mixin.LivingEntityGetNoJumpDelayAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.UseItemHelper;

public final class AutoMaceModule extends ClientModule {
   private LivingEntity target;
   private int jumpWaitTicks;
   private final NumberSetting triggerRange = this.add(new NumberSetting("Дистанция активации", 4.0, 2.0, 6.0, 0.1));
   private static final long SEQUENCE_TIMEOUT_MS = 6000L;
   private static final int ROTATION_PRIORITY = 8;
   private final NumberSetting targetRange;
   private boolean releaseJumpKey;
   private final NumberSetting attackRange = this.add(new NumberSetting("Радиус удара", 3.5, 2.5, 6.0, 0.1));
   private AutoMaceModule.State state;
   private long sequenceStartedAt;
   private static final float MIN_SMASH_FALL = 1.5F;
   private final NumberSetting jumpDelay;
   private int rotateTicks;

   private void handleJump() {
      Minecraft instance = Minecraft.getInstance();
      instance.options.keyJump.setDown(true);
      this.releaseJumpKey = true;
      if (instance.player.onGround()) {
         ((LivingEntityGetNoJumpDelayAccessor)instance.player).skycore$setNoJumpDelay(0);
         instance.player.jumpFromGround();
      }

      this.state = AutoMaceModule.State.SWAP_MACE;
   }

   private int findHotbarSlot(Item item) {
      LocalPlayer instance = Minecraft.getInstance().player;

      for (int value = 0; value < 9; value++) {
         ItemStack item2 = instance.getInventory().getItem(value);
         if (item2.is(item)) {
            return value;
         }
      }

      return -1;
   }

   private void handleWait() {
      if (this.hasRequiredItems()) {
         LivingEntity target2 = this.findTarget();
         this.target = target2;
         if (this.target != null) {
            if (CriticalHelper.getStrictDistance(this.target) <= this.triggerRange.get()) {
               this.sequenceStartedAt = System.currentTimeMillis();
               this.rotateTicks = 0;
               this.jumpWaitTicks = 0;
               this.state = AutoMaceModule.State.SWAP_WIND;
            }
         }
      }
   }

   private void handleAttack() {
      Minecraft instance = Minecraft.getInstance();
      Vec3 eyePosition = instance.player.getEyePosition();
      Vec3 closestVec = CriticalHelper.getClosestVec(eyePosition, this.target);
      RotationManager.update(LookHelper.calculateAngle(closestVec), 180.0F, 180.0F, 1, 8);
      if (this.canSmashNow()) {
         if (instance.player.isBlocking()) {
            instance.gameMode.releaseUsingItem(instance.player);
         }

         AttackHelper.sendFixedAttack(instance.player, this.target);
         this.toggle();
      }
   }

   public static void onAttackFinished() {
   }

   @Override
   protected void onEnable() {
      this.resetSequence();
      this.state = AutoMaceModule.State.WAIT;
   }

   private void handleRotate() {
      int intermediate = -1;
      intermediate = (byte)1;
      LocalPlayer instance = Minecraft.getInstance().player;
      intermediate = (byte)1;
      float throwYaw = this.getThrowYaw();
      Rotation rotation = new Rotation(throwYaw, 90.0F);
      RotationManager.update(rotation, 360.0F, 360.0F, 1, 8);
      intermediate = this.rotateTicks + 1;
      this.rotateTicks = intermediate;
      if (instance.getXRot() >= 80.0F && this.rotateTicks >= 2 || this.rotateTicks >= 6) {
         this.state = AutoMaceModule.State.THROW;
      }
   }

   private void handleSwapWind() {
      if (!this.switchToItem(Items.WIND_CHARGE)) {
         this.state = AutoMaceModule.State.WAIT;
      } else {
         this.state = AutoMaceModule.State.ROTATE;
         this.rotateTicks = 0;
      }
   }

   private void onMoveInput(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled() && this.shouldStopMovement()) {
         playerInputEvent.clearMovement(false, false);
         playerInputEvent.setSprint(false);
      }
   }

   private void handleJumpWait() {
      int intermediate = -1;
      int value = 0;
      intermediate = this.jumpWaitTicks + 1;
      this.jumpWaitTicks = intermediate;
      value = this.jumpWaitTicks;
      Double value2 = this.jumpDelay.get();
      if (value > value2.intValue()) {
         this.state = AutoMaceModule.State.JUMP;
      }
   }

   private void handleThrow() {
      Minecraft instance = Minecraft.getInstance();
      ItemStack mainHandItem = instance.player.getMainHandItem();
      if (mainHandItem.is(Items.WIND_CHARGE)) {
         ItemCooldowns cooldowns = instance.player.getCooldowns();
         if (!cooldowns.isOnCooldown(Items.WIND_CHARGE.getDefaultInstance())) {
            instance.gameMode.useItem(instance.player, InteractionHand.MAIN_HAND);
            this.state = AutoMaceModule.State.JUMP_WAIT;
            this.jumpWaitTicks = 0;
            return;
         }
      }

      this.state = AutoMaceModule.State.WAIT;
   }

   private void releaseJump() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.options != null) {
         instance.options.keyJump.setDown(false);
      }

      this.releaseJumpKey = false;
   }

   public AutoMaceModule() {
      super("Auto Mace", "Automatic wind-charge mace smash", ModuleCategory.COMBAT);
      this.targetRange = this.add(new NumberSetting("Радиус поиска", 12.0, 4.0, 16.0, 0.5));
      this.jumpDelay = this.add(new NumberSetting("Задержка прыжка", 1.0, 0.0, 3.0, 1.0));
      this.state = AutoMaceModule.State.WAIT;
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onMoveInput);
   }

   public static AutoMaceModule get() {
      var optional = SkycoreClient.modules().get(AutoMaceModule.class);
      return (AutoMaceModule)optional.orElse(null);
   }

   private boolean isValidTarget(LivingEntity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (entity != null) {
         if (entity != instance.player) {
            if (entity.isAlive()) {
               if (entity instanceof Player player) {
                  if (AntiBotModule.isBot(player)) {
                     return false;
                  }

                  FriendManager iNSTANCE = FriendManager.INSTANCE;
                  GameProfile gameProfile = player.getGameProfile();
                  return !iNSTANCE.isFriend(gameProfile.name());
               }

               return true;
            }
         }
      }

      return false;
   }

   private boolean shouldStopMovement() {
      if (this.state != AutoMaceModule.State.ROTATE) {
         if (this.state != AutoMaceModule.State.THROW) {
            if (this.state != AutoMaceModule.State.JUMP_WAIT) {
               if (this.state != AutoMaceModule.State.JUMP) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   public static void prepareForHit(LivingEntity entity) {
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (this.releaseJumpKey) {
         this.releaseJump();
      }

      if (instance.player != null && instance.level != null && instance.gameMode != null) {
         switch (this.state) {
            case WAIT:
               this.handleWait();
               break;
            case SWAP_WIND:
               this.handleSwapWind();
               break;
            case ROTATE:
               this.handleRotate();
               break;
            case THROW:
               this.handleThrow();
               break;
            case JUMP_WAIT:
               this.handleJumpWait();
               break;
            case JUMP:
               this.handleJump();
               break;
            case SWAP_MACE:
               this.handleSwapMace();
               break;
            case ATTACK:
               if (System.currentTimeMillis() - this.sequenceStartedAt >= 6000L) {
                  this.state = AutoMaceModule.State.WAIT;
               } else if (this.isValidTarget(this.target)) {
                  this.handleAttack();
               } else {
                  this.state = AutoMaceModule.State.WAIT;
               }
         }
      }
   }

   private boolean canSmashNow() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (!instance.onGround()) {
         if (instance.getDeltaMovement().y < 0.0 && instance.fallDistance >= 1.5) {
            double strictDistance = CriticalHelper.getStrictDistance(this.target);
            if (strictDistance <= this.attackRange.get()) {
               return true;
            }
         }
      }

      return false;
   }

   public static void onAttackEntity(Player player, Entity entity) {
   }

   private boolean switchToItem(Item item) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance.getMainHandItem().is(item)) {
         return true;
      }

      int hotbarSlot = this.findHotbarSlot(item);
      if (hotbarSlot >= 0) {
         UseItemHelper.switchToHotbarSlot(hotbarSlot);
         return instance.getMainHandItem().is(item);
      }

      int slot = UseItemHelper.getSlot(item);
      if (slot < 0) {
         return false;
      }

      int selectedSlot = instance.getInventory().getSelectedSlot();
      UseItemHelper.clickSlotId(UseItemHelper.toMenuSlot(slot), selectedSlot, ContainerInput.SWAP, false);
      UseItemHelper.syncCurrentPlayItem();
      return instance.getMainHandItem().is(item);
   }

   @Override
   protected void onDisable() {
      this.releaseJump();
      RotationManager.INSTANCE.stopRotation();
      this.resetSequence();
      this.state = AutoMaceModule.State.WAIT;
   }

   private void handleSwapMace() {
      AutoMaceModule.State state2;
      if (this.switchToItem(Items.MACE)) {
         state2 = AutoMaceModule.State.ATTACK;
      } else {
         state2 = AutoMaceModule.State.WAIT;
      }

      this.state = state2;
   }

   private void resetSequence() {
      this.target = null;
      this.rotateTicks = 0;
      this.jumpWaitTicks = 0;
      this.releaseJumpKey = false;
      this.sequenceStartedAt = 0L;
   }

   private float getThrowYaw() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (this.target == null) {
         return player.getYRot();
      } else {
         Vec3 closestVec = CriticalHelper.getClosestVec(player.getEyePosition(), this.target);
         return LookHelper.calculateAngle(closestVec).getYaw();
      }
   }

   private boolean hasRequiredItems() {
      if (UseItemHelper.getSlot(Items.WIND_CHARGE) >= 0) {
         if (UseItemHelper.getSlot(Items.MACE) >= 0) {
            return true;
         }
      }

      return false;
   }

   private LivingEntity findTarget() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.hitResult instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity entity && this.isValidTarget(entity)) {
         return entity;
      } else {
         AttackAuraModule attackAuraModule = SkycoreClient.modules().get(AttackAuraModule.class).orElse(null);
         if (attackAuraModule != null && attackAuraModule.isEnabled() && this.isValidTarget(attackAuraModule.currentTarget())) {
            return attackAuraModule.currentTarget();
         }

         Player player = null;
         double mAX_VALUE = Double.MAX_VALUE;

         for (Player player2 : instance.level.players()) {
            if (this.isValidTarget(player2)) {
               double strictDistance = CriticalHelper.getStrictDistance(player2);
               if (strictDistance <= this.targetRange.get() && strictDistance < mAX_VALUE) {
                  player = player2;
                  mAX_VALUE = strictDistance;
               }
            }
         }

         return player;
      }
   }

   enum State {
      WAIT,
      SWAP_WIND,
      ROTATE,
      THROW,
      JUMP_WAIT,
      JUMP,
      SWAP_MACE,
      ATTACK;
   }
}
