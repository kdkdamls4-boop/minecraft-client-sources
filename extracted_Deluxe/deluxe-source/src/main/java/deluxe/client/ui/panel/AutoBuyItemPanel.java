package deluxe.client.ui.panel;

import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.storage.AutoBuyItem;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.render.RoundedClipStack;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.ui.UiTheme;

import deluxe.client.ui.AutoBuySettingsScreen;
import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.render.Render2D;
import deluxe.client.setting.TextSetting;

import java.util.ArrayList;

final class AutoBuyItemPanel {
   private static final String[] field02627 = new String[]{"500K", "250K", "100K", "50K", "0"};
   private static String field02628;

   static void method1100(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float local1519_3,
      float local1519_4,
      float local1519_5,
      float local1519_6,
      boolean local1519_7,
      long local1519_8
   ) {
      settingRenderContext.method3564(
         local1519_3, local1519_4, local1519_5, local1519_6, 4.0F * settingRenderContext.scale, ClickGuiStyle.field10826, UiTheme.field09696
      );
      boolean local1519_10 = autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null;
      if (local1519_10) {
         field02628 = autoBuySettingsScreen.field03133;
      }

      FontText.method1397(
         FontFace.SEMI,
         "Цены",
         local1519_3 + 7.0F * settingRenderContext.scale,
         local1519_4 + 7.0F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09680
      );
      Render2D.method4705(settingRenderContext.method3544("ab:pricehint", !local1519_10));
      FontText.method1397(
         FontFace.SEMI,
         "для всех предметов",
         local1519_3
            + 7.0F * settingRenderContext.scale
            + FontText.method1396(FontFace.SEMI, "Цены", 5.5F * settingRenderContext.scale)
            + 4.0F * settingRenderContext.scale,
         local1519_4 + 7.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.field09682
      );
      Render2D.method4650();
      method1115(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuyPanel,
         local1519_3,
         local1519_4 - 1.5F * settingRenderContext.scale,
         local1519_5 + 1.0F * settingRenderContext.scale,
         local1519_7,
         local1519_10
      );
      float local1519_11 = AutoBuyPanel.method3076(local1519_8 - autoBuySettingsScreen.field03134, 0.0F, 0.16F);
      float local1519_12 = 92.0F * settingRenderContext.scale;
      float local1519_13 = 13.0F * settingRenderContext.scale;
      float local1519_14 = local1519_3 + local1519_5 - 7.0F * settingRenderContext.scale - local1519_12;
      Render2D.method4705(local1519_11);
      FontText.method1397(
         FontFace.SEMI,
         "Покупка до",
         local1519_3 + 7.0F * settingRenderContext.scale,
         local1519_4 + 24.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09681
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         local1519_10 ? autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133).field03214 : autoBuySettingsScreen.field03117,
         local1519_10 ? "item:buy:" + autoBuySettingsScreen.field03133 : "buyMax",
         "$",
         null,
         local1519_14,
         local1519_4 + 24.0F * settingRenderContext.scale + (1.0F - local1519_11) * 2.0F * settingRenderContext.scale,
         local1519_12,
         local1519_13,
         local1519_7,
         local1519_8
      );
      FontText.method1397(
         FontFace.SEMI,
         "Продажа от",
         local1519_3 + 7.0F * settingRenderContext.scale,
         local1519_4 + 42.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09681
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         local1519_10 ? autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133).field03215 : autoBuySettingsScreen.field03118,
         local1519_10 ? "item:sell:" + autoBuySettingsScreen.field03133 : "sellMin",
         "$",
         null,
         local1519_14,
         local1519_4 + 42.0F * settingRenderContext.scale + (1.0F - local1519_11) * 2.0F * settingRenderContext.scale,
         local1519_12,
         local1519_13,
         local1519_7,
         local1519_8
      );
      Render2D.method4650();
      FontText.method1397(
         FontFace.SEMI,
         "Прочность от",
         local1519_3 + 7.0F * settingRenderContext.scale,
         local1519_4 + 60.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09681
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03119,
         "durability",
         null,
         "%",
         local1519_14,
         local1519_4 + 60.0F * settingRenderContext.scale,
         local1519_12,
         local1519_13,
         local1519_7,
         local1519_8
      );
   }

   static void method1101(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float local1520_3,
      float local1520_4,
      float local1520_5,
      float local1520_6,
      boolean local1520_7,
      long local1520_8
   ) {
      settingRenderContext.method3557(
         local1520_3, local1520_4, local1520_5, local1520_6, 4.0F * settingRenderContext.scale, ClickGuiStyle.field10826, UiTheme.field09696
      );
      FontText.method1397(
         FontFace.SEMI,
         "Pricing Configuration",
         local1520_3 + 8.0F * settingRenderContext.scale,
         local1520_4 + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.field09682
      );
      if (autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null) {
         field02628 = autoBuySettingsScreen.field03133;
      }

      method1115(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuyPanel,
         local1520_3,
         local1520_4,
         local1520_5,
         local1520_7,
         autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null
      );
      method1116(
         settingRenderContext,
         "Auto Buy Speed",
         "Select scan frequency and execution speed",
         local1520_3 + 8.0F * settingRenderContext.scale,
         local1520_4 + 22.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      method1113(
         settingRenderContext,
         autoBuySettingsScreen,
         local1520_3 + 98.0F * settingRenderContext.scale,
         local1520_4 + 25.0F * settingRenderContext.scale,
         local1520_5 - 106.0F * settingRenderContext.scale,
         local1520_7
      );
      Render2D.method4705(AutoBuyPanel.method3076(local1520_8 - autoBuySettingsScreen.field03134, 0.0F, 0.16F));
      method1116(
         settingRenderContext,
         "Buy Price (Max)",
         "Maximum price limit for automatic purchases",
         local1520_3 + 8.0F * settingRenderContext.scale,
         local1520_4
            + 48.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.method3076(local1520_8 - autoBuySettingsScreen.field03134, 0.0F, 0.16F)) * 2.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null
            ? autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133).field03214
            : autoBuySettingsScreen.field03117,
         autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null ? "item:buy:" + autoBuySettingsScreen.field03133 : "buyMax",
         "$",
         null,
         local1520_3 + 98.0F * settingRenderContext.scale,
         local1520_4
            + 51.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.method3076(local1520_8 - autoBuySettingsScreen.field03134, 0.0F, 0.16F)) * 2.0F * settingRenderContext.scale,
         local1520_5 - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         local1520_7,
         local1520_8
      );
      Render2D.method4650();
      Render2D.method4705(AutoBuyPanel.method3076(local1520_8 - autoBuySettingsScreen.field03134, 0.04F, 0.16F));
      method1116(
         settingRenderContext,
         "Sell Price (Min)",
         "Minimum listing price for automatic reselling",
         local1520_3 + 8.0F * settingRenderContext.scale,
         local1520_4
            + 74.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.method3076(local1520_8 - autoBuySettingsScreen.field03134, 0.04F, 0.16F)) * 2.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null
            ? autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133).field03215
            : autoBuySettingsScreen.field03118,
         autoBuySettingsScreen.field03133 != null && autoBuySettingsScreen.field03122.get(autoBuySettingsScreen.field03133) != null ? "item:sell:" + autoBuySettingsScreen.field03133 : "sellMin",
         "$",
         null,
         local1520_3 + 98.0F * settingRenderContext.scale,
         local1520_4
            + 77.0F * settingRenderContext.scale
            + (1.0F - AutoBuyPanel.method3076(local1520_8 - autoBuySettingsScreen.field03134, 0.04F, 0.16F)) * 2.0F * settingRenderContext.scale,
         local1520_5 - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         local1520_7,
         local1520_8
      );
      Render2D.method4650();
      method1116(
         settingRenderContext,
         "Minimum Durability",
         "Items below this durability will be ignored",
         local1520_3 + 8.0F * settingRenderContext.scale,
         local1520_4 + 100.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03119,
         "durability",
         null,
         "%",
         local1520_3 + 98.0F * settingRenderContext.scale,
         local1520_4 + 103.0F * settingRenderContext.scale,
         local1520_5 - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         local1520_7,
         local1520_8
      );
   }

   static void method1102(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float local1521_2,
      float local1521_3,
      float local1521_4,
      float local1521_5,
      boolean local1521_6
   ) {
      settingRenderContext.method3564(
         local1521_2,
         local1521_3,
         local1521_4,
         local1521_5,
         4.0F * settingRenderContext.scale,
         ClickGuiStyle.field10826,
         UiTheme.method2881(
            UiTheme.field09696,
            UiTheme.field09697,
            settingRenderContext.method3544("ab:searchf", settingRenderContext.textSetting == autoBuySettingsScreen.field03115)
         )
      );
      settingRenderContext.method3547(
         "R",
         local1521_2 + 8.0F * settingRenderContext.scale + 2.75F * settingRenderContext.scale,
         local1521_3 + local1521_5 / 2.0F,
         5.5F * settingRenderContext.scale,
         UiTheme.method2881(
            UiTheme.field09682,
            UiTheme.field09681,
            settingRenderContext.method3544("ab:searchf", settingRenderContext.textSetting == autoBuySettingsScreen.field03115)
         )
      );
      FontText.method1397(
         FontFace.SEMI,
         (
               autoBuySettingsScreen.field03115.getValue().isEmpty() && settingRenderContext.textSetting != autoBuySettingsScreen.field03115
                  ? autoBuySettingsScreen.field03115.getPlaceholder()
                  : autoBuySettingsScreen.field03115.getValue()
            )
            + (settingRenderContext.textSetting == autoBuySettingsScreen.field03115 ? SettingRenderContext.method3542() : ""),
         local1521_2 + 8.0F * settingRenderContext.scale + 5.5F * settingRenderContext.scale + 4.0F * settingRenderContext.scale,
         local1521_3 + local1521_5 / 2.0F - 2.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         autoBuySettingsScreen.field03115.getValue().isEmpty() ? UiTheme.field09681 : UiTheme.field09680
      );
      if (local1521_6) {
         settingRenderContext.method3554(local1521_2, local1521_3, local1521_4, local1521_5, null, () -> {
            settingRenderContext.textSetting = autoBuySettingsScreen.field03115;
            autoBuySettingsScreen.field03115.moveToEnd(false);
         });
      }
   }

   static void method1106(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float local1525_2,
      float local1525_3,
      float local1525_4,
      float local1525_5,
      boolean local1525_6,
      long local1525_7
   ) {
      settingRenderContext.method3564(
         local1525_2, local1525_3, local1525_4, local1525_5, 4.0F * settingRenderContext.scale, ClickGuiStyle.field10826, UiTheme.field09696
      );
      FontText.method1397(
         FontFace.SEMI,
         "Telegram",
         local1525_2 + 7.0F * settingRenderContext.scale,
         local1525_3 + 7.0F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "уведомления о покупках",
         local1525_2
            + 7.0F * settingRenderContext.scale
            + FontText.method1396(FontFace.SEMI, "Telegram", 5.5F * settingRenderContext.scale)
            + 4.0F * settingRenderContext.scale,
         local1525_3 + 7.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.field09682
      );
      float local1525_9 = 92.0F * settingRenderContext.scale;
      float local1525_10 = 13.0F * settingRenderContext.scale;
      float local1525_11 = local1525_2 + local1525_4 - 7.0F * settingRenderContext.scale - local1525_9;
      FontText.method1397(
         FontFace.SEMI,
         "Токен бота",
         local1525_2 + 7.0F * settingRenderContext.scale,
         local1525_3 + 20.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09681
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03120,
         "tgToken",
         null,
         null,
         local1525_11,
         local1525_3 + 20.0F * settingRenderContext.scale,
         local1525_9,
         local1525_10,
         local1525_6,
         local1525_7
      );
      FontText.method1397(
         FontFace.SEMI,
         "ID чата",
         local1525_2 + 7.0F * settingRenderContext.scale,
         local1525_3 + 36.0F * settingRenderContext.scale + 3.75F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09681
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03121,
         "tgChat",
         null,
         null,
         local1525_11,
         local1525_3 + 36.0F * settingRenderContext.scale,
         local1525_9,
         local1525_10,
         local1525_6,
         local1525_7
      );
   }

   static void method1110(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float local1529_2,
      float local1529_3,
      float local1529_4,
      float local1529_5,
      boolean local1529_6,
      long local1529_7
   ) {
      settingRenderContext.method3557(
         local1529_2, local1529_3, local1529_4, local1529_5, 4.0F * settingRenderContext.scale, ClickGuiStyle.field10826, UiTheme.field09696
      );
      FontText.method1397(
         FontFace.SEMI,
         "Bot API Configuration",
         local1529_2 + 8.0F * settingRenderContext.scale,
         local1529_3 + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.field09682
      );
      method1116(
         settingRenderContext,
         "Telegram Bot Token",
         "API token obtained from @BotFather",
         local1529_2 + 8.0F * settingRenderContext.scale,
         local1529_3 + 22.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03120,
         "tgToken",
         null,
         null,
         local1529_2 + 98.0F * settingRenderContext.scale,
         local1529_3 + 22.0F * settingRenderContext.scale,
         local1529_4 - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         local1529_6,
         local1529_7
      );
      method1116(
         settingRenderContext,
         "Telegram Chat ID",
         "Target chat or channel ID for alerts",
         local1529_2 + 8.0F * settingRenderContext.scale,
         local1529_3 + 42.0F * settingRenderContext.scale,
         86.0F * settingRenderContext.scale
      );
      method1112(
         settingRenderContext,
         autoBuySettingsScreen,
         autoBuySettingsScreen.field03121,
         "tgChat",
         null,
         null,
         local1529_2 + 98.0F * settingRenderContext.scale,
         local1529_3 + 42.0F * settingRenderContext.scale,
         local1529_4 - 106.0F * settingRenderContext.scale,
         14.0F * settingRenderContext.scale,
         local1529_6,
         local1529_7
      );
   }

   static void method1111(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float local1530_2,
      float local1530_3,
      float local1530_4,
      float local1530_5,
      float local1530_6,
      long local1530_7,
      long local1530_9
   ) {
      settingRenderContext.method3564(
         local1530_2, local1530_3, local1530_4, local1530_5, 4.0F * settingRenderContext.scale, ClickGuiStyle.field10826, UiTheme.field09696
      );
      FontText.method1397(
         FontFace.SEMI,
         "История покупок",
         local1530_2 + 7.0F * settingRenderContext.scale,
         local1530_3 + 7.0F * settingRenderContext.scale,
         5.5F * settingRenderContext.scale,
         UiTheme.field09680
      );
      float local1530_11 = local1530_3 + 22.0F * settingRenderContext.scale;
      float local1530_12 = local1530_3 + local1530_5 - 8.0F * settingRenderContext.scale - local1530_11;
      float local1530_13 = local1530_4 - 20.0F * settingRenderContext.scale;
      autoBuySettingsScreen.field03130 = new float[]{local1530_2, local1530_11, local1530_4, local1530_12};
      autoBuySettingsScreen.field03128.maximumOffset = Math.max(
         0.0F, autoBuySettingsScreen.field03123.size() * 13.0F * settingRenderContext.scale - 6.0F * settingRenderContext.scale - local1530_12
      );
      autoBuySettingsScreen.field03128.update(local1530_6);
      if (autoBuySettingsScreen.field03123.isEmpty()) {
         FontText.method1397(
            FontFace.SEMI,
            "Покупок пока нет",
            local1530_2 + (local1530_4 - FontText.method1396(FontFace.SEMI, "Покупок пока нет", 5.0F * settingRenderContext.scale)) / 2.0F,
            local1530_11 + local1530_12 / 2.0F - 2.5F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.field09683
         );
      }

      Render2D.method4678(
         settingRenderContext.drawContext,
         local1530_2 + 6.0F * settingRenderContext.scale,
         local1530_11 - 1.0F * settingRenderContext.scale,
         local1530_4 - 12.0F * settingRenderContext.scale,
         local1530_12 + 2.0F * settingRenderContext.scale
      );

      for (int index = 0; index < autoBuySettingsScreen.field03123.size(); index++) {
         float local1530_15 = local1530_11 + index * 13.0F * settingRenderContext.scale - autoBuySettingsScreen.field03128.getPixelAlignedOffset();
         if (!(local1530_15 + 7.0F * settingRenderContext.scale < local1530_11 - 2.0F) && !(local1530_15 > local1530_11 + local1530_12 + 2.0F)) {
            float local1530_16 = AutoBuyPanel.method3076(local1530_7 - local1530_9, 0.22F + Math.min(4, index) * 0.06F, 0.25F);
            if (!(local1530_16 < 0.005F)) {
               Render2D.method4705(local1530_16);
               ItemRenderCache.method2336(
                  autoBuySettingsScreen.field03123.get(index).item().displayStack(),
                  local1530_2 + 8.0F * settingRenderContext.scale + (1.0F - local1530_16) * 4.0F * settingRenderContext.scale,
                  local1530_15 + 0.75F * settingRenderContext.scale,
                  5.5F * settingRenderContext.scale,
                  ItemDrawOptions.noDecorations(AutoBuyPanel.method3079())
               );
               Render2D.method4678(
                  settingRenderContext.drawContext,
                  local1530_2 + 17.5F * settingRenderContext.scale,
                  local1530_15 - 1.0F * settingRenderContext.scale,
                  75.0F * settingRenderContext.scale,
                  9.0F * settingRenderContext.scale
               );
               FontText.method1397(
                  FontFace.SEMI,
                  autoBuySettingsScreen.field03123.get(index).item().name(),
                  local1530_2 + 17.5F * settingRenderContext.scale + (1.0F - local1530_16) * 4.0F * settingRenderContext.scale,
                  local1530_15,
                  5.5F * settingRenderContext.scale,
                  UiTheme.field09680
               );
               FontText.method1397(
                  FontFace.SEMI,
                  "x" + autoBuySettingsScreen.field03123.get(index).count(),
                  local1530_2
                     + 19.5F * settingRenderContext.scale
                     + (1.0F - local1530_16) * 4.0F * settingRenderContext.scale
                     + FontText.method1396(
                        FontFace.SEMI, autoBuySettingsScreen.field03123.get(index).item().name(), 5.5F * settingRenderContext.scale
                     ),
                  local1530_15 + 0.5F * settingRenderContext.scale,
                  5.0F * settingRenderContext.scale,
                  UiTheme.field09681
               );
               Render2D.method4661(settingRenderContext.drawContext);
               FontText.method1397(
                  FontFace.SEMI,
                  autoBuySettingsScreen.field03123.get(index).time(),
                  local1530_2 + 95.0F * settingRenderContext.scale,
                  local1530_15,
                  5.5F * settingRenderContext.scale,
                  UiTheme.field09682
               );
               FontText.method1397(
                  FontFace.SEMI,
                  AutoBuySettingsScreen.method1134(autoBuySettingsScreen.field03123.get(index).priceCoins()),
                  local1530_2
                     + 8.0F * settingRenderContext.scale
                     + local1530_13
                     - FontText.method1396(
                        FontFace.SEMI,
                        AutoBuySettingsScreen.method1134(autoBuySettingsScreen.field03123.get(index).priceCoins()),
                        5.5F * settingRenderContext.scale
                     ),
                  local1530_15,
                  5.5F * settingRenderContext.scale,
                  UiTheme.field09680
               );
               FontText.method1397(
                  FontFace.SEMI,
                  "$",
                  local1530_2
                     + 8.0F * settingRenderContext.scale
                     + local1530_13
                     - FontText.method1396(
                        FontFace.SEMI,
                        AutoBuySettingsScreen.method1134(autoBuySettingsScreen.field03123.get(index).priceCoins()),
                        5.5F * settingRenderContext.scale
                     )
                     - FontText.method1396(FontFace.SEMI, "$", 5.5F * settingRenderContext.scale),
                  local1530_15,
                  5.5F * settingRenderContext.scale,
                  UiTheme.field09686
               );
               Render2D.method4650();
            }
         }
      }

      Render2D.method4661(settingRenderContext.drawContext);
      Render2D.method4715(
         local1530_2 + local1530_4 - 5.0F * settingRenderContext.scale,
         local1530_11,
         1.0F * settingRenderContext.scale,
         local1530_12,
         0.5F * settingRenderContext.scale,
         UiTheme.field09696
      );
      if (autoBuySettingsScreen.field03128.maximumOffset > 0.0F) {
         Render2D.method4715(
            local1530_2 + local1530_4 - 5.0F * settingRenderContext.scale,
            local1530_11
               + (local1530_12 - Math.max(6.0F * settingRenderContext.scale, local1530_12 * (local1530_12 / (local1530_12 + autoBuySettingsScreen.field03128.maximumOffset))))
                  * (autoBuySettingsScreen.field03128.getPixelAlignedOffset() / autoBuySettingsScreen.field03128.maximumOffset),
            1.0F * settingRenderContext.scale,
            Math.max(6.0F * settingRenderContext.scale, local1530_12 * (local1530_12 / (local1530_12 + autoBuySettingsScreen.field03128.maximumOffset))),
            0.5F * settingRenderContext.scale,
            UiTheme.field09684
         );
      }
   }

   private AutoBuyItemPanel() {
   }

   static void method1112(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      TextSetting newTextSetting,
      String local1533_3,
      String local1533_4,
      String local1533_5,
      float local1533_6,
      float local1533_7,
      float local1533_8,
      float local1533_9,
      boolean local1533_10,
      long local1533_11
   ) {
      Render2D.method4715(local1533_6, local1533_7, local1533_8, local1533_9, 2.0F * settingRenderContext.scale, ClickGuiStyle.field10826);
      Render2D.method4769(
         local1533_6,
         local1533_7,
         local1533_8,
         local1533_9,
         2.0F * settingRenderContext.scale,
         0.5F,
         UiTheme.method2881(
            UiTheme.field09696,
            UiTheme.field09697,
            settingRenderContext.method3544("ab:inf:" + local1533_3, settingRenderContext.textSetting == newTextSetting)
         )
      );
      Render2D.method4715(
         local1533_6 + local1533_8 - 14.5F * settingRenderContext.scale,
         local1533_7 + 1.0F * settingRenderContext.scale,
         0.5F * settingRenderContext.scale,
         local1533_9 - 2.0F * settingRenderContext.scale,
         0.0F,
         UiTheme.field09696
      );
      String local1533_13 = settingRenderContext.textSetting == newTextSetting
         ? (local1533_4 == null ? "" : local1533_4)
            + (
               newTextSetting.getValue().isEmpty()
                  ? ""
                  : (
                     local1533_4 == null && local1533_5 == null
                        ? newTextSetting.getValue()
                        : AutoBuySettingsScreen.method1134(AutoBuySettingsScreen.method1135(newTextSetting.getValue()))
                  )
            )
            + SettingRenderContext.method3542()
            + (local1533_5 == null ? "" : local1533_5)
         : (
            newTextSetting.getValue().isEmpty()
               ? newTextSetting.getPlaceholder()
               : (
                  local1533_4 == null && local1533_5 == null
                     ? newTextSetting.getValue()
                     : (local1533_4 == null ? "" : local1533_4)
                        + AutoBuySettingsScreen.method1134(AutoBuySettingsScreen.method1135(newTextSetting.getValue()))
                        + (local1533_5 == null ? "" : local1533_5)
               )
         );
      Render2D.method4678(
         settingRenderContext.drawContext,
         local1533_6 + 5.0F * settingRenderContext.scale,
         local1533_7,
         local1533_8 - 14.5F * settingRenderContext.scale - 8.0F * settingRenderContext.scale,
         local1533_9
      );
      FontText.method1397(
         FontFace.SEMI,
         local1533_13,
         settingRenderContext.textSetting == newTextSetting
               && FontText.method1396(FontFace.SEMI, local1533_13, 5.0F * settingRenderContext.scale)
                  > local1533_8 - 14.5F * settingRenderContext.scale - 8.0F * settingRenderContext.scale
            ? local1533_6
               + local1533_8
               - 17.5F * settingRenderContext.scale
               - FontText.method1396(FontFace.SEMI, local1533_13, 5.0F * settingRenderContext.scale)
            : local1533_6 + 5.0F * settingRenderContext.scale,
         local1533_7 + local1533_9 / 2.0F - 2.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         newTextSetting.getValue().isEmpty() && settingRenderContext.textSetting != newTextSetting ? UiTheme.field09682 : UiTheme.field09680
      );
      Render2D.method4661(settingRenderContext.drawContext);
      float local1533_14 = autoBuySettingsScreen.field03126.get(local1533_3) != null && (float)(local1533_11 - autoBuySettingsScreen.field03126.get(local1533_3)) / 400.0F < 1.0F
         ? 1.0F - (float)(local1533_11 - autoBuySettingsScreen.field03126.get(local1533_3)) / 400.0F
         : 0.0F;
      if (autoBuySettingsScreen.field03126.get(local1533_3) != null && local1533_14 <= 0.0F) {
         autoBuySettingsScreen.field03126.remove(local1533_3);
      }

      Render2D.method4715(
         local1533_6 + local1533_8 - 9.5F * settingRenderContext.scale,
         local1533_7 + local1533_9 / 2.0F - 2.5F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         1.5F * settingRenderContext.scale,
         UiTheme.method2881(
            UiTheme.method2881(
               UiTheme.field09686,
               UiTheme.field09688,
               settingRenderContext.method3544(
                  "ab:int:" + local1533_3,
                  local1533_10
                     && UiTheme.method2879(
                        settingRenderContext.mouseX,
                        settingRenderContext.mouseY,
                        local1533_6 + local1533_8 - 14.0F * settingRenderContext.scale,
                        local1533_7,
                        14.0F * settingRenderContext.scale,
                        local1533_9
                     )
               )
            ),
            UiTheme.field09680,
            local1533_14
         )
      );
      Render2D.method4715(
         local1533_6 + local1533_8 - 9.5F * settingRenderContext.scale + 1.1F * settingRenderContext.scale,
         local1533_7 + local1533_9 / 2.0F - 2.5F * settingRenderContext.scale + 1.1F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale - 2.2F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale - 2.2F * settingRenderContext.scale,
         0.9F * settingRenderContext.scale,
         -1
      );
      Render2D.method4719(
         local1533_6 + local1533_8 - 7.0F * settingRenderContext.scale, local1533_7 + local1533_9 / 2.0F, 0.7F * settingRenderContext.scale, -3158318
      );
      if (local1533_10) {
         settingRenderContext.method3554(local1533_6, local1533_7, local1533_8 - 14.0F * settingRenderContext.scale, local1533_9, null, () -> {
            settingRenderContext.textSetting = newTextSetting;
            newTextSetting.moveToEnd(false);
         });
         settingRenderContext.method3554(
            local1533_6 + local1533_8 - 14.0F * settingRenderContext.scale, local1533_7, 14.0F * settingRenderContext.scale, local1533_9, null, () -> {
               if (settingRenderContext.textSetting == newTextSetting) {
                  settingRenderContext.textSetting = null;
               }

               autoBuySettingsScreen.field03126.put(local1533_3, System.currentTimeMillis());
            }
         );
      }
   }

   private static void method1113(
      SettingRenderContext settingRenderContext, AutoBuySettingsScreen autoBuySettingsScreen, float local1534_2, float local1534_3, float local1534_4, boolean local1534_5
   ) {
      if (Math.max(0, Math.min(2, autoBuySettingsScreen.numberSetting.intValue())) != 2) {
         FontText.method1397(
            FontFace.SEMI,
            AutoBuySettingsScreen.field03110[Math.max(0, Math.min(2, autoBuySettingsScreen.numberSetting.intValue()))],
            local1534_2,
            local1534_3,
            4.5F * settingRenderContext.scale,
            UiTheme.field09681
         );
      }

      FontText.method1397(
         FontFace.SEMI,
         AutoBuySettingsScreen.field03110[2],
         local1534_2 + local1534_4 - FontText.method1396(FontFace.SEMI, AutoBuySettingsScreen.field03110[2], 4.5F * settingRenderContext.scale),
         local1534_3,
         4.5F * settingRenderContext.scale,
         UiTheme.field09680
      );
      Render2D.method4715(
         local1534_2,
         local1534_3 + 7.0F * settingRenderContext.scale,
         local1534_4,
         7.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         ClickGuiStyle.field10827
      );
      Render2D.method4715(
         local1534_2,
         local1534_3 + 7.0F * settingRenderContext.scale + 0.15F,
         local1534_2
            + 1.0F * settingRenderContext.scale
            + settingRenderContext.method3560("ab:speed", autoBuySettingsScreen.numberSetting.getFraction())
               * (local1534_4 - 2.0F * settingRenderContext.scale - 5.0F * settingRenderContext.scale)
            + 6.0F * settingRenderContext.scale
            - local1534_2,
         7.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         UiTheme.method2881(
            UiTheme.field09686,
            UiTheme.method2881(UiTheme.field09692, UiTheme.field09686, 0.3F),
            settingRenderContext.method3567(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2)
         )
      );
      if (settingRenderContext.method3567(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2) > 0.005F) {
         Render2D.method4764(
            SettingRenderContext.method3562(local1534_2),
            SettingRenderContext.method3562((local1534_3 + 7.0F * settingRenderContext.scale) * settingRenderContext.scale),
            local1534_4,
            7.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)) / 2.0F,
            settingRenderContext.method3567(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2),
            UiTheme.field09686
         );
      }

      if (settingRenderContext.method3567(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2) > 0.005F) {
         Render2D.method4678(
            settingRenderContext.drawContext, local1534_2, local1534_3 + 7.0F * settingRenderContext.scale, local1534_4, 7.0F * settingRenderContext.scale
         );
         RoundedClipStack.method4836(
            local1534_2,
            local1534_3 + 7.0F * settingRenderContext.scale,
            local1534_4,
            7.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale
         );

         for (int index = 0; index * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)) < local1534_4; index++) {
            for (int index2 = 0; index2 * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)) < 7.0F * settingRenderContext.scale; index2++) {
               if ((index + index2 + (int)(System.currentTimeMillis() / 90L)) % 2 != 1) {
                  Render2D.method4715(
                     local1534_2 + index * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     local1534_3 + 7.0F * settingRenderContext.scale + index2 * Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     Math.max(2.0F, Math.round(3.0F * settingRenderContext.scale)),
                     0.0F,
                     UiTheme.method2877(
                        UiTheme.field09686,
                        settingRenderContext.method3567(autoBuySettingsScreen.numberSetting, autoBuySettingsScreen.numberSetting.intValue() == 2)
                           * (0.28F + 0.34F * (float)Math.sin(System.currentTimeMillis() / 260.0 + index * 0.55F))
                     )
                  );
               }
            }
         }

         RoundedClipStack.method4838();
         Render2D.method4661(settingRenderContext.drawContext);
      }

      for (int index3 = 0; index3 < 3; index3++) {
         Render2D.method4715(
            local1534_2 + 1.5F * settingRenderContext.scale + index3 * (local1534_4 - 5.0F * settingRenderContext.scale) / 2.0F,
            local1534_3 + 10.5F * settingRenderContext.scale - 1.0F * settingRenderContext.scale + 0.25F,
            2.0F * settingRenderContext.scale,
            2.0F * settingRenderContext.scale,
            1.0F * settingRenderContext.scale,
            UiTheme.field09683
         );
      }

      Render2D.method4715(
         local1534_2
            + 1.0F * settingRenderContext.scale
            + settingRenderContext.method3560("ab:speed", autoBuySettingsScreen.numberSetting.getFraction()) * (local1534_4 - 7.0F * settingRenderContext.scale),
         local1534_3 + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         1.0F * settingRenderContext.scale,
         UiTheme.field09689
      );
      if (local1534_5) {
         settingRenderContext.field12415.put(autoBuySettingsScreen.numberSetting, new float[]{local1534_2, local1534_4});
         settingRenderContext.method3554(
            local1534_2,
            local1534_3 + 5.0F * settingRenderContext.scale,
            local1534_4,
            11.0F * settingRenderContext.scale,
            null,
            () -> settingRenderContext.numberSetting = autoBuySettingsScreen.numberSetting
         );
      }
   }

   private static void method1115(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float local1536_3,
      float local1536_4,
      float local1536_5,
      boolean local1536_6,
      boolean local1536_7
   ) {
      if (!(settingRenderContext.method3544("ab:editmode", local1536_7) < 0.005F) && field02628 != null && AuctionItemCatalog.method1055(field02628) != null) {
         float local1536_8 = 3.0F * settingRenderContext.scale
            + 5.5F * settingRenderContext.scale
            + 3.0F * settingRenderContext.scale
            + FontText.method1396(FontFace.SEMI, field02628, 5.0F * settingRenderContext.scale)
            + 4.0F * settingRenderContext.scale
            + 5.0F * settingRenderContext.scale
            + 3.0F * settingRenderContext.scale;
         float local1536_9 = local1536_3
            + local1536_5
            - 8.0F * settingRenderContext.scale
            - local1536_8
            + (1.0F - settingRenderContext.method3544("ab:editmode", local1536_7)) * 8.0F * settingRenderContext.scale;
         float local1536_10 = local1536_4 + 8.0F * settingRenderContext.scale + 2.5F * settingRenderContext.scale - 5.0F * settingRenderContext.scale;
         Render2D.method4705(settingRenderContext.method3544("ab:editmode", local1536_7));
         Render2D.method4715(
            local1536_9, local1536_10, local1536_8, 10.0F * settingRenderContext.scale, 2.0F * settingRenderContext.scale, ClickGuiStyle.field10828
         );
         ItemRenderCache.method2336(
            AuctionItemCatalog.method1055(field02628).displayStack(),
            local1536_9 + 3.0F * settingRenderContext.scale,
            local1536_10 + 2.25F * settingRenderContext.scale,
            5.5F * settingRenderContext.scale,
            ItemDrawOptions.noDecorations(AutoBuyPanel.method3079())
         );
         FontText.method1397(
            FontFace.SEMI,
            field02628,
            local1536_9 + 11.5F * settingRenderContext.scale,
            local1536_10 + 2.0F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.field09680
         );
         settingRenderContext.method3547(
            "ε",
            local1536_9 + local1536_8 - 5.5F * settingRenderContext.scale,
            local1536_10 + 5.0F * settingRenderContext.scale,
            5.5F * settingRenderContext.scale,
            UiTheme.method2881(
               UiTheme.field09682,
               UiTheme.field09680,
               settingRenderContext.method3544(
                  "ab:editx",
                  local1536_6
                     && local1536_7
                     && UiTheme.method2879(
                        settingRenderContext.mouseX, settingRenderContext.mouseY, local1536_9, local1536_10, local1536_8, 10.0F * settingRenderContext.scale
                     )
               )
            )
         );
         Render2D.method4650();
         if (local1536_6 && local1536_7) {
            settingRenderContext.method3554(local1536_9, local1536_10, local1536_8, 10.0F * settingRenderContext.scale, null, () -> autoBuyPanel.method3088());
         }
      }
   }

   static void method1116(SettingRenderContext settingRenderContext, String local1537_1, String local1537_2, float local1537_3, float local1537_4, float local1537_5) {
      FontText.method1397(
         FontFace.SEMI, local1537_1, local1537_3, local1537_4, 5.5F * settingRenderContext.scale, UiTheme.field09680
      );
      float local1537_6 = local1537_4 + 8.0F * settingRenderContext.scale;

      for (String local1537_8 : UiTheme.method2873(FontFace.SEMI, local1537_2, 5.0F * settingRenderContext.scale, local1537_5)) {
         FontText.method1397(
            FontFace.SEMI, local1537_8, local1537_3, local1537_6, 5.0F * settingRenderContext.scale, UiTheme.field09681
         );
         local1537_6 += 6.0F * settingRenderContext.scale;
      }
   }

   static void method1117(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      AutoBuyPanel autoBuyPanel,
      float local1538_3,
      float local1538_4,
      float local1538_5,
      float local1538_6,
      boolean local1538_7,
      float local1538_8,
      long local1538_9
   ) {
      float local1538_11 = 23.0F * settingRenderContext.scale;
      float local1538_12 = local1538_5 - 5.0F * settingRenderContext.scale;
      float local1538_13 = local1538_11 + (local1538_12 - 11.0F * local1538_11) / 10.0F;
      autoBuySettingsScreen.field03129 = new float[]{local1538_3, local1538_4, local1538_5, local1538_6};
      ArrayList<AutoBuyItem> local1538_14 = autoBuySettingsScreen.method1143();
      autoBuySettingsScreen.field03127.maximumOffset = Math.max(
         0.0F, (local1538_14.size() + 10) / 11 * local1538_13 - (local1538_12 - 11.0F * local1538_11) / 10.0F - local1538_6
      );
      autoBuySettingsScreen.field03127.update(local1538_8);
      if (local1538_14.isEmpty()) {
         FontText.method1397(
            FontFace.SEMI,
            "Ничего не найдено",
            local1538_3 + (local1538_12 - FontText.method1396(FontFace.SEMI, "Ничего не найдено", 5.0F * settingRenderContext.scale)) / 2.0F,
            local1538_4 + local1538_6 / 2.0F - 2.5F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.field09682
         );
      }

      float[] floatArray = new float[]{local1538_3, local1538_4, local1538_12, local1538_6};
      String local1538_16 = "";
      float local1538_17 = 0.0F;
      float local1538_18 = 0.0F;
      Render2D.method4678(
         settingRenderContext.drawContext,
         local1538_3 - 3.0F * settingRenderContext.scale,
         local1538_4 - 2.0F * settingRenderContext.scale,
         local1538_12 + 6.0F * settingRenderContext.scale,
         local1538_6 + 4.0F * settingRenderContext.scale
      );
      RoundedClipStack.method4836(
         local1538_3 - 3.0F * settingRenderContext.scale,
         local1538_4 - 2.0F * settingRenderContext.scale,
         local1538_12 + 6.0F * settingRenderContext.scale,
         local1538_6 + 4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale
      );

      for (int index = 0; index < local1538_14.size(); index++) {
         float local1538_20 = local1538_3 + index % 11 * local1538_13;
         float local1538_21 = local1538_4 + index / 11 * local1538_13 - autoBuySettingsScreen.field03127.getPixelAlignedOffset();
         if (!(local1538_21 + local1538_11 < local1538_4 - 2.0F) && !(local1538_21 > local1538_4 + local1538_6 + 2.0F)) {
            String local1538_22 = local1538_14.get(index).name();
            float local1538_23 = AutoBuyPanel.method3076(
               local1538_9 - autoBuySettingsScreen.field03135, Math.min(0.5F, 0.05F + index / 11 * 0.045F + index % 11 * 0.012F), 0.25F
            );
            if (!(local1538_23 < 0.005F)) {
               float local1538_24 = local1538_21 + (1.0F - local1538_23) * 3.0F * settingRenderContext.scale;
               float local1538_25 = settingRenderContext.method3544("ab:sel:" + local1538_22, autoBuySettingsScreen.method1142(local1538_22));
               float local1538_26 = settingRenderContext.method3544(
                  "ab:hov:" + local1538_22,
                  local1538_7
                     && UiTheme.method2879(settingRenderContext.mouseX, settingRenderContext.mouseY, local1538_20, local1538_24, local1538_11, local1538_11)
                     && UiTheme.method2879(settingRenderContext.mouseX, settingRenderContext.mouseY, local1538_3, local1538_4, local1538_12, local1538_6)
               );
               Render2D.method4705(local1538_23);
               if (local1538_26 > 0.005F) {
                  Render2D.method4715(
                     local1538_20,
                     local1538_24,
                     local1538_11,
                     local1538_11,
                     4.0F * settingRenderContext.scale,
                     UiTheme.method2877(ClickGuiStyle.field10827, local1538_26)
                  );
               }

               if (local1538_25 > 0.005F) {
                  Render2D.method4715(
                     local1538_20,
                     local1538_24,
                     local1538_11,
                     local1538_11,
                     4.0F * settingRenderContext.scale,
                     UiTheme.method2877(UiTheme.field09686, 0.14F * local1538_25)
                  );
               }

               if (settingRenderContext.method3544("ab:editc:" + local1538_22, local1538_22.equals(autoBuySettingsScreen.field03133)) > 0.005F) {
                  Render2D.method4769(
                     local1538_20,
                     local1538_24,
                     local1538_11,
                     local1538_11,
                     4.0F * settingRenderContext.scale,
                     0.5F,
                     UiTheme.method2877(
                        UiTheme.field09686, settingRenderContext.method3544("ab:editc:" + local1538_22, local1538_22.equals(autoBuySettingsScreen.field03133))
                     )
                  );
               }

               if (autoBuySettingsScreen.field03125.get(local1538_22) != null && (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F < 1.0F) {
                  Render2D.method4769(
                     local1538_20
                        - 2.5F
                           * settingRenderContext.scale
                           * (
                              (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22))
                                 / 350.0F
                                 * (2.0F - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F)
                           ),
                     local1538_24
                        - 2.5F
                           * settingRenderContext.scale
                           * (
                              (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22))
                                 / 350.0F
                                 * (2.0F - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F)
                           ),
                     local1538_11
                        + 5.0F
                           * settingRenderContext.scale
                           * (
                              (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22))
                                 / 350.0F
                                 * (2.0F - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F)
                           ),
                     local1538_11
                        + 5.0F
                           * settingRenderContext.scale
                           * (
                              (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22))
                                 / 350.0F
                                 * (2.0F - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F)
                           ),
                     4.0F * settingRenderContext.scale
                        + 2.5F
                           * settingRenderContext.scale
                           * (
                              (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22))
                                 / 350.0F
                                 * (2.0F - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F)
                           ),
                     0.75F,
                     UiTheme.method2877(
                        UiTheme.field09686,
                        (
                              1.0F
                                 - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22))
                                    / 350.0F
                                    * (2.0F - (float)(local1538_9 - autoBuySettingsScreen.field03125.get(local1538_22)) / 350.0F)
                           )
                           * 0.9F
                     )
                  );
               } else if (autoBuySettingsScreen.field03125.get(local1538_22) != null) {
                  autoBuySettingsScreen.field03125.remove(local1538_22);
               }

               ItemRenderCache.method2336(
                  local1538_14.get(index).displayStack(),
                  local1538_20 + local1538_11 / 4.0F,
                  local1538_24 + local1538_11 / 4.0F,
                  local1538_11 * 0.5F,
                  ItemDrawOptions.noDecorations(AutoBuyPanel.method3079() * (0.48F + 0.52F * Math.max(local1538_25, local1538_26 * 0.55F)))
               );
               Render2D.method4650();
               if (local1538_7
                  && UiTheme.method2879(settingRenderContext.mouseX, settingRenderContext.mouseY, local1538_20, local1538_24, local1538_11, local1538_11)
                  && UiTheme.method2879(settingRenderContext.mouseX, settingRenderContext.mouseY, local1538_3, local1538_4, local1538_12, local1538_6)) {
                  local1538_16 = local1538_22;
                  local1538_17 = local1538_20 + local1538_11 / 2.0F;
                  local1538_18 = local1538_24;
               }

               if (local1538_7) {
                  settingRenderContext.method3551(
                     local1538_20,
                     local1538_24,
                     local1538_11,
                     local1538_11,
                     floatArray,
                     () -> autoBuySettingsScreen.method1140(local1538_22),
                     () -> autoBuyPanel.method3085(local1538_22)
                  );
               }
            }
         }
      }

      RoundedClipStack.method4838();
      Render2D.method4661(settingRenderContext.drawContext);
      if (!local1538_16.isEmpty()) {
         Render2D.method4715(
            Math.max(
               local1538_3,
               Math.min(
                  local1538_3
                     + local1538_12
                     - FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale)
                     - 6.0F * settingRenderContext.scale,
                  local1538_17
                     - FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale) / 2.0F
                     - 3.0F * settingRenderContext.scale
               )
            ),
            local1538_18 - 12.0F * settingRenderContext.scale < local1538_4
               ? local1538_18 + local1538_11 + 3.0F * settingRenderContext.scale
               : local1538_18 - 12.0F * settingRenderContext.scale,
            FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale) + 6.0F * settingRenderContext.scale,
            10.0F * settingRenderContext.scale,
            3.0F * settingRenderContext.scale,
            ClickGuiStyle.field10827
         );
         Render2D.method4769(
            Math.max(
               local1538_3,
               Math.min(
                  local1538_3
                     + local1538_12
                     - FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale)
                     - 6.0F * settingRenderContext.scale,
                  local1538_17
                     - FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale) / 2.0F
                     - 3.0F * settingRenderContext.scale
               )
            ),
            local1538_18 - 12.0F * settingRenderContext.scale < local1538_4
               ? local1538_18 + local1538_11 + 3.0F * settingRenderContext.scale
               : local1538_18 - 12.0F * settingRenderContext.scale,
            FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale) + 6.0F * settingRenderContext.scale,
            10.0F * settingRenderContext.scale,
            3.0F * settingRenderContext.scale,
            0.5F,
            UiTheme.field09696
         );
         FontText.method1397(
            FontFace.SEMI,
            local1538_16,
            Math.max(
                  local1538_3,
                  Math.min(
                     local1538_3
                        + local1538_12
                        - FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale)
                        - 6.0F * settingRenderContext.scale,
                     local1538_17
                        - FontText.method1396(FontFace.SEMI, local1538_16, 5.0F * settingRenderContext.scale) / 2.0F
                        - 3.0F * settingRenderContext.scale
                  )
               )
               + 3.0F * settingRenderContext.scale,
            (
                  local1538_18 - 12.0F * settingRenderContext.scale < local1538_4
                     ? local1538_18 + local1538_11 + 3.0F * settingRenderContext.scale
                     : local1538_18 - 12.0F * settingRenderContext.scale
               )
               + 2.5F * settingRenderContext.scale,
            5.0F * settingRenderContext.scale,
            UiTheme.field09680
         );
      }

      if (autoBuySettingsScreen.field03127.maximumOffset > 0.0F) {
         Render2D.method4715(
            local1538_3 + local1538_5 - 1.0F * settingRenderContext.scale,
            local1538_4
               + (local1538_6 - Math.max(8.0F * settingRenderContext.scale, local1538_6 * (local1538_6 / (local1538_6 + autoBuySettingsScreen.field03127.maximumOffset))))
                  * (autoBuySettingsScreen.field03127.getPixelAlignedOffset() / autoBuySettingsScreen.field03127.maximumOffset),
            1.0F * settingRenderContext.scale,
            Math.max(8.0F * settingRenderContext.scale, local1538_6 * (local1538_6 / (local1538_6 + autoBuySettingsScreen.field03127.maximumOffset))),
            0.5F * settingRenderContext.scale,
            UiTheme.field09684
         );
      }
   }

   static void method1118(
      SettingRenderContext settingRenderContext,
      AutoBuySettingsScreen autoBuySettingsScreen,
      float local1539_2,
      float local1539_3,
      float local1539_4,
      float local1539_5,
      long local1539_6,
      long local1539_8,
      float local1539_10
   ) {
      settingRenderContext.method3557(
         local1539_2, local1539_3, local1539_4, local1539_5, 4.0F * settingRenderContext.scale, ClickGuiStyle.field10826, UiTheme.field09696
      );
      FontText.method1397(
         FontFace.SEMI,
         "Price Chart",
         local1539_2 + 8.0F * settingRenderContext.scale,
         local1539_3 + 8.0F * settingRenderContext.scale,
         5.0F * settingRenderContext.scale,
         UiTheme.field09682
      );
      autoBuySettingsScreen.field03132 = autoBuySettingsScreen.field03132 + (autoBuySettingsScreen.field03131 - autoBuySettingsScreen.field03132) * (1.0 - Math.exp(-local1539_10 * 8.0F));
      if (Math.abs(autoBuySettingsScreen.field03131 - autoBuySettingsScreen.field03132) < 1.0) {
         autoBuySettingsScreen.field03132 = autoBuySettingsScreen.field03131;
      }

      FontText.method1397(
         FontFace.SEMI,
         "Total sales: $" + AutoBuySettingsScreen.method1134(Math.round(autoBuySettingsScreen.field03132)),
         local1539_2
            + local1539_4
            - 8.0F * settingRenderContext.scale
            - FontText.method1396(
               FontFace.SEMI, "Total sales: $" + AutoBuySettingsScreen.method1134(Math.round(autoBuySettingsScreen.field03132)), 4.5F * settingRenderContext.scale
            ),
         local1539_3 + 8.5F * settingRenderContext.scale,
         4.5F * settingRenderContext.scale,
         UiTheme.field09683
      );
      float local1539_11 = local1539_3 + 22.0F * settingRenderContext.scale;
      float local1539_12 = local1539_3 + local1539_5 - 8.0F * settingRenderContext.scale - local1539_11;
      float local1539_13 = local1539_2 + 28.0F * settingRenderContext.scale;
      float local1539_14 = local1539_2 + local1539_4 - 8.0F * settingRenderContext.scale - local1539_13;

      for (int index = 0; index < field02627.length; index++) {
         FontText.method1397(
            FontFace.SEMI,
            field02627[index],
            local1539_2
               + 20.0F * settingRenderContext.scale
               - FontText.method1396(FontFace.SEMI, field02627[index], 4.5F * settingRenderContext.scale),
            local1539_11 + index * (local1539_12 - 5.0F * settingRenderContext.scale) / (field02627.length - 1),
            4.5F * settingRenderContext.scale,
            UiTheme.field09683
         );
         Render2D.method4715(
            local1539_13,
            local1539_11 + index * (local1539_12 - 5.0F * settingRenderContext.scale) / (field02627.length - 1) + 2.25F * settingRenderContext.scale,
            local1539_14,
            0.5F * settingRenderContext.scale,
            0.25F * settingRenderContext.scale,
            UiTheme.field09685
         );
      }

      for (int index2 = 0; index2 < autoBuySettingsScreen.field03124.length; index2++) {
         float local1539_16 = Math.max(
            0.0F,
            settingRenderContext.method3560("ab:bar:" + index2, autoBuySettingsScreen.field03124[index2])
               * local1539_12
               * AutoBuyPanel.method3076(local1539_6 - local1539_8, 0.18F + index2 * 0.025F, 0.3F)
         );
         if (!(local1539_16 < 0.5F)) {
            Render2D.method4767(
               local1539_13 + index2 * (local1539_14 - 13.0F * settingRenderContext.scale) / (autoBuySettingsScreen.field03124.length - 1),
               local1539_11 + local1539_12 - local1539_16,
               13.0F * settingRenderContext.scale,
               local1539_16,
               Math.min(2.0F * settingRenderContext.scale, local1539_16 / 2.0F),
               Math.min(2.0F * settingRenderContext.scale, local1539_16 / 2.0F),
               0.0F,
               0.0F,
               UiTheme.method2881(
                  UiTheme.field09686,
                  UiTheme.field09688,
                  settingRenderContext.method3544(
                     "ab:barh:" + index2,
                     UiTheme.method2879(
                        settingRenderContext.mouseX,
                        settingRenderContext.mouseY,
                        local1539_13 + index2 * (local1539_14 - 13.0F * settingRenderContext.scale) / (autoBuySettingsScreen.field03124.length - 1),
                        local1539_11,
                        13.0F * settingRenderContext.scale,
                        local1539_12
                     )
                  )
               )
            );
         }
      }
   }
}
