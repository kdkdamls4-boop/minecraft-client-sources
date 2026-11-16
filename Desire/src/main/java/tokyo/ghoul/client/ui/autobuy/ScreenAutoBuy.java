package tokyo.ghoul.client.ui.autobuy;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.features.modules.autobuy.engine.AutoBuyParsedItemData;
import tokyo.ghoul.client.features.modules.autobuy.engine.AutoBuyParsedItems;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.clickgui.ClickGuiLayout;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScreenAutoBuy extends Screen implements QuickImports {
    @Getter private static final ScreenAutoBuy instance = new ScreenAutoBuy();

    private static final String[] CATEGORIES = {
            "Все", "Броня", "Инструменты", "Сферы", "Взрывчатка", "Рюкзаки", "Опыт", "Разное"
    };

    private static final int DURABILITY_STEP = 5;
    private static final int MAX_PRICE_CHARS = 12;

    private int selectedCategory = 0;
    private final StringBuilder searchQuery = new StringBuilder();
    private boolean searchFocused = false;

    private List<AutoBuyParsedItemData> allItems = new ArrayList<>();
    private float scroll = 0f;
    private float maxScroll = 0f;
    private final AnimationUtil scrollAnim = new AnimationUtil();

    private AutoBuyParsedItemData editingItem = null;
    private final StringBuilder priceInput = new StringBuilder();

    protected ScreenAutoBuy() {
        super(Component.literal("AutoBuy Catalog"));
    }

    @Override
    protected void init() {
        super.init();
        refresh();
        scroll = 0f;
        scrollAnim.setValue(0);
        editingItem = null;
        searchFocused = false;
    }

    public void refresh() {
        allItems = AutoBuyParsedItems.loadItems();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        commitEdit();
        searchFocused = false;
        super.onClose();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RenderService.setGuiContext(true);
        try {
            super.extractRenderState(context, mouseX, mouseY, delta);
            renderCatalog(context, mouseX, mouseY);
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    private void renderCatalog(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        PoseStack matrices = RenderUtil.matrices();

        float winW = Math.min(width - 60f, 720f);
        float winH = Math.min(height - 50f, 480f);
        float winX = (width - winW) / 2f;
        float winY = (height - winH) / 2f;

        RenderUtil.BLUR_RECT.draw(matrices, winX, winY, winW, winH, 8f, new Color(10, 12, 20, 210));
        RenderUtil.RECT.draw(matrices, winX, winY, winW, winH, 8f, new Color(16, 20, 30, 235));
        RenderUtil.RECT.drawOutline(matrices, winX, winY, winW, winH, 8f, 1.2f, new Color(154, 175, 255, 30));

        float titleX = winX + 20f;
        float titleY = winY + 16f;
        Fonts.PS_BOLD.drawGradientText(matrices, "КАТАЛОГ ПРЕДМЕТОВ AUTOBUY", titleX, titleY, 13f,
                UIColors.primary(), UIColors.secondary(), 10f);

        long enabledCount = allItems.stream().filter(AutoBuyParsedItemData::enabled).count();
        String counter = "Активно: " + enabledCount + " / " + allItems.size();
        Fonts.PS_REGULAR.drawText(matrices, counter, titleX + 250f, titleY + 2f, 9.5f,
                UIColors.inactiveTextColor(200));

        float searchW = 160f;
        float searchH = 18f;
        float searchX = winX + winW - 20f - searchW;
        float searchY = winY + 14f;
        boolean searchHov = MouseUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH);

        Color searchBg = searchFocused
                ? ColorUtil.setAlpha(UIColors.primary(), 45)
                : new Color(25, 30, 45, searchHov ? 200 : 160);
        RenderUtil.RECT.draw(matrices, searchX, searchY, searchW, searchH, 4f, searchBg);
        RenderUtil.RECT.drawOutline(matrices, searchX, searchY, searchW, searchH, 4f, 1f,
                searchFocused ? UIColors.primary() : new Color(154, 175, 255, 35));

        String searchDisplay = searchQuery.isEmpty() && !searchFocused ? "Поиск..." : searchQuery.toString();
        Color searchTextColor = searchQuery.isEmpty() && !searchFocused ? UIColors.inactiveTextColor(160) : Color.WHITE;
        Fonts.PS_REGULAR.drawText(matrices, searchDisplay, searchX + 7f, searchY + 4f, 8.5f, searchTextColor);

        if (searchFocused) {
            float textW = Fonts.PS_REGULAR.getWidth(searchQuery.toString(), 8.5f);
            float curAlpha = (float) ((Math.sin(System.currentTimeMillis() / 250.0) + 1.0) / 2.0);
            RenderUtil.RECT.draw(matrices, searchX + 7f + textW + 1f, searchY + 3f, 1f, searchH - 6f, 0.5f,
                    ColorUtil.setAlpha(Color.WHITE, (int) (curAlpha * 255)));
        }

        float tabsX = winX + 20f;
        float tabsY = winY + 42f;
        float tabH = 16f;
        float currentTabX = tabsX;

        for (int i = 0; i < CATEGORIES.length; i++) {
            String catName = CATEGORIES[i];
            float tabTextW = Fonts.PS_MEDIUM.getWidth(catName, 8.5f);
            float tabW = tabTextW + 14f;
            boolean tabActive = selectedCategory == i;
            boolean tabHov = MouseUtil.isHovered(mouseX, mouseY, currentTabX, tabsY, tabW, tabH);

            if (tabActive) {
                RenderUtil.RECT.draw(matrices, currentTabX, tabsY, tabW, tabH, 4f,
                        ColorUtil.setAlpha(UIColors.primary(), 70));
                RenderUtil.RECT.drawOutline(matrices, currentTabX, tabsY, tabW, tabH, 4f, 1f,
                        UIColors.primary());
            } else if (tabHov) {
                RenderUtil.RECT.draw(matrices, currentTabX, tabsY, tabW, tabH, 4f,
                        new Color(255, 255, 255, 18));
            }

            Color tabColor = tabActive ? Color.WHITE : UIColors.inactiveTextColor(tabHov ? 230 : 160);
            Fonts.PS_MEDIUM.drawCenteredText(matrices, catName, currentTabX + tabW / 2f, tabsY + 3.5f, 8.5f, tabColor);
            currentTabX += tabW + 6f;
        }

        float divY = winY + 68f;
        RenderUtil.RECT.draw(matrices, winX + 16f, divY, winW - 32f, 1f, 0.5f, new Color(255, 255, 255, 20));

        float colY = divY + 6f;
        Fonts.PS_REGULAR.drawText(matrices, "Предмет", winX + 50f, colY, 8f, UIColors.inactiveTextColor(140));
        Fonts.PS_REGULAR.drawText(matrices, "Прочность", winX + winW - 275f, colY, 8f, UIColors.inactiveTextColor(140));
        Fonts.PS_REGULAR.drawText(matrices, "Макс. цена", winX + winW - 170f, colY, 8f, UIColors.inactiveTextColor(140));
        Fonts.PS_REGULAR.drawText(matrices, "Вкл", winX + winW - 65f, colY, 8f, UIColors.inactiveTextColor(140));

        List<AutoBuyParsedItemData> visibleItems = getFilteredItems();

        float listX = winX + 16f;
        float listY = divY + 20f;
        float listW = winW - 32f;
        float listH = winH - (listY - winY) - 16f;

        scrollAnim.update();
        scrollAnim.run(scroll, 220, Easing.EXPO_OUT);
        float offset = (float) scrollAnim.getValue();

        float rowH = 24f;
        float rowGap = 4f;

        ScissorUtil.start(matrices, listX, listY, listW, listH);
        context.enableScissor((int) Math.floor(listX), (int) Math.floor(listY),
                (int) Math.ceil(listX + listW), (int) Math.ceil(listY + listH));
        try {
            float rowY = listY + offset;
            for (AutoBuyParsedItemData item : visibleItems) {
                if (rowY + rowH >= listY && rowY <= listY + listH) {
                    renderItemRow(context, matrices, item, listX, rowY, listW, rowH, mouseX, mouseY);
                }
                rowY += rowH + rowGap;
            }
        } finally {
            context.disableScissor();
            ScissorUtil.stop(matrices);
        }

        float totalContentH = visibleItems.size() * (rowH + rowGap);
        maxScroll = Math.min(listH - totalContentH, 0f);
        scroll = Mth.clamp(scroll, maxScroll, 0f);

        if (visibleItems.isEmpty()) {
            Fonts.PS_MEDIUM.drawCenteredText(matrices, "Предметы не найдены", winX + winW / 2f,
                    listY + listH / 2f - 8f, 11f, UIColors.inactiveTextColor(160));
        }
    }

    private void renderItemRow(GuiGraphicsExtractor context, PoseStack matrices, AutoBuyParsedItemData item,
                              float x, float y, float w, float h, int mouseX, int mouseY) {
        boolean hov = MouseUtil.isHovered(mouseX, mouseY, x, y, w, h);

        Color rowBg = item.enabled()
                ? new Color(20, 26, 40, hov ? 180 : 130)
                : new Color(14, 16, 25, hov ? 140 : 90);
        RenderUtil.RECT.draw(matrices, x, y, w, h, 4f, rowBg);
        if (hov) {
            RenderUtil.RECT.drawOutline(matrices, x, y, w, h, 4f, 0.8f, new Color(154, 175, 255, 30));
        }

        ItemStack icon = item.iconStack();
        if (icon != null && !icon.isEmpty()) {
            context.item(icon, Math.round(x + 6f), Math.round(y + (h - 16f) / 2f));
        }

        Color nameColor = item.enabled() ? Color.WHITE : UIColors.inactiveTextColor(180);
        String name = item.resolvedDisplayName();
        Fonts.PS_MEDIUM.drawText(matrices, name, x + 30f, y + (h - 9f) / 2f, 9f, nameColor);

        float durX = x + w - 265f;
        float durY = y + (h - 14f) / 2f;
        if (item.damageable()) {
            boolean minusHov = MouseUtil.isHovered(mouseX, mouseY, durX, durY, 14f, 14f);
            RenderUtil.RECT.draw(matrices, durX, durY, 14f, 14f, 3f, new Color(255, 255, 255, minusHov ? 35 : 15));
            Fonts.PS_MEDIUM.drawCenteredText(matrices, "-", durX + 7f, durY + 2f, 9f, Color.WHITE);

            Fonts.PS_REGULAR.drawCenteredText(matrices, item.minDurability() + "%", durX + 32f, durY + 2.5f, 8.5f,
                    UIColors.textColor());

            float plusX = durX + 50f;
            boolean plusHov = MouseUtil.isHovered(mouseX, mouseY, plusX, durY, 14f, 14f);
            RenderUtil.RECT.draw(matrices, plusX, durY, 14f, 14f, 3f, new Color(255, 255, 255, plusHov ? 35 : 15));
            Fonts.PS_MEDIUM.drawCenteredText(matrices, "+", plusX + 7f, durY + 2f, 9f, Color.WHITE);
        } else {
            Fonts.PS_REGULAR.drawCenteredText(matrices, "—", durX + 32f, durY + 2.5f, 8.5f,
                    UIColors.inactiveTextColor(120));
        }

        float priceX = x + w - 165f;
        float priceY = y + (h - 16f) / 2f;
        float priceW = 80f;
        float priceH = 16f;
        boolean editingThis = editingItem == item;
        boolean priceHov = MouseUtil.isHovered(mouseX, mouseY, priceX, priceY, priceW, priceH);

        Color priceBg = editingThis
                ? ColorUtil.setAlpha(UIColors.primary(), 45)
                : new Color(25, 30, 45, priceHov ? 190 : 130);
        RenderUtil.RECT.draw(matrices, priceX, priceY, priceW, priceH, 3.5f, priceBg);
        RenderUtil.RECT.drawOutline(matrices, priceX, priceY, priceW, priceH, 3.5f, 0.8f,
                editingThis ? UIColors.primary() : new Color(154, 175, 255, 30));

        String priceVal = editingThis ? priceInput.toString() : item.price();
        if (priceVal.isBlank() && !editingThis) {
            priceVal = "Авто";
        }
        Fonts.PS_MEDIUM.drawText(matrices, priceVal, priceX + 6f, priceY + 3.5f, 8.5f,
                editingThis ? Color.WHITE : UIColors.textColor());

        if (editingThis) {
            float textW = Fonts.PS_MEDIUM.getWidth(priceVal, 8.5f);
            float curAlpha = (float) ((Math.sin(System.currentTimeMillis() / 250.0) + 1.0) / 2.0);
            RenderUtil.RECT.draw(matrices, priceX + 6f + textW + 1f, priceY + 2.5f, 1f, priceH - 5f, 0.5f,
                    ColorUtil.setAlpha(Color.WHITE, (int) (curAlpha * 255)));
        }

        float toggleW = 28f;
        float toggleH = 14f;
        float toggleX = x + w - 50f;
        float toggleY = y + (h - toggleH) / 2f;

        Color trackColor = item.enabled()
                ? UIColors.primary()
                : new Color(55, 65, 85, 180);
        RenderUtil.RECT.draw(matrices, toggleX, toggleY, toggleW, toggleH, toggleH / 2f, trackColor);

        float knobSize = toggleH - 3f;
        float knobX = item.enabled()
                ? toggleX + toggleW - knobSize - 1.5f
                : toggleX + 1.5f;
        RenderUtil.RECT.draw(matrices, knobX, toggleY + 1.5f, knobSize, knobSize, knobSize / 2f, Color.WHITE);
    }

    private List<AutoBuyParsedItemData> getFilteredItems() {
        String query = searchQuery.toString().toLowerCase(Locale.ROOT).trim();
        List<AutoBuyParsedItemData> result = new ArrayList<>();

        for (AutoBuyParsedItemData item : allItems) {
            if (selectedCategory > 0 && !matchesCategory(item, selectedCategory)) {
                continue;
            }
            if (!query.isEmpty()) {
                String name = item.resolvedDisplayName().toLowerCase(Locale.ROOT);
                String id = item.itemId() != null ? item.itemId().toLowerCase(Locale.ROOT) : "";
                if (!name.contains(query) && !id.contains(query)) {
                    continue;
                }
            }
            result.add(item);
        }
        return result;
    }

    private boolean matchesCategory(AutoBuyParsedItemData item, int catIndex) {
        String name = item.resolvedDisplayName().toLowerCase(Locale.ROOT);
        String id = item.itemId() != null ? item.itemId().toLowerCase(Locale.ROOT) : "";

        return switch (catIndex) {
            case 1 -> item.damageable() && (id.contains("helmet") || id.contains("chestplate")
                    || id.contains("leggings") || id.contains("boots") || id.contains("elytra")
                    || (item.iconStack() != null && item.iconStack().has(DataComponents.EQUIPPABLE)));
            case 2 -> id.contains("sword") || id.contains("pickaxe") || id.contains("axe")
                    || id.contains("shovel") || id.contains("hoe") || id.contains("bow")
                    || id.contains("crossbow") || id.contains("mace") || id.contains("trident");
            case 3 -> name.contains("сфера") || name.contains("шар") || name.contains("orb");
            case 4 -> name.contains("взрыв") || name.contains("динамит") || id.contains("tnt")
                    || name.contains("мина") || name.contains("бомб");
            case 5 -> name.contains("рюкзак") || name.contains("сумка") || name.contains("backpack")
                    || id.contains("shulker");
            case 6 -> name.contains("опыт") || name.contains("пузырек") || id.contains("experience_bottle");
            default -> true;
        };
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();

        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled);
        }

        float winW = Math.min(width - 60f, 720f);
        float winH = Math.min(height - 50f, 480f);
        float winX = (width - winW) / 2f;
        float winY = (height - winH) / 2f;

        float searchW = 160f;
        float searchH = 18f;
        float searchX = winX + winW - 20f - searchW;
        float searchY = winY + 14f;
        if (MouseUtil.isHovered(mouseX, mouseY, searchX, searchY, searchW, searchH)) {
            commitEdit();
            searchFocused = true;
            return true;
        } else {
            searchFocused = false;
        }

        float tabsX = winX + 20f;
        float tabsY = winY + 42f;
        float tabH = 16f;
        float currentTabX = tabsX;

        for (int i = 0; i < CATEGORIES.length; i++) {
            float tabW = Fonts.PS_MEDIUM.getWidth(CATEGORIES[i], 8.5f) + 14f;
            if (MouseUtil.isHovered(mouseX, mouseY, currentTabX, tabsY, tabW, tabH)) {
                commitEdit();
                selectedCategory = i;
                scroll = 0f;
                scrollAnim.setValue(0);
                return true;
            }
            currentTabX += tabW + 6f;
        }

        float divY = winY + 68f;
        float listX = winX + 16f;
        float listY = divY + 20f;
        float listW = winW - 32f;
        float listH = winH - (listY - winY) - 16f;

        if (MouseUtil.isHovered(mouseX, mouseY, listX, listY, listW, listH)) {
            List<AutoBuyParsedItemData> visibleItems = getFilteredItems();
            float rowH = 24f;
            float rowGap = 4f;
            float rowY = listY + (float) scrollAnim.getValue();

            for (AutoBuyParsedItemData item : visibleItems) {
                if (MouseUtil.isHovered(mouseX, mouseY, listX, rowY, listW, rowH)) {
                    float toggleW = 28f;
                    float toggleH = 14f;
                    float toggleX = listX + listW - 50f;
                    float toggleY = rowY + (rowH - toggleH) / 2f;
                    if (MouseUtil.isHovered(mouseX, mouseY, toggleX, toggleY, toggleW, toggleH)) {
                        commitEdit();
                        AutoBuyParsedItems.persistEnabled(item.path(), !item.enabled());
                        refresh();
                        return true;
                    }

                    float priceX = listX + listW - 165f;
                    float priceY = rowY + (rowH - 16f) / 2f;
                    float priceW = 80f;
                    float priceH = 16f;
                    if (MouseUtil.isHovered(mouseX, mouseY, priceX, priceY, priceW, priceH)) {
                        if (editingItem != item) {
                            commitEdit();
                            editingItem = item;
                            priceInput.setLength(0);
                            priceInput.append(item.price());
                        }
                        return true;
                    }

                    if (item.damageable()) {
                        float durX = listX + listW - 265f;
                        float durY = rowY + (rowH - 14f) / 2f;
                        if (MouseUtil.isHovered(mouseX, mouseY, durX, durY, 14f, 14f)) {
                            commitEdit();
                            AutoBuyParsedItems.persistMinDurability(item.path(),
                                    AutoBuyParsedItems.clampPercent(item.minDurability() - DURABILITY_STEP));
                            refresh();
                            return true;
                        }
                        float plusX = durX + 50f;
                        if (MouseUtil.isHovered(mouseX, mouseY, plusX, durY, 14f, 14f)) {
                            commitEdit();
                            AutoBuyParsedItems.persistMinDurability(item.path(),
                                    AutoBuyParsedItems.clampPercent(item.minDurability() + DURABILITY_STEP));
                            refresh();
                            return true;
                        }
                    }
                }
                rowY += rowH + rowGap;
            }
        }

        commitEdit();
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        commitEdit();
        scroll += (float) (verticalAmount * 35.0);
        scroll = Mth.clamp(scroll, maxScroll, 0f);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (editingItem != null) {
                cancelEdit();
                return true;
            }
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
            onClose();
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (editingItem != null) {
                commitEdit();
                return true;
            }
            if (searchFocused) {
                searchFocused = false;
                return true;
            }
        }

        if (editingItem != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !priceInput.isEmpty()) {
                priceInput.deleteCharAt(priceInput.length() - 1);
                return true;
            }
            return true;
        }

        if (searchFocused) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery.deleteCharAt(searchQuery.length() - 1);
                scroll = 0f;
                scrollAnim.setValue(0);
                return true;
            }
            return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        char chr = (char) input.codepoint();

        if (editingItem != null) {
            if (Character.isDigit(chr) && priceInput.length() < MAX_PRICE_CHARS) {
                priceInput.append(chr);
                return true;
            }
            return true;
        }

        if (searchFocused) {
            searchQuery.append(chr);
            scroll = 0f;
            scrollAnim.setValue(0);
            return true;
        }

        return super.charTyped(input);
    }

    private void commitEdit() {
        if (editingItem == null) return;
        String newPrice = priceInput.toString().trim();
        AutoBuyParsedItems.persistPrice(editingItem.path(), newPrice);
        editingItem = null;
        priceInput.setLength(0);
        refresh();
    }

    private void cancelEdit() {
        editingItem = null;
        priceInput.setLength(0);
    }
}
