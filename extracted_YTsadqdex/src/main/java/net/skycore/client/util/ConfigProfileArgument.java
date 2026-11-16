package net.skycore.client.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.ConfigManager;

public final class ConfigProfileArgument implements ArgumentType<String> {
   @Override
   public Collection<String> getExamples() {
      return List.of("default", "pvp");
   }

   public String parse(StringReader stringReader) throws CommandSyntaxException {
      return stringReader.readUnquotedString();
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      if (SkycoreClient.config() == null) {
         return Suggestions.empty();
      } else {
         ConfigManager configManager = SkycoreClient.config();
         return SharedSuggestionProvider.suggest(configManager.listProfiles(), suggestionsBuilder);
      }
   }

   public static ConfigProfileArgument create() {
      return new ConfigProfileArgument();
   }
}
