package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.ItemDatabase;
import tokyo.ghoul.client.autobuy.ItemEntry;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.*;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.text;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.textCenter;
import static tokyo.ghoul.client.ui.clickgui.screen.LightStyle.textRight;

/**
 * Вкладка AutoBuy в ClickGUI Desire: разделы слева, карточки предметов справа.
 * Панель рисуется в under-слое, поэтому ванильные иконки предметов выводятся отдельно в {@link #renderItems}.
 */
public final class AutoBuyPage {
    public enum FilterMode {
        ALL("Все"),
        ENABLED("Вкл"),
        DISABLED("Выкл");

        final String label;

        FilterMode(String label) {
            this.label = label;
        }
    }

    public enum Section {
        ALL("Все предметы", Items.CHEST),
        WEAPON("Оружие", Items.NETHERITE_SWORD),
        ARMOR("Броня", Items.NETHERITE_CHESTPLATE),
        TOOLS("Инструменты", Items.NETHERITE_PICKAXE),
        TALISMANS("Талисманы", Items.TOTEM_OF_UNDYING),
        CONSUMABLES("Расходники", Items.EXPERIENCE_BOTTLE),
        CUSTOM("Кастомные", Items.NETHER_STAR),
        RESOURCES("Ресурсы", Items.NETHERITE_INGOT);

        final String label;
        final ItemStack icon;

        Section(String label, Item icon) {
            this.label = label;
            this.icon = new ItemStack(icon);
        }

        static Section of(ItemEntry e) {
            String id = e.itemId == null ? "" : e.itemId.toLowerCase(Locale.ROOT);
            int colon = id.indexOf(':');
            if (colon >= 0) id = id.substring(colon + 1);

            if (e.matchAnySword || id.endsWith("_sword") || id.equals("bow") || id.equals("crossbow")
                    || id.equals("trident") || id.equals("mace")) return WEAPON;
            if (e.isArmor() || id.equals("shield")) return ARMOR;
            if (id.endsWith("_pickaxe") || id.endsWith("_axe") || id.endsWith("_shovel") || id.endsWith("_hoe")
                    || id.equals("shears") || id.equals("fishing_rod") || id.equals("flint_and_steel")) return TOOLS;
            if (id.equals("totem_of_undying") || id.equals("player_head")) return TALISMANS;
            if (id.endsWith("potion") || id.equals("experience_bottle") || id.endsWith("golden_apple")
                    || id.equals("ender_pearl") || id.equals("chorus_fruit") || id.equals("firework_rocket")
                    || id.endsWith("_spawn_egg")) return CONSUMABLES;
            if (e.requireCustom) return CUSTOM;
            return RESOURCES;
        }
    }

    private record ItemDraw(ItemStack stack, float x, float y, float size, boolean clipped) {}

    private static final int COLUMNS = 3;
    private static final long PRICE_STEP = 50_000L;
    private static final Color MARKET = new Color(110, 200, 130);
    private static final float MARQUEE_SPEED = 38f;
    private static final float MARQUEE_DELAY = 0.35f;
    private static final float MARQUEE_GAP = 2.2f;
    /** Период перелива градиента тира, мс (у шрифта по умолчанию 3000). */
    private static final long TIER_PERIOD = 4800L;

    private Section section = Section.ALL;
    private FilterMode filter = FilterMode.ALL;
    private String query = "";
    private final Map<String, ItemStack> iconCache = new HashMap<>();
    private final Map<ItemEntry, float[]> anims = new HashMap<>();
    private final Map<Section, float[]> sectionAnims = new EnumMap<>(Section.class);
    private final Map<Section, Integer> counts = new EnumMap<>(Section.class);
    private final float[] toolHover = new float[FilterMode.values().length + 2];
    private float filterAnim;

    private ItemEntry editingItem;
    private String priceBuffer = "";

    private float scroll, scrollTarget;
    private float x, y, w, h;
    private final List<ItemEntry> shown = new ArrayList<>();
    private final List<ItemDraw> itemDraws = new ArrayList<>();

