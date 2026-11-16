package tokyo.ghoul.client.features.modules.misc.automine;

import lombok.Getter;
import tokyo.ghoul.api.utils.other.TextUtil;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.features.modules.misc.BotsModule;
import tokyo.ghoul.client.services.AnarchyReconnectService;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BotAutoMine {
    @Getter private static final BotAutoMine instance = new BotAutoMine();

    private final Map<String, AutoMineWorker> workers = new HashMap<>();

    private BotAutoMine() {
    }

    public void tick() {
        BotSessionManager manager = BotSessionManager.getInstance();
        if (!BotsModule.getInstance().botAutoMine.getValue()) {
            if (!workers.isEmpty()) stopAll(manager);
            return;
        }

        workers.keySet().removeIf(name -> manager.getBot(name) == null);

        for (BotSession session : manager.getAllBots()) {
            if (!session.isPlayable()) continue;
            AutoMineWorker worker = workers.computeIfAbsent(session.getName(),
                    name -> new AutoMineWorker(AnarchyReconnectService.detached(), true));
            if (worker.isFinished()) continue;
            manager.runInContext(session, worker::tick);
            flush(session.getName(), worker);
        }
    }

    public AutoMineWorker worker(String name) {
        if (!BotsModule.getInstance().botAutoMine.getValue()) return null;
        AutoMineWorker worker = workers.get(name);
        return worker != null && !worker.isFinished() ? worker : null;
    }

    private void stopAll(BotSessionManager manager) {
        Iterator<Map.Entry<String, AutoMineWorker>> it = workers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, AutoMineWorker> entry = it.next();
            BotSession session = manager.getBot(entry.getKey());
            if (session != null && session.isPlayable()) manager.runInContext(session, entry.getValue()::stop);
            flush(entry.getKey(), entry.getValue());
            it.remove();
        }
    }

    private static void flush(String name, AutoMineWorker worker) {
        for (String message : worker.drainMessages()) {
            TextUtil.sendMessage("[AutoMine] §f" + name + "§7: " + message);
        }
    }
}
