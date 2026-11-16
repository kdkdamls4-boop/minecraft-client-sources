package net.skycore.client.module.combat;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.AttackHelper;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.event.BlockPlaceEvent;
import net.skycore.client.event.CancellableEvent;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;

public final class AutoExplosionModule extends ClientModule implements MinecraftAccess {
   private BlockPos pkmPlacePos;
   private final BooleanSetting fast;
   private BlockPos targetPos;
   private final BooleanSetting noExplodeResources;
   private int pkmCrystalSlot;
   private boolean pkmBlocked;
   private int pendingCrystalId = -1;
   private int explodeTicks;
   private Vec3 pendingCrystalPos;
   private boolean needSync;
   public static final AutoExplosionModule INSTANCE = new AutoExplosionModule();
   private final BooleanSetting test;
   private boolean blocked;
   private AABB crystalArea;
   private int oldSlot;
   private int pendingCrystalTicks;
   private int targetSlot;

   private boolean isHoldingCrystal() {
      return mc.player.getMainHandItem().is(Items.END_CRYSTAL) || mc.player.getOffhandItem().is(Items.END_CRYSTAL);
   }

   private void handleObsidianPlace() {
      if (this.needSync) {
         this.needSync = false;
         UseItemHelper.switchToHotbarSlot(this.oldSlot);
      } else if (this.targetPos != null) {
         if (mc.level.getBlockState(this.targetPos).isAir()) {
            this.targetPos = null;
         } else if (this.blocked) {
            this.blocked = false;
         } else {
            this.placeCrystalOnObsidian(this.targetPos, this.targetSlot, InteractionHand.MAIN_HAND);
            this.needSync = true;
            this.targetPos = null;
         }
      }
   }

   private void attackCrystal(Entity entity) {
      if (entity != null) {
         if (mc.player != null) {
            AttackHelper.sendFixedAttack(mc.player, entity);
            if (entity instanceof EndCrystal) {
               entity.discard();
            }

            return;
         }
      }
   }

   public void onCrystalSpawnApplied(ClientboundAddEntityPacket clientboundAddEntityPacket) {
      int value = 0;
      value = (byte)0;
      if (this.isEnabled() && clientboundAddEntityPacket != null && clientboundAddEntityPacket.getType() == EntityTypes.END_CRYSTAL) {
         value = clientboundAddEntityPacket.getId();
         if (value == this.pendingCrystalId) {
            if (mc.level != null) {
               this.attackCrystalInArea();
               return;
            }
         }
      }
   }

   private Vec3 getCrystalAimPoint(BlockPos blockPos) {
      return this.getCrystalAimPoint(new AABB(blockPos).expandTowards(0.0, 1.0, 0.0));
   }

   @Override
   protected void onDisable() {
      this.clearExplosionState();
      this.targetPos = null;
      this.pkmPlacePos = null;
      this.pkmCrystalSlot = -1;
      this.pkmBlocked = false;
      super.onDisable();
   }

   private void tryAttackPendingCrystal() {
      if (this.pendingCrystalTicks > 0 && mc.level != null) {
         this.pendingCrystalTicks--;
         if ((this.pendingCrystalId == -1 ? null : mc.level.getEntity(this.pendingCrystalId)) instanceof EndCrystal endCrystal
            && !this.shouldNotExplodeBecauseOfResources(endCrystal)) {
            this.rotateToCrystal(endCrystal);
            this.attackCrystal(endCrystal);
            this.clearExplosionState();
         } else {
            if (this.pendingCrystalTicks <= 0) {
               this.clearExplosionState();
            }
         }
      }
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      if (this.isEnabled()) {
         if (packetReceiveEvent.getPacket() instanceof ClientboundAddEntityPacket clientboundAddEntityPacket) {
            if (clientboundAddEntityPacket.getType() == EntityTypes.END_CRYSTAL) {
               double x = clientboundAddEntityPacket.getX();
               double y = clientboundAddEntityPacket.getY();
               double z = clientboundAddEntityPacket.getZ();
               if (this.crystalArea != null) {
                  if (this.crystalArea.inflate(0.5).contains(x, y, z)) {
                     if (this.pendingCrystalId == -1) {
                        this.pendingCrystalId = clientboundAddEntityPacket.getId();
                        this.pendingCrystalPos = new Vec3(x, y, z);
                        this.pendingCrystalTicks = this.fast.get() ? 8 : 6;
                        if (this.fast.get() && mc.level != null) {
                           this.tryAttackPendingCrystal();
                           if (mc.level.getEntity(clientboundAddEntityPacket.getId()) instanceof EndCrystal endCrystal && !this.shouldNotExplodeBecauseOfResources(endCrystal)) {
                              this.rotateToCrystal(endCrystal);
                              this.attackCrystal(endCrystal);
                              this.clearExplosionState();
                              return;
                           }
                        }

                        this.explodeTicks = this.fast.get() ? 7 : 3;
                        this.attackCrystalInArea();
                     }
                  }
               }
            }
         }
      }
   }

