package tokyo.ghoul.client.features.modules.render;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;

@ModuleRegister(name = "ItemReplacer", description = "Замена моделей предметов", category = Category.RENDER, hidden = true)
public final class ItemReplacerModule extends Module {
    private static final Map<String, String> MODELS = createModels();
    private static final Map<String, Identifier> ICON_CACHE = new ConcurrentHashMap<>();

    @Getter private static final ItemReplacerModule instance = new ItemReplacerModule();

    public final ModeSetting type = new ModeSetting("Тип предмета").value("Меч").values("Меч", "Топор", "Оба");
    public final ModeSetting model = new ModeSetting("Модель").value("Katana").values(MODELS.keySet().toArray(new String[0]));

    public ItemReplacerModule() {
        model.setVisible(() -> false);
        addSettings(type, model);
    }

    @Override
    public void onEvent() {
    }

    public ModeSetting getModelSetting() {
        return this.model;
    }

    public static List<String> modelLabels() {
        return new ArrayList<>(MODELS.keySet());
    }

    public static String modelId(String label) {
        return MODELS.get(label);
    }

    public String[] getAvailableModelNames() {
        return MODELS.keySet().toArray(new String[0]);
    }

    public static Identifier icon(String label) {
        if (label == null || label.isEmpty()) return null;
        return ICON_CACHE.computeIfAbsent(label, ItemReplacerModule::resolveIcon);
    }

