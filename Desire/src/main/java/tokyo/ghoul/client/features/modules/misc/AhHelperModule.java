package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BindSetting;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "AhHelper", description = "Помощник для работы с аукционом", category = Category.OTHER)
public class AhHelperModule extends Module {
    @Getter private static final AhHelperModule instance = new AhHelperModule();

    private static final long STEP_MS = 100L;
    private static final long PAGE_TIMEOUT_MS = 5000L;
    private static final long CONFIRM_COOLDOWN_MS = 3000L;
    private static final double SELL_PRICE_SPREAD = 1.6;
    private static final String AUTO_CONFIRM = "ah search auto confirm";

    private static final Pattern PAGE = Pattern.compile("\\b(\\d+)\\s*/\\s*(\\d+)\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d.,\\u00A0 ]*\\d|\\d");
    private static final Pattern BARE_PRICE = Pattern.compile("^[^\\p{L}\\d]{0,3}\\d[\\d.,\\u00A0 ]*[^\\p{L}\\d]{0,3}$");
    private static final Pattern SEARCH_TITLE = Pattern.compile("(?:^|\\s)(?:поиск|поис|пои|по|п)\\s*:",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern FORMATTING = Pattern.compile("(?i)§[0-9a-fk-or]");
    private static final Pattern POTION_TIME = Pattern.compile("(\\d+):(\\d+)");

    private final ModeSetting priceMode = new ModeSetting("Цена").values("За 1 шт", "За всё").value("За 1 шт");
    private final ColorSetting lowColor = new ColorSetting("Цвет дешёвого").value(new Color(60, 255, 60, 250));
    private final ColorSetting highColor = new ColorSetting("Цвет дорогого").value(new Color(255, 255, 60, 250));
    private final BooleanSetting showExpensive = new BooleanSetting("Показывать дорогие").value(false);
    private final BooleanSetting fastBuy = new BooleanSetting("Быстрая покупка (Shift+клик)").value(false);
    private final BindSetting searchKey = new BindSetting("Поиск предмета в руке").value(-999);
    private final BooleanSetting autoConfirm = new BooleanSetting("Авто-подтверждение (HW)").value(false);
    private final BooleanSetting autoSell = new BooleanSetting("Авто-продажа").value(false);
    private final ModeSetting sellMode = new ModeSetting("Режим продажи").values("FunTime", "HolyWorld").value("FunTime")
            .setVisible(autoSell::getValue);
    private final TextSetting sellPages = new TextSetting("Страниц для оценки").value("1")
            .setVisible(autoSell::getValue);
    private final BindSetting sellKey = new BindSetting("Бинд продажи").value(-999)
            .setVisible(autoSell::getValue);

    private final BooleanSetting armorThorns = new BooleanSetting("С шипами").value(true);
    private final BooleanSetting armorNoProt5 = new BooleanSetting("Без защиты 5").value(true);
    private final BooleanSetting armorDamaged = new BooleanSetting("С поломками").value(false);
    private final BooleanSetting armorNoMending = new BooleanSetting("Без починки").value(false);
    private final MultiBooleanSetting armorFilter = new MultiBooleanSetting("Скрывать броню")
            .value(armorThorns, armorNoProt5, armorDamaged, armorNoMending);
    private final SliderSetting minDurability = new SliderSetting("Мин. прочность брони, %").value(100f).range(1f, 100f).step(1f)
            .setVisible(armorDamaged::getValue);

    private final BooleanSetting pickNoSilk = new BooleanSetting("Без шёлкового касания").value(false);
    private final BooleanSetting pickNoMega = new BooleanSetting("Без мега-бульдозера").value(false);
    private final BooleanSetting pickNoMiner = new BooleanSetting("Без бульдозера 2").value(false);
    private final BooleanSetting pickNoMending = new BooleanSetting("Без починки").value(false);
    private final BooleanSetting pickNoMagnet = new BooleanSetting("Без магнита").value(false);
    private final BooleanSetting pickNoExp = new BooleanSetting("Без опытного 3").value(false);
    private final MultiBooleanSetting pickaxeFilter = new MultiBooleanSetting("Скрывать кирки")
            .value(pickNoSilk, pickNoMega, pickNoMiner, pickNoMending, pickNoMagnet, pickNoExp);

    private final BooleanSetting potionNoLevel3 = new BooleanSetting("Без 3 лвл").value(false);
    private final BooleanSetting potionNotCombined = new BooleanSetting("Не совмещёнки").value(false);
    private final BooleanSetting potionUnder6Min = new BooleanSetting("Без 6 минут").value(false);
    private final MultiBooleanSetting potionFilter = new MultiBooleanSetting("Скрывать зелья")
            .value(potionNoLevel3, potionNotCombined, potionUnder6Min);

    private enum Kind {
        ANY, ARMOR, PICKAXE, POTION
    }

    private enum SellState {
        IDLE, SEARCH, SCAN, WAIT_PAGE
    }

    private record Lot(int slot, long price, double effective) {
    }

    private record SellLot(long total, int count, double unit) {
    }

    private record Page(int current, int total) {
    }

    private final List<Lot> lots = new ArrayList<>();
    private double averagePrice;
    private double minPrice = Double.MAX_VALUE;
    private Kind kind = Kind.ANY;

    private int bestRateSlot = -1;
    private double bestRate = -1;
    private long exchangeBalance = -1;

    private boolean buyArmed;
    private volatile boolean confirmRequested;
    private long lastConfirmAt;
    private volatile String lastSearch = "";

    private SellState sellState = SellState.IDLE;
    private Item sellItem;
    private ItemStack sellStack = ItemStack.EMPTY;
    private String sellName = "";
    private int sellPagesWanted = 1;
    private int sellPagesScanned;
    private int expectedPage = -1;
    private final List<SellLot> sellLots = new ArrayList<>();
    private long stepAt;
    private long stateAt;

    public AhHelperModule() {
        addSettings(priceMode, lowColor, highColor, showExpensive, fastBuy, searchKey, autoConfirm,
                autoSell, sellMode, sellPages, sellKey, armorFilter, minDurability, pickaxeFilter, potionFilter);
    }

    @Override
    public void onEvent() {
        EventListener key = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != GLFW.GLFW_PRESS || mc.player == null || mc.gui.screen() != null) return;
            int pressed = event.mouse() ? -100 + event.key() : event.key();
            if (autoSell.getValue() && isBind(sellKey, pressed)) {
                startAutoSell();
            } else if (isBind(searchKey, pressed)) {
                searchHeldItem();
            }
        }));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend() && event.packet() instanceof ServerboundChatCommandPacket command) {
                String text = command.command().toLowerCase(Locale.ROOT);
                if (text.startsWith("ah search ")) lastSearch = text.substring("ah search ".length());
            } else if (event.isReceive() && autoConfirm.getValue() && event.packet() instanceof ClientboundSystemChatPacket chat
                    && plainLower(chat.content()).contains("/" + AUTO_CONFIRM)) {
                confirmRequested = true;
            }
        }));
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        addEvents(key, packet, update);
    }

    @Override
    public void onDisable() {
        resetSell();
        clearLots();
        buyArmed = false;
        super.onDisable();
    }

    private static boolean isBind(BindSetting bind, int pressed) {
        Integer value = bind.getValue();
        return value != null && value != -999 && value == pressed;
    }


    private void onUpdate() {
        if (mc.player == null) return;

        if (confirmRequested) {
            confirmRequested = false;
            long now = System.currentTimeMillis();
            if (now - lastConfirmAt >= CONFIRM_COOLDOWN_MS) {
                lastConfirmAt = now;
                mc.player.connection.sendCommand(AUTO_CONFIRM);
            }
        }

        if (sellState != SellState.IDLE) {
            tickAutoSell();
            return;
        }

        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            clearLots();
            buyArmed = false;
            return;
        }
        String title = screen.getTitle().getString();
        if (buyArmed && isConfirmScreen(screen)) {
            if (mc.player.containerMenu instanceof ChestMenu menu && mc.gameMode != null) {
                mc.gameMode.handleContainerInput(menu.containerId, 11, 0, ContainerInput.PICKUP, mc.player);
            }
            buyArmed = false;
        } else if (!isAuction(screen)) {
            clearLots();
        } else {
            detectKind(title);
            scanLots(screen.getMenu(), title);
        }
    }


    public static void renderHighlights(GuiGraphicsExtractor graphics, AbstractContainerMenu menu, int left, int top) {
        AhHelperModule module = instance;
        if (!module.isEnabled() || module.lots.isEmpty() && module.bestRateSlot == -1) return;
        for (Lot lot : module.lots) {
            if (!module.showExpensive.getValue() && lot.effective() > module.averagePrice) continue;
            if (lot.slot() < 0 || lot.slot() >= menu.slots.size()) continue;
            Slot slot = menu.slots.get(lot.slot());
            int color = module.priceColor(lot.effective());
            graphics.fillGradient(left + slot.x, top + slot.y, left + slot.x + 16, top + slot.y + 16, color & 0x00FFFFFF, color);
        }
        if (module.bestRateSlot >= 0 && module.bestRateSlot < menu.slots.size()
                && module.exchangeBalance != -1 && module.exchangeBalance >= module.bestRate) {
            Slot slot = menu.slots.get(module.bestRateSlot);
            graphics.fillGradient(left + slot.x, top + slot.y, left + slot.x + 16, top + slot.y + 16, 0x003CFF78, 0xBB3CFF78);
        }
    }

    private int priceColor(double effective) {
        double spread = averagePrice - minPrice;
        float t = spread > 0 ? (float) ((effective - minPrice) / spread) : 0f;
        t = Math.max(0f, Math.min(1f, t));
        Color low = lowColor.getValue();
        Color high = highColor.getValue();
        int r = Math.round(low.getRed() + (high.getRed() - low.getRed()) * t);
        int g = Math.round(low.getGreen() + (high.getGreen() - low.getGreen()) * t);
        int b = Math.round(low.getBlue() + (high.getBlue() - low.getBlue()) * t);
        float alpha = (low.getAlpha() + (high.getAlpha() - low.getAlpha()) * t) / 255f * (1f - t * 0.6f) * 0.8f;
        return Math.round(alpha * 255f) << 24 | r << 16 | g << 8 | b;
    }

    private void scanLots(AbstractContainerMenu menu, String title) {
        lots.clear();
        minPrice = Double.MAX_VALUE;
        averagePrice = 0;
        bestRateSlot = -1;
        bestRate = -1;
        boolean exchange = isExchange(title);
        double sum = 0;
        int counted = 0;

        int size = menu.slots.size() - 36;
        for (int i = 0; i < size; i++) {
            Slot slot = menu.slots.get(i);
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            List<Component> tooltip = tooltip(stack);
            if (isFiltered(stack, tooltip)) continue;

            if (exchange) {
                long rate = -1;
                for (Component line : tooltip) {
                    String text = clean(line.getString());
                    String lower = text.toLowerCase(Locale.ROOT);
                    if (lower.contains("биржа баланс")) exchangeBalance = parseNumber(text);
                    else if (lower.contains("курс")) rate = parseNumber(text);
                }
                if (rate > 0) {
                    if (exchangeBalance != -1 && exchangeBalance < rate) continue;
                    if (rate > bestRate) {
                        bestRate = rate;
                        bestRateSlot = slot.index;
                    }
                    lots.add(new Lot(slot.index, rate, -rate));
                    sum += -rate;
                    counted++;
                    minPrice = Math.min(minPrice, -rate);
                }
                continue;
            }

            long price = parsePrice(tooltip);
            if (price <= 0) continue;
            int count = Math.max(1, stack.getCount());
            double base = priceMode.is("За 1 шт") ? (double) price / count : price;
            double effective = base / durabilityFactor(stack);
            lots.add(new Lot(slot.index, price, effective));
            sum += effective;
            counted++;
            minPrice = Math.min(minPrice, effective);
        }

        if (counted > 0) {
            averagePrice = sum / counted;
        } else {
            clearLots();
        }
    }

    private void clearLots() {
        lots.clear();
        averagePrice = 0;
        minPrice = Double.MAX_VALUE;
        kind = Kind.ANY;
        bestRateSlot = -1;
        bestRate = -1;
        exchangeBalance = -1;
    }

    private static double durabilityFactor(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? 1.0 : Math.max(0.1, (double) (max - stack.getDamageValue()) / max);
    }


    public static void onContainerClick(Slot hovered) {
        AhHelperModule module = instance;
        if (!module.isEnabled() || !module.fastBuy.getValue() || hovered == null || !hovered.hasItem()) return;
        if (!KeyStorage.isPressed(GLFW.GLFW_KEY_LEFT_SHIFT) && !KeyStorage.isPressed(GLFW.GLFW_KEY_RIGHT_SHIFT)) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen) || !module.isAuction(screen)) return;

        Lot lot = module.lots.stream().filter(l -> l.slot() == hovered.index).findFirst().orElse(null);
        if (lot == null) return;
        if (lot.effective() > module.minPrice * 2.0) {
            NotifyWidget.text("Цена больше минимальной на 100%+");
            return;
        }
        module.buyArmed = true;
    }

    private boolean isConfirmScreen(AbstractContainerScreen<?> screen) {
        String title = lower(screen.getTitle().getString());
        if (title.contains("покупк") || title.contains("подтвер")) return true;
        return !isLargeChest(screen.getMenu()) && !isAuction(screen);
    }


    private void searchHeldItem() {
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            NotifyWidget.text("Нужен предмет в правой руке!");
            return;
        }
        mc.player.connection.sendCommand("ah search " + searchName(held));
    }

    private static String searchName(ItemStack stack) {
        return stack.getHoverName().getString()
                .replace("[", "").replace("] ", "").replace("- ", "").replace(" -", "")
                .replace("xxx ", "").replace(" xxx", "").replace("ggg ", "").replace(" ggg", "")
                .replace("gg ", "").replace(" gg", "").replace("123 ", "").replace(" 123", "")
                .replace("★", "").trim();
    }


    private void startAutoSell() {
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty()) {
            NotifyWidget.text("Возьмите предмет в руку");
            return;
        }
        sellItem = held.getItem();
        sellStack = held.copy();
        sellName = searchName(held);
        if (sellName.isEmpty()) {
            NotifyWidget.text("Не удалось получить название предмета");
            return;
        }
        String digits = sellPages.getValue() == null ? "" : sellPages.getValue().replaceAll("[^\\d]", "");
        try {
            sellPagesWanted = digits.isEmpty() ? 1 : Math.max(1, Integer.parseInt(digits));
        } catch (NumberFormatException e) {
            sellPagesWanted = 1;
        }
        sellPagesScanned = 0;
        expectedPage = -1;
        sellLots.clear();
        setSellState(SellState.SEARCH);
    }

    private void tickAutoSell() {
        if (mc.level == null) {
            resetSell();
            return;
        }
        long now = System.currentTimeMillis();
        switch (sellState) {
            case SEARCH -> {
                if (now - stepAt < STEP_MS) return;
                closeContainer();
                mc.player.connection.sendCommand("ah search " + sellName);
                setSellState(SellState.SCAN);
            }
            case SCAN -> {
                AbstractContainerMenu menu = openMenu();
                if (menu == null) {
                    if (now - stateAt >= PAGE_TIMEOUT_MS) {
                        print("Не удалось открыть аукцион");
                        resetSell();
                    }
                    return;
                }
                if (now - stepAt < STEP_MS) return;
                sellPagesScanned++;
                collectSellLots(menu);
                if (sellPagesScanned >= sellPagesWanted) {
                    finishAutoSell();
                    return;
                }
                Page page = currentPage(menu);
                if (page != null && page.current() >= page.total()) {
                    finishAutoSell();
                    return;
                }
                int next = nextPageSlot(menu);
                if (next == -1) {
                    finishAutoSell();
                    return;
                }
                expectedPage = page == null ? -1 : page.current() + 1;
                mc.gameMode.handleContainerInput(menu.containerId, next, 0, ContainerInput.PICKUP, mc.player);
                setSellState(SellState.WAIT_PAGE);
            }
            case WAIT_PAGE -> {
                AbstractContainerMenu menu = openMenu();
                if (menu == null) {
                    finishAutoSell();
                    return;
                }
                Page page = currentPage(menu);
                if (page != null && expectedPage != -1 && page.current() >= expectedPage
                        || expectedPage == -1 && now - stepAt >= STEP_MS) {
                    setSellState(SellState.SCAN);
                } else if (now - stateAt >= PAGE_TIMEOUT_MS) {
                    finishAutoSell();
                }
            }
            default -> {
            }
        }
    }

    private void collectSellLots(AbstractContainerMenu menu) {
        int size = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty() || stack.getItem() != sellItem) continue;
            int count = Math.max(1, stack.getCount());
            double unit = unitPrice(stack, count);
            if (unit > 0) sellLots.add(new SellLot(Math.round(unit * count), count, unit));
        }
    }

    private double unitPrice(ItemStack stack, int count) {
        List<Component> tooltip = tooltip(stack);
        if (sellMode.is("HolyWorld")) {
            for (Component line : tooltip) {
                String text = line.getString();
                String lower = text.toLowerCase(Locale.ROOT);
                if (lower.contains("цена за 1") || lower.contains("price per 1")) {
                    int colon = text.lastIndexOf(':');
                    long value = parseNumber(colon == -1 ? text : text.substring(colon + 1));
                    if (value > 0) return value;
                }
            }
        }
        for (Component line : tooltip) {
            String text = line.getString();
            if (text.contains("$") && !text.contains("%")) {
                long value = parseNumber(text);
                if (value > 0) return (double) value / count;
            }
        }
        return -1;
    }

    private void finishAutoSell() {
        List<SellLot> sorted = sellLots.stream().sorted(Comparator.comparingDouble(SellLot::unit)).toList();
        long total = 0;
        int units = 0;
        int used = 0;
        if (!sorted.isEmpty()) {
            double limit = sorted.getFirst().unit() * SELL_PRICE_SPREAD;
            for (SellLot lot : sorted) {
                if (lot.unit() > limit && used > 0) break;
                total += lot.total();
                units += lot.count();
                used++;
            }
            if (used == 1 && sorted.size() > 1) {
                total += sorted.get(1).total();
                units += sorted.get(1).count();
            }
        }

        closeContainer();
        if (units > 0) {
            long unit = Math.round((double) total / units);
            int count = Math.max(1, sellStack.getCount());
            long price = unit * count;
            mc.player.connection.sendCommand("ah sell " + price);
            print("Выставил " + sellItem.getName(sellStack).getString() + " x" + count + " за $" + format(price)
                    + " (" + format(unit) + "$ за 1 шт.)");
        } else {
            print("Цены для предмета не найдены");
        }
        resetSell();
    }

    private void setSellState(SellState state) {
        sellState = state;
        stepAt = System.currentTimeMillis();
        stateAt = stepAt;
    }

    private void resetSell() {
        sellState = SellState.IDLE;
        sellItem = null;
        sellStack = ItemStack.EMPTY;
        sellName = "";
        sellPagesWanted = 1;
        sellPagesScanned = 0;
        expectedPage = -1;
        sellLots.clear();
    }

    private Page currentPage(AbstractContainerMenu menu) {
        if (mc.gui.screen() != null) {
            Page page = parsePage(mc.gui.screen().getTitle().getString());
            if (page != null) return page;
        }
        int size = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            for (Component line : tooltip(stack)) {
                Page page = parsePage(line.getString());
                if (page != null) return page;
            }
        }
        return null;
    }

    private static Page parsePage(String text) {
        Matcher matcher = PAGE.matcher(text);
        if (!matcher.find()) return null;
        try {
            return new Page(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int nextPageSlot(AbstractContainerMenu menu) {
        int size = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) continue;
            StringBuilder text = new StringBuilder(stack.getHoverName().getString().toLowerCase(Locale.ROOT));
            for (Component line : tooltip(stack)) text.append(' ').append(line.getString().toLowerCase(Locale.ROOT));
            String all = text.toString();
            if (all.contains("след") || all.contains("далее") || all.contains("впер") || all.contains("next")) return i;
        }
        return -1;
    }

    private AbstractContainerMenu openMenu() {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?>) || mc.player == null) return null;
        AbstractContainerMenu menu = mc.player.containerMenu;
        return menu == mc.player.inventoryMenu ? null : menu;
    }

    private void closeContainer() {
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.closeContainer();
        }
    }

    private static String format(long value) {
        return String.format(Locale.US, "%,d", value);
    }


    private boolean isFiltered(ItemStack stack, List<Component> tooltip) {
        if (kind == Kind.ARMOR && isArmor(stack)) {
            if (armorThorns.getValue() && level(stack, Enchantments.THORNS) > 0) return true;
            if (armorNoProt5.getValue() && level(stack, Enchantments.PROTECTION) < 5) return true;
            if (armorDamaged.getValue() && stack.getMaxDamage() > 0) {
                double percent = (double) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage() * 100.0;
                if (percent < minDurability.getValue()) return true;
            }
            if (armorNoMending.getValue() && level(stack, Enchantments.MENDING) == 0) return true;
        }

        if (kind == Kind.PICKAXE && stack.getItem().getDescriptionId().contains("pickaxe")) {
            if (pickNoSilk.getValue() && level(stack, Enchantments.SILK_TOUCH) == 0) return true;
            if (pickNoMending.getValue() && level(stack, Enchantments.MENDING) == 0) return true;
            if (pickNoMagnet.getValue() && !tooltipHas(tooltip, "магнит")) return true;
            if (pickNoMega.getValue() && !tooltipHas(tooltip, "мега-бульдозер")) return true;
            if (pickNoExp.getValue() && !tooltipHasLevel(tooltip, "опытный", 3)) return true;
            if (pickNoMiner.getValue() && !tooltipHasLevel(tooltip, "бульдозер", 2)) return true;
        }

        if (kind == Kind.POTION && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))) {
            List<String> lines = tooltip.stream().map(line -> line.getString().toLowerCase(Locale.ROOT)).toList();
            if (lines.stream().noneMatch(line -> line.contains("инвиз") || line.contains("невид"))) {
                boolean buff = isBuffPotion(lines);
                if (potionNoLevel3.getValue() && buff && !isLevel3(lines)) return true;
                if (potionNotCombined.getValue() && buff && !isCombined(lines)) return true;
                return potionUnder6Min.getValue() && isUnder6Min(lines);
            }
        }
        return false;
    }

    private static boolean isArmor(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null || stack.getMaxDamage() <= 0) return false;
        EquipmentSlot slot = equippable.slot();
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private static int level(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        if (mc.level == null) return 0;
        Holder<Enchantment> holder = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantment);
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(holder);
    }

    private static boolean tooltipHas(List<Component> tooltip, String word) {
        for (Component line : tooltip) {
            if (line.getString().toLowerCase(Locale.ROOT).contains(word)) return true;
        }
        return false;
    }

    private static boolean tooltipHasLevel(List<Component> tooltip, String word, int level) {
        String roman = switch (level) {
            case 2 -> "ii";
            case 3 -> "iii";
            case 4 -> "iv";
            case 5 -> "v";
            default -> "";
        };
        for (Component line : tooltip) {
            String text = line.getString().toLowerCase(Locale.ROOT);
            if (!text.contains(word)) continue;
            if (level <= 1) return true;
            if (!roman.isEmpty() && text.contains(roman) || text.contains(" " + level) || text.contains(level + " ")) return true;
        }
        return false;
    }

    private static boolean isBuffPotion(List<String> lines) {
        for (String line : lines) {
            if (line.contains("сила") && !line.contains("урон") || line.contains("скорость") && !line.contains("атаки")
                    || line.contains("strength") || line.contains("speed") && !line.contains("attack")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLevel3(List<String> lines) {
        int index = 0;
        for (String line : lines) {
            if (++index > 2) break;
            if (!(line.contains("сил") || line.contains("скорость")) || line.contains("iii") || line.contains("усиленн")) return true;
        }
        return false;
    }

    private static boolean isCombined(List<String> lines) {
        return lines.stream().anyMatch(line -> line.contains("сила")) && lines.stream().anyMatch(line -> line.contains("скорость"));
    }

    private static boolean isUnder6Min(List<String> lines) {
        for (String line : lines) {
            if (!line.contains("сила")) continue;
            Matcher matcher = POTION_TIME.matcher(line);
            if (matcher.find()) {
                try {
                    if (Integer.parseInt(matcher.group(1)) < 6) return true;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private void detectKind(String title) {
        if (!detectKindFrom(lower(title)) && !detectKindFrom(lastSearch)) kind = Kind.ANY;
    }

    private boolean detectKindFrom(String text) {
        if (text == null || text.isEmpty()) return false;
        if (containsAny(text, "зель", "баф", "сила", "силы", "силка", "силька", "скорост", "инвиз", "невид")) {
            kind = Kind.POTION;
            return true;
        }
        if (containsAny(text, "шлем", "нагрудник", "понож", "ботин", "брон")) {
            kind = Kind.ARMOR;
            return true;
        }
        if (containsAny(text, "кирк", "кирок", "шахт", "бур")) {
            kind = Kind.PICKAXE;
            return true;
        }
        return false;
    }


    private boolean isAuction(AbstractContainerScreen<?> screen) {
        String title = lower(screen.getTitle().getString());
        if (title.contains("аукцион") || title.contains("поиск") || isExchange(title) || SEARCH_TITLE.matcher(title).find()) return true;
        if (containsAny(title, "нагрудник", "шлем", "понож", "ботин", "кирк", "зель")) return true;
        if (!isLargeChest(screen.getMenu())) return false;
        int priced = 0;
        int size = screen.getMenu().slots.size() - 36;
        for (int i = 0; i < size && priced < 3; i++) {
            ItemLore lore = screen.getMenu().slots.get(i).getItem().get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty() && parsePrice(lore.lines()) > 0) priced++;
        }
        return priced >= 3 || title.startsWith(":") && priced > 0;
    }

    private static boolean isExchange(String title) {
        String lower = lower(title);
        return lower.contains("донмаркет") || lower.contains("дон маркет");
    }

    private static boolean isLargeChest(AbstractContainerMenu menu) {
        return menu instanceof ChestMenu chest && chest.getRowCount() >= 5;
    }


    private static long parsePrice(List<Component> lines) {
        long fallback = -1;
        for (Component line : lines) {
            String text = clean(line.getString());
            if (text.isEmpty() || text.contains("%")) continue;
            String lower = text.toLowerCase(Locale.ROOT);
            if (lower.contains("цена") || lower.contains("цeна") || lower.contains("ценa") || lower.contains("цeнa")
                    || lower.contains("$") || lower.contains("price") || lower.contains("стоим")) {
                long value = parseNumber(text);
                if (value > 0) return value;
            } else if (BARE_PRICE.matcher(text).matches()) {
                long value = parseNumber(text);
                if (value >= 100) fallback = Math.max(fallback, value);
            }
        }
        return fallback;
    }

    private static long parseNumber(String text) {
        Matcher matcher = NUMBER.matcher(clean(text));
        long best = -1;
        while (matcher.find()) {
            String digits = matcher.group().replaceAll("[^\\d]", "");
            if (digits.isEmpty()) continue;
            try {
                best = Math.max(best, Long.parseLong(digits));
            } catch (NumberFormatException ignored) {
            }
        }
        return best;
    }

    private static String clean(String text) {
        if (text == null || text.isEmpty()) return "";
        String stripped = FORMATTING.matcher(text).replaceAll("");
        StringBuilder out = new StringBuilder(stripped.length());
        stripped.codePoints().forEach(c -> {
            int type = Character.getType(c);
            if (type != Character.PRIVATE_USE && type != Character.CONTROL && type != Character.FORMAT
                    && type != Character.SURROGATE && type != Character.UNASSIGNED) {
                out.appendCodePoint(c);
            }
        });
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    private static String lower(String text) {
        return clean(text).toLowerCase(Locale.ROOT);
    }

    private static String plainLower(Component text) {
        return text.getString().replaceAll("(?i)§[0-9a-fk-or]", "").replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private static List<Component> tooltip(ItemStack stack) {
        try {
            Item.TooltipContext context = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
            return stack.getTooltipLines(context, mc.player, mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
        } catch (Exception e) {
            return List.of();
        }
    }
}
