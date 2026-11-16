package net.skycore.client.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import java.util.Collection;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.config.FriendManager;
import net.skycore.client.util.FriendNameArgument;
import net.skycore.client.util.OnlinePlayerArgument;

public final class FriendCommand extends ClientCommand {
   public FriendCommand() {
      super("friend");
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      OnlinePlayerArgument elements = OnlinePlayerArgument.create();
      RequiredArgumentBuilder argument = arg("player", elements).executes(commandContext -> {
         String argument2 = (String)commandContext.getArgument("player", String.class);
         var stream = FakeplayerCommand.INSTANCE.getFakePlayers().stream();
         if (stream.anyMatch(player -> {
            GameProfile gameProfile = player.getGameProfile();
            return gameProfile.name().equalsIgnoreCase(argument2);
         })) {
            ChatMessages.displayInfo("Нельзя добавить фейк-игрока в друзья");
            return 0;
         }

         if (FriendManager.INSTANCE.isFriend(argument2)) {
            String text = String.valueOf(ChatFormatting.WHITE);
            ChatMessages.displayInfo("Игрок " + text + argument2 + ChatFormatting.GRAY + " уже в списке друзей");
         } else {
            if (FriendManager.INSTANCE.add(argument2)) {
               String text2 = String.valueOf(ChatFormatting.WHITE);
               String text3 = String.valueOf(ChatFormatting.GRAY);
               String text4 = "Игрок " + text2 + argument2 + text3 + " добавлен в список друзей";
               ChatMessages.displayInfo(text4);
            } else {
               ChatMessages.displayError("Некорректный ник: " + argument2);
            }
         }

         return 1;
      });
      ArgumentBuilder argumentBuilder = literalArgumentBuilder2.then(argument);
      literalArgumentBuilder.then(argumentBuilder);
      argumentBuilder = literal("remove");
      FriendNameArgument createHelper = FriendNameArgument.create();
      argument = arg("player", createHelper);
      Command argument3 = commandContext2 -> {
         String argument4 = (String)commandContext2.getArgument("player", String.class);
         if (FriendManager.INSTANCE.remove(argument4)) {
            String text5 = String.valueOf(ChatFormatting.WHITE);
            ChatMessages.displayInfo("Игрок " + text5 + argument4 + ChatFormatting.GRAY + " удален из списка друзей");
         } else {
            ChatMessages.displayInfo("Игрок не найден в списке друзей");
         }

         return 1;
      };
      literalArgumentBuilder.then(argumentBuilder.then(argument.executes(argument3)));
      argumentBuilder = literal("list");
      argumentBuilder = argumentBuilder.executes(commandContext3 -> {
         var friends = FriendManager.INSTANCE.getFriends();
         if (friends.isEmpty()) {
            ChatMessages.displayInfo("Список друзей пуст");
         } else {
            String text6 = FriendManager.INSTANCE.getFriendDescriptions().toString();
            ChatMessages.displayInfo(text6);
         }

         return 1;
      });
      literalArgumentBuilder.then(argumentBuilder);
      literalArgumentBuilder.then(literal("clear").executes(commandContext4 -> {
         if (FriendManager.INSTANCE.clear()) {
            ChatMessages.displayInfo("Список друзей очищен");
         } else {
            ChatMessages.displayInfo("Список друзей уже пуст");
         }

         return 1;
      }));
      literalArgumentBuilder.executes(commandContext5 -> {
         ChatMessages.displayInfo("Использование: .friend add/remove/list/clear");
         return 1;
      });
   }
}
