package net.skycore.client.module.combat;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.InputPredictor;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class TriggerBotModule extends ClientModule {
   private final BooleanSetting breakShield;
   private long lastAttackTime;
   private final BooleanSetting tpsSync;
   private final BooleanSetting smartCrits;
   private static final long ATTACK_DELAY_MS = 450L;
   private static final double ATTACK_RANGE = 3.0;
   private final MultiSelectSetting targets = this.add(
      new MultiSelectSetting("Targets", List.of("Players", "Friends", "Naked", "Animals", "Mobs"), Set.of("Players", "Naked"))
   );

   private boolean canPlayerCritical(LocalPlayer player) {
      if (!player.onGround()) {
         if (!(player.getDeltaMovement().y >= 0.0) && !(player.fallDistance <= 0.0)) {
            if (this.isValidAttackCondition(player)) {
               if (!(player.fallDistance <= 0.1F)) {
                  InputPredictor inputPredictor = InputPredictor.simulateLocalPlayer(1);
                  if (inputPredictor.onGround()) {
                     return false;
                  }
               }

               return true;
            }
         }
      }

      return false;
   }

   private void breakShieldPlayer(Minecraft minecraft, LocalPlayer player, Player player2) {
      int slot = -1;

      for (int value = 0; value < 36; value++) {
         if (player.getInventory().getItem(value).is(ItemTags.AXES)) {
            slot = value;
            break;
         }
      }

      int selectedSlot = player.getInventory().getSelectedSlot();
      if (slot >= 0 && slot != selectedSlot) {
         if (slot < 9) {
            player.getInventory().setSelectedSlot(slot);

            try {
               minecraft.gameMode.attack(player, player2);
               player.swing(InteractionHand.MAIN_HAND);
            } finally {
               player.getInventory().setSelectedSlot(selectedSlot);
            }
         } else {
            minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, selectedSlot, ContainerInput.SWAP, player);

            try {
               minecraft.gameMode.attack(player, player2);
               player.swing(InteractionHand.MAIN_HAND);
            } finally {
               minecraft.gameMode.handleContainerInput(player.inventoryMenu.containerId, slot, selectedSlot, ContainerInput.SWAP, player);
            }
         }
      }
   }

   private void onPreTick(PlayerTickEndEvent event) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = event.player();
         if (player != null
            && instance.level != null
            && instance.gameMode != null
            && instance.gui.screen() == null
            && player.isAlive()
            && !player.isSpectator()
            && instance.hitResult instanceof EntityHitResult entityHitResult
            && entityHitResult.getEntity() instanceof LivingEntity entity
            && this.isValidTarget(player, entity)
            && !(entityHitResult.getLocation().distanceToSqr(player.getEyePosition()) > 9.0)
            && this.canAttack(player, instance)) {
            if (this.breakShield.get() && entity instanceof Player player2 && player2.isBlocking()) {
               this.breakShieldPlayer(instance, player, player2);
            }

            if (player.isUsingItem() && (player.getUseItem().is(Items.SHIELD) || player.isBlocking())) {
               instance.gameMode.releaseUsingItem(player);
            }

            instance.gameMode.attack(player, entity);
            player.swing(InteractionHand.MAIN_HAND);
            this.lastAttackTime = System.currentTimeMillis();
         }
      }
   }

   public TriggerBotModule() {
      super("TriggerBot", "Attacks only the target under your crosshair", ModuleCategory.COMBAT);
      this.breakShield = this.add(new BooleanSetting("Break Shield", true));
      this.tpsSync = this.add(new BooleanSetting("TPS Sync", false));
      this.smartCrits = this.add(new BooleanSetting("Smart crits", false));
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   private boolean isValidTarget(LocalPlayer player, LivingEntity entity) {
      if (entity == player || entity.tickCount < 3 || !entity.isAlive() || entity.isInvulnerable() || entity instanceof ArmorStand) {
         return false;
      } else if (entity instanceof Player player2) {
         return !AntiBotModule.isBot(player2)
               && !player2.getGameProfile().name().equalsIgnoreCase(player.getGameProfile().name())
               && (this.targets.isSelected("Friends") || !FriendManager.INSTANCE.isFriend(player2.getGameProfile().name()))
               && (player2.getArmorValue() != 0 || this.targets.isSelected("Naked"))
            ? this.targets.isSelected("Players") || this.targets.isSelected("Naked") || this.targets.isSelected("Friends")
            : false;
      } else {
         return !(entity instanceof Animal) && !(entity instanceof AmbientCreature) ? this.targets.isSelected("Mobs") : this.targets.isSelected("Animals");
      }
   }

   private float attackCooldown(LocalPlayer player) {
      ItemStack mainHandItem = player.getMainHandItem();
      if (!mainHandItem.isEmpty()) {
         if (!mainHandItem.is(Items.AIR)) {
            return !mainHandItem.is(ItemTags.AXES) && !mainHandItem.is(Items.WOODEN_SHOVEL) ? 0.93F : 0.95F;
         }
      }

      return 0.9F;
   }

   private boolean isValidAttackCondition(LocalPlayer player) {
      if (!player.hasEffect(MobEffects.LEVITATION)) {
         if (!player.hasEffect(MobEffects.BLINDNESS)) {
            if (!player.isInWater() && !player.isInLava()) {
               if (!player.isEyeInFluid(FluidTags.WATER)) {
                  if (!player.isEyeInFluid(FluidTags.LAVA)
                     && !player.getInBlockState().is(Blocks.COBWEB)
                     && !player.getAbilities().flying
                     && !player.isFallFlying()
                     && !player.onClimbable()) {
                     if (!player.isPassenger()) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   private boolean canAttack(LocalPlayer player, Minecraft minecraft) {
      float value = this.tpsSync.get() ? 1.0F : 1.5F;
      if (System.currentTimeMillis() - this.lastAttackTime < 450L || player.getAttackStrengthScale(value) <= this.attackCooldown(player)) {
         return false;
      } else if (this.smartCrits.get() && !CriticalHelper.isJumpBlockedByCeiling() && this.isValidAttackCondition(player)) {
         int value2 = !minecraft.options.keyJump.isDown() && player.onGround() ? 0 : 1;
         return value2 == 0 || this.canPlayerCritical(player);
      } else {
         return true;
      }
   }

   @Override
   protected void onEnable() {
      this.lastAttackTime = 0L;
   }
}