    public void refresh() {
        if (ItemDatabase.LIST.isEmpty()) {
            ItemDatabase.reload();
        }
        if (AutoBuyConfig.cfg().entries.isEmpty()) {
            AutoBuyConfig.cfg().reconcileWithDatabase();
            AutoBuyConfig.save();
        }
    }

    public void resetScroll() {
        scroll = scrollTarget = 0f;
    }

    public boolean isTyping() {
        return editingItem != null;
    }

    public void releaseKeyboard() {
        stopEditing();
    }


    // Боковая панель разделов
    private float sideW() { return r(196f); }
    private float sideRowH() { return r(40f); }
    private float sideRowY(int i) { return y + r(4f) + i * (sideRowH() + r(4f)); }
    private float splitX() { return x + sideW() + r(18f); }

    // Правая часть: тулбар и сетка карточек
    private float mainX() { return splitX() + r(20f); }
    private float mainW() { return x + w - mainX(); }
    private float barH() { return r(36f); }
    private float barCY() { return y + barH() / 2f; }
    private float segW() { return r(54f); }
    private float segH() { return r(32f); }
    private float actionW() { return r(84f); }
    private float disAllX() { return x + w - actionW(); }
    private float enAllX() { return disAllX() - r(8f) - actionW(); }
    private float filterX() { return enAllX() - r(14f) - segW() * FilterMode.values().length - r(6f); }

    private float gap() { return r(14f); }
    private float rowGap() { return r(15f); }
    private float cardW() { return (mainW() - gap() * (COLUMNS - 1)) / COLUMNS; }
    private float cardH() { return r(100f); }
    private float listY() { return y + barH() + r(18f); }
    private float listH() { return y + h - listY(); }
    private float cardX(int index) { return mainX() + (index % COLUMNS) * (cardW() + gap()); }
    private float cardY(int index) { return listY() + (index / COLUMNS) * (cardH() + rowGap()) - Math.round(scroll); }

    private float pad() { return r(16f); }
    private float slot() { return r(36f); }
    private float topCY(int i) { return cardY(i) + pad() + slot() / 2f; }
    private float toggleW() { return r(27f); }
    private float toggleH() { return r(15f); }
    private float toggleX(int i) { return cardX(i) + cardW() - pad() - toggleW(); }
    private float fieldY(int i) { return cardY(i) + cardH() - pad() - fieldH(); }
    private float fieldH() { return r(28f); }
    private float zeroW() { return r(30f); }
    private float plusW() { return r(48f); }
    private float zeroX(int i) { return cardX(i) + cardW() - pad() - zeroW(); }
    private float plusX(int i) { return zeroX(i) - r(6f) - plusW(); }
    private float fieldX(int i) { return cardX(i) + pad(); }
    private float fieldW(int i) { return plusX(i) - r(6f) - fieldX(i); }


    private boolean matchesQuery(ItemEntry e) {
        if (query.isEmpty()) return true;
        return contains(e.displayName) || contains(e.itemId) || contains(e.searchQuery) || contains(e.loreKeyword);
    }

