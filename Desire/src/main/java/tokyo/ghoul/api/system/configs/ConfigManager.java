package tokyo.ghoul.api.system.configs;

import com.google.gson.*;
import lombok.Getter;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.GameLoopEvent;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.module.setting.*;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    @Getter private static final ConfigManager instance = new ConfigManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String AUTO_CONFIG = "autoConfig";
    private static final long AUTOSAVE_INTERVAL_MS = 5000L;
    private final Path CONFIG_DIR;
    private String lastSnapshot;
    private long lastAutosaveCheck;
    private boolean autosaveEnabled;

    private ConfigManager() {
        this.CONFIG_DIR = Paths.get(ClientInfo.CONFIG_PATH_MAIN);
        try {
            Files.createDirectories(CONFIG_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create config dir", e);
        }
    }

    public List<String> getConfigsNames() {
        File configDir = new File(ClientInfo.CONFIG_PATH_MAIN);
        if (!configDir.exists()) return Collections.emptyList();

        File[] files = configDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return Collections.emptyList();

        return Arrays.stream(files).map(f -> f.getName().replace(".json", "")).collect(Collectors.toList());
    }

    private Path getConfigFile(String name) {
        return CONFIG_DIR.resolve(name + ".json");
    }

    private String serializeAll() {
        JsonObject root = new JsonObject();
        JsonObject modulesJson = new JsonObject();
        for (Module m : ModuleManager.getInstance().getModules()) {
            modulesJson.add(m.getName(), createConfigFromModule(m));
        }
        root.add("Modules", modulesJson);
        return GSON.toJson(root);
    }

    public void save(String name) {
        String json = serializeAll();
        writeAtomically(getConfigFile(name), json);
        if (name.equals(AUTO_CONFIG)) lastSnapshot = json;
    }

    private void writeAtomically(Path file, String content) {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(tmp, content);
            if (Files.exists(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            System.err.printf("Failed to save config %s: %s%n", file.getFileName(), e.getMessage());
        }
    }

    public void load(String name) {
        Path file = getConfigFile(name);
        Path backup = file.resolveSibling(file.getFileName() + ".bak");
        if (!Files.exists(file) && !Files.exists(backup)) return;

        JsonObject root = readConfig(file);
        if (root == null) {
            System.err.printf("Config %s is unreadable, trying backup%n", name);
            root = readConfig(backup);
        }
        if (root == null || !root.has("Modules")) return;

        for (var entry : root.getAsJsonObject("Modules").entrySet()) {
            Module module = ModuleManager.getInstance()
                    .getModules().stream()
                    .filter(m -> m.getName().equalsIgnoreCase(entry.getKey()))
                    .findFirst().orElse(null);

            if (module == null) {
                System.err.println("Module not found for config: " + entry.getKey());
                continue;
            }
            try {
                applyConfigToModule(module, entry.getValue().getAsJsonObject());
            } catch (Exception e) {
                System.err.printf("Failed to apply %s from config %s: %s%n", entry.getKey(), name, e.getMessage());
            }
        }
        if (name.equals(AUTO_CONFIG)) lastSnapshot = serializeAll();
    }

    private JsonObject readConfig(Path file) {
        try {
            if (!Files.exists(file)) return null;
            return GSON.fromJson(Files.readString(file), JsonObject.class);
        } catch (IOException | JsonParseException e) {
            return null;
        }
    }

    public void remove(String name) {
        Path file = getConfigFile(name);
        try {
            if (Files.exists(file)) {
                Path trash = CONFIG_DIR.resolve(".trash");
                Files.createDirectories(trash);
                String stamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Files.move(file, trash.resolve(name + "-" + stamp + ".json"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.deleteIfExists(file.resolveSibling(file.getFileName() + ".bak"));
        } catch (IOException e) {
            System.err.printf("Failed to remove config %s: %s%n", name, e.getMessage());
        }
    }


    public void enableAutosave() {
        if (autosaveEnabled) return;
        autosaveEnabled = true;
        lastSnapshot = serializeAll();
        GameLoopEvent.getInstance().subscribe(new Listener<>(event -> tickAutosave()));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                save(AUTO_CONFIG);
            } catch (Exception ignored) {
            }
        }, "desire-config-autosave"));
    }

    private void tickAutosave() {
        long now = System.currentTimeMillis();
        if (now - lastAutosaveCheck < AUTOSAVE_INTERVAL_MS) return;
        lastAutosaveCheck = now;
        try {
            if (!serializeAll().equals(lastSnapshot)) {
                save(AUTO_CONFIG);
            }
        } catch (Exception e) {
            System.err.println("Autosave failed: " + e.getMessage());
        }
    }

    public boolean exists(String name) {
        return Files.exists(getConfigFile(name));
    }

    public long getLastModified(String name) {
        try {
            return Files.getLastModifiedTime(getConfigFile(name)).toMillis();
        } catch (IOException ignored) {
            return 0L;
        }
    }

    private JsonObject createConfigFromModule(Module module) {
        JsonObject json = new JsonObject();
        json.addProperty("enabled", module.isEnabled());
        json.addProperty("bind", module.getBind());

        JsonObject settings = new JsonObject();
        module.getSettings().forEach(s -> {
            JsonElement v = serializeSetting(s);
            if (v != null) settings.add(s.getName(), v);
        });
        json.add("settings", settings);
        return json;
    }

    private void applyConfigToModule(Module module, JsonObject json) {
        if (json.has("enabled")) {
            boolean before = module.isEnabled();
            module.setEnabled(json.get("enabled").getAsBoolean(), true);
            if (autosaveEnabled && before != module.isEnabled()
                    && !(module instanceof tokyo.ghoul.client.features.modules.render.ClickGUIModule)) {
                module.applyEnabledState();
            }
        }
        if (json.has("bind")) module.setBind(json.get("bind").getAsInt());

        if (json.has("settings")) {
            JsonObject settings = json.getAsJsonObject("settings");
            module.getSettings().forEach(s -> {
                if (settings.has(s.getName())) deserializeSetting(s, settings.get(s.getName()));
            });
        }
    }

    private JsonElement serializeSetting(Setting<?> s) {
        return switch (s) {
            case BooleanSetting b -> GSON.toJsonTree(b.getValue());
            case ModeSetting m -> GSON.toJsonTree(m.getValue());
            case SliderSetting sl -> GSON.toJsonTree(sl.getValue());
            case BindSetting bi -> GSON.toJsonTree(bi.getValue());
            case MultiBooleanSetting mb -> {
                JsonObject obj = new JsonObject();
                mb.getValue().forEach(b -> obj.addProperty(b.getName(), b.getValue()));
                yield obj;
            }
            case ColorSetting c -> {
                Color col = c.getValue();
                JsonObject obj = new JsonObject();
                obj.addProperty("r", col.getRed());
                obj.addProperty("g", col.getGreen());
                obj.addProperty("b", col.getBlue());
                obj.addProperty("a", col.getAlpha());
                yield obj;
            }
            default -> null;
        };
    }

    private void deserializeSetting(Setting<?> s, JsonElement e) {
        try {
            switch (s) {
                case BooleanSetting b -> b.setValue(e.getAsBoolean());
                case ModeSetting m -> m.setValue(e.getAsString());
                case SliderSetting sl -> sl.setValue(e.getAsFloat());
                case BindSetting bi -> bi.setValue(e.getAsInt());
                case MultiBooleanSetting mb -> {
                    JsonObject obj = e.getAsJsonObject();
                    mb.getValue().forEach(b -> {
                        if (obj.has(b.getName())) b.setValue(obj.get(b.getName()).getAsBoolean());
                    });
                }
                case ColorSetting c -> {
                    JsonObject obj = e.getAsJsonObject();
                    int r = obj.get("r").getAsInt();
                    int g = obj.get("g").getAsInt();
                    int b = obj.get("b").getAsInt();
                    int a = obj.get("a").getAsInt();
                    c.setValue(new Color(r, g, b, a));
                }
                default -> {}
            }
        } catch (Exception ex) {
            System.err.printf("Failed to deserialize %s: %s%n", s.getName(), ex.getMessage());
        }
    }
}
