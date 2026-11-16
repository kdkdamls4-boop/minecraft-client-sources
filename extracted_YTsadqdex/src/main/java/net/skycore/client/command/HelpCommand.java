package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public final class HelpCommand extends ClientCommand {
   private final CommandManager manager;

   public HelpCommand(CommandManager manager2) {
      super("help");
      this.manager = manager2;
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command prefix = commandContext -> {
         String prefix2 = this.manager.getPrefix();
         String text = "Commands (" + prefix2 + "):";
         ChatMessages.displayInfo(text);
         var commands = this.manager.getCommands();
         Iterator iterator = commands.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return 1;
            }

            ClientCommand element = (ClientCommand)iterator.next();
            String text2 = String.valueOf(ChatFormatting.WHITE);
            String command = element.getCommand();
            String text3 = text2 + prefix2 + command;
            ChatMessages.displayInfo(text3);
         }
      };
      literalArgumentBuilder.executes(prefix);
   }
}