    private boolean contains(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private boolean matchesFilter(ItemEntry e) {
        return filter == FilterMode.ALL || (filter == FilterMode.ENABLED) == e.enabled;
    }

    private void collect() {
        shown.clear();
        counts.clear();
        for (Section s : Section.values()) counts.put(s, 0);
        for (ItemEntry e : AutoBuyConfig.cfg().entries) {
            if (e == null || !matchesQuery(e) || !matchesFilter(e)) continue;
            Section s = Section.of(e);
            counts.merge(Section.ALL, 1, Integer::sum);
            counts.merge(s, 1, Integer::sum);
            if (section == Section.ALL || section == s) shown.add(e);
        }
    }

    private List<Section> visibleSections() {
        List<Section> list = new ArrayList<>();
        int[] total = new int[Section.values().length];
        for (ItemEntry e : AutoBuyConfig.cfg().entries) {
            if (e != null) total[Section.of(e).ordinal()]++;
        }
        for (Section s : Section.values()) {
            if (s == Section.ALL || total[s.ordinal()] > 0 || s == section) list.add(s);
        }
        return list;
    }

    private ItemStack getStack(ItemEntry entry) {
        if (entry == null) return ItemStack.EMPTY;
        String key = (entry.itemId != null ? entry.itemId : "") + "#" + entry.matchAnySword + "#" + entry.requireCustom;
        return iconCache.computeIfAbsent(key, k -> {
            ItemStack stack = new ItemStack(Items.CHEST);
            if (entry.matchAnySword) {
                stack = new ItemStack(Items.NETHERITE_SWORD);
            } else if (entry.itemId != null && !entry.itemId.isBlank()) {
                Identifier id = Identifier.tryParse(entry.itemId);
                if (id != null) {
                    Item item = BuiltInRegistries.ITEM.getValue(id);
                    if (item != Items.AIR) stack = new ItemStack(item);
                }
            }
            if (entry.requireCustom) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
            return stack;
        });
    }


    public void render(float x, float y, float w, float h, String search, float alpha, double mx, double my, float dt) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        itemDraws.clear();

        String q = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        if (!q.equals(query)) {
            query = q;
            resetScroll();
        }
        collect();

        drawSidebar(alpha, mx, my, dt);
        rect(splitX(), y, r(1f), h, 0f, a(tint(20), alpha));
        drawToolbar(alpha, mx, my, dt);
        drawGrid(alpha, mx, my, dt);
    }

    /** Ванильные предметы рисуются после under-слоя, иначе панель их перекрывает. */
    public void renderItems(GuiGraphicsExtractor context, float alpha) {
        if (alpha < 0.5f || itemDraws.isEmpty()) return;
        boolean clipping = false;
        for (ItemDraw draw : itemDraws) {
            if (draw.clipped() && !clipping) {
                context.enableScissor((int) Math.floor(mainX() - r(4f)), (int) Math.floor(listY()),
                        (int) Math.ceil(x + w + r(4f)), (int) Math.ceil(listY() + listH()));
                clipping = true;
            } else if (!draw.clipped() && clipping) {
                context.disableScissor();
                clipping = false;
            }
            context.pose().pushMatrix();
            context.pose().translate(draw.x(), draw.y());
            context.pose().scale(draw.size() / 16f, draw.size() / 16f);
            context.item(draw.stack(), 0, 0);
            context.pose().popMatrix();
        }
        if (clipping) context.disableScissor();
    }

