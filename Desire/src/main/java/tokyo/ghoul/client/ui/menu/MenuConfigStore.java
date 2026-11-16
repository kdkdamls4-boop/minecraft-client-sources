package tokyo.ghoul.client.ui.menu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class MenuConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = Path.of(ClientInfo.CONFIG_PATH_MAIN, "menu.json");
    private static JsonObject root;

    private MenuConfigStore() {
    }

    public static String getString(String key, String defaultValue) {
        JsonElement element = data().get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : defaultValue;
    }

    public static JsonArray getArray(String key) {
        JsonElement element = data().get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : null;
    }

    public static void save(Consumer<JsonObject> mutator) {
        JsonObject data = data();
        mutator.accept(data);
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(data));
        } catch (IOException exception) {
            System.err.printf("Failed to save menu.json: %s%n", exception.getMessage());
        }
    }

    private static JsonObject data() {
        if (root != null) {
            return root;
        }
        if (Files.isRegularFile(PATH)) {
            try {
                JsonObject loaded = GSON.fromJson(Files.readString(PATH), JsonObject.class);
                root = loaded != null ? loaded : new JsonObject();
                return root;
            } catch (RuntimeException | IOException ignored) {
            }
        }
        root = new JsonObject();
        return root;
    }
}
