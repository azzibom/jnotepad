package by.azzi.jnotepad;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import java.util.prefs.Preferences;

public enum Theme {
    LIGHT, DARK;

    private static final String THEME_PROPERTY_NAME = "theme";
    private static final Preferences p = Preferences.userNodeForPackage(Theme.class);

    private static boolean save = true;
    private static Theme current;

    public static void setupTheme() {
        save = false;
        try {
            setupTheme(Theme.valueOf(p.get(THEME_PROPERTY_NAME, LIGHT.name())));
        } catch (IllegalArgumentException e) {
            setupTheme(LIGHT);
        } finally {
            save = true;
        }
    }

    public static void setupTheme(Theme theme) {
        if (theme == current) {
            return;
        }
        current = theme;
        FlatLaf.setup(theme.getLaf());
        FlatLaf.updateUI();
        if (save) {
            p.put(THEME_PROPERTY_NAME, theme.name());
        }
    }

    private FlatLaf getLaf() {
        switch (this) {
            case DARK:
                return new FlatDarkLaf();
            case LIGHT:
            default:
                return new FlatLightLaf();
        }
    }
}
