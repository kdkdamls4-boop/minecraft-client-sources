package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.client.ui.widget.WidgetManager;

@ModuleRegister(name = "Interface", description = "Элементы интерфейса на экране", category = Category.RENDER)
public class InterfaceModule extends Module {
    @Getter private static final InterfaceModule instance = new InterfaceModule();

    public final MultiBooleanSetting widgets = new MultiBooleanSetting("Widgets");
    private static final float HUD_SCALE = 0.8f;
    private static final float GUI_SCALE = 0.75f;
    private static final int BLUR_PASSES = 3;
    private static final float BLUR_OFFSET = 20f;

    public static float getHudScale() { return HUD_SCALE; }
    public static float getGuiScale() { return GUI_SCALE; }
    public static float getGlassy() { return 0.5f; }
    public static int getPasses() { return BLUR_PASSES; }
    public static float getOffset() { return BLUR_OFFSET; }

    public void init() {
        widgets.value(WidgetManager.getInstance().getWidgets().stream()
                .map(widget -> {
                    BooleanSetting setting = new BooleanSetting(widget.getName()).value(widget.isEnabled());
                    setting.onAction(() -> widget.setEnabled(setting.getValue()));
                    return setting;
                })
                .toList());

        addSettings(widgets);
    }

    @Override
    public void onEvent() {

    }
}
