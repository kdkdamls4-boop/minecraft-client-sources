package deluxe.client.ui.panel;

import deluxe.client.ui.AnimatedValue;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.render.CornerRadii;
import deluxe.client.render.FontText;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.util.PotionPreset;
import deluxe.client.render.RoundedClipStack;
import deluxe.client.ui.setting.SettingRenderContext;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.ui.UiTheme;

import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.render.Render2D;
import deluxe.client.module.player.ServerHelperModule;
import deluxe.client.setting.TextSetting;

import java.util.ArrayList;
import net.minecraft.item.ItemStack;

public final class PotionPresetPanel {
   static final String field03651 = "assets/deluxe/textures/gui/presets/logo.png";
   static final String field03652 = "assets/deluxe/textures/gui/presets/chip.png";
   static final String field03653 = "assets/deluxe/textures/gui/presets/search.png";
   static final String field03654 = "assets/deluxe/textures/gui/presets/keyboard_dim.png";
   static final String field03655 = "assets/deluxe/textures/gui/presets/keyboard_on.png";
   static final String field03656 = "assets/deluxe/textures/gui/presets/toggle.png";
   static final String field03657 = "assets/deluxe/textures/gui/presets/trash.png";
   static final String field03658 = "assets/deluxe/textures/gui/presets/chip_small.png";
   static final String field03659 = "assets/deluxe/textures/gui/presets/potion_add.png";
   static final String field03660 = "assets/deluxe/textures/gui/presets/search_small.png";
   private static PotionPresetPanel potionPresetPanel;
   private AnimatedValue animatedValue = new AnimatedValue(300L).method3870(0.0);
   private boolean field03663;
   private long field03664;

