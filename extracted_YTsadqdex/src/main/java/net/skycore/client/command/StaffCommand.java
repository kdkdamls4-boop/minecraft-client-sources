package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.NameList;
import net.skycore.client.util.StaffNameArgument;
import net.skycore.client.util.OnlinePlayerArgument;

public final class StaffCommand extends ClientCommand {
   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      literalArgumentBuilder.then(literalArgumentBuilder2.then(arg("player", OnlinePlayerArgument.create()).executes(commandContext -> {
         String argument = (String)commandContext.getArgument("player", String.class);
         NameList elements = SkycoreClient.staff();
         if (elements.contains(argument)) {
            ChatMessages.displayInfo("Already added " + argument);
         } else {
            SkycoreClient.staff().add(argument);
            String text = "Added " + argument;
            ChatMessages.displayInfo(text);
         }

         return 1;
      })));
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("remove");
      RequiredArgumentBuilder requiredArgumentBuilder = arg("player", StaffNameArgument.create());
      Command argument2 = commandContext2 -> {
         String argument3 = (String)commandContext2.getArgument("player", String.class);
         NameList elements2 = SkycoreClient.staff();
         elements2.remove(argument3);
         String text2 = argument3 + " removed from staff";
         ChatMessages.displayInfo(text2);
         return 1;
      };
      ArgumentBuilder argumentBuilder = requiredArgumentBuilder.executes(argument2);
      ArgumentBuilder argumentBuilder2 = literalArgumentBuilder3.then(argumentBuilder);
      literalArgumentBuilder.then(argumentBuilder2);
      ArgumentBuilder items = literal("list").executes(commandContext3 -> {
         NameList elements3 = SkycoreClient.staff();
         var items2 = elements3.getItems();
         String text3;
         if (items2.isEmpty()) {
            text3 = "Staff list is empty";
         } else {
            text3 = "Staff: " + String.join(", ", items2);
         }

         ChatMessages.displayInfo(text3);
         return 1;
      });
      literalArgumentBuilder.then(items);
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("clear");
      literalArgumentBuilder.then(literalArgumentBuilder4.executes(commandContext4 -> {
         NameList elements4 = SkycoreClient.staff();
         elements4.clear();
         ChatMessages.displayInfo("Staff list cleared");
         return 1;
      }));
   }

   public StaffCommand() {
      super("staff");
   }
}
