package tokyo.ghoul.client.features.modules.render.targetesp.render;

import lombok.Generated;
import net.minecraft.resources.Identifier;

public final class EspResources {
   private static final String f_34360 = "desire";
   private static final String f_34361 = "desire";
   private static final String f_34362 = "desire";
   private static final String f_34363 = "This is a utility class and cannot be instantiated";

   public static Identifier m_9310(String var0) {
      String path = var0.startsWith("fonts/") ? var0 : "fonts/" + var0;
      return Identifier.fromNamespaceAndPath(f_34361, path);
   }

   @Generated
   private EspResources() {
      throw new UnsupportedOperationException(f_34363);
   }

   public static Identifier m_9311(String var0) {
      String path = "targetesp/" + (var0.startsWith("images/") ? var0 : "images/" + var0);
      if (!path.endsWith(".png")) {
         path = path + ".png";
      }
      return Identifier.fromNamespaceAndPath(f_34362, path);
   }

   public static Identifier m_9312(String var0) {
      String path = "core/targetesp/" + (var0.startsWith("core/") ? var0.substring(5) : var0);
      return Identifier.fromNamespaceAndPath(f_34360, path);
   }
}
