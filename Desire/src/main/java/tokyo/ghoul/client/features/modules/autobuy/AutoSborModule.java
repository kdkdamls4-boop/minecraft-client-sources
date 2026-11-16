package tokyo.ghoul.client.features.modules.autobuy;

import lombok.Getter;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.utils.other.TextUtil;
import tokyo.ghoul.client.features.modules.autobuy.engine.AutoBuyParsedItemData;
import tokyo.ghoul.client.features.modules.autobuy.engine.AutoBuyParsedItems;
import tokyo.ghoul.client.features.modules.autobuy.engine.ServerHandler;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


@ModuleRegister(name = "AutoSbor", description = "Автоматический сбор ресурсов", category = Category.AUTOBUY)
public class AutoSborModule extends Module {
    @Getter private static final AutoSborModule instance = new AutoSborModule();

    private static final String[] NICK_PREFIXES = {
            "Player", "Gamer", "Winner", "Lucky", "Shadow", "Night", "Cyber", "Mega", "Swift", "Ninja"
    };

    public final TextSetting botNick = new TextSetting("Ник бота").value("");
    public final RunSetting genNick = new RunSetting("Сгенерировать ник").value(this::generateRandomNick);
    public final SliderSetting armorDurability = new SliderSetting("Прочность брони").range(0, 100).step(1).value(80f);
    public final SliderSetting priceMultiplier = new SliderSetting("Множитель цены").range(0, 100).step(1).value(100f);
    public final BooleanSetting autoConfig = new BooleanSetting("Авто-настройка").value(false);
    public final SliderSetting interval = new SliderSetting("Интервал (мин)")
            .range(1, 360).step(1).value(30f)
            .setVisible(autoConfig::getValue);

    private BotAutoSetupState state = BotAutoSetupState.INACTIVE;
    private long nextRunAtMs;
    private long stateDeadlineAtMs;

    public AutoSborModule() {
        addSettings(botNick, genNick, armorDurability, priceMultiplier, autoConfig, interval);
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(e -> onTick()));
        addEvents(tick);
    }

    @Override
    public void onEnable() {
        if (!ServerHandler.getInstance().isHolyWorld()) {
            TextUtil.sendMessage("§c[AutoSbor]§r Авто-настройка работает только на HolyWorld!");
            setEnabled(false);
            return;
        }

        if (botNick.getText().isBlank()) {
            generateRandomNick();
        }

        state = BotAutoSetupState.IDLE;
        TextUtil.sendMessage("§d[AutoSbor]§r Запуск авто-настройки с ботом §b" + botNick.getText());
        BotSessionManager.getInstance().setBackgroundAutoSetupBotName(botNick.getText());
    }

    @Override
    public void onDisable() {
        state = BotAutoSetupState.INACTIVE;
        BotSessionManager.getInstance().setBackgroundAutoSetupBotName(null);
        TextUtil.sendMessage("§d[AutoSbor]§r Авто-настройка остановлена.");
    }

    private void onTick() {
        if (!isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        BotSessionManager manager = BotSessionManager.getInstance();

        switch (state) {
            case IDLE -> {
                String nick = botNick.getText().trim();
                if (nick.isEmpty()) {
                    generateRandomNick();
                    return;
                }

                List<AutoBuyParsedItemData> items = AutoBuyParsedItems.loadItems().stream()
                        .filter(AutoBuyParsedItemData::enabled)
                        .toList();

                if (items.isEmpty()) {
                    TextUtil.sendMessage("§c[AutoSbor]§r Нет включённых предметов для настройки.");
                    setEnabled(false);
                    return;
                }

                state = BotAutoSetupState.WAIT_BOT_CONNECTED;
                stateDeadlineAtMs = now + 25000L;
                manager.setBackgroundAutoSetupBotName(nick);
                TextUtil.sendMessage("§d[AutoSbor]§r Подключаю бота §b" + nick + "§r...");
            }

            case WAIT_BOT_CONNECTED -> {
                String nick = botNick.getText().trim();
                BotSession bot = manager.getBot(nick);
                if (bot != null && bot.isActive()) {
                    state = BotAutoSetupState.RUNNING;
                    stateDeadlineAtMs = now + 60000L;
                    TextUtil.sendMessage("§a[AutoSbor]§r Бот подключён! Сканирую аукцион...");
                } else if (now >= stateDeadlineAtMs) {
                    TextUtil.sendMessage("§c[AutoSbor]§r Таймаут ожидания подключения бота.");
                    if (autoConfig.getValue()) {
                        scheduleNextCycle(now);
                    } else {
                        setEnabled(false);
                    }
                }
            }

            case RUNNING -> {
                if (now >= stateDeadlineAtMs) {
                    TextUtil.sendMessage("§a[AutoSbor]§r Цены успешно настроены и сохранены.");
                    if (autoConfig.getValue()) {
                        scheduleNextCycle(now);
                    } else {
                        setEnabled(false);
                    }
                }
            }

            case WAITING_NEXT_CYCLE -> {
                if (now >= nextRunAtMs) {
                    state = BotAutoSetupState.IDLE;
                }
            }

            default -> {}
        }
    }

    private void scheduleNextCycle(long now) {
        state = BotAutoSetupState.WAITING_NEXT_CYCLE;
        long delayMs = Math.round(interval.getValue() * 60_000L);
        nextRunAtMs = now + delayMs;
        TextUtil.sendMessage("§7[AutoSbor] Следующий запуск через " + Math.round(interval.getValue()) + " мин.");
    }

    private void generateRandomNick() {
        String prefix = NICK_PREFIXES[ThreadLocalRandom.current().nextInt(NICK_PREFIXES.length)];
        int number = ThreadLocalRandom.current().nextInt(100, 9999);
        String generated = prefix + "_" + number;
        botNick.setText(generated);
        TextUtil.sendMessage("§d[AutoSbor]§r Сгенерирован ник: §b" + generated);
    }

    private enum BotAutoSetupState {
        INACTIVE,
        IDLE,
        WAIT_BOT_CONNECTED,
        RUNNING,
        WAITING_NEXT_CYCLE
    }
}
