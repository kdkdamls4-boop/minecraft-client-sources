package tokyo.ghoul.client.features.modules.misc;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.phys.Vec2;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.configs.FriendManager;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "AutoTpaccept", description = "Автоматически принимает запросы на телепорт", category = Category.OTHER)
public class AutoTpacceptModule extends Module {
    @Getter private static final AutoTpacceptModule instance = new AutoTpacceptModule();

    private static final Pattern COLOR_CODES = Pattern.compile("§.");
    private static final Pattern LEADING_NICK = Pattern.compile("^([a-zA-Z0-9_]{3,16})\\b");
    private static final Pattern LABELED_NICK = Pattern.compile("(?i)\\b(?:ник|nick)\\s*:?\\s*([a-zA-Z0-9_]{3,16})\\b");

    private final BooleanSetting friendsOnly = new BooleanSetting("Только друзья").value(false);
    private final ModeSetting mode = new ModeSetting("Режим").value("Рейджовый").values("Рейджовый", "Легитный");
    private final SliderSetting stopTicks = new SliderSetting("Тики остановки").value(4f).range(0f, 20f).step(1f)
            .setVisible(() -> mode.is("Легитный"));

    private boolean pending;
    private int ticksLeft;

    public AutoTpacceptModule() {
        addSettings(friendsOnly, mode, stopTicks);
    }

    @Override
    public void onEvent() {
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isReceive() && event.packet() instanceof ClientboundSystemChatPacket chat) {
                onChat(chat.content().getString());
            }
        }));
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        addEvents(packet, update);
    }

    @Override
    public void onDisable() {
        if (pending) restoreKeys();
        pending = false;
        super.onDisable();
    }

    private void onChat(String raw) {
        if (mc.player == null || mc.getConnection() == null) return;
        String text = COLOR_CODES.matcher(raw).replaceAll("").replace('\n', ' ').trim().toLowerCase(Locale.ROOT);
        if (!isTeleportRequest(text)) return;

        if (friendsOnly.getValue()) {
            Optional<String> nick = requester(text);
            if (nick.isEmpty() || !isFriend(nick.get())) return;
        }

        if (mode.is("Рейджовый")) {
            mc.getConnection().sendCommand("tpaccept");
        } else if (!pending) {
            pending = true;
            ticksLeft = stopTicks.getValue().intValue();
        }
    }

    private void onUpdate() {
        if (!pending || mc.player == null || mc.getConnection() == null) return;
        for (KeyMapping key : movementKeys()) key.setDown(false);
        if (isMoving()) return;
        if (ticksLeft > 0) {
            ticksLeft--;
            return;
        }
        mc.getConnection().sendCommand("tpaccept");
        pending = false;
        restoreKeys();
    }

    private void restoreKeys() {
        if (mc.gui.screen() != null) return;
        for (KeyMapping key : movementKeys()) {
            key.setDown(InputConstants.isKeyDown(mc.getWindow(), InputConstants.getKey(key.saveString()).getValue()));
        }
    }

    private KeyMapping[] movementKeys() {
        return new KeyMapping[]{
                mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft,
                mc.options.keyRight, mc.options.keyJump, mc.options.keySprint
        };
    }

    private boolean isMoving() {
        Vec2 move = mc.player.input.getMoveVector();
        return move.x != 0f || move.y != 0f;
    }

    private static boolean isTeleportRequest(String text) {
        boolean teleport = text.contains("телепорт") || text.contains("teleport");
        return teleport && text.contains("просит") || teleport && (text.contains("request") || text.contains("want") || text.contains("хочет"));
    }

    private static Optional<String> requester(String text) {
        Matcher labeled = LABELED_NICK.matcher(text);
        if (labeled.find()) return Optional.ofNullable(labeled.group(1));
        Matcher leading = LEADING_NICK.matcher(text);
        return leading.find() ? Optional.ofNullable(leading.group(1)) : Optional.empty();
    }

    private static boolean isFriend(String nick) {
        for (String friend : FriendManager.getInstance().getData()) {
            if (friend.equalsIgnoreCase(nick)) return true;
        }
        return false;
    }
}
