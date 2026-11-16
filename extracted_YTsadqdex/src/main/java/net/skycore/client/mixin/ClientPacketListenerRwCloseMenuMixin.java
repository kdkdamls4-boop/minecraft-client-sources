package net.skycore.client.mixin;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.RotationController;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.integration.IrcClient;
import net.skycore.client.integration.PartyClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.combat.AutoExplosionModule;
import net.skycore.client.module.combat.TargetPearlModule;
import net.skycore.client.module.misc.AutoDuelModule;
import net.skycore.client.module.misc.AutoMessageModule;
import net.skycore.client.module.misc.ReallyWorldHelperModule;
import net.skycore.client.util.NeedsPacketOrderSafeHelper;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.util.SynchronizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerRwCloseMenuMixin {
   @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
   private void skycore$rwCloseMenu(ClientboundOpenScreenPacket clientboundOpenScreenPacket, CallbackInfo callbackInfo) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.isSameThread() && SynchronizeHelper.isActive()) {
         if (ReallyWorldHelperModule.INSTANCE.shouldCloseMenu(clientboundOpenScreenPacket)) {
            instance.player.connection.send(new ServerboundContainerClosePacket(clientboundOpenScreenPacket.getContainerId()));
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "onDisconnect", at = @At("HEAD"), require = 0)
   private void skycore$resetViaBypassOnDisconnect(CallbackInfo callbackInfo) {
      NeedsPacketOrderSafeHelper.resetNativeBypass();
   }

   @Inject(method = "handleAddEntity", at = @At("HEAD"))
   private void skycore$handleAddEntity(ClientboundAddEntityPacket clientboundAddEntityPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         if (SynchronizeHelper.isActive()) {
            SkycoreClient.events().post(new PacketReceiveEvent(clientboundAddEntityPacket));
         }
      }
   }

   @Inject(method = "handleAddEntity", at = @At("TAIL"))
   private void skycore$afterAddEntity(ClientboundAddEntityPacket clientboundAddEntityPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         AutoExplosionModule.INSTANCE.onCrystalSpawnApplied(clientboundAddEntityPacket);
      }
   }

   @Inject(method = "handleSystemChat", at = @At("HEAD"), cancellable = true)
   private void skycore$handleSystemChat(ClientboundSystemChatPacket clientboundSystemChatPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         if (skycore$shouldDispatchChatPacket()) {
            PacketReceiveEvent packetReceiveEvent = new PacketReceiveEvent(clientboundSystemChatPacket);
            SkycoreClient.events().post(packetReceiveEvent);
            if (packetReceiveEvent.isCancelled()) {
               callbackInfo.cancel();
            }
         }
      }
   }

   @Inject(method = "handleDisguisedChat", at = @At("HEAD"), cancellable = true)
   private void skycore$handleDisguisedChat(ClientboundDisguisedChatPacket clientboundDisguisedChatPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         if (skycore$shouldDispatchChatPacket()) {
            PacketReceiveEvent packetReceiveEvent = new PacketReceiveEvent(clientboundDisguisedChatPacket);
            SkycoreClient.events().post(packetReceiveEvent);
            if (packetReceiveEvent.isCancelled()) {
               callbackInfo.cancel();
            }
         }
      }
   }

   @Inject(method = "handleAnimate", at = @At("HEAD"))
   private void skycore$handleAnimate(ClientboundAnimatePacket clientboundAnimatePacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         if (SkycoreClient.modules().get(TargetPearlModule.class).map(ClientModule::isEnabled).orElse(false)) {
            SkycoreClient.events().post(new PacketReceiveEvent(clientboundAnimatePacket));
         }
      }
   }

   private static boolean skycore$shouldDispatchChatPacket() {
      return SkycoreClient.modules().get(AutoMessageModule.class).map(ClientModule::isEnabled).orElse(false)
         || SkycoreClient.modules().get(AutoDuelModule.class).map(ClientModule::isEnabled).orElse(false);
   }

   @Inject(method = "handleMovePlayer", at = @At("RETURN"))
   private void skycore$afterPlayerTeleport(ClientboundPlayerPositionPacket clientboundPlayerPositionPacket, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread()) {
         if (SynchronizeHelper.isActive()) {
            SkycoreClient.events().post(new PacketReceiveEvent(clientboundPlayerPositionPacket));
         }

         RotationController.resyncAfterTeleport();
      }
   }

   @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
   private void skycore$handleClientCommand(String text, CallbackInfo callbackInfo) {
      if (text != null && text.startsWith("$") && text.length() > "$".length()) {
         if (!IrcClient.isUserEnabled()) {
            SendDonationHelper.sendIrc("IRC выключен. Напиши .irc on");
            callbackInfo.cancel();
         } else {
            IrcClient.sendMessage(text.substring("$".length()).trim());
            callbackInfo.cancel();
         }
      } else if (SkycoreClient.commands() != null && SkycoreClient.commands().tryExecute(text)) {
         callbackInfo.cancel();
      } else {
         if (ReallyWorldHelperModule.INSTANCE.shouldBlockChat(text)) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "sendCommand", at = @At("HEAD"), cancellable = true)
   private void skycore$handlePartyButton(String text, CallbackInfo callbackInfo) {
      if (text != null) {
         String text2 = text.trim().toLowerCase(Locale.ROOT);
         if (text2.equals("party accept")) {
            PartyClient.accept();
            callbackInfo.cancel();
         } else if (text2.equals("party decline")) {
            PartyClient.decline();
            callbackInfo.cancel();
         }
      }
   }
}
