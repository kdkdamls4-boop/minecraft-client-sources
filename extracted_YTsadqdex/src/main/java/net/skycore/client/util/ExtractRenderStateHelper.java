package net.skycore.client.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Button.Builder;
import net.minecraft.client.gui.components.Button.OnPress;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.skycore.client.module.render.DuplisheModule;

public final class ExtractRenderStateHelper extends Screen {
   private final DuplisheModule scene;

   public ExtractRenderStateHelper(DuplisheModule scene2) {
      super(Component.literal("Вы умерли!"));
      this.scene = scene2;
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int coordinate, int coordinate2, float coordinate3) {
      guiGraphicsExtractor.fill(0, 0, this.width, this.height, -1334837248);
      String string = this.getTitle().getString();
      String text = "Мама-птица сбила вас на питбайке";
      Font font = this.font;
      int intermediate = (this.width - this.font.width(string)) / 2;
      int intermediate2 = this.height / 2 - 35;
      guiGraphicsExtractor.text(font, string, intermediate, intermediate2, -1, true);
      int width = -1;
      Font font2 = this.font;
      width = (this.width - this.font.width(text)) / 2;
      guiGraphicsExtractor.text(font2, text, width, this.height / 2 - 10, -1, true);
      super.extractRenderState(guiGraphicsExtractor, coordinate, coordinate2, coordinate3);
   }

   @Override
   protected void init() {
      MutableComponent mutableComponent = Component.literal("Вернуться");
      OnPress onPress = button -> {
         this.onClose();
      };
      Builder builder = Button.builder(mutableComponent, onPress);
      int height = this.width / 2 - 100;
      Button button2 = builder.bounds(height, this.height / 2 + 35, 200, 20).build();
      this.addRenderableWidget(button2);
   }

   @Override
   public void onClose() {
      this.scene.setEnabled(false);
      this.minecraft.setScreenAndShow(null);
   }
}
