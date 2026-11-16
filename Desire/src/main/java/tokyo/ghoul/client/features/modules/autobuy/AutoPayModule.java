package tokyo.ghoul.client.features.modules.autobuy;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.utils.other.TextUtil;
import tokyo.ghoul.client.features.modules.autobuy.engine.AutoBuyParsedItems;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "AutoPay", description = "Автоматический перевод денег игрокам", category = Category.AUTOBUY)
public class AutoPayModule extends Module {
    @Getter private static final AutoPayModule instance = new AutoPayModule();

    private static final long CHECK_INTERVAL_MS = 1000L;
    private static final long COMMAND_COOLDOWN_MS = 3500L;
    private static final long BALANCE_REQUEST_COOLDOWN_MS = 12000L;
    private static final long KNOWN_BALANCE_TTL_MS = 15000L;
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([0-9][0-9\\s,\\.]*[0-9]|[0-9])");

    public final TextSetting nick = new TextSetting("Никнейм игрока").value("");
    public final TextSetting keepCoins = new TextSetting("Оставлять монет").value("10000000");
    public final SliderSetting triggerPercent = new SliderSetting("Отправлять от %")
            .range(1, 100).step(1).value(50f);

    private long nextCheckAtMs;
    private long lastBalanceRequestAtMs;
    private long lastCommandAtMs;
    private long lastKnownBalance = -1L;
    private long lastKnownBalanceAtMs;

    public AutoPayModule() {
        addSettings(nick, keepCoins, triggerPercent);
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(e -> onTick()));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        addEvents(tick, packet);
    }

    @Override
    public void onEnable() {
        nextCheckAtMs = 0L;
        lastKnownBalance = -1L;
    }

    @Override
    public void onDisable() {
        lastKnownBalance = -1L;
    }

    private void onTick() {
        if (!isEnabled() || mc.player == null || mc.getConnection() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < nextCheckAtMs) {
            return;
        }
        nextCheckAtMs = now + CHECK_INTERVAL_MS;

        String receiver = sanitizeNick(nick.getText());
        if (receiver.isBlank()) {
            return;
        }

        long keep = parseMoney(keepCoins.getText(), -1L);
        if (keep < 0L) {
            return;
        }

        long balance = resolveBalance(now);
        if (balance < 0L) {
            requestBalance(now);
            return;
        }

        if (now - lastCommandAtMs < COMMAND_COOLDOWN_MS) {
            return;
        }

        long surplus = balance - keep;
        if (surplus <= 0L) {
            return;
        }

        double percent = triggerPercent.getValue() / 100.0;
        long payout = Math.max(1L, Math.round(surplus * percent));
        payout = Math.min(payout, surplus);
        if (payout <= 0L) {
            return;
        }

        sendPayCommand(receiver, payout, now);
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (!isEnabled() || !event.isReceive()) {
            return;
        }

        Packet<?> raw = event.packet();
        String text = null;
        if (raw instanceof ClientboundSystemChatPacket p) {
            text = p.content().getString();
        } else if (raw instanceof ClientboundDisguisedChatPacket d) {
            text = d.message().getString();
        } else if (raw instanceof ClientboundPlayerChatPacket pc) {
            text = pc.body().content();
        }

        if (text != null) {
            handleChatMessage(text);
        }
    }

    public void handleChatMessage(String message) {
        long balance = parseBalanceLine(message);
        if (balance >= 0L) {
            lastKnownBalance = balance;
            lastKnownBalanceAtMs = System.currentTimeMillis();
        }
    }

    private long resolveBalance(long now) {
        long scoreboardBalance = readScoreboardBalance();
        if (scoreboardBalance >= 0L) {
            lastKnownBalance = scoreboardBalance;
            lastKnownBalanceAtMs = now;
            return scoreboardBalance;
        }

        if (lastKnownBalance >= 0L && now - lastKnownBalanceAtMs <= KNOWN_BALANCE_TTL_MS) {
            return lastKnownBalance;
        }

        return -1L;
    }

    private long readScoreboardBalance() {
        if (mc.level == null) {
            return -1L;
        }

        Scoreboard scoreboard = mc.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (objective == null) {
            return -1L;
        }

        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            Component decorated = PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner()));
            long balance = parseBalanceLine(decorated.getString());
            if (balance >= 0L) {
                return balance;
            }
        }

        return -1L;
    }

    private long parseBalanceLine(String text) {
        if (text == null || text.isBlank()) {
            return -1L;
        }

        String normalized = AutoBuyParsedItems.normalizeDisplayName(text)
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е')
                .trim();

        if (!normalized.contains("монеток")
                && !normalized.contains("баланс")
                && !normalized.contains("balance")
                && !normalized.contains("coins")
                && !normalized.contains("монет")) {
            return -1L;
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(normalized);
        long lastParsed = -1L;
        while (matcher.find()) {
            long val = parseMoney(matcher.group(1), -1L);
            if (val >= 0L) {
                lastParsed = val;
            }
        }
        return lastParsed;
    }

    private void requestBalance(long now) {
        if (now - lastBalanceRequestAtMs < BALANCE_REQUEST_COOLDOWN_MS) {
            return;
        }
        lastBalanceRequestAtMs = now;
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("money");
        }
    }

    private void sendPayCommand(String receiver, long amount, long now) {
        lastCommandAtMs = now;
        if (mc.getConnection() != null) {
            mc.getConnection().sendCommand("pay " + receiver + " " + amount);
            TextUtil.sendMessage("§d[AutoPay]§r Перевёл §a" + amount + "§r монет игроку §b" + receiver);
        }
    }

    private String sanitizeNick(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9_]", "").trim();
    }

    private long parseMoney(String text, long fallback) {
        if (text == null || text.isBlank()) return fallback;
        try {
            String clean = text.replaceAll("[^0-9]", "");
            return clean.isEmpty() ? fallback : Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
