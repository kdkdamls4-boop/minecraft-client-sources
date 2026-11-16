package net.skycore.client.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class NeuroCommand extends ClientCommand {
   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      literalArgumentBuilder.executes(commandContext -> {
         ChatMessages.displayInfo(".neuro requires the Neuro module (not available)");
         return 1;
      });
   }

   public NeuroCommand() {
      super("neuro");
   }
}
