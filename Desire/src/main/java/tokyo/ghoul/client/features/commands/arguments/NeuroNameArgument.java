package tokyo.ghoul.client.features.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class NeuroNameArgument implements ArgumentType<String> {
    private final Supplier<List<String>> names;

    public NeuroNameArgument(Supplier<List<String>> names) {
        this.names = names;
    }

    @Override
    public String parse(StringReader r) {
        int start = r.getCursor();
        while (r.canRead() && !Character.isWhitespace(r.peek())) r.skip();
        return r.getString().substring(start, r.getCursor());
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();
        names.get().stream()
                .filter(name -> name.toLowerCase().startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
