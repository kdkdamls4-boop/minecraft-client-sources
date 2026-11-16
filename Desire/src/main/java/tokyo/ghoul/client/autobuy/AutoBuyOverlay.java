package tokyo.ghoul.client.autobuy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.client.features.modules.misc.FlowAutoBuyModule;
import tokyo.ghoul.client.ui.autobuy.AutoBuyItemsScreen;

public final class AutoBuyOverlay {
   private static final float LEFT_PANEL_WIDTH = 120.0F;
   private static final float RIGHT_PANEL_WIDTH = 176.0F;
   private static final float BUTTON_HEIGHT = 20.0F;
   private static final float BUTTON_GAP = 4.0F;
   private static final float ROW_HEIGHT = 22.0F;

   private static float historyScroll = 0.0F;
   private static float maxHistoryScroll = 0.0F;
   private static boolean isDraggingSlider = false;
   private static boolean isDraggingDurabilitySlider = false;
   public static volatile long suspiciousNotificationUntil = 0L;

   public static void showSuspiciousNotification() {
      suspiciousNotificationUntil = System.currentTimeMillis() + 4500L;
   }

   private AutoBuyOverlay() {
   }

   public static boolean shouldShow(AbstractContainerScreen<?> screen) {
      if (!(screen instanceof ContainerScreen gcs)) {
         return false;
      } else {
         if (AutoBuyEngine.isAuctionScreen(gcs)) {
            return true;
         }

         if (gcs.getMenu().getRowCount() != 6) {
            return false;
         }

         String title = gcs.getTitle().getString();
         return title != null && title.toLowerCase(Locale.ROOT).contains("аукцион");
      }
   }

   public static void render(GuiGraphicsExtractor context, int gx, int gy, int gw, int gh, int mouseX, int mouseY) {
      Minecraft mc = Minecraft.getInstance();
      Font tr = mc.font;
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      boolean parsingActive = AutoBuyEngine.INSTANCE.parser.isActive();

      // =========================================================
      // 1. ЛЕВАЯ ПАНЕЛЬ: КНОПКИ УПРАВЛЕНИЯ И ПОЛЗУНОК СКИДКИ
      // =========================================================
      int leftPanelX = (int) (gx - LEFT_PANEL_WIDTH - 6.0F);
      if (leftPanelX < 2) {
         leftPanelX = 2;
      }
      int leftPanelY = gy;
      int leftPanelW = (int) LEFT_PANEL_WIDTH;

      int btnX = leftPanelX + 4;
      int btnW = leftPanelW - 8;
      int btnH = (int) BUTTON_HEIGHT;

      int btn0Y = leftPanelY + 20;
      int btn1Y = btn0Y + btnH + (int) BUTTON_GAP;
      int btn2Y = btn1Y + btnH + (int) BUTTON_GAP;
      int btn3Y = btn2Y + btnH + (int) BUTTON_GAP;
      int btn4Y = btn3Y + btnH + (int) BUTTON_GAP;
      int sliderDiscountY = btn4Y + btnH + (int) BUTTON_GAP;
      int sliderDurabilityY = sliderDiscountY + btnH + (int) BUTTON_GAP;

      int leftPanelH = Math.max(gh, sliderDurabilityY + btnH + 6 - leftPanelY);

      // Отрисовка фона и рамки левой панели в дефолтном стиле Minecraft
      drawVanillaBox(context, leftPanelX, leftPanelY, leftPanelW, leftPanelH, false);

      // Шапка левой панели
      drawText(context, tr, "§fУправление", leftPanelX + 6, leftPanelY + 5, 0xFFFFFF);
      context.fill(leftPanelX + 2, leftPanelY + 16, leftPanelX + leftPanelW - 2, leftPanelY + 17, 0xFF373737);

      // Обработка перетаскивания ползунка скидки мышью
      if (isDraggingSlider) {
         long window = mc.getWindow().handle();
         if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            isDraggingSlider = false;
         } else {
            updateSliderFromMouse(mouseX, btnX, btnW);
         }
      }

