package net.skycore.client.combat;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public interface AttackState {
   default void onAttack() {
   }

   public void tick(LocalPlayer player, LivingEntity entity, Vec3 position, boolean condition);

   default void reset() {
   }
}
