package net.skycore.client.event;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EventBus {
   private final Map<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

   public void post(Object value) {
      if (value != null) {
         this.listeners.forEach((element, objects) -> {
            if (element.isInstance(value)) {
               Iterator iterator = objects.iterator();

               while (true) {
                  if (!iterator.hasNext()) {
                     break;
                  }

                  Consumer element2 = (Consumer)iterator.next();
                  element2.accept(value);
               }
            }
         });
      }
   }

   public <T> Subscription subscribe(Class<T> element, Consumer<? super T> consumer) {
      Consumer<java.lang.Object> consumer2 = value -> {
         consumer.accept(element.cast(value));
      };
      var listeners2 = this.listeners;
      Function<Class<?>, CopyOnWriteArrayList<Consumer<Object>>> function = element2 -> {
         return new CopyOnWriteArrayList();
      };
      CopyOnWriteArrayList<Consumer<Object>> objects = listeners2.computeIfAbsent(element, function);
      objects.add(consumer2);
      return new EventBus.EventSubscription(objects, consumer2);
   }

   public static final class EventSubscription implements Subscription {
      private final CopyOnWriteArrayList<Consumer<Object>> bucket;
      private boolean closed;
      private final Consumer<Object> listener;

      private EventSubscription(CopyOnWriteArrayList<Consumer<Object>> bucket2, Consumer<Object> listener2) {
         this.bucket = bucket2;
         this.listener = listener2;
      }

      @Override
      public void close() {
         if (!this.closed) {
            this.closed = true;
            this.bucket.remove(this.listener);
         }
      }
   }
}
