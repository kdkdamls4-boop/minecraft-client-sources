package deluxe.client.ui;

import deluxe.client.ui.panel.PotionPresetPanel;

import deluxe.client.util.InputBinding;
import deluxe.client.util.PotionPreset;
import deluxe.client.util.GuiScale;
import deluxe.client.ui.panel.PotionPresetPanel;
import deluxe.client.ui.UiTheme;
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

public final class DeluxePotionCombinerScreen extends Screen {
   private static DeluxePotionCombinerScreen deluxePotionCombinerScreen;
   private SettingRenderContext settingRenderContext = new SettingRenderContext();
   private long field00935 = System.currentTimeMillis();

   @Override
   public void render(DrawContext drawContext, int local827_2, int local827_3, float local827_4) {
      this.settingRenderContext
         .method3574(
            drawContext, Math.min(1.66F, Math.min(this.width * sf() / 640.0F, this.height * sf() / 360.0F) * 1.66F), local827_2 * sf(), local827_3 * sf()
         );
      Render2D.method4713(drawContext);
      HudRenderContext.method4333(drawContext);
      PotionPresetPanel.method1316()
         .method1330(
            this.settingRenderContext,
            this.width * sf(),
            this.height * sf(),
            PotionPresetPanel.method1316().method1328(),
            Math.min(0.05F, (float)(System.currentTimeMillis() - this.field00935) / 1000.0F)
         );
      this.field00935 = System.currentTimeMillis();
      Render2D.method4754();
      if (!PotionPresetPanel.method1316().method1333() && PotionPresetPanel.method1316().method1328() <= 0.003F) {
         MinecraftClient.getInstance().setScreen(null);
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   public static DeluxePotionCombinerScreen instance() {
      if (deluxePotionCombinerScreen == null) {
         deluxePotionCombinerScreen = new DeluxePotionCombinerScreen();
      }

      return deluxePotionCombinerScreen;
   }

   private DeluxePotionCombinerScreen() {
      super(Text.literal("Deluxe PotionCombiner"));
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local831_2, int local831_3, float local831_4) {
   }

   @Override
   public boolean mouseScrolled(double local832_1, double local832_3, double local832_5, double local832_7) {
      return PotionPresetPanel.method1316().method1318((float)local832_1 * sf(), (float)local832_3 * sf(), local832_7, this.settingRenderContext.scale);
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
   public boolean keyPressed(KeyInput keyInput) {
      if (this.settingRenderContext.keybindSetting != null) {
         this.settingRenderContext.keybindSetting.withKeyCode(keyInput.key() != 256 && keyInput.key() != 261 ? keyInput.key() : -1);
         this.settingRenderContext.keybindSetting = null;
         PotionPreset.method2166().method2167();
         return true;
      }

      if (keyInput.key() == 256) {
         if (!PotionPresetPanel.method1316().method1337()) {
            PotionPresetPanel.method1316().method1320();
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

   private static float sf() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   @Override
   public boolean mouseClicked(Click click, boolean local836_2) {
      if (this.settingRenderContext.keybindSetting != null) {
         if (click.button() != 0) {
            this.settingRenderContext.keybindSetting.setBinding(InputBinding.mouseButton(click.button()));
         }

         this.settingRenderContext.keybindSetting = null;
         PotionPreset.method2166().method2167();
         if (click.button() != 0) {
            return true;
         }
      }

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
}