    private void drawSidebar(float alpha, double mx, double my, float dt) {
        List<Section> sections = visibleSections();
        float rowH = sideRowH(), iconSize = r(20f);
        for (int i = 0; i < sections.size(); i++) {
            Section s = sections.get(i);
            float ry = sideRowY(i), cy = ry + rowH / 2f;
            float[] anim = sectionAnims.computeIfAbsent(s, k -> new float[2]);
            anim[0] = ease(anim[0], inside(mx, my, x, ry, sideW(), rowH) ? 1f : 0f, 18f, dt);
            anim[1] = ease(anim[1], s == section ? 1f : 0f, 16f, dt);

            float bg = Math.max(anim[0] * 0.6f, anim[1]);
            if (bg > 0.01f) rect(x, ry, sideW(), rowH, r(10f), a(tint(10), alpha * bg));
            if (anim[1] > 0.01f) {
                float bh = r(16f) * (0.4f + 0.6f * anim[1]), bw = r(2.5f);
                glow(x + bw, cy, r(14f), bh * 1.8f, a(accent(110), alpha * anim[1]));
                rect(x, cy - bh / 2f, bw, bh, bw / 2f, a(accent(), alpha * anim[1]));
            }

            itemDraws.add(new ItemDraw(s.icon, x + r(14f), cy - iconSize / 2f, iconSize, false));
            Color label = mix(mix(GRAY, WHITE, anim[0]), WHITE, anim[1]);
            text(INTER, s.label, x + r(44f), cy, r(16f), a(label, alpha));

            String count = String.valueOf(counts.getOrDefault(s, 0));
            float cs = r(13f), cw = Math.max(r(24f), INTER.getWidth(count, cs) + r(12f)), ch = r(20f);
            float cx = x + sideW() - r(10f) - cw;
            rect(cx, cy - ch / 2f, cw, ch, r(6f), a(mix(tint(8), accent(46), anim[1]), alpha));
            textCenter(INTER, count, cx + cw / 2f, cy, cs, a(mix(GRAY, accent(), anim[1]), alpha));
        }

        int total = AutoBuyConfig.cfg().entries.size(), enabled = 0;
        for (ItemEntry e : AutoBuyConfig.cfg().entries) if (e != null && e.enabled) enabled++;
        float by = y + h - r(52f);
        rect(x, by, sideW(), r(52f), r(12f), a(tint(5), alpha));
        text(INTER, "Активно", x + r(14f), by + r(17f), r(13f), a(GRAY, alpha));
        textRight(INTER, enabled + " / " + total, x + sideW() - r(14f), by + r(17f), r(13f), a(WHITE, alpha));
        float trackY = by + r(34f), trackW = sideW() - r(28f), fill = total == 0 ? 0f : trackW * enabled / total;
        rect(x + r(14f), trackY, trackW, r(4f), r(2f), a(tint(10), alpha));
        if (fill > 0f) rect(x + r(14f), trackY, fill, r(4f), r(2f), a(accent(), alpha));
    }

    private void drawToolbar(float alpha, double mx, double my, float dt) {
        float cy = barCY();
        float size = r(20f), tx = mainX();
        String path = "Autobuy / ";
        text(INTER, path, tx, cy, size, a(WHITE, alpha));
        float nx = tx + INTER.getWidth(path, size);
        text(INTER, ellipsize(INTER, section.label, size, filterX() - nx - r(12f)), nx, cy, size, a(accent(), alpha));

        FilterMode[] modes = FilterMode.values();
        filterAnim = ease(filterAnim, filter.ordinal(), 16f, dt);
        float fx = filterX(), fy = cy - segH() / 2f, fw = segW() * modes.length + r(6f);
        rect(fx, fy, fw, segH(), r(10f), a(new Color(0, 0, 0, 110), alpha));
        rect(fx + r(3f) + segW() * filterAnim, fy + r(3f), segW(), segH() - r(6f), r(8f), a(accent(46), alpha));
        for (FilterMode mode : modes) {
            int i = mode.ordinal();
            float sx = fx + r(3f) + segW() * i;
            toolHover[i] = ease(toolHover[i], inside(mx, my, sx, fy, segW(), segH()) ? 1f : 0f, 18f, dt);
            float on = Math.max(0f, 1f - Math.abs(filterAnim - i));
            Color c = mix(mix(GRAY, WHITE, toolHover[i]), accent(), on);
            textCenter(INTER, mode.label, sx + segW() / 2f, cy, r(14.5f), a(c, alpha));
        }

        drawAction("Вкл все", enAllX(), modes.length, alpha, mx, my, dt);
        drawAction("Выкл все", disAllX(), modes.length + 1, alpha, mx, my, dt);
    }

    private void drawAction(String label, float bx, int idx, float alpha, double mx, double my, float dt) {
        float by = barCY() - segH() / 2f;
        toolHover[idx] = ease(toolHover[idx], inside(mx, my, bx, by, actionW(), segH()) ? 1f : 0f, 18f, dt);
        rect(bx, by, actionW(), segH(), r(10f), a(mix(tint(7), tint(13), toolHover[idx]), alpha));
        textCenter(INTER, label, bx + actionW() / 2f, barCY(), r(14.5f), a(mix(GRAY, WHITE, toolHover[idx]), alpha));
    }

