package tokyo.ghoul.client.features.modules.render.cosmetics.imported;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsModule;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ImportedCosmetics {
    public static final int BASE = 300_000;

    private static final String ROOT = "/assets/desire/cosmetics/imported/";
    private static final List<Imported> LIST = load();

    private ImportedCosmetics() {
    }

    public enum Attach {
        HEAD, BODY, PAWS, STOCKINGS;

        static Attach parse(String value) {
            return switch (value == null ? "" : value.toLowerCase(Locale.ROOT)) {
                case "head" -> HEAD;
                case "paws" -> PAWS;
                case "stockings" -> STOCKINGS;
                default -> BODY;
            };
        }

        public boolean limb() {
            return this == PAWS || this == STOCKINGS;
        }
    }

    public static List<Imported> all() {
        return LIST;
    }

    public static Imported get(int resourceIndex) {
        int index = resourceIndex - BASE;
        return index >= 0 && index < LIST.size() ? LIST.get(index) : null;
    }

    public static Imported of(CosmeticsModule.Entry entry) {
        return entry == null ? null : get(entry.resourceIndex());
    }

    private static List<Imported> load() {
        List<Imported> result = new ArrayList<>();
        try (InputStream input = ImportedCosmetics.class.getResourceAsStream(ROOT + "catalog.json")) {
            if (input == null) return List.copyOf(result);
            for (JsonElement element : JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonArray()) {
                JsonObject json = element.getAsJsonObject();
                String category = json.get("category").getAsString();
                String name = json.get("name").getAsString();
                result.add(new Imported(
                        ROOT + category + "/" + name + "/",
                        Identifier.fromNamespaceAndPath("desire", "cosmetics/imported/" + category + "/" + name + "/texture.png"),
                        json.get("type").getAsString(),
                        json.get("label").getAsString(),
                        json.get("scale").getAsFloat(), json.get("x").getAsFloat(), json.get("y").getAsFloat(), json.get("z").getAsFloat(),
                        json.has("animation") && json.get("animation").getAsBoolean(),
                        Attach.parse(json.has("attach") ? json.get("attach").getAsString() : null)));
            }
        } catch (Exception ignored) {
        }
        return List.copyOf(result);
    }

    public static final class Imported {
        private final String directory;
        private final Identifier texture;
        private final String type;
        private final String label;
        private final float scale, x, y, z;
        private final boolean animated;
        private final Attach attach;
        private ImportedModel model;
        private ImportedAnimation animation;
        private boolean loaded;

        Imported(String directory, Identifier texture, String type, String label,
                 float scale, float x, float y, float z, boolean animated, Attach attach) {
            this.directory = directory;
            this.texture = texture;
            this.type = type;
            this.label = label;
            this.scale = scale;
            this.x = x;
            this.y = y;
            this.z = z;
            this.animated = animated;
            this.attach = attach;
        }

        public Identifier texture() {
            return texture;
        }

        public String type() {
            return type;
        }

        public String label() {
            return label;
        }

        public float scale() {
            return scale;
        }

        public float x() {
            return x;
        }

        public float y() {
            return y;
        }

        public float z() {
            return z;
        }

        public boolean animated() {
            return animated;
        }

        public Attach attach() {
            return attach;
        }

        public synchronized ImportedModel model() {
            ensureLoaded();
            return model;
        }

        public synchronized ImportedAnimation animation() {
            ensureLoaded();
            return animation;
        }

        private void ensureLoaded() {
            if (loaded) return;
            loaded = true;
            try (InputStream input = ImportedCosmetics.class.getResourceAsStream(directory + "model.geo.json")) {
                if (input != null) {
                    JsonObject root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                    model = ImportedModel.parse(root, attach.limb());
                }
            } catch (Exception ignored) {
            }
            if (!animated) return;
            try (InputStream input = ImportedCosmetics.class.getResourceAsStream(directory + "animation.json")) {
                if (input != null) {
                    animation = ImportedAnimation.parse(JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject());
                }
            } catch (Exception ignored) {
            }
        }
    }
}
