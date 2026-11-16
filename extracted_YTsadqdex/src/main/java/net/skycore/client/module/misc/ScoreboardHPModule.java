package net.skycore.client.module.misc;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class ScoreboardHPModule extends ClientModule {
   public ScoreboardHPModule() {
      super("Scoreboard HP", "Reads opponent health from the scoreboard", ModuleCategory.MISC);
   }
}
