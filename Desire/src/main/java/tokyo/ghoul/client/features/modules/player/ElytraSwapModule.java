package tokyo.ghoul.client.features.modules.player;

import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BindSetting;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.api.utils.inventory.InventoryActions;
import tokyo.ghoul.api.utils.math.TimerUtil;
import tokyo.ghoul.api.utils.other.ActionSequence;
import tokyo.ghoul.api.utils.other.ScreenUtil;
import tokyo.ghoul.api.utils.other.ServerUtil;
import tokyo.ghoul.api.utils.player.DirectionalInput;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;
import tokyo.ghoul.inject.accessor.MultiPlayerGameModeAccessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ModuleRegister(name = "Elytra Swap", description = "Быстрая смена элитр и нагрудника", category = Category.MOVEMENT)
public class ElytraSwapModule extends Module {
    @Getter private static final ElytraSwapModule instance = new ElytraSwapModule();

    private static final int CHEST_MENU_SLOT = 6;
    private static final int UNBOUND = -999;

    private static final SwapTiming DEFAULT_TIMING = new SwapTiming(2, 2, 2);
    private static final SwapTiming FUNTIME_TIMING = new SwapTiming(50, 50, 50);
    private static final SwapTiming SPOOKYTIME_TIMING = new SwapTiming(50, 50, 150);
    private static final SwapTiming HOLY_TIMING = new SwapTiming(50, 50, 50);

    private final BindSetting swap = new BindSetting("Кнопка свапа").value(UNBOUND);
    private final BindSetting use = new BindSetting("Кнопка фейерверка").value(UNBOUND);
    private final BooleanSetting autoFly = new BooleanSetting("Авто полёт").value(false);
    private final TextSetting flyCommands = new TextSetting("Команды полёта").value("fly; speed 10")
            .placeholder("Команды через ;").setVisible(autoFly::getValue);
    private final BooleanSetting oldSwap = new BooleanSetting("Старый свап").value(false);
    private final BooleanSetting chestSwap = new BooleanSetting("Свап в PvP").value(false);

    private SwapTiming timing = DEFAULT_TIMING;
    private final ActionSequence actionSequence = new ActionSequence();
    private final TimerUtil flyTimer = new TimerUtil();
    private boolean flyCommandsPending;
    private boolean blockMovement;

