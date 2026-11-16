package tokyo.ghoul.client.features.modules.render.targetesp.render;

import java.util.Random;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public interface McHolder {
   @NotNull
   Minecraft f_11537 = Minecraft.getInstance();

   default float m_4026(float var1) {
      return (new Random().nextFloat() * 2.0F - 1.0F) * var1;
   }
}
