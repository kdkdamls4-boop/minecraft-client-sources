package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.MacroManager;
import net.skycore.client.module.KeyBinds;

public final class MacroCommand extends ClientCommand {
   public MacroCommand() {
      super("macro");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      RequiredArgumentBuilder requiredArgumentBuilder = arg("name", StringArgumentType.word());
      RequiredArgumentBuilder requiredArgumentBuilder2 = arg("bind", StringArgumentType.word());
      StringArgumentType stringArgumentType = StringArgumentType.greedyString();
      RequiredArgumentBuilder requiredArgumentBuilder3 = arg("text", stringArgumentType);
      Command string = commandContext -> {
         String string2 = StringArgumentType.getString(commandContext, "name");
         String string3 = StringArgumentType.getString(commandContext, "bind");
         String string4 = StringArgumentType.getString(commandContext, "text");
         int value = KeyBinds.parse(string3);
         if (value < 0) {
            String text = String.valueOf(ChatFormatting.WHITE);
            String text2 = string3.toUpperCase(Locale.ROOT);
            String text3 = String.valueOf(ChatFormatting.GRAY);
            String text4 = "Key " + text + text2 + text3 + " not found";
            ChatMessages.displayInfo(text4);
            return 1;
         } else {
            MacroManager macroManager = SkycoreClient.macros();
            macroManager.addMacro(string2, value, string4);
            String text5 = String.valueOf(ChatFormatting.WHITE);
            String text6 = String.valueOf(ChatFormatting.GRAY);
            String text7 = String.valueOf(ChatFormatting.WHITE);
            String text8 = KeyBinds.name(value);
            ChatMessages.displayInfo("Macro " + text5 + string2 + text6 + " on key " + text7 + text8 + ChatFormatting.GRAY + " added");
            return 1;
         }
      };
      ArgumentBuilder argumentBuilder = requiredArgumentBuilder.then(requiredArgumentBuilder2.then(requiredArgumentBuilder3.executes(string)));
      literalArgumentBuilder.then(literalArgumentBuilder2.then(argumentBuilder));
      literalArgumentBuilder2 = literal("remove");
      StringArgumentType stringArgumentType2 = StringArgumentType.word();
      argumentBuilder = arg("name", stringArgumentType2);
      argumentBuilder = argumentBuilder.executes(commandContext2 -> {
         String string5 = StringArgumentType.getString(commandContext2, "name");
         MacroManager macroManager2 = SkycoreClient.macros();
         var macros = macroManager2.getMacros();
         var stream = macros.stream();
         boolean condition = stream.anyMatch(macro -> {
            return macro.name().equalsIgnoreCase(string5);
         });
         if (!condition) {
            String text9 = "Macro " + ChatFormatting.WHITE + string5 + ChatFormatting.GRAY + " not found";
            ChatMessages.displayInfo(text9);
            return 1;
         } else {
            SkycoreClient.macros().removeMacro(string5);
            String text10 = String.valueOf(ChatFormatting.WHITE);
            ChatMessages.displayInfo("Macro " + text10 + string5 + ChatFormatting.GRAY + " removed");
            return 1;
         }
      });
      literalArgumentBuilder.then(literalArgumentBuilder2.then(argumentBuilder));
      literalArgumentBuilder2 = literal("list");
      Command macros2 = commandContext3 -> {
         var macros3 = SkycoreClient.macros().getMacros();
         if (macros3.isEmpty()) {
            ChatMessages.displayInfo("No macros");
            return 1;
         }

         StringBuilder stringBuilder = new StringBuilder();
         Iterator iterator = macros3.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               String text11 = stringBuilder.toString();
               ChatMessages.displayInfo(text11);
               return 1;
            }

            MacroManager.Macro element = (MacroManager.Macro)iterator.next();
            StringBuilder stringBuilder2 = stringBuilder.append("\n");
            StringBuilder stringBuilder3 = stringBuilder2.append(ChatFormatting.GRAY)
               .append(element.name())
               .append(ChatFormatting.WHITE)
               .append(" [")
               .append(KeyBinds.name(element.keyCode()));
            StringBuilder stringBuilder4 = stringBuilder3.append("] ");
            StringBuilder stringBuilder5 = stringBuilder4.append(ChatFormatting.GREEN);
            String text12 = element.text();
            stringBuilder5.append(text12);
         }
      };
      ArgumentBuilder argumentBuilder2 = literalArgumentBuilder2.executes(macros2);
      literalArgumentBuilder.then(argumentBuilder2);
      argumentBuilder2 = literal("clear");
      Command macros4 = commandContext4 -> {
         MacroManager macroManager3 = SkycoreClient.macros();
         var macros5 = macroManager3.getMacros();
         if (macros5.isEmpty()) {
            ChatMessages.displayInfo("No macros");
            return 1;
         } else {
            MacroManager macroManager4 = SkycoreClient.macros();
            macroManager4.clear();
            ChatMessages.displayInfo("Macro list cleared");
            return 1;
         }
      };
      literalArgumentBuilder.then(argumentBuilder2.executes(macros4));
   }

   @Override
   public void register(CommandDispatcher<ClientSuggestionProvider> commandDispatcher) {
      Iterator iterator = List.of("macro", "mac").iterator();

      while (iterator.hasNext()) {
         String element = (String)iterator.next();
         LiteralArgumentBuilder literalArgumentBuilder = LiteralArgumentBuilder.literal(element);
         this.execute(literalArgumentBuilder);
         commandDispatcher.register(literalArgumentBuilder);
      }
   }
}
