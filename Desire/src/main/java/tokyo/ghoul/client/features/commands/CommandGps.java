package tokyo.ghoul.client.features.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.SharedSuggestionProvider;
import org.joml.Vector3i;
import tokyo.ghoul.api.command.Command;
import tokyo.ghoul.api.command.CommandRegister;
import tokyo.ghoul.api.system.client.GpsManager;

@CommandRegister(name = "gps")
public class CommandGps extends Command {
    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.then(literal("off").executes(context -> {
            if (GpsManager.getInstance().getGpsPosition() == null) {
                print("А чё ты выключить собираешься?");
            } else {
                GpsManager.getInstance().setGpsPosition(null);
                print("Метка успешно удалена.");
            }

            return SINGLE_SUCCESS;
        }));

        builder.then(argument("x", IntegerArgumentType.integer())
                .then(argument("y", IntegerArgumentType.integer())
                        .then(argument("z", IntegerArgumentType.integer())
                                .executes(context -> setGps(context, false))
                                .then(argument("name", StringArgumentType.greedyString())
                                        .executes(context -> setGps(context, true))))));
    }

    private int setGps(CommandContext<SharedSuggestionProvider> context, boolean hasName) {
        int x = IntegerArgumentType.getInteger(context, "x");
        int y = IntegerArgumentType.getInteger(context, "y");
        int z = IntegerArgumentType.getInteger(context, "z");
        String name = hasName ? StringArgumentType.getString(context, "name") : "GPS";

        if (x == 0 && y == 0 && z == 0) {
            print("Ты не можешь указать метку на нулевые координаты.");
            return SINGLE_SUCCESS;
        }

        GpsManager.getInstance().setGpsName(name);
        GpsManager.getInstance().setGpsPosition(new Vector3i(x, y, z));
        print("Метка \"" + name + "\" установлена: " + x + " " + y + " " + z + ".");
        return SINGLE_SUCCESS;
    }
}