    public ItemStack previewStack(String label) {
        String name = label != null && MODELS.containsKey(label) ? label : model.getValue();
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        stack.set(DataComponents.ITEM_MODEL, Identifier.parse("desire:item_replacer"));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(name), List.of()));
        return stack;
    }

    public ItemStack getRenderStack(ItemStack stack) {
        if (!isEnabled() || stack == null || stack.isEmpty() || !isTarget(stack)) return stack;
        Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);
        if (itemModel != null && "desire".equals(itemModel.getNamespace()) && "item_replacer".equals(itemModel.getPath())) {
            return stack;
        }
        ItemStack copy = stack.copy();
        copy.set(DataComponents.ITEM_MODEL, Identifier.parse("desire:item_replacer"));
        copy.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(model.getValue()), List.of()));
        return copy;
    }

    private boolean isTarget(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;

        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        boolean isSword = path.contains("sword");
        boolean isAxe = path.endsWith("_axe");
        if (type.is("Топор")) return isAxe;
        if (type.is("Оба")) return isSword || isAxe;
        return isSword;
    }

    private static Identifier resolveIcon(String label) {
        String modelId = MODELS.get(label);
        if (modelId == null) return null;
        List<String> candidates = new ArrayList<>();
        String fromJson = textureFromModelJson(modelId);
        if (fromJson != null) candidates.add(fromJson);
        candidates.add("textures/item/" + modelId + ".png");
        candidates.add("textures/item/" + modelId.replace('-', '_') + ".png");
        for (String path : candidates) {
            Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
            if (resourceExists(id)) return id;
        }
        return Identifier.fromNamespaceAndPath("minecraft", "textures/item/diamond_sword.png");
    }

    private static String textureFromModelJson(String modelId) {
        String path = "/assets/minecraft/models/item/" + modelId + ".json";
        try (InputStream input = ItemReplacerModule.class.getResourceAsStream(path)) {
            if (input == null) return null;
            JsonObject root = JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("textures") || !root.get("textures").isJsonObject()) return null;
            JsonObject textures = root.getAsJsonObject("textures");
            String key = textures.has("0") ? "0" : textures.has("layer0") ? "layer0" : textures.has("particle") ? "particle" : null;
            if (key == null) return null;
            String ref = textures.get(key).getAsString();
            if (ref.startsWith("item/")) return "textures/" + ref + ".png";
            if (ref.contains(":")) {
                int colon = ref.indexOf(':');
                String local = ref.substring(colon + 1);
                if (local.startsWith("item/")) return "textures/" + local + ".png";
                return "textures/item/" + local + ".png";
            }
            return "textures/item/" + ref + ".png";
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean resourceExists(Identifier id) {
        String path = "/assets/" + id.getNamespace() + "/" + id.getPath();
        try (InputStream stream = ItemReplacerModule.class.getResourceAsStream(path)) {
            return stream != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Map<String, String> createModels() {
        String[][] data = new String[][]{
            {"Abominable Blade", "abominableblade"},
            {"Abominable Greatsaber", "abominablegreatsaber"},
            {"Abominable Scythe", "abominablescythe"},
            {"Acid Demon", "aciddemon"},
            {"Amethyst Shuriken", "amethyst_shuriken"},
            {"Ancient Royal Greatsword", "ancient_royal_great_sword"},
            {"Aquantic Sacred Blade", "aquantic_sacred_blade"},
            {"Aquantic Trident", "aquantictrident"},
            {"Arcanethyst", "arcanethyst"},
            {"Ashura Blade", "ashura_blade"},
            {"Awakened Lichblade", "awakened_lichblade"},
            {"Blood Edge", "bloodedge"},
            {"Bloody Death", "bloodydeath"},
            {"Bramblethorn", "bramblethorn"},
            {"Brimstone Claymore", "brimstone_claymore"},
            {"Carian Sword", "cariansword"},
            {"Chrono Blade", "chrono_blade"},
            {"Corrupted Mythic Blade", "corruptedmythicblade"},
            {"Creation Splitter", "creationsplitter"},
            {"Crescent Rose", "crescentrose"},
            {"Cyber Katana", "cyberkatana"},
            {"Cyber Mantis Blade", "cybermantisblade"},
            {"Cybernetic Katana", "cybernetickatana"},
            {"Cybernetic Knife", "cyberneticknife"},
            {"Cybernetic Sawblade", "cyberneticsawblade"},
            {"Cyber Sword", "cybersword"},
            {"Dainsleif", "dainsleif"},
            {"Dark Blade", "dark_blade"},
            {"Dark Cleaver", "dark_cleaver"},
            {"Death Knight Dagger", "death_knight_dagger"},
            {"Death Knight Sword", "death_knight_sword"},
            {"Demigod's Unholy Blade", "demigodsunholyblade"},
            {"Demigod's Unholy Halberd", "demigodsunholyhalberd"},
            {"Demonic Blade", "demonicblade"},
            {"Demonic Cleaver", "demoniccleaver"},
            {"Demon Lord's Great Axe", "demonlordsgreataxe"},
            {"Demon Lord's Sword", "demonlordsword"},
            {"Divine Justice", "divine_justice"},
            {"Divine Reaper", "divine_reaper"},
            {"Divine Axe Rhitta", "divineaxerhitta"},
            {"Divine Punisher", "divinepunisher"},
            {"Dragon Slaying Blade", "dragonslayingblade"},
            {"Edge of the Astral Plane", "edgeoftheastralplane"},
            {"Emberblade", "emberblade"},
            {"Enigma", "enigma"},
            {"Epic Sword", "epicsword"},
            {"Estoc", "estoc"},
            {"Excalibur", "excalibur"},
            {"Fallen God Spear", "fallengodspear"},
            {"Fallen God Sword", "fallengodsword"},
            {"Floral Longsword", "floral_longsword"},
            {"Floral Sabre", "floral_sabre"},
            {"Forest Guardian Glaive", "forest_guardian_glaive"},
            {"Frost Axe", "frostaxe"},
            {"Frostblade", "frostblade"},
            {"Frost Scythe", "frostscythe"},
            {"Green Scythe", "greenscythe"},
            {"Hearthflame", "hearthflame"},
            {"Hero Sword", "herosword"},
            {"Holy Moonlight Sword", "holymoonlightsword"},
            {"Hornet's Needle", "hornetsneedle"},
            {"Ice Whisper", "icewhisper"},
            {"Jade Halberd", "jadehalberd"},
            {"Katana", "katana"},
            {"Legendary Sword", "legendarysword"},
            {"Longsword", "longsword"},
            {"Magi Scythe", "magiscythe"},
            {"Masamune", "masamune"},
            {"Mjolnir", "mjolnir"},
            {"Molten Blade", "moltenblade"},
            {"Molten Sword", "moltensword"},
            {"Muramasa", "muramasa"},
            {"Mystical Spellblade", "mysticalspellblade"},
            {"Mythic Blade", "mythicblade"},
            {"Partisan", "partisan"},
            {"Pharaoh's Treasure", "pharaohs_treasure"},
            {"Phoenix Grace", "pheonixgrace"},
            {"Powerfuse Hammer", "powerfusehammer"},
            {"Powerfuse Sword", "powerfusesword"},
            {"Requiem of Hell", "requiem_of_hell"},
            {"Ribbon Cleaver", "ribboncleaver"},
            {"Righteous Relic", "righteous_relic"},
            {"Rivers of Blood", "riversofblood"},
            {"Royal Chakram", "royalchakram"},
            {"Royal Rapier", "royalrapier"},
            {"Sabre", "sabre"},
            {"Scissor Blade", "scissorblade"},
            {"Sculk Cleaver", "sculkcleaver"},
            {"Sculk Scythe", "sculkscythe"},
            {"Sculk Sword", "sculksword"},
            {"Sentinel's Will", "sentinels_will"},
            {"Silverine Blade", "silverine_blade"},
            {"Soul Claws", "soulclaws"},
            {"Soul Edge", "souledge"},
            {"Soul Harvester", "soulharvester"},
            {"Soul Render", "soulrender"},
            {"Soul Stealer", "soulstealer"},
            {"Soul Collector", "soul_collector"},
            {"Soul Devourer", "soul_devourer"},
            {"Star's Edge", "stars_edge"},
            {"Steel Sword", "steelsword"},
            {"Stop Sign", "stop_sign"},
            {"Stormbringer", "stormbringer"},
            {"Storm's Edge", "storms_edge"},
            {"Sunbreak", "sunbreak"},
            {"Tengen's Blade", "tengensblade"},
            {"Terrablade", "terrablade"},
            {"Thousand Demon Daggers", "thousanddemondaggers"},
            {"Thunderbrand", "thunderbrand"},
            {"Thunderbringer", "thunderbringer"},
            {"Toxic Longsword", "toxic_longsword"},
            {"Vampiric Needle", "vampiricneedle"},
            {"Wakizashi", "wakizashi"},
            {"Watcher Claymore", "watcher_claymore"},
            {"Watching Warglaive", "watching_warglaive"},
            {"Waxweaver", "waxweaver"},
            {"Whisperwind", "whisperwind"},
            {"Wickpiercer", "wickpiercer"},
            {"Yoru", "yoru"}
        };
        Map<String, String> map = new LinkedHashMap<>();
        for (String[] row : data) map.put(row[0], row[1]);
        return map;
    }
}