    private void drawGrid(float alpha, double mx, double my, float dt) {
        float listY = listY(), listH = listH();
        int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
        float content = Math.max(0f, rows * (cardH() + rowGap()) - rowGap());
        float maxScroll = Math.max(0f, content - listH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll = ease(scroll, scrollTarget, 17f, dt);

        if (shown.isEmpty()) {
            float cx = mainX() + mainW() / 2f, cy = listY + listH / 2f - r(20f);
            textCenter(INTER, query.isEmpty() ? "В этом разделе пусто" : "Ничего не найдено", cx, cy, r(17f), a(WHITE, alpha));
            textCenter(INTER, query.isEmpty() ? "Смените раздел или фильтр" : "Поиск идёт по названию и id предмета",
                    cx, cy + r(24f), r(14f), a(MUTE, alpha));
            return;
        }

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, mainX() - r(4f), listY, mainW() + r(8f), listH);
        try {
            boolean live = inside(mx, my, mainX(), listY, mainW(), listH);
            for (int i = 0; i < shown.size(); i++) {
                float cy = cardY(i);
                if (cy + cardH() < listY || cy > listY + listH) continue;
                drawCard(i, shown.get(i), alpha, live, mx, my, dt);
            }
        } finally {
            ScissorUtil.stop(matrices);
        }

        if (maxScroll > 0f) {
            float bw = r(2.5f), thumb = Math.max(r(36f), listH * listH / content);
            float offset = (listH - thumb) * (scroll / maxScroll);
            rect(x + w + r(8f), listY + offset, bw, thumb, bw / 2f, a(tint(22), alpha));
        }
    }

