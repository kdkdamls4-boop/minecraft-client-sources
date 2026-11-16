package tokyo.ghoul.client.ui.theme.basic;

import tokyo.ghoul.client.ui.theme.Theme;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class PresetThemes {
    private PresetThemes() { }

    private static final Color BACKGROUND = new Color(16, 16, 18);

    private static final Object[][] DEFINITIONS = {
            {"Desire",       0xFF2DD4BF, 0xFF0EA5E9},
            {"Aurora",       0xFF43E97B, 0xFF38F9D7},
            {"Lime Soda",    0xFFA8FF78, 0xFF78FFD6},
            {"Lagoon",       0xFF4FACFE, 0xFF00F2FE},
            {"Royal",        0xFF667EEA, 0xFF764BA2},
            {"Ultraviolet",  0xFF8E2DE2, 0xFF4A00E0},
            {"Lavender",     0xFFC3A6FF, 0xFFF5B8E8},
            {"Cosmic",       0xFFFF3CAC, 0xFF784BA0},
            {"Flamingo",     0xFFF093FB, 0xFFF5576C},
            {"Sakura",       0xFFFF9A9E, 0xFFFAD0C4},
            {"Blood Orange", 0xFFFF512F, 0xFFDD2476},
            {"Crimson",      0xFFED213A, 0xFF93291E},
            {"Sunset",       0xFFFF7E5F, 0xFFFEB47B},
            {"Golden Hour",  0xFFF7971E, 0xFFFFD200},
            {"Moonlight",    0xFFDDE6F5, 0xFF8A9BB8},
    };

    private static final List<Theme> ALL = build();

    public static List<Theme> all() {
        return ALL;
    }

    private static List<Theme> build() {
        List<Theme> themes = new ArrayList<>();
        for (Object[] def : DEFINITIONS) {
            Theme theme = new Theme((String) def[0], true);
            Color primary = new Color((int) def[1], true);
            set(theme, "Primary", primary);
            set(theme, "Secondary", new Color((int) def[2], true));
            set(theme, "Knob", lighten(primary, 0.3f));
            set(theme, "Text", new Color(255, 255, 255));
            set(theme, "Inactive text", new Color(187, 187, 187));
            set(theme, "Background blur", BACKGROUND);
            themes.add(theme);
        }
        return themes;
    }

    private static void set(Theme theme, String element, Color color) {
        for (Theme.ElementColor elementColor : theme.getElementColors()) {
            if (elementColor.getName().equalsIgnoreCase(element)) {
                elementColor.setColor(color);
                return;
            }
        }
    }

    private static Color lighten(Color color, float factor) {
        return new Color(
                (int) (color.getRed() + (255 - color.getRed()) * factor),
                (int) (color.getGreen() + (255 - color.getGreen()) * factor),
                (int) (color.getBlue() + (255 - color.getBlue()) * factor));
    }
}
