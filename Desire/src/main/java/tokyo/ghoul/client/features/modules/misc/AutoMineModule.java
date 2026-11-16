package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.features.modules.misc.automine.AutoMineWorker;
import tokyo.ghoul.client.features.modules.misc.automine.BotAutoMine;
import tokyo.ghoul.client.services.AnarchyReconnectService;

@ModuleRegister(name = "AutoMine", description = "Автоматическая добыча блоков", category = Category.OTHER)
public class AutoMineModule extends Module {
    @Getter private static final AutoMineModule instance = new AutoMineModule();

    public final SliderSetting firstAnarchy = new SliderSetting("Первая анархия").range(1, 60).step(1).value(1f);
    public final SliderSetting lastAnarchy = new SliderSetting("Последняя анархия").range(1, 60).step(1).value(12f);
    public final BooleanSetting loop = new BooleanSetting("По кругу").value(true);
    public final SliderSetting rotationSpeed = new SliderSetting("Скорость ротации").range(5, 60).step(1).value(25f);

    private AutoMineWorker worker;

    public AutoMineModule() {
        addSettings(firstAnarchy, lastAnarchy, loop, rotationSpeed);
    }

    public static void frame() {
        BotSessionManager manager = BotSessionManager.getInstance();
        BotSession foreground = manager.getActiveSession();
        AutoMineWorker active = foreground == manager.getMainSession()
                ? instance.worker
                : BotAutoMine.getInstance().worker(foreground.getName());
        if (active != null) active.frame();
    }

    public int firstAnarchy() {
        return Math.round(firstAnarchy.getValue());
    }

    public int lastAnarchy() {
        return Math.max(firstAnarchy(), Math.round(lastAnarchy.getValue()));
    }

    @Override
    public void onEnable() {
        if (mc.player == null) {
            setEnabled(false);
            return;
        }
        worker = new AutoMineWorker(AnarchyReconnectService.getInstance(), false);
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(tick);
    }

    @Override
    public void onDisable() {
        if (worker != null) {
            worker.stop();
            flush();
            worker = null;
        }
        super.onDisable();
    }

    private void onTick() {
        if (worker == null) return;
        BotSessionManager manager = BotSessionManager.getInstance();
        if (!manager.isForeground(manager.getMainSession())) return;
        worker.tick();
        flush();
        if (worker.isFinished()) setEnabled(false);
    }

    private void flush() {
        for (String message : worker.drainMessages()) print("[AutoMine] " + message);
    }
}
