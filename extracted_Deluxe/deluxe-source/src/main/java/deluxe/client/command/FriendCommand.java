package deluxe.client.command;

import deluxe.client.util.ClientChat;
import deluxe.client.storage.FriendStore;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;

public class FriendCommand implements ClientCommand {
   private static String method4015(String local6703_0) {
      if (MinecraftClient.getInstance().getNetworkHandler() == null) {
         return local6703_0;
      }

      for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
         if (playerListEntry.getProfile().name().equalsIgnoreCase(local6703_0)) {
            return playerListEntry.getProfile().name();
         }
      }

      return local6703_0;
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("add", "remove", "list", "clear"));
      }

      if (stringArray.length != 2) {
         return new ArrayList<>();
      }

      if (!stringArray[0].equalsIgnoreCase("remove") && !stringArray[0].equalsIgnoreCase("delete")) {
         ArrayList<String> local6704_2 = new ArrayList<>();
         if (stringArray[0].equalsIgnoreCase("add") && MinecraftClient.getInstance().getNetworkHandler() != null) {
            for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
               if (!FriendStore.method3431(playerListEntry.getProfile().name())) {
                  local6704_2.add(playerListEntry.getProfile().name());
               }
            }
         }

         return local6704_2;
      } else {
         return FriendStore.method3432();
      }
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length == 0 || stringArray[0].equalsIgnoreCase("list")) {
         ClientChat.method2267(
            FriendStore.method3432().isEmpty() ? "Друзей пока нет." : "Друзья: &7" + String.join(", ", FriendStore.method3432())
         );
      } else if (stringArray[0].equalsIgnoreCase("clear")) {
         FriendStore.method3435();
         ClientChat.method2267("Список друзей очищен.");
      } else if (stringArray.length < 2) {
         ClientChat.method2267("Использование: &7.friend add/remove/list/clear <ник>");
      } else {
         switch (stringArray[0].toLowerCase()) {
            case "add":
               ClientChat.method2267(
                  FriendStore.method3434(method4015(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))))
                     ? "&7" + method4015(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r добавлен в друзья."
                     : "&7" + method4015(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r уже в друзьях."
               );
               break;
            case "remove":
            case "delete":
               ClientChat.method2267(
                  FriendStore.method3436(method4015(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))))
                     ? "&7" + method4015(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r удалён из друзей."
                     : "&7" + method4015(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length))) + "&r не в друзьях."
               );
               break;
            default:
               ClientChat.method2267("Использование: &7.friend add/remove/list/clear <ник>");
         }
      }
   }

   @Override
   public String getName() {
      return "friend";
   }

   @Override
   public String getDescription() {
      return "Друзья (.friend add/remove/list/clear <ник>)";
   }
}
