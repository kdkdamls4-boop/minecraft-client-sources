package deluxe.client.ui.panel;

import deluxe.client.ui.AnimatedValue;
import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.FontText;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.ui.UiTheme;

import deluxe.client.ui.AutoBuySettingsScreen;
import deluxe.client.render.FontFace;
import deluxe.client.render.Render2D;

public final class AutoBuyPanel {
   static final String field10510 = "ε";
   private static AutoBuyPanel autoBuyPanel;
   private AnimatedValue animatedValue = new AnimatedValue(300L).method3870(0.0);
   private boolean field10513;
   private long field10514;
   private SettingRenderContext settingRenderContext;

   public boolean method3072() {
      if (AutoBuySettingsScreen.method1139().field03133 != null) {
         this.method3088();
         return true;
      }

      if (!this.field10513) {
         return false;
      }

      this.field10513 = false;
      return true;
   }

   public void method3074() {
      if (!this.field10513) {
         AuctionItemCatalog.method1056();
         this.field10513 = true;
         this.field10514 = System.currentTimeMillis();
         AutoBuySettingsScreen.method1139().field03135 = this.field10514;
         this.method3088();
      }
   }

   static float method3076(long local4909_0, float local4909_2, float local4909_3) {
      float local4909_4 = ((float)local4909_0 / 1000.0F - local4909_2) / local4909_3;
      if (local4909_4 <= 0.0F) {
         return 0.0F;
      } else {
         return local4909_4 >= 1.0F ? 1.0F : local4909_4 * local4909_4 * (3.0F - 2.0F * local4909_4);
      }
   }

   public boolean method3077() {
      return this.field10513;
   }

   private void method3078(SettingRenderContext newSettingRenderContext, String local4911_2, String local4911_3, float local4911_4, float local4911_5) {
      Render2D.method4715(
         local4911_4,
         local4911_5,
         FontText.method1396(FontFace.SEMI, local4911_2, 4.5F * newSettingRenderContext.scale) + 6.0F * newSettingRenderContext.scale,
         9.0F * newSettingRenderContext.scale,
         2.0F * newSettingRenderContext.scale,
         ClickGuiStyle.field10828
      );
      FontText.method1397(
         FontFace.SEMI,
         local4911_2,
         local4911_4 + 3.0F * newSettingRenderContext.scale,
         local4911_5 + 2.25F * newSettingRenderContext.scale,
         4.5F * newSettingRenderContext.scale,
         UiTheme.field09686
      );
      FontText.method1397(
         FontFace.SEMI,
         local4911_3,
         local4911_4 + FontText.method1396(FontFace.SEMI, local4911_2, 4.5F * newSettingRenderContext.scale) + 10.0F * newSettingRenderContext.scale,
         local4911_5 + 2.0F * newSettingRenderContext.scale,
         5.0F * newSettingRenderContext.scale,
         UiTheme.field09682
      );
   }

   static float method3079() {
      return (Render2D.method4724(-16777216) >>> 24 & 0xFF) / 255.0F;
   }

   public static AutoBuyPanel method3080() {
      if (autoBuyPanel == null) {
         autoBuyPanel = new AutoBuyPanel();
      }

      return autoBuyPanel;
   }

   private AutoBuyPanel() {
   }

