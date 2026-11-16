package net.skycore.client.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public final class HclipCommand extends ClientCommand {
   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      DoubleArgumentType doubleArgumentType = DoubleArgumentType.doubleArg();
      RequiredArgumentBuilder requiredArgumentBuilder = arg("distance", doubleArgumentType);
      literalArgumentBuilder.then(requiredArgumentBuilder.executes(commandContext -> {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null && Minecraft.getInstance().getConnection() != null) {
            double value = DoubleArgumentType.getDouble(commandContext, "distance");
            double yRot = Math.toRadians(instance.getYRot());
            double x = instance.getX() - Math.sin(yRot) * value;
            double y = instance.getY();
            double z = instance.getZ() + Math.cos(yRot) * value;

            for (int value2 = 0; value2 < 3; value2++) {
               instance.connection.send(new StatusOnly(instance.onGround(), instance.horizontalCollision));
            }

            instance.connection.send(new Pos(x, y, z, false, instance.horizontalCollision));
            instance.setPos(x, y, z);
            ChatMessages.displayInfo("Teleported " + value + " blocks horizontally");
            return 1;
         } else {
            return 0;
         }
      }));
   }

   public HclipCommand() {
      super("hclip");
   }
}
