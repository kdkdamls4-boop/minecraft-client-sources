package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.Minecraft;

public interface ClickGui {
    boolean isOpen();

    ClickGuiTab getTab();

    boolean isTextInputFocused();

    static boolean isShown() {
        return Minecraft.getInstance().gui.screen() instanceof ClickGui;
    }

    static ClickGui current() {
        return Minecraft.getInstance().gui.screen() instanceof ClickGui gui ? gui : null;
    }
}
