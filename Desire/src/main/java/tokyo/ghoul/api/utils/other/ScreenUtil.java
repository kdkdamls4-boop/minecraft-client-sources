package tokyo.ghoul.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import tokyo.ghoul.api.event.events.other.ScreenEvent;
import tokyo.ghoul.api.system.backend.Pair;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.player.InventoryUtil;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ScreenUtil implements QuickImports {
    public void drawButton(ScreenEvent.ScreenEventData event) {
        int buttonWidth = 80;
        int buttonHeight = 20;
        Screen screen = event.screen();

        if (screen instanceof InventoryScreen inv) {
            int x = (screen.width - buttonWidth) / 2;
            int y = (screen.height - inv.imageHeight) / 2 - buttonHeight - 5;

            addButtons(event, x, y, buttonWidth, buttonHeight, new Pair<>("Drop all", () -> drop(inv)));
        } else if (screen instanceof AbstractContainerScreen<?> handledScreen
                && handledScreen.getMenu() instanceof ChestMenu genericHandler) {

            String title = handledScreen.getTitle().getString();
            int rows = genericHandler.getRowCount();
            int backgroundHeight = 114 + rows * 18;

            int buttonX = screen.width / 2 + 90;
            int buttonY = (screen.height - backgroundHeight) / 2;


            if (title.contains("Аукцион") && title.contains("Поиск")) return;

            addButtons(event, buttonX, buttonY, buttonWidth, buttonHeight,
                    new Pair<>("Steal all", () -> toInv(genericHandler)),
                    new Pair<>("From inv", () -> fromInv(genericHandler))
                    );
        }
    }

    /** Экран, в котором бинды модулей продолжают работать: нет экрана или контейнер без активного поля ввода. */
    public boolean allowsMovement(Screen screen) {
        if (screen == null) return true;
        if (screen instanceof ChatScreen || screen instanceof AbstractSignEditScreen || screen instanceof BookEditScreen) return false;
        if (!(screen instanceof AbstractContainerScreen<?>)) return false;
        return !(screen.getFocused() instanceof EditBox box) || !box.isActive();
    }

    @SafeVarargs
    private void addButtons(ScreenEvent.ScreenEventData event, int x, int y, int width, int height, Pair<String, Runnable>... pairs) {
        List<Button> buttons = new ArrayList<>();

        int offsetY = y;
        for (Pair<String, Runnable> pair : pairs) {
            Button button = Button.builder(
                    Component.literal(pair.left()),
                    b -> pair.right().run()
            ).bounds(x, offsetY, width, height).build();

            buttons.add(button);
            offsetY += height + 5;
        }

        event.buttons().addAll(buttons);
    }

    private void toInv(ChestMenu containerHandler) {
        for (int i = 0; i < containerHandler.getContainer().getContainerSize(); i++) {
            Slot slot = containerHandler.getSlot(i);
            if (slot.hasItem()) {
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
            }
        }
    }

    private void fromInv(ChestMenu containerHandler) {
        for (int i = containerHandler.getContainer().getContainerSize(); i < containerHandler.slots.size(); i++) {
            Slot slot = containerHandler.getSlot(i);
            if (slot.hasItem()) {
                mc.gameMode.handleContainerInput(mc.player.containerMenu.containerId, i, 0, ContainerInput.QUICK_MOVE, mc.player);
            }
        }
    }

    private void drop(AbstractContainerScreen<?> screen){
        for (Slot slot : screen.getMenu().slots) {
            if (slot.getItem().isEmpty()) continue;
            InventoryUtil.dropSlot(slot.index);
        }
    }
}
