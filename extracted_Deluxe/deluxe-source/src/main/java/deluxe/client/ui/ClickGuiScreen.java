package deluxe.client.ui;

import deluxe.client.util.Colors;
import deluxe.client.render.FontFace;
import deluxe.client.util.InputBinding;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.module.ModuleCategory;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;
import deluxe.client.ui.ScreenClosingTransition;
import deluxe.client.ui.UiTheme;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.ui.panel.ModuleCategoryPanel;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.render.GuiEffectsState;
import deluxe.client.ui.SmoothScroll;
import deluxe.client.render.Render2D;
import deluxe.client.setting.TextSetting;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class ClickGuiScreen extends Screen implements ScreenClosingTransition.Nested0386 {
   private final SettingRenderContext settingRenderContext = new SettingRenderContext();
   private final ModuleCategory[] moduleCategoryArray = ModuleCategory.values();
   private final SmoothScroll[] recoveredClass0320Array;
   private final float[][] field01817;
   private float field01818;
   private float field01819;
   private long field01820;
   private boolean field01821;
   private boolean field01822;
   private boolean field01823;
   private boolean field01824;
   private long field01825 = System.nanoTime() / 1000000L;
   private boolean field01826;
   private final TextSetting textSetting = new TextSetting("Поиск", "").withLengthRange(0, 28);
   private long field01828;
   private static ClickGuiScreen clickGuiScreen;

   @Override
   public void close() {
      if (!this.field01822) {
         this.field01822 = true;
         this.settingRenderContext.textSetting = null;
         this.settingRenderContext.field12425 = null;
         this.settingRenderContext.keybindSetting = null;
         this.settingRenderContext.field12427 = null;
         this.settingRenderContext.module = null;
         this.field01824 = true;
         ScreenClosingTransition.method1458(this);
         if (MinecraftClient.getInstance().currentScreen == this) {
            MinecraftClient.getInstance().setScreen(null);
         }
      }
   }

   private void drawSearchBar(float local1247_1, float local1247_2, float local1247_3) {
      float local1247_4 = this.settingRenderContext.scale;
      float local1247_5 = 74.8F * local1247_4;
      float local1247_6 = 19.31F * local1247_4;
      float local1247_7 = (local1247_1 - local1247_5) / 2.0F;
      float local1247_8 = Math.min(local1247_3 + 30.8F * local1247_4, local1247_2 - local1247_6 - 5.0F * local1247_4);
      Render2D.method4725(
         local1247_7 + 0.5F * local1247_4,
         local1247_8 + 0.5F * local1247_4,
         local1247_5 - 1.0F * local1247_4,
         local1247_6 - 1.0F * local1247_4,
         local1247_6 / 2.26F - 0.5F * local1247_4,
         30.0F,
         1.0F,
         -1
      );
      Render2D.method4715(local1247_7, local1247_8, local1247_5, local1247_6, local1247_6 / 2.26F, ClickGuiStyle.field10781);
      Render2D.method4769(local1247_7, local1247_8, local1247_5, local1247_6, local1247_6 / 2.26F, 0.5F, ClickGuiStyle.field10782);
      Render2D.method4767(
         local1247_7, local1247_8, local1247_6, local1247_6, local1247_6 / 2.26F, 0.0F, 0.0F, local1247_6 / 2.26F, ClickGuiStyle.field10818
      );
      Render2D.method4777(
         "assets/deluxe/textures/gui/mainmenu/icons/logo.png",
         local1247_7 + local1247_6 / 2.0F - 3.4F * local1247_4,
         local1247_8 + local1247_6 / 2.0F - 2.65F * local1247_4,
         6.8F * local1247_4,
         5.3F * local1247_4,
         0.0F,
         ClickGuiStyle.field10780
      );
      Render2D.method4777(
         "assets/deluxe/textures/gui/clickgui/search.png",
         local1247_7 + local1247_5 - 10.4F * local1247_4,
         local1247_8 + local1247_6 / 2.0F - 2.65F * local1247_4,
         5.3F * local1247_4,
         5.3F * local1247_4,
         0.0F,
         ClickGuiStyle.field10783
      );
      String local1247_9 = this.textSetting.getText();
      String local1247_10 = local1247_9.isEmpty() ? (this.field01826 ? "" : "Search...") : local1247_9;
      float local1247_11 = local1247_7 + 25.4F * local1247_4;
      float local1247_12 = local1247_8 + (local1247_6 - 6.0F * local1247_4) / 2.0F;
      float local1247_13 = local1247_5 - 25.4F * local1247_4 - 12.0F * local1247_4;
      Render2D.method4678(this.settingRenderContext.drawContext, local1247_11 - 1.0F * local1247_4, local1247_8, local1247_13 + 2.0F * local1247_4, local1247_6);
      float local1247_14 = FontText.method1396(FontFace.SF, local1247_10, 6.0F * local1247_4);
      float local1247_15 = Math.max(0.0F, local1247_14 - local1247_13);
      if (this.textSetting.hasSelection() && !local1247_9.isEmpty()) {
         Render2D.method4715(
            local1247_11
               - local1247_15
               + FontText.method1396(FontFace.SF, local1247_9.substring(0, this.textSetting.getSelectionStart()), 6.0F * local1247_4),
            local1247_12 - 1.0F * local1247_4,
            FontText.method1396(
               FontFace.SF, local1247_9.substring(this.textSetting.getSelectionStart(), this.textSetting.getSelectionEnd()), 6.0F * local1247_4
            ),
            8.0F * local1247_4,
            1.0F * local1247_4,
            Colors.method3728(-1, 56)
         );
      }

      if (!local1247_10.isEmpty()) {
         FontText.method1397(
            FontFace.SF,
            local1247_10,
            local1247_11 - local1247_15,
            local1247_12,
            6.0F * local1247_4,
            local1247_9.isEmpty() ? ClickGuiStyle.field10786 : Colors.method3728(-1, 210)
         );
      }

      if (this.field01826 && System.nanoTime() / 1000000L / 530L % 2L == 0L) {
         float local1247_16 = local1247_11
            - local1247_15
            + FontText.method1396(FontFace.SF, local1247_9.substring(0, this.textSetting.getCaret()), 6.0F * local1247_4);
         Render2D.method4715(
            local1247_16 + 0.4F * local1247_4,
            local1247_12 - 0.5F * local1247_4,
            0.6F * local1247_4,
            7.0F * local1247_4,
            0.3F * local1247_4,
            Colors.method3728(-1, 200)
         );
      }

      Render2D.method4661(this.settingRenderContext.drawContext);
      this.settingRenderContext.method3554(local1247_7, local1247_8, local1247_5, local1247_6, null, () -> {
         this.field01826 = true;
         this.textSetting.moveToEnd(false);
         this.field01828 = System.nanoTime() / 1000000L;
      });
   }

   public static ClickGuiScreen instance() {
      if (clickGuiScreen == null) {
         clickGuiScreen = new ClickGuiScreen();
      }

      return clickGuiScreen;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local1249_2, int local1249_3, float local1249_4) {
   }

   @Override
   public void render(DrawContext drawContext, int local1250_2, int local1250_3, float local1250_4) {
      float local1250_5 = sf();
      float mouseX = local1250_2 * local1250_5;
      float mouseY = local1250_3 * local1250_5;
      this.settingRenderContext
         .method3574(
            drawContext, 1.13F * Math.min(1.0F, Math.min(this.width * local1250_5 / 640.0F, this.height * local1250_5 / 460.0F)), mouseX, mouseY
         );
      local1250_5 = Math.min(0.05F, (float)(System.nanoTime() / 1000000L - this.field01825) / 1000.0F);
      this.field01825 = System.nanoTime() / 1000000L;
      InterfaceModule.method2247().method2250();
      Render2D.method4713(drawContext);
      boolean local1250_8 = !this.field01822;
      if (local1250_8 != this.field01821) {
         this.field01819 = this.field01818;
         this.field01820 = System.nanoTime() / 1000000L;
         this.field01821 = local1250_8;
      }

      mouseX = Math.min(1.0F, (float)(System.nanoTime() / 1000000L - this.field01820) / 300.0F);
      mouseY = mouseX * mouseX * mouseX * (mouseX * (mouseX * 6.0F - 15.0F) + 10.0F);
      this.field01818 = this.field01819 + ((local1250_8 ? 1.0F : 0.0F) - this.field01819) * mouseY;
      if (this.field01822 && this.field01818 <= 0.003F) {
         GuiEffectsState.method3973(false);
         Render2D.method4754();
         if (!this.field01823) {
            this.field01823 = true;
            MinecraftClient.getInstance().execute(() -> {
               if (MinecraftClient.getInstance().currentScreen == this) {
                  MinecraftClient.getInstance().setScreen(null);
               }
            });
         }
      } else {
         float local1250_9 = this.width * sf();
         float local1250_10 = this.height * sf();
         GuiEffectsState.method3973(true);
         GuiEffectsState.method3975(8.0F);
         GuiEffectsState.method3980(0.0F);
         drawContext.createNewRootLayer();
         if (this.field01824) {
            try {
               drawContext.applyBlur();
            } catch (IllegalStateException exception) {
            }
         }

         Render2D.method4705(this.field01818);
         float local1250_11 = Math.min(266.5F * this.settingRenderContext.scale, local1250_10 - 60.0F * this.settingRenderContext.scale);
         float local1250_12 = (local1250_10 - local1250_11) / 2.0F;

         for (int index = 0; index < this.moduleCategoryArray.length; index++) {
            this.field01817[index][0] = (
                     local1250_9
                        - (this.moduleCategoryArray.length * 112.5F * this.settingRenderContext.scale + (this.moduleCategoryArray.length - 1) * 9.0F * this.settingRenderContext.scale)
                  )
                  / 2.0F
               + index * 121.5F * this.settingRenderContext.scale;
            this.field01817[index][1] = local1250_12;
            this.field01817[index][2] = 112.5F * this.settingRenderContext.scale;
            this.field01817[index][3] = local1250_11;
            ModuleCategoryPanel.method3286(
               this.settingRenderContext,
               this.moduleCategoryArray[index],
               this.recoveredClass0320Array[index],
               this.field01817[index][0],
               this.field01817[index][1],
               this.field01817[index][2],
               this.field01817[index][3],
               local1250_5
            );
         }

         this.drawTip(local1250_9, local1250_12);
         this.drawSearchBar(local1250_9, local1250_10, local1250_12 + local1250_11);
         this.settingRenderContext.method3552();
         Render2D.method4650();
         this.updateDrag(local1250_2 * sf(), local1250_3 * sf());
         Render2D.method4754();
      }
   }

   private void finishOverlay() {
      GuiEffectsState.method3973(false);
      this.field01824 = false;
      this.field01822 = false;
      this.field01823 = false;
      this.field01818 = 0.0F;
      this.field01819 = 0.0F;
      this.field01821 = false;
      this.field01820 = 0L;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   private int searchWordHop(int local1253_1) {
      String local1253_2 = this.textSetting.getText();
      int local1253_3 = this.textSetting.getCaret();
      if (local1253_1 < 0) {
         while (local1253_3 > 0 && local1253_2.charAt(local1253_3 - 1) == ' ') {
            local1253_3--;
         }

         while (local1253_3 > 0 && local1253_2.charAt(local1253_3 - 1) != ' ') {
            local1253_3--;
         }

         return local1253_3 - this.textSetting.getCaret();
      } else {
         while (local1253_3 < local1253_2.length() && local1253_2.charAt(local1253_3) != ' ') {
            local1253_3++;
         }

         while (local1253_3 < local1253_2.length() && local1253_2.charAt(local1253_3) == ' ') {
            local1253_3++;
         }

         return local1253_3 - this.textSetting.getCaret();
      }
   }

   @Override
   public void removed() {
      GuiEffectsState.method3973(false);
      if (!this.field01824) {
         this.field01822 = false;
         this.field01823 = false;
         this.field01818 = 0.0F;
         this.field01819 = 0.0F;
         this.field01821 = false;
         this.field01820 = 0L;
      }

      this.settingRenderContext.textSetting = null;
      this.settingRenderContext.field12425 = null;
      this.settingRenderContext.keybindSetting = null;
      this.settingRenderContext.field12427 = null;
      this.settingRenderContext.module = null;
      this.field01826 = false;
      this.textSetting.setValue("");
      this.settingRenderContext.field12426 = "";
      super.removed();
   }

   @Override
   public boolean charTyped(CharInput charInput) {
      if (this.settingRenderContext.field12425 != null) {
         if (charInput.codepoint() >= 32) {
            this.settingRenderContext.field12425.method3577((char)charInput.codepoint());
         }

         return true;
      } else if (this.settingRenderContext.textSetting != null) {
         if (charInput.codepoint() >= 32) {
            this.settingRenderContext.textSetting.insertText(String.valueOf((char)charInput.codepoint()));
         }

         return true;
      } else if (this.field01826) {
         if (charInput.codepoint() >= 32) {
            this.textSetting.insertText(String.valueOf((char)charInput.codepoint()));
            this.settingRenderContext.field12426 = this.textSetting.getText().toLowerCase();
            this.field01828 = System.nanoTime() / 1000000L;
         }

         return true;
      } else {
         return super.charTyped(charInput);
      }
   }

   private static float sf() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   private void updateDrag(float local1259_1, float local1259_2) {
      if (this.settingRenderContext.numberSetting != null || this.settingRenderContext.colorSetting != null) {
         float local1259_3 = GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) == 1 ? 1.0F : 0.0F;
         if (this.settingRenderContext.numberSetting != null) {
            if (local1259_3 < 0.5F) {
               this.settingRenderContext.numberSetting = null;
            } else if (this.settingRenderContext.field12415.get(this.settingRenderContext.numberSetting) != null) {
               this.settingRenderContext
                  .numberSetting
                  .setFraction(
                     SettingRenderContext.method3541(
                        (local1259_1 - this.settingRenderContext.field12415.get(this.settingRenderContext.numberSetting)[0])
                           / this.settingRenderContext.field12415.get(this.settingRenderContext.numberSetting)[1],
                        0.0F,
                        1.0F
                     )
                  );
            }
         }

         if (this.settingRenderContext.colorSetting != null) {
            if (local1259_3 < 0.5F) {
               this.settingRenderContext.colorSetting = null;
            } else if (this.settingRenderContext.field12416.get(this.settingRenderContext.colorSetting) != null) {
               float[] floatArray = this.settingRenderContext.method3553(this.settingRenderContext.colorSetting);
               float[] floatArray2 = this.settingRenderContext.field12416.get(this.settingRenderContext.colorSetting);
               if (this.settingRenderContext.field12421 == 0) {
                  floatArray[1] = SettingRenderContext.method3541((local1259_1 - floatArray2[0]) / floatArray2[2], 0.0F, 1.0F);
                  floatArray[2] = SettingRenderContext.method3541(1.0F - (local1259_2 - floatArray2[1]) / floatArray2[3], 0.0F, 1.0F);
               } else {
                  floatArray[0] = SettingRenderContext.method3541((local1259_1 - floatArray2[4]) / floatArray2[6], 0.0F, 1.0F);
               }

               this.settingRenderContext.method3558(this.settingRenderContext.colorSetting, floatArray);
            }
         }
      }
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (this.field01822) {
         return true;
      }

      int local1260_2 = keyInput.key();
      if (this.settingRenderContext.module != null) {
         this.settingRenderContext.module.withKeyCode(local1260_2 != 256 && local1260_2 != 261 && local1260_2 != 344 ? local1260_2 : -1);
         this.settingRenderContext.module = null;
         return true;
      }

      if (this.settingRenderContext.keybindSetting != null) {
         this.settingRenderContext.keybindSetting.withKeyCode(local1260_2 != 256 && local1260_2 != 261 ? local1260_2 : -1);
         this.settingRenderContext.keybindSetting = null;
         return true;
      }

      if (this.settingRenderContext.field12425 != null) {
         if (local1260_2 == 256) {
            this.settingRenderContext.field12425 = null;
         } else {
            this.settingRenderContext.field12425.method3576(local1260_2);
         }

         return true;
      } else if (this.settingRenderContext.textSetting != null) {
         if ((keyInput.modifiers() & 2) != 0 && local1260_2 == 65) {
            this.settingRenderContext.textSetting.selectAll();
         } else if ((keyInput.modifiers() & 2) != 0 && local1260_2 == 67 && this.settingRenderContext.textSetting.hasSelection()) {
            MinecraftClient.getInstance().keyboard.setClipboard(this.settingRenderContext.textSetting.getSelectedText());
         } else if ((keyInput.modifiers() & 2) != 0 && local1260_2 == 88 && this.settingRenderContext.textSetting.hasSelection()) {
            MinecraftClient.getInstance().keyboard.setClipboard(this.settingRenderContext.textSetting.getSelectedText());
            this.settingRenderContext.textSetting.backspace();
         } else if ((keyInput.modifiers() & 2) != 0 && local1260_2 == 86) {
            this.settingRenderContext.textSetting.insertText(MinecraftClient.getInstance().keyboard.getClipboard());
         } else if (local1260_2 == 259) {
            this.settingRenderContext.textSetting.backspace();
         } else if (local1260_2 == 261) {
            this.settingRenderContext.textSetting.deleteForward();
         } else if (local1260_2 == 263) {
            this.settingRenderContext.textSetting.moveCaret(-1, (keyInput.modifiers() & 1) != 0);
         } else if (local1260_2 == 262) {
            this.settingRenderContext.textSetting.moveCaret(1, (keyInput.modifiers() & 1) != 0);
         } else if (local1260_2 == 268) {
            this.settingRenderContext.textSetting.moveToStart((keyInput.modifiers() & 1) != 0);
         } else if (local1260_2 == 269) {
            this.settingRenderContext.textSetting.moveToEnd((keyInput.modifiers() & 1) != 0);
         } else if (local1260_2 == 257 || local1260_2 == 256) {
            this.settingRenderContext.textSetting = null;
         }

         return true;
      } else {
         if (this.settingRenderContext.field12427 != null && local1260_2 == 256) {
            this.settingRenderContext.field12427 = null;
            return true;
         }

         if ((keyInput.modifiers() & 2) != 0 && local1260_2 == 70) {
            this.field01826 = !this.field01826;
            if (!this.field01826) {
               this.textSetting.setValue("");
               this.settingRenderContext.field12426 = "";
            }

            this.field01828 = System.nanoTime() / 1000000L;
            return true;
         } else if (!this.field01826) {
            if (local1260_2 != 344 && local1260_2 != 256) {
               return super.keyPressed(keyInput);
            }

            this.close();
            return true;
         } else if (local1260_2 != 256 && local1260_2 != 257) {
            boolean local1260_3 = (keyInput.modifiers() & 2) != 0;
            boolean local1260_4 = (keyInput.modifiers() & 1) != 0;
            String local1260_5 = this.textSetting.getText();
            if (local1260_3 && local1260_2 == 65) {
               this.textSetting.selectAll();
            } else if (local1260_3 && local1260_2 == 67 && this.textSetting.hasSelection()) {
               MinecraftClient.getInstance().keyboard.setClipboard(this.textSetting.getSelectedText());
            } else if (local1260_3 && local1260_2 == 88 && this.textSetting.hasSelection()) {
               MinecraftClient.getInstance().keyboard.setClipboard(this.textSetting.getSelectedText());
               this.textSetting.backspace();
            } else if (local1260_3 && local1260_2 == 86) {
               this.textSetting.insertText(MinecraftClient.getInstance().keyboard.getClipboard());
            } else if (local1260_2 == 259) {
               if (local1260_3 && !this.textSetting.hasSelection()) {
                  this.textSetting.moveCaret(this.searchWordHop(-1), true);
               }

               this.textSetting.backspace();
            } else if (local1260_2 == 261) {
               if (local1260_3 && !this.textSetting.hasSelection()) {
                  this.textSetting.moveCaret(this.searchWordHop(1), true);
               }

               this.textSetting.deleteForward();
            } else if (local1260_2 == 263) {
               this.textSetting.moveCaret(local1260_3 ? this.searchWordHop(-1) : -1, local1260_4);
            } else if (local1260_2 == 262) {
               this.textSetting.moveCaret(local1260_3 ? this.searchWordHop(1) : 1, local1260_4);
            } else if (local1260_2 == 268) {
               this.textSetting.moveToStart(local1260_4);
            } else if (local1260_2 == 269) {
               this.textSetting.moveToEnd(local1260_4);
            }

            if (!local1260_5.equals(this.textSetting.getText())) {
               this.settingRenderContext.field12426 = this.textSetting.getText().toLowerCase();
               this.field01828 = System.nanoTime() / 1000000L;
            }

            return true;
         } else {
            this.field01826 = false;
            this.textSetting.setValue("");
            this.settingRenderContext.field12426 = "";
            return true;
         }
      }
   }

   private void drawTip(float local1261_1, float local1261_2) {
      if (this.settingRenderContext.field12428 != null) {
         this.settingRenderContext.field12429 = this.settingRenderContext.field12428;
      }

      float local1261_3 = this.settingRenderContext.method3560("tip.show", this.settingRenderContext.field12428 != null ? 1.0F : 0.0F);
      if (this.settingRenderContext.field12428 != null) {
         local1261_3 = 1.0F;
      }

      if (!(local1261_3 < 0.01F) && !this.settingRenderContext.field12429.isEmpty()) {
         float local1261_4 = this.settingRenderContext.scale;
         List<String> local1261_5 = UiTheme.method2873(FontFace.SF, this.settingRenderContext.field12429, 6.0F * local1261_4, 240.0F * local1261_4);
         float local1261_6 = local1261_5.size() * 6.95F * local1261_4;
         float local1261_7 = Math.max(5.0F * local1261_4, local1261_2 - 29.6F * local1261_4 + (13.9F * local1261_4 - local1261_6) / 2.0F);
         Render2D.method4705(local1261_3);

         for (String local1261_9 : local1261_5) {
            FontText.method1397(
               FontFace.SF,
               local1261_9,
               (local1261_1 - FontText.method1396(FontFace.SF, local1261_9, 6.0F * local1261_4)) / 2.0F,
               local1261_7,
               6.0F * local1261_4,
               ClickGuiStyle.field10819
            );
            local1261_7 += 6.95F * local1261_4;
         }

         Render2D.method4650();
      }
   }

   @Override
   public boolean mouseScrolled(double local1262_1, double local1262_3, double local1262_5, double local1262_7) {
      if (this.field01822) {
         return true;
      }

      float local1262_9 = sf();
      float local1262_10 = (float)local1262_1 * local1262_9;
      float local1262_11 = (float)local1262_3 * local1262_9;
      if (this.settingRenderContext.field12434 != null
         && UiTheme.method2879(
            local1262_10,
            local1262_11,
            this.settingRenderContext.field12434[0],
            this.settingRenderContext.field12434[1],
            this.settingRenderContext.field12434[2],
            this.settingRenderContext.field12434[3]
         )) {
         this.settingRenderContext
            .field12432
            .put(
               this.settingRenderContext.field12433,
               SettingRenderContext.method3541(
                  this.settingRenderContext.field12432.getOrDefault(this.settingRenderContext.field12433, 0.0F) - (float)local1262_7 * 17.86F * this.settingRenderContext.scale,
                  0.0F,
                  this.settingRenderContext.field12435
               )
            );
         return true;
      }

      if (this.settingRenderContext.field12418 > 0) {
         return true;
      }

      for (int index = 0; index < this.moduleCategoryArray.length; index++) {
         if (this.field01817[index] != null
            && UiTheme.method2879(
               local1262_10,
               local1262_11,
               this.field01817[index][0],
               this.field01817[index][1],
               this.field01817[index][2],
               this.field01817[index][3]
            )) {
            this.recoveredClass0320Array[index].targetOffset = SettingRenderContext.method3541(
               this.recoveredClass0320Array[index].targetOffset - (float)local1262_7 * 24.0F * this.settingRenderContext.scale,
               0.0F,
               this.recoveredClass0320Array[index].maximumOffset
            );
            return true;
         }
      }

      return super.mouseScrolled(local1262_1, local1262_3, local1262_5, local1262_7);
   }

   private ClickGuiScreen() {
      super(Text.literal("Deluxe ClickGUI"));
      this.recoveredClass0320Array = new SmoothScroll[this.moduleCategoryArray.length];
      this.field01817 = new float[this.moduleCategoryArray.length][4];

      for (int index = 0; index < this.moduleCategoryArray.length; index++) {
         this.recoveredClass0320Array[index] = new SmoothScroll();
      }
   }

   public SettingRenderContext getCtx() {
      return this.settingRenderContext;
   }

   @Override
   protected void init() {
      super.init();
      if (this.field01824) {
         ScreenClosingTransition.method1461(this);
         this.field01824 = false;
         this.field01822 = false;
         this.field01823 = false;
      }
   }

   @Override
   public boolean renderClosing(DrawContext drawContext) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (this.field01824 && minecraftClient.player != null) {
         this.render(drawContext, (int)minecraftClient.mouse.getScaledX(minecraftClient.getWindow()), (int)minecraftClient.mouse.getScaledY(minecraftClient.getWindow()), 0.0F);
         if (this.field01818 <= 0.003F) {
            this.finishOverlay();
            return false;
         } else {
            return true;
         }
      } else {
         this.finishOverlay();
         return false;
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean local1267_2) {
      if (this.field01822) {
         return true;
      }

      if (this.settingRenderContext.keybindSetting != null) {
         if (click.button() != 0) {
            this.settingRenderContext.keybindSetting.setBinding(InputBinding.mouseButton(click.button()));
         }

         this.settingRenderContext.keybindSetting = null;
         return true;
      } else {
         if (click.button() == 0 || click.button() == 1 || click.button() == 2) {
            float local1267_3 = sf();
            float local1267_4 = (float)click.x() * local1267_3;
            float local1267_5 = (float)click.y() * local1267_3;
            if (click.button() == 0) {
               this.settingRenderContext.textSetting = null;
               this.settingRenderContext.field12425 = null;
               this.settingRenderContext.module = null;
            }

            for (int index = this.settingRenderContext.field12417.size() - 1; index >= this.settingRenderContext.field12418; index--) {
               SettingRenderContext.Nested0493 local1267_7 = this.settingRenderContext.field12417.get(index);
               if (UiTheme.method2879(local1267_4, local1267_5, local1267_7.x(), local1267_7.y(), local1267_7.w(), local1267_7.h())) {
                  if (click.button() == 0) {
                     local1267_7.action().run();
                     return true;
                  }

                  if (click.button() == 1 && local1267_7.rightAction() != null) {
                     local1267_7.rightAction().run();
                     return true;
                  }

                  if (click.button() == 2 && local1267_7.middleAction() != null) {
                     local1267_7.middleAction().run();
                     return true;
                  }
               }
            }

            if (this.settingRenderContext.field12418 > 0) {
               return true;
            }
         }

         return super.mouseClicked(click, local1267_2);
      }
   }
}
