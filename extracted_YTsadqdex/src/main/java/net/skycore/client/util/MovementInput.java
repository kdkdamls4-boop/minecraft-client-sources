package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;

public final class MovementInput {
   private MovementInput() {
   }

   public static boolean hasPlayerMovement() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (instance.input != null) {
            Vec2 moveVector = instance.input.getMoveVector();
            if (moveVector.lengthSquared() > 0.0F) {
               return true;
            }
         }
      }

      return false;
   }
}
