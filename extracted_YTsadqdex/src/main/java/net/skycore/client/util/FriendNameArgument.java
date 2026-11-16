package net.skycore.client.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.skycore.client.config.FriendManager;

public final class FriendNameArgument implements ArgumentType<String> {
   private static final DynamicCommandExceptionType NOT_FRIEND = new DynamicCommandExceptionType(value -> {
      return Component.literal("No friend named " + value);
   });

   @Override
   public Collection<String> getExamples() {
      return List.of();
   }

   public static FriendNameArgument create() {
      return new FriendNameArgument();
   }

   public String parse(StringReader stringReader) throws CommandSyntaxException {
      String text = stringReader.readUnquotedString();
      if (!FriendManager.INSTANCE.isFriend(text)) {
         throw NOT_FRIEND.create(text);
      } else {
         return text;
      }
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      var friends = FriendManager.INSTANCE.getFriends();
      return SharedSuggestionProvider.suggest(friends, suggestionsBuilder);
   }
}
