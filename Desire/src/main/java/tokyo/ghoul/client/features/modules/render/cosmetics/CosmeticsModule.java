package tokyo.ghoul.client.features.modules.render.cosmetics;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.client.features.modules.render.ItemReplacerModule;
import tokyo.ghoul.client.features.modules.render.cosmetics.imported.ImportedCosmetics;
import tokyo.ghoul.client.features.modules.render.custommodels.CustomModelsModel;

import java.util.Locale;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Cosmetics", description = "Косметика игрока", category = Category.PLAYER, hidden = true)
public class CosmeticsModule extends Module {
    private static final List<Entry> ENTRIES = loadEntries();
    private static final String[] WINGS = options("wings");
    private static final String[] BODIES = options("bodywear");
    private static final String[] PETS = options("pet");
    private static final String[] MASKS = options("hat");
    private static final String[] HEADWEAR = options("headwear");
    private static final String[] EARS = options("ears");
    private static final String[] TAILS = options("tails");
    private static final String[] HEADS = options("heads");
    private static final String[] PAWS = options("paws");
    private static final String[] STOCKINGS = options("stockings");

    @Getter private static final CosmeticsModule instance = new CosmeticsModule();

    public final ModeSetting wings = new ModeSetting("Крылья").value("None").values(WINGS);
    public final ModeSetting body = new ModeSetting("Рюкзак").value("None").values(BODIES);
    public final ModeSetting pet = new ModeSetting("Питомцы").value("None").values(PETS);
    public final ModeSetting hat = new ModeSetting("Маски").value("None").values(MASKS);
    public final ModeSetting headwear = new ModeSetting("Шляпы").value("None").values(HEADWEAR);
    public final ModeSetting ears = new ModeSetting("Уши").value("None").values(EARS);
    public final ModeSetting tails = new ModeSetting("Хвосты").value("None").values(TAILS);
    public final ModeSetting heads = new ModeSetting("Головы").value("None").values(HEADS);
    public final ModeSetting paws = new ModeSetting("Лапки").value("None").values(PAWS);
    public final ModeSetting stockings = new ModeSetting("Чулки").value("None").values(STOCKINGS);

    public final BooleanSetting customModels = new BooleanSetting("Кастомные модели").value(false)
            .setVisible(() -> false);
    public final ModeSetting customModel = new ModeSetting("Модель").value("Sahur")
            .values("Sahur", "Crazy Rabbit", "White Demon", "Red Demon", "Amogus",
                    "Chimpanzini Bananini", "Fish Bear", "Coconut Capybara",
                    "Tralalero Tralala", "Bombardiro Crocodilo", "Ballerina Cappuccina", "Lirili Larila", "Brr Brr Patapim")
            .setVisible(() -> false);
    public final BooleanSetting friends = new BooleanSetting("Модели на друзей").value(true)
            .setVisible(customModels::getValue);

    public CosmeticsModule() {
        addSettings(wings, body, pet, hat, headwear, ears, tails, heads, paws, stockings, customModels, customModel, friends);
    }

    public boolean appliesCustomModel(int entityId) {
        if (!isEnabled() || !customModels.getValue()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && entityId == minecraft.player.getId()) return true;
        if (!friends.getValue() || minecraft.level == null) return false;
        Entity entity = minecraft.level.getEntity(entityId);
        return entity != null && FriendManager.getInstance().contains(entity.getName().getString());
    }

    public boolean appliesCustomModel(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        if (state != null && CosmeticsGuiPreview.forcedModelFor(state) != null) return true;
        return appliesCustomModel(state == null ? -1 : state.id);
    }

    public CustomModelsModel.ModelType currentModelType() {
        return resolveModelType(customModel.getValue());
    }

    public CustomModelsModel.ModelType currentModelType(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        String forced = CosmeticsGuiPreview.forcedModelFor(state);
        return resolveModelType(forced != null ? forced : customModel.getValue());
    }

    public Identifier customModelTexture() {
        return modelTexture(customModel.getValue());
    }

    public Identifier customModelTexture(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        String forced = CosmeticsGuiPreview.forcedModelFor(state);
        return modelTexture(forced != null ? forced : customModel.getValue());
    }

