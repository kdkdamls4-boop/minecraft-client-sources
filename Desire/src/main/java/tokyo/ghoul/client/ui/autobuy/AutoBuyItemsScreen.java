package tokyo.ghoul.client.ui.autobuy;

import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.ItemDatabase;
import tokyo.ghoul.client.autobuy.ItemEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import tokyo.ghoul.client.features.modules.misc.FlowAutoBuyModule;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Окно выбора и настройки предметов AutoBuy в классическом дефолтном стиле Minecraft.
 */
public class AutoBuyItemsScreen extends Screen {

    public enum FilterMode {
        ALL("Все"),
        ENABLED("Вкл"),
        DISABLED("Выкл");

        final String label;

        FilterMode(String label) {
            this.label = label;
        }
    }

    private final Screen parentScreen;
    private final Map<String, ItemStack> iconCache = new HashMap<>();

    private FilterMode filter = FilterMode.ALL;
    private String search = "";
    private boolean searchFocused = false;

    private float scroll = 0.0f;
    private float maxScroll = 0.0f;
    private boolean isDraggingScrollbar = false;
    private float scrollbarDragStartY = 0.0f;
    private float scrollbarStartScroll = 0.0f;

    private ItemEntry editingItem = null;
    private String priceBuffer = "";
    private int durabilityBuffer = 50;
    private boolean isDraggingModalDurability = false;

    private ItemStack hoveredTooltipStack = ItemStack.EMPTY;

