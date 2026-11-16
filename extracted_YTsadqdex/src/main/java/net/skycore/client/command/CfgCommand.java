package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.ConfigManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.util.ConfigProfileArgument;

public final class CfgCommand extends ClientCommand {
   @Override
   public void register(CommandDispatcher<ClientSuggestionProvider> commandDispatcher) {
      LiteralArgumentBuilder command = LiteralArgumentBuilder.literal(this.getCommand());
      this.execute(command);
      commandDispatcher.register(command);
      LiteralArgumentBuilder literalArgumentBuilder = LiteralArgumentBuilder.literal("config");
      this.execute(literalArgumentBuilder);
      commandDispatcher.register(literalArgumentBuilder);
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("save");
      StringArgumentType stringArgumentType = StringArgumentType.word();
      RequiredArgumentBuilder requiredArgumentBuilder = arg("name", stringArgumentType);
      SuggestionProvider suggestionProvider = (commandContext, suggestionsBuilder) -> ConfigProfileArgument.create().listSuggestions(commandContext, suggestionsBuilder);
      requiredArgumentBuilder = requiredArgumentBuilder.suggests(suggestionProvider);
      Command string = commandContext2 -> {
         String string2 = StringArgumentType.getString(commandContext2, "name");
         ConfigManager configManager = SkycoreClient.config();
         if (configManager.saveProfile(string2)) {
            String text = "Config \"" + string2 + "\" saved!";
            ChatMessages.displayInfo(text);
            return 1;
         } else {
            String text2 = "Could not save \"" + string2 + "\"";
            ChatMessages.displayError(text2);
            return 0;
         }
      };
      ArgumentBuilder argumentBuilder = literalArgumentBuilder2.then(requiredArgumentBuilder.executes(string));
      literalArgumentBuilder.then(argumentBuilder);
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("load");
      requiredArgumentBuilder = arg("name", ConfigProfileArgument.create());
      Command argument = commandContext3 -> {
         String argument2 = (String)commandContext3.getArgument("name", String.class);
         ConfigManager configManager2 = SkycoreClient.config();
         if (!configManager2.profileExists(argument2)) {
            ChatMessages.displayInfo("Config \"" + argument2 + "\" not found.");
            return 0;
         } else if (SkycoreClient.config().loadProfile(argument2)) {
            String text3 = "Config \"" + argument2 + "\" loaded!";
            ChatMessages.displayInfo(text3);
            return 1;
         } else {
            String text4 = "Could not load \"" + argument2 + "\"";
            ChatMessages.displayError(text4);
            return 0;
         }
      };
      ArgumentBuilder argumentBuilder2 = requiredArgumentBuilder.executes(argument);
      ArgumentBuilder argumentBuilder3 = literalArgumentBuilder3.then(argumentBuilder2);
      literalArgumentBuilder.then(argumentBuilder3);
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("remove");
      ArgumentBuilder argument3 = literalArgumentBuilder4.then(arg("name", ConfigProfileArgument.create()).executes(commandContext4 -> {
         String argument4 = (String)commandContext4.getArgument("name", String.class);
         ConfigManager configManager3 = SkycoreClient.config();
         if (configManager3.deleteProfile(argument4)) {
            String text5 = "Config \"" + argument4 + "\" deleted!";
            ChatMessages.displayInfo(text5);
         } else {
            String text6 = "Config \"" + argument4 + "\" not found.";
            ChatMessages.displayInfo(text6);
         }

         return 1;
      }));
      literalArgumentBuilder.then(argument3);
      literalArgumentBuilder.then(literal("list").executes(commandContext5 -> {
         var strings = SkycoreClient.config().listProfiles();
         if (strings.isEmpty()) {
            ChatMessages.displayInfo("No configurations found!");
         } else {
            String text7 = String.join(", ", strings);
            text7 = "Configs: " + text7;
            ChatMessages.displayInfo(text7);
         }

         return 1;
      }));
      LiteralArgumentBuilder literalArgumentBuilder5 = literal("dir");
      Command profilesDirectory = commandContext6 -> {
         try {
            Path profilesDirectory2 = SkycoreClient.config().getProfilesDirectory();
            FileAttribute[] fileAttributeValues = new FileAttribute[0];
            Files.createDirectories(profilesDirectory2, fileAttributeValues);
            Path path = profilesDirectory2.toAbsolutePath().normalize();
            File file = path.toFile();
            openDirectory(file);
            String text8 = String.valueOf(ChatFormatting.GRAY);
            String text9 = String.valueOf(ChatFormatting.WHITE);
            ChatMessages.displayInfo(text8 + "Folder: " + text9 + file.getAbsolutePath());
         } catch (Exception failure) {
            String message = failure.getMessage();
            ChatMessages.displayError("Could not open folder: " + message);
         }

         return 1;
      };
      literalArgumentBuilder.then(literalArgumentBuilder5.executes(profilesDirectory));
      ArgumentBuilder modules = literal("reset").executes(commandContext7 -> {
         int value = 0;
         var modules2 = SkycoreClient.modules().getModules();

         for (ClientModule clientModule : modules2) {
            if (clientModule.isEnabled()) {
               clientModule.setEnabled(false);
               value++;
            }
         }

         SkycoreClient.config().save();
         ChatMessages.displayInfo("Disabled " + value + " modules and saved current config");
         return 1;
      });
      literalArgumentBuilder.then(modules);
      Command command = commandContext8 -> {
         ChatMessages.displayInfo("Usage: .cfg save/load/remove/list/dir/reset");
         return 1;
      };
      literalArgumentBuilder.executes(command);
   }

   public CfgCommand() {
      super("cfg");
   }

   private static void openDirectory(File file) throws Exception {
      if (!file.exists() && !file.mkdirs()) {
         throw new IllegalStateException("could not create " + file);
      }

      String property = System.getProperty("os.name", "").toLowerCase();
      if (property.contains("win")) {
         new ProcessBuilder("explorer.exe", file.getAbsolutePath()).start();
      } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN)) {
         Desktop.getDesktop().open(file);
      } else if (property.contains("mac")) {
         new ProcessBuilder("open", file.getAbsolutePath()).start();
      } else {
         new ProcessBuilder("xdg-open", file.getAbsolutePath()).start();
      }
   }
}
