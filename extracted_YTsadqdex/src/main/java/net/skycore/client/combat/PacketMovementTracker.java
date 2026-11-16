package net.skycore.client.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PacketMovementTracker<P> {
   private boolean descending;
   private final List<P> pongs;
   private double y;
   private double x;
   private final List<P> interactions = new ArrayList<>();
   private double z;
   private int smallMovementTick;

   public boolean descending() {
      return this.descending;
   }

   public boolean interaction(P p, boolean condition) {
      if (condition) {
         if (this.interactions.isEmpty()) {
            return false;
         }
      }

      this.interactions.add((P)p);
      return true;
   }

   public PacketMovementTracker() {
      this.pongs = new ArrayList<>();
      this.x = Double.NaN;
      this.y = Double.NaN;
      this.z = Double.NaN;
      this.smallMovementTick = Integer.MIN_VALUE;
   }

   public void pong(P p) {
      this.pongs.add((P)p);
   }

   public void movement(boolean condition, double x2, double y2, double z2, int smallMovementTick2) {
      if (!condition) {
         this.descending = false;
      } else {
         if (Double.isNaN(this.y)) {
            this.descending = false;
         } else {
            this.descending = ((y2 < this.y - 1.0E-9 ? 1 : 0) != 0);
            double x3 = x2 - this.x;
            double y3 = y2 - this.y;
            double z3 = z2 - this.z;
            if (x3 * x3 + y3 * y3 + z3 * z3 < 9.0E-4) {
               this.smallMovementTick = smallMovementTick2;
            }
         }

         this.x = x2;
         this.y = y2;
         this.z = z2;
      }
   }

   public void flush(Consumer<P> consumer) {
      this.pongs.forEach(consumer);
      this.pongs.clear();
      this.interactions.forEach(consumer);
      this.interactions.clear();
   }

   public void reset() {
      this.interactions.clear();
      this.pongs.clear();
      this.x = this.y = this.z = Double.NaN;
      this.descending = false;
      this.smallMovementTick = Integer.MIN_VALUE;
   }

   public boolean recentSmallMovement(int value) {
      return this.smallMovementTick != Integer.MIN_VALUE && value - this.smallMovementTick <= 2;
   }

   public boolean endTick(P p, Consumer<P> consumer) {
      if (this.interactions.isEmpty()) {
         if (this.pongs.isEmpty()) {
            return false;
         }
      }

      consumer.accept(p);
      this.flush(consumer);
      return true;
   }
}
