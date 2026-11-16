package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;
// import tokyo.ghoul.client.ui.clickgui.CsGuiScreen;
import tokyo.ghoul.client.ui.clickgui.LightGuiScreen;
// import tokyo.ghoul.client.ui.clickgui.ScreenClickGUI;

@ModuleRegister(name = "Click GUI", description = "Меню клиента", category = Category.RENDER, bind = GLFW.GLFW_KEY_RIGHT_SHIFT)
public class ClickGUIModule extends Module {
    @Getter private static final ClickGUIModule instance = new ClickGUIModule();

    // public static final String DROPDOWN = "Dropdown";
    // public static final String CSGUI = "CsGui";
    public static final String DESIRE = "Desire";

    // public final ModeSetting style = new ModeSetting("Style").values(DROPDOWN, CSGUI, DESIRE).value(DESIRE);
    public final ModeSetting style = new ModeSetting("Style").values(DESIRE).value(DESIRE);

    public ClickGUIModule() {
        addSettings(style);
    }

    @Override
    public void onEnable() {
        if (mc.gui.screen() != null) return;

        mc.gui.setScreen(LightGuiScreen.getInstance());
        // if (style.is(DESIRE)) mc.gui.setScreen(LightGuiScreen.getInstance());
        // else mc.gui.setScreen(style.is(CSGUI) ? CsGuiScreen.getInstance() : ScreenClickGUI.getInstance());
    }

    @Override
    public void onEvent() {
        toggle();
    }
}
