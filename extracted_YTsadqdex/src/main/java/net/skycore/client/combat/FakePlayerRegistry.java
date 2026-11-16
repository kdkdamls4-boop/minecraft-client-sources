package net.skycore.client.combat;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.skycore.client.command.FakeplayerCommand;

public final class FakePlayerRegistry {
   public static final FakeplayerCommand INSTANCE = FakeplayerCommand.INSTANCE;

   public static boolean isFake(Entity entity) {
      return FakeplayerCommand.isFake(entity);
   }

   private FakePlayerRegistry() {
   }

   public List<Player> getFakePlayers() {
      return INSTANCE.getFakePlayers();
   }
}
