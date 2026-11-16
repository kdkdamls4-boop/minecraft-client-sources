package deluxe.client.command;

import deluxe.client.util.ClientChat;
import deluxe.client.command.ClientCommand;
import deluxe.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.util.Util;

public class ResourcePackCommand implements MinecraftAccess, ClientCommand {
   @Override
   public String getName() {
      return "rp";
   }

   @Override
   public String getDescription() {
      return "Открывает папку с ресурспаками (.rp dir)";
   }

   @Override
   public void execute(String[] stringArray) {
      if (stringArray.length > 0 && stringArray[0].equalsIgnoreCase("dir")) {
         Util.getOperatingSystem().open(mc.getResourcePackDir());
         ClientChat.method2267("Папка ресурспаков открыта.");
      } else {
         ClientChat.method2267("Использование: &7.rp dir");
      }
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      return stringArray.length <= 1 ? new ArrayList<>(Arrays.asList("dir")) : new ArrayList<>();
   }
}
