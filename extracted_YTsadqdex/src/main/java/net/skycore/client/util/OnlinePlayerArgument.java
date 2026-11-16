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
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;

public final class OnlinePlayerArgument implements ArgumentType<String> {
   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      if (connection == null) {
         return Suggestions.empty();
      } else {
         var onlinePlayers = connection.getOnlinePlayers();
         var stream = onlinePlayers.stream();
         Function<net.minecraft.client.multiplayer.PlayerInfo, java.lang.String> profile = playerInfo -> {
            return playerInfo.getProfile().name();
         };
         return SharedSuggestionProvider.suggest(stream.map(profile), suggestionsBuilder);
      }
   }

   public String parse(StringReader stringReader) throws CommandSyntaxException {
      return stringReader.readUnquotedString();
   }

   @Override
   public Collection<String> getExamples() {
      return List.of("Steve", "Alex");
   }

   public static OnlinePlayerArgument create() {
      return new OnlinePlayerArgument();
   }
}