      // Обработка перетаскивания ползунка прочности брони мышью
      if (isDraggingDurabilitySlider) {
         long window = mc.getWindow().handle();
         if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            isDraggingDurabilitySlider = false;
         } else {
            updateDurabilitySliderFromMouse(mouseX, btnX, btnW);
         }
      }

      boolean hov0 = isHovered(mouseX, mouseY, btnX, btn0Y, btnW, btnH);
      boolean hov1 = isHovered(mouseX, mouseY, btnX, btn1Y, btnW, btnH);
      boolean hov2 = isHovered(mouseX, mouseY, btnX, btn2Y, btnW, btnH);
      boolean hov3 = isHovered(mouseX, mouseY, btnX, btn3Y, btnW, btnH);
      boolean hov4 = isHovered(mouseX, mouseY, btnX, btn4Y, btnW, btnH);
      boolean hovSliderDiscount = isHovered(mouseX, mouseY, btnX, sliderDiscountY, btnW, btnH);
      boolean hovSliderDurability = isHovered(mouseX, mouseY, btnX, sliderDurabilityY, btnW, btnH);

      // Кнопка 0: AutoBuy ВКЛ / ВЫКЛ
      String text0 = cfg.enabled ? "§aAutoBuy: ВКЛ" : "§cAutoBuy: ВЫКЛ";
      renderVanillaButton(context, tr, btnX, btn0Y, btnW, btnH, text0, hov0);

      // Кнопка 1: AutoSell ВКЛ / ВЫКЛ
      String text1 = cfg.autoSell ? "§aAutoSell: ВКЛ" : "§cAutoSell: ВЫКЛ";
      renderVanillaButton(context, tr, btnX, btn1Y, btnW, btnH, text1, hov1);

      // Кнопка 2: SafeMode ВКЛ / ВЫКЛ
      String text2 = cfg.safeMode ? "§aSafeMode: ВКЛ" : "§cSafeMode: ВЫКЛ";
      renderVanillaButton(context, tr, btnX, btn2Y, btnW, btnH, text2, hov2);

      // Кнопка 3: Парсинг цен
      String text3 = parsingActive ? "§eПарсер: СТОП" : "§fПарсить цены";
      renderVanillaButton(context, tr, btnX, btn3Y, btnW, btnH, text3, hov3);

      // Кнопка 4: Выбор предметов в стиле обычного майнкрафта
      String text4 = hov4 ? "§eВыбор предметов" : "§fВыбор предметов";
      renderVanillaButton(context, tr, btnX, btn4Y, btnW, btnH, text4, hov4);

      // Кнопка 5: Ползунок скидки перепродажи
      renderPercentSlider(context, tr, btnX, sliderDiscountY, btnW, btnH, "Скидка: " + cfg.sellDiscountPercent + "%", cfg.sellDiscountPercent, 50, hovSliderDiscount, isDraggingSlider);

      // Кнопка 6: Ползунок прочности брони
      renderPercentSlider(context, tr, btnX, sliderDurabilityY, btnW, btnH, "Броня: ≥" + cfg.armorDurabilityPercent + "%", cfg.armorDurabilityPercent, 100, hovSliderDurability, isDraggingDurabilitySlider);

      // Статус парсинга или подсказка под кнопками
      int infoY = sliderDurabilityY + btnH + 6;
      if (parsingActive) {
         String status = AutoBuyEngine.INSTANCE.parser.statusLine();
         String trimmedStatus = trimVanillaText(tr, status, leftPanelW - 8);
         drawText(context, tr, "§e" + trimmedStatus, leftPanelX + 4, infoY, 0xFFFFA0);
      }

      // =========================================================
      // 2. ПРАВАЯ ПАНЕЛЬ: ИСТОРИЯ ПОКУПОК СО СКРОЛЛОМ
      // =========================================================
      renderHistoryPanel(context, tr, gx, gy, gw, gh, mouseX, mouseY);

      // Уведомление на AH при подозрительной цене
      if (System.currentTimeMillis() < suspiciousNotificationUntil) {
         String warnText = "§c§lВНИМАНИЕ: ПОДОЗРИТЕЛЬНАЯ ЦЕНА!";
         int textW = tr.width(warnText);
         int bannerW = Math.max(160, textW + 20);
         int bannerH = 18;
         int bannerX = gx + (gw - bannerW) / 2;
         int bannerY = Math.max(2, gy - bannerH - 4);
         drawVanillaBox(context, bannerX, bannerY, bannerW, bannerH, false);
         drawText(context, tr, warnText, bannerX + (bannerW - textW) / 2, bannerY + 5, 0xFF5555);
      }
   }

   /**
    * Отрисовка ползунка в классическом стиле настроек звука/музыки Minecraft.
    */
   private static void renderPercentSlider(GuiGraphicsExtractor context, Font tr, int x, int y, int w, int h, String label, int percent, int maxPercent, boolean hover, boolean dragging) {
      // 1. Углубленный фон трека (дефолтный sunken box майнкрафта)
      context.fill(x, y, x + w, y + h, 0xD0101010);
      // Внешняя черная рамка
      context.fill(x, y, x + w, y + 1, 0xFF000000);
      context.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
      context.fill(x, y + 1, x + 1, y + h, 0xFF000000);
      context.fill(x + w - 1, y, x + w, y + h, 0xFF000000);
      // Внутренние затемнения трека
      context.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF373737);
      context.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF373737);
      context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
      context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF555555);

      // 2. Ползунок (слайдер-кнопка)
      int thumbW = 8;
      float progress = Mth.clamp((float) percent / (float) maxPercent, 0.0F, 1.0F);
      int thumbX = x + 1 + (int) (progress * (w - 2 - thumbW));
      int thumbY = y + 1;
      int thumbH = h - 2;

      boolean thumbHover = hover || dragging;
      context.fill(thumbX, thumbY, thumbX + thumbW, thumbY + thumbH, thumbHover ? 0xFF888888 : 0xFF666666);
      int light = thumbHover ? 0xFFFFFFFF : 0xFFB0B0B0;
      int dark = thumbHover ? 0xFFA0A0A0 : 0xFF333333;
      context.fill(thumbX, thumbY, thumbX + thumbW, thumbY + 1, light);
      context.fill(thumbX, thumbY, thumbX + 1, thumbY + thumbH, light);
      context.fill(thumbX, thumbY + thumbH - 1, thumbX + thumbW, thumbY + thumbH, dark);
      context.fill(thumbX + thumbW - 1, thumbY, thumbX + thumbW, thumbY + thumbH, dark);

      // 3. Надпись по центру ползунка с тенью
      int textW = tr.width(label);
      int tx = x + (w - textW) / 2;
      int ty = y + (h - 8) / 2;
      drawText(context, tr, label, tx, ty, thumbHover ? 0xFFFFA0 : 0xFFFFFF);
   }

   /**
    * Обновление процента скидки от положения мыши по шкале 0% - 50%.
    */
   private static void updateSliderFromMouse(double mouseX, int x, int w) {
      int thumbW = 8;
      float norm = (float) (mouseX - (x + 1 + thumbW / 2.0F)) / (float) (w - 2 - thumbW);
      norm = Mth.clamp(norm, 0.0F, 1.0F);
      int newPercent = Math.round(norm * 50.0F);
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (newPercent != cfg.sellDiscountPercent) {
         cfg.sellDiscountPercent = newPercent;
         FlowAutoBuyModule.getInstance().sellDiscount.setValue((float) newPercent);
         AutoBuyConfig.save();
      }
   }

   /**
    * Обновление процента минимальной прочности брони от положения мыши по шкале 0% - 100%.
    */
   private static void updateDurabilitySliderFromMouse(double mouseX, int x, int w) {
      int thumbW = 8;
      float norm = (float) (mouseX - (x + 1 + thumbW / 2.0F)) / (float) (w - 2 - thumbW);
      norm = Mth.clamp(norm, 0.0F, 1.0F);
      int newPercent = Math.round(norm * 100.0F);
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (newPercent != cfg.armorDurabilityPercent) {
         cfg.armorDurabilityPercent = newPercent;
         FlowAutoBuyModule.getInstance().armorDurability.setValue((float) newPercent);
         AutoBuyConfig.save();
      }
   }

   /**
    * Отрисовка кнопки в классическом дефолтном стиле интерфейса Minecraft.
    */
   private static void renderVanillaButton(GuiGraphicsExtractor context, Font tr, int x, int y, int w, int h, String text, boolean hover) {
      drawVanillaBox(context, x, y, w, h, hover);
      int textW = tr.width(text);
      int tx = x + (w - textW) / 2;
      int ty = y + (h - 8) / 2;
      drawText(context, tr, text, tx, ty, hover ? 0xFFFFA0 : 0xFFFFFF);
   }

   /**
    * Классическая дефолтная рамка Minecraft (темный полупрозрачный фон и скосы bevel).
    */
   private static void drawVanillaBox(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean hover) {
      // 1. Фон (полупрозрачный темно-серый фон дефолт майнкрафта)
      context.fill(x, y, x + w, y + h, hover ? 0xE0252525 : 0xD0101010);

      // 2. Внешняя черная рамка в 1 пиксель
      context.fill(x, y, x + w, y + 1, 0xFF000000);
      context.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
      context.fill(x, y + 1, x + 1, y + h, 0xFF000000);
      context.fill(x + w - 1, y, x + w, y + h, 0xFF000000);

      // 3. Внутренний скос (светлый сверху-слева, темный снизу-справа)
      int topLight = hover ? 0xFFFFFFFF : 0xFF8B8B8B;
      int botDark = hover ? 0xFFA0A0A0 : 0xFF373737;

      context.fill(x + 1, y + 1, x + w - 1, y + 2, topLight);
      context.fill(x + 1, y + 1, x + 2, y + h - 1, topLight);
      context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, botDark);
      context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, botDark);
   }

   /**
    * Отрисовка правой панели истории в дефолтном стиле Minecraft со скроллом (листанием).
    */
   private static void renderHistoryPanel(GuiGraphicsExtractor context, Font tr, int gx, int gy, int gw, int gh, int mouseX, int mouseY) {
      int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int panelX = gx + gw + 4;
      if (panelX + (int) RIGHT_PANEL_WIDTH > screenWidth) {
         panelX = gx - (int) RIGHT_PANEL_WIDTH - 4;
      }
      if (panelX < 0) {
         return;
      }

      int panelY = gy;
      int panelW = (int) RIGHT_PANEL_WIDTH;
      int panelH = gh;

      // 1. Отрисовка рамки панели в стиле дефолт майнкрафта
      drawVanillaBox(context, panelX, panelY, panelW, panelH, false);

      // 2. Шапка истории
      List<PurchaseRecord> history = AutoBuyConfig.cfg().history;
      drawText(context, tr, "§fИстория", panelX + 6, panelY + 5, 0xFFFFFF);
      String countStr = "§7(" + history.size() + ")";
      drawText(context, tr, countStr, panelX + panelW - 6 - tr.width(countStr), panelY + 5, 0xFFFFFF);

      // Разделитель шапки
      context.fill(panelX + 2, panelY + 16, panelX + panelW - 2, panelY + 17, 0xFF373737);

      // 3. Область элементов с поддержкой скролла
      int contentTop = panelY + 18;
      int contentBottom = panelY + panelH - 2;
      int contentHeight = contentBottom - contentTop;

      float totalContentHeight = history.size() * ROW_HEIGHT;
      maxHistoryScroll = Math.max(0.0F, totalContentHeight - contentHeight);
      historyScroll = Mth.clamp(historyScroll, 0.0F, maxHistoryScroll);

      boolean hasScrollbar = maxHistoryScroll > 0;
      int itemAreaWidth = panelW - (hasScrollbar ? 8 : 4);

      ItemStack hoveredStack = ItemStack.EMPTY;
      PurchaseRecord hoveredRecord = null;

      if (history.isEmpty()) {
         drawText(context, tr, "§7История пуста", panelX + 8, contentTop + 6, 0x888888);
      } else {
         context.enableScissor(panelX + 1, contentTop, panelX + panelW - 1, contentBottom);

         for (int i = 0; i < history.size(); i++) {
            PurchaseRecord rec = history.get(i);
            float rowY = contentTop - historyScroll + i * ROW_HEIGHT;

            // Отсечение невидимых строк
            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) {
               continue;
            }

            int iry = (int) rowY;
            boolean isRowHovered = mouseX >= panelX + 2 && mouseX <= panelX + itemAreaWidth
                    && mouseY >= iry && mouseY < iry + (int) ROW_HEIGHT;

            if (isRowHovered) {
               context.fill(panelX + 2, iry, panelX + itemAreaWidth, iry + (int) ROW_HEIGHT, 0x25FFFFFF);
               hoveredRecord = rec;
            }

            // Иконка предмета
            ItemStack stack = createDisplayStack(rec.itemId);
            if (!stack.isEmpty()) {
               context.item(stack, panelX + 4, iry + 3);
               if (mouseX >= panelX + 4 && mouseX <= panelX + 20 && mouseY >= iry + 3 && mouseY <= iry + 19) {
                  hoveredStack = stack;
               }
            }

            // Строка 1: Статус и название
            String status = rec.success ? "§a✓ " : "§c✗ ";
            String name = rec.label != null && !rec.label.isBlank() ? rec.label : (rec.itemId != null ? rec.itemId : "?");
            String trimmedTitle = trimVanillaText(tr, status + name, itemAreaWidth - 26);
            drawText(context, tr, trimmedTitle, panelX + 24, iry + 3, 0xFFFFFF);

            // Строка 2: Количество и цена
            String countText = "§7×" + rec.count;
            String priceText = "§6" + formatPrice(rec.totalPrice) + "$";
            drawText(context, tr, countText, panelX + 24, iry + 12, 0xAAAAAA);
            drawText(context, tr, priceText, panelX + itemAreaWidth - tr.width(priceText) - 2, iry + 12, 0xFFAA00);
         }

         context.disableScissor();

         // 4. Скроллбар дефолт майнкрафта
         if (hasScrollbar) {
            int scrollbarX = panelX + panelW - 6;
            int scrollbarW = 4;

            // Дорожка скроллбара
            context.fill(scrollbarX, contentTop, scrollbarX + scrollbarW, contentBottom, 0xFF000000);

            // Ползунок
            float thumbHeight = Math.max(14.0F, contentHeight * (contentHeight / totalContentHeight));
            float thumbY = contentTop + (historyScroll / maxHistoryScroll) * (contentHeight - thumbHeight);

            int ity = (int) thumbY;
            int ith = (int) thumbHeight;
            context.fill(scrollbarX, ity, scrollbarX + scrollbarW, ity + ith, 0xFF8B8B8B);
            context.fill(scrollbarX + 1, ity + 1, scrollbarX + scrollbarW - 1, ity + ith - 1, 0xFFC0C0C0);
         }
      }

      // 5. Всплывающий тултип над предметом или строкой истории
      if (hoveredRecord != null) {
         List<Component> tooltip = new ArrayList<>();
         String status = hoveredRecord.success ? "§aУспешно куплено" : "§cОшибка покупки";
         tooltip.add(Component.literal(status));
         String title = (hoveredRecord.label != null && !hoveredRecord.label.isBlank()) ? hoveredRecord.label : (hoveredRecord.itemId != null ? hoveredRecord.itemId : "?");
         tooltip.add(Component.literal("§f" + title + " §7(×" + hoveredRecord.count + ")"));
         if (hoveredRecord.seller != null && !hoveredRecord.seller.isBlank()) {
            tooltip.add(Component.literal("§7Продавец: §f" + hoveredRecord.seller));
         }
         tooltip.add(Component.literal("§7Цена: §6" + formatPrice(hoveredRecord.totalPrice) + "$"));
         if (!hoveredRecord.success && hoveredRecord.failReason != null) {
            tooltip.add(Component.literal("§cПричина: §7" + hoveredRecord.failReason));
         }
         context.setComponentTooltipForNextFrame(tr, tooltip, mouseX, mouseY);
      } else if (!hoveredStack.isEmpty()) {
         context.setTooltipForNextFrame(tr, hoveredStack, mouseX, mouseY);
      }
   }

   /**
    * Обработка скролла колесиком мыши (для истории покупок и ползунка скидки).
    */
   public static boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, int gx, int gy, int gw, int gh) {
      // 1. Скролл над ползунком скидки на левой панели (+-1%)
      int leftPanelX = (int) (gx - LEFT_PANEL_WIDTH - 6.0F);
      if (leftPanelX < 2) {
         leftPanelX = 2;
      }
      int leftPanelW = (int) LEFT_PANEL_WIDTH;
      int btnX = leftPanelX + 4;
      int btnW = leftPanelW - 8;
      int btnH = (int) BUTTON_HEIGHT;
      int sliderY = gy + 20 + (btnH + (int) BUTTON_GAP) * 5;

      if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= sliderY && mouseY <= sliderY + btnH) {
         int step = (int) Math.signum(verticalAmount);
         AutoBuyConfig cfg = AutoBuyConfig.cfg();
         int newPercent = Mth.clamp(cfg.sellDiscountPercent + step, 0, 50);
         if (newPercent != cfg.sellDiscountPercent) {
            cfg.sellDiscountPercent = newPercent;
            FlowAutoBuyModule.getInstance().sellDiscount.setValue((float) newPercent);
            AutoBuyConfig.save();
         }
         return true;
      }

      // 2. Скролл над панелью истории покупок
      int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
      int panelX = gx + gw + 4;
      if (panelX + (int) RIGHT_PANEL_WIDTH > screenWidth) {
         panelX = gx - (int) RIGHT_PANEL_WIDTH - 4;
      }
      if (panelX < 0) {
         return false;
      }

      if (mouseX >= panelX && mouseX <= panelX + RIGHT_PANEL_WIDTH && mouseY >= gy && mouseY <= gy + gh) {
         if (maxHistoryScroll > 0) {
            historyScroll = Mth.clamp((float) (historyScroll - verticalAmount * 22.0), 0.0F, maxHistoryScroll);
            return true;
         }
      }
      return false;
   }

   public static boolean mouseClicked(double mouseX, double mouseY, int button, int gx, int gy, int gw, int gh) {
      Minecraft mc = Minecraft.getInstance();
      AutoBuyConfig cfg = AutoBuyConfig.cfg();

      int leftPanelX = (int) (gx - LEFT_PANEL_WIDTH - 6.0F);
      if (leftPanelX < 2) {
         leftPanelX = 2;
      }
      int leftPanelW = (int) LEFT_PANEL_WIDTH;
      int leftPanelY = gy;

      int btnX = leftPanelX + 4;
      int btnW = leftPanelW - 8;
      int btnH = (int) BUTTON_HEIGHT;

      int btn0Y = leftPanelY + 20;
      int btn1Y = btn0Y + btnH + (int) BUTTON_GAP;
      int btn2Y = btn1Y + btnH + (int) BUTTON_GAP;
      int btn3Y = btn2Y + btnH + (int) BUTTON_GAP;
      int btn4Y = btn3Y + btnH + (int) BUTTON_GAP;
      int sliderDiscountY = btn4Y + btnH + (int) BUTTON_GAP;
      int sliderDurabilityY = sliderDiscountY + btnH + (int) BUTTON_GAP;

      // Клик по кнопке 0: AutoBuy ВКЛ / ВЫКЛ
      if (isHovered(mouseX, mouseY, btnX, btn0Y, btnW, btnH)) {
         cfg.enabled = !cfg.enabled;
         FlowAutoBuyModule.getInstance().setEnabled(cfg.enabled);
         AutoBuyConfig.save();
         return true;
      }

      // Клик по кнопке 1: AutoSell ВКЛ / ВЫКЛ
      if (isHovered(mouseX, mouseY, btnX, btn1Y, btnW, btnH)) {
         cfg.autoSell = !cfg.autoSell;
         FlowAutoBuyModule.getInstance().autoSell.setValue(cfg.autoSell);
         if (!cfg.autoSell) {
            AutoBuyEngine.INSTANCE.seller.stop();
         }
         AutoBuyConfig.save();
         return true;
      }

      // Клик по кнопке 2: SafeMode ВКЛ / ВЫКЛ
      if (isHovered(mouseX, mouseY, btnX, btn2Y, btnW, btnH)) {
         cfg.safeMode = !cfg.safeMode;
         FlowAutoBuyModule.getInstance().safeMode.setValue(cfg.safeMode);
         AutoBuyConfig.save();
         return true;
      }

      // Клик по кнопке 3: Парсинг цен
      if (isHovered(mouseX, mouseY, btnX, btn3Y, btnW, btnH)) {
         if (AutoBuyEngine.INSTANCE.parser.isActive()) {
            AutoBuyEngine.INSTANCE.parser.stop();
         } else {
            AutoBuyEngine.INSTANCE.parser.start();
         }
         return true;
      }

      // Клик по кнопке 4: Выбор предметов в стиле обычного майнкрафта
      if (isHovered(mouseX, mouseY, btnX, btn4Y, btnW, btnH)) {
         mc.gui.setScreen(new AutoBuyItemsScreen(mc.gui.screen()));
         return true;
      }

      // Клик по ползунку скидки перепродажи
      if (isHovered(mouseX, mouseY, btnX, sliderDiscountY, btnW, btnH)) {
         if (button == 0) {
            isDraggingSlider = true;
            updateSliderFromMouse(mouseX, btnX, btnW);
         } else if (button == 1) { // ПКМ - сброс на дефолтные 5%
            cfg.sellDiscountPercent = 5;
            FlowAutoBuyModule.getInstance().sellDiscount.setValue(5.0F);
            AutoBuyConfig.save();
         }
         return true;
      }

      // Клик по ползунку прочности брони
      if (isHovered(mouseX, mouseY, btnX, sliderDurabilityY, btnW, btnH)) {
         if (button == 0) {
            isDraggingDurabilitySlider = true;
            updateDurabilitySliderFromMouse(mouseX, btnX, btnW);
         } else if (button == 1) { // ПКМ - сброс на дефолтные 50%
            cfg.armorDurabilityPercent = 50;
            FlowAutoBuyModule.getInstance().armorDurability.setValue(50.0F);
            AutoBuyConfig.save();
         }
         return true;
      }

      return false;
   }

   private static ItemStack createDisplayStack(String itemId) {
      if (itemId != null && !itemId.isBlank()) {
         Identifier id = Identifier.tryParse(itemId);
         if (id == null) {
            return ItemStack.EMPTY;
         }

         Item item = BuiltInRegistries.ITEM.getValue(id);
         return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
      } else {
         return ItemStack.EMPTY;
      }
   }

   private static String formatPrice(long v) {
      if (v <= 0L) {
         return "0";
      }

      StringBuilder sb = new StringBuilder(Long.toString(v));
      for (int i = sb.length() - 3; i > 0; i -= 3) {
         sb.insert(i, ' ');
      }
      return sb.toString();
   }

   private static boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
      return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
   }

   private static void drawText(GuiGraphicsExtractor context, Font tr, String text, int x, int y, int color) {
      context.text(tr, text, x, y, (color >>> 24) == 0 ? color | 0xFF000000 : color, true);
   }

   private static String trimVanillaText(Font tr, String text, float maxWidth) {
      if (text == null || text.isEmpty() || maxWidth <= 0.0F) {
         return "";
      }
      if (tr.width(text) <= maxWidth) {
         return text;
      }
      for (int i = text.length() - 1; i > 0; i--) {
         String sub = text.substring(0, i) + "…";
         if (tr.width(sub) <= maxWidth) {
            return sub;
         }
      }
      return "";
   }
}
