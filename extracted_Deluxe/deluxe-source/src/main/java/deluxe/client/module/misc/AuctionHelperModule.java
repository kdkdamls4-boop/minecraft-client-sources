package deluxe.client.module.misc;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.ContainerRenderEvent;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiChoiceSetting;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.setting.NumericTextSetting;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FlintAndSteelItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

@ModuleInfo(
   name = "AuctionHelper",
   description = "Подсвечивает самые выгодные лоты на аукционе, фильтрует их по зачарованиям и ищет предмет из руки по бинду.",
   category = ModuleCategory.MISC
)
public final class AuctionHelperModule extends Module {
   private static AuctionHelperModule auctionHelperModule;
   public BooleanSetting field08075;
   public NumberSetting field08076;
   public BooleanSetting field08077;
   public BooleanSetting field08078;
   public BooleanSetting field08079;
   public ModeSetting modeSetting;
   public NumberSetting field08081;
   public BooleanSetting field08082;
   public MultiChoiceSetting field08083;
   public MultiChoiceSetting field08084;
   public MultiChoiceSetting field08085;
   public MultiChoiceSetting field08086;
   public MultiChoiceSetting field08087;
   public MultiChoiceSetting field08088;
   public MultiChoiceSetting field08089;
   public NumericTextSetting numericTextSetting;
   public KeybindSetting keybindSetting;
   private final Pattern field08092 = Pattern.compile("(\\d+)\\*(\\d+)");
   private boolean field08093;
   private final EventListener<PacketEvent> field08094 = packetEvent -> {
      if (packetEvent.isOutgoing() && this.field08075.isChecked() && packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandExecutionC2SPacket) {
         String local3610_3 = this.method2311(commandExecutionC2SPacket.command());
         if (!local3610_3.equals(commandExecutionC2SPacket.command())) {
            packetEvent.setPacket(new CommandExecutionC2SPacket(local3610_3));
         }
      }
   };
   private final EventListener<PlayerTickEvent> field08095 = playerTickEvent -> {
      boolean local3600_2 = this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
      if (local3600_2 && !this.field08093 && mc.currentScreen == null) {
         this.method2315();
      }

      this.field08093 = local3600_2;
   };
   private final EventListener<ContainerRenderEvent> field08096 = containerRenderEvent -> {
      if (mc.player != null
         && mc.world != null
         && containerRenderEvent.method3212() instanceof GenericContainerScreen genericContainerScreen
         && this.method2331(genericContainerScreen.getTitle().getString())) {
         Optional<Integer> local3617_3 = this.method2304();
         List<AuctionHelperModule.Nested0427> local3617_4 = genericContainerScreen.getScreenHandler()
            .slots
            .stream()
            .filter(newSlot -> newSlot.hasStack() && this.method2303(newSlot.getStack()))
            .map(newSlot2 -> new AuctionHelperModule.Nested0427(this.method2314(newSlot2.getStack()), newSlot2))
            .filter(
               local3619_2 -> local3619_2.price() >= 0 && (!this.field08077.isChecked() || local3617_3.isEmpty() || local3619_2.price() <= local3617_3.get())
            )
            .toList();
         this.method2333(
            containerRenderEvent.method3213(), local3617_4.stream().min(Comparator.comparingInt(AuctionHelperModule.Nested0427::price)).orElse(null), -16711936
         );
         this.method2333(
            containerRenderEvent.method3213(),
            local3617_4.stream()
               .filter(local3611_0 -> local3611_0.slot().getStack().getCount() > 1)
               .min(Comparator.comparingDouble(local3618_0 -> (double)local3618_0.price() / local3618_0.slot().getStack().getCount()))
               .orElse(null),
            -16711681
         );
      }
   };

   private boolean method2303(ItemStack itemStack) {
      if (itemStack.isOf(Items.GRAY_DYE) && itemStack.getName().getString().contains("Товар не актуален")) {
         return false;
      }

      if (itemStack.isDamageable()
         && (float)(itemStack.getMaxDamage() - itemStack.getDamage()) / itemStack.getMaxDamage() < this.field08076.floatValue() / 100.0F) {
         return false;
      }

      if (this.field08078.isChecked() && itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT).isEmpty()) {
         return false;
      }

