package deluxe.mixin;

import deluxe.client.DeluxeClient;
import deluxe.client.event.EventBus;
import deluxe.client.event.PacketEvent;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin {
   @Unique
   private static boolean resendGuard;

   @Inject(method = "handlePacket(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;)V", at = @At("HEAD"), cancellable = true)
   private static void receivePacket(Packet<?> packet, PacketListener packetListener, CallbackInfo callbackInfo) {
      if (EventBus.getInstance().hasListeners(PacketEvent.class)) {
         PacketEvent packetEvent = new PacketEvent(PacketEvent.Direction.RECEIVE, packet);

         try {
            EventBus.getInstance().post(packetEvent);
         } catch (Throwable throwable) {
            DeluxeClient.logger().error("[Net] Слушатель входящего пакета {} упал", packet.getClass().getSimpleName(), throwable);
            return;
         }

         if (packetEvent.method4421()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void sendPacket(Packet<?> packet, CallbackInfo callbackInfo) {
      if (!resendGuard && EventBus.getInstance().hasListeners(PacketEvent.class)) {
         PacketEvent packetEvent = new PacketEvent(PacketEvent.Direction.SENT, packet);

         try {
            EventBus.getInstance().post(packetEvent);
         } catch (Throwable throwable) {
            DeluxeClient.logger().error("[Net] Слушатель исходящего пакета {} упал", packet.getClass().getSimpleName(), throwable);
            return;
         }

         if (packetEvent.method4421()) {
            callbackInfo.cancel();
         } else {
            if (packetEvent.getPacket() != packet) {
               callbackInfo.cancel();
               resendGuard = true;
               ((ClientConnection)(Object)this).send(packetEvent.getPacket());
               resendGuard = false;
            }
         }
      }
   }

   static {
      VMBridge.identifyClass(ClientConnectionMixin.class, "YORiXNiE");
   }
}
