package net.skycore.client.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;

public final class ConsumeSilentHelper {
   private static final Set<Packet<?>> SILENT = Collections.newSetFromMap(new ConcurrentHashMap<>());

   public static boolean consumeSilent(Packet<?> packet) {
      if (packet != null) {
         if (SILENT.remove(packet)) {
            return true;
         }
      }

      return false;
   }

   public static void sendSilentPacket(Packet<?> packet) {
      if (packet != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.getConnection() != null) {
            SILENT.add(packet);
            ClientPacketListener connection = instance.getConnection();
            connection.send(packet);
         }
      }
   }

   private ConsumeSilentHelper() {
   }
}
