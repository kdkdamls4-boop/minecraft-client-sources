package deluxe.client.ui;

import deluxe.client.util.Easings;
import deluxe.client.render.FontFace;
import deluxe.client.util.ItemDrawOptions;
import deluxe.client.module.player.PotionThrowerModule;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;
import deluxe.client.ui.ScreenClosingTransition;
import deluxe.client.render.ItemRenderCache;
import deluxe.client.util.ServerItemType;
import deluxe.client.util.Animation;
import deluxe.client.ui.UiTheme;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.storage.AutoBuyItem;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;
import deluxe.client.render.Render2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.Window;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class PotionWheelScreen extends Screen implements ScreenClosingTransition.Nested0386 {
   private final ArrayList<Animation> field13448 = new ArrayList<>();
   private final Animation field13449 = new Animation();
   private final Animation field13450 = new Animation();
   private final ArrayList<Animation> field13451 = new ArrayList<>();
   private final HashMap<String, ItemStack> field13452 = new HashMap<>();
   private int field13453 = -1;
   private int field13454 = -1;
   private int field13455 = -1;
   private int field13456 = -1;
   private float field13457 = 92.0F;
   private float field13458;
   private float field13459;
   private float field13460;
   private float field13461;
   private float field13462;
   private float field13463;
   private boolean field13464;

   private static PotionThrowerModule thrower() {
      return PotionThrowerModule.method2641();
   }

   @Override
   protected void init() {
      ScreenClosingTransition.method1461(this);
      this.field13464 = false;
      this.field13453 = -1;
      this.field13454 = -1;
      this.field13455 = -1;
      this.field13449.animateTo(1.0, 0.22, Easings.EASE_OUT_QUAD);
   }

   @Override
   public void renderBackground(DrawContext drawContext, int local6362_2, int local6362_3, float local6362_4) {
   }

   private void drawSlot(int local6363_1, float local6363_2, float local6363_3, float local6363_4, float local6363_5, float local6363_6) {
      AutoBuyItem autoBuyItem = thrower().method2634(local6363_1);
      if (autoBuyItem == null) {
         float local6363_8 = 20.0F * local6363_4;
         FontText.method1405(
            FontFace.BOLD,
            "+",
            local6363_2,
            local6363_3 - local6363_8 / 2.0F,
            local6363_8,
            UiTheme.method2877(HudStyle.method4815(0.4F + 0.3F * local6363_6), local6363_5)
         );
      } else {
         float local6363_8 = 20.0F * local6363_4;
         float local6363_9 = Math.round((local6363_2 - local6363_8 / 2.0F) * 2.0F) / 2.0F;
         float local6363_10 = Math.round((local6363_3 - local6363_8 / 2.0F) * 2.0F) / 2.0F;
         ItemRenderCache.method2336(this.potionStack(autoBuyItem), local6363_9, local6363_10, local6363_8, ItemDrawOptions.noDecorations(local6363_5));
      }
   }

   private void drawPicker(float local6364_1, float local6364_2, float local6364_3, float local6364_4) {
      ArrayList<AutoBuyItem> local6364_5 = thrower().method2639();
      ArrayList<String> local6364_6 = this.pickerRows(local6364_5);
      float local6364_7 = 0.0F;

      for (String local6364_9 : local6364_6) {
         local6364_7 = Math.max(local6364_7, FontText.method1396(FontFace.SEMI_BOLD, local6364_9, 6.0F));
      }

      this.field13462 = 16.0F + local6364_7 + 6.0F;
      this.field13463 = local6364_6.size() * 13.0F + 4.0F;
      this.field13460 = local6364_1;
      this.field13461 = Math.max(4.0F, Math.min(local6364_3 - this.field13463 - 4.0F, local6364_2 - this.field13463 / 2.0F));
      float local6364_8 = 6.0F * (1.0F - local6364_4);
      float local6364_9 = this.field13460 + local6364_8;
      HudStyle.method4817(local6364_9, this.field13461, this.field13462, this.field13463, local6364_4, 6.0F);
      this.field13456 = -1;

      while (this.field13451.size() < local6364_6.size()) {
         this.field13451.add(new Animation());
      }

      float local6364_10 = this.field13450.floatValue();

      for (int index = 0; index < local6364_6.size(); index++) {
         float local6364_12 = this.field13461 + 2.0F + index * 13.0F;
         boolean local6364_13 = this.pickerOpen()
            && this.field13458 >= local6364_9
            && this.field13458 <= local6364_9 + this.field13462
            && this.field13459 >= local6364_12
            && this.field13459 < local6364_12 + 13.0F;
         if (local6364_13) {
            this.field13456 = index;
         }

         Animation animation = this.field13451.get(index);
         animation.update();
         animation.animateTo(local6364_13 ? 1.0 : 0.0, 0.16, Easings.EASE_OUT_QUAD);
         float local6364_15 = animation.floatValue();
         float local6364_16 = (float)Easings.EASE_OUT_QUART
            .apply(Math.max(0.0F, Math.min(1.0F, local6364_10 * (1.0F + local6364_6.size() * 0.12F) - index * 0.12F)));
         float local6364_17 = local6364_4 * local6364_16 * (0.72F + 0.28F * local6364_15);
         float local6364_18 = local6364_9 + 4.0F + 6.0F * (1.0F - local6364_16) + 2.0F * local6364_15;
         boolean local6364_19 = index >= local6364_5.size();
         if (!local6364_19) {
            ItemRenderCache.method2336(
               this.potionStack(local6364_5.get(index)),
               local6364_18,
               local6364_12 + 2.5F,
               8.0F,
               ItemDrawOptions.noDecorations(local6364_4 * local6364_16)
            );
         }

         if (local6364_19) {
            FontText.method1397(
               FontFace.SEMI_BOLD,
               local6364_6.get(index),
               local6364_18 + 8.0F + 4.0F,
               local6364_12 + 6.5F - 3.0F,
               6.0F,
               UiTheme.method2877(UiTheme.method2881(HudStyle.method4815(1.0F), -42386, local6364_15), local6364_17)
            );
         } else {
            FontText.method1400(
               FontFace.SEMI_BOLD,
               this.potionName(local6364_5.get(index)),
               local6364_18 + 8.0F + 4.0F,
               local6364_12 + 6.5F - 3.0F,
               6.0F,
               UiTheme.method2877(HudStyle.method4820(1.0F), local6364_17)
            );
         }
      }
   }

   private ArrayList<String> pickerRows(ArrayList<AutoBuyItem> local6365_1) {
      ArrayList<String> local6365_2 = new ArrayList<>();

      for (AutoBuyItem autoBuyItem : local6365_1) {
         local6365_2.add(autoBuyItem.name());
      }

      if (this.field13455 != -1 && this.field13455 < slots().size() && !slots().get(this.field13455).isEmpty()) {
         local6365_2.add("Очистить");
      }

      return local6365_2;
   }

   @Override
   public boolean shouldPause() {
      return false;
   }

   @Override
   public boolean renderClosing(DrawContext drawContext) {
      this.drawWheel(drawContext, false);
      return this.field13449.floatValue() > 0.01F;
   }

   private void closePicker() {
      this.field13455 = -1;
      this.field13456 = -1;
      if (!thrower().method2630()) {
         this.startClosing();
      }
   }

   private ItemStack potionStack(AutoBuyItem autoBuyItem) {
      return this.field13452
         .computeIfAbsent(
            autoBuyItem.name(),
            local6368_1 -> {
               ServerItemType serverItemType = ServerItemType.method2460(local6368_1);
               ItemStack itemStack = autoBuyItem.displayStack().copy();
               if (serverItemType != null) {
                  itemStack.set(
                     DataComponentTypes.POTION_CONTENTS,
                     new PotionContentsComponent(Optional.empty(), Optional.of(serverItemType.method2458() | 0xFF000000), List.of(), Optional.empty())
                  );
               }

               return itemStack;
            }
         );
   }

   private void clampCursor(MinecraftClient minecraftClient, float local6371_2, float local6371_3) {
      if (minecraftClient.isWindowFocused()) {
         Window window = minecraftClient.getWindow();
         double local6371_5 = (double)window.getFramebufferWidth() / local6371_2;
         double local6371_7 = (double)window.getFramebufferHeight() / local6371_3;
         double local6371_9 = local6371_2 / 2.0 * local6371_5;
         double local6371_11 = local6371_3 / 2.0 * local6371_7;
         double local6371_13 = minecraftClient.mouse.getX() - local6371_9;
         double local6371_15 = minecraftClient.mouse.getY() - local6371_11;
         double local6371_17 = (this.field13457 - 1.0) * local6371_5;
         double local6371_19 = Math.hypot(local6371_13, local6371_15);
         if (local6371_19 > local6371_17) {
            double local6371_21 = local6371_17 / local6371_19;
            GLFW.glfwSetCursorPos(window.getHandle(), local6371_9 + local6371_13 * local6371_21, local6371_11 + local6371_15 * local6371_21);
         }
      }
   }

   public boolean closing() {
      return this.field13464;
   }

   private void openPicker(int local6373_1) {
      this.field13455 = local6373_1;
      this.field13454 = -1;
   }

   private static float space() {
      return GuiScale.method0797() / GuiScale.method0794();
   }

   public PotionWheelScreen() {
      super(Text.literal("PotionWheel"));
   }

   @Override
   public void render(DrawContext drawContext, int local6376_2, int local6376_3, float local6376_4) {
      if (!this.field13464) {
         this.drawWheel(drawContext, true);
      }
   }

   public void releaseHeldSlot(int local6377_1) {
      if (this.field13454 != -1) {
         if (local6377_1 != -1 && local6377_1 != this.field13454 && local6377_1 < slots().size() && this.field13454 < slots().size()) {
            Collections.swap(slots(), this.field13454, local6377_1);
            thrower().method2631();
         }

         this.field13454 = -1;
      }
   }

   public int draggedSlot() {
      return this.field13454;
   }

   private Text potionName(AutoBuyItem autoBuyItem) {
      ServerItemType serverItemType = ServerItemType.method2460(autoBuyItem.name());
      return serverItemType != null ? serverItemType.method2459() : Text.literal(autoBuyItem.name());
   }

   public boolean pickerOpen() {
      return this.field13455 != -1;
   }

   private int hoveredAt(float local6381_1, float local6381_2, float local6381_3, float local6381_4, int local6381_5) {
      float local6381_6 = local6381_1 - local6381_3;
      float local6381_7 = local6381_2 - local6381_4;
      if (Math.hypot(local6381_6, local6381_7) > this.field13457 * 1.25F) {
         return -1;
      }

      double local6381_8 = 360.0 / local6381_5;
      double local6381_10 = Math.toDegrees(Math.atan2(local6381_7, local6381_6)) + 90.0 + local6381_8 / 2.0;

      while (local6381_10 < 0.0) {
         local6381_10 += 360.0;
      }

      local6381_10 %= 360.0;
      return Math.min(local6381_5 - 1, (int)(local6381_10 / local6381_8));
   }

   public void startClosing() {
      if (!this.field13464) {
         this.field13464 = true;
         this.field13453 = -1;
         this.field13454 = -1;
         this.field13455 = -1;
         this.field13449.animateTo(0.0, 0.22, Easings.EASE_OUT_QUAD);
         ScreenClosingTransition.method1458(this);
         if (this.client != null && this.client.currentScreen == this) {
            this.client.setScreen(null);
         }
      }
   }

   private static ArrayList<String> slots() {
      return thrower().method2636();
   }

   private void drawWheel(DrawContext drawContext, boolean local6384_2) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.player != null) {
         Render2D.method4713(drawContext);
         HudRenderContext.method4333(drawContext);
         float local6384_4 = this.width * space();
         float local6384_5 = this.height * space();
         float local6384_6 = local6384_4 / 2.0F;
         float local6384_7 = local6384_5 / 2.0F;
         this.field13457 = Math.min(140.0F, Math.max(56.0F, Math.min(local6384_4, local6384_5) * 0.17F));
         if (local6384_2 && !this.pickerOpen()) {
            this.clampCursor(minecraftClient, local6384_4, local6384_5);
         }

         this.field13449.update();
         this.field13450.update();
         this.field13450.animateTo(this.pickerOpen() ? 1.0 : 0.0, this.pickerOpen() ? 0.32 : 0.16, Easings.EASE_OUT_QUAD);
         this.field13458 = (float)minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * space();
         this.field13459 = (float)minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * space();
         int local6384_8 = slots().size();

         while (this.field13448.size() < local6384_8) {
            this.field13448.add(new Animation());
         }

         this.field13453 = local6384_2 && !this.field13464 && !this.pickerOpen()
            ? this.hoveredAt(this.field13458, this.field13459, local6384_6, local6384_7, local6384_8)
            : -1;
         float local6384_9 = this.field13449.floatValue();
         if (!(local6384_9 <= 0.004F)) {
            float local6384_10 = this.field13457 * (0.88F + 0.12F * local6384_9);
            float local6384_11 = local6384_10 / 92.0F;
            float local6384_12 = 360.0F / local6384_8;

            for (int index = 0; index < local6384_8; index++) {
               Animation animation = this.field13448.get(index);
               animation.update();
               animation.animateTo(
                  index != this.field13453 && index != this.field13454 && index != this.field13455 ? 0.0 : 1.0,
                  0.18,
                  Easings.EASE_OUT_QUAD
               );
               float local6384_15 = animation.floatValue();
               float local6384_16 = -90.0F - local6384_12 / 2.0F + local6384_12 * index;
               double local6384_17 = Math.toRadians(local6384_16 + local6384_12 / 2.0F);
               float local6384_19 = 1.0F + 0.03F * local6384_15;
               HudRenderContext.method4336(local6384_6, local6384_7, local6384_19);
               ShapeDrawing.method3652(
                  ShapeDrawing.Nested0502.MIDDLE,
                  local6384_6,
                  local6384_7,
                  local6384_10,
                  local6384_16,
                  local6384_12,
                  6.0F * local6384_11 / local6384_19,
                  10.0F * local6384_11,
                  index == this.field13454
                     ? UiTheme.method2881(HudStyle.method4818(), HudStyle.method4811(1.0F), 0.35F)
                     : HudStyle.method4818(),
                  0.8F,
                  local6384_9
               );
               float local6384_20 = local6384_6 + (float)Math.cos(local6384_17) * local6384_10 * 0.62F;
               float local6384_21 = local6384_7 + (float)Math.sin(local6384_17) * local6384_10 * 0.62F;
               if (index != this.field13454) {
                  this.drawSlot(index, local6384_20, local6384_21, local6384_11, local6384_9, local6384_15);
               }

               HudRenderContext.method4337();
            }

            if (this.field13454 != -1) {
               AutoBuyItem autoBuyItem = thrower().method2634(this.field13454);
               if (autoBuyItem != null) {
                  float local6384_14 = 20.0F * local6384_11;
                  ItemRenderCache.method2336(
                     this.potionStack(autoBuyItem),
                     this.field13458 - local6384_14 / 2.0F,
                     this.field13459 - local6384_14 / 2.0F,
                     local6384_14,
                     ItemDrawOptions.noDecorations(local6384_9)
                  );
               }
            }

            int local6384_13 = this.field13454 != -1 ? this.field13454 : this.field13453;
            AutoBuyItem autoBuyItem2 = thrower().method2634(local6384_13);
            if (autoBuyItem2 != null) {
               Text text = this.potionName(autoBuyItem2);
               FontText.method1400(
                  FontFace.SEMI_BOLD,
                  text,
                  local6384_6 - FontText.method1396(FontFace.SEMI_BOLD, text.getString(), 6.0F) / 2.0F,
                  local6384_7 + local6384_10 + 6.0F,
                  6.0F,
                  UiTheme.method2877(HudStyle.method4820(1.0F), local6384_9)
               );
            }

            float local6384_15 = this.field13450.floatValue();
            if (local6384_15 > 0.004F) {
               this.drawPicker(local6384_6 + local6384_10 + 12.0F, local6384_7, local6384_5, local6384_9 * local6384_15);
            } else {
               this.field13456 = -1;
            }

            Render2D.method4754();
         }
      }
   }

   @Override
   public void close() {
      if (this.pickerOpen()) {
         this.closePicker();
      } else {
         this.startClosing();
      }
   }

   @Override
   public boolean mouseReleased(Click click) {
      if (click.button() == 0 && this.field13454 != -1) {
         this.releaseHeldSlot(this.field13453);
         return true;
      } else {
         return super.mouseReleased(click);
      }
   }

   public int hoveredSlot() {
      return this.field13453;
   }

   @Override
   public boolean mouseClicked(Click click, boolean local6388_2) {
      if (this.field13464) {
         return true;
      }

      if (this.pickerOpen()) {
         if (click.button() == 0 && this.field13456 != -1) {
            ArrayList<AutoBuyItem> local6388_3 = thrower().method2639();
            ArrayList<String> local6388_4 = this.pickerRows(local6388_3);
            if (this.field13456 < local6388_3.size()) {
               slots().set(this.field13455, local6388_3.get(this.field13456).name());
            } else if (local6388_4.get(this.field13456).equals("Очистить")) {
               slots().set(this.field13455, "");
            }

            thrower().method2631();
         }

         this.closePicker();
         return true;
      } else {
         if (this.field13453 != -1) {
            if (click.button() == 1) {
               this.openPicker(this.field13453);
               return true;
            }

            if (click.button() == 0) {
               if (thrower().method2634(this.field13453) != null) {
                  this.field13454 = this.field13453;
               } else {
                  this.openPicker(this.field13453);
               }

               return true;
            }
         }

         return super.mouseClicked(click, local6388_2);
      }
   }
}
