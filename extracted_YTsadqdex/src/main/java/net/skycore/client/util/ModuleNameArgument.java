package net.skycore.client.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleManager;

public final class ModuleNameArgument implements ArgumentType<String> {
   private static final DynamicCommandExceptionType MISSING = new DynamicCommandExceptionType(value -> {
      String text = value + " does not exist.";
      return Component.literal(text);
   });

   public static ModuleNameArgument create() {
      return new ModuleNameArgument();
   }

   public String parse(StringReader stringReader) throws CommandSyntaxException {
      String text = stringReader.readUnquotedString();
      if (resolve(text) == null) {
         throw MISSING.create(text);
      } else {
         return text;
      }
   }

   @Override
   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
      ArrayList elements = new ArrayList();
      ModuleManager moduleManager = SkycoreClient.modules();
      Iterator iterator = moduleManager.getModules().iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return SharedSuggestionProvider.suggest(elements, suggestionsBuilder);
         }

         ClientModule element = (ClientModule)iterator.next();
         String name = element.getName();
         elements.add(name);
      }
   }

   public static ClientModule resolve(String text) {
      if (text == null) {
         return null;
      }

      String text2 = text.replaceAll("\\s+", "");

      for (ClientModule clientModule : SkycoreClient.modules().getModules()) {
         if (clientModule.getName().replaceAll("\\s+", "").equalsIgnoreCase(text2)) {
            return clientModule;
         }
      }

      return !"Aura".equalsIgnoreCase(text2) && !"AttackAura".equalsIgnoreCase(text2)
         ? SkycoreClient.modules().get(text2).orElse(null)
         : SkycoreClient.modules().get("AttackAura").orElse(null);
   }

   @Override
   public Collection<String> getExamples() {
      return List.of("AttackAura", "Sprint");
   }
}
