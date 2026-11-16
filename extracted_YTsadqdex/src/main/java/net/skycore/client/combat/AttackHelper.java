package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AttackHelper {
   private AttackHelper() {
   }

   public static void sendFixedAttack(Player player, Entity entity) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.gameMode != null && player != null && entity != null) {
         instance.gameMode.attack(player, entity);
         player.swing(InteractionHand.MAIN_HAND);
      }
   }
}
