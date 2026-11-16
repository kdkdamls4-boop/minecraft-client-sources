package net.skycore.client.util;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.player.Player;

public final class WorldPlayerArgument implements ArgumentType<String> {
   private final Supplier<List<Player>> players;

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      var players2 = this.players.get();
      if (players2 != null && !players2.isEmpty()) {
         var gameProfile = players2.stream().map(player -> {
            GameProfile gameProfile2 = player.getGameProfile();
            return gameProfile2.name();
         });
         return SharedSuggestionProvider.suggest(gameProfile, suggestionsBuilder);
      } else {
         return Suggestions.empty();
      }
   }

   public static WorldPlayerArgument create(List<Player> players2) {
      Supplier<List<Player>> supplier = () -> {
         return players2;
      };
      return new WorldPlayerArgument(supplier);
   }

   @Override
   public Collection<String> getExamples() {
      return List.of("FakePlayer1");
   }

   private WorldPlayerArgument(Supplier<List<Player>> players2) {
      this.players = players2;
   }

   public String parse(StringReader stringReader) throws CommandSyntaxException {
      return stringReader.readUnquotedString();
   }
}
