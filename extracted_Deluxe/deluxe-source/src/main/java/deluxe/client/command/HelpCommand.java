package deluxe.client.command;

import deluxe.client.util.ClientChat;

import java.util.ArrayList;

public class HelpCommand implements ClientCommand {
   private final ArrayList<ClientCommand> field03551;

   public HelpCommand(ArrayList<ClientCommand> local1784_1) {
      this.field03551 = local1784_1;
   }

   @Override
   public void execute(String[] stringArray) {
      for (ClientCommand clientCommand : this.field03551) {
         if (stringArray.length > 0 && clientCommand.getName().equalsIgnoreCase(stringArray[0])) {
            ClientChat.method2269("&f." + clientCommand.getName() + ":", "&f" + clientCommand.getDescription());
            return;
         }
      }

      String[] stringArray2 = new String[this.field03551.size()];

      for (int index = 0; index < this.field03551.size(); index++) {
         stringArray2[index] = "&7." + this.field03551.get(index).getName() + " &8— &f" + this.field03551.get(index).getDescription();
      }

      ClientChat.method2269("&fКоманды клиента:", stringArray2);
   }

   @Override
   public String getDescription() {
      return "Показывает список всех команд";
   }

   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      ArrayList<String> local1787_2 = new ArrayList<>();
      if (stringArray.length <= 1) {
         for (ClientCommand clientCommand : this.field03551) {
            local1787_2.add(clientCommand.getName());
         }
      }

      return local1787_2;
   }

   @Override
   public String getName() {
      return "help";
   }
}
