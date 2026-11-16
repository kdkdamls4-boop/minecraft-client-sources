package tokyo.ghoul.api.utils.combat;

import lombok.experimental.UtilityClass;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.util.Mth;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class TpsSync implements QuickImports {
    private final Map<Long, Long> pendingKeepAlives = new HashMap<>();
    private float tps = 20.0f;
    private boolean subscribed;

    private void ensureSubscribed() {
        if (subscribed) return;
        subscribed = true;

        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend() && event.packet() instanceof ServerboundKeepAlivePacket packet) {
                Long sentAt = pendingKeepAlives.remove(packet.getId());
                if (sentAt != null) {
                    long delay = System.currentTimeMillis() - sentAt;
                    if (delay > 0 && delay < 5000) {
                        float measured = Mth.clamp(1000f / delay, 0.1f, 20f);
                        tps = tps * 0.8f + measured * 0.2f;
                    }
                }
            } else if (!event.isSend() && event.packet() instanceof ClientboundKeepAlivePacket packet) {
                pendingKeepAlives.put(packet.getId(), System.currentTimeMillis());
            }
        }));
    }

    public float getCurrentTPS() {
        ensureSubscribed();
        return Mth.clamp(tps, 0.1f, 20f);
    }

    public long getAdjustedCooldown(long baseCooldown, boolean enabled) {
        if (!enabled) return baseCooldown;

        float currentTps = getCurrentTPS();
        if (currentTps >= 20.0f) return baseCooldown;

        float multiplier = 20.0f / currentTps;
        float additionalFactor = 1.0f + (20.0f - currentTps) * 0.05f;
        return (long) (baseCooldown * multiplier * additionalFactor);
    }
}
