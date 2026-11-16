package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;
import org.apache.commons.lang3.mutable.MutableObject;

public final class ShouldDropFailedUseOnHelper {
   private static final ThreadLocal<Boolean> DROP_FAILED_USE_ON = ThreadLocal.withInitial(() -> {
      byte flag = 1;
      return false;
   });

   public static boolean shouldDropFailedUseOn(Object value) {
      return Boolean.TRUE.equals(DROP_FAILED_USE_ON.get()) && value instanceof ServerboundUseItemOnPacket;
   }

   public static InteractionResult useItemOn(LocalPlayer player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
      Minecraft instance = Minecraft.getInstance();
      MultiPlayerGameMode gameMode = instance.gameMode;
      ClientLevel level = instance.level;
      if (player == null || gameMode == null || level == null || blockHitResult == null) {
         return InteractionResult.PASS;
      }

      if (gameMode instanceof MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor) {
         multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$ensureHasSentCarriedItem();
         MutableObject mutableObject = new MutableObject<>(InteractionResult.PASS);

         try {
            multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$startPrediction(level, value -> {
               int value2 = 1;
               value2 = (byte)1;
               value2 = (byte)1;
               InteractionResult interactionResult = multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$performUseItemOn(player, interactionHand, blockHitResult);
               mutableObject.setValue(interactionResult);
               ThreadLocal dROP_FAILED_USE_ON = DROP_FAILED_USE_ON;
               value2 = !interactionResult.consumesAction() ? 1 : 0;
               dROP_FAILED_USE_ON.set(Boolean.valueOf((value2 != 0)));
               return new ServerboundUseItemOnPacket(interactionHand, blockHitResult, value);
            });
         } finally {
            DROP_FAILED_USE_ON.set(false);
         }

         return (InteractionResult)mutableObject.getValue();
      } else {
         return gameMode.useItemOn(player, interactionHand, blockHitResult);
      }
   }

   private ShouldDropFailedUseOnHelper() {
   }
}