   private void method3082(SettingRenderContext newSettingRenderContext, float local4916_2, float local4916_3, boolean local4916_4, float local4916_5, float local4916_6) {
      float local4916_7 = 512.0F * newSettingRenderContext.scale;
      float local4916_8 = 300.0F * newSettingRenderContext.scale;
      float local4916_9 = (local4916_2 - local4916_7) / 2.0F;
      float local4916_10 = (local4916_3 - local4916_8) / 2.0F + local4916_6;
      long local4916_11 = System.currentTimeMillis();
      HudStyle.method4812(
         local4916_9, local4916_10, local4916_7, local4916_8, method3079(), 7.0F * newSettingRenderContext.scale, ShapeDrawing.Nested0502.LOW
      );
      if (local4916_4) {
         newSettingRenderContext.method3554(local4916_9, local4916_10, local4916_7, local4916_8, null, () -> {});
      }

      newSettingRenderContext.method3547(
         "9",
         local4916_9 + 13.0F * newSettingRenderContext.scale,
         local4916_10 + 12.0F * newSettingRenderContext.scale,
         6.6F * newSettingRenderContext.scale,
         UiTheme.field09686
      );
      FontText.method1397(
         FontFace.SEMI,
         "AutoBuy",
         local4916_9 + 21.0F * newSettingRenderContext.scale,
         local4916_10 + 9.0F * newSettingRenderContext.scale,
         6.0F * newSettingRenderContext.scale,
         UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "в списке " + AutoBuySettingsScreen.method1139().field03122.size(),
         local4916_9
            + 21.0F * newSettingRenderContext.scale
            + FontText.method1396(FontFace.SEMI, "AutoBuy", 6.0F * newSettingRenderContext.scale)
            + 5.0F * newSettingRenderContext.scale,
         local4916_10 + 9.5F * newSettingRenderContext.scale,
         5.0F * newSettingRenderContext.scale,
         UiTheme.field09682
      );
      float local4916_13 = newSettingRenderContext.method3544(
         "ab:close",
         local4916_4
            && UiTheme.method2879(
               newSettingRenderContext.mouseX,
               newSettingRenderContext.mouseY,
               local4916_9 + local4916_7 - 19.0F * newSettingRenderContext.scale,
               local4916_10 + 6.0F * newSettingRenderContext.scale,
               12.0F * newSettingRenderContext.scale,
               12.0F * newSettingRenderContext.scale
            )
      );
      if (local4916_13 > 0.005F) {
         Render2D.method4719(
            local4916_9 + local4916_7 - 13.0F * newSettingRenderContext.scale,
            local4916_10 + 12.0F * newSettingRenderContext.scale,
            6.0F * newSettingRenderContext.scale,
            UiTheme.method2877(UiTheme.field09684, local4916_13)
         );
      }

      newSettingRenderContext.method3547(
         "ε",
         local4916_9 + local4916_7 - 13.0F * newSettingRenderContext.scale,
         local4916_10 + 12.0F * newSettingRenderContext.scale,
         6.6F * newSettingRenderContext.scale,
         UiTheme.method2881(UiTheme.field09683, UiTheme.field09680, local4916_13)
      );
      if (local4916_4) {
         newSettingRenderContext.method3554(
            local4916_9 + local4916_7 - 19.0F * newSettingRenderContext.scale,
            local4916_10 + 6.0F * newSettingRenderContext.scale,
            12.0F * newSettingRenderContext.scale,
            12.0F * newSettingRenderContext.scale,
            null,
            () -> this.method3083()
         );
      }

      Render2D.method4715(
         local4916_9 + 8.0F * newSettingRenderContext.scale,
         local4916_10 + 23.5F * newSettingRenderContext.scale,
         local4916_7 - 16.0F * newSettingRenderContext.scale,
         0.5F * newSettingRenderContext.scale,
         0.0F,
         UiTheme.field09696
      );
      float local4916_14 = local4916_10 + 30.0F * newSettingRenderContext.scale;
      float local4916_15 = local4916_10 + local4916_8 - 8.0F * newSettingRenderContext.scale;
      AutoBuyItemPanel.method1102(
         newSettingRenderContext,
         AutoBuySettingsScreen.method1139(),
         local4916_9 + 8.0F * newSettingRenderContext.scale,
         local4916_14,
         300.0F * newSettingRenderContext.scale,
         17.0F * newSettingRenderContext.scale,
         local4916_4
      );
      AutoBuyItemPanel.method1117(
         newSettingRenderContext,
         AutoBuySettingsScreen.method1139(),
         this,
         local4916_9 + 8.0F * newSettingRenderContext.scale,
         local4916_14 + 21.0F * newSettingRenderContext.scale,
         300.0F * newSettingRenderContext.scale,
         local4916_15 - 14.0F * newSettingRenderContext.scale - local4916_14 - 21.0F * newSettingRenderContext.scale,
         local4916_4,
         local4916_5,
         local4916_11
      );
      this.method3078(newSettingRenderContext, "ЛКМ", "добавить или убрать", local4916_9 + 8.0F * newSettingRenderContext.scale, local4916_15 - 9.0F * newSettingRenderContext.scale);
      this.method3078(newSettingRenderContext, "ПКМ", "свои цены предмета", local4916_9 + 100.0F * newSettingRenderContext.scale, local4916_15 - 9.0F * newSettingRenderContext.scale);
      float local4916_16 = local4916_9 + 316.0F * newSettingRenderContext.scale;
      float local4916_17 = local4916_7 - 324.0F * newSettingRenderContext.scale;
      Render2D.method4705(method3076(local4916_11 - this.field10514, 0.06F, 0.3F));
      AutoBuyItemPanel.method1100(
         newSettingRenderContext,
         AutoBuySettingsScreen.method1139(),
         this,
         local4916_16,
         local4916_14,
         local4916_17,
         80.0F * newSettingRenderContext.scale,
         local4916_4,
         local4916_11
      );
      Render2D.method4650();
      Render2D.method4705(method3076(local4916_11 - this.field10514, 0.12F, 0.3F));
      AutoBuyItemPanel.method1106(
         newSettingRenderContext,
         AutoBuySettingsScreen.method1139(),
         local4916_16,
         local4916_14 + 86.0F * newSettingRenderContext.scale,
         local4916_17,
         52.0F * newSettingRenderContext.scale,
         local4916_4,
         local4916_11
      );
      Render2D.method4650();
      Render2D.method4705(method3076(local4916_11 - this.field10514, 0.18F, 0.3F));
      AutoBuyItemPanel.method1111(
         newSettingRenderContext,
         AutoBuySettingsScreen.method1139(),
         local4916_16,
         local4916_14 + 144.0F * newSettingRenderContext.scale,
         local4916_17,
         local4916_15 - local4916_14 - 144.0F * newSettingRenderContext.scale,
         local4916_5,
         local4916_11,
         this.field10514
      );
      Render2D.method4650();
   }

