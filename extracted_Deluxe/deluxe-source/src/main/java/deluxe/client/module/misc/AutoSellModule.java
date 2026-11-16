package deluxe.client.module.misc;

import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.setting.NumericTextSetting;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.util.Stopwatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(
   name = "AutoSell",
   description = "Выставляет предмет из руки на аукцион по минимальной цене с процентом, по второму бинду продаёт весь инвентарь и содержимое шалкеров.",
   category = ModuleCategory.MISC
)
public final class AutoSellModule extends Module {
   private static AutoSellModule autoSellModule;
   private static final Pattern field04229 = Pattern.compile(".*максимальная цена на этот товар.*");
   private static final HashMap<String, String> field04230 = new HashMap<>();
   public NumberSetting numberSetting;
   public NumericTextSetting field04232;
   public NumericTextSetting field04233;
   public KeybindSetting field04234;
   public KeybindSetting field04235;
   private AutoSellModule.Nested0385 field04236 = AutoSellModule.Nested0385.IDLE;
   private final Stopwatch stopwatch = new Stopwatch();
   private final HashSet<String> field04238 = new HashSet<>();
   private final HashSet<Integer> field04239 = new HashSet<>();
   private boolean field04240;
   private boolean field04241;
   private boolean field04242;
   private int field04243 = -1;
   private int field04244 = -1;
   private boolean field04245;
   private String field04246 = "";
   private boolean field04247;
   private boolean field04248;
   private int field04249 = -1;
   private int field04250;
   private final EventListener<PlayerTickEvent> field04251 = playerTickEvent -> {
      if (mc.player != null) {
         boolean local2082_2 = this.field04234.getBinding().method2749(mc.getWindow().getHandle());
         if (local2082_2 && !this.field04241) {
            this.method1421(false);
         }

         this.field04241 = local2082_2;
         boolean local2082_3 = this.field04235.getBinding().method2749(mc.getWindow().getHandle());
         if (local2082_3 && !this.field04242) {
            this.method1421(true);
         }

         this.field04242 = local2082_3;
         switch (this.field04236) {
            case IDLE:
            default:
               break;
            case PICK:
               if (InventoryActionQueue.method2656().method2665() || InventorySlotManager.method4176().method4179() || !this.stopwatch.hasExceededMillis(1500L)) {
                  return;
               }

               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
                  return;
               }

               this.field04243 = -1;
               this.field04244 = -1;

               for (int index = 0; index <= 35; index++) {
                  if (!mc.player.getInventory().getStack(index).isEmpty()) {
                     if (Block.getBlockFromItem(mc.player.getInventory().getStack(index).getItem()) instanceof ShulkerBoxBlock) {
                        if (this.field04244 == -1 && !this.field04239.contains(index)) {
                           this.field04244 = index;
                        }
                     } else if (this.field04243 == -1
                        && !this.field04238.contains(mc.player.getInventory().getStack(index).getName().getString())) {
                        this.field04243 = index;
                     }
                  }
               }

               if (this.field04243 == -1 && this.field04244 == -1) {
                  this.field04236 = AutoSellModule.Nested0385.IDLE;
                  return;
               }

               if (this.field04243 == -1) {
                  this.field04239.add(this.field04244);
                  InventoryUtils.method0547(InventoryUtils.method0533(this.field04244), 1, SlotActionType.PICKUP, false);
                  this.stopwatch.reset();
                  this.field04236 = AutoSellModule.Nested0385.SHULKER;
                  return;
               }

               if (this.field04243 != InventoryUtils.method0554()) {
                  InventoryActionQueue.method2656().method2662(this.field04243, InventoryUtils.method0554(), 1);
               }

               this.stopwatch.reset();
               this.field04236 = AutoSellModule.Nested0385.SEARCH;
               break;
            case SEARCH:
               if (InventoryActionQueue.method2656().method2665() || InventorySlotManager.method4176().method4179() || !this.stopwatch.hasExceededMillis(200L)) {
                  return;
               }

               if (mc.player.getMainHandStack().isEmpty()) {
                  this.stopwatch.reset();
                  this.field04236 = this.field04240 ? AutoSellModule.Nested0385.PICK : AutoSellModule.Nested0385.IDLE;
                  return;
               }

               this.field04246 = mc.player.getMainHandStack().getName().getString();
               this.field04245 = false;
               this.method1419();
               this.stopwatch.reset();
               this.field04236 = AutoSellModule.Nested0385.WAIT_LOTS;
               break;
            case WAIT_LOTS:
               if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen
                  && AuctionHelperModule.method2310().method2331(genericContainerScreen.getTitle().getString())) {
                  if (!this.stopwatch.hasExceededMillis(250L)) {
                     return;
                  }

                  int local2082_5 = -1;

                  for (int index2 = 0; index2 < mc.player.currentScreenHandler.slots.size() - 36; index2++) {
                     ItemStack itemStack = mc.player.currentScreenHandler.getSlot(index2).getStack();
                     if (!itemStack.isEmpty()
                        && ItemStack.areItemsEqual(itemStack, mc.player.getMainHandStack())
                        && this.method1413(itemStack, mc.player.getMainHandStack())
                        && AuctionHelperModule.method2310().method2314(itemStack) > 0
                        && !AuctionHelperModule.method2310().method2321(itemStack.getName().getString()).contains("не актуален")
                        && (local2082_5 == -1 || AuctionHelperModule.method2310().method2314(itemStack) / Math.max(1, itemStack.getCount()) < local2082_5)) {
                        local2082_5 = AuctionHelperModule.method2310().method2314(itemStack) / Math.max(1, itemStack.getCount());
                     }
                  }

                  if (local2082_5 == -1) {
                     if (!this.stopwatch.hasExceededMillis(600L)) {
                        return;
                     }

                     mc.player.closeHandledScreen();
                     this.field04238.add(this.field04246);
                     this.stopwatch.reset();
                     this.field04236 = this.field04240 ? AutoSellModule.Nested0385.PICK : AutoSellModule.Nested0385.IDLE;
                     return;
                  }

                  mc.player.closeHandledScreen();
                  this.field04250 = local2082_5;
                  this.field04247 = false;
                  mc.player.networkHandler.sendChatCommand(this.method1412() + " " + this.method1406(local2082_5));
                  this.stopwatch.reset();
                  this.field04236 = AutoSellModule.Nested0385.AFTER_SELL;
                  break;
               }

               if (!this.field04245 && this.stopwatch.hasExceededMillis(600L) && mc.currentScreen == null) {
                  this.field04245 = true;
                  this.method1419();
                  return;
               }

               if (!this.stopwatch.hasExceededMillis(1800L)) {
                  return;
               }

               this.field04238.add(this.field04246);
               this.stopwatch.reset();
               this.field04236 = this.field04240 ? AutoSellModule.Nested0385.PICK : AutoSellModule.Nested0385.IDLE;
               return;
            case AFTER_SELL:
               if (!this.stopwatch.hasExceededMillis(100L)) {
                  return;
               }

               if (this.field04247) {
                  this.field04247 = false;
                  if (mc.player.getMainHandStack().getCount() > 1 && InventoryUtils.method0546(0, 35) != -1) {
                     this.field04249 = InventoryUtils.method0546(0, 35);
                     InventoryUtils.method0547(InventoryUtils.method0533(InventoryUtils.method0554()), 1, SlotActionType.PICKUP, false);
                     InventoryUtils.method0547(InventoryUtils.method0533(this.field04249), 0, SlotActionType.PICKUP, false);
                     this.field04248 = true;
                     this.stopwatch.reset();
                     return;
                  }

                  this.field04238.add(this.field04246);
                  this.stopwatch.reset();
                  this.field04236 = this.field04240 ? AutoSellModule.Nested0385.PICK : AutoSellModule.Nested0385.IDLE;
                  return;
               }

               if (this.field04248) {
                  if (!this.stopwatch.hasExceededMillis(300L)) {
                     return;
                  }

                  this.field04248 = false;
                  mc.player.networkHandler.sendChatCommand(this.method1412() + " " + this.method1406(this.field04250));
                  this.stopwatch.reset();
                  return;
               }

               if (!mc.player.getMainHandStack().isEmpty() && mc.player.getMainHandStack().getName().getString().equals(this.field04246)) {
                  if (!this.stopwatch.hasExceededMillis(700L)) {
                     return;
                  }

                  this.field04238.add(this.field04246);
               }

               this.stopwatch.reset();
               if (!this.field04240
                  && this.field04249 != -1
                  && !this.field04238.contains(this.field04246)
                  && !mc.player.getInventory().getStack(this.field04249).isEmpty()) {
                  int local2082_4 = this.field04249;
                  InventoryActionQueue.method2656().method2662(local2082_4, InventoryUtils.method0554(), 1);
                  this.field04249 = -1;
                  this.field04236 = AutoSellModule.Nested0385.SEARCH;
                  return;
               }

               this.field04236 = this.field04240 ? AutoSellModule.Nested0385.PICK : AutoSellModule.Nested0385.IDLE;
               break;
            case SHULKER:
               if (InventoryActionQueue.method2656().method2665() || InventorySlotManager.method4176().method4179()) {
                  return;
               }

               if (!(
                  mc.currentScreen instanceof GenericContainerScreen genericContainerScreen2
                     && !AuctionHelperModule.method2310().method2331(genericContainerScreen2.getTitle().getString())
               )) {
                  if (!this.stopwatch.hasExceededMillis(800L)) {
                     return;
                  }

                  if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
                     InventoryUtils.method0547(InventoryUtils.method0533(this.field04244), 0, SlotActionType.PICKUP, false);
                  }

                  this.stopwatch.reset();
                  this.field04236 = AutoSellModule.Nested0385.PICK;
                  return;
               }

