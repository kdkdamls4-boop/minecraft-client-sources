package deluxe.client.storage;

import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.event.PlayerTickEvent;



import java.util.LinkedList;
import java.util.Queue;

public class VillagerTaskQueue {
   private Queue<VillagerTaskQueue.Nested0471> field10707 = new LinkedList<>();
   private final EventListener<PlayerTickEvent> field10708 = playerTickEvent -> this.method3134(playerTickEvent);

   public void method3131(VillagerTaskQueue.Nested0471 local4991_1) {
      this.field10707.clear();
      this.field10707.add(local4991_1);
      this.method3134(new PlayerTickEvent());
   }

   public void method3132() {
      this.field10707.clear();
   }

   public void method3134(Object local4994_1) {
      if (this.field10707.peek() != null) {
         if (this.field10707.peek().method3137(local4994_1)) {
            this.field10707.poll();
         }
      }
   }

   public VillagerTaskQueue() {
      EventBus.getInstance().subscribe(this);
   }

   public static class Nested0471 {
      private Queue<VillagerTaskQueue.Nested0469<?>> field10711 = new LinkedList<>();
      private int field10712 = 0;
      private int field10713 = 400;

      public boolean method3137(Object local5000_1) {
         if (this.field10711.peek() == null) {
            return true;
         }

         for (int index = 0;
            index < 64
               && this.field10711.peek() != null
               && this.field10711.peek().field10709.isInstance(local5000_1)
               && this.field10711.peek().method3135(local5000_1);
            index++
         ) {
            this.field10711.poll();
            this.field10712 = 0;
         }

         if (this.field10711.isEmpty()) {
            return true;
         } else if (++this.field10712 > this.field10713) {
            this.field10711.clear();
            return true;
         } else {
            return false;
         }
      }

      public VillagerTaskQueue.Nested0471 method3138(int local5001_1) {
         this.field10713 = Math.max(1, local5001_1);
         return this;
      }

      public <E> VillagerTaskQueue.Nested0471 method3139(Class<E> local5002_1, VillagerTaskQueue.Nested0470<E> local5002_2) {
         this.field10711.add(new VillagerTaskQueue.Nested0469<>(local5002_1, local5002_2));
         return this;
      }
   }

   static class Nested0469<E> {
      private final Class<E> field10709;
      private final VillagerTaskQueue.Nested0470<E> field10710;
   
      public Nested0469(Class<E> local4996_1, VillagerTaskQueue.Nested0470<E> local4996_2) {
         this.field10709 = local4996_1;
         this.field10710 = local4996_2;
      }
   
      boolean method3135(Object local4997_1) {
         return this.field10710.method3136(this.field10709.cast(local4997_1));
      }
   }

   public static interface Nested0470<E> {
      boolean method3136(E var1);
   }
}
