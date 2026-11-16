package net.skycore.client.combat;

import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public record CrystalDamageResult(EndCrystal crystal, float enemyDamage, float selfDamage) {
   public EndCrystal crystal() {
      return this.crystal;
   }

   public float enemyDamage() {
      return this.enemyDamage;
   }
}