      if (this.field08079.isChecked()) {
         if (this.modeSetting.isSelected("Точно") && itemStack.getCount() != this.field08081.intValue()) {
            return false;
         }

         if (this.modeSetting.isSelected("Минимум") && itemStack.getCount() < this.field08081.intValue()) {
            return false;
         }
      }

      if (!this.field08082.isChecked()) {
         return true;
      }

      Item item = itemStack.getItem();
      if (this.field08089.matchesOption("Стрелы") && item instanceof ArrowItem) {
         return false;
      }

      if (this.field08089.matchesOption("Пузырьки") && itemStack.isOf(Items.GLASS_BOTTLE)) {
         return false;
      }

      if (this.field08089.matchesOption("Дыхание дракона") && itemStack.isOf(Items.DRAGON_BREATH)) {
         return false;
      }

      if (this.method2320(itemStack)) {
         if (this.field08089.matchesOption("Броня")) {
            return false;
         }

         if (this.field08083.matchesOption("Без шипов") && this.method2329(itemStack, "minecraft:thorns") != -1) {
            return false;
         }

         if (this.field08083.matchesOption("Защита V") && this.method2329(itemStack, "minecraft:protection") < 5) {
            return false;
         }

         if (this.field08083.matchesOption("Прочность V") && this.method2329(itemStack, "minecraft:unbreaking") < 5) {
            return false;
         }

         if (this.field08083.matchesOption("Починка") && this.method2329(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.field08083.matchesOption("Починка II") && this.method2329(itemStack, "minecraft:mending") < 2) {
            return false;
         }
      }

      if (itemStack.isOf(Items.ELYTRA)) {
         if (this.field08089.matchesOption("Элитры")) {
            return false;
         }

         if (this.field08084.matchesOption("Прочность V") && this.method2329(itemStack, "minecraft:unbreaking") < 5) {
            return false;
         }

         if (this.field08084.matchesOption("Починка") && this.method2329(itemStack, "minecraft:mending") == -1) {
            return false;
         }
      }

      if (this.field08089.matchesOption("Тотемы") && itemStack.isOf(Items.TOTEM_OF_UNDYING)) {
         return false;
      }

      if (this.field08089.matchesOption("Головы") && itemStack.isOf(Items.PLAYER_HEAD)) {
         return false;
      }

      if (this.field08089.matchesOption("Трезубцы") && item instanceof TridentItem) {
         return false;
      }

      if (this.field08089.matchesOption("Луки") && item instanceof BowItem) {
         return false;
      }

      if (this.field08089.matchesOption("Арбалеты") && item instanceof CrossbowItem) {
         return false;
      }

      if (this.field08089.matchesOption("Огниво") && item instanceof FlintAndSteelItem) {
         return false;
      }

      if (this.field08089.matchesOption("Конская броня")
         && "minecraft".equals(Registries.ITEM.getId(item).getNamespace())
         && Registries.ITEM.getId(item).getPath().endsWith("_horse_armor")) {
         return false;
      }

      if (itemStack.isIn(ItemTags.SWORDS)) {
         if (this.field08089.matchesOption("Мечи")) {
            return false;
         }

         if (this.field08085.matchesOption("С отдачей") && this.method2329(itemStack, "minecraft:knockback") == -1) {
            return false;
         }

         if (this.field08085.matchesOption("Без отдачи") && this.method2329(itemStack, "minecraft:knockback") != -1) {
            return false;
         }

         if (this.field08085.matchesOption("Острота") && this.method2329(itemStack, "minecraft:sharpness") == -1) {
            return false;
         }

         if (this.field08085.matchesOption("Прочность") && this.method2329(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.field08085.matchesOption("Яд") && this.method2324(itemStack, "poison") == -1) {
            return false;
         }

         if (this.field08085.matchesOption("Обнаружение") && this.method2324(itemStack, "detection") == -1) {
            return false;
         }

         if (this.field08085.matchesOption("Окисление") && this.method2324(itemStack, "oxidation") == -1) {
            return false;
         }

         if (this.field08085.matchesOption("Вампиризм") && this.method2324(itemStack, "vampirism") == -1) {
            return false;
         }
      }

      if (itemStack.isIn(ItemTags.PICKAXES)) {
         if (this.field08089.matchesOption("Кирки")) {
            return false;
         }

         if (this.field08086.matchesOption("Эффективность") && this.method2329(itemStack, "minecraft:efficiency") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Починка") && this.method2329(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Удача") && this.method2329(itemStack, "minecraft:fortune") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Прочность") && this.method2329(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Шёлковое касание") && this.method2329(itemStack, "minecraft:silk_touch") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Переплавка") && this.method2324(itemStack, "smelting") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Магнит") && this.method2324(itemStack, "magnet") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Бульдозер") && this.method2324(itemStack, "buldozing") == -1 && this.method2324(itemStack, "megabuldozing") == -1) {
            return false;
         }

