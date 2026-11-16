package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.interfaces.HoveredSlotProvider;
import tokyo.ghoul.api.utils.math.TimerUtil;

@Getter
@ModuleRegister(name = "Item Scroller", description = "Быстрое перемещение предметов в инвентаре", category = Category.OTHER)
public class ItemScrollerModule extends Module {
    @Getter private static final ItemScrollerModule instance = new ItemScrollerModule();

    private final SliderSetting delay = new SliderSetting("Задержка").value(0f).range(0f, 10f).step(1f);

    private boolean mouseHold;
    private int lastSlotIndex = -1;
    private final TimerUtil clickTimer = new TimerUtil();

    public ItemScrollerModule() {
        addSettings(delay);
    }

    @Override
    public void onEvent() {
        EventListener tickEvent = TickEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        addEvents(tickEvent);
    }

    private void onTick() {
        if (mc.player == null || mc.gameMode == null) return;
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> container)) {
            mouseHold = false;
            return;
        }

        mouseHold = GLFW.glfwGetMouseButton(mc.getWindow().handle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (!mouseHold) {
            lastSlotIndex = -1;
            return;
        }

        if (!(container instanceof HoveredSlotProvider provider)) return;
        Slot slot = provider.static$getHoveredSlot();
        if (slot == null || !slot.hasItem()) return;
        if (slot.index == lastSlotIndex) return;
        if (delay.getValue() > 0f && !clickTimer.finished(delay.getValue() * 10f)) return;

        clickTimer.reset();
        mc.gameMode.handleContainerInput(
                container.getMenu().containerId,
                slot.index,
                0,
                net.minecraft.world.inventory.ContainerInput.QUICK_MOVE,
                mc.player);
        lastSlotIndex = slot.index;
    }
}
