package net.skycore.client.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Collection;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.SharedSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.NameList;
import net.skycore.client.util.OnlinePlayerArgument;

public final class CustomnameCommand extends ClientCommand {
   public CustomnameCommand() {
      super("customname");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("remove");
      RequiredArgumentBuilder requiredArgumentBuilder = arg("player", StringArgumentType.word());
      SuggestionProvider items = (commandContext, suggestionsBuilder) -> {
         NameList elements = SkycoreClient.customNames();
         return SharedSuggestionProvider.suggest(elements.getItems(), suggestionsBuilder);
      };
      ArgumentBuilder string = literalArgumentBuilder2.then(requiredArgumentBuilder.suggests(items).executes(commandContext2 -> {
         String string2 = StringArgumentType.getString(commandContext2, "player");
         NameList elements2 = SkycoreClient.customNames();
         elements2.remove(string2);
         String text = String.valueOf(ChatFormatting.WHITE);
         String text2 = String.valueOf(ChatFormatting.GRAY);
         ChatMessages.displayInfo("Custom nickname for " + text + string2 + text2 + " removed");
         return 1;
      }));
      literalArgumentBuilder.then(string);
      literalArgumentBuilder.then(literal("clear").executes(commandContext3 -> {
         SkycoreClient.customNames().clear();
         ChatMessages.displayInfo("All custom nicknames removed");
         return 1;
      }));
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("list");
      ArgumentBuilder items2 = literalArgumentBuilder3.executes(commandContext4 -> {
         var items3 = SkycoreClient.customNames().getItems();
         String text3;
         if (items3.isEmpty()) {
            text3 = "No custom nicknames";
         } else {
            text3 = items3.toString();
         }

         ChatMessages.displayInfo(text3);
         return 1;
      });
      literalArgumentBuilder.then(items2);
      RequiredArgumentBuilder requiredArgumentBuilder2 = arg("player", OnlinePlayerArgument.create());
      requiredArgumentBuilder = arg("displayName", StringArgumentType.greedyString());
      literalArgumentBuilder.then(requiredArgumentBuilder2.then(requiredArgumentBuilder.executes(commandContext5 -> {
         String argument = (String)commandContext5.getArgument("player", String.class);
         String string3 = StringArgumentType.getString(commandContext5, "displayName");
         SkycoreClient.customNames().set(argument, string3);
         String text4 = String.valueOf(ChatFormatting.WHITE);
         String text5 = String.valueOf(ChatFormatting.GRAY);
         String text6 = String.valueOf(ChatFormatting.WHITE);
         ChatMessages.displayInfo("For " + text4 + argument + text5 + " nickname set to " + text6 + string3);
         return 1;
      })));
   }
}
