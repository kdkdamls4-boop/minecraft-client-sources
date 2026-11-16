package net.skycore.client.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DraggableManager {
   private static final float MAX_FRAME_DT = 0.05F;
   private final Map<String, Draggable> byId = new LinkedHashMap<>();
   private Draggable active;
   private static final float FIXED_STEP = 0.008333334F;
   private float accumulator;

   public Draggable beginDrag(double value, double value2, List<Draggable> draggables) {
      if (this.active != null) {
         return this.active;
      }

      var draggables2 = draggables == null ? new ArrayList<>(this.byId.values()) : draggables;

      for (int index = draggables2.size() - 1; index >= 0; index--) {
         Draggable draggable = (Draggable)draggables2.get(index);
         if (draggable.contains(value, value2)) {
            draggable.beginDrag(value, value2);
            this.active = draggable;
            return draggable;
         }
      }

      return null;
   }

   public Draggable get(String text) {
      return this.byId.get(text);
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   public Draggable endDrag() {
      Draggable active2 = this.active;
      if (active2 != null) {
         active2.endDrag();
         this.active = null;
      }

      return active2;
   }

   public Draggable active() {
      return this.active;
   }

   public List<Draggable> all() {
      return List.copyOf(this.byId.values());
   }

   public void updateMotion(float index, int width, int height) {
      float clampedValue = clamp(index, 0.0F, 0.05F);
      this.accumulator += clampedValue;

      for (Draggable draggable : this.byId.values()) {
         float computedValue = Math.max(0.0F, width - draggable.width());
         float computedValue2 = Math.max(0.0F, height - draggable.height());
         draggable.setTarget(clamp(draggable.targetX(), 0.0F, computedValue), clamp(draggable.targetY(), 0.0F, computedValue2));
      }

      while (this.accumulator >= 0.008333334F) {
         for (Draggable draggable2 : this.byId.values()) {
            draggable2.updateMotion(0.008333334F);
         }

         this.accumulator -= 0.008333334F;
      }
   }

   public void dragTo(double value, double value2, int value3, int value4) {
      if (this.active != null) {
         this.active.dragTo(value, value2, value3, value4);
      }
   }

   public Draggable register(Draggable draggable) {
      if (draggable == null) {
         throw new IllegalArgumentException("draggable");
      } else {
         Draggable draggable2 = this.byId.putIfAbsent(draggable.id(), draggable);
         if (draggable2 != null) {
            String text = "Duplicate draggable id: " + draggable.id();
            throw new IllegalArgumentException(text);
         } else {
            return draggable;
         }
      }
   }
}
