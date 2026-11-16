package net.skycore.client.event;

import net.skycore.client.SkycoreClient;
import net.skycore.client.command.CommandManager;

@Deprecated
public final class ChatCommandHandler {
   private ChatCommandHandler() {
   }

   public static boolean handle(String text) {
      if (SkycoreClient.commands() != null) {
         CommandManager commandManager = SkycoreClient.commands();
         if (commandManager.tryExecute(text)) {
            return true;
         }
      }

      return false;
   }
}
