package tokyo.ghoul.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.module.Module;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

public class StrictlyModuleArgument implements ArgumentType<String> {
    public static StrictlyModuleArgument create() {
        return new StrictlyModuleArgument();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String module = reader.readString();
        if (findModule(module) == null) {
            throw new DynamicCommandExceptionType(
                    name -> Component.literal("Модуль " + name + " не найден")
            ).create(module);
        }
        return module;
    }

    public static Module findModule(String name) {
        return ModuleManager.getInstance().getModules().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(
                ModuleManager.getInstance().getModules().stream()
                        .filter(m -> !m.isHidden())
                        .map(Module::getName)
                        .collect(Collectors.toList()),
                builder
        );
    }

    @Override
    public Collection<String> getExamples() {
        List<String> examples = ModuleManager.getInstance().getModules().stream()
                .map(Module::getName).limit(5).collect(Collectors.toList());
        return examples.isEmpty() ? List.of("Aura", "Speed") : examples;
    }
}
