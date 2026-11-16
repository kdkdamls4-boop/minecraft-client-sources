package deluxe.client.command;

import deluxe.client.util.ClientChat;
import deluxe.client.storage.StaffStore;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;

public class StaffCommand implements ClientCommand {
   @Override
   public String getName() {
      return "staff";
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("name", "prefix", "del", "list", "clear"));
      }

      if (stringArray.length != 2) {
         return new ArrayList<>();
      }

      if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove") && !stringArray[0].equalsIgnoreCase("delete")) {
         ArrayList<String> local7032_2 = new ArrayList<>();
         if (MinecraftClient.getInstance().getNetworkHandler() != null && MinecraftClient.getInstance().world != null) {
            for (PlayerListEntry playerListEntry : MinecraftClient.getInstance().getNetworkHandler().getPlayerList()) {
               if (stringArray[0].equalsIgnoreCase("name") && !StaffStore.method4328(playerListEntry.getProfile().name())) {
                  local7032_2.add(playerListEntry.getProfile().name());
               }

               if (stringArray[0].equalsIgnoreCase("prefix")) {
                  Team team = MinecraftClient.getInstance().world.getScoreboard().getScoreHolderTeam(playerListEntry.getProfile().name());
                  if (team != null
                     && !team.getPrefix().getString().trim().isEmpty()
                     && !local7032_2.contains(team.getPrefix().getString().trim())) {
                     local7032_2.add(team.getPrefix().getString().trim());
                  }
               }
            }

            return local7032_2;
         } else {
            return local7032_2;
         }
      } else {
         ArrayList<String> local7032_2 = StaffStore.method4327();
         local7032_2.addAll(StaffStore.method4326());
         return local7032_2;
      }
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length != 0 && !stringArray[0].equalsIgnoreCase("list")) {
         if (stringArray[0].equalsIgnoreCase("clear")) {
            StaffStore.method4329();
            ClientChat.method2267("Список своих стаффов очищен.");
         } else if (stringArray.length < 2) {
            ClientChat.method2267(
               "Использование: &7.staff name <ник>&r, &7.staff prefix <префикс>&r, &7.staff del <ник или префикс>&r, &7.staff list&r, &7.staff clear"
            );
         } else {
            switch (stringArray[0].toLowerCase()) {
               case "name":
               case "nick":
                  ClientChat.method2267(
                     StaffStore.method4323(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                        ? "Ник &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r теперь всегда в стафф-листе."
                        : "Ник &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r уже добавлен."
                  );
                  break;
               case "prefix":
               case "tag":
                  ClientChat.method2267(
                     StaffStore.method4332(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                        ? "Префикс &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r теперь считается стаффским."
                        : "Префикс &7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r уже добавлен."
                  );
                  break;
               case "del":
               case "remove":
               case "delete":
                  ClientChat.method2267(
                     StaffStore.method4331(String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)))
                        ? "&7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r убран из своих стаффов."
                        : "&7" + String.join(" ", Arrays.copyOfRange(stringArray, 1, stringArray.length)) + "&r не найден."
                  );
                  break;
               default:
                  ClientChat.method2267(
                     "Использование: &7.staff name <ник>&r, &7.staff prefix <префикс>&r, &7.staff del <ник или префикс>&r, &7.staff list&r, &7.staff clear"
                  );
            }
         }
      } else if (StaffStore.method4327().isEmpty() && StaffStore.method4326().isEmpty()) {
         ClientChat.method2267("Своих стаффов пока нет. Пример: &7.staff name Spa1k1&r, &7.staff prefix ГМ");
      } else {
         ClientChat.method2269(
            "&fСвои стаффы:",
            "Ники: &7" + (StaffStore.method4327().isEmpty() ? "нет" : String.join(", ", StaffStore.method4327())),
            "Префиксы: &7" + (StaffStore.method4326().isEmpty() ? "нет" : String.join(", ", StaffStore.method4326()))
         );
      }
   }

   @Override
   public String getDescription() {
      return "Свои стаффы (.staff name <ник> / prefix <префикс> / del <ник или префикс> / list / clear)";
   }
}
