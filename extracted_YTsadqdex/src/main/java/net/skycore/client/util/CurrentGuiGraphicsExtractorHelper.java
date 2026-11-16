package net.skycore.client.util;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CurrentGuiGraphicsExtractorHelper {
   private static GuiGraphicsExtractor extractor;
   private static DeltaTracker deltaTracker;
   private static boolean active;

   public static GuiGraphicsExtractor currentGuiGraphicsExtractor() {
      return extractor;
   }

   public static void exit2D() {
      active = false;
      extractor = null;
      deltaTracker = null;
   }

   public static void enter2D(GuiGraphicsExtractor guiGraphicsExtractor, DeltaTracker deltaTracker2) {
      active = true;
      extractor = guiGraphicsExtractor;
      deltaTracker = deltaTracker2;
   }

   public static boolean isIn2D() {
      return active;
   }

   public static DeltaTracker currentDeltaTracker() {
      return deltaTracker;
   }

   private CurrentGuiGraphicsExtractorHelper() {
   }
}
