package deluxe.client.module.misc;

import deluxe.client.DeluxeClient;
import com.deluxe.lib.discord.DiscordNative$Handlers;
import com.deluxe.lib.discord.DiscordNative$Presence;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.util.DiscordRpcLibrary;

import java.util.Locale;

@ModuleInfo(name = "DiscordRPC", description = "Показывает клиент в статусе Discord.", category = ModuleCategory.MISC)
public final class DiscordRPCModule extends Module {
   private static DiscordRPCModule discordRPCModule;
   private final DiscordNative$Handlers field11624 = new DiscordNative$Handlers();
   private volatile Thread field11625;
   private volatile String field11626 = "";
   private long field11627;

   private void method3301() {
      try {
         DiscordRpcLibrary.discordRpcLibrary.Discord_Initialize("1527791397556588664", this.field11624, true, "");
         this.method3304();

         while (this.field11625 == Thread.currentThread()) {
            DiscordRpcLibrary.discordRpcLibrary.Discord_RunCallbacks();
            Thread.sleep(2000L);
         }
      } catch (InterruptedException exception) {
      } catch (Throwable throwable) {
         DeluxeClient.logger().warn("Discord RPC недоступен: {}", throwable.toString());
      } finally {
         try {
            DiscordRpcLibrary.discordRpcLibrary.Discord_ClearPresence();
            DiscordRpcLibrary.discordRpcLibrary.Discord_Shutdown();
         } catch (Throwable throwable2) {
         }
      }
   }

   public static DiscordRPCModule method3302() {
      if (discordRPCModule == null) {
         discordRPCModule = new DiscordRPCModule();
      }

      return discordRPCModule;
   }

   private void method3304() {
      DiscordNative$Presence presence = new DiscordNative$Presence();
      ProfileAPI.get().getModel().getName();
      presence.details = "cracked by Vk0ntakt3";
      presence.state = "feel deluxe";
      presence.startTimestamp = this.field11627;
      presence.largeImageKey = "https://i.ibb.co/hxsyxR5h/ASD1-ASD0000-0200-2.gif";
      presence.largeImageText = "t.me/delnmt";
      if (!this.field11626.isEmpty()) {
         presence.smallImageKey = this.field11626;
         presence.smallImageText = "feel deluxe";
      }

      presence.button_label_1 = "Телеграм";
      presence.button_url_1 = "https://t.me/delnmt";
      presence.button_label_2 = "Дискорд";
      presence.button_url_2 = "https://discord.gg/deluxeclient";
      DiscordRpcLibrary.discordRpcLibrary.Discord_UpdatePresence(presence);
   }

   @Override
   protected void onEnable() {
      if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux") && this.field11625 == null) {
         this.field11627 = System.currentTimeMillis() / 1000L;
         Thread thread = new Thread(this::method3301, "deluxe-discord-rpc");
         thread.setDaemon(true);
         this.field11625 = thread;
         thread.start();
      }
   }

   private DiscordRPCModule() {
      this.field11624.ready = user -> {
         if (user.avatar != null && !user.avatar.isEmpty()) {
            this.field11626 = "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png";
         }

         this.method3304();
      };
   }

   @Override
   protected void onDisable() {
      Thread thread = this.field11625;
      this.field11625 = null;
      if (thread != null) {
         thread.interrupt();
      }
   }
}
