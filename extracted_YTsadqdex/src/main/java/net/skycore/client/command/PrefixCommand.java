package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;

public final class PrefixCommand extends ClientCommand {
   public PrefixCommand() {
      super("prefix");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      literalArgumentBuilder.executes(commandContext -> {
         CommandManager commandManager = SkycoreClient.commands();
         String prefix = commandManager.getPrefix();
         String text = "Current prefix: " + ChatFormatting.WHITE + prefix + ChatFormatting.GRAY + "  |  Usage: " + prefix + "prefix \"new\"";
         ChatMessages.displayInfo(text);
         return 1;
      });
      StringArgumentType stringArgumentType = StringArgumentType.string();
      RequiredArgumentBuilder requiredArgumentBuilder = arg("prefix", stringArgumentType);
      Command string = commandContext2 -> {
         String string2 = StringArgumentType.getString(commandContext2, "prefix");
         String text2 = CommandManager.validatePrefix(string2);
         if (text2 != null) {
            ChatMessages.displayInfo(text2);
            return 0;
         } else {
            CommandManager commandManager2 = SkycoreClient.commands();
            commandManager2.setPrefix(string2);
            String text3 = "Command prefix changed to " + ChatFormatting.WHITE + string2;
            ChatMessages.displayInfo(text3);
            return 1;
         }
      };
      literalArgumentBuilder.then(requiredArgumentBuilder.executes(string));
   }
}
