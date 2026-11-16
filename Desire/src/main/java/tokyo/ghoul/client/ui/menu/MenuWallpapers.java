package tokyo.ghoul.client.ui.menu;

import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.render.pipeline.EvaTextures;

import java.util.List;

public final class MenuWallpapers {
    public record Wallpaper(String name, Identifier image, Identifier preview, float aspect) {
    }

    private static final String CONFIG_KEY = "wallpaper";

    private static final List<Wallpaper> WALLPAPERS = List.of(
            wallpaper(1, "Jungle Mist", 1.7778f),
            wallpaper(2, "Overgrown", 1.7582f),
            wallpaper(3, "Golden Hour", 1.7778f),
            wallpaper(4, "Sakura", 1.7778f),
            wallpaper(5, "Dusk Ridge", 1.7778f),
            wallpaper(6, "Aurora", 1.7778f),
            wallpaper(7, "Nebula", 1.7778f),
            wallpaper(8, "Blue Hour", 1.7778f)
    );

    private static boolean selectionLoaded;
    private static int selected;
    private static boolean warmed;

    private MenuWallpapers() {
    }

    public static List<Wallpaper> all() {
        return WALLPAPERS;
    }

    public static int size() {
        return WALLPAPERS.size();
    }

    public static Wallpaper get(int index) {
        if (WALLPAPERS.isEmpty()) return null;
        return WALLPAPERS.get(Math.max(0, Math.min(WALLPAPERS.size() - 1, index)));
    }

    public static int selectedIndex() {
        loadSelection();
        return selected;
    }

    public static Wallpaper selected() {
        return get(selectedIndex());
    }

    public static void select(int index) {
        loadSelection();
        if (index < 0 || index >= WALLPAPERS.size() || index == selected) return;
        selected = index;
        MenuConfigStore.save(data -> data.addProperty(CONFIG_KEY, selected));
    }

    public static void warmTextures() {
        if (warmed) return;
        warmed = true;
        for (Wallpaper wallpaper : WALLPAPERS) {
            touch(wallpaper.image());
            touch(wallpaper.preview());
        }
    }

    private static void touch(Identifier texture) {
        try {
            EvaTextures.view(texture);
        } catch (Throwable ignored) {
        }
    }

    private static void loadSelection() {
        if (selectionLoaded) return;
        selectionLoaded = true;

        String raw = MenuConfigStore.getString(CONFIG_KEY, "0").trim();
        int index = -1;
        try {
            index = Integer.parseInt(raw);
        } catch (NumberFormatException ignored) {
        }
        if (index < 0 || index >= WALLPAPERS.size()) {
            for (int i = 0; i < WALLPAPERS.size(); i++) {
                if (WALLPAPERS.get(i).name().equalsIgnoreCase(raw)) {
                    index = i;
                    break;
                }
            }
        }
        selected = index >= 0 && index < WALLPAPERS.size() ? index : 0;
    }

    private static Wallpaper wallpaper(int index, String name, float aspect) {
        String path = "textures/menu/wallpapers/wallpaper_" + index;
        return new Wallpaper(name,
                Identifier.fromNamespaceAndPath("desire", path + ".png"),
                Identifier.fromNamespaceAndPath("desire", path + "_preview.png"),
                aspect);
    }
}
