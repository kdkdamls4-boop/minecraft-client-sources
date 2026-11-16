package net.skycore.client.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class AddSystemHelper {
   private static final List<AddSystemHelper.Entry> ENTRIES = new ArrayList<>();
   private static final int MAX_ENTRIES = 400;

   public static void addSystem(String text) {
      if (text != null && !text.isBlank()) {
         AddSystemHelper.Entry entry = new AddSystemHelper.Entry(AddSystemHelper.Type.SYSTEM, null, null, text);
         add(entry);
      }
   }

   private static void add(AddSystemHelper.Entry entry) {
      synchronized (ENTRIES) {
         ENTRIES.add(entry);

         while (true) {
            if (ENTRIES.size() <= 400) {
               return;
            }

            ENTRIES.remove(0);
         }
      }
   }

   public static List<AddSystemHelper.Entry> snapshotForView(String text) {
      synchronized (ENTRIES) {
         if (text != null && !text.isBlank()) {
            String text2 = text.trim();
            String text3 = text2.toLowerCase(Locale.ROOT);
            var stream = ENTRIES.stream();
            Predicate<net.skycore.client.util.AddSystemHelper.Entry> predicate = entry -> {
               if (entry.type() == AddSystemHelper.Type.DM) {
                  if (entry.peer() != null) {
                     String text4 = entry.peer();
                     String text5 = text4.trim();
                     String text6 = text5.toLowerCase(Locale.ROOT);
                     if (text6.equals(text3)) {
                        return true;
                     }
                  }
               }

               return false;
            };
            return stream.filter(predicate).toList();
         } else {
            var stream2 = ENTRIES.stream();
            var stream3 = stream2.filter(entry2 -> {
               AddSystemHelper.Type type = entry2.type();
               if (type != AddSystemHelper.Type.PUBLIC) {
                  AddSystemHelper.Type type2 = entry2.type();
                  if (type2 != AddSystemHelper.Type.SYSTEM) {
                     return false;
                  }
               }

               return true;
            });
            return stream3.toList();
         }
      }
   }

   private AddSystemHelper() {
   }

   public static void addPublic(String text, String text2) {
      if (text2 != null) {
         if (!text2.isBlank()) {
            AddSystemHelper.Entry entry = new AddSystemHelper.Entry(AddSystemHelper.Type.PUBLIC, text == null ? "?" : text, null, text2);
            add(entry);
            return;
         }
      }
   }

   public static void addDm(String text, String text2, String text3, boolean condition) {
      if (text3 != null && !text3.isBlank()) {
         String text4 = condition ? text2 : text;
         AddSystemHelper.Entry entry = new AddSystemHelper.Entry(AddSystemHelper.Type.DM, text, text4, text3);
         add(entry);
      }
   }

   public static List<AddSystemHelper.Entry> snapshot() {
      synchronized (ENTRIES) {
         return List.copyOf(ENTRIES);
      }
   }

   public record Entry(AddSystemHelper.Type type, String author, String peer, String message) {
      public String author() {
         return this.author;
      }

      public AddSystemHelper.Type type() {
         return this.type;
      }

      public String peer() {
         return this.peer;
      }

      public String message() {
         return this.message;
      }
   }

   public enum Type {
      PUBLIC,
      DM,
      SYSTEM;
   }
}