    private static CustomModelsModel.ModelType resolveModelType(String name) {
        if (name == null) return CustomModelsModel.ModelType.SAHUR;
        return switch (name) {
            case "Crazy Rabbit" -> CustomModelsModel.ModelType.CRAZY_RABBIT;
            case "White Demon" -> CustomModelsModel.ModelType.WHITE_DEMON;
            case "Red Demon" -> CustomModelsModel.ModelType.RED_DEMON;
            case "Amogus" -> CustomModelsModel.ModelType.AMOGUS;
            case "Chimpanzini Bananini" -> CustomModelsModel.ModelType.CHIMPANZINI_BANANINI;
            case "Fish Bear" -> CustomModelsModel.ModelType.FISH_BEAR;
            case "Coconut Capybara" -> CustomModelsModel.ModelType.COCONUT_CAPYBARA;
            case "Tralalero Tralala" -> CustomModelsModel.ModelType.TRALALERO_TRALALA;
            case "Bombardiro Crocodilo" -> CustomModelsModel.ModelType.BOMBARDIRO_CROCODILO;
            case "Ballerina Cappuccina" -> CustomModelsModel.ModelType.BALLERINA_CAPPUCCINA;
            case "Lirili Larila" -> CustomModelsModel.ModelType.LIRILI_LARILA;
            case "Brr Brr Patapim" -> CustomModelsModel.ModelType.BRR_BRR_PATAPIM;
            default -> CustomModelsModel.ModelType.SAHUR;
        };
    }

    public static Identifier modelTexture(String name) {
        String file = switch (name == null ? "" : name) {
            case "Crazy Rabbit" -> "rabbit";
            case "White Demon" -> "whitedemon";
            case "Red Demon" -> "reddemon";
            case "Amogus" -> "amogus";
            default -> name == null ? "sahur" : name.toLowerCase(Locale.ROOT).replace(' ', '_');
        };
        return Identifier.fromNamespaceAndPath("desire", "textures/models/" + file + ".png");
    }

    @Override
    public void onEvent() {
    }

    public void unequipAll() {
        wings.setValue("None");
        body.setValue("None");
        pet.setValue("None");
        hat.setValue("None");
        headwear.setValue("None");
        ears.setValue("None");
        tails.setValue("None");
        heads.setValue("None");
        paws.setValue("None");
        stockings.setValue("None");
        customModels.setValue(false);
        ItemReplacerModule swords = ItemReplacerModule.getInstance();
        if (swords.isEnabled()) swords.setEnabled(false);
    }

    public int[] selected() {
        return new int[]{index(wings.getValue()), index(body.getValue()), index(pet.getValue()), index(hat.getValue()), index(headwear.getValue()),
                index(ears.getValue()), index(tails.getValue()), index(heads.getValue()), index(paws.getValue()), index(stockings.getValue())};
    }

    public static Entry entry(int index) {
        return index >= 0 && index < ENTRIES.size() ? ENTRIES.get(index) : null;
    }

    public static List<Entry> entries() {
        return ENTRIES;
    }

    public static int indexOf(Entry entry) {
        if (entry == null) return -1;
        for (int i = 0; i < ENTRIES.size(); i++) {
            if (ENTRIES.get(i) == entry || ENTRIES.get(i).resourceIndex == entry.resourceIndex) return i;
        }
        return -1;
    }

