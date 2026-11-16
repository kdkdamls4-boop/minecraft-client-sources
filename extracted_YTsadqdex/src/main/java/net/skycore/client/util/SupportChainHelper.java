package net.skycore.client.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public final class SupportChainHelper {
   private static final Direction[] DIRECTIONS = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
   };

   public static List<BlockPos> supportChain(BlockPos blockPos, int value, Predicate<BlockPos> predicate, Predicate<BlockPos> predicate2) {
      if (!predicate.test(blockPos)) {
         return List.of();
      }

      ArrayDeque arrayDeque = new ArrayDeque();
      HashSet entries = new HashSet();
      arrayDeque.add(List.of(blockPos.immutable()));
      entries.add(blockPos);
      int value2 = 0;

      while (!arrayDeque.isEmpty() && value2++ < 64) {
         var elements = (List)arrayDeque.removeFirst();
         BlockPos last = (BlockPos)elements.getLast();
         if (predicate2.test(last)) {
            return List.copyOf(elements.reversed());
         }

         if (elements.size() < value) {
            for (Direction direction : DIRECTIONS) {
               BlockPos blockPos2 = last.relative(direction).immutable();
               if (entries.add(blockPos2) && predicate.test(blockPos2)) {
                  ArrayList elements2 = new ArrayList(elements);
                  elements2.add(blockPos2);
                  arrayDeque.addLast(elements2);
               }
            }
         }
      }

      return List.of();
   }

   public static List<BlockPos> candidates(List<AABB> aABBs, double index, double index2) {
      LinkedHashSet entries = new LinkedHashSet();
      double maximum = Math.max(0.0, index);
      double index3 = Math.min(index2, aABBs.size() - 1);

      for (int roundedDown = (int)Math.floor(maximum); roundedDown < index3; roundedDown++) {
         AABB bounds = (AABB)aABBs.get(roundedDown);
         AABB bounds2 = (AABB)aABBs.get(roundedDown + 1);
         BlockPos blockPos = BlockPos.containing(Math.min(bounds.minX, bounds2.minX), Math.min(bounds.minY, bounds2.minY), Math.min(bounds.minZ, bounds2.minZ));
         BlockPos blockPos2 = BlockPos.containing(Math.max(bounds.maxX, bounds2.maxX), Math.max(bounds.maxY, bounds2.maxY), Math.max(bounds.maxZ, bounds2.maxZ));

         for (BlockPos blockPos3 : BlockPos.betweenClosed(blockPos, blockPos2)) {
            if (!entries.contains(blockPos3) && overlap(bounds, bounds2, blockPos3, Math.max(0.0, maximum - roundedDown), Math.min(1.0, index3 - roundedDown)) != null) {
               entries.add(blockPos3.immutable());
            }
         }
      }

      return List.copyOf(entries);
   }

   private SupportChainHelper() {
   }

   private static SupportChainHelper.Interval overlap(AABB bounds, AABB bounds2, BlockPos blockPos, double value, double value2) {
      double[] doubleValues = new double[]{bounds.minX, bounds.minY, bounds.minZ};
      double[] doubleValues2 = new double[]{bounds.maxX, bounds.maxY, bounds.maxZ};
      double[] doubleValues3 = new double[]{bounds2.minX - bounds.minX, bounds2.minY - bounds.minY, bounds2.minZ - bounds.minZ};
      double[] doubleValues4 = new double[]{blockPos.getX(), blockPos.getY(), blockPos.getZ()};

      for (int value3 = 0; value3 < 3; value3++) {
         double computedValue = doubleValues4[value3] + 0.03 - doubleValues2[value3];
         double computedValue2 = doubleValues4[value3] + 0.97 - doubleValues[value3];
         if (Math.abs(doubleValues3[value3]) < 1.0E-9) {
            if (computedValue >= 0.0 || computedValue2 <= 0.0) {
               return null;
            }
         } else {
            double computedValue3 = computedValue / doubleValues3[value3];
            double computedValue4 = computedValue2 / doubleValues3[value3];
            value = Math.max(value, Math.min(computedValue3, computedValue4));
            value2 = Math.min(value2, Math.max(computedValue3, computedValue4));
         }

         if (value2 <= value + 1.0E-7) {
            return null;
         }
      }

      return new SupportChainHelper.Interval(value, value2);
   }

   public static SupportChainHelper.Interval arrival(List<AABB> aABBs, BlockPos blockPos, double value, double value2) {
      double maximum = Math.max(0.0, value);
      double index = Math.min(value2, aABBs.size() - 1);
      SupportChainHelper.Interval interval = null;

      for (int roundedDown = (int)Math.floor(maximum); roundedDown < index; roundedDown++) {
         SupportChainHelper.Interval interval2 = overlap(
            (AABB)aABBs.get(roundedDown), (AABB)aABBs.get(roundedDown + 1), blockPos, Math.max(0.0, maximum - roundedDown), Math.min(1.0, index - roundedDown)
         );
         if (interval2 == null) {
            if (interval != null) {
               break;
            }
         } else {
            double computedValue = roundedDown + interval2.enter();
            double computedValue2 = roundedDown + interval2.exit();
            if (interval != null && computedValue > interval.exit() + 1.0E-7) {
               break;
            }

            interval = new SupportChainHelper.Interval(interval == null ? computedValue : interval.enter(), computedValue2);
         }
      }

      return interval;
   }

   public record Interval(double enter, double exit) {
      public double enter() {
         return this.enter;
      }

      public double exit() {
         return this.exit;
      }
   }
}
