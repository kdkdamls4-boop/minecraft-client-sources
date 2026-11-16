package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.module.render.ViewModelModule;

public final class ViewmodelCommand extends ClientCommand {
   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command command = commandContext -> {
         ViewModelModule.openWorldEditor(true);
         return 1;
      };
      literalArgumentBuilder.executes(command);
   }

   public ViewmodelCommand(String text) {
      super(text);
   }

   public ViewmodelCommand() {
      this("viewmodel");
   }
}
