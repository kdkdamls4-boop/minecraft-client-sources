package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class TpCommand extends ClientCommand {
   public TpCommand() {
      super("tp");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command command = commandContext -> {
         ChatMessages.displayInfo(".tp requires TpExploit/TpGrim (not available). Use .vclip / .hclip");
         return 1;
      };
      literalArgumentBuilder.executes(command);
   }
}
