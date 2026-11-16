package net.skycore.client.module.combat;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.InventoryHelper;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.setting.BooleanSetting;

public final class AutoTotemModule extends ClientModule {
   private final NumberSetting fallDistance;
   private static final double ANCHOR_RANGE = 6.0;
   private int previousSlot;
   private final NumberSetting health = this.add(new NumberSetting("Health", "", 4.0, 0.0, 20.0, 0.5, " hp"));
   private Slot pendingSlot;
   private final BooleanSetting ignoreDragonidTotem;
   private int ballUseGraceTicks;
   private boolean pendingRestore;
   private final NumberSetting elytraHealth;
   private final MultiSelectSetting mode = this.add(
      new MultiSelectSetting("Modes", List.of("Elytra", "Fall", "Crystals", "Anchor"), Set.of("Elytra", "Fall", "Crystals", "Anchor"))
   );
   private ItemStack lastBallStack;
   private int popRefillTicks;
   private long actionGeneration;
   private ItemStack previousStack;
   private int cooldownTicks;
   private static final int SPRINT_STOP_WAIT = 1;
   private final NumberSetting healthWithBall;

   private void flushPendingSwap() {
      if (this.isEnabled() && this.pendingSlot != null) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null && Minecraft.getInstance().level != null) {
            try {
               boolean pendingRestore2 = this.pendingRestore;
               this.pendingSlot = null;
               this.pendingRestore = false;
               if (!InventoryHelper.isServerScreen()) {
                  if (pendingRestore2 && this.popRefillTicks == 0 && !this.shouldUseTotem()) {
                     Slot previousSlot2 = this.findPreviousSlot();
                     if (previousSlot2 != null && this.swapToOffhand(previousSlot2)) {
                        this.cooldownTicks = 3;
                     }
                  } else if (!pendingRestore2 && !this.isUsableTotem(instance.getOffhandItem())) {
                     Slot totemSlot = this.findTotemSlot();
                     if (totemSlot != null && this.swapToOffhand(totemSlot)) {
                        this.cooldownTicks = 3;
                        this.popRefillTicks = 0;
                     }
                  }
               }
            } finally {
               this.actionGeneration++;
            }
         }
      }
   }

   private void clearPreviousStack() {
      this.previousStack = null;
      this.previousSlot = -1;
   }

   private boolean shouldUseTotem() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null && !player.isCreative() && !player.isSpectator()) {
         if (player.getHealth() <= this.health.get().floatValue()) {
            return true;
         } else if (this.mode.isSelected("Fall") && player.fallDistance >= this.fallDistance.get().floatValue() && !player.isFallFlying()) {
            return true;
         } else if (this.mode.isSelected("Elytra") && this.isWearingElytra() && player.getHealth() <= this.elytraHealth.get().floatValue()) {
            return true;
         } else {
            return this.mode.isSelected("Crystals") && this.hasNearbyCrystalThreat() ? true : this.mode.isSelected("Anchor") && this.hasNearbyChargedAnchor();
         }
      } else {
         return false;
      }
   }

   private void queueSwap(Slot pendingSlot2, boolean pendingRestore2) {
      if (Minecraft.getInstance().player != null && pendingSlot2 != null) {
         this.pendingSlot = pendingSlot2;
         this.pendingRestore = pendingRestore2;
         long timestamp = this.actionGeneration + 1L;
         this.actionGeneration = timestamp;
         long timestamp2 = timestamp;
         Runnable runnable = () -> {
            if (timestamp2 == this.actionGeneration) {
               this.flushPendingSwap();
            }
         };
         ScheduleHelper.schedule(runnable);
      }
   }

   public void onEntityEvent(ClientboundEntityEventPacket clientboundEntityEventPacket) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled()) {
         if (instance.player != null) {
            if (instance.level != null) {
               if (clientboundEntityEventPacket.getEventId() == 35) {
                  Entity entity = clientboundEntityEventPacket.getEntity(instance.level);
                  if (entity == instance.player) {
                     this.cooldownTicks = 0;
                     this.popRefillTicks = 12;
                     this.pendingSlot = null;
                     this.pendingRestore = false;
                     this.actionGeneration++;
                  }

                  return;
               }
            }
         }
      }
   }

   private boolean hasNearbyChargedAnchor() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         BlockPos blockPos = player.blockPosition();
         int roundedUp = (int)Math.ceil(6.0);
         double distance = 36.0;
         Vec3 eyePosition = player.getEyePosition();

         for (int index = -roundedUp; index <= roundedUp; index++) {
            for (int index2 = -roundedUp; index2 <= roundedUp; index2++) {
               for (int index3 = -roundedUp; index3 <= roundedUp; index3++) {
                  BlockPos blockPos2 = blockPos.offset(index, index2, index3);
                  BlockState blockState = instance.level.getBlockState(blockPos2);
                  if (blockState.getBlock() == Blocks.RESPAWN_ANCHOR
                     && blockState.getValue(RespawnAnchorBlock.CHARGE) > 0
                     && eyePosition.distanceToSqr(Vec3.atCenterOf(blockPos2)) <= distance) {
                     return true;
                  }
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   @Override
   protected void onDisable() {
      this.pendingSlot = null;
      this.pendingRestore = false;
      this.actionGeneration++;
      this.popRefillTicks = 0;
      this.cooldownTicks = 0;
      this.lastBallStack = ItemStack.EMPTY;
      this.ballUseGraceTicks = 0;
      this.clearPreviousStack();
   }

   private boolean isUsableTotem(ItemStack itemStack) {
      if (itemStack != null) {
         if (!itemStack.isEmpty()) {
            if (itemStack.getItem() == Items.TOTEM_OF_UNDYING && !this.isDragonidTotem(itemStack)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean isBusy() {
      if (this.isEnabled()) {
         if (this.cooldownTicks > 0 || this.needsTotem() || this.pendingSlot != null) {
            return true;
         }
      }

      return false;
   }

   private void updateBallUseGrace(LocalPlayer player) {
      ItemStack offhandItem = player.getOffhandItem();
      if (!this.isEnchantedBall(offhandItem)) {
         if (this.ballUseGraceTicks > 0) {
            this.ballUseGraceTicks--;
         } else {
            if (!offhandItem.isEmpty()) {
               this.lastBallStack = ItemStack.EMPTY;
            }
         }
      } else {
         ItemStack itemStack = offhandItem.copy();
         this.lastBallStack = itemStack;
         Minecraft instance = Minecraft.getInstance();
         if (instance.options.keyUse.isDown() || player.isUsingItem()) {
            this.ballUseGraceTicks = 4;
         }
      }
   }

   public boolean needsTotem() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (this.isEnabled() && player != null) {
         if (this.shouldUseTotem()) {
            return !this.isUsableTotem(player.getOffhandItem());
         }
      }

      return false;
   }

   private Slot findPreviousSlot() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && this.previousStack != null && !this.previousStack.isEmpty()) {
         if (this.previousSlot >= 0 && this.previousSlot < instance.containerMenu.slots.size()) {
            Slot slot = instance.containerMenu.getSlot(this.previousSlot);
            if (slot.index != 45 && ItemStack.isSameItemSameComponents(slot.getItem(), this.previousStack)) {
               return slot;
            }
         }

         for (int value = 9; value <= 44 && value < instance.containerMenu.slots.size(); value++) {
            Slot slot2 = instance.containerMenu.getSlot(value);
            if (ItemStack.isSameItemSameComponents(slot2.getItem(), this.previousStack)) {
               return slot2;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private boolean hasNearbyCrystalThreat() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         for (Entity entity : instance.level.entitiesForRendering()) {
            if (entity instanceof EndCrystal endCrystal
               && endCrystal.isAlive()
               && !(player.getEyePosition().distanceToSqr(endCrystal.getBoundingBox().getCenter()) > 36.0)
               && (!this.hasProtectedBallInHand() || !(player.getHealth() > this.healthWithBall.get().floatValue()))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean swapToOffhand(Slot slot) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && slot != null && !InventoryHelper.isServerScreen() && slot.index >= 0) {
         if (slot.index < player.containerMenu.slots.size() && slot.index != 45) {
            if (player.containerMenu.getSlot(slot.index) == slot) {
               InventoryHelper.swapHand(slot, InteractionHand.OFF_HAND, false);
               return true;
            }
         }
      }

      return false;
   }

   public void cancelPendingAction() {
      this.actionGeneration++;
      this.pendingSlot = null;
      this.pendingRestore = false;
   }

   private boolean isDragonidTotem(ItemStack itemStack) {
      if (this.ignoreDragonidTotem.get() && itemStack != null) {
         if (!itemStack.isEmpty()) {
            Item item = itemStack.getItem();
            if (item == Items.TOTEM_OF_UNDYING) {
               return itemStack.getHoverName().getString().contains("Тотем Драконида");
            }
         }
      }

      return false;
   }

   public AutoTotemModule() {
      super("Auto Totem", "Automatically equips a totem in dangerous situations", ModuleCategory.COMBAT);
      this.elytraHealth = this.add(new NumberSetting("Elytra Health", "", 10.0, 0.0, 20.0, 0.5, " hp"));
      this.fallDistance = this.add(new NumberSetting("Fall Distance", "", 20.0, 10.0, 50.0, 0.1, " blocks"));
      this.healthWithBall = this.add(new NumberSetting("Health with Ball", "", 10.0, 0.0, 20.0, 0.5, " hp"));
      this.ignoreDragonidTotem = this.add(new BooleanSetting("Ignore Dragonid Totem", true));
      this.previousSlot = -1;
      this.lastBallStack = ItemStack.EMPTY;
      this.elytraHealth.visibleWhen(() -> {
         return this.mode.isSelected("Elytra");
      });
      this.fallDistance.visibleWhen(() -> {
         return this.mode.isSelected("Fall");
      });
      this.healthWithBall.visibleWhen(() -> {
         return this.mode.isSelected("Crystals");
      });
      SkycoreClient.events().subscribe(PacketSendEvent.class, this::onPacketSend);
   }

   private boolean isEnchantedBallInOffhand() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         return false;
      }

      ItemStack offhandItem = player.getOffhandItem();
      return this.isEnchantedBall(offhandItem);
   }

   private boolean isEnchantedBall(ItemStack itemStack) {
      if (itemStack != null) {
         if (!itemStack.isEmpty()) {
            Item item = itemStack.getItem();
            if (item == Items.PLAYER_HEAD) {
               if (itemStack.isEnchanted()) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
      if (this.isEnabled()) {
         InteractionHand interactionHand = null;
         if (packetSendEvent.getPacket() instanceof ServerboundUseItemOnPacket serverboundUseItemOnPacket) {
            interactionHand = serverboundUseItemOnPacket.getHand();
         } else if (packetSendEvent.getPacket() instanceof ServerboundUseItemPacket serverboundUseItemPacket) {
            interactionHand = serverboundUseItemPacket.getHand();
         }

         LocalPlayer instance = Minecraft.getInstance().player;
         if (interactionHand == InteractionHand.OFF_HAND && instance != null && this.isEnchantedBall(instance.getOffhandItem())) {
            this.lastBallStack = instance.getOffhandItem().copy();
            this.ballUseGraceTicks = 6;
         }
      }
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null) {
         if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
         }

         if (this.popRefillTicks > 0) {
            this.popRefillTicks--;
         }

         this.updateBallUseGrace(player);
         if (this.pendingSlot == null) {
            ItemStack offhandItem = player.getOffhandItem();
            ItemStack itemStack = offhandItem.isEmpty() ? null : offhandItem;
            if (itemStack != null && this.previousStack != null && ItemStack.isSameItemSameComponents(itemStack, this.previousStack)) {
               this.clearPreviousStack();
            }

            if (this.popRefillTicks <= 0 && !this.shouldUseTotem()) {
               if (this.previousStack != null && this.cooldownTicks == 0 && !InventoryHelper.isServerScreen()) {
                  Slot previousSlot2 = this.findPreviousSlot();
                  if (previousSlot2 != null) {
                     this.queueSwap(previousSlot2, true);
                  } else {
                     this.clearPreviousStack();
                  }
               }
            } else if (!this.isUsableTotem(itemStack)) {
               Slot totemSlot = this.findTotemSlot();
               if (totemSlot != null && !InventoryHelper.isServerScreen()) {
                  if (this.previousStack == null && itemStack != null) {
                     this.previousStack = itemStack.copy();
                     this.previousSlot = totemSlot.index;
                  }

                  this.queueSwap(totemSlot, false);
               }
            }
         }
      } else {
         this.pendingSlot = null;
         this.pendingRestore = false;
         this.actionGeneration++;
         this.popRefillTicks = 0;
         this.lastBallStack = ItemStack.EMPTY;
         this.ballUseGraceTicks = 0;
         this.clearPreviousStack();
      }
   }

   private boolean hasProtectedBallInHand() {
      if (!this.isEnchantedBallInOffhand()) {
         if (this.ballUseGraceTicks <= 0 || !this.isEnchantedBall(this.lastBallStack)) {
            return false;
         }
      }

      return true;
   }

   private boolean isWearingElytra() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.CHEST);
         if (itemBySlot.getItem() == Items.ELYTRA) {
            return true;
         }
      }

      return false;
   }

   private Slot findTotemSlot() {
      return InventoryHelper.getSlot(Items.TOTEM_OF_UNDYING, slot -> {
         if (slot.index >= 9) {
            if (slot.index <= 44) {
               ItemStack item = slot.getItem();
               if (this.isUsableTotem(item)) {
                  return true;
               }
            }
         }

         return false;
      });
   }
}
