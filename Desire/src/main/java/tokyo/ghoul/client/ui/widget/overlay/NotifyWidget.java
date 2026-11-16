package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.screens.ChatScreen;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.client.ui.notifications.NotifyManager;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.client.ui.notifications.impl.ItemNotify;
import tokyo.ghoul.client.ui.notifications.impl.ModuleNotify;
import tokyo.ghoul.client.ui.notifications.impl.TextNotify;
import tokyo.ghoul.client.ui.widget.Widget;

public class NotifyWidget extends Widget {
    @Getter
    private static final NotifyWidget instance = new NotifyWidget();

    public NotifyWidget() {
        super(0f, 0f);
        enableHudSettingsMenu();
        NotifyManager.getInstance().init();
    }

    @Override
    public String getName() {
        return "Notify";
    }

    public static void toggle(Module module, boolean enabled) {
        NotifyManager.getInstance().addNotify(new ModuleNotify(module.getName(), enabled));
    }

    public static void text(String message) {
        NotifyManager.getInstance().addNotify(new TextNotify(message));
    }

    public static void item(String title, ItemStack stack, String message) {
        NotifyManager.getInstance().addNotify(new ItemNotify(title, stack, message));
    }

    @Override
    public void render(PoseStack ms) {
        NotifyManager.getInstance().render(ms);

        boolean editing = mc.gui.screen() instanceof ChatScreen;
        float cx = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float cy = mc.getWindow().getGuiScaledHeight() / 2.0f + 20.0f;

        if (editing && NotifyManager.getInstance().isEmpty()) {
            ModuleNotify preview = new ModuleNotify("Example", true);
            preview.getAnimation().setValue(1.0);
            preview.setX(cx);
            preview.setY(cy);
            preview.render(ms);
        }

        getDraggable().setX(cx - 50f);
        getDraggable().setY(cy);
        getDraggable().setWidth(100f);
        getDraggable().setHeight(15f);

        renderHudSettingsMenu(ms);
    }
}