         if (this.field08086.matchesOption("Без тяжести") && this.method2324(itemStack, "heavy") != -1) {
            return false;
         }
      }

      if (itemStack.isIn(ItemTags.AXES)) {
         if (this.field08089.matchesOption("Топоры")) {
            return false;
         }

         if (this.field08087.matchesOption("Эффективность") && this.method2329(itemStack, "minecraft:efficiency") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Починка") && this.method2329(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Удача") && this.method2329(itemStack, "minecraft:fortune") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Прочность") && this.method2329(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Магнит") && this.method2324(itemStack, "magnet") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Лесоруб") && this.method2324(itemStack, "lumberjack") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Пингер") && this.method2324(itemStack, "pinger") == -1) {
            return false;
         }

         if (this.field08087.matchesOption("Бульдозер") && this.method2324(itemStack, "buldozing") == -1 && this.method2324(itemStack, "megabuldozing") == -1) {
            return false;
         }
      }

      if (itemStack.isIn(ItemTags.SHOVELS)) {
         if (this.field08089.matchesOption("Лопаты")) {
            return false;
         }

         if (this.field08088.matchesOption("Эффективность") && this.method2329(itemStack, "minecraft:efficiency") == -1) {
            return false;
         }

         if (this.field08088.matchesOption("Починка") && this.method2329(itemStack, "minecraft:mending") == -1) {
            return false;
         }

         if (this.field08088.matchesOption("Удача") && this.method2329(itemStack, "minecraft:fortune") == -1) {
            return false;
         }

         if (this.field08088.matchesOption("Прочность") && this.method2329(itemStack, "minecraft:unbreaking") == -1) {
            return false;
         }

         if (this.field08088.matchesOption("Магнит") && this.method2324(itemStack, "magnet") == -1) {
            return false;
         }

         if (this.field08088.matchesOption("Бульдозер") && this.method2324(itemStack, "buldozing") == -1 && this.method2324(itemStack, "megabuldozing") == -1) {
            return false;
         }
      }

      return !itemStack.isIn(ItemTags.HOES) || !this.field08089.matchesOption("Мотыги");
   }

   private Optional<Integer> method2304() {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objectiveForSlot = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      return objectiveForSlot == null
         ? Optional.empty()
         : scoreboard.getScoreboardEntries(objectiveForSlot)
            .stream()
            .filter(scoreboardEntry -> !scoreboardEntry.hidden())
            .map(scoreboardEntry2 -> Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry2.owner()), scoreboardEntry2.name()).getString())
            .map(this::method2321)
            .filter(local3598_0 -> local3598_0.contains("монет") || local3598_0.contains("баланс"))
            .map(this::method2332)
            .flatMap(Optional::stream)
            .findFirst();
   }

   public static AuctionHelperModule method2310() {
      if (auctionHelperModule == null) {
         auctionHelperModule = new AuctionHelperModule();
      }

      return auctionHelperModule;
   }

   private String method2311(String local3602_1) {
      Matcher matcher = this.field08092.matcher(local3602_1);
      StringBuilder stringBuilder = new StringBuilder();

      while (matcher.find()) {
         try {
            matcher.appendReplacement(stringBuilder, String.valueOf(Long.parseLong(matcher.group(1)) * Long.parseLong(matcher.group(2))));
         } catch (NumberFormatException exception) {
            matcher.appendReplacement(stringBuilder, matcher.group(0));
         }
      }

      matcher.appendTail(stringBuilder);
      return stringBuilder.toString();
   }

   private AuctionHelperModule() {
      this.field08075 = this.addSetting(new BooleanSetting("Калькулятор", "Считает выражения вида 2*64 в отправляемых командах (/ah sell 12*12).", true));
      this.field08076 = this.addSetting(
         new NumberSetting("Прочность", "Минимальная оставшаяся прочность лота.")
            .withRange(0.0F, 100.0F)
            .withStep(1.0F)
            .withSuffix("%")
            .setValue(95.0F)
      );
      this.field08077 = this.addSetting(new BooleanSetting("Учитывать баланс", "Не подсвечивает лоты дороже баланса.", true));
      this.field08078 = this.addSetting(new BooleanSetting("Только зачарованные", "Пропускает лоты без зачарований.", false));
      this.field08079 = this.addSetting(new BooleanSetting("Фильтр количества", "Отбирает лоты по размеру стака.", false));
      this.modeSetting = this.addSetting(new ModeSetting("Режим количества", "Точно ровно столько, Минимум не меньше.", "Точно", "Точно", "Минимум"));
      this.field08081 = this.addSetting(new NumberSetting("Размер стака", "Сколько предметов должно быть в лоте.").withIntegerRange(1, 64).setValue(1.0F));
      this.modeSetting.withVisibility(() -> this.field08079.isChecked());
      this.field08081.withVisibility(() -> this.field08079.isChecked());
      this.field08082 = this.addSetting(new BooleanSetting("Фильтры предметов", "Требования к зачарованиям по типам предметов.", false));
      this.field08083 = this.addSetting(
         new MultiChoiceSetting("Броня", "Каким требованиям должна отвечать броня.")
            .withOptions("Без шипов", "Защита V", "Прочность V", "Починка", "Починка II")
      );
      this.field08084 = this.addSetting(new MultiChoiceSetting("Элитры", "Каким требованиям должны отвечать элитры.").withOptions("Прочность V", "Починка"));
      this.field08085 = this.addSetting(
         new MultiChoiceSetting("Мечи", "Каким требованиям должны отвечать мечи.")
            .withOptions("С отдачей", "Без отдачи", "Острота", "Прочность", "Яд", "Обнаружение", "Окисление", "Вампиризм")
      );
      this.field08086 = this.addSetting(
         new MultiChoiceSetting("Кирки", "Каким требованиям должны отвечать кирки.")
            .withOptions("Эффективность", "Починка", "Удача", "Прочность", "Шёлковое касание", "Переплавка", "Магнит", "Бульдозер", "Без тяжести")
      );
      this.field08087 = this.addSetting(
         new MultiChoiceSetting("Топоры", "Каким требованиям должны отвечать топоры.")
            .withOptions("Эффективность", "Починка", "Удача", "Прочность", "Магнит", "Лесоруб", "Пингер", "Бульдозер")
      );
      this.field08088 = this.addSetting(
         new MultiChoiceSetting("Лопаты", "Каким требованиям должны отвечать лопаты.")
            .withOptions("Эффективность", "Починка", "Удача", "Прочность", "Магнит", "Бульдозер")
      );
      this.field08089 = this.addSetting(
         new MultiChoiceSetting("Исключения", "Какие лоты не подсвечивать вовсе.")
            .withOptions(
               "Стрелы",
               "Пузырьки",
               "Дыхание дракона",
               "Броня",
               "Элитры",
               "Тотемы",
               "Головы",
               "Трезубцы",
               "Луки",
               "Арбалеты",
               "Огниво",
               "Конская броня",
               "Мечи",
               "Кирки",
               "Топоры",
               "Лопаты",
               "Мотыги"
            )
      );
      this.field08083.withVisibility(() -> this.field08082.isChecked());
      this.field08084.withVisibility(() -> this.field08082.isChecked());
      this.field08085.withVisibility(() -> this.field08082.isChecked());
      this.field08086.withVisibility(() -> this.field08082.isChecked());
      this.field08087.withVisibility(() -> this.field08082.isChecked());
      this.field08088.withVisibility(() -> this.field08082.isChecked());
      this.field08089.withVisibility(() -> this.field08082.isChecked());
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Команда поиска", "Команда, к которой допишется имя предмета из руки.", "ah search", 50));
      this.keybindSetting = this.addSetting(new KeybindSetting("Поиск с руки", "Ищет предмет из главной руки на аукционе."));
   }

   public int method2314(ItemStack itemStack) {
      for (Text text : itemStack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines()) {
         if (this.method2321(text.getString()).contains("цена") || this.method2321(text.getString()).contains("монет")) {
            return this.method2332(text.getString()).orElse(-1);
         }
      }

      return -1;
   }

   public void method2315() {
      if (mc.player != null && !mc.player.getMainHandStack().isEmpty()) {
         String local3607_1 = Formatting.strip(mc.player.getMainHandStack().getName().getString().trim().replace("\"", "\\\""))
            .replace("[★]", "")
            .replace("fff", "")
            .replace("ggg", "")
            .replace("xxx", "")
            .replace("123", "")
            .trim();
         if (!local3607_1.isEmpty()) {
            String local3607_2 = this.numericTextSetting.getValue().isBlank() ? "ah search" : this.numericTextSetting.getValue().stripLeading();
            mc.player.networkHandler.sendChatCommand((local3607_2.startsWith("/") ? local3607_2.substring(1) : local3607_2) + " " + local3607_1);
         }
      }
   }

   private boolean method2320(ItemStack itemStack) {
      return itemStack.isIn(ItemTags.HEAD_ARMOR)
         || itemStack.isIn(ItemTags.CHEST_ARMOR)
         || itemStack.isIn(ItemTags.LEG_ARMOR)
         || itemStack.isIn(ItemTags.FOOT_ARMOR);
   }

   public String method2321(String local3613_1) {
      return local3613_1.toLowerCase(Locale.ROOT).replace('o', 'о').replace('e', 'е').replace('a', 'а').replace('c', 'с');
   }

   private int method2324(ItemStack itemStack, String local3616_2) {
      NbtList listOrEmpty = itemStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT).copyNbt().getListOrEmpty("custom-enchantments");

      for (int index = 0; index < listOrEmpty.size(); index++) {
         if (listOrEmpty.getCompoundOrEmpty(index).getString("type", "").equals(local3616_2)) {
            return listOrEmpty.getCompoundOrEmpty(index).getInt("level", -1);
         }
      }

      return -1;
   }

   private int method2329(ItemStack itemStack, String local3621_2) {
      for (Entry<RegistryEntry<Enchantment>> local3621_4 : itemStack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
         .getEnchantmentEntries()) {
         if (((RegistryEntry)local3621_4.getKey()).matchesId(Identifier.of(local3621_2))) {
            return local3621_4.getIntValue();
         }
      }

      return -1;
   }

   public boolean method2331(String local3623_1) {
      return local3623_1.contains("1A0ꀄ")
         || local3623_1.contains("ꈁꀀꈂꌲꈂꀁ§0ꈃꄙ")
         || local3623_1.toLowerCase().contains("漢:")
         || local3623_1.toLowerCase().contains("аукцион")
         || local3623_1.toLowerCase().contains("поиск:")
         || local3623_1.toLowerCase().contains("п:")
         || local3623_1.toLowerCase().contains("категория:");
   }

   private Optional<Integer> method2332(String local3624_1) {
      String local3624_2 = local3624_1.replaceAll("\\D", "");
      return !local3624_2.isEmpty() && local3624_2.length() <= 9 ? Optional.of(Integer.parseInt(local3624_2)) : Optional.empty();
   }

   private void method2333(DrawContext drawContext, AuctionHelperModule.Nested0427 local3625_2, int local3625_3) {
      if (local3625_2 != null) {
         drawContext.fill(local3625_2.slot().x, local3625_2.slot().y, local3625_2.slot().x + 16, local3625_2.slot().y + 16, local3625_3);
      }
   }

   private record Nested0427(int price, Slot slot) {
   }
}
