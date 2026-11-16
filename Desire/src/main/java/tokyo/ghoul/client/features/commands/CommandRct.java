package tokyo.ghoul.client.features.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import tokyo.ghoul.api.command.Command;
import tokyo.ghoul.api.command.CommandRegister;
import tokyo.ghoul.client.services.AnarchyReconnectService;
import tokyo.ghoul.client.services.AnarchyReconnectService.Target;

@CommandRegister(name = "rct", aliases = {"reconnect"})
public class CommandRct extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        AnarchyReconnectService service = AnarchyReconnectService.getInstance();

        builder.executes(context -> {
            service.start(Target.AUTO, null);
            return SINGLE_SUCCESS;
        });

        builder.then(argument("number", IntegerArgumentType.integer(1)).executes(context -> {
            service.start(Target.AUTO, IntegerArgumentType.getInteger(context, "number"));
            return SINGLE_SUCCESS;
        }));

        for (String mode : new String[]{"an", "grief"}) {
            Target target = Target.parse(mode);
            builder.then(literal(mode)
                    .executes(context -> {
                        service.start(target, null);
                        return SINGLE_SUCCESS;
                    })
                    .then(argument("number", IntegerArgumentType.integer(1)).executes(context -> {
                        service.start(target, IntegerArgumentType.getInteger(context, "number"));
                        return SINGLE_SUCCESS;
                    })));
        }

        builder.then(literal("stop").executes(context -> {
            service.stop();
            return SINGLE_SUCCESS;
        }));
    }
}
