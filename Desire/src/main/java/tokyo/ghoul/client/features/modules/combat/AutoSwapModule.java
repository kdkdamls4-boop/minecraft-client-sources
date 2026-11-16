package tokyo.ghoul.client.features.modules.combat;

import com.mojang.authlib.properties.Property;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.AbstractSkullBlock;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BindSetting;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.utils.inventory.InventoryActionManager;
import tokyo.ghoul.api.utils.inventory.InventoryActionSequence;
import tokyo.ghoul.api.utils.inventory.InventoryActions;
import tokyo.ghoul.client.features.modules.combat.autoswap.AutoSwapWheelScreen;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

@ModuleRegister(name = "Auto Swap", description = "Быстрая смена предметов по бинду", category = Category.COMBAT)
public class AutoSwapModule extends Module {
    @Getter private static final AutoSwapModule instance = new AutoSwapModule();

    private static final String SWAP_OFFHAND = "Свап в левой руке";
    private static final String SWAP_HEAD = "Свап на голову";
    private static final String MODE_TWO = "2 предмета";
    private static final String MODE_THREE = "3 предмета";
    private static final String ITEM_SHIELD = "Shield";
    private static final String ITEM_HEAD = "Shar";
    private static final String ITEM_GAPPLE = "GGaple";
    private static final String ITEM_TOTEM = "Totem";

    private static final int WHEEL_SLOTS = 3;
    private static final int HEAD_MENU_SLOT = 5;
    private static final int UNBOUND = -999;

    private final MultiBooleanSetting swaps = new MultiBooleanSetting("Свапы").value(
            new BooleanSetting(SWAP_OFFHAND).value(true),
            new BooleanSetting(SWAP_HEAD).value(false)
    );
    private final ModeSetting swapMode = new ModeSetting("Мод свапа").value(MODE_TWO).values(MODE_TWO, MODE_THREE)
            .setVisible(this::isOffhandEnabled);
    private final BindSetting offhandKey = new BindSetting("Кнопка левой руки").value(UNBOUND)
            .setVisible(this::isOffhandEnabled);
    private final ModeSetting firstItem = new ModeSetting("1 предмет").value(ITEM_HEAD)
            .values(ITEM_SHIELD, ITEM_HEAD, ITEM_GAPPLE, ITEM_TOTEM)
            .setVisible(this::isTwoItemsMode);
    private final ModeSetting secondItem = new ModeSetting("2 предмет").value(ITEM_TOTEM)
            .values(ITEM_SHIELD, ITEM_HEAD, ITEM_GAPPLE, ITEM_TOTEM)
            .setVisible(this::isTwoItemsMode);
    private final BindSetting headKey = new BindSetting("Кнопка свапа на голову").value(UNBOUND)
            .setVisible(this::isHeadEnabled);
    private final BindSetting rememberHeadKey = new BindSetting("Запомнить голову").value(UNBOUND)
            .setVisible(this::isHeadEnabled);
    // выбранная голова: "id|skin|name", редактируется биндом "Запомнить голову"
    private final TextSetting headFilter = new TextSetting("Голова").setVisible(() -> false);

    private final List<ItemStack> wheelSlots = new ArrayList<>();
    private AutoSwapWheelScreen wheelScreen;
    private int pickingIndex = -1;
    private InventoryActionSequence sequence;
    private ItemStack previousHead = ItemStack.EMPTY;
    private HeadFilter cachedFilter;
    private String cachedFilterRaw;

    public AutoSwapModule() {
        addSettings(swaps, swapMode, offhandKey, firstItem, secondItem, headKey, rememberHeadKey, headFilter);
        for (int i = 0; i < WHEEL_SLOTS; i++) {
            wheelSlots.add(ItemStack.EMPTY);
        }
    }

