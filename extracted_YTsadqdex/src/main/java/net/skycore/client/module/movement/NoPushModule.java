package net.skycore.client.module.movement;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class NoPushModule extends ClientModule {
   private final BooleanSetting blocksVisibility;
   private final MultiSelectSetting mode = this.add(
      new MultiSelectSetting("Type", List.of("Players", "Blocks", "Water", "Fishing rod"), Set.of("Players", "Blocks", "Water", "Fishing rod"))
   );
   public static final NoPushModule INSTANCE = new NoPushModule();

   public static boolean fishingRodPush() {
      NoPushModule noPushModule = enabled();
      if (noPushModule != null) {
         if (noPushModule.mode.isSelected("Fishing rod")) {
            return true;
         }
      }

      return false;
   }

   public static boolean allowsBlockVisibility() {
      NoPushModule noPushModule = enabled();
      if (noPushModule != null && noPushModule.mode.isSelected("Blocks")) {
         if (noPushModule.blocksVisibility.get()) {
            return true;
         }
      }

      return false;
   }

   public static boolean playersPush() {
      NoPushModule noPushModule = enabled();
      if (noPushModule != null) {
         if (noPushModule.mode.isSelected("Players")) {
            return true;
         }
      }

      return false;
   }

   private NoPushModule() {
      super("No Push", "Avoids knockback from players, blocks, water, and fishing hooks", ModuleCategory.MOVEMENT);
      this.blocksVisibility = this.add(new BooleanSetting("Visible in blocks", true));
      this.blocksVisibility.visibleWhen(() -> {
         return this.mode.isSelected("Blocks");
      });
   }

   private static NoPushModule enabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(NoPushModule.class);
      NoPushModule noPushModule = (NoPushModule)optional.orElse(null);
      return noPushModule != null && noPushModule.isEnabled() ? noPushModule : null;
   }

   public static boolean blocksPush() {
      NoPushModule noPushModule = enabled();
      if (noPushModule != null) {
         if (noPushModule.mode.isSelected("Blocks")) {
            return true;
         }
      }

      return false;
   }

   public static boolean waterPush() {
      NoPushModule noPushModule = enabled();
      if (noPushModule != null) {
         if (noPushModule.mode.isSelected("Water")) {
            return true;
         }
      }

      return false;
   }
}