    public static List<Entry> entriesForCategory(String category) {
        List<Entry> result = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (matchesCategory(entry.type, category)) result.add(entry);
        }
        return result;
    }

    public ModeSetting settingFor(Entry entry) {
        if (entry == null) return null;
        return switch (entry.type) {
            case "wings" -> wings;
            case "bodywear" -> body;
            case "pet" -> pet;
            case "hat" -> hat;
            case "headwear" -> headwear;
            case "ears" -> ears;
            case "tails" -> tails;
            case "heads" -> heads;
            case "paws" -> paws;
            case "stockings" -> stockings;
            default -> null;
        };
    }

    public boolean isEquipped(Entry entry) {
        if (entry == null) return false;
        if ("model".equals(entry.type)) {
            return customModels.getValue() && entry.label.equals(customModel.getValue());
        }
        if ("sword".equals(entry.type)) {
            ItemReplacerModule swords = ItemReplacerModule.getInstance();
            return swords.isEnabled() && entry.label.equals(swords.model.getValue());
        }
        ModeSetting setting = settingFor(entry);
        return setting != null && entry.label.equals(setting.getValue());
    }

    public void toggleEquip(Entry entry) {
        if (entry == null) return;
        if ("model".equals(entry.type)) {
            String label = entry.label();
            if (!customModel.getModes().contains(label)) {
                customModel.getModes().add(label);
            }
            if (customModels.getValue() && label.equals(customModel.getValue())) {
                customModels.setValue(false);
                return;
            }
            customModel.setValue(label);
            customModels.setValue(true);
            if (!isEnabled()) setEnabled(true);
            return;
        }
        if ("sword".equals(entry.type)) {
            ItemReplacerModule swords = ItemReplacerModule.getInstance();
            String label = entry.label();
            if (!swords.model.getModes().contains(label)) {
                swords.model.getModes().add(label);
            }
            if (swords.isEnabled() && label.equals(swords.model.getValue())) {
                swords.setEnabled(false);
                return;
            }
            swords.model.setValue(label);
            if (!swords.isEnabled()) swords.setEnabled(true);
            if (!isEnabled()) setEnabled(true);
            return;
        }
        ModeSetting setting = settingFor(entry);
        if (setting == null) return;
        String label = entry.label();
        if (!setting.getModes().contains(label)) {
            setting.getModes().add(label);
        }
        if (label.equals(setting.getValue())) {
            setting.setValue("None");
            return;
        }
        setting.setValue(label);
        if (!isEnabled()) setEnabled(true);
    }

    public static Identifier icon(Entry entry) {
        if (entry == null) return null;
        if ("model".equals(entry.type)) {
            return modelTexture(entry.label);
        }
        if ("sword".equals(entry.type)) return ItemReplacerModule.icon(entry.label);
        ImportedCosmetics.Imported imported = ImportedCosmetics.of(entry);
        if (imported != null) return imported.texture();
        Identifier item = Identifier.fromNamespaceAndPath("desire", "cosmetics/textures/item_" + entry.resourceIndex + ".png");
        if (resourceExists(item)) return item;
        return Identifier.fromNamespaceAndPath("desire", "cosmetics/textures/cosmetic_" + entry.resourceIndex + ".png");
    }

    public static boolean isSword(Entry entry) {
        return entry != null && "sword".equals(entry.type);
    }

    public static boolean isModel(Entry entry) {
        return entry != null && "model".equals(entry.type);
    }

    private static boolean matchesCategory(String type, String category) {
        if (category == null || category.isEmpty()) return true;
        if ("Hats".equalsIgnoreCase(category)) return "headwear".equals(type);
        if ("Masks".equalsIgnoreCase(category)) return "hat".equals(type);
        if ("Body".equalsIgnoreCase(category)) return "bodywear".equals(type);
        if ("Wings".equalsIgnoreCase(category)) return "wings".equals(type);
        if ("Pets".equalsIgnoreCase(category)) return "pet".equals(type);
        if ("Sword".equalsIgnoreCase(category) || "Swords".equalsIgnoreCase(category)) return "sword".equals(type);
        if ("Models".equalsIgnoreCase(category) || "Model".equalsIgnoreCase(category)) return "model".equals(type);
        return type.equalsIgnoreCase(category);
    }

    private static boolean resourceExists(Identifier id) {
        String path = "/assets/" + id.getNamespace() + "/" + id.getPath();
        try (InputStream stream = CosmeticsModule.class.getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int index(String label) {
        if ("None".equals(label)) return -1;
        for (int i = 0; i < ENTRIES.size(); i++) if (ENTRIES.get(i).label.equals(label)) return i;
        return -1;
    }

    private static String[] options(String type) {
        List<String> values = new ArrayList<>();
        values.add("None");
        for (Entry entry : ENTRIES) if (entry.type.equals(type)) values.add(entry.label);
        return values.toArray(String[]::new);
    }

    private static List<Entry> loadEntries() {
        List<Entry> entries = new ArrayList<>();
        for (int index = 0; index < 256; index++) {
            String path = "/assets/desire/cosmetics/cosmetics/models/cosmetic_" + index + ".json";
            try (InputStream input = CosmeticsModule.class.getResourceAsStream(path)) {
                if (input == null) continue;
                JsonObject json = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
                String raw = json.has("name") ? json.get("name").getAsString() : "Cosmetic " + index;
                String name = cleanLabel(raw);
                int pos = json.has("pos") ? json.get("pos").getAsInt() : -1;
                String type = json.has("type") && !json.get("type").isJsonNull()
                        ? json.get("type").getAsString().toLowerCase().trim()
                        : "";
                if (type.isEmpty()) type = classify(index, name, pos);
                if (index == 51) {
                    name = "Angel Halo";
                    type = "headwear";
                }
                if ("cape".equals(type)) continue;
                entries.add(new Entry(index, uniqueLabel(entries, name), type));
            } catch (Exception ignored) {
            }
        }
        List<ImportedCosmetics.Imported> imported = ImportedCosmetics.all();
        for (int i = 0; i < imported.size(); i++) {
            entries.add(new Entry(ImportedCosmetics.BASE + i, uniqueLabel(entries, imported.get(i).label()), imported.get(i).type()));
        }
        int swordId = 100_000;
        for (String label : ItemReplacerModule.modelLabels()) {
            entries.add(new Entry(swordId++, label, "sword"));
        }
        entries.add(new Entry(200_000, "Sahur", "model"));
        entries.add(new Entry(200_001, "Crazy Rabbit", "model"));
        entries.add(new Entry(200_002, "White Demon", "model"));
        entries.add(new Entry(200_003, "Red Demon", "model"));
        entries.add(new Entry(200_004, "Amogus", "model"));
        entries.add(new Entry(200_005, "Chimpanzini Bananini", "model"));
        entries.add(new Entry(200_006, "Fish Bear", "model"));
        entries.add(new Entry(200_007, "Coconut Capybara", "model"));
        entries.add(new Entry(200_008, "Tralalero Tralala", "model"));
        entries.add(new Entry(200_009, "Bombardiro Crocodilo", "model"));
        entries.add(new Entry(200_010, "Ballerina Cappuccina", "model"));
        entries.add(new Entry(200_011, "Lirili Larila", "model"));
        entries.add(new Entry(200_012, "Brr Brr Patapim", "model"));
        return List.copyOf(entries);
    }

    private static String cleanLabel(String raw) {
        if (raw == null || raw.isBlank()) return "Cosmetic";
        String name = raw.trim();
        if (name.regionMatches(true, 0, "pulse_", 0, 6)) name = name.substring(6);
        name = name.replace('_', ' ').trim();
        name = name.replaceFirst("(?i)^(akvi4|bro9i|fluger|wota)\\s+", "").trim();
        if (name.equalsIgnoreCase("Lui Vuitton")) name = "Louis Vuitton";
        if (name.equalsIgnoreCase("Nimb")) name = "Angel Halo";
        if (name.equalsIgnoreCase("Vulcano Wings")) name = "Volcano Wings";
        if (name.isBlank()) name = raw.replace('_', ' ').trim();
        return name;
    }

    private static String uniqueLabel(List<Entry> entries, String base) {
        String label = base;
        int suffix = 2;
        while (hasLabel(entries, label)) {
            label = base + " " + suffix++;
        }
        return label;
    }

    private static boolean hasLabel(List<Entry> entries, String label) {
        for (Entry entry : entries) {
            if (entry.label.equalsIgnoreCase(label)) return true;
        }
        return false;
    }

    private static String classify(int index, String name, int pos) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.contains("cape")) return "cape";
        if (lower.contains("wing")) return "wings";
        if (lower.contains("pet") || lower.contains("bee") || lower.contains("radish")) return "pet";
        if (lower.contains("mask") || lower.contains("shades")
                || lower.equals("angry") || lower.equals("wink") || lower.equals("nerd")) return "hat";
        if (lower.contains("hat") || lower.contains("nimb") || lower.contains("halo")
                || lower.contains("glasses") || lower.contains("cone") || lower.contains("chef")) return "headwear";
        if (pos == 2) return "headwear";
        if (index <= 13) return "cape";
        if (index <= 26) return "wings";
        if (index <= 37) return "bodywear";
        if (index <= 49) return "pet";
        return "bodywear";
    }

    public record Entry(int resourceIndex, String label, String type) { }
}
