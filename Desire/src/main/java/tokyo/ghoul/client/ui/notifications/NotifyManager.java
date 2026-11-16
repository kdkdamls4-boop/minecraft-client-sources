package tokyo.ghoul.client.ui.notifications;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.client.ui.clickgui.ClickGui;
import tokyo.ghoul.client.ui.notifications.impl.EquipmentNotify;
import tokyo.ghoul.client.ui.notifications.impl.PickupItemNotify;
import tokyo.ghoul.client.ui.notifications.impl.SpecNotify;
import tokyo.ghoul.client.ui.notifications.impl.TotemNotify;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

public class NotifyManager implements QuickImports {
    @Getter
    private static final NotifyManager instance = new NotifyManager();

    private static final Pattern SPEC_MESSAGE_PATTERN = Pattern.compile(
            "(?:^|[^\\p{L}\\p{N}_])(?:spec|\\u0441\\u043f\\u0435\\u043a|cgtr|spec\\s+cgtr|\\u0441\\u043f\\u0435\\u043a\\s+cgtr)(?:[^\\p{L}\\p{N}_]|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private final ConcurrentLinkedQueue<Notify> notifyList = new ConcurrentLinkedQueue<>();
    private boolean initialized;

    private boolean notifiedHelmet;
    private boolean notifiedChestplate;
    private boolean notifiedLeggings;
    private boolean notifiedBoots;

    public void init() {
        if (initialized) return;
        initialized = true;

        TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
    }

    public void addNotify(Notify notify) {
        notifyList.add(notify);
    }

    public boolean isEmpty() {
        return notifyList.isEmpty();
    }

    public void render(PoseStack ms) {
        if (mc.gui.screen() instanceof ClickGui) {
            notifyList.removeIf(notify -> notify.getAliveTime().finished(1500));
            return;
        }

        float x = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float y = mc.getWindow().getGuiScaledHeight() / 2.0f + 20.0f;

        List<Notify> snapshot = new ArrayList<>(notifyList);

        for (int i = snapshot.size() - 1; i >= 0; i--) {
            Notify notify = snapshot.get(i);

            notify.getAnimation().setDuration(120);
            notify.getAnimation().setEasing(Easing.LINEAR);

            if (!notify.getAliveTime().finished(1300)) {
                notify.getAnimation().animate(1);
            } else {
                notify.getAnimation().animate(0);
            }

            notify.getAnimation().update();

            notify.setX(x);
            notify.setY(y);
            notify.render(ms);

            y += (float) (NotificationStyle.pitch() * notify.getAnimation().getValue());
        }

        notifyList.removeIf(notify -> notify.getAliveTime().finished(1500));
    }

    private void onTick() {
        if (mc.player == null) return;

        checkArmor(mc.player.getItemBySlot(EquipmentSlot.HEAD), EquipmentNotify.Type.HEAD);
        checkArmor(mc.player.getItemBySlot(EquipmentSlot.CHEST), EquipmentNotify.Type.CHEST);
        checkArmor(mc.player.getItemBySlot(EquipmentSlot.LEGS), EquipmentNotify.Type.LEGGINGS);
        checkArmor(mc.player.getItemBySlot(EquipmentSlot.FEET), EquipmentNotify.Type.BOOTS);
    }

    private void checkArmor(ItemStack stack, EquipmentNotify.Type type) {
        boolean lowDurability = !stack.isEmpty() && stack.isDamageableItem() &&
                (stack.getMaxDamage() - stack.getDamageValue()) < (stack.getMaxDamage() / 5);

        switch (type) {
            case HEAD -> {
                if (lowDurability) {
                    if (!notifiedHelmet) {
                        addNotify(new EquipmentNotify(type, stack.copy()));
                        notifiedHelmet = true;
                    }
                } else {
                    notifiedHelmet = false;
                }
            }
            case CHEST -> {
                if (lowDurability) {
                    if (!notifiedChestplate) {
                        addNotify(new EquipmentNotify(type, stack.copy()));
                        notifiedChestplate = true;
                    }
                } else {
                    notifiedChestplate = false;
                }
            }
            case LEGGINGS -> {
                if (lowDurability) {
                    if (!notifiedLeggings) {
                        addNotify(new EquipmentNotify(type, stack.copy()));
                        notifiedLeggings = true;
                    }
                } else {
                    notifiedLeggings = false;
                }
            }
            case BOOTS -> {
                if (lowDurability) {
                    if (!notifiedBoots) {
                        addNotify(new EquipmentNotify(type, stack.copy()));
                        notifiedBoots = true;
                    }
                } else {
                    notifiedBoots = false;
                }
            }
        }
    }

    private void onPacket(PacketEvent.PacketEventData data) {
        if (!data.isReceive() || mc.level == null || mc.player == null) return;

        var packet = data.packet();

        if (packet instanceof ClientboundEntityEventPacket entityPacket) {
            if (entityPacket.getEventId() == 35) {
                Entity entity = entityPacket.getEntity(mc.level);
                if (entity instanceof Player player && player != mc.player) {
                    boolean enchanted = (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) && player.getMainHandItem().isEnchanted())
                            || (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && player.getOffhandItem().isEnchanted());
                    addNotify(new TotemNotify(player.getScoreboardName(), enchanted));
                }
            }
        }

        if (packet instanceof ClientboundTakeItemEntityPacket takePacket) {
            if (takePacket.getPlayerId() == mc.player.getId()) {
                Entity entity = mc.level.getEntity(takePacket.getItemId());
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getItem();
                    if (!stack.isEmpty() && (!stack.getHoverName().getSiblings().isEmpty() || stack.is(Items.ELYTRA) || stack.is(Items.TOTEM_OF_UNDYING))) {
                        addNotify(new PickupItemNotify(stack.copy()));
                    }
                }
            }
        }

        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            handleChatMessage(systemChat.content().getString());
        } else if (packet instanceof ClientboundDisguisedChatPacket disguisedChat) {
            handleChatMessage(disguisedChat.message().getString());
        } else if (packet instanceof ClientboundPlayerChatPacket playerChat) {
            handleChatMessage(playerChat.body().content());
        }
    }

    private void handleChatMessage(String message) {
        if (message == null || mc.level == null) return;

        if (SPEC_MESSAGE_PATTERN.matcher(message).find()) {
            for (Player player : mc.level.players()) {
                String name = player.getScoreboardName();
                if (player != mc.player && message.contains(name)) {
                    addNotify(new SpecNotify(name));
                }
            }
        }
    }
}
