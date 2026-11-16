package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class LookHelper {
   private LookHelper() {
   }

   public static Rotation calculateAngle(Vec3 position) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && position != null) {
         Vec3 eyePosition = player.getEyePosition();
         return Rotation.lookingAt(position, eyePosition);
      } else {
         return Rotation.ZERO;
      }
   }
}
