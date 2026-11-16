package tokyo.ghoul.client.services;

import lombok.Getter;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.autobuy.engine.ServerHandler;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AnarchyReconnectService implements QuickImports {
    @Getter private static final AnarchyReconnectService instance = new AnarchyReconnectService();

    private static final long TIMEOUT_MS = 30_000L;
    private static final long CLICK_DELAY_MS = 300L;
    public static final Set<Integer> BLOCKED_HOLY_ANARCHIES = Set.of(2, 11, 21, 29);

    public enum Target {
        AUTO, ANARCHY, GRIEF;

        public static Target parse(String value) {
            if (value == null) return null;
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.startsWith("an") || lower.contains("анарх")) return ANARCHY;
            if (lower.startsWith("gr") || lower.contains("гриф")) return GRIEF;
            return null;
        }
    }

    private enum HolyMode {
        UNKNOWN, LITE_SPLIT, LITE, CLASSIC
    }

    private enum LiteCategory {
        SOLO("Соло", 0), DUO("Дуо", 1), TRIO("Трио", 2), CLAN("Клан", 3), NONE("", -1);

        private final String title;
        private final int tabSlot;

        LiteCategory(String title, int tabSlot) {
            this.title = title;
            this.tabSlot = tabSlot;
        }

        private static LiteCategory of(int anarchy) {
            if (anarchy >= 1 && anarchy <= 12) return SOLO;
            if (anarchy >= 13 && anarchy <= 26) return DUO;
            if (anarchy >= 27 && anarchy <= 39) return TRIO;
            return anarchy >= 40 ? CLAN : NONE;
        }

        private static LiteCategory of(String name) {
            if (name == null) return NONE;
            for (LiteCategory category : values()) {
                if (category != NONE && name.contains(category.title)) return category;
            }
            return NONE;
        }
    }

    private enum PageResult {
        NOT_FOUND, PAGED, CLICKED
    }

    private boolean running;
    private Target target = Target.AUTO;
    private int number = -1;
    private HolyMode holyMode = HolyMode.UNKNOWN;
    private String liteCategory = "";
    private boolean liteListOpened;
    private boolean categoryOpened;
    private int categoryCooldown;
    private final List<Integer> triedCategories = new ArrayList<>();
    private boolean menuRequested;
    private boolean compassUsed;
    private long stepAtMs;
    private long startedAtMs;
    private long nextClickAtMs;

    private AnarchyReconnectService() {
        this(true);
    }

    private AnarchyReconnectService(boolean subscribe) {
        if (subscribe) TickEvent.getInstance().subscribe(new Listener<>(e -> onTick()));
    }

    public static AnarchyReconnectService detached() {
        return new AnarchyReconnectService(false);
    }

    public void tick() {
        onTick();
    }


    public void start(Target requested, Integer requestedNumber) {
        if (mc.player == null || mc.level == null) return;
        if (ServerHandler.getInstance().isPvp()) {
            print("[RCT] В пвп нельзя перезаходить!");
            return;
        }

        reset();
        target = resolveTarget(requested);
        detectHolyMode();

        if (requestedNumber != null) {
            if (isHolyWorld()) {
                if (holyMode == HolyMode.UNKNOWN) holyMode = HolyMode.LITE_SPLIT;
                if (!acceptHolyNumber(requestedNumber)) {
                    invalidNumber();
                    return;
                }
            } else {
                number = requestedNumber;
            }
        } else if (isReallyWorld() && target == Target.ANARCHY) {
            invalidNumber();
            return;
        }

        if (isHolyWorld()) {
            if (holyMode == HolyMode.UNKNOWN) {
                print("[RCT] Неизвестный режим сервера!");
                reset();
                return;
            }
            if (number <= 0) {
                invalidNumber();
                return;
            }
            if (BLOCKED_HOLY_ANARCHIES.contains(number)) {
                print("[RCT] Анархия #" + number + " с привязкой к аккаунту — не захожу");
                reset();
                return;
            }
        } else if (isFunTime() || isSpooky()) {
            number = number > 0 ? number : currentCopyTimeNumber();
            if (number <= 0) {
                invalidNumber();
                return;
            }
        } else if (!isReallyWorld()) {
            print("[RCT] Этот сервер не поддерживается");
            reset();
            return;
        }

        long now = System.currentTimeMillis();
        stepAtMs = now;
        startedAtMs = now;
        mc.player.connection.sendCommand("hub");
        running = true;
        print("[RCT] Перезахожу" + (number > 0 ? " на " + number : "") + "…");
    }

    public int currentHolyNumber() {
        if (running || mc.player == null || !isHolyWorld()) return -1;
        detectHolyMode();
        int current = number;
        reset();
        return current;
    }

    public void stop() {
        if (running) print("[RCT] Остановлено");
        reset();
    }

    public boolean isRunning() {
        return running;
    }

    private void reset() {
        running = false;
        target = Target.AUTO;
        number = -1;
        holyMode = HolyMode.UNKNOWN;
        liteCategory = "";
        resetLiteList();
        liteListOpened = false;
        menuRequested = false;
        compassUsed = false;
        nextClickAtMs = 0L;
    }

    private void resetLiteList() {
        categoryOpened = false;
        categoryCooldown = 0;
        triedCategories.clear();
    }

    private void invalidNumber() {
        print("[RCT] Неверный номер сервера!");
        reset();
    }

    private Target resolveTarget(Target requested) {
        if (requested != null && requested != Target.AUTO) return requested;
        if (isReallyWorld()) return Target.GRIEF;
        return (isFunTime() || isSpooky()) && isCopyTimeGrief() ? Target.GRIEF : Target.ANARCHY;
    }


    private void onTick() {
        if (!running || mc.player == null || mc.level == null) return;
        if (System.currentTimeMillis() - startedAtMs > TIMEOUT_MS) {
            print("[RCT] Не получилось зайти — время вышло");
            reset();
            return;
        }

        if (isHolyWorld()) {
            tickHolyWorld();
        } else if (isFunTime() || isSpooky()) {
            tickCopyTime();
        } else if (isReallyWorld()) {
            tickReallyWorld();
        }
    }

    private boolean elapsed(long ms) {
        return System.currentTimeMillis() - stepAtMs >= ms;
    }


    private void tickCopyTime() {
        if (!elapsed(1000L)) return;
        if (mc.level.getDifficulty() == Difficulty.EASY || inHub() || elapsed(4000L)) {
            mc.player.connection.sendCommand("an" + number);
            reset();
        }
    }


    private void tickReallyWorld() {
        if (!elapsed(CLICK_DELAY_MS)) return;
        if (!compassUsed) {
            if (useCompass()) {
                compassUsed = true;
                stepAtMs = System.currentTimeMillis();
            }
            return;
        }

        ChestMenu menu = openChest();
        if (menu == null) return;
        String title = normalize(mc.gui.screen().getTitle().getString());
        boolean grief = target == Target.GRIEF;

        if (title.contains("выбор сервера") || title.contains("выберите сервер")) {
            int slot = findServerSlot(menu, grief);
            if (slot != -1) {
                click(menu, slot);
                stepAtMs = System.currentTimeMillis();
            }
        } else if (title.contains("выбор мира") && (!grief || title.contains("гриф"))) {
            int wanted = number > 0 ? number : grief ? reallyWorldGriefNumber() : -1;
            if (wanted <= 0) {
                invalidNumber();
                return;
            }
            PageResult result = pickWorld(menu, title, wanted, grief);
            if (result == PageResult.CLICKED) {
                reset();
            } else if (result == PageResult.PAGED) {
                stepAtMs = System.currentTimeMillis();
            }
        }
    }

    private boolean useCompass() {
        if (mc.gameMode == null) return false;
        for (int slot = 0; slot < 9; slot++) {
            if (!mc.player.getInventory().getItem(slot).is(Items.COMPASS)) continue;
            mc.player.getInventory().setSelectedSlot(slot);
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    private static int findServerSlot(ChestMenu menu, boolean grief) {
        int size = containerSize(menu);
        int firstGrief = -1;
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            String text = normalize(itemText(stack));
            if (grief) {
                if (stack.is(Items.CRAFTING_TABLE) && (text.contains("гриф") || text.contains("выжив"))) return i;
                if (firstGrief == -1 && text.contains("гриф")) firstGrief = i;
            } else if (text.contains("анарх")) {
                return i;
            }
        }
        if (grief) {
            for (int i = 0; i < size; i++) {
                if (menu.getSlot(i).getItem().is(Items.CRAFTING_TABLE)) return i;
            }
        }
        return firstGrief;
    }

    private PageResult pickWorld(ChestMenu menu, String title, int wanted, boolean grief) {
        int size = containerSize(menu);
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.isEmpty() && worldNumber(stack, grief) == wanted) {
                click(menu, i);
                return PageResult.CLICKED;
            }
        }

        int bySlot = layoutSlot(menu, title, wanted);
        if (bySlot != -1) {
            click(menu, bySlot);
            return PageResult.CLICKED;
        }

        int min = Integer.MAX_VALUE;
        int max = -1;
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            int n = worldNumber(stack, grief);
            if (n > 0) {
                min = Math.min(min, n);
                max = Math.max(max, n);
            }
        }

        if (max > 0) {
            if (wanted < min && currentPage(title) > 1) return turnPage(menu, false);
            if (wanted > max && hasNextPage(title)) return turnPage(menu, true);
            return PageResult.NOT_FOUND;
        }
        return hasNextPage(title) ? turnPage(menu, true) : PageResult.NOT_FOUND;
    }

    private static int layoutSlot(ChestMenu menu, String title, int wanted) {
        int page = currentPage(title);
        if (page <= 0) return -1;
        int index = wanted - (page - 1) * 32;
        if (index < 1 || index > 32) return -1;
        int row = (index - 1) / 8;
        int col = (index - 1) % 8;
        if (col >= 4) col++;
        int slot = row * 9 + col;
        if (slot >= containerSize(menu)) return -1;
        ItemStack stack = menu.getSlot(slot).getItem();
        return stack.isEmpty() || stack.is(Items.ARROW) ? -1 : slot;
    }

    private PageResult turnPage(ChestMenu menu, boolean next) {
        int size = containerSize(menu);
        int fallback = -1;
        for (int i = 0; i < size; i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (!stack.is(Items.ARROW) && !stack.is(Items.SPECTRAL_ARROW)) continue;
            String text = normalize(itemText(stack));
            if (next) {
                if (text.contains("след") || text.contains("даль") || text.contains("next")) return clickPage(menu, i);
                if (i % 9 >= 4) fallback = i;
            } else {
                if (text.contains("пред") || text.contains("назад") || text.contains("previous") || text.contains("back")) {
                    return clickPage(menu, i);
                }
                if (i % 9 <= 4 && fallback == -1) fallback = i;
            }
        }
        return fallback == -1 ? PageResult.NOT_FOUND : clickPage(menu, fallback);
    }

    private PageResult clickPage(ChestMenu menu, int slot) {
        click(menu, slot);
        return PageResult.PAGED;
    }

    private static int worldNumber(ItemStack stack, boolean grief) {
        String text = normalize(itemText(stack));
        int hash = text.indexOf('#');
        if (hash >= 0) {
            int n = digitsFrom(text, hash + 1);
            if (n > 0) return n;
        }
        for (String key : grief ? new String[]{"гриф", "grief"} : new String[]{"анарх", "anarchy"}) {
            int at = text.indexOf(key);
            if (at >= 0) {
                int n = digitsFrom(text, at + key.length());
                if (n > 0) return n;
            }
        }
        return -1;
    }

    private static int currentPage(String title) {
        int slash = title.indexOf('/');
        if (slash == -1) return -1;
        int end = slash - 1;
        while (end >= 0 && !Character.isDigit(title.charAt(end))) end--;
        if (end < 0) return -1;
        int start = end;
        while (start >= 0 && Character.isDigit(title.charAt(start))) start--;
        return parse(title.substring(start + 1, end + 1));
    }

    private static boolean hasNextPage(String title) {
        int slash = title.indexOf('/');
        int current = currentPage(title);
        int total = slash == -1 ? -1 : digitsFrom(title, slash + 1);
        return current <= 0 || total <= 0 || current < total;
    }


    private void tickHolyWorld() {
        Screen screen = mc.gui.screen();
        ChestMenu menu = openChest();
        if (menu == null) {
            if (screen == null && hasCompass() && !menuRequested) {
                menuRequested = true;
                if (holyMode == HolyMode.LITE_SPLIT) resetLiteList();
                mc.player.connection.sendCommand(holyMode == HolyMode.LITE_SPLIT ? "lite" : "menu");
            }
            return;
        }
        if (isEmpty(menu)) return;

        String title = screen.getTitle().getString();
        switch (holyMode) {
            case LITE_SPLIT -> tickLiteSplit(menu, title);
            case LITE -> tickLite(menu, title);
            case CLASSIC -> tickClassic(menu, title);
            default -> {
            }
        }
    }

    private void tickLiteSplit(ChestMenu menu, String title) {
        if (title.contains("Выберите режим")) {
            if (clickReady()) {
                click(menu, 12);
                resetLiteList();
            }
            return;
        }
        if (!title.contains("Выбор Лайт анархии")) return;
        if (!liteListOpened) {
            liteListOpened = true;
            resetLiteList();
        }

        int slot = findNumberedLite(menu, number);
        if (hasItem(menu, slot)) {
            if (clickReady()) {
                click(menu, slot);
                reset();
            }
            return;
        }

        if (categoryCooldown > 0) {
            categoryCooldown--;
            return;
        }
        int category = nextCategory(menu);
        if (category == -1) {
            print("[RCT] Лайт #" + number + " не найден в меню");
            reset();
            return;
        }
        if (!categoryOpened) {
            if (!clickReady()) return;
            click(menu, category);
            categoryOpened = true;
            triedCategories.add(category);
            categoryCooldown = 5;
            return;
        }
        categoryOpened = false;
    }

    private void tickLite(ChestMenu menu, String title) {
        if (!clickReady()) return;
        if (title.contains("Выберите режим")) {
            click(menu, 10);
        } else if (title.contains("Выберите сервер Лайт")) {
            int slot = switch (number) {
                case 1 -> 11;
                case 2 -> 12;
                case 3 -> 13;
                default -> 10;
            };
            if (hasItem(menu, slot)) {
                click(menu, slot);
                reset();
            }
        }
    }

    private void tickClassic(ChestMenu menu, String title) {
        if (!clickReady()) return;
        if (title.contains("Выберите режим")) {
            click(menu, 15);
        } else if (title.contains("Выберите Классич")) {
            int slot = classicSlot(number);
            if (hasItem(menu, slot)) {
                click(menu, slot);
                reset();
            }
        }
    }

    private boolean acceptHolyNumber(int value) {
        return switch (holyMode) {
            case LITE_SPLIT -> {
                LiteCategory category = LiteCategory.of(value);
                if (category == LiteCategory.NONE) yield false;
                number = value;
                liteCategory = category.title;
                yield true;
            }
            case LITE -> {
                if (value < 1 || value > 3) yield false;
                number = value;
                yield true;
            }
            case CLASSIC -> {
                if (classicSlot(value) == -1) yield false;
                number = value;
                yield true;
            }
            case UNKNOWN -> false;
        };
    }

    private void detectHolyMode() {
        holyMode = HolyMode.UNKNOWN;
        liteCategory = "";
        number = -1;
        if (!isHolyWorld()) return;

        String header = tabHeader();
        if (header.isBlank() && mc.player.connection != null) {
            StringBuilder names = new StringBuilder();
            for (PlayerInfo info : mc.player.connection.getOnlinePlayers()) {
                names.append(info.getProfile().name()).append(' ');
            }
            header = names.toString();
        }
        if (header.isBlank()) return;

        if (header.contains("Классик")) {
            holyMode = HolyMode.CLASSIC;
            parseHolyHeader(header, "Классик");
        } else if (header.contains("Лайт") && !header.contains("СолоЛайт") && !header.contains("ДуоЛайт")
                && !header.contains("ТриоЛайт") && !header.contains("КланЛайт")) {
            holyMode = HolyMode.LITE;
            parseHolyHeader(header, "Лайт");
        } else if (header.contains("Лайт")) {
            holyMode = HolyMode.LITE_SPLIT;
            parseHolyHeader(header, "Лайт");
        }
    }

    private void parseHolyHeader(String header, String modeWord) {
        String[] parts = header.split("▶");
        if (parts.length < 2) return;
        String server = parts[1].replace("Анархия", "").trim();
        String[] byHash = server.split("#");
        if (byHash.length < 2) return;
        liteCategory = server.replace(modeWord, "").replaceAll("#\\d+", "").trim();
        String digits = byHash[1].replaceAll("[^0-9]", "");
        if (!digits.isEmpty()) number = parse(digits);
    }

    private int nextCategory(ChestMenu menu) {
        if (!liteCategory.isBlank()) {
            int slot = findCategorySlot(menu, liteCategory);
            if (slot != -1 && !triedCategories.contains(slot)) return slot;
            int tab = LiteCategory.of(liteCategory).tabSlot;
            if (tab != -1 && !triedCategories.contains(tab)) return tab;
        }
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty() || triedCategories.contains(slot)) continue;
            String text = normalize(itemText(stack));
            if (!text.contains("#") && text.contains("лайт")
                    && (text.contains("соло") || text.contains("дуо") || text.contains("трио") || text.contains("клан"))) {
                return slot;
            }
        }
        return -1;
    }

    private static int findCategorySlot(ChestMenu menu, String category) {
        String wanted = normalize(category);
        if (wanted.isBlank()) return -1;
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            String text = normalize(itemText(stack));
            if (text.contains(wanted) && text.contains("лайт") && !text.contains("#")) return slot;
        }
        return -1;
    }

    private static int findNumberedLite(ChestMenu menu, int wanted) {
        if (wanted <= 0) return -1;
        String tag = "#" + wanted;
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) continue;
            String text = normalize(itemText(stack));
            if (!text.contains("лайт")) continue;
            int from = 0;
            while (true) {
                int at = text.indexOf(tag, from);
                if (at < 0) break;
                int end = at + tag.length();
                if (end == text.length() || !Character.isDigit(text.charAt(end))) return slot;
                from = end;
            }
        }
        return -1;
    }

    private static int classicSlot(int value) {
        return switch (value) {
            case 1 -> 20;
            case 2 -> 21;
            case 3 -> 22;
            case 4 -> 29;
            case 5 -> 30;
            case 6 -> 31;
            case 7 -> 38;
            case 8 -> 39;
            case 9 -> 40;
            default -> -1;
        };
    }


    private static String address() {
        ServerData server = BotContext.current() != null && mc.getConnection() != null
                ? mc.getConnection().getServerData()
                : mc.getCurrentServer();
        if ((server == null || server.ip == null) && mc.getConnection() != null) {
            server = mc.getConnection().getServerData();
        }
        return server != null && server.ip != null ? server.ip.toLowerCase(Locale.ROOT) : "";
    }

    private static boolean addressHas(String... keys) {
        String address = address();
        for (String key : keys) {
            if (address.contains(key)) return true;
        }
        return false;
    }

    private static boolean isFunTime() {
        return addressHas("funtime", "playft");
    }

    private static boolean isSpooky() {
        return addressHas("spooky");
    }

    private static boolean isReallyWorld() {
        return addressHas("reallyworld", "playrw");
    }

    private static boolean isHolyWorld() {
        if (addressHas("holy", "holly", "playhw")) return true;
        String header = tabHeader().toLowerCase(Locale.ROOT);
        return header.contains("holyworld") || header.contains("режимpvp");
    }

    private static boolean isCopyTimeGrief() {
        return tabHeader().toLowerCase(Locale.ROOT).contains("гриф");
    }

    private static int currentCopyTimeNumber() {
        String title = sidebarTitle();
        if (isFunTime()) {
            if (isCopyTimeGrief()) {
                return title.contains("Гриферский") ? digitsAfter(title, '-') : -1;
            }
            if (mc.level.getDifficulty() == Difficulty.EASY) return -1;
        } else if (tabHeader().toLowerCase(Locale.ROOT).contains("дуэли")) {
            return 1;
        }
        return title.contains("Анархия") ? digitsAfter(title, '-') : -1;
    }

    private static int reallyWorldGriefNumber() {
        String title = sidebarTitle();
        return title.contains("ГРИФ") ? digitsAfter(title, '#') : -1;
    }

    private static boolean inHub() {
        if (mc.player == null || mc.level == null) return false;
        BlockPos pos = mc.player.blockPosition();
        Block below = mc.level.getBlockState(pos.below()).getBlock();
        Block bottom = mc.level.getBlockState(new BlockPos(pos.getX(), 0, pos.getZ())).getBlock();
        if (isFunTime() && mc.level.getDifficulty() == Difficulty.NORMAL) return false;
        if (isFunTime() || isSpooky()) return below == Blocks.AIR || bottom == Blocks.AIR;
        if (isHolyWorld()) return mc.level.getBlockState(new BlockPos(pos.getX(), 6, pos.getZ())).getBlock() == Blocks.SAND;
        return mc.level.dimension() == Level.OVERWORLD && (bottom == Blocks.BEDROCK || below == Blocks.BEDROCK);
    }

    private static String tabHeader() {
        if (mc.gui == null || mc.gui.hud == null) return "";
        Component header = mc.gui.hud.getTabList().header;
        return header != null ? header.getString() : "";
    }

    private static String sidebarTitle() {
        if (mc.level == null) return "";
        Objective objective = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
        return objective != null ? objective.getDisplayName().getString() : "";
    }


    private static ChestMenu openChest() {
        return mc.gui.screen() instanceof AbstractContainerScreen<?>
                && mc.player.containerMenu instanceof ChestMenu menu ? menu : null;
    }

    private boolean clickReady() {
        return System.currentTimeMillis() >= nextClickAtMs;
    }

    private void click(ChestMenu menu, int slot) {
        if (mc.gameMode == null || slot < 0 || slot >= menu.slots.size()) return;
        mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
        nextClickAtMs = System.currentTimeMillis() + CLICK_DELAY_MS;
    }

    private static boolean hasItem(ChestMenu menu, int slot) {
        return slot >= 0 && slot < menu.slots.size() && menu.getSlot(slot).hasItem();
    }

    private static boolean isEmpty(ChestMenu menu) {
        int size = containerSize(menu);
        for (int i = 0; i < size; i++) {
            if (menu.getSlot(i).hasItem()) return false;
        }
        return true;
    }

    private static boolean hasCompass() {
        for (int slot = 0; slot < 36; slot++) {
            if (mc.player.getInventory().getItem(slot).is(Items.COMPASS)) return true;
        }
        return false;
    }

    private static int containerSize(ChestMenu menu) {
        return Math.max(0, menu.slots.size() - 36);
    }

    private static String itemText(ItemStack stack) {
        StringBuilder text = new StringBuilder(stack.getHoverName().getString());
        try {
            Item.TooltipContext context = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
            for (Component line : stack.getTooltipLines(context, mc.player, TooltipFlag.NORMAL)) {
                text.append(' ').append(line.getString());
            }
        } catch (Exception ignored) {
        }
        return text.toString();
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("§.", "").replace('ё', 'е').replace('Ё', 'Е').toLowerCase(Locale.ROOT);
    }

    private static int digitsAfter(String text, char separator) {
        int at = text.indexOf(separator);
        return at < 0 ? -1 : digitsFrom(text, at + 1);
    }

    private static int digitsFrom(String text, int from) {
        if (from < 0) return -1;
        for (int i = from; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) continue;
            int end = i + 1;
            while (end < text.length() && Character.isDigit(text.charAt(end))) end++;
            return parse(text.substring(i, end));
        }
        return -1;
    }

    private static int parse(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
