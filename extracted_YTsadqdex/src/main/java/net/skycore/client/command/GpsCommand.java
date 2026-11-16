package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.List;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent.Type;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.waypoint.WaypointManager;

public final class GpsCommand extends ClientCommand {
   public GpsCommand() {
      super("gps");
   }

   private static void playGpsSound(String text) {
      Runnable resourceAsStream = () -> {
         InputStream resourceAsStream2 = GpsCommand.class.getResourceAsStream("/assets/skycore/gpssounds/" + text);
         if (resourceAsStream2 != null) {
            try (
               resourceAsStream2;
               BufferedInputStream bufferedInputStream = new BufferedInputStream(resourceAsStream2);
               AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bufferedInputStream);
            ) {
               Clip clip = AudioSystem.getClip();
               clip.open(audioInputStream);
               clip.addLineListener(lineEvent -> {
                  Type type = lineEvent.getType();
                  if (type == Type.STOP) {
                     clip.close();
                  }
               });
               clip.start();
            } catch (Exception failure) {
            }
         }
      };
      Thread.startVirtualThread(resourceAsStream);
   }

   private int addPoint(int value, int value2, String text) {
      double instance = Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getY() : 64.0;
      String text2 = text != null && !text.isBlank() ? text.trim() : null;
      WaypointManager waypointManager = SkycoreClient.waypoints();
      waypointManager.set(new WaypointManager.Waypoint(text2, value, instance, value2));
      playGpsSound("enableairpods.wav");
      if (text2 != null) {
         ChatMessages.displayInfo("Метка «" + text2 + "» → XZ " + value + ", " + value2);
      } else {
         ChatMessages.displayInfo("Метка → XZ " + value + ", " + value2);
      }

      return 1;
   }

   private int removePoint(String text) {
      WaypointManager waypointManager = SkycoreClient.waypoints();
      if (waypointManager.isEmpty()) {
         ChatMessages.displayInfo("Список меток пуст");
         return 1;
      }

      String text2 = text.trim();

      try {
         int value = Integer.parseInt(text2);
         WaypointManager.Waypoint waypoints = value >= 1 && value <= waypointManager.size() ? waypointManager.getWaypoints().get(value - 1) : null;
         if (waypoints != null && waypointManager.removeAt(value)) {
            playGpsSound("disableairpods.wav");
            ChatMessages.displayInfo("Удалена #" + value + " «" + waypoints.getDisplayName() + "»");
            return 1;
         } else {
            ChatMessages.displayInfo("Нет метки с номером #" + value);
            return 1;
         }
      } catch (NumberFormatException failure) {
         if (waypointManager.removeByName(text2)) {
            playGpsSound("disableairpods.wav");
            ChatMessages.displayInfo("Удалена метка «" + text2 + "»");
            return 1;
         } else {
            ChatMessages.displayInfo("Метка «" + text2 + "» не найдена");
            return 1;
         }
      }
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      RequiredArgumentBuilder requiredArgumentBuilder = arg("X", IntegerArgumentType.integer());
      RequiredArgumentBuilder requiredArgumentBuilder2 = arg("Z", IntegerArgumentType.integer());
      Command argument = commandContext -> {
         int value = 1;
         value = (Integer)commandContext.getArgument("X", Integer.class);
         Integer argument2 = (Integer)commandContext.getArgument("Z", Integer.class);
         return this.addPoint(value, argument2, null);
      };
      requiredArgumentBuilder2 = (RequiredArgumentBuilder)requiredArgumentBuilder2.executes(argument);
      RequiredArgumentBuilder requiredArgumentBuilder3 = arg("name", StringArgumentType.greedyString());
      ArgumentBuilder argument3 = literalArgumentBuilder2.then(requiredArgumentBuilder.then(requiredArgumentBuilder2.then(requiredArgumentBuilder3.executes(commandContext2 -> {
         int intermediate = -1;
         intermediate = (byte)1;
         intermediate = (Integer)commandContext2.getArgument("X", Integer.class);
         int argument4 = (Integer)commandContext2.getArgument("Z", Integer.class);
         return this.addPoint(intermediate, argument4, StringArgumentType.getString(commandContext2, "name"));
      }))));
      literalArgumentBuilder.then(argument3);
      IntegerArgumentType integerArgumentType = IntegerArgumentType.integer();
      RequiredArgumentBuilder requiredArgumentBuilder4 = arg("X", integerArgumentType);
      IntegerArgumentType integerArgumentType2 = IntegerArgumentType.integer();
      requiredArgumentBuilder = arg("Z", integerArgumentType2);
      requiredArgumentBuilder = (RequiredArgumentBuilder)requiredArgumentBuilder.executes(commandContext3 -> {
         int intermediate2 = -1;
         intermediate2 = (byte)-1;
         intermediate2 = (byte)-1;
         Integer argument5 = (Integer)commandContext3.getArgument("X", Integer.class);
         intermediate2 = argument5;
         Integer argument6 = (Integer)commandContext3.getArgument("Z", Integer.class);
         int value2 = argument6;
         return this.addPoint(intermediate2, value2, null);
      });
      StringArgumentType stringArgumentType = StringArgumentType.greedyString();
      requiredArgumentBuilder2 = arg("name", stringArgumentType);
      ArgumentBuilder argument7 = requiredArgumentBuilder.then(requiredArgumentBuilder2.executes(commandContext4 -> {
         int intermediate3 = -1;
         Integer argument8 = (Integer)commandContext4.getArgument("X", Integer.class);
         intermediate3 = argument8;
         Integer argument9 = (Integer)commandContext4.getArgument("Z", Integer.class);
         int value3 = argument9;
         return this.addPoint(intermediate3, value3, StringArgumentType.getString(commandContext4, "name"));
      }));
      literalArgumentBuilder.then(requiredArgumentBuilder4.then(argument7));
      LiteralArgumentBuilder literalArgumentBuilder3 = literal("list");
      literalArgumentBuilder.then(literalArgumentBuilder3.executes(commandContext5 -> {
         return this.listPoints();
      }));
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("remove");
      Command command = commandContext6 -> {
         return this.clearAll();
      };
      LiteralArgumentBuilder literalArgumentBuilder5 = (LiteralArgumentBuilder)literalArgumentBuilder4.executes(command);
      argument7 = arg("target", StringArgumentType.greedyString());
      Command string = commandContext7 -> {
         String string2 = StringArgumentType.getString(commandContext7, "target");
         return this.removePoint(string2);
      };
      literalArgumentBuilder.then(literalArgumentBuilder5.then(argument7.executes(string)));
      LiteralArgumentBuilder literalArgumentBuilder6 = literal("off");
      Command command2 = commandContext8 -> {
         return this.clearAll();
      };
      ArgumentBuilder argumentBuilder = literalArgumentBuilder6.executes(command2);
      literalArgumentBuilder.then(argumentBuilder);
      ArgumentBuilder argumentBuilder2 = literal("clear").executes(commandContext9 -> {
         return this.clearAll();
      });
      literalArgumentBuilder.then(argumentBuilder2);
      LiteralArgumentBuilder literalArgumentBuilder7 = literal("player");
      argument7 = arg("name", StringArgumentType.word());
      IntegerArgumentType integerArgumentType3 = IntegerArgumentType.integer();
      requiredArgumentBuilder2 = arg("X", integerArgumentType3);
      ArgumentBuilder argument10 = arg("Z", IntegerArgumentType.integer()).executes(commandContext10 -> {
         String argument11 = (String)commandContext10.getArgument("name", String.class);
         Integer argument12 = (Integer)commandContext10.getArgument("X", Integer.class);
         int value4 = argument12;
         Integer argument13 = (Integer)commandContext10.getArgument("Z", Integer.class);
         int value5 = argument13;
         Minecraft instance = Minecraft.getInstance();
         double value6;
         if (instance.player != null) {
            value6 = Minecraft.getInstance().player.getY();
         } else {
            value6 = 64.0;
         }

         double value7 = value6;
         WaypointManager waypointManager = SkycoreClient.waypoints();
         double value8 = value4;
         double intermediate4 = value5;
         waypointManager.setPlayerWaypoint(new WaypointManager.Waypoint(argument11, value8, value7, intermediate4));
         playGpsSound("enableairpods.wav");
         String text = "GPS на игрока " + argument11 + " создан: XZ " + value4 + ", " + value5;
         ChatMessages.displayInfo(text);
         return 1;
      });
      literalArgumentBuilder.then(literalArgumentBuilder7.then(argument7.then(requiredArgumentBuilder2.then(argument10))));
      LiteralArgumentBuilder literalArgumentBuilder8 = literal("player");
      LiteralArgumentBuilder literalArgumentBuilder9 = literal("remove");
      ArgumentBuilder argumentBuilder3 = literalArgumentBuilder8.then(literalArgumentBuilder9.executes(commandContext11 -> {
         WaypointManager waypointManager2 = SkycoreClient.waypoints();
         if (!waypointManager2.isEmptyPlayerWaypoint()) {
            waypointManager2.clearPlayerWaypoint();
            playGpsSound("disableairpods.wav");
            ChatMessages.displayInfo("GPS на игрока удален");
         } else {
            ChatMessages.displayInfo("Нет активного GPS на игрока");
         }

         return 1;
      }));
      literalArgumentBuilder.then(argumentBuilder3);
   }

   private int clearAll() {
      WaypointManager waypointManager = SkycoreClient.waypoints();
      if (waypointManager.resolveActive() == null) {
         ChatMessages.displayInfo("Нет активных меток");
         return 1;
      } else {
         waypointManager.clearAll();
         playGpsSound("disableairpods.wav");
         ChatMessages.displayInfo("Все GPS-метки удалены");
         return 1;
      }
   }

   private int listPoints() {
      WaypointManager waypointManager = SkycoreClient.waypoints();
      var waypoints = waypointManager.getWaypoints();
      int value = !waypointManager.isEmptyPlayerWaypoint() ? 1 : 0;
      if (waypoints.isEmpty() && value == 0) {
         ChatMessages.displayInfo("Список меток пуст");
         return 1;
      }

      ChatMessages.displayInfo("Метки GPS (" + waypoints.size() + "):");

      for (int value2 = 0; value2 < waypoints.size(); value2++) {
         WaypointManager.Waypoint waypoint = (WaypointManager.Waypoint)waypoints.get(value2);
         String displayName = ChatFormatting.WHITE
            + "#"
            + (value2 + 1)
            + ChatFormatting.GRAY
            + " "
            + waypoint.getDisplayName()
            + " — XZ "
            + Math.round(waypoint.x())
            + ", "
            + Math.round(waypoint.z());
         ChatMessages.displayInfo(displayName);
      }

      if (value != 0) {
         WaypointManager.Waypoint activePlayerWaypoint = waypointManager.getActivePlayerWaypoint();
         ChatMessages.displayInfo(
            ChatFormatting.WHITE
               + "player"
               + ChatFormatting.GRAY
               + " "
               + activePlayerWaypoint.getDisplayName()
               + " — XZ "
               + Math.round(activePlayerWaypoint.x())
               + ", "
               + Math.round(activePlayerWaypoint.z())
         );
      }

      return 1;
   }
}
