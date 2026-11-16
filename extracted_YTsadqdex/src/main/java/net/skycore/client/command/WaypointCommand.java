package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.waypoint.WaypointManager;

public final class WaypointCommand extends ClientCommand {
   private int usage() {
      ChatMessages.displayInfo(".way add <name> [x y z], remove <name>, list, clear");
      return 1;
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      StringArgumentType stringArgumentType = StringArgumentType.word();
      RequiredArgumentBuilder string = arg("name", stringArgumentType).executes(commandContext -> {
         return this.addAtPlayer(StringArgumentType.getString(commandContext, "name"));
      });
      RequiredArgumentBuilder requiredArgumentBuilder = arg("x", DoubleArgumentType.doubleArg());
      RequiredArgumentBuilder requiredArgumentBuilder2 = arg("y", DoubleArgumentType.doubleArg());
      RequiredArgumentBuilder requiredArgumentBuilder3 = arg("z", DoubleArgumentType.doubleArg());
      ArgumentBuilder string2 = requiredArgumentBuilder3.executes(commandContext2 -> {
         String string3 = StringArgumentType.getString(commandContext2, "name");
         double value = DoubleArgumentType.getDouble(commandContext2, "x");
         double value2 = DoubleArgumentType.getDouble(commandContext2, "y");
         return this.add(string3, value, value2, DoubleArgumentType.getDouble(commandContext2, "z"));
      });
      RequiredArgumentBuilder requiredArgumentBuilder4 = (RequiredArgumentBuilder)string.then(requiredArgumentBuilder.then(requiredArgumentBuilder2.then(string2)));
      ArgumentBuilder argumentBuilder = literalArgumentBuilder2.then(requiredArgumentBuilder4);
      literalArgumentBuilder.then(argumentBuilder);
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("remove");
      requiredArgumentBuilder4 = arg("name", StringArgumentType.word());
      SuggestionProvider waypoints = (commandContext3, suggestionsBuilder) -> {
         WaypointManager waypointManager = SkycoreClient.waypoints();
         var waypoints2 = waypointManager.getWaypoints();
         Iterator iterator = waypoints2.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return suggestionsBuilder.buildFuture();
            }

            WaypointManager.Waypoint element = (WaypointManager.Waypoint)iterator.next();
            suggestionsBuilder.suggest(element.getDisplayName());
         }
      };
      requiredArgumentBuilder4 = requiredArgumentBuilder4.suggests(waypoints);
      literalArgumentBuilder.then(literalArgumentBuilder3.then(requiredArgumentBuilder4.executes(commandContext4 -> {
         String string4 = StringArgumentType.getString(commandContext4, "name");
         return this.remove(string4);
      })));
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("clear");
      Command command = commandContext5 -> {
         return this.clear();
      };
      ArgumentBuilder argumentBuilder2 = literalArgumentBuilder4.executes(command);
      literalArgumentBuilder.then(argumentBuilder2);
      LiteralArgumentBuilder literalArgumentBuilder5 = literal("list");
      Command command2 = commandContext6 -> {
         return this.list();
      };
      literalArgumentBuilder.then(literalArgumentBuilder5.executes(command2));
      Command command3 = commandContext7 -> {
         return this.usage();
      };
      literalArgumentBuilder.executes(command3);
   }

   private static String format(double value) {
      Locale rOOT = Locale.ROOT;
      Object[] objectValues = new Object[1];
      objectValues[0] = value;
      return String.format(rOOT, "%.1f", objectValues);
   }

   private int list() {
      int intermediate = -1;
      intermediate = (byte)1;
      intermediate = (byte)1;
      var waypoints = SkycoreClient.waypoints().getWaypoints();
      if (waypoints.isEmpty()) {
         ChatMessages.displayInfo("Список точек пуст");
         return 1;
      }

      ChatMessages.displayInfo("Список точек:");

      for (WaypointManager.Waypoint waypoint : waypoints) {
         intermediate = (byte)-1;
         String text = String.valueOf(ChatFormatting.WHITE);
         String displayName = waypoint.getDisplayName();
         String text2 = String.valueOf(ChatFormatting.GRAY);
         double x = waypoint.x();
         String text3 = format(x);
         String text4 = format(waypoint.y());
         String text5 = text + displayName + text2 + " {x: " + text3 + ", y: " + text4 + ", z: " + format(waypoint.z()) + "}";
         ChatMessages.displayInfo(text5);
      }

      String text6 = String.valueOf(ChatFormatting.WHITE);
      intermediate = waypoints.size();
      ChatMessages.displayInfo("Всего: " + text6 + intermediate);
      return 1;
   }

   private static boolean validName(String text) {
      return text != null && text.matches("^[A-Za-z0-9]{3,8}$");
   }

   public WaypointCommand(String text) {
      super(text);
   }

   private int addAtPlayer(String text) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return this.usage();
      }

      double x = instance.player.getX();
      double y = instance.player.getY();
      return this.add(text, x, y, instance.player.getZ());
   }

   private int remove(String text) {
      if (SkycoreClient.waypoints().removeByName(text)) {
         String text2 = String.valueOf(ChatFormatting.WHITE);
         ChatMessages.displayInfo("Точка '" + text2 + text + ChatFormatting.GRAY + "' удалена");
         return 1;
      } else {
         ChatMessages.displayError("Точка '" + text + "' не найдена");
         return 0;
      }
   }

   private int add(String text, double value, double value2, double value3) {
      if (!validName(text)) {
         ChatMessages.displayError("Имя точки: 3–8 символов A-Z/0-9");
         return 0;
      } else {
         WaypointManager waypointManager = SkycoreClient.waypoints();
         WaypointManager.Waypoint waypoint = new WaypointManager.Waypoint(text, value, value2, value3);
         waypointManager.add(waypoint);
         String text2 = String.valueOf(ChatFormatting.WHITE);
         String text3 = String.valueOf(ChatFormatting.GRAY);
         String text4 = format(value);
         String text5 = format(value2);
         String text6 = format(value3);
         String text7 = "Точка '" + text2 + text + text3 + "' установлена: " + text4 + ", " + text5 + ", " + text6;
         ChatMessages.displayInfo(text7);
         return 1;
      }
   }

   private int clear() {
      SkycoreClient.waypoints().clearAll();
      ChatMessages.displayInfo("Все точки удалены");
      return 1;
   }
}
