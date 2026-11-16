package tokyo.ghoul.client.features.modules.movement;

import lombok.Getter;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractCommandBlockEditScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundContainerSlotStateChangedPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.event.events.player.other.CloseScreenEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.client.ThreadManager;
import tokyo.ghoul.api.utils.math.TimerUtil;
import tokyo.ghoul.api.utils.other.NetworkUtil;
import tokyo.ghoul.api.utils.player.MoveUtil;
import tokyo.ghoul.api.utils.other.SlownessManager;

import java.util.LinkedList;

@ModuleRegister(name = "Inventory Move", description = "Движение с открытым инвентарём", category = Category.MOVEMENT)
public class InventoryMoveModule extends Module {
    @Getter private static final InventoryMoveModule instance = new InventoryMoveModule();

    public final ModeSetting swapMode = new ModeSetting("Swap mode").value("Universal").values("Basic", "Grim", "Legit", "Universal");
    public final SliderSetting slownessDuration = new SliderSetting("Slowness duration").value(50f).range(1f, 400f).step(1f)
            .setVisible(this::isUniversal);

    private static final long UNIVERSAL_STOP_MS = 200L;

    private final LinkedList<Packet<?>> packet = new LinkedList<>();
    private final TimerUtil timerUtil = new TimerUtil();
    private final TimerUtil flushTimer = new TimerUtil();
    private boolean slowed = false;
    private boolean wasSprinting = false;
    private boolean pendingFlush = false;
    private int pendingContainerId = 0;

    public InventoryMoveModule() {
        addSettings(swapMode, slownessDuration);
    }

    public boolean isLegit() {
        return swapMode.is("Legit");
    }
    public boolean isGrim() {
        return swapMode.is("Grim");
    }
    public boolean isBasic() {
        return swapMode.is("Basic");
    }
    public boolean isUniversal() {
        return swapMode.is("Universal");
    }

    @Override
    public void onEvent() {
        EventListener closeScreenEvent = CloseScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            closeScreenEvent();
        }));
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            updateEvent();
        }));
        EventListener packetEvent = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            packetEvent(event);
        }));
        EventListener sprintEvent = SprintEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.gui.screen() != null) {
                event.setSprint(false);
            }
        }));

        addEvents(closeScreenEvent, updateEvent, packetEvent, sprintEvent);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        if (pendingFlush && mc.player != null && !packet.isEmpty()) {
            sendQueuedPackets();
        }
        packet.clear();
        pendingFlush = false;
        wasSprinting = false;
    }

    private void closeScreenEvent() {
        if (isUniversal() && mc.gui.screen() instanceof InventoryScreen && !packet.isEmpty()) {
            CloseScreenEvent.getInstance().setCancel(true);

            wasSprinting = mc.player.isSprinting();
            pendingContainerId = mc.player.containerMenu.containerId;
            pendingFlush = true;
            flushTimer.reset();
            return;
        }

        if (mc.gui.screen() instanceof InventoryScreen && !packet.isEmpty() && isLegit()) {
            ThreadManager.run(() -> {
                slowed = true;
                timerUtil.reset();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                while (!packet.isEmpty()) {
                    sendPacket(packet.removeLast());
                }
                slowed = false;
            });
            CloseScreenEvent.getInstance().setCancel(true);
        }
    }
    
    private void updateEvent() {
        if (!timerUtil.finished(100) && isLegit()) {
            slowed = true;
            for (KeyMapping keyBinding : MoveUtil.getMovementKeys()) {
                keyBinding.setDown(false);
            }
            slowed = false;
            return;
        }

        if (isUniversal() && pendingFlush) {
            if (mc.player == null) {
                pendingFlush = false;
                packet.clear();
            } else if (mc.gui.screen() instanceof AbstractContainerScreen<?>
                    || flushTimer.finished(slownessDuration.getValue().longValue())) {
                stopAndFlush();
            }
        }

        if (
                mc.gui.screen() instanceof ChatScreen
                        || mc.gui.screen() instanceof SignEditScreen
                        || mc.gui.screen() instanceof AnvilScreen
                        || mc.gui.screen() instanceof AbstractCommandBlockEditScreen
                        || mc.gui.screen() instanceof StructureBlockEditScreen
                        || slowed
                        || SlownessManager.slowed
        ) {
            return;
        }

        MoveUtil.updateMovementKeys();

        if (mc.player != null && mc.gui.screen() != null) {
            mc.options.keySprint.setDown(false);
            mc.player.setSprinting(false);
        }
    }

    private void stopAndFlush() {
        pendingFlush = false;
        if (packet.isEmpty()) return;
        SlownessManager.applySlowness(UNIVERSAL_STOP_MS, 0L, this::sendQueuedPackets);
    }

    private void sendQueuedPackets() {
        if (mc.player == null) {
            packet.clear();
            wasSprinting = false;
            return;
        }
        if (wasSprinting) mc.player.setSprinting(false);
        while (!packet.isEmpty()) {
            NetworkUtil.sendSilentPacket(packet.removeFirst());
        }
        NetworkUtil.sendSilentPacket(new ServerboundContainerClosePacket(pendingContainerId));
        if (wasSprinting) {
            mc.player.setSprinting(true);
            wasSprinting = false;
        }
    }
    
    private void packetEvent(PacketEvent.PacketEventData event) {
        if (!event.isSend()) return;
        if (mc.gui.screen() == null || mc.gui.screen() instanceof ChatScreen) return;

        Packet<?> pacl = event.packet();

        if (isUniversal()) {
            if (pacl instanceof ServerboundContainerClickPacket
                    || pacl instanceof ServerboundContainerButtonClickPacket
                    || pacl instanceof ServerboundSetCreativeModeSlotPacket
                    || pacl instanceof ServerboundContainerSlotStateChangedPacket) {
                if (MoveUtil.isMoving() && mc.gui.screen() instanceof InventoryScreen) {
                    packet.add(pacl);
                    PacketEvent.getInstance().setCancel(true);
                }
            }
            return;
        }

        if (isLegit() && (MoveUtil.isMoving() || mc.options.keyJump.isDown())) {
            if (pacl instanceof ServerboundContainerClickPacket
                    || pacl instanceof ServerboundContainerButtonClickPacket
                    || pacl instanceof ServerboundSetCreativeModeSlotPacket
                    || pacl instanceof ServerboundContainerSlotStateChangedPacket
            ) {
                if (mc.gui.screen() instanceof InventoryScreen) {
                    packet.add(pacl);
                    PacketEvent.getInstance().setCancel(true);
                }
            }

            if (pacl instanceof ServerboundContainerClosePacket) {
                packet.add(pacl);
                PacketEvent.getInstance().setCancel(true);
            }
        }
    }
}