   private void rotateToCrystal(Entity entity) {
      if (entity != null) {
         if (mc.player != null) {
            AABB boundingBox = entity.getBoundingBox();
            if (boundingBox.contains(mc.player.getEyePosition())) {
               return;
            }

            AABB boundingBox2 = entity.getBoundingBox();
            Vec3 crystalAimPoint = this.getCrystalAimPoint(boundingBox2);
            this.applyRotationToPoint(crystalAimPoint);
            return;
         }
      }
   }

   private void scheduleCrystalExplosion(BlockPos blockPos) {
      int value = 1;
      BlockPos blockPos2 = blockPos.above();
      AABB bounds = new AABB(blockPos2);
      Boolean get = this.fast.get();
      AABB bounds2 = bounds.inflate(get ? 0.5 : 0.1);
      this.crystalArea = bounds2;
      value = this.fast.get() ? 7 : 5;
      this.explodeTicks = value;
      this.pendingCrystalId = -1;
      double x = blockPos2.getX() + 0.5;
      double y = blockPos2.getY() + 0.5;
      double z = blockPos2.getZ() + 0.5;
      this.pendingCrystalPos = new Vec3(x, y, z);
      Boolean get2 = this.fast.get();
      this.pendingCrystalTicks = get2 ? 8 : 6;
   }

   private void attackCrystalInArea() {
      if (this.crystalArea != null && mc.player != null && mc.level != null) {
         if ((this.pendingCrystalId == -1 ? null : mc.level.getEntity(this.pendingCrystalId)) instanceof EndCrystal endCrystal
            && this.crystalArea.inflate(0.5).contains(endCrystal.position())) {
            if (this.shouldNotExplodeBecauseOfResources(endCrystal)) {
               this.clearExplosionState();
               return;
            }

            this.rotateToCrystal(endCrystal);
            this.attackCrystal(endCrystal);
            this.clearExplosionState();
         }
      }
   }

   private void clearExplosionState() {
      this.crystalArea = null;
      this.pendingCrystalId = -1;
      this.pendingCrystalPos = null;
      this.pendingCrystalTicks = 0;
   }

   private void onPlaceBlock(BlockPlaceEvent targetPos2) {
      if (this.isEnabled() && mc.player != null && mc.level != null) {
         if (targetPos2.getBlock() == Blocks.OBSIDIAN) {
            if (mc.player.getCooldowns().isOnCooldown(Items.END_CRYSTAL.getDefaultInstance())) {
               return;
            }

            int hotbarSlot = UseItemHelper.findHotbarSlot(Items.END_CRYSTAL, -1);
            if (hotbarSlot == -1) {
               return;
            }

            this.targetPos = targetPos2.getPos();
            this.targetSlot = hotbarSlot;
            this.scheduleCrystalExplosion(targetPos2.getPos());
            this.blocked = true;
         }
      }
   }

   private void rotateToObsidian(BlockPos blockPos) {
      BlockPos blockPos2 = blockPos.above();
      Vec3 crystalAimPoint = this.getCrystalAimPoint(blockPos2);
      this.applyRotationToPoint(crystalAimPoint);
   }

   private void onClick(CancellableEvent cancellableEvent) {
      if (this.isEnabled() && mc.player != null && mc.level != null) {
         if (mc.hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (mc.level.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN && mc.level.getBlockState(blockPos.above()).getBlock() == Blocks.AIR) {
               if (this.isHoldingCrystal()) {
                  this.rotateToObsidian(blockPos);
                  this.scheduleCrystalExplosion(blockPos);
               } else if (this.test.get()) {
                  int hotbarSlot = UseItemHelper.findHotbarSlot(Items.END_CRYSTAL, -1);
                  if (hotbarSlot != -1) {
                     if ((!(mc.player.getMainHandItem().getItem() instanceof BlockItem) || mc.player.getMainHandItem().getItem() == Items.PLAYER_HEAD)
                        && (!(mc.player.getOffhandItem().getItem() instanceof BlockItem) || mc.player.getOffhandItem().getItem() == Items.PLAYER_HEAD)) {
                        this.pkmPlacePos = blockPos;
                        this.pkmCrystalSlot = hotbarSlot;
                        this.pkmBlocked = true;
                        this.scheduleCrystalExplosion(blockPos);
                        cancellableEvent.setCancelled(true);
                     }
                  }
               }
            }
         }
      }
   }

   private boolean shouldNotExplodeBecauseOfResources(Entity entity) {
      if (!this.noExplodeResources.get()) {
         return false;
      }

      if (mc.level == null) {
         return false;
      }

      for (ItemEntity itemEntity : mc.level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(6.0), itemEntity2 -> true)) {
         Item item = itemEntity.getItem().getItem();
         if (item == Items.TOTEM_OF_UNDYING
            || item == Items.END_CRYSTAL
            || item == Items.ENCHANTED_GOLDEN_APPLE
            || item == Items.NETHERITE_HELMET
            || item == Items.NETHERITE_CHESTPLATE
            || item == Items.NETHERITE_LEGGINGS
            || item == Items.NETHERITE_BOOTS
            || item == Items.NETHERITE_SWORD
            || item == Items.DIAMOND_SWORD
            || item == Items.ELYTRA
            || item == Items.TRIDENT) {
            return true;
         }
      }

