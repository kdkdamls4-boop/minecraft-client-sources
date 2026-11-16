package deluxe.client.util;

import deluxe.client.storage.AutoBuyItem;

import deluxe.client.MinecraftAccess;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Pair;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;

public final class AuctionItemCatalog implements MinecraftAccess {
   private static final ArrayList<AutoBuyItem> field02338 = new ArrayList<>();
   private static boolean field02339;
   public static final BiPredicate<ItemStack, AutoBuyItem> field02340 = (itemStack, autoBuyItem) -> {
      if (itemStack.getItem() != Items.PLAYER_HEAD || autoBuyItem.item() != Items.PLAYER_HEAD) {
         return false;
      } else if (autoBuyItem.attributes().isEmpty()) {
         return false;
      } else {
         return !AutoBuyItem.CHECKER.test(itemStack, autoBuyItem) ? false : method1052(itemStack, autoBuyItem.displayStack());
      }
   };

   public static boolean method1052(ItemStack itemStack, ItemStack itemStack2) {
      ProfileComponent profileComponent = itemStack.get(DataComponentTypes.PROFILE);
      ProfileComponent profileComponent2 = itemStack2.get(DataComponentTypes.PROFILE);
      if (profileComponent != null && profileComponent2 != null) {
         String local1436_4 = method1060(profileComponent);
         String local1436_5 = method1060(profileComponent2);
         return local1436_4 != null && local1436_4.equals(local1436_5);
      } else {
         return false;
      }
   }

   public static ArrayList<AuctionItemCatalog.Nested0366> method1053() {
      ArrayList<AuctionItemCatalog.Nested0366> local1437_0 = new ArrayList<>();
      method1069(local1437_0, "Броня");
      method1069(local1437_0, "Оружие");
      method1069(local1437_0, "Сферы");
      method1069(local1437_0, "Талисманы");
      method1069(local1437_0, "Зелья");
      method1069(local1437_0, "Прочее");
      return local1437_0;
   }

   private static AutoBuyItem.Nested0523 method1054(String local1438_0, int local1438_1, int local1438_2, int local1438_3, int local1438_4) {
      return new AutoBuyItem.Nested0523()
         .with__("Сферы")
         .method4223(method1062(local1438_0, local1438_1, local1438_2, local1438_3, local1438_4))
         .method4221(Items.PLAYER_HEAD);
   }

   public static AutoBuyItem method1055(String local1439_0) {
      return field02338.stream().filter(autoBuyItem -> autoBuyItem.name().equals(local1439_0)).findFirst().orElse(null);
   }