    public ElytraSwapModule() {
        addSettings(swap, use, autoFly, flyCommands, oldSwap, chestSwap);
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener key = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != GLFW.GLFW_PRESS || mc.player == null) return;
            if (!ScreenUtil.allowsMovement(mc.gui.screen())) return;
            int code = event.mouse() ? -100 + event.key() : event.key();
            if (code != UNBOUND) handleKey(code);
        }));
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(-3, event -> {
            if (blockMovement) {
                event.setDirectionalInput(DirectionalInput.NONE);
                event.setJump(false);
            }
        }));
        EventListener sprint = SprintEvent.getInstance().subscribe(new Listener<>(-3, event -> {
            if (blockMovement) event.setSprint(false);
        }));
        addEvents(tick, key, input, sprint);
    }

    @Override
    public void onDisable() {
        actionSequence.clear();
        blockMovement = false;
        flyCommandsPending = false;
        super.onDisable();
    }

    private void onTick() {
        if (mc.player == null) return;

        actionSequence.tick();
        timing = currentTiming();

        if (flyCommandsPending) {
            if (flyTimer.getElapsedTime() > 3000L) {
                flyCommandsPending = false;
            } else if (mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA) && actionSequence.isFinished()) {
                flyCommandsPending = false;
                runFlyCommands();
            }
        }

        // свап нагрудника обратно в PvP, если на сервере элитры запрещены в бою
        if (chestSwap.getValue() && ServerUtil.isInPvp() && mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                && isPvpSwapServer() && mc.player.tickCount % 15 == 0) {
            swapElytra();
        }
    }

    private static SwapTiming currentTiming() {
        if (ServerUtil.isServer("funtime") || ServerUtil.isLocalProxy()) return FUNTIME_TIMING;
        if (ServerUtil.isServer("holy")) return SPOOKYTIME_TIMING;
        if (ServerUtil.isServer("spookytime") || ServerUtil.isServer("dildo")) return HOLY_TIMING;
        return DEFAULT_TIMING;
    }

    private static boolean isPvpSwapServer() {
        return ServerUtil.isServer("funtime") || ServerUtil.isLocalProxy() || ServerUtil.isServer("spookytime")
                || ServerUtil.isServer("dildo");
    }

    private void handleKey(int code) {
        if (code == swap.getValue()) {
            if (actionSequence.isFinished()) {
                swapElytra();
            }
        } else if (code == use.getValue() && mc.player.isFallFlying()) {
            if (findMainInventorySlot(Items.FIREWORK_ROCKET) != null) {
                InventoryActions.useFirework();
            } else {
                notify(Items.FIREWORK_ROCKET, "Фейерверков нет в инвентаре!");
            }
        }
    }

    private void swapElytra() {
        flyCommandsPending = false;
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        Slot chestplate = findChestplateSlot();

        if (chest.is(Items.ELYTRA)) {
            if (chestplate == null) {
                swapViaFreeSlot();
                return;
            }
        } else if (!chest.isEmpty() || chestplate == null) {
            Slot elytra = findElytraSlot();
            if (elytra == null) {
                notify(Items.ELYTRA, "Элитры нет в инвентаре!");
                return;
            }
            swapChestSlot(elytra.index);
            if (autoFly.getValue()) {
                flyCommandsPending = true;
                flyTimer.reset();
            }
            return;
        }

        swapChestSlot(chestplate.index);
    }

    private void runFlyCommands() {
        for (String part : flyCommands.getValue().split("[;\n]")) {
            String command = part.trim();
            if (command.startsWith("/")) {
                command = command.substring(1);
            }
            if (!command.isEmpty() && mc.getConnection() != null) {
                mc.getConnection().sendCommand(command);
            }
        }
    }

    // снять элитры в пустой слот
    private void swapViaFreeSlot() {
        Slot slot = findEmptySlot();
        if (slot == null) {
            notify(Items.ELYTRA, "Нет свободного слота в инвентаре!");
            return;
        }

        actionSequence.clear()
                .addDelayed(0, () -> blockMovement = true)
                .addDelayed(timing.firstAt(), () -> {
                    clickSlot(CHEST_MENU_SLOT, 0, ContainerInput.PICKUP);
                    clickSlot(slot.index, 0, ContainerInput.PICKUP);
                    closeContainer();
                    blockMovement = false;
                });
    }

    private Slot findEmptySlot() {
        for (Slot slot : getPlayerSlots()) {
            if (slot.getItem().isEmpty()) return slot;
        }
        return null;
    }

    private void swapChestSlot(int slot) {
        if (oldSwap.getValue()) {
            if (ServerUtil.isHolyWorld()) {
                swapLegacy(slot);
            } else {
                swapDirect(slot);
            }
            return;
        }

        int hotbar = toHotbarIndex(slot);
        if (hotbar != -1) {
            useHotbarSlot(hotbar);
            return;
        }

        int selected = mc.player.getInventory().getSelectedSlot();
        actionSequence.clear()
                .addDelayed(0, () -> blockMovement = true)
                .addDelayed(timing.firstAt(), () -> {
                    clickSlot(slot, selected, ContainerInput.SWAP);
                    closeContainer();
                })
                .addDelayed(timing.armorAt(), () -> sendUseItem(InteractionHand.MAIN_HAND))
                .addDelayed(timing.backAt(), () -> {
                    clickSlot(slot, selected, ContainerInput.SWAP);
                    closeContainer();
                    blockMovement = false;
                });
    }

    // надеть ПКМ предметом из хотбара тихой сменой слота
    private void useHotbarSlot(int hotbar) {
        int selected = mc.player.getInventory().getSelectedSlot();
        ((MultiPlayerGameModeAccessor) mc.gameMode).ghoul$ensureHasSentCarriedItem();
        if (hotbar != selected) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(hotbar));
        }
        sendUseItem(InteractionHand.MAIN_HAND);
        if (hotbar != selected) {
            mc.player.connection.send(new ServerboundSetCarriedItemPacket(selected));
        }
    }

    private void swapDirect(int slot) {
        actionSequence.clear()
                .addDelayed(0, () -> blockMovement = true)
                .addDelayed(timing.firstAt(), () -> {
                    clickSlot(slot, 0, ContainerInput.PICKUP);
                    clickSlot(CHEST_MENU_SLOT, 0, ContainerInput.PICKUP);
                    clickSlot(slot, 0, ContainerInput.PICKUP);
                    closeContainer();
                })
                .addDelayed(timing.backAt(), () -> blockMovement = false);
    }

    private void swapLegacy(int slot) {
        int hotbar = toHotbarIndex(slot);
        if (hotbar == -1) {
            int selected = mc.player.getInventory().getSelectedSlot();
            actionSequence.clear()
                    .addDelayed(0, () -> blockMovement = true)
                    .addDelayed(timing.firstAt(), () -> clickSlot(slot, selected, ContainerInput.SWAP))
                    .addDelayed(timing.armorAt(), () -> clickSlot(CHEST_MENU_SLOT, selected, ContainerInput.SWAP))
                    .addDelayed(timing.backAt(), () -> {
                        clickSlot(slot, selected, ContainerInput.SWAP);
                        closeContainer();
                        blockMovement = false;
                    });
        } else {
            actionSequence.clear()
                    .addDelayed(0, () -> blockMovement = true)
                    .addDelayed(timing.firstAt(), () -> {
                        clickSlot(CHEST_MENU_SLOT, hotbar, ContainerInput.SWAP);
                        closeContainer();
                    })
                    .addDelayed(timing.backAt(), () -> blockMovement = false);
        }
    }

    private void closeContainer() {
        if (mc.gui.screen() == null) {
            mc.player.connection.send(new ServerboundContainerClosePacket(0));
        }
    }

    private static void clickSlot(int slot, int button, ContainerInput type) {
        if (mc.player != null && mc.gameMode != null) {
            mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, slot, button, type, mc.player);
        }
    }

    private static void sendUseItem(InteractionHand hand) {
        ((MultiPlayerGameModeAccessor) mc.gameMode).ghoul$startPrediction(mc.level,
                sequence -> new ServerboundUseItemPacket(hand, sequence, mc.player.getYRot(), mc.player.getXRot()));
    }

    private void notify(Item item, String message) {
        NotifyWidget.item(getName(), item.getDefaultInstance(), message);
    }

    // ---- поиск слотов ----

    private static int toHotbarIndex(int slot) {
        return slot >= 36 && slot < 45 && mc.player.containerMenu.containerId == 0 ? slot - 36 : -1;
    }

    private static List<Slot> getPlayerSlots() {
        List<Slot> slots = new ArrayList<>();
        int size = mc.player.containerMenu.slots.size();
        for (int i = Math.max(0, size - 37); i < size; i++) {
            slots.add(mc.player.containerMenu.getSlot(i));
        }
        return slots;
    }

    // слоты 9..44 меню, с конца (хотбар первым)
    private static Slot findMainInventorySlot(Item item) {
        if (mc.player == null) return null;
        List<Slot> slots = new ArrayList<>();
        for (Slot slot : mc.player.containerMenu.slots) {
            if (slot.index >= 9 && slot.index <= 44) slots.add(slot);
        }
        Collections.reverse(slots);
        for (Slot slot : slots) {
            if (!slot.getItem().isEmpty() && slot.getItem().is(item)) return slot;
        }
        return null;
    }

    public static Slot findChestplateSlot() {
        for (Item item : new Item[]{Items.NETHERITE_CHESTPLATE, Items.DIAMOND_CHESTPLATE, Items.IRON_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE, Items.GOLDEN_CHESTPLATE, Items.LEATHER_CHESTPLATE}) {
            Slot slot = findMainInventorySlot(item);
            if (slot != null) return slot;
        }
        return null;
    }

    public static Slot findElytraSlot() {
        Slot slot = findMainInventorySlot(Items.ELYTRA);
        return slot != null && slot.getItem().getDamageValue() < slot.getItem().getMaxDamage() ? slot : null;
    }

    // ---- для ElytraFly ----

    /** Надеть элитры или вернуть нагрудник. */
    public static void swap(SwapItem item) {
        if (mc.player == null || mc.gameMode == null || !instance.actionSequence.isFinished()) return;

        boolean wearingElytra = mc.player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (item == SwapItem.ELYTRA ? !wearingElytra : wearingElytra) {
            instance.timing = currentTiming();
            instance.swapElytra();
        }
    }

    public static int findBestElytraSlot() {
        if (mc.player == null) return -1;
        Slot slot = findElytraSlot();
        return slot == null ? -1 : slot.index;
    }

    public enum SwapItem {
        ELYTRA,
        CHESTPLATE
    }

    private record SwapTiming(int first, int armor, int back) {
        int firstAt() {
            return first;
        }

        int armorAt() {
            return first + armor;
        }

        int backAt() {
            return first + armor + back;
        }
    }
}