   private void method1314(SettingRenderContext settingRenderContext, float local1824_2, float local1824_3, boolean local1824_4, float local1824_5, float local1824_6) {
      PotionPreset potionPreset = PotionPreset.method2166();
      float local1824_8 = (local1824_2 - 263.0F * settingRenderContext.scale) / 2.0F;
      float local1824_9 = (local1824_3 - 133.33F * settingRenderContext.scale) / 2.0F + local1824_6;
      long local1824_10 = System.currentTimeMillis();
      method1317(
         local1824_8, local1824_9, 166.0F * settingRenderContext.scale, 133.33F * settingRenderContext.scale, 5.7F * settingRenderContext.scale, settingRenderContext.scale
      );
      method1317(
         local1824_8 + 172.0F * settingRenderContext.scale,
         local1824_9,
         91.0F * settingRenderContext.scale,
         133.33F * settingRenderContext.scale,
         5.7F * settingRenderContext.scale,
         settingRenderContext.scale
      );
      if (local1824_4) {
         settingRenderContext.method3554(local1824_8, local1824_9, 166.0F * settingRenderContext.scale, 133.33F * settingRenderContext.scale, null, () -> {});
      }

      if (local1824_4) {
         settingRenderContext.method3554(
            local1824_8 + 172.0F * settingRenderContext.scale, local1824_9, 91.0F * settingRenderContext.scale, 133.33F * settingRenderContext.scale, null, () -> {}
         );
      }

      method1319(
         settingRenderContext,
         local1824_8,
         local1824_9,
         166.0F * settingRenderContext.scale,
         "Создание пресета",
         false,
         "Potion Combiner",
         "Выбрасывает указанные зелья из инвентаря по нажатию указанной вами клавиши",
         136.0F,
         151.33F
      );
      method1319(
         settingRenderContext,
         local1824_8 + 172.0F * settingRenderContext.scale,
         local1824_9,
         91.0F * settingRenderContext.scale,
         "Настройки пресетов",
         true,
         "Ваши пресеты",
         "Созданные вами ранее пресеты системы Potion Combiner",
         80.0F,
         75.0F
      );
      FontText.method1397(
         FontFace.SEMI,
         "Имя пресета",
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 46.67F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "Уникальное имя данного пресета",
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 52.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09680, 0.35F)
      );
      method1323(
         settingRenderContext,
         potionPreset.field07356,
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 60.0F * settingRenderContext.scale,
         151.33F * settingRenderContext.scale,
         9.0F * settingRenderContext.scale,
         local1824_4,
         "pc:name"
      );
      FontText.method1397(
         FontFace.SEMI,
         "Горячая клавиша",
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 74.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.field09680
      );
      FontText.method1397(
         FontFace.SEMI,
         "Зелье выбрасываются при нажатии бинда",
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 80.0F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09680, 0.35F)
      );
      this.method1321(
         settingRenderContext,
         potionPreset.keybindSetting,
         local1824_8 + 134.0F * settingRenderContext.scale,
         local1824_9 + 76.0F * settingRenderContext.scale,
         30.0F,
         local1824_4,
         "pc:draftbind"
      );

      for (int index = 0; index < 7; index++) {
         float local1824_13 = local1824_8 + 6.67F * settingRenderContext.scale + index * 22.0F * settingRenderContext.scale;
         float local1824_14 = local1824_9 + 90.0F * settingRenderContext.scale;
         Render2D.method4715(
            local1824_13,
            local1824_14,
            19.67F * settingRenderContext.scale,
            19.67F * settingRenderContext.scale,
            3.33F * settingRenderContext.scale,
            UiTheme.method2877(UiTheme.field09686, 0.05F)
         );
         Render2D.method4769(
            local1824_13,
            local1824_14,
            19.67F * settingRenderContext.scale,
            19.67F * settingRenderContext.scale,
            3.33F * settingRenderContext.scale,
            0.35F * settingRenderContext.scale,
            UiTheme.method2877(
               UiTheme.field09686,
               index == 6
                  ? 0.19F
                  : 0.06F
                     + 0.13F
                        * settingRenderContext.method3544(
                           "pc:slot:" + index,
                           local1824_4
                              && UiTheme.method2879(
                                 settingRenderContext.mouseX,
                                 settingRenderContext.mouseY,
                                 local1824_13,
                                 local1824_14,
                                 19.67F * settingRenderContext.scale,
                                 19.67F * settingRenderContext.scale
                              )
                        )
            )
         );
         if (index < 6 && !potionPreset.field07359[index].isEmpty()) {
            ItemRenderCache.method2336(
               potionPreset.field07359[index],
               local1824_13 + 4.33F * settingRenderContext.scale,
               local1824_14 + 4.17F * settingRenderContext.scale,
               11.0F * settingRenderContext.scale,
               ItemDrawOptions.noDecorations(method1331())
            );
         }

         if (index < 6 && potionPreset.field07360[index] != null) {
            settingRenderContext.method3573(
               potionPreset.field07360[index], local1824_13, local1824_14, 19.67F * settingRenderContext.scale, 19.67F * settingRenderContext.scale
            );
         }

         int index2 = index;
         if (local1824_4) {
            settingRenderContext.method3554(local1824_13, local1824_14, 19.67F * settingRenderContext.scale, 19.67F * settingRenderContext.scale, null, () -> {
               if (index2 < 6 && !potionPreset.field07359[index2].isEmpty()) {
                  potionPreset.field07359[index2] = ItemStack.EMPTY;
                  potionPreset.field07360[index2] = null;
               } else {
                  potionPreset.field07361 = index2 < 6 ? index2 : potionPreset.method2165();
               }
            });
         }
      }

      float local1824_12 = settingRenderContext.method3544(
         "pc:save",
         local1824_4
            && UiTheme.method2879(
               settingRenderContext.mouseX,
               settingRenderContext.mouseY,
               local1824_8 + 6.67F * settingRenderContext.scale,
               local1824_9 + 116.33F * settingRenderContext.scale,
               151.33F * settingRenderContext.scale,
               11.0F * settingRenderContext.scale
            )
      );
      Render2D.method4715(
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 116.33F * settingRenderContext.scale,
         151.33F * settingRenderContext.scale,
         11.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09680, 0.02F + 0.03F * local1824_12)
      );
      Render2D.method4769(
         local1824_8 + 6.67F * settingRenderContext.scale,
         local1824_9 + 116.33F * settingRenderContext.scale,
         151.33F * settingRenderContext.scale,
         11.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         0.35F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09686, 0.06F + 0.14F * local1824_12)
      );
      FontText.method1397(
         FontFace.SEMI_BOLD,
         "Сохранить пресет",
         local1824_8
            + 6.67F * settingRenderContext.scale
            + (
                  151.33F * settingRenderContext.scale
                     - FontText.method1396(FontFace.SEMI_BOLD, "Сохранить пресет", 4.2F * settingRenderContext.scale)
               )
               / 2.0F,
         local1824_9 + 119.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09680, 1.0F - 0.45F * local1824_12)
      );
      if (local1824_4) {
         settingRenderContext.method3554(
            local1824_8 + 6.67F * settingRenderContext.scale,
            local1824_9 + 116.33F * settingRenderContext.scale,
            151.33F * settingRenderContext.scale,
            11.0F * settingRenderContext.scale,
            null,
            () -> potionPreset.method2169()
         );
      }

      this.method1338(settingRenderContext, potionPreset, local1824_8 + 172.0F * settingRenderContext.scale, local1824_9, local1824_4, local1824_5);
      if (potionPreset.field07361 < 0 && !(settingRenderContext.method3567("pc:dropdown", potionPreset.field07361 >= 0) > 0.005F)) {
         potionPreset.field07365 = null;
      } else {
         this.method1335(
            settingRenderContext,
            potionPreset,
            local1824_8 - 80.0F * settingRenderContext.scale,
            local1824_9 + 27.33F * settingRenderContext.scale,
            local1824_4 && potionPreset.field07361 >= 0,
            local1824_5,
            settingRenderContext.method3567("pc:dropdown", potionPreset.field07361 >= 0)
         );
      }
   }

   public static PotionPresetPanel method1316() {
      if (potionPresetPanel == null) {
         potionPresetPanel = new PotionPresetPanel();
      }

      return potionPresetPanel;
   }

   public static void method1317(float local1827_0, float local1827_1, float local1827_2, float local1827_3, float local1827_4, float local1827_5) {
      ShapeDrawing.method3653(
         local1827_0, local1827_1, local1827_2, local1827_3, method1331(), CornerRadii.uniform(local1827_4), ShapeDrawing.Nested0502.LOW, true
      );
      Render2D.method4715(local1827_0, local1827_1, local1827_2, local1827_3, local1827_4, ClickGuiStyle.field10781);
      Render2D.method4769(
         local1827_0,
         local1827_1,
         local1827_2,
         local1827_3,
         local1827_4,
         0.35F * local1827_5,
         UiTheme.method2877(UiTheme.field09686, 0.06F)
      );
   }

   public boolean method1318(float local1828_1, float local1828_2, double local1828_3, float local1828_5) {
      if (PotionPreset.method2166().field07365 != null
         && PotionPreset.method2166().field07361 >= 0
         && UiTheme.method2879(
            local1828_1,
            local1828_2,
            PotionPreset.method2166().field07365[0],
            PotionPreset.method2166().field07365[1],
            PotionPreset.method2166().field07365[2],
            PotionPreset.method2166().field07365[3]
         )) {
         PotionPreset.method2166().field07363.targetOffset = SettingRenderContext.method3541(
            PotionPreset.method2166().field07363.targetOffset - (float)local1828_3 * 14.0F * local1828_5,
            0.0F,
            PotionPreset.method2166().field07363.maximumOffset
         );
      } else if (PotionPreset.method2166().field07366 != null
         && UiTheme.method2879(
            local1828_1,
            local1828_2,
            PotionPreset.method2166().field07366[0],
            PotionPreset.method2166().field07366[1],
            PotionPreset.method2166().field07366[2],
            PotionPreset.method2166().field07366[3]
         )) {
         PotionPreset.method2166().field07364.targetOffset = SettingRenderContext.method3541(
            PotionPreset.method2166().field07364.targetOffset - (float)local1828_3 * 14.0F * local1828_5,
            0.0F,
            PotionPreset.method2166().field07364.maximumOffset
         );
      }

      return true;
   }

   public static void method1319(
      SettingRenderContext settingRenderContext,
      float local1829_1,
      float local1829_2,
      float local1829_3,
      String local1829_4,
      boolean local1829_5,
      String local1829_6,
      String local1829_7,
      float local1829_8,
      float local1829_9
   ) {
      FontText.method1397(
         FontFace.SEMI_BOLD,
         local1829_4,
         local1829_5 ? local1829_1 + 8.0F * settingRenderContext.scale : local1829_1 + 6.67F * settingRenderContext.scale,
         local1829_2 + 6.33F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09680, 0.1F)
      );
      FontText.method1397(
         FontFace.SEMI,
         local1829_6,
         local1829_1 + (local1829_5 ? 8.0F : 6.67F) * settingRenderContext.scale,
         local1829_2 + 15.33F * settingRenderContext.scale,
         5.33F * settingRenderContext.scale,
         UiTheme.field09680
      );
      float local1829_10 = local1829_2 + 23.33F * settingRenderContext.scale;

      for (String local1829_12 : UiTheme.method2873(
         FontFace.SEMI, local1829_7, 4.2F * settingRenderContext.scale, local1829_8 * settingRenderContext.scale
      )) {
         FontText.method1397(
            FontFace.SEMI,
            local1829_12,
            local1829_1 + (local1829_5 ? 8.0F : 6.67F) * settingRenderContext.scale,
            local1829_10,
            4.2F * settingRenderContext.scale,
            UiTheme.method2877(UiTheme.field09680, 0.35F)
         );
         local1829_10 += 5.0F * settingRenderContext.scale;
      }

      Render2D.method4715(
         local1829_1 + (local1829_5 ? 8.0F : 6.67F) * settingRenderContext.scale,
         local1829_2 + 40.0F * settingRenderContext.scale,
         local1829_9 * settingRenderContext.scale,
         0.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.method2877(UiTheme.field09680, 0.03F)
      );
   }

   public void method1320() {
      this.field03663 = false;
      PotionPreset.method2166().field07361 = -1;
      PotionPreset.method2166().method2167();
   }

   private void method1321(
      SettingRenderContext settingRenderContext,
      KeybindSetting newKeybindSetting,
      float local1831_3,
      float local1831_4,
      float local1831_5,
      boolean local1831_6,
      String local1831_7
   ) {
      boolean local1831_8 = settingRenderContext.keybindSetting == newKeybindSetting;
      Render2D.method4777(
         !newKeybindSetting.isBound() && !local1831_8
            ? "assets/deluxe/textures/gui/presets/keyboard_dim.png"
            : "assets/deluxe/textures/gui/presets/keyboard_on.png",
         local1831_3,
         local1831_4,
         3.33F * settingRenderContext.scale,
         3.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.method2877(UiTheme.field09680, 1.0F)
      );
      Render2D.method4678(
         settingRenderContext.drawContext, local1831_3, local1831_4 - 2.0F * settingRenderContext.scale, local1831_5 * settingRenderContext.scale, 8.0F * settingRenderContext.scale
      );
      FontText.method1397(
         FontFace.SEMI_BOLD,
         local1831_8 ? "..." : (newKeybindSetting.isBound() ? newKeybindSetting.getKeyName().toUpperCase() : "Press key"),
         local1831_3 + 5.67F * settingRenderContext.scale,
         local1831_4 - 0.33F * settingRenderContext.scale,
         4.0F * settingRenderContext.scale,
         !local1831_8 && !newKeybindSetting.isBound()
            ? UiTheme.method2877(
               UiTheme.field09680,
               0.2F
                  + 0.3F
                     * settingRenderContext.method3544(
                        local1831_7,
                        local1831_6
                           && UiTheme.method2879(
                              settingRenderContext.mouseX,
                              settingRenderContext.mouseY,
                              local1831_3 - 1.0F * settingRenderContext.scale,
                              local1831_4 - 2.0F * settingRenderContext.scale,
                              local1831_5 * settingRenderContext.scale,
                              8.0F * settingRenderContext.scale
                           )
                     )
            )
            : UiTheme.field09686
      );
      Render2D.method4661(settingRenderContext.drawContext);
      if (local1831_6) {
         settingRenderContext.method3551(
            local1831_3 - 1.0F * settingRenderContext.scale,
            local1831_4 - 2.0F * settingRenderContext.scale,
            local1831_5 * settingRenderContext.scale,
            8.0F * settingRenderContext.scale,
            null,
            () -> settingRenderContext.keybindSetting = newKeybindSetting,
            () -> newKeybindSetting.withKeyCode(-1)
         );
      }
   }

   public static void method1323(
      SettingRenderContext settingRenderContext,
      TextSetting newTextSetting,
      float local1833_2,
      float local1833_3,
      float local1833_4,
      float local1833_5,
      boolean local1833_6,
      String local1833_7
   ) {
      Render2D.method4715(
         local1833_2, local1833_3, local1833_4, local1833_5, 2.0F * settingRenderContext.scale, UiTheme.method2877(UiTheme.field09686, 0.05F)
      );
      Render2D.method4769(
         local1833_2,
         local1833_3,
         local1833_4,
         local1833_5,
         2.0F * settingRenderContext.scale,
         0.35F * settingRenderContext.scale,
         UiTheme.method2877(
            UiTheme.field09686, 0.06F + 0.13F * settingRenderContext.method3544(local1833_7, settingRenderContext.textSetting == newTextSetting)
         )
      );
      Render2D.method4678(
         settingRenderContext.drawContext, local1833_2 + 2.5F * settingRenderContext.scale, local1833_3, local1833_4 - 5.5F * settingRenderContext.scale, local1833_5
      );
      FontText.method1397(
         FontFace.SEMI,
         (newTextSetting.getValue().isEmpty() && settingRenderContext.textSetting != newTextSetting ? newTextSetting.getPlaceholder() : newTextSetting.getValue())
            + (settingRenderContext.textSetting == newTextSetting ? SettingRenderContext.method3542() : ""),
         local1833_2 + 3.33F * settingRenderContext.scale,
         local1833_3 + local1833_5 / 2.0F - 1.67F * settingRenderContext.scale,
         3.33F * settingRenderContext.scale,
         newTextSetting.getValue().isEmpty() && settingRenderContext.textSetting != newTextSetting
            ? UiTheme.method2877(UiTheme.field09680, 0.24F)
            : UiTheme.field09680
      );
      Render2D.method4661(settingRenderContext.drawContext);
      if (local1833_6) {
         settingRenderContext.method3554(local1833_2, local1833_3, local1833_4, local1833_5, null, () -> {
            settingRenderContext.textSetting = newTextSetting;
            newTextSetting.moveToEnd(false);
         });
      }
   }

   public static void method1325(SettingRenderContext settingRenderContext, float local1835_1, float local1835_2) {
      Render2D.method4777(
         "assets/deluxe/textures/gui/presets/logo.png",
         local1835_1 / 2.0F
            - (8.0F + FontText.method1396(FontFace.BOLD, "Deluxe Client", 3.43F * settingRenderContext.scale) / settingRenderContext.scale)
               * settingRenderContext.scale
               / 2.0F,
         local1835_2 - 23.0F * settingRenderContext.scale,
         5.33F * settingRenderContext.scale,
         4.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.method2877(UiTheme.field09680, 1.0F)
      );
      FontText.method1397(
         FontFace.BOLD,
         "Deluxe Client",
         local1835_1 / 2.0F
            - (8.0F + FontText.method1396(FontFace.BOLD, "Deluxe Client", 3.43F * settingRenderContext.scale) / settingRenderContext.scale)
               * settingRenderContext.scale
               / 2.0F
            + 8.0F * settingRenderContext.scale,
         local1835_2 - 22.7F * settingRenderContext.scale,
         3.43F * settingRenderContext.scale,
         UiTheme.field09686
      );
      FontText.method1397(
         FontFace.SEMI,
         "Beta 1.21.11",
         local1835_1 / 2.0F
            - (8.0F + FontText.method1396(FontFace.BOLD, "Deluxe Client", 3.43F * settingRenderContext.scale) / settingRenderContext.scale)
               * settingRenderContext.scale
               / 2.0F
            + 9.0F * settingRenderContext.scale,
         local1835_2 - 17.3F * settingRenderContext.scale,
         3.43F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09686, 0.4F)
      );
   }

   private PotionPresetPanel() {
   }

   public void method1327() {
      if (!this.field03663) {
         this.field03663 = true;
         this.field03664 = System.currentTimeMillis();
      }
   }

   public float method1328() {
      this.animatedValue.method3869(this.field03663 ? 1.0 : 0.0);
      return (float)this.animatedValue.method3871();
   }

   public void method1330(SettingRenderContext settingRenderContext, float local1841_2, float local1841_3, float local1841_4, float local1841_5) {
      if (!(local1841_4 < 0.005F)) {
         settingRenderContext.field12418 = settingRenderContext.field12417.size();
         Render2D.method4715(0.0F, 0.0F, local1841_2, local1841_3, 0.0F, UiTheme.method2877(ClickGuiStyle.field10820, local1841_4));
         if (this.field03663 && local1841_4 > 0.6F) {
            settingRenderContext.method3554(0.0F, 0.0F, local1841_2, local1841_3, null, () -> this.method1320());
         }

         Render2D.method4705(local1841_4);
         this.method1314(
            settingRenderContext, local1841_2, local1841_3, this.field03663 && local1841_4 > 0.6F, local1841_5, (1.0F - local1841_4) * 12.0F * settingRenderContext.scale
         );
         Render2D.method4650();
      }
   }

   public static float method1331() {
      return (Render2D.method4724(-16777216) >>> 24 & 0xFF) / 255.0F;
   }

   public boolean method1333() {
      return this.field03663;
   }

   private void method1335(
      SettingRenderContext settingRenderContext,
      PotionPreset potionPreset,
      float local1846_3,
      float local1846_4,
      boolean local1846_5,
      float local1846_6,
      float local1846_7
   ) {
      local1846_5 = local1846_5 && local1846_7 > 0.6F;
      local1846_3 -= (1.0F - local1846_7) * 8.0F * settingRenderContext.scale;
      Render2D.method4705(local1846_7);
      if (local1846_5) {
         settingRenderContext.method3554(0.0F, 0.0F, 99999.0F, 99999.0F, null, () -> potionPreset.field07361 = -1);
      }

      float local1846_8 = 74.0F * settingRenderContext.scale;
      float local1846_9 = 78.67F * settingRenderContext.scale;
      method1317(local1846_3, local1846_4, local1846_8, local1846_9, 3.0F * settingRenderContext.scale, settingRenderContext.scale);
      Render2D.method4715(
         local1846_3, local1846_4, local1846_8, local1846_9, 3.0F * settingRenderContext.scale, UiTheme.method2877(UiTheme.field09686, 0.05F)
      );
      if (local1846_5) {
         settingRenderContext.method3554(local1846_3, local1846_4, local1846_8, local1846_9, null, () -> {});
      }

      FontText.method1397(
         FontFace.SEMI_BOLD,
         "Ваши зелья",
         local1846_3 + 5.5F * settingRenderContext.scale,
         local1846_4 + 5.0F * settingRenderContext.scale,
         2.8F * settingRenderContext.scale,
         UiTheme.method2877(UiTheme.field09680, 0.1F)
      );
      FontText.method1397(
         FontFace.SEMI,
         "Текущие зелья",
         local1846_3 + 5.5F * settingRenderContext.scale,
         local1846_4 + 10.0F * settingRenderContext.scale,
         4.2F * settingRenderContext.scale,
         UiTheme.field09680
      );
      method1323(
         settingRenderContext,
         potionPreset.field07357,
         local1846_3 + 5.5F * settingRenderContext.scale,
         local1846_4 + 18.33F * settingRenderContext.scale,
         63.0F * settingRenderContext.scale,
         9.0F * settingRenderContext.scale,
         local1846_5,
         "pc:psearch"
      );
      Render2D.method4715(
         local1846_3 + 5.5F * settingRenderContext.scale,
         local1846_4 + 32.33F * settingRenderContext.scale,
         63.0F * settingRenderContext.scale,
         0.33F * settingRenderContext.scale,
         0.0F,
         UiTheme.method2877(UiTheme.field09680, 0.03F)
      );
      ArrayList<ServerHelperModule.Nested0360> local1846_10 = potionPreset.method2163();
      float local1846_11 = local1846_4 + 36.33F * settingRenderContext.scale;
      float local1846_12 = local1846_9 - 40.0F * settingRenderContext.scale;
      potionPreset.field07365 = new float[]{local1846_3, local1846_4, local1846_8, local1846_9};
      potionPreset.field07363.maximumOffset = Math.max(0.0F, local1846_10.size() * 7.33F * settingRenderContext.scale - local1846_12);
      potionPreset.field07363.update(local1846_6);
      if (local1846_10.isEmpty()) {
         FontText.method1397(
            FontFace.SEMI,
            "Зелий нет",
            local1846_3 + 12.0F * settingRenderContext.scale,
            local1846_11 + 2.0F * settingRenderContext.scale,
            3.67F * settingRenderContext.scale,
            UiTheme.method2877(UiTheme.field09680, 0.2F)
         );
      }

      Render2D.method4678(
         settingRenderContext.drawContext, local1846_3 + 2.0F * settingRenderContext.scale, local1846_11, local1846_8 - 4.0F * settingRenderContext.scale, local1846_12
      );
      float[] floatArray = new float[]{local1846_3 + 2.0F * settingRenderContext.scale, local1846_11, local1846_8 - 4.0F * settingRenderContext.scale, local1846_12};

      for (int index = 0; index < local1846_10.size(); index++) {
         float local1846_15 = local1846_11 + index * 7.33F * settingRenderContext.scale - potionPreset.field07363.getPixelAlignedOffset();
         if (!(local1846_15 + 6.0F * settingRenderContext.scale < local1846_11 - 2.0F) && !(local1846_15 > local1846_11 + local1846_12 + 2.0F)) {
            ServerHelperModule.Nested0360 local1846_16 = local1846_10.get(index);
            float local1846_17 = settingRenderContext.method3544(
               "pc:drow:" + index,
               local1846_5
                  && UiTheme.method2879(
                     settingRenderContext.mouseX,
                     settingRenderContext.mouseY,
                     local1846_3 + 3.5F * settingRenderContext.scale,
                     local1846_15 - 1.0F * settingRenderContext.scale,
                     local1846_8 - 9.5F * settingRenderContext.scale,
                     7.33F * settingRenderContext.scale
                  )
                  && UiTheme.method2879(settingRenderContext.mouseX, settingRenderContext.mouseY, local1846_3, local1846_11, local1846_8, local1846_12)
            );
            if (local1846_17 > 0.005F) {
               Render2D.method4715(
                  local1846_3 + 3.5F * settingRenderContext.scale,
                  local1846_15 - 1.0F * settingRenderContext.scale,
                  local1846_8 - 9.5F * settingRenderContext.scale,
                  7.0F * settingRenderContext.scale,
                  2.0F * settingRenderContext.scale,
                  UiTheme.method2877(UiTheme.field09686, 0.08F * local1846_17)
               );
            }

            ItemRenderCache.method2336(
               PotionPreset.method2168(local1846_16.getKeybindSetting().name),
               local1846_3 + 5.5F * settingRenderContext.scale,
               local1846_15,
               4.67F * settingRenderContext.scale,
               ItemDrawOptions.noDecorations(method1331())
            );
            Render2D.method4678(
               settingRenderContext.drawContext,
               local1846_3 + 12.5F * settingRenderContext.scale,
               local1846_15 - 1.0F * settingRenderContext.scale,
               local1846_8 - 19.5F * settingRenderContext.scale,
               7.33F * settingRenderContext.scale
            );
            float var10002 = local1846_3 + 12.5F * settingRenderContext.scale;
            float var10003 = local1846_15 + 0.33F * settingRenderContext.scale;
            float var10004 = 3.67F * settingRenderContext.scale;
            FontText.method1397(
               FontFace.SEMI, local1846_16.getKeybindSetting().name, var10002, var10003, var10004, UiTheme.field09680
            );
            Render2D.method4661(settingRenderContext.drawContext);
            if (local1846_5) {
               settingRenderContext.method3554(
                  local1846_3 + 3.5F * settingRenderContext.scale,
                  local1846_15 - 1.0F * settingRenderContext.scale,
                  local1846_8 - 9.5F * settingRenderContext.scale,
                  7.33F * settingRenderContext.scale,
                  floatArray,
                  () -> potionPreset.method2164(
                     potionPreset.field07361 < 0 ? potionPreset.method2165() : potionPreset.field07361, local1846_16.getKeybindSetting().name
                  )
               );
            }
         }
      }

      Render2D.method4661(settingRenderContext.drawContext);
      if (potionPreset.field07363.maximumOffset > 0.0F) {
         Render2D.method4715(
            local1846_3 + 67.67F * settingRenderContext.scale,
            local1846_11 + (local1846_12 - 11.0F * settingRenderContext.scale) * (potionPreset.field07363.getPixelAlignedOffset() / potionPreset.field07363.maximumOffset),
            0.67F * settingRenderContext.scale,
            11.0F * settingRenderContext.scale,
            3.0F * settingRenderContext.scale,
            UiTheme.method2877(UiTheme.field09686, 0.35F)
         );
      }

      Render2D.method4650();
   }

   public boolean method1337() {
      if (PotionPreset.method2166().field07361 >= 0) {
         PotionPreset.method2166().field07361 = -1;
         return true;
      }

      if (!this.field03663) {
         return false;
      }

      this.field03663 = false;
      PotionPreset.method2166().method2167();
      return true;
   }

   private void method1338(
      SettingRenderContext settingRenderContext, PotionPreset potionPreset, float local1849_3, float local1849_4, boolean local1849_5, float local1849_6
   ) {
      float local1849_7 = local1849_4 + 45.0F * settingRenderContext.scale;
      float local1849_8 = 133.33F * settingRenderContext.scale - 47.0F * settingRenderContext.scale;
      potionPreset.field07366 = new float[]{local1849_3, local1849_7, 91.0F * settingRenderContext.scale, local1849_8};
      potionPreset.field07364.maximumOffset = Math.max(0.0F, potionPreset.field07362.size() * 16.0F * settingRenderContext.scale - local1849_8);
      potionPreset.field07364.update(local1849_6);
      if (potionPreset.field07362.isEmpty()) {
         FontText.method1397(
            FontFace.SEMI,
            "Пресетов пока нет",
            local1849_3
               + (91.0F * settingRenderContext.scale - FontText.method1396(FontFace.SEMI, "Пресетов пока нет", 4.0F * settingRenderContext.scale))
                  / 2.0F,
            local1849_7 + 8.0F * settingRenderContext.scale,
            4.0F * settingRenderContext.scale,
            UiTheme.method2877(UiTheme.field09680, 0.2F)
         );
      }

      Render2D.method4678(
         settingRenderContext.drawContext, local1849_3 + 2.0F * settingRenderContext.scale, local1849_7, 89.0F * settingRenderContext.scale, local1849_8
      );
      RoundedClipStack.method4836(
         local1849_3 + 2.0F * settingRenderContext.scale,
         local1849_7,
         89.0F * settingRenderContext.scale,
         local1849_8,
         2.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale,
         2.0F * settingRenderContext.scale
      );
      float[] floatArray = new float[]{local1849_3 + 2.0F * settingRenderContext.scale, local1849_7, 89.0F * settingRenderContext.scale, local1849_8};

      for (int index = 0; index < potionPreset.field07362.size(); index++) {
         PotionPreset.Nested0421 local1849_11 = potionPreset.field07362.get(index);
         float local1849_12 = local1849_7 + index * 16.0F * settingRenderContext.scale - potionPreset.field07364.getPixelAlignedOffset();
         if (!(local1849_12 + 15.5F * settingRenderContext.scale < local1849_7 - 2.0F) && !(local1849_12 > local1849_7 + local1849_8 + 2.0F)) {
            FontText.method1397(
               FontFace.SEMI,
               local1849_11.field07389,
               local1849_3 + 8.0F * settingRenderContext.scale,
               local1849_12,
               3.8F * settingRenderContext.scale,
               UiTheme.method2877(UiTheme.field09680, local1849_11.field07390 ? 1.0F : 0.5F)
            );
            float local1849_13 = local1849_12 + 5.5F * settingRenderContext.scale;
            float local1849_14 = local1849_11.field07392.size() <= 1 ? 8.5F : Math.min(8.5F, 29.0F / (Math.min(5, local1849_11.field07392.size()) - 1));

            for (int index2 = 0; index2 < Math.min(5, local1849_11.field07392.size()); index2++) {
               ItemRenderCache.method2336(
                  local1849_11.field07392.get(index2).method2171(),
                  local1849_3 + 8.0F * settingRenderContext.scale + index2 * local1849_14 * settingRenderContext.scale,
                  local1849_13 + 0.83F * settingRenderContext.scale,
                  7.0F * settingRenderContext.scale,
                  ItemDrawOptions.noDecorations(method1331() * (local1849_11.field07390 ? 1.0F : 0.4F))
               );
            }

            this.method1321(
               settingRenderContext,
               local1849_11.keybindSetting,
               local1849_3 + (18.0F + (Math.min(5, local1849_11.field07392.size()) - 1) * local1849_14) * settingRenderContext.scale,
               local1849_13 + 2.0F * settingRenderContext.scale,
               46.0F - (Math.min(5, local1849_11.field07392.size()) - 1) * local1849_14,
               local1849_5,
               "pc:prb:" + index
            );
            float local1849_15 = local1849_3 + 64.67F * settingRenderContext.scale;
            float local1849_16 = local1849_3 + 74.67F * settingRenderContext.scale;
            Render2D.method4715(
               local1849_15,
               local1849_13,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               UiTheme.method2877(UiTheme.field09686, 0.05F)
            );
            Render2D.method4769(
               local1849_15,
               local1849_13,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               0.35F * settingRenderContext.scale,
               UiTheme.method2877(
                  UiTheme.field09686,
                  0.06F
                     + 0.13F
                        * settingRenderContext.method3544(
                           "pc:tgh:" + index,
                           local1849_5
                              && UiTheme.method2879(
                                 settingRenderContext.mouseX,
                                 settingRenderContext.mouseY,
                                 local1849_15,
                                 local1849_13,
                                 8.67F * settingRenderContext.scale,
                                 8.67F * settingRenderContext.scale
                              )
                        )
               )
            );
            settingRenderContext.method3547(
               local1849_11.field07390 ? "O" : "N",
               local1849_15 + 4.33F * settingRenderContext.scale,
               local1849_13 + 4.33F * settingRenderContext.scale,
               4.5F * settingRenderContext.scale,
               local1849_11.field07390
                  ? UiTheme.field09686
                  : UiTheme.method2881(
                     UiTheme.field09682,
                     UiTheme.field09680,
                     settingRenderContext.method3544(
                        "pc:tgh:" + index,
                        local1849_5
                           && UiTheme.method2879(
                              settingRenderContext.mouseX,
                              settingRenderContext.mouseY,
                              local1849_15,
                              local1849_13,
                              8.67F * settingRenderContext.scale,
                              8.67F * settingRenderContext.scale
                           )
                     )
                  )
            );
            Render2D.method4715(
               local1849_16,
               local1849_13,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               UiTheme.method2877(UiTheme.field09686, 0.05F)
            );
            Render2D.method4769(
               local1849_16,
               local1849_13,
               8.67F * settingRenderContext.scale,
               8.67F * settingRenderContext.scale,
               2.0F * settingRenderContext.scale,
               0.35F * settingRenderContext.scale,
               UiTheme.method2877(
                  UiTheme.field09686,
                  0.06F
                     + 0.13F
                        * settingRenderContext.method3544(
                           "pc:trh:" + index,
                           local1849_5
                              && UiTheme.method2879(
                                 settingRenderContext.mouseX,
                                 settingRenderContext.mouseY,
                                 local1849_16,
                                 local1849_13,
                                 8.67F * settingRenderContext.scale,
                                 8.67F * settingRenderContext.scale
                              )
                        )
               )
            );
            settingRenderContext.method3547(
               "P",
               local1849_16 + 4.33F * settingRenderContext.scale,
               local1849_13 + 4.33F * settingRenderContext.scale,
               4.5F * settingRenderContext.scale,
               UiTheme.method2881(
                  UiTheme.field09682,
                  UiTheme.field09680,
                  settingRenderContext.method3544(
                     "pc:trh:" + index,
                     local1849_5
                        && UiTheme.method2879(
                           settingRenderContext.mouseX,
                           settingRenderContext.mouseY,
                           local1849_16,
                           local1849_13,
                           8.67F * settingRenderContext.scale,
                           8.67F * settingRenderContext.scale
                        )
                  )
               )
            );
            int index3 = index;
            if (local1849_5) {
               settingRenderContext.method3554(local1849_15, local1849_13, 8.67F * settingRenderContext.scale, 8.67F * settingRenderContext.scale, floatArray, () -> {
                  local1849_11.field07390 = !local1849_11.field07390;
                  potionPreset.method2167();
               });
            }

            if (local1849_5) {
               settingRenderContext.method3554(local1849_16, local1849_13, 8.67F * settingRenderContext.scale, 8.67F * settingRenderContext.scale, floatArray, () -> {
                  potionPreset.field07362.remove(index3);
                  potionPreset.method2167();
               });
            }
         }
      }

      RoundedClipStack.method4838();
      Render2D.method4661(settingRenderContext.drawContext);
   }
}
