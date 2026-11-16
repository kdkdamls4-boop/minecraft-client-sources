package net.skycore.client.module.combat;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.PacketMovementTracker;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.ConsumeSilentHelper;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class CriticalsModule extends ClientModule {
   private static final String MODE_SLOW_FALLING = "С плавным падением";
   private static final double ACCELERATION_MAX_Y = -0.085F;
   private static final double ACCELERATION_MIN_Y = -0.15F;
   private static final float CRITICAL_FALL_DISTANCE = 0.001F;
   private int clientTick;
   private final BooleanSetting accelerateAttacks;
   private final PacketMovementTracker<Packet<?>> packetState;
   private static final float ACCELERATION_RANGE = 2.5F;
   private static final float ACCELERATION_TIMER = 0.6F;
   private static final double POSITION_OFFSET = 1.0E-9;
   private static final String MODE_COBWEB = "В паутине";
   public static final CriticalsModule INSTANCE = new CriticalsModule();
   private final MultiSelectSetting workWhen = this.add(
      new MultiSelectSetting("Работать если", List.of("В паутине", "С плавным падением"), Set.of("В паутине", "С плавным падением"))
   );

   private void resetPacketState() {
      this.packetState.reset();
   }

   private void ensureCriticalFallDistance() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.player.fallDistance <= 0.0) {
         instance.player.fallDistance = 0.001F;
      }
   }

   private boolean isMovementKeyDown() {
      Minecraft instance = Minecraft.getInstance();
      if (!instance.options.keyUp.isDown()) {
         if (!instance.options.keyDown.isDown()) {
            if (!instance.options.keyLeft.isDown() && !instance.options.keyRight.isDown()) {
               if (!instance.options.keyJump.isDown()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   @Override
   protected void onDisable() {
      this.flushDeferredPackets();
      this.resetPacketState();
   }

   private void flushDeferredPackets() {
      PacketMovementTracker packetState2 = this.packetState;
      Consumer<net.minecraft.network.protocol.Packet> consumer = ConsumeSilentHelper::sendSilentPacket;
      packetState2.flush(consumer);
   }

   private boolean isInsideCobweb() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         AABB boundingBox = instance.player.getBoundingBox();
         int roundedDown = Mth.floor(boundingBox.minX);
         int roundedDown2 = Mth.floor(boundingBox.minY);
         int roundedDown3 = Mth.floor(boundingBox.minZ);
         int roundedDown4 = Mth.floor(boundingBox.maxX);
         int roundedDown5 = Mth.floor(boundingBox.maxY);
         int roundedDown6 = Mth.floor(boundingBox.maxZ);
         MutableBlockPos mutablePos = new MutableBlockPos();

         for (int index = roundedDown; index <= roundedDown4; index++) {
            for (int index2 = roundedDown2; index2 <= roundedDown5; index2++) {
               for (int index3 = roundedDown3; index3 <= roundedDown6; index3++) {
                  if (instance.level.getBlockState(mutablePos.set(index, index2, index3)).is(Blocks.COBWEB)) {
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

   public CriticalsModule() {
      super("Criticals", "Позволяет наносить критические удары", ModuleCategory.COMBAT);
      this.accelerateAttacks = this.add(new BooleanSetting("Ускорение ударов", false));
      this.packetState = new PacketMovementTracker<>();
      this.accelerateAttacks.visibleWhen(() -> {
         return this.workWhen.isSelected("С плавным падением");
      });
      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         this.resetPacketState();
      });
   }

   public void onAttackPost(Entity entity) {
   }

   public void onPacketSend(PacketSendEvent packetSendEvent) {
      if (this.isEnabled() && !packetSendEvent.isCancelled()) {
         Packet packet = packetSendEvent.getPacket();
         if (packet instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            this.packetState.movement(serverboundMovePlayerPacket.hasPosition(), serverboundMovePlayerPacket.getX(Double.NaN), serverboundMovePlayerPacket.getY(Double.NaN), serverboundMovePlayerPacket.getZ(Double.NaN), this.clientTick);
         } else if (packet instanceof ServerboundClientTickEndPacket) {
            this.packetState.endTick(packet, packet2 -> {
               packetSendEvent.cancel();
               ConsumeSilentHelper.sendSilentPacket(packet2);
            });
         } else {
            if (!(packet instanceof ServerboundAttackPacket) && !(packet instanceof ServerboundSwingPacket)) {
               if (packet instanceof ServerboundPongPacket) {
                  if (!this.isMovementKeyDown() || !this.shouldDeferSlowFallingPackets()) {
                     return;
                  }

                  this.packetState.pong(packet);
                  packetSendEvent.cancel();
               }
            } else {
               if (!this.shouldDeferSlowFallingPackets()) {
                  return;
               }

               if (this.packetState.interaction(packet, packet instanceof ServerboundSwingPacket)) {
                  packetSendEvent.cancel();
               }
            }
         }
      }
   }

   public void onPlayerPreTick() {
      if (this.isEnabled()) {
         if (!this.shouldDeferSlowFallingPackets()) {
            this.flushDeferredPackets();
         }
      }
   }

   public boolean allowsAuraCritical(Player player) {
      if (!this.isEnabled() || player == null || player.isInWater() || player.isInLava() || player.isFallFlying()) {
         return false;
      } else {
         return this.usesSlowFallingMode(player)
            ? !player.onGround() && player.fallDistance >= 0.001
            : this.workWhen.isSelected("В паутине") && this.isInsideCobweb();
      }
   }

   public void onClientTickStart() {
      int intermediate = -1;
      intermediate = this.clientTick + 1;
      this.clientTick = intermediate;
   }

   private LivingEntity currentAuraTarget() {
      var optional = SkycoreClient.modules().get(AttackAuraModule.class);
      var optional2 = optional.map(AttackAuraModule::currentTarget);
      return (LivingEntity)optional2.orElse(null);
   }

   @Override
   protected void onEnable() {
      this.resetPacketState();
   }

   public boolean usesSlowFallingMode(Player player) {
      if (this.isEnabled()) {
         if (this.workWhen.isSelected("С плавным падением") && player != null) {
            if (player.hasEffect(MobEffects.SLOW_FALLING) && !player.isFallFlying()) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean shouldDeferSlowFallingPackets() {
      Minecraft instance = Minecraft.getInstance();
      if (this.usesSlowFallingMode(instance.player)) {
         if (instance.player.connection != null && !instance.player.onGround()) {
            if (!instance.player.isInWater()) {
               if (!instance.player.isInLava() && this.currentAuraTarget() != null && this.packetState.descending()) {
                  boolean recentSmallMovement = this.packetState.recentSmallMovement(this.clientTick);
                  if (this.isMovementKeyDown() || recentSmallMovement) {
                     if (!this.isInsideCobweb()) {
                        return true;
                     }
                  }

                  return false;
               }
            }
         }
      }

      return false;
   }

   public float timerSpeedOverride() {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled()
         && instance.player != null
         && !instance.player.isFallFlying()
         && !instance.player.isInWater()
         && !instance.player.isInLava()
         && this.accelerateAttacks.get()
         && this.workWhen.isSelected("С плавным падением")
         && instance.player.hasEffect(MobEffects.SLOW_FALLING)
         && !instance.player.onGround()) {
         LivingEntity entity = this.currentAuraTarget();
         if (entity != null && !((float)CriticalHelper.getStrictDistance(entity) > 2.5)) {
            double deltaMovement = instance.player.getDeltaMovement().y;
            return deltaMovement <= -0.085F && deltaMovement >= -0.15F ? 0.6F : Float.NaN;
         } else {
            return Float.NaN;
         }
      } else {
         return Float.NaN;
      }
   }

   public void onAttackPre(Entity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && entity != null && instance.player != null && instance.level != null) {
         LocalPlayer player = instance.player;
         if (!player.isFallFlying() && !player.isInWater() && !player.isInLava() && this.workWhen.isSelected("В паутине") && this.isInsideCobweb()) {
            player.connection.send(new Pos(player.getX(), player.getY() + 1.0E-9, player.getZ(), false, player.horizontalCollision));
         }
      }
   }
}
