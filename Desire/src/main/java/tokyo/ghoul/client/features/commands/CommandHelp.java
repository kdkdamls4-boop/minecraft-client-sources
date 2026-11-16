package tokyo.ghoul.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.SharedSuggestionProvider;
import tokyo.ghoul.api.command.Command;
import tokyo.ghoul.api.command.CommandRegister;

@CommandRegister(name = "help")
public class CommandHelp extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            print(".help — список команд");
            print(".cfg save|load|list|remove|folder — конфиги");
            print(".bind <модуль> <клавиша> | .bind <модуль> clear | .bind list — бинды");
            print(".gps <x> <y> <z> [название] | .gps off — метка со стрелкой и дистанцией");
            print(".friend add|remove|clear|list — друзья");
            print(".staffs add|remove|clear|list — стаффы");
            print(".macro add|remove|clear|list — макросы");
            print(".skin set <ник> | .skin off — скин");
            print(".neuro record <название>|stop|train [запись]|list|play <модель> — нейро-ротация Aura");
            return SINGLE_SUCCESS;
        });
    }
}
