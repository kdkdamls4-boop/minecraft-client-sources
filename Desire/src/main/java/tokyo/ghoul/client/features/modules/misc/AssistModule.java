package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import tokyo.ghoul.api.module.setting.Setting;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.utils.player.InventoryUtil;
import tokyo.ghoul.client.features.modules.autobuy.engine.ServerHandler;
import tokyo.ghoul.client.services.AnarchyReconnectService;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

@ModuleRegister(name = "Assist", description = "Вспомогательные функции для игры", category = Category.OTHER)
public class AssistModule extends Module {
    @Getter private static final AssistModule instance = new AssistModule();

    private static final long SCROLL_INTERVAL_MS = 50L;
    private static final List<Item> BACKPACKS = List.of(Items.DYED_SHULKER_BOX.magenta(), Items.DYED_SHULKER_BOX.purple(),
            Items.DYED_SHULKER_BOX.red(), Items.DYED_SHULKER_BOX.pink(), Items.DYED_SHULKER_BOX.blue());

    private enum Use {
        PRESS,
        RELEASE,
        HOLD,
        WIND,
        BACKPACK
    }

    private enum Group {
        ALL, FT, HW
    }

    private record EventItem(String name, BindSetting bind, Predicate<ItemStack> match, Group group, Use use) {
    }

    private final ModeSetting mode = new ModeSetting("Режим").values("Авто", "FunTime", "HolyWorld").value("Авто");
    private final BooleanSetting autoRct = new BooleanSetting("Авто .rct после чарки (HW)").value(false);
    private final List<EventItem> items = new ArrayList<>();

    private final Set<Integer> waitingRelease = new HashSet<>();
    private EventItem holding;
    private long lastHoldUse;

    public AssistModule() {
        add("Трапка", this::isTrapka, Group.ALL, Use.RELEASE);
        add("Ком снега", item(Items.SNOWBALL), Group.ALL, Use.RELEASE);
        add("Заряд ветра", item(Items.WIND_CHARGE), Group.ALL, Use.WIND);
        add("Рюкзак", stack -> BACKPACKS.contains(stack.getItem()), Group.ALL, Use.BACKPACK);

        add("Дезориентация", item(Items.ENDER_EYE), Group.FT, Use.RELEASE);
        add("Божья аура", item(Items.PHANTOM_MEMBRANE), Group.FT, Use.RELEASE);
        add("Огненный смерч", item(Items.FIRE_CHARGE), Group.FT, Use.PRESS);
        add("Пласт", item(Items.DRIED_KELP), Group.FT, Use.RELEASE);
        add("Явная пыль", item(Items.SUGAR), Group.FT, Use.RELEASE);
        add("Хлопушка", splash("Хлопушка"), Group.FT, Use.PRESS);
        add("Зелье радиации", splash("Зелье Радиации"), Group.FT, Use.PRESS);
        add("Снотворное", splash("Снотворное"), Group.FT, Use.PRESS);
        add("Святая вода", splash("Святая вода"), Group.FT, Use.PRESS);
        add("Зелье гнева", splash("Зелье гнева"), Group.FT, Use.PRESS);
        add("Зелье паладина", splash("Зелье паладина"), Group.FT, Use.PRESS);
        add("Зелье ассасина", splash("Зелье ассасина"), Group.FT, Use.PRESS);

        add("Стан", item(Items.NETHER_STAR), Group.HW, Use.RELEASE);
        add("Взрывная штучка", item(Items.FIRE_CHARGE), Group.HW, Use.PRESS);
        add("Взрывная трапка", item(Items.PRISMARINE_SHARD), Group.HW, Use.RELEASE);
        add("Ловушка", item(Items.HEART_OF_THE_SEA), Group.HW, Use.PRESS);
        add("Анти полет", named(Items.FIREWORK_STAR, "анти", "полет"), Group.HW, Use.PRESS);
        add("Тёмный пульс", named(Items.FIREWORK_STAR, "темн", "пульс"), Group.HW, Use.PRESS);
        add("Свиток опыта", named(Items.FLOWER_BANNER_PATTERN, "свит", "опыт"), Group.HW, Use.HOLD);

        autoRct.setVisible(() -> activeMode() == Group.HW);

        List<Setting<?>> settings = new ArrayList<>();
        settings.add(mode);
        settings.add(autoRct);
        for (EventItem item : items) settings.add(item.bind());
        addSettings(settings.toArray(new Setting[0]));
    }

    private void add(String name, Predicate<ItemStack> match, Group group, Use use) {
        BindSetting bind = new BindSetting(name).value(-999);
        bind.setVisible(() -> inMode(group));
        items.add(new EventItem(name, bind, match, group, use));
    }

