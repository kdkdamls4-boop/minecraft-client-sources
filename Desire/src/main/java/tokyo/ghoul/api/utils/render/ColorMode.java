package tokyo.ghoul.api.utils.render;

import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.utils.color.UIColors;

public final class ColorMode {
    public static final String SYNC = "Sync";
    public static final String CUSTOM = "Custom";

    private ColorMode() {
    }

    public static ModeSetting setting() {
        return new ModeSetting("Color Mode").values(SYNC, CUSTOM).value(SYNC);
    }

    public static boolean isCustom(ModeSetting mode) {
        return mode != null && CUSTOM.equals(mode.getValue());
    }

    public static int resolve(ModeSetting mode, ColorSetting customColor) {
        return isCustom(mode) && customColor != null && customColor.getValue() != null
                ? customColor.getValue().getRGB()
                : UIColors.primary().getRGB();
    }
}
