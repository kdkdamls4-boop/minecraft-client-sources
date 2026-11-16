package tokyo.ghoul.api.utils.render.fonts;

import lombok.experimental.UtilityClass;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Fonts {
    public final String sf = "sf_pro";
    public final String ps = "product_sans";

    private static final Map<String, Font> cache = new HashMap<>();

    public final Font SF_BOLD = get("blade_sf/sf_pro_bold");
    public final Font SF_SEMIBOLD = get("blade_sf/sf_pro_semibold");
    public final Font SF_MEDIUM = get("blade_sf/sf_pro_medium");
    public final Font SF_REGULAR = get("blade_sf/sf_pro_regular");
    public final Font SF_LIGHT = get("blade_sf/sf_pro_regular");

    public final Font HUD_REGULAR = get("blade_sf/sf_pro_regular");
    public final Font HUD_MEDIUM = get("blade_sf/sf_pro_medium");
    public final Font HUD_SEMIBOLD = get("blade_sf/sf_pro_semibold");
    public final Font HUD_BOLD = get("blade_sf/sf_pro_bold");
    public final Font HUD_GUILLEMET = get("sf_pro/sf_semibold");

    public final Font PS_BLACK = get("blade_sf/sf_pro_bold");
    public final Font PS_BOLD = get("blade_sf/sf_pro_bold");
    public final Font PS_MEDIUM = get("blade_sf/sf_pro_semibold");
    public final Font PS_REGULAR = get("blade_sf/sf_pro_medium");
    public final Font PS_LIGHT = get("blade_sf/sf_pro_regular");
    public final Font PS_THIN = get("blade_sf/sf_pro_regular");

    public final Font ICONS = get("other/icons");
    public final Font CATEGORY_ICONS = get("wyvern/icons");
    public final Font CLICKGUI_ICONS = get("wyvern/category");

    public final Font MINCED_ICONS = get("polyak/iconsminced");
    public final Font DIVINE = get("polyak/divine");

    public final Font WYVERN = get("wyvern/wyvern");
    public final Font NURIKI = get("wyvern/nuriki");
    public final Font WFONT = get("wyvern/font");
    public final Font KEY = get("key");
    public final Font ICONS_NURIK = get("icons_nurik");
    public final Font LOGO = get("wyvern/desirelogo");

    public final Font DREAMLINE_SF = get("dreamline/dreamlinesf");
    public final Font DREAMLINE_ICONS = get("dreamline/dreamlineicons");
    public final Font ETERNAL_HEAVEN = get("eternal/eternal_heaven");
    public final Font MENU_INTER = get("mainmenu/mainmenu_inter");
    public final Font MENU_ICONS = get("mainmenu/mainmenu_icons");

    public final Font DIMASIK_MEDIUM = get("dimasik/medium");
    public final Font DIMASIK_ICONS = get("dimasik/icons");

    public float getMediumThickness() { return 0.07f; }
    public float getBoldThickness() { return 0.1f; }

    private Font get(String input) {
        return Font.builder().find(input).load();
    }
}
