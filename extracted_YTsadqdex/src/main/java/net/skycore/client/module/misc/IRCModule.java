package net.skycore.client.module.misc;

import net.skycore.client.integration.IrcClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class IRCModule extends ClientModule {
   public static final IRCModule INSTANCE = new IRCModule();

   @Override
   protected void onDisable() {
      if (IrcClient.isUserEnabled()) {
         IrcClient.turnOff();
      }
   }

   @Override
   protected void onEnable() {
      if (!IrcClient.isUserEnabled()) {
         IrcClient.turnOn();
      }
   }

   private IRCModule() {
      super("IRC", "In-game IRC between client users", ModuleCategory.MISC);
   }
}
