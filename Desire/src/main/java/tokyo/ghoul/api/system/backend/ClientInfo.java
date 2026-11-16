package tokyo.ghoul.api.system.backend;

import lombok.experimental.UtilityClass;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

@UtilityClass
public class ClientInfo {
    public final String DISPLAY_NAME = "DesireAlpha";
    public final String NAME = "Desire";
    public final String VERSION = FabricLoader.getInstance().getModContainer(NAME.toLowerCase()).get().getMetadata().getVersion().getFriendlyString();

    static {
        File oldDir = new File(System.getProperty("user.dir"), "Static");
        File newDir = new File(System.getProperty("user.dir"), NAME);
        if (oldDir.isDirectory() && !newDir.exists()) {
            oldDir.renameTo(newDir);
        }
    }

    public final String GAME_PATH = new File(System.getProperty("user.dir")).getAbsolutePath();
    public final String CONFIG_PATH_OTHER = new File(System.getProperty("user.dir"), NAME + "/other").getAbsolutePath();
    public final String CONFIG_PATH_THEMES = new File(System.getProperty("user.dir"), NAME + "/themes").getAbsolutePath();
    public final String CONFIG_PATH_MAIN = new File(System.getProperty("user.home"), NAME + "/main").getAbsolutePath();
    public final String LEGACY_CONFIG_PATH_MAIN = new File(System.getProperty("user.dir"), NAME + "/main").getAbsolutePath();

    static {
        migrateLegacyConfigs();
    }

    private void migrateLegacyConfigs() {
        File legacy = new File(LEGACY_CONFIG_PATH_MAIN);
        File target = new File(CONFIG_PATH_MAIN);
        try {
            if (!legacy.isDirectory() || legacy.getCanonicalFile().equals(target.getCanonicalFile())) return;
            java.nio.file.Files.createDirectories(target.toPath());
            File marker = new File(target, ".migrated_" + Integer.toHexString(legacy.getCanonicalPath().hashCode()));
            if (marker.exists()) return;
            File[] files = legacy.listFiles(File::isFile);
            if (files == null) return;
            for (File file : files) {
                File dest = new File(target, file.getName());
                if (!dest.exists() || file.lastModified() > dest.lastModified()) {
                    java.nio.file.Files.copy(file.toPath(), dest.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            java.nio.file.StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            marker.createNewFile();
        } catch (java.io.IOException e) {
            System.err.println("Failed to migrate configs: " + e.getMessage());
        }
    }
}