   public void method3083() {
      this.field10513 = false;
      this.method3088();
   }

   public boolean method3084(float local4918_1, float local4918_2, double local4918_3, float local4918_5) {
      if (AutoBuySettingsScreen.method1139().field03129 != null
         && UiTheme.method2879(
            local4918_1,
            local4918_2,
            AutoBuySettingsScreen.method1139().field03129[0],
            AutoBuySettingsScreen.method1139().field03129[1],
            AutoBuySettingsScreen.method1139().field03129[2],
            AutoBuySettingsScreen.method1139().field03129[3]
         )) {
         AutoBuySettingsScreen.method1139().field03127.targetOffset = SettingRenderContext.method3541(
            AutoBuySettingsScreen.method1139().field03127.targetOffset - (float)local4918_3 * 24.0F * local4918_5,
            0.0F,
            AutoBuySettingsScreen.method1139().field03127.maximumOffset
         );
      } else if (AutoBuySettingsScreen.method1139().field03130 != null
         && UiTheme.method2879(
            local4918_1,
            local4918_2,
            AutoBuySettingsScreen.method1139().field03130[0],
            AutoBuySettingsScreen.method1139().field03130[1],
            AutoBuySettingsScreen.method1139().field03130[2],
            AutoBuySettingsScreen.method1139().field03130[3]
         )) {
         AutoBuySettingsScreen.method1139().field03128.targetOffset = SettingRenderContext.method3541(
            AutoBuySettingsScreen.method1139().field03128.targetOffset - (float)local4918_3 * 24.0F * local4918_5,
            0.0F,
            AutoBuySettingsScreen.method1139().field03128.maximumOffset
         );
      }

      return true;
   }

   void method3085(String local4919_1) {
      if (this.settingRenderContext != null) {
         this.settingRenderContext.textSetting = null;
      }

      if (!AutoBuySettingsScreen.method1139().method1142(local4919_1)) {
         AutoBuySettingsScreen.method1139().method1140(local4919_1);
      }

      if (!local4919_1.equals(AutoBuySettingsScreen.method1139().field03133)) {
         AutoBuySettingsScreen.method1139().field03133 = local4919_1;
         AutoBuySettingsScreen.method1139().field03134 = System.currentTimeMillis();
      }
   }

   public float method3086() {
      this.animatedValue.method3869(this.field10513 ? 1.0 : 0.0);
      return (float)this.animatedValue.method3871();
   }

   public void method3087(SettingRenderContext newSettingRenderContext, float local4921_2, float local4921_3, float local4921_4, float local4921_5) {
      if (!(local4921_4 < 0.005F)) {
         this.settingRenderContext = newSettingRenderContext;
         newSettingRenderContext.field12418 = newSettingRenderContext.field12417.size();
         AutoBuySettingsScreen.method1139().method1138(System.currentTimeMillis());
         Render2D.method4715(0.0F, 0.0F, local4921_2, local4921_3, 0.0F, UiTheme.method2877(ClickGuiStyle.field10820, local4921_4));
         if (this.field10513 && local4921_4 > 0.6F) {
            newSettingRenderContext.method3554(0.0F, 0.0F, local4921_2, local4921_3, null, () -> this.method3083());
         }

         Render2D.method4705(local4921_4);
         this.method3082(
            newSettingRenderContext, local4921_2, local4921_3, this.field10513 && local4921_4 > 0.6F, local4921_5, (1.0F - local4921_4) * 12.0F * newSettingRenderContext.scale
         );
         Render2D.method4650();
      }
   }

   void method3088() {
      if (AutoBuySettingsScreen.method1139().field03133 != null) {
         if (this.settingRenderContext != null
            && AutoBuySettingsScreen.method1139().field03122.get(AutoBuySettingsScreen.method1139().field03133) != null
            && (
               this.settingRenderContext.textSetting == AutoBuySettingsScreen.method1139().field03122.get(AutoBuySettingsScreen.method1139().field03133).field03214
                  || this.settingRenderContext.textSetting == AutoBuySettingsScreen.method1139().field03122.get(AutoBuySettingsScreen.method1139().field03133).field03215
            )) {
            this.settingRenderContext.textSetting = null;
         }

         AutoBuySettingsScreen.method1139().field03133 = null;
         AutoBuySettingsScreen.method1139().field03134 = System.currentTimeMillis();
      }
   }
}
