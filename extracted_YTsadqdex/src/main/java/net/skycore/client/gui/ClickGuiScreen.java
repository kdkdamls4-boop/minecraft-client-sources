package net.skycore.client.gui;

import java.util.List;

public final class ClickGuiScreen {
   public static List<ClickGuiScreen.Action> actionsFor(ClickGuiScreen.Tab tab) {
      return switch (tab) {
         case FRIENDS -> List.of();
         case PARTY -> {
            ClickGuiScreen.Action action = new ClickGuiScreen.Action("Создать пати");
            ClickGuiScreen.Action action2 = new ClickGuiScreen.Action("Принять приглашение");
            yield List.of(action, action2, new ClickGuiScreen.Action("Отклонить приглашение"), new ClickGuiScreen.Action("Выйти из пати"));
         }
         case IRC -> {
            ClickGuiScreen.Action action3 = new ClickGuiScreen.Action("Включить общий чат");
            ClickGuiScreen.Action action4 = new ClickGuiScreen.Action("Выключить общий чат");
            yield List.of(action3, action4);
         }
      };
   }

   private ClickGuiScreen() {
   }

   public record Action(String label) {
      public String label() {
         return this.label;
      }
   }

   public enum Tab {
      FRIENDS("Друзья"),
      PARTY("Party"),
      IRC("Общий чат");

      private final String label;

      Tab(String label2) {
         this.label = label2;
      }

      public String label() {
         return this.label;
      }
   }
}
