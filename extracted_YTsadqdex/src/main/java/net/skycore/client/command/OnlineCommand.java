package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.util.ReadBodyHelper;

public final class OnlineCommand extends ClientCommand {
   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command command = commandContext -> {
         ReadBodyHelper.fetchAsync(value -> {
            ChatMessages.displayInfo("Онлайн SkyCore: " + value);
         });
         return 1;
      };
      literalArgumentBuilder.executes(command);
   }

   public OnlineCommand() {
      super("online");
   }
}
