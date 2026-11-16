package net.skycore.client.util;

import java.util.EnumSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class BeforeMovementHelper {
   private static boolean flushing;
   private static LocalPlayer owner;
   private static final EnumSet<InteractionHand> pending = EnumSet.noneOf(InteractionHand.class);

   public static boolean defer(LocalPlayer player, InteractionHand interactionHand) {
      if (!flushing && SynchronizeHelper.isActive()) {
         if (!player.isPassenger()) {
            if (player == Minecraft.getInstance().player && player.getItemInHand(interactionHand).is(Items.EXPERIENCE_BOTTLE)) {
               if (owner != player) {
                  pending.clear();
                  owner = player;
               }

               pending.add(interactionHand);
               return true;
            }
         }
      }

      return false;
   }

   public static void beforeMovement(LocalPlayer player) {
      Minecraft instance = Minecraft.getInstance();
      if (!pending.isEmpty()) {
         EnumSet<InteractionHand> interactionHands = EnumSet.copyOf(pending);
         pending.clear();
         if (owner == player && player == instance.player && instance.level != null && instance.gameMode != null && player.connection != null && player.isAlive()) {
            flushing = true;

            try {
               for (InteractionHand interactionHand : interactionHands) {
                  if (player.getItemInHand(interactionHand).is(Items.EXPERIENCE_BOTTLE) && !player.isUsingItem()) {
                     instance.gameMode.useItem(player, interactionHand);
                  }
               }
            } finally {
               flushing = false;
            }
         } else {
            owner = null;
         }
      }
   }

   private BeforeMovementHelper() {
   }
}
