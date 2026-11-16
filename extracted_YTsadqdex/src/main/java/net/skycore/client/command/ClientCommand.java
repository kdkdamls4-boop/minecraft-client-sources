package net.skycore.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public abstract class ClientCommand {
   private final String command;

   protected static <T> RequiredArgumentBuilder<ClientSuggestionProvider, T> arg(String text, ArgumentType<T> argumentType) {
      return RequiredArgumentBuilder.argument(text, argumentType);
   }

   protected ClientCommand(String command2) {
      this.command = command2;
   }

   protected static LiteralArgumentBuilder<ClientSuggestionProvider> literal(String text) {
      return LiteralArgumentBuilder.literal(text);
   }

   public abstract void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder);

   public String getCommand() {
      return this.command;
   }

   public void register(CommandDispatcher<ClientSuggestionProvider> commandDispatcher) {
      LiteralArgumentBuilder literalArgumentBuilder = LiteralArgumentBuilder.literal(this.command);
      this.execute(literalArgumentBuilder);
      commandDispatcher.register(literalArgumentBuilder);
   }
}
