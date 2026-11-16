package net.skycore.client.waypoint;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class WaypointManager {
   private final List<WaypointManager.Waypoint> waypoints = new ArrayList<>();
   private WaypointManager.Waypoint playerWaypoint;

   public synchronized void clearAll() {
      this.waypoints.clear();
      this.playerWaypoint = null;
   }

   public synchronized void add(WaypointManager.Waypoint waypoint) {
      if (waypoint != null) {
         String displayName = waypoint.getDisplayName();
         var waypoints2 = this.waypoints;
         Predicate<net.skycore.client.waypoint.WaypointManager.Waypoint> displayName2 = waypoint2 -> {
            return waypoint2.getDisplayName().equalsIgnoreCase(displayName);
         };
         waypoints2.removeIf(displayName2);
         this.waypoints.add(waypoint);
      }
   }

   public synchronized boolean isEmpty() {
      return this.waypoints.isEmpty();
   }

   public synchronized boolean isEmptyPlayerWaypoint() {
      return this.playerWaypoint == null;
   }

   public synchronized List<WaypointManager.Waypoint> resolveAll() {
      ArrayList elements = new ArrayList<>(this.waypoints);
      if (this.playerWaypoint != null) {
         elements.add(this.playerWaypoint);
      }

      return List.copyOf(elements);
   }

   public synchronized void set(WaypointManager.Waypoint element) {
      if (element != null) {
         this.waypoints.clear();
         this.waypoints.add(element);
      }
   }

   public synchronized void setPlayerWaypoint(WaypointManager.Waypoint playerWaypoint2) {
      this.playerWaypoint = playerWaypoint2;
   }

   public synchronized boolean removeByName(String text) {
      return this.waypoints.removeIf(waypoint -> {
         return waypoint.getDisplayName().equalsIgnoreCase(text);
      });
   }

   public synchronized boolean removeAt(int value) {
      int computedValue = value - 1;
      if (computedValue >= 0) {
         if (computedValue < this.waypoints.size()) {
            this.waypoints.remove(computedValue);
            return true;
         }
      }

      return false;
   }

   public synchronized void clearPlayerWaypoint() {
      this.playerWaypoint = null;
   }

   public synchronized int size() {
      return this.waypoints.size();
   }

   public synchronized WaypointManager.Waypoint resolveActive() {
      WaypointManager.Waypoint waypoint;
      if (this.waypoints.isEmpty()) {
         waypoint = this.playerWaypoint;
      } else {
         waypoint = this.waypoints.getFirst();
      }

      return waypoint;
   }

   public synchronized List<WaypointManager.Waypoint> getWaypoints() {
      return List.copyOf(this.waypoints);
   }

   public synchronized WaypointManager.Waypoint getActivePlayerWaypoint() {
      return this.playerWaypoint;
   }

   public record Waypoint(String name, double x, double y, double z) {
      public String getDisplayName() {
         if (this.name != null) {
            if (!this.name.isBlank()) {
               return this.name.trim();
            }
         }

         return "Метка";
      }

      public double x() {
         return this.x;
      }

      public String name() {
         return this.name;
      }

      public double z() {
         return this.z;
      }

      public double y() {
         return this.y;
      }
   }
}
