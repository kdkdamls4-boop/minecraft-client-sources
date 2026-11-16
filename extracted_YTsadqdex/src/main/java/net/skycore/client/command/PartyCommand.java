package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.integration.PartyClient;

public final class PartyCommand extends ClientCommand {
   public PartyCommand() {
      super("party");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command partyName = commandContext -> {
         String text;
         if (PartyClient.hasParty()) {
            String text2 = String.valueOf(ChatFormatting.GREEN);
            String partyName2 = PartyClient.getPartyName();
            text = text2 + partyName2;
         } else {
            String text3 = String.valueOf(ChatFormatting.RED);
            text = text3 + "не в пати";
         }

         String text4 = "Party " + text;
         ChatMessages.displayInfo(text4);
         String text5 = ChatFormatting.GRAY + ".party create <название> | invite <ник> | accept | decline | leave | disband | kick <ник> | chat <текст>";
         ChatMessages.displayInfo(text5);
         return 1;
      };
      literalArgumentBuilder.executes(partyName);
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("create");
      RequiredArgumentBuilder requiredArgumentBuilder = arg("name", StringArgumentType.greedyString());
      literalArgumentBuilder.then(literalArgumentBuilder2.then(requiredArgumentBuilder.executes(commandContext2 -> {
         String string = StringArgumentType.getString(commandContext2, "name");
         PartyClient.create(string);
         return 1;
      })));
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("invite");
      StringArgumentType stringArgumentType = StringArgumentType.word();
      requiredArgumentBuilder = arg("player", stringArgumentType);
      literalArgumentBuilder.then(literalArgumentBuilder3.then(requiredArgumentBuilder.executes(commandContext3 -> {
         PartyClient.invite(StringArgumentType.getString(commandContext3, "player"));
         return 1;
      })));
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("accept");
      Command command = commandContext4 -> {
         PartyClient.accept();
         return 1;
      };
      ArgumentBuilder argumentBuilder = literalArgumentBuilder4.executes(command);
      literalArgumentBuilder.then(argumentBuilder);
      LiteralArgumentBuilder literalArgumentBuilder5 = literal("decline");
      Command command2 = commandContext5 -> {
         PartyClient.decline();
         return 1;
      };
      literalArgumentBuilder.then(literalArgumentBuilder5.executes(command2));
      LiteralArgumentBuilder literalArgumentBuilder6 = literal("leave");
      ArgumentBuilder argumentBuilder2 = literalArgumentBuilder6.executes(commandContext6 -> {
         PartyClient.leave();
         return 1;
      });
      literalArgumentBuilder.then(argumentBuilder2);
      LiteralArgumentBuilder literalArgumentBuilder7 = literal("disband");
      ArgumentBuilder argumentBuilder3 = literalArgumentBuilder7.executes(commandContext7 -> {
         PartyClient.disband();
         return 1;
      });
      literalArgumentBuilder.then(argumentBuilder3);
      LiteralArgumentBuilder literalArgumentBuilder8 = literal("kick");
      StringArgumentType stringArgumentType2 = StringArgumentType.word();
      requiredArgumentBuilder = arg("player", stringArgumentType2);
      ArgumentBuilder string2 = requiredArgumentBuilder.executes(commandContext8 -> {
         PartyClient.kick(StringArgumentType.getString(commandContext8, "player"));
         return 1;
      });
      ArgumentBuilder argumentBuilder4 = literalArgumentBuilder8.then(string2);
      literalArgumentBuilder.then(argumentBuilder4);
      LiteralArgumentBuilder literalArgumentBuilder9 = literal("chat");
      StringArgumentType stringArgumentType3 = StringArgumentType.greedyString();
      string2 = arg("message", stringArgumentType3).executes(commandContext9 -> {
         PartyClient.sendChat(StringArgumentType.getString(commandContext9, "message"));
         return 1;
      });
      literalArgumentBuilder.then(literalArgumentBuilder9.then(string2));
   }
}
