package deluxe.client.command;

import deluxe.client.util.WaypointManager;

import deluxe.client.util.ClientChat;
import deluxe.client.command.ClientCommand;
import deluxe.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.Arrays;

public class WaypointCommand implements MinecraftAccess, ClientCommand {
   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      if (stringArray.length <= 1) {
         return new ArrayList<>(Arrays.asList("add", "del", "clear"));
      } else if (stringArray.length != 2 || !stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove")) {
         return stringArray.length == 2 && stringArray[0].equalsIgnoreCase("add") && mc.player != null
            ? new ArrayList<>(Arrays.asList(mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()))
            : new ArrayList<>();
      } else {
         return WaypointManager.method0947().method0946();
      }
   }

   @Override
   public String getName() {
      return "way";
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length == 0) {
         ClientChat.method2267("Использование: &7.way add [название] <x y z>&r, &7.way del <название>&r, &7.way clear");
      } else if (stringArray[0].equalsIgnoreCase("clear")) {
         ClientChat.method2267(
            WaypointManager.method0947().method0952() == 0 ? "Меток нет." : "Удалено меток: &7" + WaypointManager.method0947().method0952()
         );
         WaypointManager.method0947().method0945();
      } else if (!stringArray[0].equalsIgnoreCase("del") && !stringArray[0].equalsIgnoreCase("remove")) {
         if (!stringArray[0].equalsIgnoreCase("add")) {
            ClientChat.method2267("Использование: &7.way add [название] <x y z>&r, &7.way del <название>&r, &7.way clear");
         } else if (stringArray.length == 1 && mc.player != null) {
            WaypointManager.method0947().method0949("Метка", mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
            ClientChat.method2267(
               "Метка &7Метка&r поставлена на &7" + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ()
            );
         } else if (stringArray.length != 4 && stringArray.length != 5) {
            ClientChat.method2267("Использование: &7.way add [название] <x y z>");
         } else {
            try {
               WaypointManager.method0947()
                  .method0949(
                     stringArray.length == 5 ? stringArray[1] : "Метка",
                     Integer.parseInt(stringArray[stringArray.length - 3]),
                     Integer.parseInt(stringArray[stringArray.length - 2]),
                     Integer.parseInt(stringArray[stringArray.length - 1])
                  );
               ClientChat.method2267(
                  "Метка &7"
                     + (stringArray.length == 5 ? stringArray[1] : "Метка")
                     + "&r поставлена на &7"
                     + stringArray[stringArray.length - 3]
                     + " "
                     + stringArray[stringArray.length - 2]
                     + " "
                     + stringArray[stringArray.length - 1]
               );
            } catch (NumberFormatException exception) {
               ClientChat.method2267("Координаты должны быть целыми числами.");
            }
         }
      } else if (stringArray.length < 2) {
         ClientChat.method2267("Использование: &7.way del <название>");
      } else {
         ClientChat.method2267(
            WaypointManager.method0947().method0948(stringArray[1])
               ? "Метка &7" + stringArray[1] + "&r удалена."
               : "Метка &7" + stringArray[1] + "&r не найдена."
         );
      }
   }

   @Override
   public String getDescription() {
      return "Метки (.way add [название] <x y z> / del <название> / clear)";
   }
}