      return false;
   }

   public boolean isSilentLooking() {
      if (this.isEnabled()) {
         if (RotationManager.INSTANCE.isRotating()) {
            return true;
         }
      }

      return false;
   }

   private Vec3 getCrystalAimPoint(AABB bounds) {
      Vec3 eyePosition = mc.player.getEyePosition();
      double computedValue = (bounds.minY + bounds.maxY) * 0.5;
      double value;
      if (eyePosition.y < bounds.minY) {
         value = computedValue + (bounds.maxY - bounds.minY) * 0.15;
      } else {
         if (eyePosition.y > bounds.maxY) {
            value = computedValue;
         } else {
            double computedValue2 = bounds.minY + 0.2;
            double computedValue3 = bounds.maxY - 0.2;
            value = Mth.clamp(eyePosition.y, computedValue2, computedValue3);
         }
      }

      double computedValue4 = (bounds.minX + bounds.maxX) * 0.5;
      return new Vec3(computedValue4, value, (bounds.minZ + bounds.maxZ) * 0.5);
   }

   private AutoExplosionModule() {
      super("Auto Explosion", "Automatically detonates crystals within three blocks", ModuleCategory.COMBAT);
      this.pkmCrystalSlot = -1;
      this.test = this.add(new BooleanSetting("Right-Click Explosion", true));
      this.fast = this.add(new BooleanSetting("Fast", true));
      this.noExplodeResources = this.add(new BooleanSetting("Protect Resources", false));
      SkycoreClient.events().subscribe(BlockPlaceEvent.class, this::onPlaceBlock);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onUpdate);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onPacket);
      SkycoreClient.events().subscribe(CancellableEvent.class, this::onClick);
   }

   private void applyRotationToPoint(Vec3 position) {
      if (mc.player != null) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 position2 = position.subtract(eyePosition);
         double angleRadians = Math.atan2(position2.z, position2.x);
         double computedValue = Math.toDegrees(angleRadians) - 90.0;
         float angle = (float)Mth.wrapDegrees(computedValue);
         double y = position2.y;
         double x = Math.hypot(position2.x, position2.z);
         float angle2 = (float)(-Math.toDegrees(Math.atan2(y, x)));
         RotationManager.update(new Rotation(angle, angle2), 180.0F, 180.0F, 1, 6);
      }
   }

   private void handlePkmPlace() {
      if (this.test.get() && this.pkmPlacePos != null) {
         if (mc.level.getBlockState(this.pkmPlacePos).getBlock() != Blocks.OBSIDIAN || !mc.level.getBlockState(this.pkmPlacePos.above()).isAir()) {
            this.pkmPlacePos = null;
            this.pkmCrystalSlot = -1;
            this.pkmBlocked = false;
         } else if (this.pkmBlocked) {
            this.pkmBlocked = false;
         } else if (this.pkmCrystalSlot == -1) {
            this.pkmPlacePos = null;
         } else {
            this.placeCrystalOnObsidian(this.pkmPlacePos, this.pkmCrystalSlot, InteractionHand.MAIN_HAND);
            this.needSync = true;
            this.pkmPlacePos = null;
            this.pkmCrystalSlot = -1;
         }
      }
   }

   private void onUpdate(PlayerTickEndEvent playerTickEndEvent) {
      int value = 1;
      value = (byte)-1;
      if (this.isEnabled() && mc.player != null) {
         if (mc.level != null) {
            if (mc.gameMode != null) {
               this.handlePkmPlace();
               this.handleObsidianPlace();
               if (this.explodeTicks > 0) {
                  value = this.explodeTicks - 1;
                  this.explodeTicks = value;
               }

               this.tryAttackPendingCrystal();
               if (this.crystalArea != null) {
                  this.attackCrystalInArea();
               }

               return;
            }
         }
      }
   }

   private void placeCrystalOnObsidian(BlockPos blockPos, int slot, InteractionHand interactionHand) {
      int slot2 = -1;
      Vec3 eyePosition = mc.player.getEyePosition();
      AABB bounds = new AABB(blockPos);
      Vec3 closestVec = CriticalHelper.getClosestVec(eyePosition, bounds);
      this.rotateToObsidian(blockPos);
      Inventory inventory = mc.player.getInventory();
      slot2 = inventory.getSelectedSlot();
      this.oldSlot = slot2;
      UseItemHelper.switchToHotbarSlot(slot);
      Vec3 position = closestVec.subtract(eyePosition).scale(-1.0);
      MultiPlayerGameMode gameMode = mc.gameMode;
      LocalPlayer player = mc.player;
      Direction approximateNearest = Direction.getApproximateNearest(position.x, position.y, position.z);
      BlockHitResult blockHitResult = new BlockHitResult(closestVec, approximateNearest, blockPos, false);
      gameMode.useItemOn(player, interactionHand, blockHitResult);
      mc.player.swing(interactionHand);
      this.scheduleCrystalExplosion(blockPos);
   }
}