    private void drawCard(int i, ItemEntry item, float alpha, boolean live, double mx, double my, float dt) {
        // 0 hover, 1 enabled, 2 +50k hover, 3 reset hover, 4 field hover, 5 marquee offset, 6 hover time
        float[] anim = anims.computeIfAbsent(item, k -> new float[]{0f, item.enabled ? 1f : 0f, 0f, 0f, 0f, 0f, 0f});
        float cx = cardX(i), cy = cardY(i), cw = cardW(), ch = cardH();
        float fy = fieldY(i), fh = fieldH();
        anim[0] = ease(anim[0], live && inside(mx, my, cx, cy, cw, ch) ? 1f : 0f, 16f, dt);
        anim[1] = ease(anim[1], item.enabled ? 1f : 0f, 16f, dt);
        anim[2] = ease(anim[2], live && inside(mx, my, plusX(i), fy, plusW(), fh) ? 1f : 0f, 18f, dt);
        anim[3] = ease(anim[3], live && inside(mx, my, zeroX(i), fy, zeroW(), fh) ? 1f : 0f, 18f, dt);
        anim[4] = ease(anim[4], live && inside(mx, my, fieldX(i), fy, fieldW(i), fh) ? 1f : 0f, 18f, dt);
        boolean editing = editingItem == item;
        float e = anim[1];

        Color top = mix(tint(8), tint(13), anim[0]);
        Color bottom = mix(tint(4), tint(8), anim[0]);
        RenderUtil.GRADIENT_RECT.draw(RenderUtil.matrices(), cx, cy, cw, ch, r(12f),
                a(top, alpha), a(top, alpha), a(bottom, alpha), a(bottom, alpha));
        if (e > 0.01f) glowUp(cx, cy, cw, ch, r(12f), a(accent(24), alpha * e));

        // Иконка предмета
        float slot = slot(), sx = cx + pad(), sy = cy + pad(), tcy = topCY(i);
        rect(sx, sy, slot, slot, r(9f), a(mix(new Color(0, 0, 0, 90), accent(30), e), alpha));
        float itemSize = r(24f);
        itemDraws.add(new ItemDraw(getStack(item), sx + (slot - itemSize) / 2f, sy + (slot - itemSize) / 2f, itemSize, true));

        // Переключатель
        float tx = toggleX(i), tw = toggleW(), th = toggleH(), ty = tcy - th / 2f;
        rect(tx, ty, tw, th, th / 3.2f, a(new Color(48, 46, 41), alpha));
        float k = r(9f), inset = (th - k) / 2f, kx = tx + inset + (tw - k - inset * 2f) * e;
        rect(kx, tcy - k / 2f, k, k, k / 3.2f, a(mix(new Color(238, 236, 230), accent(), e), alpha));

        // Название и рыночная цена
        String name = item.displayName != null && !item.displayName.isBlank() ? item.displayName : item.itemId;
        if (name == null) name = "Предмет";
        float textX = sx + slot + r(12f), maxW = tx - textX - r(10f), nameSize = r(16.5f);
        drawName(name, anim, textX, tcy - r(8f), nameSize, maxW, a(mix(WHITE, accent(), e), alpha), alpha, dt);
        boolean market = item.marketPrice > 0;
        String sub = market ? "Рынок  " + formatPrice(item.marketPrice) + " $" : "Рынок  —";
        text(INTER, ellipsize(INTER, sub, r(13f), maxW), textX, tcy + r(10f), r(13f), a(market ? MARKET : MUTE, alpha));

        // Поле цены скупки
        float fx = fieldX(i), fw = fieldW(i), fcy = fy + fh / 2f;
        Color fieldBg = editing ? accent(40) : mix(new Color(0, 0, 0, 90), new Color(0, 0, 0, 130), anim[4]);
        rect(fx, fy, fw, fh, r(8f), a(fieldBg, alpha));
        String price;
        Color priceColor;
        if (editing) {
            price = (priceBuffer.isEmpty() ? "" : formatPrice(Long.parseLong(priceBuffer)))
                    + ((System.currentTimeMillis() / 450L) % 2 == 0 ? "|" : "");
            priceColor = WHITE;
        } else if (item.maxUnitPrice > 0) {
            price = "до " + formatPrice(item.maxUnitPrice) + " $";
            priceColor = WHITE;
        } else {
            price = "Любая цена";
            priceColor = MUTE;
        }
        text(INTER, ellipsize(INTER, price, r(14f), fw - r(20f)), fx + r(10f), fcy, r(14f), a(priceColor, alpha));

        float px = plusX(i);
        rect(px, fy, plusW(), fh, r(8f), a(mix(tint(7), accent(46), anim[2]), alpha));
        textCenter(INTER, "+50k", px + plusW() / 2f, fcy, r(13.5f), a(mix(GRAY, accent(), anim[2]), alpha));

        float zx = zeroX(i);
        rect(zx, fy, zeroW(), fh, r(8f), a(mix(tint(7), tint(14), anim[3]), alpha));
        textCenter(INTER, "0", zx + zeroW() / 2f, fcy, r(14f), a(mix(GRAY, WHITE, anim[3]), alpha));
    }