               if (!this.stopwatch.hasExceededMillis(200L)) {
                  return;
               }

               if (InventoryUtils.method0546(0, 35) == -1) {
                  mc.player.closeHandledScreen();
                  this.stopwatch.reset();
                  this.field04236 = AutoSellModule.Nested0385.PICK;
                  return;
               }

               for (int index3 = 0; index3 < mc.player.currentScreenHandler.slots.size() - 36; index3++) {
                  if (mc.player.currentScreenHandler.getSlot(index3).hasStack()) {
                     InventoryUtils.method0547(index3, 0, SlotActionType.QUICK_MOVE, false);
                     this.stopwatch.reset();
                     return;
                  }
               }

               mc.player.closeHandledScreen();
               this.stopwatch.reset();
               this.field04236 = AutoSellModule.Nested0385.PICK;
         }
      }
   };
   private final EventListener<PacketEvent> field04252 = packetEvent -> {
      if (this.field04236 == AutoSellModule.Nested0385.AFTER_SELL
         && packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         if (field04229.matcher(AuctionHelperModule.method2310().method2321(gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", ""))).matches()) {
            this.field04247 = true;
         }
      }
   };
   private static final HashMap<String, String> field04253 = new HashMap<>();
   private static final HashMap<String, String> field04254 = new HashMap<>();

   private int method1406(int local2070_1) {
      return Math.max(1, (int)Math.ceil(local2070_1 * (1.0F + this.numberSetting.getValue() / 100.0F)))
         * Math.max(1, mc.player.getMainHandStack().getCount());
   }

   static {
      field04230.put(
         "minecraft:netherite_helmet minecraft:aqua_affinity1 minecraft:blast_protection5 minecraft:fire_protection5 minecraft:mending1 minecraft:projectile_protection5 minecraft:protection5 minecraft:respiration3 minecraft:unbreaking5",
         "Шлем Крушителя"
      );
      field04230.put(
         "minecraft:netherite_chestplate minecraft:blast_protection5 minecraft:fire_protection5 minecraft:mending1 minecraft:projectile_protection5 minecraft:protection5 minecraft:unbreaking5",
         "Нагрудник Крушителя"
      );
      field04230.put(
         "minecraft:netherite_leggings minecraft:blast_protection5 minecraft:fire_protection5 minecraft:mending1 minecraft:projectile_protection5 minecraft:protection5 minecraft:unbreaking5",
         "Поножи Крушителя"
      );
      field04230.put(
         "minecraft:netherite_boots minecraft:blast_protection5 minecraft:depth_strider3 minecraft:feather_falling4 minecraft:fire_protection5 minecraft:mending1 minecraft:projectile_protection5 minecraft:protection5 minecraft:soul_speed3 minecraft:unbreaking5",
         "Ботинки Крушителя"
      );
      field04230.put(
         "minecraft:netherite_sword minecraft:bane_of_arthropods7 minecraft:fire_aspect2 minecraft:looting5 minecraft:mending1 minecraft:sharpness7 minecraft:smite7 minecraft:sweeping_edge3 minecraft:unbreaking5",
         "Меч Крушителя"
      );
      field04230.put("minecraft:netherite_pickaxe minecraft:efficiency10 minecraft:fortune5 minecraft:mending1 minecraft:unbreaking5", "Кирка Крушителя");
      field04230.put(
         "minecraft:trident minecraft:channeling1 minecraft:fire_aspect2 minecraft:impaling5 minecraft:loyalty3 minecraft:mending1 minecraft:sharpness7 minecraft:unbreaking5",
         "Трезубец Крушителя"
      );
      field04230.put(
         "minecraft:crossbow minecraft:mending1 minecraft:multishot1 minecraft:piercing5 minecraft:quick_charge3 minecraft:unbreaking3", "Арбалет Крушителя"
      );
      field04230.put("minecraft:bow minecraft:flame1 minecraft:infinity1 minecraft:mending1 minecraft:power5 minecraft:unbreaking5", "Лук Крушителя");
      field04230.put("minecraft:elytra minecraft:mending1 minecraft:unbreaking5", "Элитры Крушителя");
      field04253.put("Хаос искажает реальность", "сфера Хаоса");
      field04253.put("Шёпот Сатира звучит", "сфера Сатира");
      field04253.put("Звериная дикая мощь", "сфера Бестии");
      field04253.put("Дух Ареса пылает внутри", "сфера Ареса");
      field04253.put("Живучесть темных глубин", "сфера Гидры");
      field04253.put("Хранит волю Икара", "сфера Икара");
      field04253.put("Мощь Титанов крепка", "сфера Титана");
      field04253.put("Холод Эриды вечен", "сфера Эрида");
      field04253.put("Несокрушимая мощь", "талисман Крушителя");
      field04253.put("Раздор жаждает хаоса", "талисман Раздора");
      field04253.put("Тиран подавляет слабых", "талисман Тирана");
      field04253.put("Чистая, дикая агрессия", "талисман Ярости");
      field04253.put("Вихрь не знает покоя", "талисман Вихря");
      field04253.put("Мрак сгущается рядом", "талисман Мрака");
      field04253.put("Несёт строгий приговор", "талисман Карателя");
      field04253.put("Печать разжигает ярость", "талисман Демона");
      field04253.put("Иссушение III (0:05)", "Мучительная стрела");
      field04253.put("Мучительная стрела", "Мучительная стрела");
      field04253.put("Замедление III (0:03)", "Стрела Зевса");
      field04253.put("Стрела Зевса", "Стрела Зевса");
      field04253.put("Тошнота (0:05)", "Кровавая стрела");
      field04253.put("Кровавая стрела", "Кровавая стрела");
      field04253.put("Стрела обледенения", "Стрела обледенения");
      field04253.put("Спешка III (0:05)", "Световая стрела");
      field04253.put("Световая стрела", "Световая стрела");
      field04253.put("Замедление V (0:07)", "Стрела терапии");
      field04253.put("Стрела терапии", "Стрела терапии");
      field04254.put("Замедление III\nИссушение III\nОтравление III", "Мучительная стрела");
      field04254.put("Замедление III\nМоментальный урон II", "стрела Зевса");
      field04254.put("Слабость III\nСлепота\nТошнота\nУтомление", "Кровавая стрела");
      field04254.put("Скорость V\nСлепота\nСпешка III", "Световая стрела");
      field04254.put("Замедление V\nИсцеление\nСлабость", "стрела терапии");
      field04254.put("Замедление IX\nУтомление III", "стрела обледенения");
      field04254.put("Замедление potion.potency.8\nУтомление III", "стрела обледенения");
   }

   private String method1407(ItemStack itemStack) {
      Matcher matcher = Pattern.compile(
            "(?i)(Сфера|Талисман) (Хаоса|Сатира|Бестии|Ареса|Гидры|Икара|Титана|Эрида|Крушителя|Раздора|Тирана|Ярости|Вихря|Мрака|Карателя|Демона)"
         )
         .matcher(Formatting.strip(itemStack.getName().getString()));
      if (matcher.find()) {
         return matcher.group(1).toLowerCase() + " " + matcher.group(2);
      }

      for (Entry<String, String> local2072_4 : field04253.entrySet()) {
         if (Formatting.strip(itemStack.getName().getString()).contains(local2072_4.getKey())) {
            return local2072_4.getValue();
         }
      }

      if (itemStack.get(DataComponentTypes.LORE) == null) {
         return "";
      }

      for (Text text : itemStack.get(DataComponentTypes.LORE).lines()) {
         for (Entry<String, String> local2072_6 : field04253.entrySet()) {
            if (Formatting.strip(text.getString()).contains(local2072_6.getKey())) {
               return local2072_6.getValue();
            }
         }
      }

      return "";
   }

   private int method1408(ItemStack itemStack, String local2073_2) {
      for (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<RegistryEntry<Enchantment>> local2073_4 : itemStack.getEnchantments().getEnchantmentEntries()) {
         if (((RegistryEntry)local2073_4.getKey()).getIdAsString().equals(local2073_2)) {
            return local2073_4.getIntValue();
         }
      }

      return 0;
   }

   private HashSet<String> method1410(ItemStack itemStack) {
      NbtCompound compoundOrEmpty = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT)
         .copyNbt()
         .getCompoundOrEmpty("PublicBukkitValues");
      HashSet<String> local2075_3 = new HashSet<>();

      for (String local2075_5 : compoundOrEmpty.getKeys()) {
         if (local2075_5.startsWith("spookyenchantments:") && local2075_5.endsWith("lvl")) {
            local2075_3.add(local2075_5 + compoundOrEmpty.getInt(local2075_5, 0));
         }
      }

      return local2075_3;
   }

   private String method1411(ItemStack itemStack) {
      if (!this.method1420(itemStack).isEmpty()) {
         return field04254.getOrDefault(this.method1420(itemStack), this.method1420(itemStack).split("\n")[0]);
      } else if (!this.method1407(itemStack).isEmpty()) {
         return this.method1407(itemStack);
      } else {
         return this.method1414(itemStack).isEmpty()
            ? Formatting.strip(itemStack.getName().getString()).replaceAll("[^\\p{L} ]", " ").replaceAll(" +", " ").trim()
            : this.method1414(itemStack);
      }
   }

   private String method1412() {
      return this.field04233.getValue().isBlank()
         ? "ah sell"
         : (this.field04233.getValue().trim().startsWith("/") ? this.field04233.getValue().trim().substring(1) : this.field04233.getValue().trim());
   }

   private boolean method1413(ItemStack itemStack, ItemStack itemStack2) {
      if (!this.method1420(itemStack2).isEmpty()) {
         return this.method1420(itemStack2).equals(this.method1420(itemStack));
      } else if (!this.method1407(itemStack2).isEmpty()) {
         return this.method1407(itemStack2).equals(this.method1407(itemStack));
      } else {
         return !this.method1414(itemStack2).isEmpty()
            ? this.method1414(itemStack2).equals(this.method1414(itemStack))
            : itemStack.getEnchantments().equals(itemStack2.getEnchantments())
               && itemStack.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
                  .equals(itemStack2.getOrDefault(DataComponentTypes.STORED_ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT))
               && this.method1410(itemStack).equals(this.method1410(itemStack2))
               && this.method1418(itemStack).equals(this.method1418(itemStack2));
      }
   }

   private String method1414(ItemStack itemStack) {
      for (Entry<String, String> local2079_3 : field04230.entrySet()) {
         if (this.method1415(itemStack, local2079_3.getKey())) {
            return local2079_3.getValue();
         }
      }

      return "";
   }

   private boolean method1415(ItemStack itemStack, String local2080_2) {
      for (String local2080_6 : local2080_2.split(" ")) {
         if (!Character.isDigit(local2080_6.charAt(local2080_6.length() - 1))) {
            if (!Registries.ITEM.getId(itemStack.getItem()).toString().equals(local2080_6)) {
               return false;
            }
         } else if (this.method1408(itemStack, local2080_6.replaceAll("\\d+$", "")) < Integer.parseInt(local2080_6.replaceAll("^.*?(\\d+)$", "$1"))) {
            return false;
         }
      }

      return true;
   }

   private AutoSellModule() {
      this.numberSetting = this.addSetting(
         new NumberSetting("Процент к цене", "Сколько процентов накинуть на минимальную цену лота, минус выставлять дешевле.")
            .withIntegerRange(-90, 300)
            .withSuffix("%")
            .setValue(0.0F)
      );
      this.field04232 = this.addSetting(new NumericTextSetting("Команда поиска", "Команда, к которой допишется имя предмета из руки.", "ah search", 50));
      this.field04233 = this.addSetting(new NumericTextSetting("Команда продажи", "Команда, к которой допишется посчитанная цена.", "ah sell", 50));
      this.field04234 = this.addSetting(new KeybindSetting("Продать с руки", "Выставляет предмет из главной руки."));
      this.field04235 = this.addSetting(
         new KeybindSetting("Продать инвентарь", "Выставляет весь инвентарь и содержимое шалкеров, сами шалкеры не продаёт.")
      );
   }

   @Override
   protected void onDisable() {
      this.field04236 = AutoSellModule.Nested0385.IDLE;
   }

   public static AutoSellModule method1417() {
      if (autoSellModule == null) {
         autoSellModule = new AutoSellModule();
      }

      return autoSellModule;
   }

   private HashSet<String> method1418(ItemStack itemStack) {
      HashSet<String> local2085_2 = new HashSet<>();

      for (StatusEffectInstance statusEffectInstance : itemStack.getOrDefault(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent.DEFAULT).getEffects()) {
         local2085_2.add(statusEffectInstance.getEffectType().getIdAsString() + statusEffectInstance.getAmplifier() + "/" + statusEffectInstance.getDuration());
      }

      return local2085_2;
   }

   private void method1419() {
      if (mc.player != null && !mc.player.getMainHandStack().isEmpty()) {
         PotionContentsComponent potionContentsComponent = mc.player.getMainHandStack().get(DataComponentTypes.POTION_CONTENTS);
         if (potionContentsComponent != null) {
            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
               if (statusEffectInstance.getEffectType().value() == StatusEffects.INSTANT_HEALTH.value()
                  || statusEffectInstance.getEffectType().value() == StatusEffects.STRENGTH.value()) {
                  mc.player
                     .networkHandler
                     .sendChatCommand(
                        (
                              this.field04232.getValue().isBlank()
                                 ? "ah search"
                                 : (
                                    this.field04232.getValue().trim().startsWith("/")
                                       ? this.field04232.getValue().trim().substring(1)
                                       : this.field04232.getValue().trim()
                                 )
                           )
                           + " "
                           + (statusEffectInstance.getEffectType().value() == StatusEffects.STRENGTH.value() ? "Зелье силы" : "Зелье исцеления")
                     );
                  return;
               }
            }
         }

         if (!this.method1411(mc.player.getMainHandStack()).isEmpty()) {
            mc.player
               .networkHandler
               .sendChatCommand(
                  (
                        this.field04232.getValue().isBlank()
                           ? "ah search"
                           : (
                              this.field04232.getValue().trim().startsWith("/")
                                 ? this.field04232.getValue().trim().substring(1)
                                 : this.field04232.getValue().trim()
                           )
                     )
                     + " "
                     + this.method1411(mc.player.getMainHandStack())
               );
         }
      }
   }

   private String method1420(ItemStack itemStack) {
      if (!itemStack.isOf(Items.TIPPED_ARROW)) {
         return "";
      }

      String[] stringArray = new String[]{"", " II", " III", " IV", " V", " VI", " VII", " VIII", " IX", " X"};
      ArrayList<String> local2087_3 = new ArrayList<>();
      if (itemStack.get(DataComponentTypes.POTION_CONTENTS) != null) {
         for (StatusEffectInstance statusEffectInstance : itemStack.get(DataComponentTypes.POTION_CONTENTS).getEffects()) {
            local2087_3.add(
               statusEffectInstance.getEffectType().value().getName().getString().replace("Мгновенное исцеление", "Исцеление")
                  + stringArray[Math.max(0, Math.min(9, statusEffectInstance.getAmplifier()))]
            );
         }
      }

      if (local2087_3.isEmpty() && itemStack.get(DataComponentTypes.LORE) != null) {
         for (Text text : itemStack.get(DataComponentTypes.LORE).lines()) {
            if (Formatting.strip(text.getString()).trim().isEmpty()) {
               break;
            }

            local2087_3.add(Formatting.strip(text.getString()).trim().replaceAll("\\s*\\(.*", ""));
         }
      }

      Collections.sort(local2087_3);
      return String.join("\n", local2087_3);
   }

   private void method1421(boolean local2088_1) {
      if (this.field04236 != AutoSellModule.Nested0385.IDLE) {
         this.field04236 = AutoSellModule.Nested0385.IDLE;
      } else {
         this.field04240 = local2088_1;
         this.field04238.clear();
         this.field04239.clear();
         this.field04247 = false;
         this.field04248 = false;
         this.field04249 = -1;
         this.stopwatch.reset();
         this.field04236 = local2088_1 ? AutoSellModule.Nested0385.PICK : AutoSellModule.Nested0385.SEARCH;
      }
   }

   private enum Nested0385 {
      IDLE,
      PICK,
      SEARCH,
      WAIT_LOTS,
      AFTER_SELL,
      SHULKER;
   }
}
