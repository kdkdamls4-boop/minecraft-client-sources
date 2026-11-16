package deluxe.client.command;

import deluxe.client.util.ClientChat;
import deluxe.client.module.misc.PartyModule;

import java.util.ArrayList;
import java.util.Arrays;

public class PartyCommand implements ClientCommand {
   @Override
   public ArrayList<String> suggestArguments(String[] stringArray) {
      return stringArray.length <= 1 ? new ArrayList<>(Arrays.asList("create", "join", "leave", "lock", "unlock", "kick", "list")) : new ArrayList<>();
   }

   @Override
   public String getName() {
      return "party";
   }

   @Override
   public String getDescription() {
      return "Пати (.party create/join <код>/leave/lock/unlock/kick <ник>/list)";
   }

   @Override
   public void execute(String[] stringArray) {
      PartyModule partyModule = PartyModule.method2968();
      switch (stringArray.length == 0 ? "" : stringArray[0].toLowerCase()) {
         case "create":
            partyModule.method2976();
            break;
         case "join":
            if (stringArray.length < 2) {
               ClientChat.method2267("Использование: &7.party join <код>");
            } else {
               partyModule.method2952(stringArray[1]);
            }
            break;
         case "leave":
            partyModule.method2951();
            break;
         case "lock":
            partyModule.method2961(true);
            break;
         case "unlock":
            partyModule.method2961(false);
            break;
         case "kick":
            if (stringArray.length < 2) {
               ClientChat.method2267("Использование: &7.party kick <ник>");
            } else {
               partyModule.method2940(stringArray[1]);
            }
            break;
         default:
            partyModule.method2964();
      }
   }
}
