package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;

public final class FreeCameraModule extends ClientModule {
   private boolean oldNoPhysics;
   private final SliderSetting speedY;
   private Vec3 origin;
   private final SliderSetting speedXZ = this.add(new SliderSetting("Скорость XZ", 1.5, 0.1, 5.0, 0.1));
   public static final FreeCameraModule INSTANCE = new FreeCameraModule();

   @Override
   protected void onEnable() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         this.origin = instance.player.position();
         this.oldNoPhysics = instance.player.noPhysics;
         instance.player.noPhysics = true;
      }
   }

   @Override
   protected void onDisable() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (this.origin != null) {
            instance.player.setPos(this.origin);
         }

         instance.player.noPhysics = this.oldNoPhysics;
         instance.player.setDeltaMovement(Vec3.ZERO);
      }

      this.origin = null;
   }

   private void onPacket(PacketSendEvent packetSendEvent) {
      if (this.isEnabled() && packetSendEvent.getPacket() instanceof ServerboundMovePlayerPacket) {
         packetSendEvent.cancel();
      }
   }

   private FreeCameraModule() {
      super("Free Camera", "Free camera without moving the server-side player", ModuleCategory.MOVEMENT);
      this.speedY = this.add(new SliderSetting("Скорость Y", 1.0, 0.1, 5.0, 0.1));
      SkycoreClient.events().subscribe(PacketSendEvent.class, this::onPacket);
      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         if (this.isEnabled()) {
            this.setEnabled(false);
         }
      });
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         instance.player.noPhysics = true;
         Vec2 moveVector = instance.player.input.getMoveVector();
         double yRot = Math.toRadians(instance.player.getYRot());
         double value = this.speedXZ.get();
         double value2 = this.speedY.get();
         double x = (-Math.sin(yRot) * moveVector.y + Math.cos(yRot) * moveVector.x) * value;
         double x2 = (Math.cos(yRot) * moveVector.y + Math.sin(yRot) * moveVector.x) * value;
         double computedValue = instance.options.keyJump.isDown() ? value2 : (instance.options.keyShift.isDown() ? -value2 : 0.0);
         instance.player.setPos(instance.player.getX() + x, instance.player.getY() + computedValue, instance.player.getZ() + x2);
         instance.player.setDeltaMovement(Vec3.ZERO);
      }
   }
}
