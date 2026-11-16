package tokyo.ghoul.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import tokyo.ghoul.api.command.Command;
import tokyo.ghoul.api.command.CommandRegister;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroManager;
import tokyo.ghoul.client.features.commands.arguments.AnyStringArgument;
import tokyo.ghoul.client.features.commands.arguments.NeuroNameArgument;

@CommandRegister(name = "neuro")
public class CommandNeuro extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        NeuroManager neuro = NeuroManager.getInstance();

        builder.then(literal("record").then(argument("name", new AnyStringArgument()).executes(context -> {
            neuro.startRecording(context.getArgument("name", String.class));
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("stop").executes(context -> {
            neuro.stopRecording();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("train")
                .executes(context -> {
                    neuro.train(null);
                    return SINGLE_SUCCESS;
                })
                .then(argument("record", new NeuroNameArgument(neuro::recordNames)).executes(context -> {
                    neuro.train(context.getArgument("record", String.class));
                    return SINGLE_SUCCESS;
                })));

        builder.then(literal("list").executes(context -> {
            neuro.list();
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("play")
                .executes(context -> {
                    var active = neuro.getActiveModel();
                    print(active != null ? "Активна модель §e" + active.getName() : "Модель не выбрана: §e.neuro play <модель>");
                    return SINGLE_SUCCESS;
                })
                .then(argument("model", new NeuroNameArgument(neuro::modelNames)).executes(context -> {
                    neuro.play(context.getArgument("model", String.class));
                    return SINGLE_SUCCESS;
                })));
    }
}
