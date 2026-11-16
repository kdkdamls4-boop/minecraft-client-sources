package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public final class PlayModeHelper {
   private static void playMode(float value, float value2, int value3) {
      int value4 = 0;
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && !(value <= 0.0F)) {
         value4 = Math.floorMod(value3, 6) + 1;
         Identifier texture = Identifier.parse("skycore:client.enable" + value4);
         SoundSource mASTER = SoundSource.MASTER;
         float minimum = Math.min(value, 1.0F);
         float maximum = Math.max(0.0F, minimum);
         float minimum2 = Math.min(value2, 2.0F);
         minimum = Math.max(0.5F, minimum2);
         RandomSource randomSource = SoundInstance.createUnseededRandom();
         SimpleSoundInstance simpleSoundInstance = new SimpleSoundInstance(texture, mASTER, maximum, minimum, randomSource, false, 0, Attenuation.NONE, 0.0, 0.0, 0.0, true);
         instance.getSoundManager().play(simpleSoundInstance);
      }
   }

   public static void playDisable(float value, float value2, int value3) {
      playMode(value, value2, value3);
   }

   public static void playEnable(float value, int value2) {
      playMode(value, 1.0F, value2);
   }

   private PlayModeHelper() {
   }
}
