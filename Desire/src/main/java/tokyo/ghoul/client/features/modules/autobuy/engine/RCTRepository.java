package tokyo.ghoul.client.features.modules.autobuy.engine;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.utils.other.TextUtil;

import java.util.Locale;

@Getter
public class RCTRepository {
    @Getter private static final RCTRepository instance = new RCTRepository();

    public static final int MAX_ANARCHY = 74;

    private static final int LIST_FIRST_SLOT = 18;

    private static final int[][] CATEGORIES = {
            {0, 1},
            {1, 18},
            {2, 39},
            {3, 58},
    };

    private static final long HOLYWORLD_GRACE_MS = 4000L;
    private static final long OPEN_LITE_DELAY_MS = 1200L;
    private static final long MENU_FIRST_CLICK_DELAY_MS = 800L;
    private static final long MENU_SECOND_CLICK_DELAY_MS = 900L;
    private static final long NAVIGATION_TIMEOUT_MS = 5000L;

    private boolean lobby;
    private int anarchy;
    private long notHolyWorldSinceMs;
    private long nextNavActionAtMs;
    private long navigationStartedAtMs;
    private long lastHubCommandAtMs;

    public RCTRepository() {
        PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        TickEvent.getInstance().subscribe(new Listener<>(e -> onTick()));
    }

    public void onPacket(PacketEvent.PacketEventData e) {
        if (anarchy == 0 || !e.isReceive()) {
            return;
        }

        Packet<?> packet = e.packet();
        if (packet instanceof ClientboundSystemChatPacket chatPacket) {
            String text = chatPacket.content().getString().toLowerCase(Locale.ROOT);
            if (text.contains("команды не существует")
                    || text.contains("нет доступа к ней")
                    || text.contains("используй /help для помощи")) {
                stop();
                return;
            }

            if (!text.contains("хаб") && text.contains("не удалось")) {
                TextUtil.sendMessage("[RCT] На данную анархию §cнельзя§r зайти");
                stop();
            }
        }
    }

    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (anarchy == 0 || mc.player == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (navigationStartedAtMs > 0L && now - navigationStartedAtMs >= NAVIGATION_TIMEOUT_MS) {
            stop();
            return;
        }

        ServerHandler serverHandler = ServerHandler.getInstance();
        if (!serverHandler.isHolyWorld()) {
            if (notHolyWorldSinceMs == 0L) {
                notHolyWorldSinceMs = now;
                return;
            }
            if (now - notHolyWorldSinceMs < HOLYWORLD_GRACE_MS) {
                return;
            }
            notHolyWorldSinceMs = 0L;
            anarchy = 0;
            return;
        }
        notHolyWorldSinceMs = 0L;

        int currentAnarchy = serverHandler.getAnarchy();
        if (lobby) {
            if (currentAnarchy == -1) {
                lobby = false;
            } else if (now - lastHubCommandAtMs >= 500L) {
                if (mc.getConnection() != null) {
                    mc.getConnection().sendCommand("hub");
                }
                lastHubCommandAtMs = now;
            }
            return;
        }

        if (currentAnarchy == anarchy) {
            anarchy = 0;
            return;
        }

        Screen currentScreen = mc.gui != null ? mc.gui.screen() : null;
        if (currentScreen instanceof AbstractContainerScreen<?> screen
                && screen.getTitle().getString().equals("Выбор Лайт анархии:")) {
            boolean secondScreen = screen.getMenu().slots.size() < 10;

            if (now < nextNavActionAtMs) {
                return;
            }

            int[] category = categoryFor(anarchy);
            if (secondScreen) {
                if (mc.gameMode != null) {
                    mc.gameMode.handleContainerInput(screen.getMenu().containerId, category[0], 0, ContainerInput.PICKUP, mc.player);
                }
                nextNavActionAtMs = now + MENU_SECOND_CLICK_DELAY_MS;
                return;
            }

            int slot = LIST_FIRST_SLOT + anarchy - category[1];
            ItemStack stack = slot >= 0 && slot < screen.getMenu().slots.size()
                    ? screen.getMenu().getSlot(slot).getItem()
                    : ItemStack.EMPTY;

            if (stack.isEmpty()) {
                return;
            }

            if (!isAnarchy(stack, anarchy)) {
                TextUtil.sendMessage("[RCT] Лайт " + anarchy + " не на своём месте — §cразметка меню изменилась");
                stop();
                return;
            }

            if (mc.gameMode != null) {
                mc.gameMode.handleContainerInput(screen.getMenu().containerId, slot, 0, ContainerInput.PICKUP, mc.player);
            }
            nextNavActionAtMs = now + OPEN_LITE_DELAY_MS;
            return;
        }

        if (now < nextNavActionAtMs) {
            return;
        }
        if (now - lastHubCommandAtMs >= 500L) {
            if (mc.getConnection() != null) {
                mc.getConnection().sendCommand("lite");
            }
            lastHubCommandAtMs = now;
            nextNavActionAtMs = now + MENU_FIRST_CLICK_DELAY_MS;
        }
    }

    public void reconnect(int anarchy) {
        if (anarchy > 0 && anarchy <= MAX_ANARCHY) {
            this.anarchy = anarchy;
            this.lobby = true;
            this.notHolyWorldSinceMs = 0L;
            this.navigationStartedAtMs = System.currentTimeMillis();
            this.nextNavActionAtMs = this.navigationStartedAtMs + OPEN_LITE_DELAY_MS;
            this.lastHubCommandAtMs = 0L;
        } else {
            TextUtil.sendMessage("[RCT] Неверный §cлайт");
        }
    }

    public void stop() {
        anarchy = 0;
        lobby = false;
        notHolyWorldSinceMs = 0L;
        nextNavActionAtMs = 0L;
        navigationStartedAtMs = 0L;
    }

    public boolean isNavigating() {
        return anarchy != 0;
    }

    private static int[] categoryFor(int anarchy) {
        int[] match = CATEGORIES[0];
        for (int[] category : CATEGORIES) {
            if (anarchy >= category[1]) {
                match = category;
            }
        }
        return match;
    }

    private static boolean isAnarchy(ItemStack stack, int anarchy) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return false;
        }

        String server = anarchy == 1 ? "lanarchy" : "lanarchy" + anarchy;
        return customData.copyTag().toString()
                .contains("\"advancedserverselecter:server\":\"" + server + "\"");
    }
}
