package net.skycore.client.combat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public record CrystalAction(
   CrystalAction.Kind kind,
   BlockPos base,
   BlockPos actionPos,
   Direction face,
   EndCrystal crystal,
   float targetDamage,
   float selfDamage,
   float splashDamage,
   float friendDamage,
   int breakCost,
   boolean pocket,
   boolean faceplace,
   boolean elytraHold,
   double score
) {
   public Direction face() {
      return this.face;
   }

   public BlockPos base() {
      return this.base;
   }

   public BlockPos actionPos() {
      return this.actionPos;
   }

   public boolean pocket() {
      return this.pocket;
   }

   public EndCrystal crystal() {
      return this.crystal;
   }

   public int breakCost() {
      return this.breakCost;
   }

   public boolean faceplace() {
      return this.faceplace;
   }

   public CrystalAction.Kind kind() {
      return this.kind;
   }

   public boolean elytraHold() {
      return this.elytraHold;
   }

   public float targetDamage() {
      return this.targetDamage;
   }

   public boolean sameAnchor(CrystalAction crystalAction) {
      if (crystalAction == null) {
         return false;
      }

      BlockPos blockPos;
      if (this.base != null) {
         blockPos = this.base;
      } else {
         blockPos = this.actionPos;
      }

      BlockPos blockPos2 = blockPos;
      BlockPos blockPos3;
      if (crystalAction.base != null) {
         blockPos3 = crystalAction.base;
      } else {
         blockPos3 = crystalAction.actionPos;
      }

      BlockPos blockPos4 = blockPos3;
      return blockPos2 != null && blockPos2.equals(blockPos4);
   }

   public float selfDamage() {
      return this.selfDamage;
   }

   public float friendDamage() {
      return this.friendDamage;
   }

   public double score() {
      return this.score;
   }

   public float splashDamage() {
      return this.splashDamage;
   }

   public enum Kind {
      EXPLODE,
      PLACE_CRYSTAL,
      PLACE_OBSIDIAN,
      DIG,
      PROTECT;
   }
}
