package deluxe.client.util;

import deluxe.mixin.ClientCommonNetworkHandlerMixin;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.MinecraftAccess;

import java.io.BufferedInputStream;
import java.util.HashMap;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

public class SoundPlayer implements MinecraftAccess {
   private static final HashMap<String, Clip[]> field10733 = new HashMap<>();

   public static void method3158(String local5083_0) {
      if (mc.world != null
         && mc.player != null
         && !(mc.currentScreen instanceof LevelLoadingScreen)
         && ((ClientCommonNetworkHandlerMixin)mc.player.networkHandler).connection().isOpen()
         && InterfaceModule.method2247().field07924.isChecked()
         && !(InterfaceModule.method2247().field07925.getValue() <= 0.0F)) {
         if (!field10733.containsKey(local5083_0)) {
            Clip[] clipArray = new Clip[2];
            field10733.put(local5083_0, clipArray);
            Thread thread = new Thread(
               () -> {
                  for (int index = 0; index < clipArray.length; index++) {
                     try {
                        Clip clip = AudioSystem.getClip();
                        clip.open(
                           AudioSystem.getAudioInputStream(
                              new BufferedInputStream(SoundPlayer.class.getResourceAsStream("/assets/deluxe/sound/" + local5083_0 + ".wav"))
                           )
                        );
                        clipArray[index] = clip;
                     } catch (Exception var4x) {
                     }
                  }
               }
            );
            thread.setDaemon(true);
            thread.start();
         } else {
            for (int index2 = 0; index2 < ((Clip[])field10733.get(local5083_0)).length; index2++) {
               if (field10733.get(local5083_0)[index2] != null
                  && (!field10733.get(local5083_0)[index2].isRunning() || index2 >= field10733.get(local5083_0).length - 1)) {
                  try {
                     ((FloatControl)field10733.get(local5083_0)[index2].getControl(Type.MASTER_GAIN))
                        .setValue(
                           Math.max(
                              20.0F * (float)Math.log10(InterfaceModule.method2247().field07925.getValue() / 100.0F),
                              ((FloatControl)field10733.get(local5083_0)[index2].getControl(Type.MASTER_GAIN)).getMinimum()
                           )
                        );
                     field10733.get(local5083_0)[index2].stop();
                     field10733.get(local5083_0)[index2].setFramePosition(0);
                     field10733.get(local5083_0)[index2].start();
                  } catch (Exception exception) {
                  }

                  return;
               }
            }
         }
      }
   }
}
