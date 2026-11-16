package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.integration.IrcClient;

public final class IrcCommand extends ClientCommand {
   public IrcCommand() {
      super("irc");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command command = commandContext -> {
         String text;
         if (IrcClient.isUserEnabled()) {
            text = ChatFormatting.GREEN + (IrcClient.isConnected() ? "онлайн" : "подключается...");
         } else {
            text = ChatFormatting.RED + "выключен";
         }

         ChatMessages.displayInfo("IRC " + text);
         String text2 = "Отправка: "
            + ChatFormatting.WHITE
            + "$сообщение"
            + ChatFormatting.GRAY
            + " или .irc <текст> | выкл: "
            + ChatFormatting.WHITE
            + ".irc off";
         ChatMessages.displayInfo(text2);
         return 1;
      };
      literalArgumentBuilder.executes(command);
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("off");
      Command command2 = commandContext2 -> {
         IrcClient.turnOff();
         return 1;
      };
      literalArgumentBuilder.then(literalArgumentBuilder2.executes(command2));
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("on");
      Command command3 = commandContext3 -> {
         IrcClient.turnOn();
         return 1;
      };
      literalArgumentBuilder.then(literalArgumentBuilder3.executes(command3));
      RequiredArgumentBuilder requiredArgumentBuilder = arg("message", StringArgumentType.greedyString());
      Command string = commandContext4 -> {
         String string2 = StringArgumentType.getString(commandContext4, "message");
         String text3 = string2.trim();
         if ("off".equalsIgnoreCase(text3)) {
            IrcClient.turnOff();
            return 1;
         } else {
            text3 = string2.trim();
            if ("on".equalsIgnoreCase(text3)) {
               IrcClient.turnOn();
               return 1;
            } else if (!IrcClient.isUserEnabled()) {
               ChatMessages.displayInfo("IRC выключен. Напиши .irc on");
               return 0;
            } else {
               IrcClient.sendMessage(string2);
               return 1;
            }
         }
      };
      ArgumentBuilder argumentBuilder = requiredArgumentBuilder.executes(string);
      literalArgumentBuilder.then(argumentBuilder);
   }
}
