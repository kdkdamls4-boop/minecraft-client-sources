package net.skycore.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.StatusOnly;

public final class VclipCommand extends ClientCommand {
   private int clipBy(double value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && Minecraft.getInstance().getConnection() != null) {
         double x = instance.getX();
         double y = instance.getY();
         double z = instance.getZ();

         for (int value2 = 0; value2 < 3; value2++) {
            instance.connection.send(new StatusOnly(instance.onGround(), instance.horizontalCollision));
         }

         instance.connection.send(new Pos(x, y + value, z, false, instance.horizontalCollision));
         instance.setPos(x, y + value, z);
         ChatMessages.displayInfo("Teleported " + value + " blocks vertically");
         return 1;
      } else {
         return 0;
      }
   }

   private int clipAuto(boolean condition) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return 0;
      }

      double value = this.findOffset(instance.blockPosition(), condition);
      return this.clipBy(value);
   }

   private double findOffset(BlockPos blockPos, boolean condition) {
      ClientLevel instance = Minecraft.getInstance().level;
      LocalPlayer instance2 = Minecraft.getInstance().player;
      if (instance != null && instance2 != null) {
         if (condition) {
            for (int value = 3; value < 255; value++) {
               BlockPos blockPos2 = blockPos.offset(0, value, 0);
               if (instance.getBlockState(blockPos2).isAir() && instance.getBlockState(blockPos2.above()).isAir()) {
                  return blockPos2.getY() - instance2.getY();
               }
            }
         } else {
            for (int index = -1; index > -255; index--) {
               BlockPos blockPos3 = blockPos.offset(0, index, 0);
               BlockPos blockPos4 = blockPos3.below();
               BlockPos blockPos5 = blockPos4.below();
               if (!instance.getBlockState(blockPos3).isAir() && instance.getBlockState(blockPos4).isAir() && instance.getBlockState(blockPos5).isAir()) {
                  return blockPos5.getY() - instance2.getY();
               }
            }
         }

         return 0.0;
      } else {
         return 0.0;
      }
   }

   public VclipCommand() {
      super("vclip");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      literalArgumentBuilder.then(literal("up").executes(commandContext -> {
         return this.clipAuto(true);
      }));
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("down");
      Command command = commandContext2 -> {
         return this.clipAuto(false);
      };
      literalArgumentBuilder.then(literalArgumentBuilder2.executes(command));
      DoubleArgumentType doubleArgumentType = DoubleArgumentType.doubleArg();
      RequiredArgumentBuilder requiredArgumentBuilder = arg("distance", doubleArgumentType);
      command = commandContext3 -> {
         double value = DoubleArgumentType.getDouble(commandContext3, "distance");
         return this.clipBy(value);
      };
      ArgumentBuilder argumentBuilder = requiredArgumentBuilder.executes(command);
      literalArgumentBuilder.then(argumentBuilder);
   }
}
