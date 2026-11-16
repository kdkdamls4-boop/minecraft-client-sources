package tokyo.ghoul.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.SharedSuggestionProvider;
import tokyo.ghoul.api.command.Command;
import tokyo.ghoul.api.command.CommandRegister;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.client.features.commands.arguments.StrictlyKeyArgument;
import tokyo.ghoul.client.features.commands.arguments.StrictlyModuleArgument;

import java.util.stream.Collectors;

@CommandRegister(name = "bind")
public class CommandBind extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(argument("module", StrictlyModuleArgument.create())
                .then(argument("key", StrictlyKeyArgument.create()).executes(context -> {
                    Module module = getModule(context);
                    int key = KeyStorage.getBind(StringArgumentType.getString(context, "key"));

                    module.setBind(key);
                    print("Модуль " + module.getName() + " забинден на " + KeyStorage.getBind(key) + ".");
                    return SINGLE_SUCCESS;
                }))
                .then(literal("clear").executes(context -> {
                    Module module = getModule(context);

                    module.setBind(-999);
                    print("Бинд модуля " + module.getName() + " сброшен.");
                    return SINGLE_SUCCESS;
                }))
        );

        builder.then(literal("list").executes(context -> {
            String binds = ModuleManager.getInstance().getModules().stream()
                    .filter(Module::hasBind)
                    .map(m -> m.getName() + " [" + KeyStorage.getBind(m.getBind()) + "]")
                    .collect(Collectors.joining(", "));

            if (binds.isEmpty()) {
                print("Забинденных модулей нет.");
            } else {
                print("Бинды: " + binds);
            }
            return SINGLE_SUCCESS;
        }));
    }

    private Module getModule(CommandContext<SharedSuggestionProvider> context) {
        return StrictlyModuleArgument.findModule(StringArgumentType.getString(context, "module"));
    }
}