   public static void method1056() {
      if (!field02339) {
         field02338.clear();
         method1065(
            method1068(Items.NETHERITE_HELMET, "Броня")
               .method4227("Шлем Крушителя")
               .method4225(EnchantmentRequirement.of(Enchantments.RESPIRATION, 3), EnchantmentRequirement.of(Enchantments.AQUA_AFFINITY, 1))
               .method4222()
         );
         method1065(method1068(Items.NETHERITE_CHESTPLATE, "Броня").method4227("Нагрудник Крушителя").method4222());
         method1065(method1068(Items.NETHERITE_LEGGINGS, "Броня").method4227("Поножи Крушителя").method4222());
         method1065(
            method1068(Items.NETHERITE_BOOTS, "Броня")
               .method4227("Ботинки Крушителя")
               .method4225(
                  EnchantmentRequirement.of(Enchantments.FEATHER_FALLING, 4),
                  EnchantmentRequirement.of(Enchantments.SOUL_SPEED, 3),
                  EnchantmentRequirement.of(Enchantments.DEPTH_STRIDER, 3)
               )
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Оружие")
               .method4227("Меч Крушителя")
               .method4221(Items.NETHERITE_SWORD)
               .method4223(method1064(Items.NETHERITE_SWORD))
               .method4225(
                  EnchantmentRequirement.of(Enchantments.SHARPNESS, 7),
                  EnchantmentRequirement.of(Enchantments.SMITE, 7),
                  EnchantmentRequirement.of(Enchantments.BANE_OF_ARTHROPODS, 7),
                  EnchantmentRequirement.of(Enchantments.SWEEPING_EDGE, 3),
                  EnchantmentRequirement.of(Enchantments.FIRE_ASPECT, 2),
                  EnchantmentRequirement.of(Enchantments.LOOTING, 5),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .method4218("Детекция III", "Окисление II", "Вампиризм II", "Опытный III", "Яд III")
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Оружие")
               .method4227("Булава Крушителя")
               .method4221(Items.MACE)
               .method4223(method1064(Items.MACE))
               .method4225(
                  EnchantmentRequirement.of(Enchantments.SHARPNESS, 7),
                  EnchantmentRequirement.of(Enchantments.SMITE, 7),
                  EnchantmentRequirement.of(Enchantments.BANE_OF_ARTHROPODS, 7),
                  EnchantmentRequirement.of(Enchantments.DENSITY, 5),
                  EnchantmentRequirement.of(Enchantments.BREACH, 3),
                  EnchantmentRequirement.of(Enchantments.SWEEPING_EDGE, 3),
                  EnchantmentRequirement.of(Enchantments.FIRE_ASPECT, 2),
                  EnchantmentRequirement.of(Enchantments.LOOTING, 5),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .method4218("Опытный III", "Вампиризм II", "Окисление II", "Яд III", "Детекция III")
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Оружие")
               .method4227("Кирка Крушителя")
               .method4221(Items.NETHERITE_PICKAXE)
               .method4223(method1064(Items.NETHERITE_PICKAXE))
               .method4225(
                  EnchantmentRequirement.of(Enchantments.FORTUNE, 5),
                  EnchantmentRequirement.of(Enchantments.EFFICIENCY, 10),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .method4218("Паутина", "Магнит", "Пингер", "Опытный III", "Авто-Плавка", "Бульдозер II")
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Оружие")
               .method4227("Трезубец Крушителя")
               .method4221(Items.TRIDENT)
               .method4223(method1064(Items.TRIDENT))
               .method4225(
                  EnchantmentRequirement.of(Enchantments.SHARPNESS, 7),
                  EnchantmentRequirement.of(Enchantments.IMPALING, 5),
                  EnchantmentRequirement.of(Enchantments.LOYALTY, 3),
                  EnchantmentRequirement.of(Enchantments.CHANNELING, 1),
                  EnchantmentRequirement.of(Enchantments.FIRE_ASPECT, 2),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 5),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .method4218(
                  "Детекция III",
                  "Яд III",
                  "Возвращение",
                  "Вампиризм II",
                  "Опытный III",
                  "Окисление II",
                  "Ступор III",
                  "Притяжение II",
                  "Подрывник",
                  "Скаут III"
               )
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Оружие")
               .method4227("Арбалет Крушителя")
               .method4221(Items.CROSSBOW)
               .method4223(method1064(Items.CROSSBOW))
               .method4225(
                  EnchantmentRequirement.of(Enchantments.MULTISHOT, 1),
                  EnchantmentRequirement.of(Enchantments.PIERCING, 5),
                  EnchantmentRequirement.of(Enchantments.QUICK_CHARGE, 3),
                  EnchantmentRequirement.of(Enchantments.UNBREAKING, 3),
                  EnchantmentRequirement.of(Enchantments.MENDING, 1)
               )
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Оружие")
               .method4227("Заострённый меч")
               .method4221(Items.NETHERITE_SWORD)
               .method4223(method1064(Items.NETHERITE_SWORD))
               .method4225(EnchantmentRequirement.of(Enchantments.SHARPNESS, 8))
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Сферы")
               .method4227("Отмычка к сферам")
               .method4221(Items.TRIPWIRE_HOOK)
               .method4223(method1064(Items.TRIPWIRE_HOOK))
               .method4220(Pair.of("spookystash:stash_id", "spheres"))
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJTcGhlcmVBdGhlbmEiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTNmOWVlZGEzYmEyM2ZlMTQyM2M0MDM2ZTdkZDBhNzQ0NjFkZmY5NmJhZGM1YjJmMmI5ZmFhN2NjMTZmMzgyZiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  694031243,
                  1593913619,
                  -1794784623,
                  596248838
               )
               .method4227("Сфера Афины")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 3.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -2.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODY0MTkwMCwKICAicHJvZmlsZUlkIiA6ICIxNzRjZmRiNGEzY2I0M2I1YmZjZGU0MjRjM2JiMmM2ZSIsCiAgInByb2ZpbGVOYW1lIiA6ICJtYXJhZWwxOCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS9lN2E3YWU3Y2RjZjYxNmU4YjdhNDIyMWE2MjFiMjQzNTc1M2M2MGVkNmEyNThlYTA2MGRhZTMwMDJmZmU5ZTI4IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
                  -1582985800,
                  1375286202,
                  -1125478243,
                  595287099
               )
               .method4227("Сфера Хаоса")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.07, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.13, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 1.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.GRAVITY, 0.09, Operation.ADD_MULTIPLIED_BASE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODYwODUyOCwKICAicHJvZmlsZUlkIiA6ICJkMTQ4NjFiM2UwZmM0Njk5OTFlMTcyNTllMzdiZjZhZCIsCiAgInByb2ZpbGVOYW1lIiA6ICJyYXhpdG9jbCIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS83NzFhOWE0OThiNGZhNWVjNDkzNjJmOWJjODhlZGE0ZjUyYjA0ZGU0OWQ3NWFhM2NhMzMyYTFmZWExYWEwZTU3IiwKICAgICAgIm1ldGFkYXRhIiA6IHsKICAgICAgICAibW9kZWwiIDogInNsaW0iCiAgICAgIH0KICAgIH0KICB9Cn0=",
                  -2101547208,
                  2058105556,
                  -1495491604,
                  1400184240
               )
               .method4227("Сфера Сатира")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.JUMP_STRENGTH, -0.1, Operation.ADD_MULTIPLIED_BASE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0MzgzNDkzMCwKICAicHJvZmlsZUlkIiA6ICI1MzUzNWIxN2M0ZDY0NWQ0YWUwY2U2ZjM4Zjk0NTFjYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJVYml2aXMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTQxMWFjMTczODFiOWZjZTliYWIzYzcyYWZkYjdmMTk4NTcwZGFmNDczMmJkODExZDMxYzIyN2Q4MGZhMzliMSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  -1896649230,
                  -398640094,
                  -1320063783,
                  945063190
               )
               .method4227("Сфера Бестии")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ARMOR, 1.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzc3NDI1NSwKICAicHJvZmlsZUlkIiA6ICJhYWMxYjA2OWNkMjE0NWE2ODNlNzQxNzE4MDcxMGU4MiIsCiAgInByb2ZpbGVOYW1lIiA6ICJqdXNhbXUiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYzE2YWRjNmJhZmNiNTdmZDcwN2RlZTdkZDZhNzM2ZmUxMjY3MTFkNTNhMWZkNmNlNzg5ZGE0MWIzYmUxM2YyYSIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  2124864246,
                  465122563,
                  -1511078600,
                  -555424585
               )
               .method4227("Сфера Ареса")
               .method4219(
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, -2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 6.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODUzMjE4MywKICAicHJvZmlsZUlkIiA6ICI1OGZmZWI5NTMxNGQ0ODcwYTQwYjVjYjQyZDRlYTU5OCIsCiAgInByb2ZpbGVOYW1lIiA6ICJTa2luREJuZXQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2UzYzExOGQ2OTZkOTEwZTU0ZGUwMmNhNGQ4MDc1NDNmOWIxOGMwMDhjOTgzOGQyZmY2OTM3NzYyMmZiMWQzMiIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9",
                  -1308798306,
                  1128020291,
                  -1307054059,
                  -1317369961
               )
               .method4227("Сфера Гидры")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.SUBMERGED_MINING_SPEED, 0.5, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.OXYGEN_BONUS, 0.5, Operation.ADD_MULTIPLIED_BASE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDI3ODU4MjQ5MSwKICAicHJvZmlsZUlkIiA6ICJhZWNkODIxZTQyYzE0ZDJlOThmNTA1OTg1MWI5OWMzNyIsCiAgInByb2ZpbGVOYW1lIiA6ICJSb2RyaVgyMDc1IiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2M2ODAzZTZkNTY2N2EyZDYxMDYyOGJjM2IzMmY4NjNjZGE0OTVjNDY1NjE2ZGU2NTVjYjMyOTkzM2I2MWFmNzciLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                  1858435695,
                  -517000716,
                  -1346102858,
                  1389869685
               )
               .method4227("Сфера Икара")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlOTY5ODQ1OGI3ODQxYzk2YWU0ZjI0ZWM4NGFlMDE3MjQxMDA2NDFjNTY0ZTJhN2IxODVmNDA2ZThlZDIzIn19fQ==",
                  654012711,
                  1596536861,
                  -2049048826,
                  -850597571
               )
               .method4227("Сфера Титана")
               .method4219(
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR_TOUGHNESS, 2.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, -0.15, Operation.ADD_MULTIPLIED_BASE)
               )
               .method4222()
         );
         method1065(
            method1054(
                  "ewogICJ0aW1lc3RhbXAiIDogMTc1MDM0Mzg2MTE4NywKICAicHJvZmlsZUlkIiA6ICJlZGUyYzdhMGFjNjM0MTNiYjA5ZDNmMGJlZTllYzhlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ0aGVEZXZKYWRlIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzZlNGUyZjEwNDdmM2VjNmU5ZTQ1OTE4NDczOWUzM2I3YzFmYzYzYWQ4MjAyYmRhYjlmMDI0NTA4YWRkMjNlNWIiLAogICAgICAibWV0YWRhdGEiIDogewogICAgICAgICJtb2RlbCIgOiAic2xpbSIKICAgICAgfQogICAgfQogIH0KfQ==",
                  -122635583,
                  2062758360,
                  -2035264015,
                  874042263
               )
               .method4227("Сфера Эрида")
               .method4219(
                  AttributeBonus.of(EntityAttributes.LUCK, 1.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.BLOCK_INTERACTION_RANGE, 1.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Крушителя",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 3.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR_TOUGHNESS, 2.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Карателя",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 7.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Раздора",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, -3.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Тирана",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 2.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Ярости",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, -4.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 5.0, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Вихря",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 2.0, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.MOVEMENT_SPEED, 0.15, Operation.ADD_MULTIPLIED_BASE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Мрака",
                  AttributeBonus.of(EntityAttributes.MAX_HEALTH, 1.5, Operation.ADD_VALUE),
                  AttributeBonus.of(EntityAttributes.ARMOR, 1.5, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1071(
                  "Талисман Демона",
                  AttributeBonus.of(EntityAttributes.ATTACK_SPEED, 0.1, Operation.ADD_MULTIPLIED_BASE),
                  AttributeBonus.of(EntityAttributes.ATTACK_DAMAGE, 2.5, Operation.ADD_VALUE)
               )
               .method4222()
         );
         method1065(
            method1061(
                  "Хлопушка",
                  StatusEffectRequirement.of(StatusEffects.SLOWNESS, 10, 8),
                  StatusEffectRequirement.of(StatusEffects.SPEED, 20, 4),
                  StatusEffectRequirement.of(StatusEffects.BLINDNESS, 5, 8),
                  StatusEffectRequirement.of(StatusEffects.GLOWING, 180, 0)
               )
               .method4222()
         );
         method1065(
            method1061(
                  "Святая вода",
                  StatusEffectRequirement.of(StatusEffects.REGENERATION, 45, 0),
                  StatusEffectRequirement.of(StatusEffects.INVISIBILITY, 600, 1),
                  StatusEffectRequirement.instant(StatusEffects.INSTANT_HEALTH, 1)
               )
               .method4222()
         );
         method1065(
            method1061("Зелье гнева", StatusEffectRequirement.of(StatusEffects.STRENGTH, 30, 4), StatusEffectRequirement.of(StatusEffects.SLOWNESS, 30, 3)).method4222()
         );
         method1065(
            method1061(
                  "Зелье палладина",
                  StatusEffectRequirement.of(StatusEffects.RESISTANCE, 600, 0),
                  StatusEffectRequirement.of(StatusEffects.FIRE_RESISTANCE, 600, 0),
                  StatusEffectRequirement.of(StatusEffects.HEALTH_BOOST, 60, 2),
                  StatusEffectRequirement.of(StatusEffects.INVISIBILITY, 900, 0)
               )
               .method4222()
         );
         method1065(
            method1061(
                  "Зелье ассасина",
                  StatusEffectRequirement.of(StatusEffects.STRENGTH, 60, 3),
                  StatusEffectRequirement.of(StatusEffects.SPEED, 300, 2),
                  StatusEffectRequirement.of(StatusEffects.HASTE, 60, 0),
                  StatusEffectRequirement.instant(StatusEffects.INSTANT_DAMAGE, 1)
               )
               .method4222()
         );
         method1065(
            method1061(
                  "Зелье радиации",
                  StatusEffectRequirement.of(StatusEffects.POISON, 20, 0),
                  StatusEffectRequirement.of(StatusEffects.WITHER, 20, 0),
                  StatusEffectRequirement.of(StatusEffects.SLOWNESS, 20, 2),
                  StatusEffectRequirement.of(StatusEffects.HUNGER, 20, 4),
                  StatusEffectRequirement.of(StatusEffects.GLOWING, 20, 0)
               )
               .method4222()
         );
         method1065(
            method1061(
                  "Снотворное",
                  StatusEffectRequirement.of(StatusEffects.WEAKNESS, 90, 0),
                  StatusEffectRequirement.of(StatusEffects.MINING_FATIGUE, 10, 0),
                  StatusEffectRequirement.of(StatusEffects.WITHER, 90, 2),
                  StatusEffectRequirement.of(StatusEffects.BLINDNESS, 10, 0)
               )
               .method4222()
         );
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Чарка").method4221(Items.ENCHANTED_GOLDEN_APPLE).method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Золотое яблоко").method4221(Items.GOLDEN_APPLE).method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Золотая морковь").method4221(Items.GOLDEN_CARROT).method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Дезориентация").method4221(Items.ENDER_EYE).method4222());
         method1065(method1066("Прочее", Items.PHANTOM_MEMBRANE, "Божья Аура", "effect-item-god").method4222());
         method1065(method1066("Прочее", Items.PAPER, "Драконий скин", "trap-skin-item-dragon").method4223(method1064(Items.PAPER)).method4222());
         method1065(method1066("Прочее", Items.PAPER, "Неизбежный скин", "trap-skin-item-inevitable").method4223(method1064(Items.PAPER)).method4222());
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Прочее")
               .method4227("Серебро")
               .method4221(Items.IRON_NUGGET)
               .method4220(Pair.of("spookystash:currency", "silver"))
               .method4222()
         );
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Алмаз").method4221(Items.DIAMOND).method4222());
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Прочее")
               .method4227("Книга починка")
               .method4221(Items.ENCHANTED_BOOK)
               .method4225(EnchantmentRequirement.of(Enchantments.MENDING, 1))
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Прочее")
               .method4227("Элитры Крушителя")
               .method4221(Items.ELYTRA)
               .method4225(EnchantmentRequirement.of(Enchantments.UNBREAKING, 5), EnchantmentRequirement.of(Enchantments.MENDING, 1))
               .method4222()
         );
         method1065(
            method1066("Прочее", Items.GOLDEN_PICKAXE, "Божье касание", "spawner-item-spawner-break").method4223(method1064(Items.GOLDEN_PICKAXE)).method4222()
         );
         method1065(
            method1066("Прочее", Items.GOLDEN_PICKAXE, "Мощный удар", "bedrock-item-bedrock-break").method4223(method1064(Items.GOLDEN_PICKAXE)).method4222()
         );
         method1065(
            method1066("Прочее", Items.NETHERITE_PICKAXE, "Молот Тора", "radius-item-mega-buldozer")
               .method4223(method1064(Items.NETHERITE_PICKAXE))
               .method4222()
         );
         method1065(method1066("Прочее", Items.NETHERITE_SCRAP, "Трапка", "schematic-item-trap").method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Спавнер").method4221(Items.SPAWNER).method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Маяк").method4221(Items.BEACON).method4222());
         method1065(
            new AutoBuyItem.Nested0523().with__("Прочее").method4227("Череп визер-скелета").method4221(Items.WITHER_SKELETON_SKULL).method4222()
         );
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Голова дракона").method4221(Items.DRAGON_HEAD).method4222());
         method1065(method1066("Прочее", Items.TNT, "Таер блэк", "tnt-item-black").method4222());
         method1065(method1066("Прочее", Items.TNT, "Таер вайт", "tnt-item-white").method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Незеритовый слиток").method4221(Items.NETHERITE_INGOT).method4222());
         method1065(new AutoBuyItem.Nested0523().with__("Прочее").method4227("Изумрудная руда").method4221(Items.EMERALD_ORE).method4222());
         method1065(method1066("Прочее", Items.SNOWBALL, "Снежок заморозка", "effect-item-snowball").method4222());
         method1065(method1066("Прочее", Items.FIRE_CHARGE, "Огненный смерч", "effect-item-smerch").method4222());
         method1065(method1066("Прочее", Items.SUGAR, "Явная пыль", "effect-item-dust").method4222());
         method1065(method1066("Прочее", Items.DRIED_KELP, "Пласт", "schematic-item-plast").method4222());
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Прочее")
               .method4227("Кровавая стрела")
               .method4221(Items.TIPPED_ARROW)
               .method4224(
                  StatusEffectRequirement.of(StatusEffects.WEAKNESS, 3, 2),
                  StatusEffectRequirement.of(StatusEffects.BLINDNESS, 2, 0),
                  StatusEffectRequirement.of(StatusEffects.MINING_FATIGUE, 2, 0),
                  StatusEffectRequirement.of(StatusEffects.NAUSEA, 5, 0)
               )
               .method4222()
         );
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Прочее")
               .method4227("Мучительная стрела")
               .method4221(Items.TIPPED_ARROW)
               .method4224(
                  StatusEffectRequirement.of(StatusEffects.SLOWNESS, 5, 2),
                  StatusEffectRequirement.of(StatusEffects.WITHER, 5, 2),
                  StatusEffectRequirement.of(StatusEffects.POISON, 5, 2)
               )
               .method4222()
         );
         method1065(method1066("Прочее", Items.STRUCTURE_BLOCK, "Чанкер 1x1", "executable-block-chunker-1").method4222());
         method1065(method1066("Прочее", Items.JIGSAW, "Дамагер", "executable-block-damager").method4222());
         method1065(method1066("Прочее", Items.POPPED_CHORUS_FRUIT, "Модификатор варпов", "modifier-item-setwarp-day").method4222());
         method1065(
            new AutoBuyItem.Nested0523()
               .with__("Прочее")
               .method4221(Items.FEATHER)
               .method4227("Модификатор полёта")
               .method4220(Pair.of("spookyitems:spooky-item", "modifier-item-fly-day"))
               .method4222()
         );
         method1065(method1066("Прочее", Items.ENDER_EYE, "Модификатор эндер-сундука", "modifier-item-ec-day").method4222());
         method1065(method1066("Прочее", Items.BUBBLE_CORAL, "Модификатор починки", "modifier-item-fix-day").method4222());
         field02339 = mc.world != null;
      }
   }

   public static boolean method1059(ItemStack itemStack, AutoBuyItem autoBuyItem) {
      return method1076(autoBuyItem) ? field02340.test(itemStack, autoBuyItem) : AutoBuyItem.CHECKER.test(itemStack, autoBuyItem);
   }

   private static String method1060(ProfileComponent profileComponent) {
      for (Property property : profileComponent.getGameProfile().properties().get("textures")) {
         if ("textures".equals(property.name())) {
            return property.value();
         }
      }

      return null;
   }

   private AuctionItemCatalog() {
   }

   private static AutoBuyItem.Nested0523 method1061(String local1446_0, StatusEffectRequirement... recoveredClass0114Array) {
      return new AutoBuyItem.Nested0523()
         .with__("Зелья")
         .method4227(local1446_0)
         .method4221(Items.SPLASH_POTION)
         .method4223(method1070(recoveredClass0114Array))
         .method4224(recoveredClass0114Array);
   }

   private static ItemStack method1062(String local1447_0, int local1447_1, int local1447_2, int local1447_3, int local1447_4) {
      ItemStack itemStack = new ItemStack(Items.PLAYER_HEAD);
      itemStack.set(
         DataComponentTypes.PROFILE,
         ProfileComponent.ofStatic(
            new GameProfile(
               new UUID((long)local1447_1 << 32 | local1447_2 & 4294967295L, (long)local1447_3 << 32 | local1447_4 & 4294967295L),
               "",
               new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", local1447_0)))
            )
         )
      );
      return itemStack;
   }

   public static float method1063() {
      float local1448_0 = 0.0F;

      for (AuctionItemCatalog.Nested0366 local1448_2 : method1053()) {
         local1448_0 += 8.5F + local1448_2.items().size() * 17.5F + 4.0F;
      }

      return local1448_0;
   }

   private static ItemStack method1064(Item newItem) {
      ItemStack defaultStack = newItem.getDefaultStack();
      if (mc.world == null) {
         return defaultStack;
      }

      defaultStack.addEnchantment(mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 1);
      return defaultStack;
   }

   private static AutoBuyItem method1065(AutoBuyItem autoBuyItem) {
      field02338.add(autoBuyItem);
      return autoBuyItem;
   }

   private static AutoBuyItem.Nested0523 method1066(String __, Item newItem, String local1451_2, String local1451_3) {
      return new AutoBuyItem.Nested0523()
         .with__(__)
         .method4221(newItem)
         .method4227(local1451_2)
         .method4220(Pair.of("spookyitems:spooky-item", local1451_3));
   }

   private static AutoBuyItem.Nested0523 method1068(Item newItem, String __) {
      return new AutoBuyItem.Nested0523()
         .with__(__)
         .method4221(newItem)
         .method4223(method1064(newItem))
         .method4225(
            EnchantmentRequirement.of(Enchantments.PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.BLAST_PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.FIRE_PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.PROJECTILE_PROTECTION, 5),
            EnchantmentRequirement.of(Enchantments.MENDING, 1),
            EnchantmentRequirement.of(Enchantments.UNBREAKING, 5)
         );
   }

   private static void method1069(ArrayList<AuctionItemCatalog.Nested0366> local1454_0, String local1454_1) {
      ArrayList<AutoBuyItem> local1454_2 = field02338.stream()
         .filter(autoBuyItem -> autoBuyItem.category().equals(local1454_1))
         .collect(Collectors.toCollection(ArrayList::new));
      if (!local1454_2.isEmpty()) {
         local1454_0.add(new AuctionItemCatalog.Nested0366(local1454_1, local1454_2));
      }
   }

   private static ItemStack method1070(StatusEffectRequirement... recoveredClass0114Array) {
      ItemStack defaultStack = Items.SPLASH_POTION.getDefaultStack();
      ArrayList<StatusEffectInstance> local1455_2 = new ArrayList<>();

      for (StatusEffectRequirement statusEffectRequirement : recoveredClass0114Array) {
         local1455_2.add(statusEffectRequirement.effectInstance());
      }

      defaultStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.empty(), local1455_2, Optional.empty()));
      return defaultStack;
   }

   private static AutoBuyItem.Nested0523 method1071(String local1456_0, AttributeBonus... recoveredClass0021Array) {
      return new AutoBuyItem.Nested0523().with__("Талисманы").method4227(local1456_0).method4221(Items.TOTEM_OF_UNDYING).method4219(recoveredClass0021Array);
   }

   public static boolean method1072(Item newItem) {
      return newItem instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock;
   }

   public static float method1073(int local1459_0) {
      if (local1459_0 <= 0) {
         return 0.0F;
      }

      int local1459_1 = (local1459_0 + 1) / 2;
      return local1459_1 * 115.0F;
   }

   public static AutoBuyItem method1075(ItemStack itemStack) {
      if (itemStack.getItem() == Items.PLAYER_HEAD) {
         for (AutoBuyItem autoBuyItem : field02338) {
            if (method1078(autoBuyItem) && field02340.test(itemStack, autoBuyItem)) {
               return autoBuyItem;
            }
         }
      }

      return field02338.stream().filter(autoBuyItem2 -> {
         if (method1072(autoBuyItem2.item()) && method1072(itemStack.getItem())) {
            return true;
         } else {
            return method1078(autoBuyItem2) ? false : AutoBuyItem.CHECKER.test(itemStack, autoBuyItem2);
         }
      }).findFirst().orElse(null);
   }

   public static boolean method1076(AutoBuyItem autoBuyItem) {
      return method1078(autoBuyItem);
   }

   public static ArrayList<AutoBuyItem> method1077() {
      return field02338;
   }

   private static boolean method1078(AutoBuyItem autoBuyItem) {
      return autoBuyItem != null
         && autoBuyItem.item() == Items.PLAYER_HEAD
         && !autoBuyItem.attributes().isEmpty()
         && autoBuyItem.displayStack().get(DataComponentTypes.PROFILE) != null;
   }

   public record Nested0366(String title, ArrayList<AutoBuyItem> items) {
   }
}
