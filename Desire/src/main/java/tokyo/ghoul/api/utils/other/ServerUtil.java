package tokyo.ghoul.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.multiplayer.ServerData;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.Locale;

/** Проверки сервера по адресу из списка серверов. */
@UtilityClass
public class ServerUtil implements QuickImports {
    public boolean isNullWorld() {
        return mc.player == null || mc.level == null;
    }

    public boolean isServer(String name) {
        ServerData server = mc.getCurrentServer();
        return server != null && server.ip != null && server.ip.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT));
    }

    public boolean isLocalProxy() {
        ServerData server = mc.getCurrentServer();
        return server != null && server.ip != null && (server.ip.equals("0.0.0.0") || server.ip.startsWith("0.0.0.0:"));
    }

    public boolean isFunTime() {
        return isServer("funtime") || isLocalProxy();
    }

    public boolean isDonut() {
        return isServer("donut");
    }

    public boolean isHolyWorld() {
        return isServer("holy");
    }

    public boolean isSpookyTime() {
        return isServer("spookytime") || isServer("dildo");
    }

    public boolean isInPvp() {
        return mc.gui != null && mc.gui.hud != null && SharedClass.inPvp();
    }
}