    /**
     * Название карточки: длинное обрезается, а при наведении плавно прокручивается бегущей строкой.
     */
    private void drawName(String name, float[] anim, float nx, float cy, float size, float maxW, Color color,
                          float alpha, float dt) {
        float fullW = INTER.getWidth(name, size);
        boolean hovered = anim[0] > 0.5f;
        anim[6] = hovered ? anim[6] + dt : 0f;

        if (fullW <= maxW) {
            anim[5] = 0f;
            drawRich(name, nx, cy, size, color, alpha);
            return;
        }

        float loop = fullW + MARQUEE_GAP * size;
        if (hovered && anim[6] > MARQUEE_DELAY) {
            anim[5] = (anim[5] + r(MARQUEE_SPEED) * dt) % loop;
        } else if (anim[5] > 0f) {
            // уводим текст в ближайшее начало, чтобы не дёргался назад через всю строку
            float target = anim[5] > loop / 2f ? loop : 0f;
            anim[5] = ease(anim[5], target, 12f, dt);
            if (Math.abs(anim[5] - target) < 0.3f) anim[5] = 0f;
        }

        if (anim[5] <= 0.01f) {
            drawRich(ellipsize(INTER, name, size, maxW), nx, cy, size, color, alpha);
            return;
        }

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, nx, cy - size, maxW, size * 2f);
        try {
            drawRich(name, nx - anim[5], cy, size, color, alpha);
            drawRich(name, nx - anim[5] + loop, cy, size, color, alpha);
        } finally {
            ScissorUtil.stop(matrices);
        }
    }

    /** Текст, в котором тир предмета (INFINITY / ETERNITY / STINGER) переливается своим градиентом. */
    private static void drawRich(String value, float tx, float cy, float size, Color color, float alpha) {
        String upper = value.toUpperCase(Locale.ROOT);
        int at = -1;
        Tier tier = null;
        for (Tier t : Tier.values()) {
            int idx = upper.indexOf(t.name());
            if (idx >= 0 && (at < 0 || idx < at)) {
                at = idx;
                tier = t;
            }
        }
        int end = tier == null ? -1 : at + tier.name().length();
        if (tier == null && upper.endsWith("..")) {
            // обрезанное название: "Нагрудник ETER.." — красим хвост, если он начало слова тира
            String body = upper.substring(0, upper.length() - 2);
            for (Tier t : Tier.values()) {
                for (int len = Math.min(t.name().length() - 1, body.length()); len >= 1; len--) {
                    int idx = body.length() - len;
                    if (body.endsWith(t.name().substring(0, len)) && (idx == 0 || body.charAt(idx - 1) == ' ')) {
                        at = idx;
                        tier = t;
                        break;
                    }
                }
                if (tier != null) break;
            }
            end = value.length();
        }
        if (tier == null) {
            text(INTER, value, tx, cy, size, color);
            return;
        }

        String before = value.substring(0, at);
        String word = value.substring(at, end);
        String after = value.substring(at + word.length());
        if (!before.isEmpty()) {
            text(INTER, before, tx, cy, size, color);
            tx += INTER.getWidth(before, size);
        }
        INTER.drawGradientText(RenderUtil.matrices(), word, tx, cy - lift(INTER, size) - size / 2f, size,
                a(tier.bright, alpha), a(tier.dark, alpha), 8f, TIER_PERIOD);
        tx += INTER.getWidth(word, size);
        if (!after.isEmpty()) drawRich(after, tx, cy, size, color, alpha);
    }

    private enum Tier {
        INFINITY(new Color(90, 255, 120), new Color(16, 105, 40)),
        ETERNITY(new Color(205, 120, 255), new Color(78, 24, 140)),
        STINGER(new Color(255, 72, 72), new Color(118, 8, 18));

        final Color bright, dark;

        Tier(Color bright, Color dark) {
            this.bright = bright;
            this.dark = dark;
        }
    }

    private static String formatPrice(long v) {
        if (v <= 0L) return "0";
        StringBuilder sb = new StringBuilder(Long.toString(v));
        for (int i = sb.length() - 3; i > 0; i -= 3) {
            sb.insert(i, ' ');
        }
        return sb.toString();
    }


    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 && button != 1) return false;

        List<Section> sections = visibleSections();
        for (int i = 0; i < sections.size(); i++) {
            if (inside(mx, my, x, sideRowY(i), sideW(), sideRowH())) {
                stopEditing();
                if (section != sections.get(i)) {
                    section = sections.get(i);
                    resetScroll();
                }
                return true;
            }
        }

        float by = barCY() - segH() / 2f;
        for (FilterMode mode : FilterMode.values()) {
            if (inside(mx, my, filterX() + r(3f) + segW() * mode.ordinal(), by, segW(), segH())) {
                stopEditing();
                filter = mode;
                resetScroll();
                return true;
            }
        }
        if (inside(mx, my, enAllX(), by, actionW(), segH())) {
            setAllShown(true);
            return true;
        }
        if (inside(mx, my, disAllX(), by, actionW(), segH())) {
            setAllShown(false);
            return true;
        }

        if (inside(mx, my, mainX(), listY(), mainW(), listH())) {
            for (int i = 0; i < shown.size(); i++) {
                if (!inside(mx, my, cardX(i), cardY(i), cardW(), cardH())) continue;
                ItemEntry entry = shown.get(i);
                float fy = fieldY(i);

                if (inside(mx, my, fieldX(i), fy, fieldW(i), fieldH())) {
                    stopEditing();
                    if (button == 1) {
                        entry.maxUnitPrice = 0L;
                        AutoBuyConfig.save();
                    } else {
                        startEditing(entry);
                    }
                    return true;
                }
                if (inside(mx, my, plusX(i), fy, plusW(), fieldH())) {
                    stopEditing();
                    long step = button == 1 ? -PRICE_STEP : PRICE_STEP;
                    entry.maxUnitPrice = Math.max(0L, entry.maxUnitPrice + step);
                    AutoBuyConfig.save();
                    return true;
                }
                if (inside(mx, my, zeroX(i), fy, zeroW(), fieldH())) {
                    stopEditing();
                    entry.maxUnitPrice = 0L;
                    AutoBuyConfig.save();
                    return true;
                }
                if (my < fy) {
                    stopEditing();
                    entry.enabled = !entry.enabled;
                    AutoBuyConfig.save();
                    return true;
                }
                stopEditing();
                return true;
            }
        }

        stopEditing();
        return false;
    }

    private void setAllShown(boolean enabled) {
        stopEditing();
        for (ItemEntry e : new ArrayList<>(shown)) e.enabled = enabled;
        AutoBuyConfig.save();
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my, mainX(), listY(), mainW() + r(14f), listH())) return false;
        scrollTarget -= (float) amount * r(52f);
        return true;
    }

    public boolean keyPressed(int key) {
        if (editingItem == null) return false;
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!priceBuffer.isEmpty()) {
                priceBuffer = priceBuffer.substring(0, priceBuffer.length() - 1);
                commitPrice();
            }
        } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
            stopEditing();
        }
        return true;
    }

    public boolean charTyped(char chr) {
        if (editingItem == null) return false;
        if (chr >= '0' && chr <= '9') {
            if (priceBuffer.length() < 12) {
                priceBuffer = priceBuffer.isEmpty() && chr == '0' ? "" : priceBuffer + chr;
                commitPrice();
            }
        } else if (chr == 'k' || chr == 'K' || chr == 'к' || chr == 'К') {
            multiplyPrice(1000L);
        } else if (chr == 'm' || chr == 'M' || chr == 'м' || chr == 'М') {
            multiplyPrice(1000000L);
        }
        return true;
    }

    private void startEditing(ItemEntry item) {
        this.editingItem = item;
        this.priceBuffer = item.maxUnitPrice > 0 ? String.valueOf(item.maxUnitPrice) : "";
    }

    private void stopEditing() {
        if (this.editingItem != null) {
            commitPrice();
            this.editingItem = null;
            this.priceBuffer = "";
            AutoBuyConfig.save();
        }
    }

    private void commitPrice() {
        if (this.editingItem != null) {
            try {
                long val = priceBuffer.isEmpty() ? 0L : Long.parseLong(priceBuffer);
                this.editingItem.maxUnitPrice = Math.max(0L, val);
                AutoBuyConfig.save();
            } catch (NumberFormatException ignored) {}
        }
    }

    private void multiplyPrice(long mult) {
        if (this.editingItem != null) {
            long cur = priceBuffer.isEmpty() ? 1L : Long.parseLong(priceBuffer);
            cur = Math.min(100_000_000_000L, cur * mult);
            this.priceBuffer = String.valueOf(cur);
            this.editingItem.maxUnitPrice = cur;
            AutoBuyConfig.save();
        }
    }
}
