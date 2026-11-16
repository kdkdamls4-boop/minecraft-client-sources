package net.skycore.client.combat;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ActionQueue {
   private Runnable current;
   private boolean executeBeforeMovement;
   private final Deque<Runnable> actions = new ArrayDeque<>();
   private int stopTicks;
   private final Deque<Runnable> endTick = new ArrayDeque<>();

   public void drainWithoutStop() {
      this.tickEnd();
      if (this.current != null) {
         Runnable current2 = this.current;
         this.current = null;
         current2.run();
      }

      while (true) {
         Runnable runnable;
         if ((runnable = this.actions.pollFirst()) == null) {
            this.clear();
            return;
         }

         runnable.run();
      }
   }

   public void tickStart() {
      int value = this.stopTicks > 0 ? 1 : 0;
      if (value != 0) {
         this.stopTicks--;
      }

      if (this.executeBeforeMovement) {
         this.executeBeforeMovement = false;
         this.execute();
      } else {
         if (this.current == null) {
            this.current = this.actions.pollFirst();
         }

         if (this.current != null) {
            if (value == 0) {
               this.stopTicks = 2;
            } else {
               this.stopTicks = Math.max(this.stopTicks, 2);
               this.executeBeforeMovement = true;
            }
         }
      }
   }

   public void beforeMovement() {
      if (this.executeBeforeMovement) {
         this.executeBeforeMovement = false;
         this.execute();
      }
   }

   private void execute() {
      this.endTick.addLast(this.current);
      this.current = null;
   }

   public void submit(Runnable runnable) {
      this.actions.addLast(runnable);
   }

   public void tickEnd() {

      Runnable runnable;
      while ((runnable = this.endTick.pollFirst()) != null) {
         runnable.run();
      }
   }

   public boolean isStopping() {
      return this.stopTicks > 0;
   }

   public void clear() {
      this.actions.clear();
      this.endTick.clear();
      this.current = null;
      this.stopTicks = 0;
      this.executeBeforeMovement = false;
   }

   public boolean isBusy() {
      if (this.current == null) {
         if (this.actions.isEmpty()) {
            if (this.endTick.isEmpty()) {
               return false;
            }
         }
      }

      return true;
   }
}
