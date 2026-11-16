package deluxe.client.ui;

import deluxe.client.util.GuiScale;
import deluxe.client.ui.UiTheme;
import deluxe.client.ui.panel.AutoBuyPanel;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.render.Render2D;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class DeluxeAutoBuyScreen extends Screen {
   private static DeluxeAutoBuyScreen deluxeAutoBuyScreen;
   private SettingRenderContext settingRenderContext = new SettingRenderContext();
   private long field14295 = System.currentTimeMillis();

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean charTyped(CharInput charInput) {
      if (this.settingRenderContext.textSetting == null) {
         return super.charTyped(charInput);
      }

      if (charInput.codepoint() >= 32) {
         this.settingRenderContext.textSetting.insertText(String.valueOf((char)charInput.codepoint()));
      }

      return true;
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local6952_2, int local6952_3, float local6952_4) {
   }

   private DeluxeAutoBuyScreen() {
      super(Text.literal("Deluxe AutoBuy"));
   }

   @Override
   public boolean mouseReleased(Click click) {
      this.settingRenderContext.numberSetting = null;
      return super.mouseReleased(click);
   }

   @Override
   public boolean mouseClicked(Click click, boolean local6955_2) {
      if (click.button() == 0) {
         this.settingRenderContext.textSetting = null;
      }

      for (int index = this.settingRenderContext.field12417.size() - 1; index >= this.settingRenderContext.field12418; index--) {
         if (UiTheme.method2879(
            (float)click.x() * sf(),
            (float)click.y() * sf(),
            this.settingRenderContext.field12417.get(index).x(),
            this.settingRenderContext.field12417.get(index).y(),
            this.settingRenderContext.field12417.get(index).w(),
            this.settingRenderContext.field12417.get(index).h()
         )) {
            if (click.button() == 0) {
               this.settingRenderContext.field12417.get(index).action().run();
               return true;
            }

            if (click.button() == 1 && this.settingRenderContext.field12417.get(index).rightAction() != null) {
               this.settingRenderContext.field12417.get(index).rightAction().run();
               return true;
            }
         }
      }

      return true;
   }

   @Override
   public boolean mouseScrolled(double local6956_1, double local6956_3, double local6956_5, double local6956_7) {
      return AutoBuyPanel.method3080().method3084((float)local6956_1 * sf(), (float)local6956_3 * sf(), local6956_7, this.settingRenderContext.scale);
   }

   private static float sf() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   @Override
   public void render(DrawContext drawContext, int local6958_2, int local6958_3, float local6958_4) {
      this.settingRenderContext
         .method3574(drawContext, Math.min(1.0F, Math.min(this.width * sf() / 640.0F, this.height * sf() / 360.0F)), local6958_2 * sf(), local6958_3 * sf());
      Render2D.method4713(drawContext);
      HudRenderContext.method4333(drawContext);
      AutoBuyPanel.method3080()
         .method3087(
            this.settingRenderContext,
            this.width * sf(),
            this.height * sf(),
            AutoBuyPanel.method3080().method3086(),
            Math.min(0.05F, (float)(System.currentTimeMillis() - this.field14295) / 1000.0F)
         );
      if (this.settingRenderContext.numberSetting != null && GLFW.glfwGetMouseButton(MinecraftClient.getInstance().getWindow().getHandle(), 0) != 1) {
         this.settingRenderContext.numberSetting = null;
      } else if (this.settingRenderContext.numberSetting != null && this.settingRenderContext.field12415.get(this.settingRenderContext.numberSetting) != null) {
         this.settingRenderContext
            .numberSetting
            .setFraction(
               SettingRenderContext.method3541(
                  (local6958_2 * sf() - this.settingRenderContext.field12415.get(this.settingRenderContext.numberSetting)[0])
                     / this.settingRenderContext.field12415.get(this.settingRenderContext.numberSetting)[1],
                  0.0F,
                  1.0F
               )
            );
      }

      this.field14295 = System.currentTimeMillis();
      Render2D.method4754();
      if (!AutoBuyPanel.method3080().method3077() && AutoBuyPanel.method3080().method3086() <= 0.003F) {
         MinecraftClient.getInstance().setScreen(null);
      }
   }

   public static DeluxeAutoBuyScreen instance() {
      if (deluxeAutoBuyScreen == null) {
         deluxeAutoBuyScreen = new DeluxeAutoBuyScreen();
      }

      return deluxeAutoBuyScreen;
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (keyInput.key() == 256) {
         if (!AutoBuyPanel.method3080().method3072()) {
            AutoBuyPanel.method3080().method3083();
         }

         return true;
      } else {
         if (this.settingRenderContext.textSetting == null) {
            return super.keyPressed(keyInput);
         }

         if (keyInput.key() == 259) {
            this.settingRenderContext.textSetting.backspace();
         } else if (keyInput.key() == 261) {
            this.settingRenderContext.textSetting.deleteForward();
         } else if (keyInput.key() == 263) {
            this.settingRenderContext.textSetting.moveCaret(-1, (keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 262) {
            this.settingRenderContext.textSetting.moveCaret(1, (keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 268) {
            this.settingRenderContext.textSetting.moveToStart((keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 269) {
            this.settingRenderContext.textSetting.moveToEnd((keyInput.modifiers() & 1) != 0);
         } else if (keyInput.key() == 65 && (keyInput.modifiers() & 2) != 0) {
            this.settingRenderContext.textSetting.selectAll();
         } else if (keyInput.key() == 257) {
            this.settingRenderContext.textSetting = null;
         }

         return true;
      }
   }
}
