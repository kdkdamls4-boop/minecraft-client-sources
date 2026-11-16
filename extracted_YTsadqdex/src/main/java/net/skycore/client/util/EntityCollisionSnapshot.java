package net.skycore.client.util;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.combat.CrystalSimulation;

public final class EntityCollisionSnapshot {
   public final Vec3 feet;
   public final AABB box;
   public final LivingEntity entity;

   public EntityCollisionSnapshot(LivingEntity entity2, AABB box2) {
      this.entity = entity2;
      this.box = box2;
      this.feet = CrystalSimulation.feet(box2);
   }
}
