package net.skycore.client.util;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot;

public final class BeginSuppressViaPosRotHelper {
   private static final ThreadLocal<Boolean> SUPPRESS_VIA_POS_ROT = ThreadLocal.withInitial(() -> {
      byte flag = 0;
      flag = 0;
      flag = 0;
      return false;
   });

   public static void beginSuppressViaPosRot() {
      if (NeedsPacketOrderSafeHelper.needsInteractMoveSync()) {
         ThreadLocal sUPPRESS_VIA_POS_ROT = SUPPRESS_VIA_POS_ROT;
         Boolean condition = true;
         sUPPRESS_VIA_POS_ROT.set(condition);
      }
   }

   private BeginSuppressViaPosRotHelper() {
   }

   public static void endSuppressViaPosRot() {
      ThreadLocal sUPPRESS_VIA_POS_ROT = SUPPRESS_VIA_POS_ROT;
      Boolean condition = false;
      sUPPRESS_VIA_POS_ROT.set(condition);
   }

   public static boolean shouldDropViaPosRot(Object value) {
      Boolean tRUE = Boolean.TRUE;
      Object value2 = SUPPRESS_VIA_POS_ROT.get();
      return tRUE.equals(value2) && value instanceof PosRot;
   }
}