    private static Group activeMode() {
        AssistModule module = instance;
        if (module.mode.is("FunTime")) return Group.FT;
        if (module.mode.is("HolyWorld")) return Group.HW;
        return addressHas("holyworld", "playhw", "hollyworld", "holy", "holly") || ServerHandler.getInstance().isHolyWorld()
                ? Group.HW : Group.FT;
    }

    private static boolean inMode(Group group) {
        return group == Group.ALL || group == activeMode();
    }

    @Override
    public void onEvent() {
        EventListener key = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != GLFW.GLFW_PRESS || mc.player == null || mc.gui.screen() != null) return;
            onBindPressed(event.mouse() ? -100 + event.key() : event.key());
        }));
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(key, tick);
    }

    @Override
    public void onDisable() {
        waitingRelease.clear();
        holding = null;
        super.onDisable();
    }


    private void onBindPressed(int key) {
        for (EventItem item : items) {
            Integer bind = item.bind().getValue();
            if (bind == null || bind == -999 || bind != key || !inMode(item.group())) continue;
            switch (item.use()) {
                case RELEASE -> waitingRelease.add(key);
                case HOLD -> {
                    holding = item;
                    lastHoldUse = System.currentTimeMillis();
                    use(item);
                }
                default -> use(item);
            }
            return;
        }
    }

    private void onTick() {
        if (mc.player == null || mc.gameMode == null) return;

        if (!waitingRelease.isEmpty()) {
            for (Integer key : new ArrayList<>(waitingRelease)) {
                if (KeyStorage.isPressed(key)) continue;
                waitingRelease.remove(key);
                if (mc.gui.screen() != null) continue;
                for (EventItem item : items) {
                    Integer bind = item.bind().getValue();
                    if (item.use() == Use.RELEASE && bind != null && bind.equals(key) && inMode(item.group())) {
                        use(item);
                        break;
                    }
                }
            }
        }

        if (holding != null) {
            Integer bind = holding.bind().getValue();
            if (bind == null || !KeyStorage.isPressed(bind) || mc.gui.screen() != null) {
                holding = null;
            } else if (System.currentTimeMillis() - lastHoldUse >= SCROLL_INTERVAL_MS) {
                lastHoldUse = System.currentTimeMillis();
                use(holding);
            }
        }
    }

    private void use(EventItem item) {
        boolean used;
        if (item.use() == Use.WIND) {
            float pitch = mc.player.getXRot();
            mc.player.setXRot(90f);
            used = InventoryUtil.swapAndUseHvH(item.match());
            mc.player.setXRot(pitch);
        } else {
            used = InventoryUtil.swapAndUseHvH(item.match());
        }
        if (!used && item.use() != Use.HOLD) {
            NotifyWidget.text("Нет предмета: " + item.name().toLowerCase(Locale.ROOT));
        }
    }


    public static void onFinishUsing(ItemStack stack) {
        AssistModule module = instance;
        if (!module.isEnabled() || !module.autoRct.getValue() || stack == null) return;
        if (!stack.is(Items.ENCHANTED_GOLDEN_APPLE) || !addressHas("holy", "holly", "playhw")) return;
        if (ServerHandler.getInstance().isPvp()) return;
        mc.execute(() -> AnarchyReconnectService.getInstance().start(AnarchyReconnectService.Target.AUTO, null));
    }


    private boolean isTrapka(ItemStack stack) {
        boolean chorus = activeMode() == Group.HW && !addressHas("holytime");
        return stack.is(chorus ? Items.POPPED_CHORUS_FRUIT : Items.NETHERITE_SCRAP);
    }

    private static Predicate<ItemStack> item(Item item) {
        return stack -> stack.is(item);
    }

    private static Predicate<ItemStack> named(Item item, String... words) {
        return stack -> {
            if (!stack.is(item)) return false;
            String name = normalize(stack.getHoverName().getString());
            for (String word : words) {
                if (!name.contains(normalize(word))) return false;
            }
            return true;
        };
    }

    private static Predicate<ItemStack> splash(String title) {
        return stack -> stack.is(Items.SPLASH_POTION) && stack.getHoverName().getString().contains(title);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    private static boolean addressHas(String... keys) {
        ServerData server = mc.getCurrentServer();
        if ((server == null || server.ip == null) && mc.getConnection() != null) server = mc.getConnection().getServerData();
        String address = server != null && server.ip != null ? server.ip.toLowerCase(Locale.ROOT) : "";
        for (String key : keys) {
            if (address.contains(key)) return true;
        }
        return false;
    }
}
