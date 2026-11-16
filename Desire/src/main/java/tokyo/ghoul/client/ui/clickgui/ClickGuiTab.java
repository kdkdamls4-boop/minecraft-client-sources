package tokyo.ghoul.client.ui.clickgui;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;

@Getter
public enum ClickGuiTab {
    COMBAT(Category.COMBAT, "Combat", "b", "Category with PvP modules", 0f, 0f),
    MOVEMENT(Category.MOVEMENT, "Movement", "c", "Category with movement modules", 0f, 0f),
    RENDER(Category.RENDER, "Visuals", "m", "Category with visual modules", 0f, 0f),
    PLAYER(Category.PLAYER, "Player", "e", "Category with player modules", 0f, 0f),
    OTHER(Category.OTHER, "Misc", "d", "Miscellaneous modules", 0f, 0f),
    CONFIGS(null, "Configs", "C", "Saved configurations", 0f, 0f),
    COSMETICS(null, "Customization", "K", "Player cosmetics", 0f, 0f),
    EVENTS(null, "Events", "q", "Server events", 0f, 0f),
    THEME(null, "Themes", "G", "Client customization", 0f, 0f),
    AUTOBUY(null, "Autobuy", "", "Auction auto-buy items", 0f, 0f);

    private final Category category;
    private final String label;
    private final String iconGlyph;
    private final String description;
    private final float iconShiftX;
    private final float iconShiftY;

    ClickGuiTab(Category category, String label, String iconGlyph, String description) {
        this(category, label, iconGlyph, description, 0f, 0f);
    }

    ClickGuiTab(Category category, String label, String iconGlyph, String description, float iconShiftX, float iconShiftY) {
        this.category = category;
        this.label = label;
        this.iconGlyph = iconGlyph;
        this.description = description;
        this.iconShiftX = iconShiftX;
        this.iconShiftY = iconShiftY;
    }

    public boolean isCategory() {
        return category != null;
    }

    public static ClickGuiTab[] sidebarTabs() {
        return new ClickGuiTab[]{
                COMBAT,
                RENDER, OTHER,
                PLAYER, MOVEMENT,
                CONFIGS, COSMETICS
        };
    }

    public static String sectionTitle(ClickGuiTab tab) {
        return switch (tab) {
            case COMBAT -> "Combat";
            case RENDER, OTHER -> "Visuals";
            case PLAYER, MOVEMENT -> "Player";
            case CONFIGS, COSMETICS -> "Settings";
            default -> "";
        };
    }

    public static boolean startsSection(ClickGuiTab tab) {
        return tab == COMBAT || tab == RENDER || tab == PLAYER || tab == CONFIGS;
    }

    public static ClickGuiTab of(Category category) {
        if (category == Category.AUTOBUY) return OTHER;
        for (ClickGuiTab tab : values()) {
            if (tab.category == category) return tab;
        }
        return COMBAT;
    }
}