    public AutoBuyItemsScreen(Screen parentScreen) {
        super(Component.literal("Выбор предметов"));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
        if (ItemDatabase.LIST.isEmpty()) {
            ItemDatabase.reload();
        }
        if (AutoBuyConfig.cfg().entries.isEmpty()) {
            AutoBuyConfig.cfg().reconcileWithDatabase();
            AutoBuyConfig.save();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        AutoBuyConfig.save();
        if (this.minecraft != null) {
            this.minecraft.gui.setScreen(this.parentScreen);
        }
    }

    private void playClickSound() {
        try {
            if (this.minecraft != null && this.minecraft.getSoundManager() != null) {
                this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
        } catch (Throwable ignored) {
        }
    }

    private List<ItemEntry> getFilteredEntries() {
        List<ItemEntry> all = AutoBuyConfig.cfg().entries;
        if (all == null) return List.of();

        List<ItemEntry> result = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);

        for (ItemEntry e : all) {
            if (e == null) continue;

            if (filter == FilterMode.ENABLED && !e.enabled) continue;
            if (filter == FilterMode.DISABLED && e.enabled) continue;

            if (!query.isEmpty()) {
                boolean matchName = e.displayName != null && e.displayName.toLowerCase(Locale.ROOT).contains(query);
                boolean matchId = e.itemId != null && e.itemId.toLowerCase(Locale.ROOT).contains(query);
                boolean matchQuery = e.searchQuery != null && e.searchQuery.toLowerCase(Locale.ROOT).contains(query);
                boolean matchLore = e.loreKeyword != null && e.loreKeyword.toLowerCase(Locale.ROOT).contains(query);
                if (!matchName && !matchId && !matchQuery && !matchLore) {
                    continue;
                }
            }

            result.add(e);
        }

        return result;
    }

    private int countEnabled() {
        int count = 0;
        for (ItemEntry e : AutoBuyConfig.cfg().entries) {
            if (e != null && e.enabled) count++;
        }
        return count;
    }

    private int countTotal() {
        return AutoBuyConfig.cfg().entries != null ? AutoBuyConfig.cfg().entries.size() : 0;
    }

    private ItemStack getStack(ItemEntry entry) {
        if (entry == null) return ItemStack.EMPTY;
        String key = (entry.itemId != null ? entry.itemId : "") + "#" + entry.matchAnySword;
        return iconCache.computeIfAbsent(key, k -> {
            if (entry.matchAnySword) {
                return new ItemStack(Items.NETHERITE_SWORD);
            }
            if (entry.itemId != null && !entry.itemId.isBlank()) {
                Identifier id = Identifier.tryParse(entry.itemId);
                if (id != null) {
                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    if (item != Items.AIR) {
                        return new ItemStack(item);
                    }
                }
            }
            return new ItemStack(Items.CHEST);
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        this.hoveredTooltipStack = ItemStack.EMPTY;

        // 1. Дефолтное затемнение фона Minecraft
        context.fill(0, 0, this.width, this.height, 0xA0000000);

        Font tr = this.font;

        // 2. Основное окно (по центру экрана в классическом стиле меню Minecraft)
        int panelW = Math.min(500, this.width - 24);
        int panelH = Math.min(320, this.height - 24);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        drawVanillaBox(context, panelX, panelY, panelW, panelH, false);

        // 3. Шапка окна
        int headerH = 24;
        drawText(context, tr, "§f§lВыбор предметов AutoBuy", panelX + 8, panelY + 7, 0xFFFFFF);
        int total = countTotal();
        int enabled = countEnabled();
        String countStr = "§7Включено: §a" + enabled + "§7 / §f" + total;
        drawText(context, tr, countStr, panelX + panelW - 8 - tr.width(countStr), panelY + 7, 0xFFFFFF);

        // Линия разделителя под шапкой
        context.fill(panelX + 2, panelY + headerH, panelX + panelW - 2, panelY + headerH + 1, 0xFF373737);

        // 4. Панель управления и фильтров (строка под шапкой)
        int controlsY = panelY + headerH + 4;
        int controlsH = 20;

        // Обработка перетаскивания ползунка прочности в модалке
        if (this.editingItem != null && this.isDraggingModalDurability) {
            long window = Minecraft.getInstance().getWindow().handle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
                this.isDraggingModalDurability = false;
            } else {
                int modalW = 286;
                int modalX = (this.width - modalW) / 2;
                int durSliderW = modalW - 16;
                int durSliderX = modalX + 8;
                updateModalDurabilityFromMouse(mouseX, durSliderX, durSliderW);
            }
        }

        // Поле поиска (sunken box)
        int searchW = 100;
        int searchX = panelX + 6;
        drawSunkenBox(context, searchX, controlsY, searchW, controlsH);

        String searchDisplay;
        if (this.search.isEmpty()) {
            searchDisplay = this.searchFocused ? "§f_" : "§8Поиск...";
        } else {
            boolean cursorVisible = this.searchFocused && ((System.currentTimeMillis() / 450L) % 2 == 0);
            searchDisplay = "§f" + this.search + (cursorVisible ? "_" : "");
        }
        drawText(context, tr, trimVanillaText(tr, searchDisplay, searchW - 8), searchX + 4, controlsY + 6, 0xFFFFFF);

        // Кнопки фильтра [Все] [Вкл] [Выкл] с индивидуальной шириной без наложений
        int btnFilterX = searchX + searchW + 4;
        int filterBtnH = controlsH;

        for (FilterMode m : FilterMode.values()) {
            int filterBtnW = tr.width(m.label) + 12;
            boolean isCur = this.filter == m;
            boolean hov = isHovered(mouseX, mouseY, btnFilterX, controlsY, filterBtnW, filterBtnH) && this.editingItem == null;
            String label = isCur ? ("§a" + m.label) : ("§7" + m.label);
            renderVanillaButton(context, tr, btnFilterX, controlsY, filterBtnW, filterBtnH, label, hov || isCur);
            btnFilterX += filterBtnW + 3;
        }

        // Кнопка глобальной настройки прочности брони в шапке
        String armorLabel = "§bБроня: ≥" + AutoBuyConfig.cfg().armorDurabilityPercent + "%";
        int armorBtnW = tr.width(armorLabel) + 10;
        boolean hovArmor = isHovered(mouseX, mouseY, btnFilterX, controlsY, armorBtnW, filterBtnH) && this.editingItem == null;
        renderVanillaButton(context, tr, btnFilterX, controlsY, armorBtnW, filterBtnH, armorLabel, hovArmor);

        // Кнопки массовых действий справа [Вкл все] [Выкл все]
        int bulkW = 54;
        int bulkH = controlsH;
        int btnDisAllX = panelX + panelW - 6 - bulkW;
        int btnEnAllX = btnDisAllX - bulkW - 4;

        boolean hovEnAll = isHovered(mouseX, mouseY, btnEnAllX, controlsY, bulkW, bulkH) && this.editingItem == null;
        renderVanillaButton(context, tr, btnEnAllX, controlsY, bulkW, bulkH, "§aВкл все", hovEnAll);

        boolean hovDisAll = isHovered(mouseX, mouseY, btnDisAllX, controlsY, bulkW, bulkH) && this.editingItem == null;
        renderVanillaButton(context, tr, btnDisAllX, controlsY, bulkW, bulkH, "§cВыкл все", hovDisAll);

        // Разделитель перед списком
        int listTopY = controlsY + controlsH + 4;
        context.fill(panelX + 2, listTopY, panelX + panelW - 2, listTopY + 1, 0xFF373737);

        // 5. Область списка предметов
        int listX = panelX + 6;
        int listY = listTopY + 2;
        int listW = panelW - 12;
        int bottomBarH = 26;
        int listH = panelH - (listY - panelY) - bottomBarH - 2;

        List<ItemEntry> entries = getFilteredEntries();
        int rowH = 26;
        int totalContentH = entries.size() * rowH;
        this.maxScroll = Math.max(0, totalContentH - listH);
        this.scroll = Mth.clamp(this.scroll, 0.0f, this.maxScroll);

        // Обработка перетаскивания скроллбара мышью
        int scrollbarW = 6;
        int scrollbarX = listX + listW - scrollbarW - 1;
        if (this.isDraggingScrollbar) {
            long window = Minecraft.getInstance().getWindow().handle();
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
                this.isDraggingScrollbar = false;
            } else if (this.maxScroll > 0) {
                float dragDelta = (float) mouseY - this.scrollbarDragStartY;
                float scrollTrackH = (float) listH;
                float thumbH = Math.max(16.0f, (float) listH * (float) listH / (float) totalContentH);
                float scrollableTrack = scrollTrackH - thumbH;
                if (scrollableTrack > 0) {
                    this.scroll = Mth.clamp(this.scrollbarStartScroll + (dragDelta / scrollableTrack) * this.maxScroll, 0.0f, this.maxScroll);
                }
            }
        }

        // Отрисовка списка с отсечением (scissor)
        context.enableScissor(listX, listY, listX + listW, listY + listH);

        int contentW = listW - (this.maxScroll > 0 ? (scrollbarW + 4) : 0);

        if (entries.isEmpty()) {
            String emptyMsg = this.search.isEmpty() ? "§7Список предметов пуст" : "§cНичего не найдено по запросу";
            int emptyW = tr.width(emptyMsg);
            drawText(context, tr, emptyMsg, listX + (contentW - emptyW) / 2, listY + (listH - 8) / 2, 0x888888);
        } else {
            for (int i = 0; i < entries.size(); i++) {
                int itemRowY = (int) (listY + i * rowH - this.scroll);
                if (itemRowY + rowH < listY || itemRowY > listY + listH) {
                    continue;
                }

                ItemEntry entry = entries.get(i);
                boolean rowHover = isHovered(mouseX, mouseY, listX, itemRowY, contentW, rowH - 2) && this.editingItem == null;

                // Фон полосы предмета
                if (rowHover) {
                    context.fill(listX, itemRowY, listX + contentW, itemRowY + rowH - 2, 0x30FFFFFF);
                } else if (i % 2 == 1) {
                    context.fill(listX, itemRowY, listX + contentW, itemRowY + rowH - 2, 0x15FFFFFF);
                }

                // Иконка предмета в слоте (sunken box 20x20)
                int slotX = listX + 2;
                int slotY = itemRowY + 2;
                int slotSize = 20;
                drawSunkenBox(context, slotX, slotY, slotSize, slotSize);

                ItemStack stack = getStack(entry);
                context.item(stack, slotX + 2, slotY + 2);

                if (rowHover && isHovered(mouseX, mouseY, slotX, slotY, slotSize, slotSize)) {
                    this.hoveredTooltipStack = stack;
                }

                // Кнопки справа в строке
                int btnToggleW = 38;
                int btnToggleH = 18;
                int btnToggleX = listX + contentW - btnToggleW - 2;
                int btnToggleY = itemRowY + 3;

                int btnEditW = 54;
                int btnEditH = 18;
                int btnEditX = btnToggleX - btnEditW - 3;
                int btnEditY = btnToggleY;

                boolean hovToggle = isHovered(mouseX, mouseY, btnToggleX, btnToggleY, btnToggleW, btnToggleH) && this.editingItem == null;
                boolean hovEdit = isHovered(mouseX, mouseY, btnEditX, btnEditY, btnEditW, btnEditH) && this.editingItem == null;

                // Кнопка переключения ВКЛ / ВЫКЛ
                String toggleText = entry.enabled ? "§aВКЛ" : "§cВЫКЛ";
                renderVanillaButton(context, tr, btnToggleX, btnToggleY, btnToggleW, btnToggleH, toggleText, hovToggle);

                // Кнопка настройки предмета (цена + прочность)
                renderVanillaButton(context, tr, btnEditX, btnEditY, btnEditW, btnEditH, "§eНастроить", hovEdit);

                // Текстовая информация о предмете
                int textX = slotX + slotSize + 6;
                int maxTextW = btnEditX - textX - 6;

                String name = entry.displayName != null && !entry.displayName.isBlank() ? entry.displayName : entry.itemId;
                String trimmedName = trimVanillaText(tr, "§f" + name, maxTextW);
                drawText(context, tr, trimmedName, textX, itemRowY + 3, entry.enabled ? 0xFFFFFF : 0x888888);

                String priceStr = "§6" + formatPrice(entry.maxUnitPrice) + " $";
                if (entry.marketPrice > 0) {
                    priceStr += " §8(рын: §7" + formatPrice(entry.marketPrice) + " §8$)";
                }
                int dur = entry.minDurabilityPercent > 0 ? entry.minDurabilityPercent : AutoBuyConfig.cfg().armorDurabilityPercent;
                if (entry.isArmor()) {
                    priceStr += " §8| §b🛡 ≥" + dur + "%";
                }
                String trimmedPrice = trimVanillaText(tr, priceStr, maxTextW);
                drawText(context, tr, trimmedPrice, textX, itemRowY + 13, 0xCCCCCC);
            }
        }

        context.disableScissor();

        // 6. Отрисовка полосы прокрутки (scrollbar)
        if (this.maxScroll > 0) {
            drawSunkenBox(context, scrollbarX, listY, scrollbarW, listH);
            float thumbH = Math.max(16.0f, (float) listH * (float) listH / (float) totalContentH);
            float thumbProgress = this.scroll / this.maxScroll;
            float thumbY = (float) listY + thumbProgress * ((float) listH - thumbH);
            boolean thumbHov = isHovered(mouseX, mouseY, scrollbarX, (int) thumbY, scrollbarW, (int) thumbH) || this.isDraggingScrollbar;
            drawVanillaBox(context, scrollbarX, (int) thumbY, scrollbarW, (int) thumbH, thumbHov);
        }

        // 7. Нижняя панель с кнопкой [ Готово ]
        int footerY = panelY + panelH - bottomBarH;
        context.fill(panelX + 2, footerY, panelX + panelW - 2, footerY + 1, 0xFF373737);

        int doneW = 100;
        int doneH = 18;
        int doneX = panelX + (panelW - doneW) / 2;
        int doneY = footerY + 4;
        boolean doneHov = isHovered(mouseX, mouseY, doneX, doneY, doneW, doneH) && this.editingItem == null;
        renderVanillaButton(context, tr, doneX, doneY, doneW, doneH, "Готово", doneHov);

        // 8. Модальное окно редактирования максимальной цены и прочности (если открыто)
        if (this.editingItem != null) {
            context.nextStratum();
            renderEditModal(context, tr, mouseX, mouseY);
        }

        // 9. Всплывающая подсказка над предметом (в самом конце, поверх всего)
        if (!this.hoveredTooltipStack.isEmpty() && this.editingItem == null) {
            context.setTooltipForNextFrame(tr, this.hoveredTooltipStack, mouseX, mouseY);
        }
    }

    /**
     * Отрисовка модального окна настройки предмета (цена + прочность брони) с 100% непрозрачным фоном.
     */
    private void renderEditModal(GuiGraphicsExtractor context, Font tr, int mouseX, int mouseY) {
        // Затемнение всего экрана позади модалки
        context.fill(0, 0, this.width, this.height, 0xC8000000);

        int modalW = 286;
        int modalH = 176;
        int modalX = (this.width - modalW) / 2;
        int modalY = (this.height - modalH) / 2;

        // ПОЛНОСТЬЮ НЕПРОЗРАЧНЫЙ ФОН модального окна (исключает просвечивание элементов позади него)
        context.fill(modalX, modalY, modalX + modalW, modalY + modalH, 0xFF141414);
        drawVanillaBox(context, modalX, modalY, modalW, modalH, false);

        // Шапка модалки
        drawText(context, tr, "§e§lНастройка предмета", modalX + 8, modalY + 6, 0xFFFFFF);
        String itemName = this.editingItem.displayName != null ? this.editingItem.displayName : this.editingItem.itemId;
        drawText(context, tr, trimVanillaText(tr, "§7" + itemName, modalW - 16), modalX + 8, modalY + 18, 0xAAAAAA);

        // Разделитель
        context.fill(modalX + 2, modalY + 29, modalX + modalW - 2, modalY + 30, 0xFF373737);

        // 1. Секция цены
        drawText(context, tr, "§fМакс. цена покупки:", modalX + 8, modalY + 33, 0xFFFFFF);

        int inputX = modalX + 8;
        int inputY = modalY + 44;
        int inputW = modalW - 16;
        int inputH = 18;
        drawSunkenBox(context, inputX, inputY, inputW, inputH);

        boolean cursorBlink = (System.currentTimeMillis() / 450L) % 2 == 0;
        String valText = this.priceBuffer.isEmpty() ? (cursorBlink ? "§f_" : "") : ("§6" + formatPrice(parsePrice(this.priceBuffer)) + " $" + (cursorBlink ? "§f_" : ""));
        drawText(context, tr, valText, inputX + 5, inputY + 5, 0xFFFFFF);

        // Кнопки пресетов цен [Рынок] [-5%] [-10%] [0]
        int presetY = inputY + inputH + 4;
        int presetH = 16;
        int presetW = 62;
        int p0X = modalX + 8;
        int p1X = p0X + presetW + 5;
        int p2X = p1X + presetW + 5;
        int p3X = p2X + presetW + 5;

        boolean hovP0 = isHovered(mouseX, mouseY, p0X, presetY, presetW, presetH);
        boolean hovP1 = isHovered(mouseX, mouseY, p1X, presetY, presetW, presetH);
        boolean hovP2 = isHovered(mouseX, mouseY, p2X, presetY, presetW, presetH);
        boolean hovP3 = isHovered(mouseX, mouseY, p3X, presetY, presetW, presetH);

        renderVanillaButton(context, tr, p0X, presetY, presetW, presetH, "Рынок", hovP0);
        renderVanillaButton(context, tr, p1X, presetY, presetW, presetH, "-5%", hovP1);
        renderVanillaButton(context, tr, p2X, presetY, presetW, presetH, "-10%", hovP2);
        renderVanillaButton(context, tr, p3X, presetY, presetW, presetH, "0", hovP3);

        // 2. Слайдер прочности брони в стиле Minecraft
        int durSliderY = presetY + presetH + 8;
        int durSliderW = modalW - 16;
        int durSliderH = 18;
        int durSliderX = modalX + 8;
        boolean hovDurSlider = isHovered(mouseX, mouseY, durSliderX, durSliderY, durSliderW, durSliderH);
        renderPercentSlider(context, tr, durSliderX, durSliderY, durSliderW, durSliderH,
                "Броня: ≥" + this.durabilityBuffer + "%", this.durabilityBuffer, 100, hovDurSlider, this.isDraggingModalDurability);

        // Разделитель перед кнопками действий
        int divY = durSliderY + durSliderH + 8;
        context.fill(modalX + 2, divY, modalX + modalW - 2, divY + 1, 0xFF373737);

        // Нижние кнопки [Сохранить] [Отмена]
        int btnW = 82;
        int btnH = 18;
        int saveX = modalX + modalW / 2 - btnW - 6;
        int cancelX = modalX + modalW / 2 + 6;
        int actionY = divY + 5;

        boolean saveHov = isHovered(mouseX, mouseY, saveX, actionY, btnW, btnH);
        boolean cancelHov = isHovered(mouseX, mouseY, cancelX, actionY, btnW, btnH);

        renderVanillaButton(context, tr, saveX, actionY, btnW, btnH, "§aСохранить", saveHov);
        renderVanillaButton(context, tr, cancelX, actionY, btnW, btnH, "§cОтмена", cancelHov);
    }

    private long parsePrice(String buf) {
        if (buf == null || buf.isBlank()) return 0L;
        try {
            return Long.parseLong(buf.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button != 0 && button != 1) {
            return super.mouseClicked(click, doubled);
        }

        // Если открыто модальное окно настройки предмета
        if (this.editingItem != null) {
            int modalW = 286;
            int modalH = 176;
            int modalX = (this.width - modalW) / 2;
            int modalY = (this.height - modalH) / 2;

            // Кнопки пресетов цен
            int inputY = modalY + 44;
            int inputH = 18;
            int presetY = inputY + inputH + 4;
            int presetH = 16;
            int presetW = 62;
            int p0X = modalX + 8;
            int p1X = p0X + presetW + 5;
            int p2X = p1X + presetW + 5;
            int p3X = p2X + presetW + 5;

            if (isHovered(mouseX, mouseY, p0X, presetY, presetW, presetH)) {
                this.priceBuffer = Long.toString(this.editingItem.marketPrice > 0 ? this.editingItem.marketPrice : 0L);
                playClickSound();
                return true;
            }
            if (isHovered(mouseX, mouseY, p1X, presetY, presetW, presetH)) {
                long p = (long) (this.editingItem.marketPrice * 0.95);
                this.priceBuffer = Long.toString(Math.max(0L, p));
                playClickSound();
                return true;
            }
            if (isHovered(mouseX, mouseY, p2X, presetY, presetW, presetH)) {
                long p = (long) (this.editingItem.marketPrice * 0.90);
                this.priceBuffer = Long.toString(Math.max(0L, p));
                playClickSound();
                return true;
            }
            if (isHovered(mouseX, mouseY, p3X, presetY, presetW, presetH)) {
                this.priceBuffer = "0";
                playClickSound();
                return true;
            }

            // Слайдер прочности брони
            int durSliderY = presetY + presetH + 8;
            int durSliderW = modalW - 16;
            int durSliderH = 18;
            int durSliderX = modalX + 8;

            if (isHovered(mouseX, mouseY, durSliderX, durSliderY, durSliderW, durSliderH)) {
                if (button == 0) {
                    this.isDraggingModalDurability = true;
                    updateModalDurabilityFromMouse(mouseX, durSliderX, durSliderW);
                    playClickSound();
                } else if (button == 1) { // ПКМ - сброс на 50%
                    this.durabilityBuffer = 50;
                    playClickSound();
                }
                return true;
            }

            // Кнопки [Сохранить] и [Отмена]
            int divY = durSliderY + durSliderH + 8;
            int btnW = 82;
            int btnH = 18;
            int saveX = modalX + modalW / 2 - btnW - 6;
            int cancelX = modalX + modalW / 2 + 6;
            int actionY = divY + 5;

            if (isHovered(mouseX, mouseY, saveX, actionY, btnW, btnH)) {
                this.editingItem.maxUnitPrice = parsePrice(this.priceBuffer);
                this.editingItem.minDurabilityPercent = this.durabilityBuffer;
                AutoBuyConfig.save();
                this.editingItem = null;
                this.isDraggingModalDurability = false;
                playClickSound();
                return true;
            }

            if (isHovered(mouseX, mouseY, cancelX, actionY, btnW, btnH)) {
                this.editingItem = null;
                this.isDraggingModalDurability = false;
                playClickSound();
                return true;
            }

            // Клик вне модалки закрывает её
            if (!isHovered(mouseX, mouseY, modalX, modalY, modalW, modalH)) {
                this.editingItem = null;
                this.isDraggingModalDurability = false;
                return true;
            }
            return true;
        }

        int panelW = Math.min(500, this.width - 24);
        int panelH = Math.min(320, this.height - 24);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;

        int headerH = 24;
        int controlsY = panelY + headerH + 4;
        int controlsH = 20;

        // Поле поиска
        int searchW = 100;
        int searchX = panelX + 6;
        if (isHovered(mouseX, mouseY, searchX, controlsY, searchW, controlsH)) {
            this.searchFocused = true;
            if (button == 1) { // ПКМ - очистка
                this.search = "";
            }
            playClickSound();
            return true;
        } else {
            this.searchFocused = false;
        }

        // Фильтры [Все] [Вкл] [Выкл]
        int btnFilterX = searchX + searchW + 4;
        int filterBtnH = controlsH;
        for (FilterMode m : FilterMode.values()) {
            int filterBtnW = this.font.width(m.label) + 12;
            if (isHovered(mouseX, mouseY, btnFilterX, controlsY, filterBtnW, filterBtnH)) {
                this.filter = m;
                this.scroll = 0.0f;
                playClickSound();
                return true;
            }
            btnFilterX += filterBtnW + 3;
        }

        // Кнопка настройки общей прочности брони
        String armorLabel = "§bБроня: ≥" + AutoBuyConfig.cfg().armorDurabilityPercent + "%";
        int armorBtnW = this.font.width(armorLabel) + 10;
        if (isHovered(mouseX, mouseY, btnFilterX, controlsY, armorBtnW, filterBtnH)) {
            if (button == 0) { // ЛКМ — циклическое переключение
                int[] cycle = {0, 25, 50, 70, 80, 90, 100};
                int cur = AutoBuyConfig.cfg().armorDurabilityPercent;
                int next = 50;
                for (int i = 0; i < cycle.length; i++) {
                    if (cycle[i] == cur) {
                        next = cycle[(i + 1) % cycle.length];
                        break;
                    }
                }
                AutoBuyConfig.cfg().armorDurabilityPercent = next;
                FlowAutoBuyModule.getInstance().armorDurability.setValue((float) next);
                AutoBuyConfig.save();
            } else if (button == 1) { // ПКМ — сброс на 50%
                AutoBuyConfig.cfg().armorDurabilityPercent = 50;
                FlowAutoBuyModule.getInstance().armorDurability.setValue(50.0F);
                AutoBuyConfig.save();
            }
            playClickSound();
            return true;
        }

        // Кнопка [Вкл все]
        int bulkW = 54;
        int bulkH = controlsH;
        int btnDisAllX = panelX + panelW - 6 - bulkW;
        int btnEnAllX = btnDisAllX - bulkW - 4;

        if (isHovered(mouseX, mouseY, btnEnAllX, controlsY, bulkW, bulkH)) {
            for (ItemEntry e : getFilteredEntries()) {
                if (e != null) e.enabled = true;
            }
            AutoBuyConfig.save();
            playClickSound();
            return true;
        }

        // Кнопка [Выкл все]
        if (isHovered(mouseX, mouseY, btnDisAllX, controlsY, bulkW, bulkH)) {
            for (ItemEntry e : getFilteredEntries()) {
                if (e != null) e.enabled = false;
            }
            AutoBuyConfig.save();
            playClickSound();
            return true;
        }

        // Клик по кнопке [Готово]
        int bottomBarH = 26;
        int footerY = panelY + panelH - bottomBarH;
        int doneW = 100;
        int doneH = 18;
        int doneX = panelX + (panelW - doneW) / 2;
        int doneY = footerY + 4;
        if (isHovered(mouseX, mouseY, doneX, doneY, doneW, doneH)) {
            playClickSound();
            onClose();
            return true;
        }

        // Область списка
        int listTopY = controlsY + controlsH + 4;
        int listX = panelX + 6;
        int listY = listTopY + 2;
        int listW = panelW - 12;
        int listH = panelH - (listY - panelY) - bottomBarH - 2;

        // Клик по скроллбару
        int scrollbarW = 6;
        int scrollbarX = listX + listW - scrollbarW - 1;
        if (this.maxScroll > 0 && isHovered(mouseX, mouseY, scrollbarX, listY, scrollbarW, listH)) {
            this.isDraggingScrollbar = true;
            this.scrollbarDragStartY = (float) mouseY;
            this.scrollbarStartScroll = this.scroll;
            return true;
        }

        // Клик по элементам списка
        List<ItemEntry> entries = getFilteredEntries();
        int contentW = listW - (this.maxScroll > 0 ? (scrollbarW + 4) : 0);
        int rowH = 26;

        if (isHovered(mouseX, mouseY, listX, listY, contentW, listH)) {
            for (int i = 0; i < entries.size(); i++) {
                int itemRowY = (int) (listY + i * rowH - this.scroll);
                if (itemRowY + rowH < listY || itemRowY > listY + listH) {
                    continue;
                }

                ItemEntry entry = entries.get(i);

                int btnToggleW = 38;
                int btnToggleH = 18;
                int btnToggleX = listX + contentW - btnToggleW - 2;
                int btnToggleY = itemRowY + 3;

                int btnEditW = 54;
                int btnEditH = 18;
                int btnEditX = btnToggleX - btnEditW - 3;
                int btnEditY = btnToggleY;

                // Клик по переключателю ВКЛ / ВЫКЛ
                if (isHovered(mouseX, mouseY, btnToggleX, btnToggleY, btnToggleW, btnToggleH)) {
                    entry.enabled = !entry.enabled;
                    AutoBuyConfig.save();
                    playClickSound();
                    return true;
                }

                // Клик по кнопке настройки предмета
                if (isHovered(mouseX, mouseY, btnEditX, btnEditY, btnEditW, btnEditH)) {
                    this.editingItem = entry;
                    this.priceBuffer = Long.toString(entry.maxUnitPrice);
                    this.durabilityBuffer = entry.minDurabilityPercent > 0 ? entry.minDurabilityPercent : AutoBuyConfig.cfg().armorDurabilityPercent;
                    playClickSound();
                    return true;
                }

                // Клик по самой строке предмета
                if (isHovered(mouseX, mouseY, listX, itemRowY, contentW, rowH - 2)) {
                    if (button == 0) {
                        entry.enabled = !entry.enabled;
                        AutoBuyConfig.save();
                        playClickSound();
                        return true;
                    } else if (button == 1) { // ПКМ по строке - настройка
                        this.editingItem = entry;
                        this.priceBuffer = Long.toString(entry.maxUnitPrice);
                        this.durabilityBuffer = entry.minDurabilityPercent > 0 ? entry.minDurabilityPercent : AutoBuyConfig.cfg().armorDurabilityPercent;
                        playClickSound();
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.editingItem == null && this.maxScroll > 0) {
            this.scroll -= (float) (verticalAmount * 28.0);
            this.scroll = Mth.clamp(this.scroll, 0.0f, this.maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (click.button() == 0) {
            this.isDraggingScrollbar = false;
            this.isDraggingModalDurability = false;
        }
        return super.mouseReleased(click);
    }

    private void updateModalDurabilityFromMouse(double mouseX, int x, int w) {
        int thumbW = 8;
        float norm = (float) (mouseX - (x + 1 + thumbW / 2.0F)) / (float) (w - 2 - thumbW);
        norm = Mth.clamp(norm, 0.0F, 1.0F);
        this.durabilityBuffer = Math.round(norm * 100.0F);
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

    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = (char) input.codepoint();
        if (this.editingItem != null) {
            if (Character.isDigit(chr)) {
                if (this.priceBuffer.length() < 12) {
                    this.priceBuffer += chr;
                }
                return true;
            }
            return false;
        }

        if (this.searchFocused) {
            if (chr >= 32 && chr != 127) {
                this.search += chr;
                this.scroll = 0.0f;
                return true;
            }
        }

        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (this.editingItem != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.editingItem = null;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                this.editingItem.maxUnitPrice = parsePrice(this.priceBuffer);
                this.editingItem.minDurabilityPercent = this.durabilityBuffer;
                AutoBuyConfig.save();
                this.editingItem = null;
                playClickSound();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.priceBuffer.isEmpty()) {
                    this.priceBuffer = this.priceBuffer.substring(0, this.priceBuffer.length() - 1);
                }
                return true;
            }
            return true;
        }

        if (this.searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!this.search.isEmpty()) {
                    this.search = this.search.substring(0, this.search.length() - 1);
                    this.scroll = 0.0f;
                }
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                this.searchFocused = false;
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }

        return super.keyPressed(input);
    }

    /**
     * Отрисовка кнопки в классическом дефолтном стиле Minecraft.
     */
    private static void renderVanillaButton(GuiGraphicsExtractor context, Font tr, int x, int y, int w, int h, String text, boolean hover) {
        drawVanillaBox(context, x, y, w, h, hover);
        int textW = tr.width(text);
        int tx = x + (w - textW) / 2;
        int ty = y + (h - 8) / 2;
        drawText(context, tr, text, tx, ty, hover ? 0xFFFFA0 : 0xFFFFFF);
    }

    /**
     * Классическая дефолтная выпуклая рамка Minecraft (темный полупрозрачный фон и bevel).
     */
    private static void drawVanillaBox(GuiGraphicsExtractor context, int x, int y, int w, int h, boolean hover) {
        context.fill(x, y, x + w, y + h, hover ? 0xE0252525 : 0xD0101010);

        // Внешняя черная рамка в 1px
        context.fill(x, y, x + w, y + 1, 0xFF000000);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
        context.fill(x, y + 1, x + 1, y + h, 0xFF000000);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF000000);

        // Внутренний скос
        int topLight = hover ? 0xFFFFFFFF : 0xFF8B8B8B;
        int botDark = hover ? 0xFFA0A0A0 : 0xFF373737;

        context.fill(x + 1, y + 1, x + w - 1, y + 2, topLight);
        context.fill(x + 1, y + 1, x + 2, y + h - 1, topLight);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, botDark);
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, botDark);
    }

    /**
     * Вдавленная классическая рамка Minecraft (sunken slot / field).
     */
    private static void drawSunkenBox(GuiGraphicsExtractor context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0xD0101010);

        // Внешняя черная рамка
        context.fill(x, y, x + w, y + 1, 0xFF000000);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF000000);
        context.fill(x, y + 1, x + 1, y + h, 0xFF000000);
        context.fill(x + w - 1, y, x + w, y + h, 0xFF000000);

        // Затемнение сверху-слева, свет снизу-справа (вдавленность)
        context.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF373737);
        context.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF373737);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        context.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF555555);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    private static boolean isHovered(double mouseX, double mouseY, double x, double y, double w, double h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static void drawText(GuiGraphicsExtractor context, Font tr, String text, int x, int y, int color) {
        context.text(tr, text, x, y, (color >>> 24) == 0 ? color | 0xFF000000 : color, true);
    }

    private static String formatPrice(long v) {
        if (v <= 0L) return "0";
        StringBuilder sb = new StringBuilder(Long.toString(v));
        for (int i = sb.length() - 3; i > 0; i -= 3) {
            sb.insert(i, ' ');
        }
        return sb.toString();
    }

    private static String trimVanillaText(Font tr, String text, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0.0F) return "";
        if (tr.width(text) <= maxWidth) return text;
        for (int i = text.length() - 1; i > 0; i--) {
            String sub = text.substring(0, i) + "…";
            if (tr.width(sub) <= maxWidth) return sub;
        }
        return "";
    }
}
