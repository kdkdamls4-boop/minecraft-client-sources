package deluxe.client.command;

import deluxe.client.util.ClientChat;

import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.util.Formatting;

public class CommandDispatcher {
   private final ArrayList<ClientCommand> field06262;
   private final String field06263;

   public ArrayList<String> method1895(String local2969_1) {
      ArrayList<String> local2969_2 = new ArrayList<>();
      if (!local2969_1.startsWith(this.field06263)) {
         return local2969_2;
      }

      String[] stringArray = local2969_1.substring(this.field06263.length()).split(" ", -1);
      if (stringArray.length == 1) {
         for (ClientCommand clientCommand : this.field06262) {
            if (clientCommand.getName().startsWith(stringArray[0].toLowerCase())) {
               local2969_2.add(this.field06263 + clientCommand.getName());
            }
         }

         return local2969_2;
      } else {
         for (ClientCommand clientCommand2 : this.field06262) {
            if (clientCommand2.getName().equalsIgnoreCase(stringArray[0])) {
               for (String local2969_7 : clientCommand2.suggestArguments(Arrays.copyOfRange(stringArray, 1, stringArray.length))) {
                  if (local2969_7.toLowerCase().startsWith(stringArray[stringArray.length - 1].toLowerCase())) {
                     local2969_2.add(local2969_7);
                  }
               }
            }
         }

         return local2969_2;
      }
   }

   public CommandDispatcher(ArrayList<ClientCommand> local2970_1, String local2970_2) {
      this.field06262 = local2970_1;
      this.field06263 = local2970_2;
   }

   public ArrayList<ClientCommand> method1896(String local2971_1) {
      ArrayList<ClientCommand> local2971_2 = new ArrayList<>();

      for (ClientCommand clientCommand : this.field06262) {
         if ((this.field06263 + clientCommand.getName()).startsWith(local2971_1) || local2971_1.startsWith(this.field06263 + clientCommand.getName() + " ")) {
            local2971_2.add(clientCommand);
         }
      }

      return local2971_2;
   }

   public ArrayList<ClientCommand> method1897() {
      return this.field06262;
   }

   public boolean method1898(String local2973_1) {
      if (!local2973_1.startsWith(this.field06263)) {
         return false;
      }

      String[] stringArray = local2973_1.substring(this.field06263.length()).trim().split(" +");

      for (ClientCommand clientCommand : this.field06262) {
         if (clientCommand.getName().equalsIgnoreCase(stringArray[0])) {
            clientCommand.execute(Arrays.copyOfRange(stringArray, 1, stringArray.length));
            return true;
         }
      }

      ClientChat.method2267("Неизвестная команда, введи " + Formatting.GRAY + this.field06263 + "help");
      return true;
   }

   public String method1899() {
      return this.field06263;
   }

   public String method1900(String local2975_1) {
      return this.method1895(local2975_1).isEmpty()
         ? local2975_1
         : local2975_1.substring(0, local2975_1.length() - local2975_1.substring(local2975_1.lastIndexOf(32) + 1).length())
            + this.method1895(local2975_1).get(0)
            + " ";
   }
}
