package tokyo.ghoul.client.features.modules.render.cosmetics.imported;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ImportedAnimation {
    private static final float MIN_LENGTH = 0.001F;
    private static final float MIN_SEGMENT = 1.0E-4F;

    private final float length;
    private final boolean loop;
    private final Map<String, BoneKeys> bones;

    private ImportedAnimation(float length, boolean loop, Map<String, BoneKeys> bones) {
        this.length = Math.max(MIN_LENGTH, length);
        this.loop = loop;
        this.bones = bones;
    }

    public record Pose(float[] rotation, float[] position, float[] scale) {
    }

    private record BoneKeys(List<Key> rotation, List<Key> position, List<Key> scale) {
    }

    private record Key(float time, float[] value) {
    }

    public Pose sample(String bone, float seconds) {
        BoneKeys keys = bones.get(bone);
        if (keys == null) return null;
        float time = loop ? seconds % length : Math.min(seconds, length);
        return new Pose(interpolate(keys.rotation, time), interpolate(keys.position, time), interpolate(keys.scale, time));
    }

    static ImportedAnimation parse(JsonObject root) {
        if (!root.has("animations")) return null;
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("animations").entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject clip = entry.getValue().getAsJsonObject();
            float length = clip.has("animation_length") ? clip.get("animation_length").getAsFloat() : 1.0F;
            boolean loop = false;
            if (clip.has("loop") && clip.get("loop").isJsonPrimitive()) {
                loop = clip.get("loop").getAsJsonPrimitive().isBoolean()
                        ? clip.get("loop").getAsBoolean()
                        : "loop".equalsIgnoreCase(clip.get("loop").getAsString());
            }
            Map<String, BoneKeys> bones = new HashMap<>();
            if (clip.has("bones")) {
                for (Map.Entry<String, JsonElement> bone : clip.getAsJsonObject("bones").entrySet()) {
                    JsonObject data = bone.getValue().getAsJsonObject();
                    bones.put(bone.getKey(), new BoneKeys(keys(data.get("rotation")), keys(data.get("position")), keys(data.get("scale"))));
                }
            }
            return new ImportedAnimation(length, loop, bones);
        }
        return null;
    }

    private static List<Key> keys(JsonElement element) {
        if (element == null || element.isJsonNull()) return List.of();
        if (element.isJsonArray()) return List.of(new Key(0.0F, vector(element)));
        JsonObject object = element.getAsJsonObject();
        if (object.has("vector") || object.has("post") || object.has("pre")) return List.of(new Key(0.0F, vector(object)));
        List<Key> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            try {
                keys.add(new Key(Float.parseFloat(entry.getKey()), vector(entry.getValue())));
            } catch (NumberFormatException ignored) {
            }
        }
        keys.sort(Comparator.comparingDouble(Key::time));
        return keys;
    }

    private static float[] vector(JsonElement element) {
        if (element == null || element.isJsonNull()) return new float[3];
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            return new float[]{number(array, 0), number(array, 1), number(array, 2)};
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("post")) return vector(object.get("post"));
        if (object.has("vector")) return vector(object.get("vector"));
        if (object.has("pre")) return vector(object.get("pre"));
        return new float[3];
    }

    private static float number(JsonArray array, int index) {
        if (array.size() <= index || !array.get(index).isJsonPrimitive()) return 0.0F;
        try {
            return array.get(index).getAsFloat();
        } catch (Exception e) {
            return 0.0F;
        }
    }

    private static float[] interpolate(List<Key> keys, float time) {
        if (keys.isEmpty()) return null;
        Key first = keys.getFirst();
        Key last = keys.getLast();
        if (keys.size() == 1 || time <= first.time) return first.value.clone();
        if (time >= last.time) return last.value.clone();
        for (int i = 0; i < keys.size() - 1; i++) {
            Key from = keys.get(i);
            Key to = keys.get(i + 1);
            if (time < from.time || time > to.time) continue;
            float t = (time - from.time) / Math.max(MIN_SEGMENT, to.time - from.time);
            return new float[]{
                    from.value[0] + (to.value[0] - from.value[0]) * t,
                    from.value[1] + (to.value[1] - from.value[1]) * t,
                    from.value[2] + (to.value[2] - from.value[2]) * t
            };
        }
        return last.value.clone();
    }
}
