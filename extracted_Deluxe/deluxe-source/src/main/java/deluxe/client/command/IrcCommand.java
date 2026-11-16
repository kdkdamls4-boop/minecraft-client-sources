package deluxe.client.command;

import deluxe.client.util.ClientChat;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.util.IrcClient;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;

import java.util.ArrayList;
import java.util.Arrays;

public class IrcCommand implements ClientCommand {
   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length > 1) {
         return new ArrayList<>();
      }

      ArrayList<String> local7467_2 = new ArrayList<>(Arrays.asList("on", "off"));
      if (this.method4438()) {
         local7467_2.add("online");
      }

      return local7467_2;
   }

   private boolean method4438() {
      return ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN);
   }

   @Override
   public String getName() {
      return "irc";
   }

   @Override
   public String getDescription() {
      return "Чат клиента (.irc on/off, писать через @сообщение)";
   }

   @Override
   public void execute(String[] stringArray) {
      switch (stringArray.length == 0 ? "" : stringArray[0].toLowerCase()) {
         case "on":
            IrcClient.method2834().method2829(true);
            ClientChat.method2267("IRC включён, пиши в чат через &7@сообщение");
            break;
         case "off":
            IrcClient.method2834().method2829(false);
            ClientChat.method2267("IRC выключен.");
            break;
         case "online":
            if (!this.method4438()) {
               ClientChat.method2267("&cКоманда доступна только администраторам.");
            } else if (!IrcClient.method2834().method2826()) {
               ClientChat.method2267("IRC выключен, включи через &7.irc on");
            } else {
               IrcClient.method2834().method2824();
            }
            break;
         default:
            ClientChat.method2267(
               "IRC " + (IrcClient.method2834().method2826() ? "&aвключён" : "&cвыключен") + "&r. Использование: &7.irc on/off&r, сообщение: &7@текст"
            );
      }
   }
}
