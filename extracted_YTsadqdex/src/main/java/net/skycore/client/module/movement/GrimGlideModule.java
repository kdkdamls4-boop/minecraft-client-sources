package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.util.FlightRawHelper;
import net.skycore.client.util.ResetSpeedHelper;

public final class GrimGlideModule extends ClientModule {
   private final SliderSetting timerBoost = this.add(new SliderSetting("Timer Boost", 1.5, 1.0, 5.0, 0.1));
   private final SliderSetting timerReset = this.add(new SliderSetting("Сброс таймера", 0.5, 0.0, 0.9, 0.01));
   public static final GrimGlideModule INSTANCE = new GrimGlideModule();
   private boolean corrected;
   private boolean timerActive;
   private int timerTicks;
   private boolean sendingStatus;
   private int flagTicks;

   @Override
   protected void onDisable() {
      if (this.timerActive) {
         ResetSpeedHelper.setSpeed(1.0F);
         this.timerActive = false;
      }
   }

   private GrimGlideModule() {
      super("GrimGlide", "Ускоряет полёт на элитрах без фейерверков", ModuleCategory.MOVEMENT);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::receive);
      SkycoreClient.events().subscribe(PacketSendEvent.class, this::send);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, playerTickEndEvent -> {
         this.preTick();
      });
   }

   private void preTick() {
      if (this.isEnabled()) {
         if (this.flagTicks > 0) {
            this.flagTicks--;
         }

         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null && instance.isFallFlying()) {
            ResetSpeedHelper.setSpeed(this.timerTicks % 2 == 0 ? this.timerBoost.get().floatValue() : this.timerReset.get().floatValue());
            this.timerActive = true;
            this.timerTicks++;
         } else {
            if (this.timerActive) {
               ResetSpeedHelper.setSpeed(1.0F);
               this.timerActive = false;
            }
         }
      }
   }

   public Vec3 onGlidingVelocity(Vec3 position) {
      if (!this.isEnabled()) {
         return position;
      } else {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance == null) {
            return position;
         } else {
            Vec3 deltaMovement = FlightRawHelper.flightRaw(instance, instance.getDeltaMovement(), 3.2, 0.04F);
            double yRot = Math.toRadians(instance.getYRot());
            if (this.flagTicks >= 1) {
               Vec3 position2 = deltaMovement.multiply(0.99F, 0.98F, 0.99F);
               double computedValue = -Math.sin(yRot) * 0.09F;
               return position2.add(computedValue, 0.03F, Math.cos(yRot) * 0.09F);
            } else {
               return deltaMovement.multiply(0.3F, 0.3F, 0.3F);
            }
         }
      }
   }

   @Override
   protected void onEnable() {
      this.flagTicks = 0;
      this.corrected = false;
      this.timerTicks = 0;
      this.timerActive = false;
   }

   private void receive(PacketReceiveEvent packetReceiveEvent) {
      if (this.isEnabled() && packetReceiveEvent.getPacket() instanceof ClientboundPlayerPositionPacket) {
         this.flagTicks = 2;
         this.corrected = true;
         if (this.timerTicks % 2 == 1) {
            this.timerTicks++;
         }
      }
   }

   private void send(PacketSendEvent packetSendEvent) {
      if (this.isEnabled() && !this.sendingStatus && packetSendEvent.getPacket() instanceof ServerboundMovePlayerPacket) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null && instance.isFallFlying() && this.flagTicks == 0 && !this.corrected) {
            this.sendingStatus = true;

            try {
               instance.connection.send(new StatusOnly(true, true));
            } finally {
               this.sendingStatus = false;
            }

            packetSendEvent.cancel();
         }

         this.corrected = false;
      }
   }
}
