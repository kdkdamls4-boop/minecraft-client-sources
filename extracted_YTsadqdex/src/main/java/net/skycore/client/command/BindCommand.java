package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.util.ModuleNameArgument;
import org.lwjgl.glfw.GLFW;

public final class BindCommand extends ClientCommand {
   private static int resolveKey(String text) {

      for (int value = 32; value <= 348; value++) {
         String text2 = GLFW.glfwGetKeyName(value, 0);
         if (text2 != null && text2.equalsIgnoreCase(text)) {
            return value;
         }
      }

      return -1;
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      Command command = commandContext -> {
         ChatMessages.displayInfo("Usage: .bind add <module> <key> | remove <module> | list | clear");
         return 1;
      };
      literalArgumentBuilder.executes(command);
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      ModuleNameArgument createHelper2 = ModuleNameArgument.create();
      RequiredArgumentBuilder requiredArgumentBuilder = arg("module", createHelper2);
      StringArgumentType stringArgumentType = StringArgumentType.word();
      RequiredArgumentBuilder requiredArgumentBuilder2 = arg("key", stringArgumentType);
      ArgumentBuilder argument = requiredArgumentBuilder2.executes(commandContext2 -> {
         ClientModule argument2 = ModuleNameArgument.resolve((String)commandContext2.getArgument("module", String.class));
         String string = StringArgumentType.getString(commandContext2, "key");
         String text = string.toUpperCase(Locale.ROOT);
         int value = resolveKey(text);
         if (argument2 == null) {
            ChatMessages.displayInfo("Module not found");
            return 0;
         } else if (value <= 0) {
            ChatMessages.displayInfo("Key " + text + " not found");
            return 0;
         } else {
            argument2.setKeyBind(value);
            String name = argument2.getName();
            ChatMessages.displayInfo("Bind " + text + " to " + name);
            return 1;
         }
      });
      ArgumentBuilder argumentBuilder = requiredArgumentBuilder.then(argument);
      ArgumentBuilder argumentBuilder2 = literalArgumentBuilder2.then(argumentBuilder);
      literalArgumentBuilder.then(argumentBuilder2);
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("remove");
      RequiredArgumentBuilder requiredArgumentBuilder3 = arg("module", ModuleNameArgument.create());
      Command argument3 = commandContext3 -> {
         String argument4 = (String)commandContext3.getArgument("module", String.class);
         ClientModule clientModule = ModuleNameArgument.resolve(argument4);
         if (clientModule == null) {
            ChatMessages.displayInfo("Module not found");
            return 0;
         } else {
            clientModule.setKeyBind(-1);
            String name2 = clientModule.getName();
            ChatMessages.displayInfo("Unbound " + name2);
            return 1;
         }
      };
      literalArgumentBuilder.then(literalArgumentBuilder3.then(requiredArgumentBuilder3.executes(argument3)));
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("list");
      literalArgumentBuilder.then(
         literalArgumentBuilder4.executes(
            commandContext4 -> {
               byte flag = 0;

               for (ClientModule clientModule2 : SkycoreClient.modules().getModules()) {
                  if (clientModule2.getKeyBind() > 0) {
                     flag = 1;
                     String keyBind = GLFW.glfwGetKeyName(clientModule2.getKeyBind(), 0);
                     ChatMessages.send(
                        Component.literal(clientModule2.getName())
                           .withStyle(ChatFormatting.WHITE)
                           .append(Component.literal(" — ").withStyle(ChatFormatting.GRAY))
                           .append(Component.literal(keyBind != null ? keyBind : String.valueOf(clientModule2.getKeyBind())).withStyle(ChatFormatting.WHITE))
                     );
                  }
               }

               if (flag == 0) {
                  ChatMessages.displayInfo("No keybinds");
               }

               return 1;
            }
         )
      );
      literalArgumentBuilder.then(literal("clear").executes(commandContext5 -> {
         ModuleManager moduleManager = SkycoreClient.modules();
         var modules = moduleManager.getModules();

         for (ClientModule clientModule3 : modules) {
            if (!(clientModule3 instanceof ClickGUIModule)) {
               clientModule3.setKeyBind(-1);
            }
         }

         ChatMessages.displayInfo("All keybinds cleared (ClickGUI preserved)");
         return 1;
      }));
   }

   public BindCommand() {
      super("bind");
   }
}