    @Override
    public void onEvent() {
        EventListener key = KeyEvent.getInstance().subscribe(new Listener<>(this::onKey));
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> {
            // инвентарь закрыли, ничего не выбрав
            if (pickingIndex >= 0 && !(mc.gui.screen() instanceof InventoryScreen)) {
                pickingIndex = -1;
            }
            // ванильный свап рук на том же бинде дал бы двойной свап
            if (conflictsWithVanillaSwap()) {
                while (mc.options.keySwapOffhand.consumeClick()) {
                }
            }
        }));
        addEvents(key, tick);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (mc.gui.screen() instanceof AutoSwapWheelScreen) {
            mc.gui.setScreen(null);
        }
        wheelScreen = null;
        pickingIndex = -1;
    }

    private void onKey(KeyEvent.KeyEventData event) {
        if (mc.player == null) return;

        int code = event.mouse() ? -100 + event.key() : event.key();
        if (code == UNBOUND) return;

        boolean head = isHeadEnabled() && code == headKey.getValue();
        boolean offhand = isOffhandEnabled() && code == offhandKey.getValue();
        boolean wheel = offhand && swapMode.is(MODE_THREE);

        if (event.action() == GLFW.GLFW_PRESS) {
            if (mc.gui.screen() != null) return;

            if (isHeadEnabled() && code == rememberHeadKey.getValue()) {
                rememberHead();
                return;
            }
            if (wheel) {
                openWheel();
            }
            handleSwap(head, offhand && !wheel);
        } else if (event.action() == GLFW.GLFW_RELEASE && wheel) {
            closeWheelAndSwap();
        }
    }

    private boolean conflictsWithVanillaSwap() {
        try {
            InputConstants.Key key = InputConstants.getKey(mc.options.keySwapOffhand.saveString());
            int code = key.getType() == InputConstants.Type.MOUSE ? -100 + key.getValue() : key.getValue();
            return isOffhandEnabled() && code == offhandKey.getValue() || isHeadEnabled() && code == headKey.getValue();
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isOffhandEnabled() {
        return swaps.isEnabled(SWAP_OFFHAND);
    }

    private boolean isHeadEnabled() {
        return swaps.isEnabled(SWAP_HEAD);
    }

    private boolean isTwoItemsMode() {
        return isOffhandEnabled() && swapMode.is(MODE_TWO);
    }

    private boolean isInventoryMenu() {
        return mc.player.containerMenu == mc.player.inventoryMenu;
    }

    private boolean isBusy() {
        return sequence != null && !sequence.isCancelled() && InventoryActionManager.INSTANCE.isBusy();
    }

    private boolean blockedByAutoTotem() {
        if (!AutoTotemModule.isBusyStatic()) return false;
        NotifyWidget.text("Авто тотем занимает вторую руку, свап невозможен");
        return true;
    }

    private void handleSwap(boolean head, boolean offhand) {
        if (mc.player == null || !head && !offhand || isBusy()) return;

        if (head && getHeadFilter() == null) {
            NotifyWidget.text("Голова для свапа не выбрана");
        }

        InventoryActionSequence next = null;
        if (head && canHeadSwap()) {
            next = InventoryActionManager.INSTANCE.enqueue(this::swapHead, 1, true);
        }
        if (offhand && pickTwoItems() != null && !blockedByAutoTotem()) {
            next = next == null
                    ? InventoryActionManager.INSTANCE.enqueue(this::swapTwoItems, 1, isInventoryMenu())
                    : next.nextTick().click(this::swapTwoItems, 1, isInventoryMenu());
        }
        if (next != null) {
            sequence = next.verify();
        }
    }

    private void swapTwoItems() {
        if (mc.player != null && !AutoTotemModule.isBusyStatic()) {
            swapToOffhand(pickTwoItems());
        }
    }

    private void swapToOffhand(Slot slot) {
        if (slot != null) {
            InventoryActions.swapToOffhand(slot.index);
        }
    }

    // первый предмет, а если он уже в руке — второй
    private Slot pickTwoItems() {
        Slot first = bestSlot(stack -> itemScore(firstItem.getValue(), stack));
        Slot second = bestSlot(stack -> itemScore(secondItem.getValue(), stack));
        if (first == null) return second;
        if (second == null) return first;
        return itemScore(firstItem.getValue(), mc.player.getOffhandItem()) > 0 ? second : first;
    }

    private int itemScore(String type, ItemStack stack) {
        Item item = itemByType(type);
        if (item == Items.AIR || stack.getItem() != item) return 0;
        return InventoryActions.isValuable(stack) ? 2 : 1;
    }

    private Item itemByType(String type) {
        return switch (type) {
            case ITEM_SHIELD -> Items.SHIELD;
            case ITEM_TOTEM -> Items.TOTEM_OF_UNDYING;
            case ITEM_GAPPLE -> Items.GOLDEN_APPLE;
            case ITEM_HEAD -> Items.PLAYER_HEAD;
            default -> Items.AIR;
        };
    }

    // лучший по счёту слот инвентаря; при равенстве — хотбар
    private Slot bestSlot(ToIntFunction<ItemStack> scorer) {
        Slot best = null;
        int bestScore = 0;
        for (Slot slot : mc.player.containerMenu.slots) {
            if (slot.container != mc.player.getInventory() || slot.getContainerSlot() >= 36) continue;
            int score = scorer.applyAsInt(slot.getItem());
            if (score > bestScore || score > 0 && score == bestScore && slot.getContainerSlot() < 9) {
                best = slot;
                bestScore = score;
            }
        }
        return best;
    }

    private Slot findExact(ItemStack target) {
        if (target.isEmpty() || ItemStack.isSameItemSameComponents(mc.player.getOffhandItem(), target)) return null;
        return bestSlot(stack -> ItemStack.isSameItemSameComponents(stack, target) ? 1 : 0);
    }

    // ---- колесо на 3 предмета ----

    public int getWheelSlotCount() {
        return wheelSlots.size();
    }

    public ItemStack getWheelSlotStack(int index) {
        if (index < 0 || index >= wheelSlots.size()) return ItemStack.EMPTY;
        ItemStack stack = wheelSlots.get(index);
        return stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public void clearWheelSlot(int index) {
        if (index >= 0 && index < wheelSlots.size()) {
            wheelSlots.set(index, ItemStack.EMPTY);
        }
    }

    /** Пустой сектор: открыть ванильный инвентарь, кликнутый там предмет попадёт в этот сектор. */
    public void startPicking(int index) {
        if (mc.player == null || index < 0 || index >= wheelSlots.size()) return;
        pickingIndex = index;
        wheelScreen = null;
        mc.gui.setScreen(new InventoryScreen(mc.player));
    }

    /** Вызывается из AbstractContainerScreen.mouseClicked; true — клик съеден выбором предмета. */
    public boolean onPickerClick(Slot slot) {
        if (pickingIndex < 0 || !(mc.gui.screen() instanceof InventoryScreen)) return false;
        if (slot == null || mc.player == null || slot.container != mc.player.getInventory() || slot.getItem().isEmpty()) {
            return true;
        }

        wheelSlots.set(pickingIndex, slot.getItem().copy());
        pickingIndex = -1;
        openWheel();
        return true;
    }

    /** ЛКМ по заполненному сектору — свап сразу. */
    public void swapWheelNow(int index) {
        mc.gui.setScreen(null);
        wheelScreen = null;
        swapWheel(index);
    }

    private void openWheel() {
        if (mc.player == null) return;
        if (wheelScreen == null) {
            wheelScreen = new AutoSwapWheelScreen(this);
        }
        if (mc.gui.screen() != wheelScreen) {
            mc.gui.setScreen(wheelScreen);
        }
    }

    public void onWheelClosed(AutoSwapWheelScreen screen) {
        if (wheelScreen == screen) {
            wheelScreen = null;
        }
    }

    private void closeWheelAndSwap() {
        if (mc.gui.screen() instanceof AutoSwapWheelScreen screen) {
            int selected = screen.getSelectedIndex();
            mc.gui.setScreen(null);
            wheelScreen = null;
            swapWheel(selected);
        }
    }

    private void swapWheel(int index) {
        if (mc.player == null || index < 0 || index >= wheelSlots.size()) return;
        ItemStack target = wheelSlots.get(index);
        if (findExact(target) != null && !blockedByAutoTotem()) {
            sequence = InventoryActionManager.INSTANCE.enqueue(() -> {
                if (mc.player != null && !AutoTotemModule.isBusyStatic()) {
                    swapToOffhand(findExact(target));
                }
            }, 1, isInventoryMenu()).verify();
        }
    }

    // ---- свап головы ----

    private boolean isHeadEquipped() {
        HeadFilter filter = getHeadFilter();
        return filter != null && filter.score(mc.player.inventoryMenu.getSlot(HEAD_MENU_SLOT).getItem()) > 0;
    }

    private boolean canHeadSwap() {
        if (getHeadFilter() == null || mc.player.containerMenu != mc.player.inventoryMenu) return false;
        if (!isHeadEquipped()) {
            return bestSlot(stack -> getHeadFilter().score(stack)) != null;
        }
        return previousHeadSlot() != -1 || InventoryActions.hasFreeInventorySlot();
    }

    // надета выбранная голова — вернуть прежний шлем (или снять в инвентарь), иначе надеть голову
    private void swapHead() {
        if (mc.player == null || !canHeadSwap()) return;

        if (isHeadEquipped()) {
            int slot = previousHeadSlot();
            if (slot != -1) {
                InventoryActions.moveSlot(slot, HEAD_MENU_SLOT);
            } else {
                InventoryActions.click(HEAD_MENU_SLOT, 0, ContainerInput.QUICK_MOVE);
            }
        } else {
            Slot slot = bestSlot(stack -> getHeadFilter().score(stack));
            previousHead = mc.player.inventoryMenu.getSlot(HEAD_MENU_SLOT).getItem().copy();
            InventoryActions.moveSlot(slot.index, HEAD_MENU_SLOT);
        }
    }

    private int previousHeadSlot() {
        if (!previousHead.isEmpty()) {
            Slot slot = bestSlot(stack -> ItemStack.isSameItemSameComponents(stack, previousHead) ? 1 : 0);
            if (slot != null) return slot.index;
        }
        return bestHelmetSlot();
    }

    private void rememberHead() {
        ItemStack stack = mc.player.getMainHandItem();
        if (!isSkull(stack)) {
            stack = mc.player.inventoryMenu.getSlot(HEAD_MENU_SLOT).getItem();
        }
        if (!isSkull(stack)) {
            NotifyWidget.text("Возьми голову в руку, чтобы запомнить её");
            return;
        }

        HeadFilter filter = HeadFilter.of(stack);
        headFilter.setValue(filter.serialize());
        NotifyWidget.text("Голова для свапа: " + (filter.name().isEmpty() ? filter.itemId() : filter.name()));
    }

    private HeadFilter getHeadFilter() {
        String raw = headFilter.getValue();
        if (!raw.equals(cachedFilterRaw)) {
            cachedFilterRaw = raw;
            cachedFilter = HeadFilter.parse(raw);
        }
        return cachedFilter;
    }

    private static boolean isSkull(ItemStack stack) {
        return stack.getItem() instanceof BlockItem block && block.getBlock() instanceof AbstractSkullBlock;
    }

    private static String skinOf(ItemStack stack) {
        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile() == null) return null;

        Iterator<Property> textures = profile.partialProfile().properties().get("textures").iterator();
        if (!textures.hasNext()) return null;

        try {
            String json = new String(Base64.getDecoder().decode(textures.next().value()), StandardCharsets.UTF_8);
            int key = json.indexOf("\"url\"");
            if (key < 0) return null;
            int open = json.indexOf('"', key + 5);
            if (open < 0) return null;
            int close = json.indexOf('"', open + 1);
            if (close < 0) return null;
            String url = json.substring(open + 1, close);
            int slash = url.lastIndexOf('/');
            return slash + 1 < url.length() ? url.substring(slash + 1) : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String nameOf(ItemStack stack) {
        String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
        return name == null ? "" : name.trim();
    }

    // ---- лучший шлем (как ArmorScoreUtils) ----

    private int bestHelmetSlot() {
        if (mc.player == null) return -1;

        ItemStack current = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack best = current;
        double bestScore = armorScore(current);
        int bestSlot = -1;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.containerMenu.getSlot(i).getItem();
            if (!isHelmet(stack)) continue;

            double score = armorScore(stack);
            if (score > 1.0E-4 && isBetterArmor(stack, best, score, bestScore)) {
                best = stack;
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private static boolean isHelmet(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.HEAD;
    }

    private static boolean isBetterArmor(ItemStack candidate, ItemStack current, double candidateScore, double currentScore) {
        if (candidateScore > currentScore + 1.0E-4) return true;
        if (candidateScore + 1.0E-4 < currentScore) return false;
        return durability(candidate) > durability(current);
    }

    private static float durability(ItemStack stack) {
        if (stack.isEmpty()) return 0.0F;
        int max = stack.getMaxDamage();
        return max <= 0 ? 1.0F : (float) (max - stack.getDamageValue()) / max;
    }

    private static double armorScore(ItemStack stack) {
        if (stack.isEmpty()) return 0.0;

        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        double score = modifiers.compute(Attributes.ARMOR, 0.0, EquipmentSlot.HEAD)
                + modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0, EquipmentSlot.HEAD) * 0.5;
        if (score <= 0.0) return 0.0;

        for (Map.Entry<Holder<Enchantment>, Integer> entry : stack.getEnchantments().entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            int level = entry.getValue();
            if (enchantment.is(Enchantments.PROTECTION)) {
                score += level;
            } else if (enchantment.is(Enchantments.PROJECTILE_PROTECTION) || enchantment.is(Enchantments.BLAST_PROTECTION)
                    || enchantment.is(Enchantments.FIRE_PROTECTION)) {
                score += level * 0.25;
            } else if (enchantment.is(Enchantments.UNBREAKING)) {
                score += level * 0.05;
            }
        }
        return score;
    }

    /** Фильтр головы: предмет + текстура скина + имя. Счёт 3 — всё совпало, 0 — не та голова. */
    private record HeadFilter(Item item, String skin, String name) {
        int score(ItemStack stack) {
            if (stack.isEmpty() || stack.getItem() != item) return 0;
            if (skin != null) {
                String stackSkin = skinOf(stack);
                if (stackSkin != null) {
                    if (!skin.equals(stackSkin)) return 0;
                    return name.equals(nameOf(stack)) ? 3 : 2;
                }
            }
            return name.equals(nameOf(stack)) ? 1 : 0;
        }

        String itemId() {
            return BuiltInRegistries.ITEM.getKey(item).toString();
        }

        String serialize() {
            return itemId() + "|" + (skin == null ? "" : skin) + "|" + name;
        }

        static HeadFilter of(ItemStack stack) {
            return new HeadFilter(stack.getItem(), skinOf(stack), nameOf(stack));
        }

        static HeadFilter parse(String raw) {
            if (raw == null || raw.isBlank()) return null;
            String[] parts = raw.split("\\|", 3);
            Identifier id = Identifier.tryParse(parts[0]);
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
            if (item == Items.AIR) return null;
            String skin = parts.length > 1 && !parts[1].isBlank() ? parts[1] : null;
            String name = parts.length > 2 ? parts[2] : "";
            return new HeadFilter(item, skin, name);
        }
    }
}
