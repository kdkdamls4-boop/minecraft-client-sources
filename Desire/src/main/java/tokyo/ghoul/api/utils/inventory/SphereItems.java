package tokyo.ghoul.api.utils.inventory;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Сферы и талисманы FunTime: узнаются по строке лора. */
@UtilityClass
public class SphereItems {
    private final List<Entry> ENTRIES = List.of(
            new Entry("Талисман Крушителя", Items.TOTEM_OF_UNDYING, "Несокрушимая мощь", null),
            new Entry("Талисман Карателя", Items.TOTEM_OF_UNDYING, "Несёт строгий приговор", null),
            new Entry("Талисман Ярости", Items.TOTEM_OF_UNDYING, "Чистая, дикая агрессия", null),
            new Entry("Талисман Вихря", Items.TOTEM_OF_UNDYING, "Вихрь не знает покоя", null),
            new Entry("Талисман Раздора", Items.TOTEM_OF_UNDYING, "Раздор жаждет хаоса", null),
            new Entry("Талисман Тирана", Items.TOTEM_OF_UNDYING, "Тиран подавляет слабых", null),
            new Entry("Талисман Мрака", Items.TOTEM_OF_UNDYING, "Мрак сгущается рядом", null),
            new Entry("Талисман Демона", Items.TOTEM_OF_UNDYING, "Печать разжигает ярость", null),
            new Entry("Сфера Титана", Items.PLAYER_HEAD, "Мощь Титанов крепка",
                    "81e9698458b7841c96ae4f24ec84ae01724100641c564e2a7b185f406e8ed23"),
            new Entry("Сфера Гидры", Items.PLAYER_HEAD, "Живучесть темных глубин",
                    "3e3c118d696d910e54de02ca4d807543f9b18c008c9838d2ff69377622fb1d32"),
            new Entry("Сфера Хаоса", Items.PLAYER_HEAD, "Хаос искажает реальность",
                    "e7a7ae7cdcf616e8b7a4221a621b2435753c60ed6a258ea060dae3002ffe9e28"),
            new Entry("Сфера Ареса", Items.PLAYER_HEAD, "Дух Ареса пылает внутри",
                    "c16adc6bafcb57fd707dee7dd6a736fe126711d53a1fd6ce789da41b3be13f2a"),
            new Entry("Сфера Бестии", Items.PLAYER_HEAD, "Звериная дикая мощь",
                    "5411ac17381b9fce9bab3c72afdb7f198570daf4732bd811d31c227d80fa39b1"),
            new Entry("Сфера Икара", Items.PLAYER_HEAD, "Хранит волю Икара",
                    "c6803e6d5667a2d610628bc3b32f863cda495c465616de655cb329933b61af77"),
            new Entry("Сфера Сатира", Items.PLAYER_HEAD, "Шёпот Сатира звучит",
                    "771a9a498b4fa5ec49362f9bc88eda4f52b04de49d75aa3ca332a1fea1aa0e57"),
            new Entry("Сфера Эрида", Items.PLAYER_HEAD, "Холод Эриды вечен",
                    "6e4e2f1047f3ec6e9e459184739e33b7c1fc63ad8202bdab9f024508add23e5b")
    );

    public String[] ids() {
        List<String> ids = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (!ids.contains(entry.id())) {
                ids.add(entry.id());
            }
        }
        return ids.toArray(new String[0]);
    }

    public String[] talismanIds() {
        List<String> ids = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (entry.item() == Items.TOTEM_OF_UNDYING) {
                ids.add(entry.id());
            }
        }
        return ids.toArray(new String[0]);
    }

    /** id сферы/талисмана для стака или "" если не опознан. */
    public String idOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        for (Entry entry : ENTRIES) {
            if (entry.matches(stack)) {
                return entry.id();
            }
        }
        return "";
    }

    public ItemStack displayStack(String id) {
        for (Entry entry : ENTRIES) {
            if (entry.id().equals(id)) {
                return entry.createStack();
            }
        }
        return ItemStack.EMPTY;
    }

    private record Entry(String id, Item item, String lore, String skin) {
        boolean matches(ItemStack stack) {
            if (stack.getItem() != item) return false;
            ItemLore itemLore = stack.get(DataComponents.LORE);
            if (itemLore == null) return false;

            String needle = lore.toLowerCase(Locale.ROOT);
            for (Component line : itemLore.lines()) {
                String text = ChatFormatting.stripFormatting(line.getString());
                if (text != null && text.toLowerCase(Locale.ROOT).contains(needle)) {
                    return true;
                }
            }
            return false;
        }

        ItemStack createStack() {
            ItemStack stack = new ItemStack(item);
            if (skin != null) {
                String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + skin + "\"}}}";
                Property property = new Property("textures", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
                PropertyMap properties = new PropertyMap(ImmutableMultimap.of(property.name(), property));
                UUID uuid = UUID.nameUUIDFromBytes(skin.getBytes(StandardCharsets.UTF_8));
                stack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(uuid, "", properties)));
            }
            return stack;
        }
    }
}
