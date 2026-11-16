package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.ConsumeSilentHelper;
import net.skycore.client.setting.BooleanSetting;

public final class AirStuckModule extends ClientModule {
   private boolean pendingPearlResync;
   private final NumberSetting auraDistance;
   private double peakY;
   private boolean frozen;
   private final BooleanSetting mode360;
   private Vec3 frozenPos;
   public static final AirStuckModule INSTANCE = new AirStuckModule();
   private final BooleanSetting changeAuraDistance = this.add(new BooleanSetting("Change aura range", false));
   private final BooleanSetting catchMoment;

   @Override
   protected void onEnable() {
      this.frozenPos = null;
      this.pendingPearlResync = false;
      LocalPlayer instance = Minecraft.getInstance().player;
      if (this.catchMoment.get()) {
         double intermediate;
         packetDecision: {
            if (instance != null) {
               if (!instance.onGround()) {
                  intermediate = instance.getY();
                  break packetDecision;
               }
            }

            intermediate = Double.NaN;
         }

         this.peakY = intermediate;
         this.frozen = false;
      } else {
         this.frozen = true;
         this.freezeAtCurrentPosition();
      }
   }

   private void freezeAtCurrentPosition() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         Vec3 position = instance.position();
         this.frozenPos = position;
         instance.setDeltaMovement(Vec3.ZERO);
         instance.fallDistance = 0.0;
      }
   }

   public float getKillAuraDistance() {
      if (this.isEnabled()) {
         if (this.changeAuraDistance.get()) {
            return this.auraDistance.get().floatValue();
         }
      }

      return -1.0F;
   }

   private void send360Full(float value, float value2) {
      if (this.frozenPos != null) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null) {
            PosRot posRot = new PosRot(this.frozenPos.x, this.frozenPos.y, this.frozenPos.z, value, value2, false, instance.horizontalCollision);
            ConsumeSilentHelper.sendSilentPacket(posRot);
         }
      }
   }

   private void onPreTick(PlayerTickEndEvent event) {
      if (this.isEnabled()) {
         LocalPlayer player = event.player();
         if (player != null) {
            if (this.pendingPearlResync) {
               this.pendingPearlResync = false;
               this.resyncAfterPearlTeleport();
            } else {
               if (this.frozen) {
                  if (this.frozenPos == null) {
                     this.freezeAtCurrentPosition();
                  } else {
                     player.setDeltaMovement(Vec3.ZERO);
                     player.fallDistance = 0.0;
                     player.setPos(this.frozenPos.x, this.frozenPos.y, this.frozenPos.z);
                  }
               }

               if (this.catchMoment.get()) {
                  double y = player.getY();
                  if (player.onGround()) {
                     this.peakY = Double.NaN;
                     this.frozen = false;
                     this.frozenPos = null;
                  } else if (!this.frozen) {
                     if (Double.isNaN(this.peakY)) {
                        this.peakY = y;
                     } else if (y > this.peakY) {
                        this.peakY = y;
                     } else if (y < this.peakY) {
                        this.frozen = true;
                        this.freezeAtCurrentPosition();
                     }
                  }
               }
            }
         }
      }
   }

   private void onReceive(PacketReceiveEvent packetReceiveEvent) {
      if (this.isEnabled()) {
         if (packetReceiveEvent.getPacket() instanceof ClientboundPlayerPositionPacket) {
            this.frozen = false;
            this.frozenPos = null;
            this.peakY = Double.NaN;
            this.pendingPearlResync = true;
         }
      }
   }

   private AirStuckModule() {
      super("Air Stuck", "Freeze yourself in the air", ModuleCategory.MOVEMENT);
      this.auraDistance = this.add(new NumberSetting("Aura range", "", 3.5, 2.0, 6.0, 0.1, " blocks"));
      this.catchMoment = this.add(new BooleanSetting("Catch peak", true));
      this.mode360 = this.add(new BooleanSetting("360 keep-alive", true));
      this.peakY = Double.NaN;
      this.auraDistance.visibleWhen(this.changeAuraDistance::get);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
      SkycoreClient.events().subscribe(PacketSendEvent.class, this::onSend);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onReceive);
      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         if (this.isEnabled()) {
            this.setEnabled(false);
         }
      });
   }

   @Override
   protected void onDisable() {
      this.frozen = false;
      this.frozenPos = null;
      this.pendingPearlResync = false;
      this.peakY = Double.NaN;
   }

   private void onSend(PacketSendEvent packetSendEvent) {
      if (this.isEnabled() && this.frozen && Minecraft.getInstance().player != null) {
         if (packetSendEvent.getPacket() instanceof ServerboundAttackPacket) {
            this.sendAttackSnap();
         } else if (packetSendEvent.getPacket() instanceof ServerboundMovePlayerPacket serverboundMovePlayerPacket) {
            packetSendEvent.cancel();
            if (this.mode360.get() && this.frozenPos != null) {
               LocalPlayer instance = Minecraft.getInstance().player;
               this.send360Full(serverboundMovePlayerPacket.getYRot(instance.getYRot()), serverboundMovePlayerPacket.getXRot(instance.getXRot()));
            }
         }
      }
   }

   private void resyncAfterPearlTeleport() {
      ClientModule.runSilently(() -> {
         this.setEnabled(false);
         this.setEnabled(true);
      });
   }

   public boolean isFrozen() {
      if (this.isEnabled()) {
         if (this.frozen) {
            return true;
         }
      }

      return false;
   }

   public void sendAttackSnap() {
      if (this.isFrozen()) {
         Boolean get = this.mode360.get();
         if (get && this.frozenPos != null) {
            Minecraft instance = Minecraft.getInstance();
            LocalPlayer player = instance.player;
            if (player == null) {
               return;
            }

            float yRot = player.getYRot();
            float xRot = player.getXRot();
            this.send360Full(yRot, xRot);
            return;
         }
      }
   }
}
