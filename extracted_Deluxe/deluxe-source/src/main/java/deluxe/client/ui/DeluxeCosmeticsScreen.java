package deluxe.client.ui;

import deluxe.client.cosmetic.CosmeticCatalog;
import deluxe.client.cosmetic.CosmeticCategory;
import deluxe.client.cosmetic.CosmeticEntry;
import deluxe.client.cosmetic.CosmeticPreviewAtlas;
import deluxe.client.cosmetic.CosmeticStore;
import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.module.misc.GlobalsModule;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;
import deluxe.client.util.Animation;
import deluxe.client.ui.UiTheme;
import deluxe.client.cosmetic.CosmeticAccess;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class DeluxeCosmeticsScreen extends Screen {
   private static final CosmeticCategory[] recoveredClass0304Array = new CosmeticCategory[]{
      CosmeticCategory.HAT,
      CosmeticCategory.ACCESSORY,
      CosmeticCategory.WINGS,
      CosmeticCategory.SHIELD,
      CosmeticCategory.SWORD,
      CosmeticCategory.MACE,
      CosmeticCategory.PET,
      CosmeticCategory.BODYWEAR
   };
   private final Animation field06059 = new Animation();
   private final Animation field06060 = new Animation();
   private final Animation[] field06061 = new Animation[recoveredClass0304Array.length];
   private final Animation[] field06062 = new Animation[7];
   private final int[] field06063 = new int[recoveredClass0304Array.length];
   private int field06064 = 0;
   private int field06065 = -1;
   private boolean field06066;
   private float field06067;
   private float field06068;
   private float field06069;

   @Override
   protected void init() {
      this.field06066 = false;
      this.field06059.animateTo(1.0, 0.22, Easings.EASE_OUT_QUART);
      this.field06060.resetTo(0.0);
      this.field06060.animateTo(1.0, 0.28, Easings.EASE_OUT_QUART);
   }

   @Override
   public boolean mouseScrolled(double local2848_1, double local2848_3, double local2848_5, double local2848_7) {
      int local2848_9 = this.options(recoveredClass0304Array[this.field06064]).size();
      if (local2848_9 <= 7) {
         return true;
      }

      this.field06063[this.field06064] = Math.max(0, Math.min(local2848_9 - 7, this.field06063[this.field06064] - (int)Math.signum(local2848_7)));
      return true;
   }

   private void drawCaption(float local2849_1, float local2849_2, float local2849_3) {
      CosmeticCategory cosmeticCategory = recoveredClass0304Array[this.field06064];
      List<CosmeticEntry> local2849_5 = this.options(cosmeticCategory);
      CosmeticEntry cosmeticEntry = CosmeticStore.method3519(cosmeticCategory);
      String local2849_7;
      if (this.field06065 >= 0) {
         CosmeticEntry cosmeticEntry2 = local2849_5.get(this.field06063[this.field06064] + this.field06065);
         if (cosmeticEntry2 == null) {
            local2849_7 = cosmeticEntry == null ? "Ничего не надето" : "Снять " + cosmeticEntry.name();
         } else {
            local2849_7 = cosmeticEntry2.name()
               + (!CosmeticAccess.method3033(cosmeticEntry2) ? " · только бета" : (CosmeticStore.method3512(cosmeticEntry2) ? " · надето" : ""));
         }
      } else if (local2849_5.size() <= 1) {
         local2849_7 = "Здесь пока пусто";
      } else {
         local2849_7 = cosmeticEntry == null ? "Ничего не надето" : "Надето: " + cosmeticEntry.name();
      }

      float local2849_8 = this.field06068 + 220.0F * this.field06069;
      FontText.method1405(
         FontFace.SEMI_BOLD,
         cosmeticCategory.method4309(),
         this.field06067,
         local2849_8,
         13.0F * this.field06069,
         UiTheme.method2877(UiTheme.field09680, local2849_3)
      );
      FontText.method1405(
         FontFace.SEMI,
         local2849_7,
         this.field06067,
         local2849_8 + 17.0F * this.field06069,
         8.5F * this.field06069,
         UiTheme.method2877(UiTheme.field09680, local2849_3 * 0.62F)
      );
      FontText.method1405(
         FontFace.SEMI,
         "ЛКМ — надеть     ПКМ — снять     Колёсико — листать     Esc — закрыть",
         this.field06067,
         local2849_8 + 32.0F * this.field06069,
         7.5F * this.field06069,
         UiTheme.method2877(UiTheme.field09680, local2849_3 * 0.4F)
      );
   }

   private List<CosmeticEntry> options(CosmeticCategory cosmeticCategory) {
      ArrayList<CosmeticEntry> local2850_2 = new ArrayList<>();
      local2850_2.add(null);
      local2850_2.addAll(CosmeticCatalog.method0523(cosmeticCategory));
      return local2850_2;
   }

   private void beginClosing() {
      if (!this.field06066) {
         this.field06066 = true;
         this.field06059.animateTo(0.0, 0.16, Easings.EASE_OUT_QUART);
      }
   }

   private int optionCount(int local2852_1) {
      return Math.min(7, this.options(recoveredClass0304Array[local2852_1]).size() - this.field06063[local2852_1]);
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local2853_2, int local2853_3, float local2853_4) {
   }

   private void drawOptions(float local2854_1) {
      List<CosmeticEntry> local2854_2 = this.options(recoveredClass0304Array[this.field06064]);
      int local2854_3 = this.field06063[this.field06064];
      int local2854_4 = this.optionCount(this.field06064);
      float local2854_5 = this.field06060.floatValue();
      CosmeticEntry cosmeticEntry = CosmeticStore.method3519(recoveredClass0304Array[this.field06064]);

      for (int index = 0; index < 7; index++) {
         Animation animation = this.field06062[index];
         animation.update();
         animation.animateTo(index == this.field06065 ? 1.0 : 0.0, 0.14, Easings.EASE_OUT_QUART);
         if (index < local2854_4) {
            CosmeticEntry cosmeticEntry2 = local2854_2.get(local2854_3 + index);
            float[] floatArray = this.optionCenter(index, local2854_4);
            float local2854_11 = Math.max(0.0F, Math.min(1.0F, local2854_5 * 1.6F - index * 0.08F));
            float local2854_12 = animation.floatValue();
            boolean local2854_13 = cosmeticEntry2 == null ? cosmeticEntry == null : CosmeticStore.method3512(cosmeticEntry2);
            boolean local2854_14 = cosmeticEntry2 != null && !CosmeticAccess.method3033(cosmeticEntry2);
            float local2854_15 = local2854_1 * local2854_11 * (local2854_14 ? 0.28F : 0.55F + 0.45F * Math.max(local2854_12, local2854_13 ? 1.0F : 0.0F));
            float local2854_16 = 35.0F * this.field06069 * (1.0F + 0.14F * local2854_12 + (local2854_13 ? 0.04F : 0.0F));
            if (cosmeticEntry2 == null) {
               float local2854_17 = 7.0F * this.field06069;
               Render2D.method4664(
                  floatArray[0],
                  floatArray[1],
                  local2854_17,
                  1.2F * this.field06069,
                  UiTheme.method2877(UiTheme.field09680, local2854_15)
               );
               Render2D.method4679(
                  floatArray[0] - local2854_17 * 0.62F,
                  floatArray[1] + local2854_17 * 0.62F,
                  floatArray[0] + local2854_17 * 0.62F,
                  floatArray[1] - local2854_17 * 0.62F,
                  1.2F * this.field06069,
                  UiTheme.method2877(UiTheme.field09680, local2854_15)
               );
            } else {
               CosmeticPreviewAtlas.Nested0499 local2854_17 = CosmeticPreviewAtlas.method3629(cosmeticEntry2);
               if (local2854_17 != null) {
                  Render2D.method4695(
                     local2854_17.atlasTexture(),
                     floatArray[0] - local2854_16 / 2.0F,
                     floatArray[1] - local2854_16 / 2.0F,
                     local2854_16,
                     local2854_16,
                     0.0F,
                     local2854_17.u0(),
                     local2854_17.v0(),
                     local2854_17.u1(),
                     local2854_17.v1(),
                     UiTheme.method2877(UiTheme.field09680, local2854_15)
                  );
               } else {
                  Render2D.method4664(
                     floatArray[0],
                     floatArray[1],
                     8.0F * this.field06069,
                     1.0F * this.field06069,
                     UiTheme.method2877(UiTheme.field09680, local2854_15 * 0.6F)
                  );
               }
            }

            if (local2854_13) {
               float local2854_17 = 185.0F * this.field06069 + 23.0F * this.field06069;
               Render2D.method4719(
                  this.field06067 + (float)Math.cos(floatArray[2]) * local2854_17,
                  this.field06068 + (float)Math.sin(floatArray[2]) * local2854_17,
                  1.8F * this.field06069,
                  UiTheme.method2877(UiTheme.field09686, local2854_1 * local2854_11)
               );
            }
         }
      }

      if (local2854_2.size() > 7) {
         float local2854_7 = 185.0F * this.field06069;
         double local2854_8 = this.optionCenter(0, local2854_4)[2] - Math.toRadians(8.0);
         double local2854_10 = this.optionCenter(local2854_4 - 1, local2854_4)[2] + Math.toRadians(8.0);
         if (local2854_3 > 0) {
            Render2D.method4719(
               this.field06067 + (float)Math.cos(local2854_8) * local2854_7,
               this.field06068 + (float)Math.sin(local2854_8) * local2854_7,
               1.3F * this.field06069,
               UiTheme.method2877(UiTheme.field09680, local2854_1 * 0.4F)
            );
         }

         if (local2854_3 + local2854_4 < local2854_2.size()) {
            Render2D.method4719(
               this.field06067 + (float)Math.cos(local2854_10) * local2854_7,
               this.field06068 + (float)Math.sin(local2854_10) * local2854_7,
               1.3F * this.field06069,
               UiTheme.method2877(UiTheme.field09680, local2854_1 * 0.4F)
            );
         }
      }
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean keyPressed(KeyInput keyInput) {
      if (keyInput.key() != 256
         && (!GlobalsModule.method0643().keybindSetting.isBound() || keyInput.key() != GlobalsModule.method0643().keybindSetting.getKeyCode())) {
         return super.keyPressed(keyInput);
      }

      this.beginClosing();
      return true;
   }

   private void updateHover(float local2857_1, float local2857_2) {
      this.field06065 = -1;
      if (!this.field06066) {
         float local2857_3 = local2857_1 - this.field06067;
         float local2857_4 = local2857_2 - this.field06068;
         float local2857_5 = (float)Math.hypot(local2857_3, local2857_4);
         if (local2857_5 > 60.0F * this.field06069 && local2857_5 < 162.0F * this.field06069) {
            double local2857_6 = Math.toDegrees(Math.atan2(local2857_4, local2857_3)) + 90.0 + 22.5;

            while (local2857_6 < 0.0) {
               local2857_6 += 360.0;
            }

            int local2857_8 = (int)(local2857_6 % 360.0 / 45.0);
            if (local2857_8 != this.field06064) {
               this.field06064 = local2857_8;
               this.field06060.resetTo(0.0);
               this.field06060.animateTo(1.0, 0.28, Easings.EASE_OUT_QUART);
            }
         }

         int local2857_6 = this.optionCount(this.field06064);

         for (int index = 0; index < local2857_6; index++) {
            float[] floatArray = this.optionCenter(index, local2857_6);
            if (Math.hypot(local2857_1 - floatArray[0], local2857_2 - floatArray[1]) < 19.0F * this.field06069) {
               this.field06065 = index;
            }
         }
      }
   }

   public static float modelAlpha() {
      return MinecraftClient.getInstance().currentScreen instanceof DeluxeCosmeticsScreen deluxeCosmeticsScreen ? deluxeCosmeticsScreen.field06059.floatValue() : 1.0F;
   }

   private float[] optionCenter(int local2859_1, int local2859_2) {
      double local2859_3 = Math.toRadians(-90.0 + this.field06064 * 45.0 + (local2859_1 - (local2859_2 - 1) / 2.0) * 12.5);
      float local2859_5 = 185.0F * this.field06069 * (0.94F + 0.06F * this.field06060.floatValue());
      return new float[]{
         this.field06067 + (float)Math.cos(local2859_3) * local2859_5, this.field06068 + (float)Math.sin(local2859_3) * local2859_5, (float)local2859_3
      };
   }

   @Override
   public void close() {
      this.beginClosing();
   }

   @Override
   public void render(DrawContext drawContext, int local2861_2, int local2861_3, float local2861_4) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null) {
         InterfaceModule.method2247().method2250();
         float local2861_6 = space();
         float local2861_7 = this.width * local2861_6;
         float local2861_8 = this.height * local2861_6;
         this.field06069 = Math.min(local2861_7 / 960.0F, local2861_8 / 540.0F);
         this.field06067 = local2861_7 / 2.0F;
         this.field06068 = local2861_8 / 2.0F;
         float local2861_9 = local2861_2 * local2861_6;
         float local2861_10 = local2861_3 * local2861_6;
         this.field06059.update();
         this.field06060.update();
         float local2861_11 = this.field06059.floatValue();
         if (this.field06066 && local2861_11 <= 0.01F) {
            minecraftClient.execute(() -> {
               if (minecraftClient.currentScreen == this) {
                  minecraftClient.setScreen(null);
               }
            });
         } else {
            this.updateHover(local2861_9, local2861_10);
            Render2D.method4713(drawContext);
            HudRenderContext.method4333(drawContext);
            float local2861_12 = 0.92F + 0.08F * local2861_11;
            float local2861_13 = 107.0F * this.field06069 * local2861_12;
            float local2861_14 = 144.0F * this.field06069 * local2861_12;

            for (int index = 0; index < recoveredClass0304Array.length; index++) {
               Animation animation = this.field06061[index];
               animation.update();
               animation.animateTo(index == this.field06064 ? 1.0 : 0.0, 0.16, Easings.EASE_OUT_QUART);
               float local2861_17 = animation.floatValue();
               float local2861_18 = -90.0F + index * 45.0F - 22.5F;
               ShapeDrawing.method3648(
                  ShapeDrawing.Nested0502.MIDDLE,
                  this.field06067,
                  this.field06068,
                  local2861_13,
                  local2861_14,
                  local2861_18,
                  45.0F,
                  3.5F * this.field06069,
                  5.0F * this.field06069,
                  UiTheme.method2881(HudStyle.method4818(), UiTheme.field09680, 0.16F * local2861_17),
                  0.8F * (1.0F - 0.28F * local2861_17),
                  local2861_11
               );
               double local2861_19 = Math.toRadians(-90.0 + index * 45.0);
               float local2861_21 = this.field06067 + (float)Math.cos(local2861_19) * (local2861_13 + local2861_14) / 2.0F;
               float local2861_22 = this.field06068 + (float)Math.sin(local2861_19) * (local2861_13 + local2861_14) / 2.0F;
               float local2861_23 = 16.0F * this.field06069;
               Render2D.method4777(
                  recoveredClass0304Array[index].method4312(),
                  local2861_21 - local2861_23 / 2.0F,
                  local2861_22 - local2861_23 / 2.0F,
                  local2861_23,
                  local2861_23,
                  0.0F,
                  UiTheme.method2877(UiTheme.field09680, local2861_11 * (0.5F + 0.5F * local2861_17))
               );
               if (CosmeticStore.method3519(recoveredClass0304Array[index]) != null && local2861_17 < 0.99F) {
                  Render2D.method4719(
                     this.field06067 + (float)Math.cos(local2861_19) * (local2861_14 + 8.0F * this.field06069),
                     this.field06068 + (float)Math.sin(local2861_19) * (local2861_14 + 8.0F * this.field06069),
                     1.8F * this.field06069,
                     UiTheme.method2877(UiTheme.field09686, local2861_11 * (1.0F - local2861_17))
                  );
               }
            }

            this.drawOptions(local2861_11);
            this.drawCaption(local2861_7, local2861_8, local2861_11);
            Render2D.method4754();
            if (local2861_11 > 0.01F) {
               int local2861_15 = (int)(60.0F * this.field06069 / local2861_6);
               int local2861_16 = (int)(95.0F * this.field06069 / local2861_6);
               int local2861_17 = (int)(this.field06067 / local2861_6);
               int local2861_18 = (int)(this.field06068 / local2861_6);
               InventoryScreen.drawEntity(
                  drawContext,
                  local2861_17 - local2861_15,
                  local2861_18 - local2861_16,
                  local2861_17 + local2861_15,
                  local2861_18 + local2861_16,
                  (int)(82.0F * this.field06069 / local2861_6),
                  0.0625F,
                  local2861_2,
                  local2861_3,
                  minecraftClient.player
               );
            }
         }
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean local2864_2) {
      if (this.field06066) {
         return true;
      }

      CosmeticCategory cosmeticCategory = recoveredClass0304Array[this.field06064];
      if (click.button() == 1) {
         CosmeticStore.method3517(cosmeticCategory);
         return true;
      }

      if (click.button() == 0 && this.field06065 >= 0) {
         CosmeticEntry cosmeticEntry = this.options(cosmeticCategory).get(this.field06063[this.field06064] + this.field06065);
         if (cosmeticEntry == null) {
            CosmeticStore.method3517(cosmeticCategory);
         } else {
            CosmeticStore.method3513(cosmeticEntry);
         }

         return true;
      } else {
         return super.mouseClicked(click, local2864_2);
      }
   }

   public DeluxeCosmeticsScreen() {
      super(Text.literal("Deluxe Cosmetics"));

      for (int index = 0; index < recoveredClass0304Array.length; index++) {
         this.field06061[index] = new Animation();
      }

      for (int index2 = 0; index2 < 7; index2++) {
         this.field06062[index2] = new Animation();
      }
   }

   private static float space() {
      return GuiScale.method0797() / GuiScale.method0794();
   }
}
