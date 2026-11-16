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
import net.skycore.client.config.NameList;

public final class StaffNameArgument implements ArgumentType<String> {
   @Override
   public Collection<String> getExamples() {
      return List.of();
   }

   public String parse(StringReader stringReader) throws CommandSyntaxException {
      return stringReader.readUnquotedString();
   }

   public static StaffNameArgument create() {
      return new StaffNameArgument();
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      NameList elements = SkycoreClient.staff();
      return SharedSuggestionProvider.suggest(elements.getItems(), suggestionsBuilder);
   }
}
