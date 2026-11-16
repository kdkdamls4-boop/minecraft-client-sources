package net.skycore.client.module.misc;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.BooleanSetting;

public final class ServerRPSpoofModule extends ClientModule {
   private final BooleanSetting onMainThread = this.add(new BooleanSetting("On main thread", false));
   public static final ServerRPSpoofModule INSTANCE = new ServerRPSpoofModule();

   private ServerRPSpoofModule() {
      super("Server RP Spoof", "Only ACK server RP without downloading — turn OFF on 1.17.x if custom heads look identical", ModuleCategory.MISC);
   }

   public boolean finishOnMainThread() {
      return this.onMainThread.get();
   }
}
