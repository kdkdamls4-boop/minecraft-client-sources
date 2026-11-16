package deluxe.client.ui;

import deluxe.client.util.AuctionItemCatalog;
import deluxe.client.setting.NumberSetting;
import deluxe.client.storage.AutoBuyItem;
import deluxe.client.ui.SmoothScroll;
import deluxe.client.setting.TextSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Random;
import net.minecraft.item.Items;

public final class AutoBuySettingsScreen {
   public static final String[] field03110 = new String[]{"Slow", "Medium", "Ultrabuy"};
   public static final float[] field03111 = new float[]{
      0.41422594F,
      0.5606695F,
      0.29707113F,
      0.65690374F,
      0.41841003F,
      0.7573222F,
      0.59414226F,
      0.37238494F,
      0.42677826F,
      0.53974897F,
      0.37656903F,
      0.8995816F,
      0.5146443F,
      0.53556484F,
      0.28870293F,
      0.50209206F
   };
   private static final DateTimeFormatter field03112 = DateTimeFormatter.ofPattern("HH:mm:ss");
   private static final HashMap<String, Long> field03113 = new HashMap<>();
   private static AutoBuySettingsScreen autoBuySettingsScreen;
   public TextSetting field03115 = new TextSetting("Search", "").withPlaceholder("Поиск предметов...");
   public NumberSetting numberSetting = new NumberSetting("Auto Buy Speed", "Select scan frequency and execution speed")
      .withIntegerRange(0, 2)
      .setValue(2.0F);
   public AutoBuySettingsScreen.Nested0374 field03117 = new AutoBuySettingsScreen.Nested0374("Buy Price (Max)", "125000", 9);
   public AutoBuySettingsScreen.Nested0374 field03118 = new AutoBuySettingsScreen.Nested0374("Sell Price (Min)", "140000", 9);
   public AutoBuySettingsScreen.Nested0374 field03119 = new AutoBuySettingsScreen.Nested0374("Minimum Durability", "85", 3);
   public TextSetting field03120 = new TextSetting("Telegram Bot Token", "").setValue("7123984120:AAHq_x9Z8k...").withPlaceholder("Bot token...");
   public TextSetting field03121 = new TextSetting("Telegram Chat ID", "").setValue("123456789").withPlaceholder("Chat ID...");
   public LinkedHashMap<String, AutoBuySettingsScreen.Nested0372> field03122 = new LinkedHashMap<>();
   public ArrayList<AutoBuySettingsScreen.Nested0373> field03123 = new ArrayList<>();
   public float[] field03124 = new float[field03111.length];
   public HashMap<String, Long> field03125 = new HashMap<>();
   public HashMap<String, Long> field03126 = new HashMap<>();
   public SmoothScroll field03127 = new SmoothScroll();
   public SmoothScroll field03128 = new SmoothScroll();
   public float[] field03129;
   public float[] field03130;
   public long field03131;
   public double field03132;
   public String field03133;
   public long field03134;
   public long field03135;
   public String field03136 = "";
   private Random field03137 = new Random();
   private long field03138;

   public static String method1134(long local1559_0) {
      return String.format(Locale.US, "%,d", local1559_0);
   }

   public static long method1135(String local1560_0) {
      if (local1560_0 != null && !local1560_0.isEmpty()) {
         try {
            return Long.parseLong(local1560_0);
         } catch (NumberFormatException exception) {
            return 0L;
         }
      } else {
         return 0L;
      }
   }

   public static long method1136(String local1561_0) {
      return field03113.get(local1561_0) != null
         ? field03113.get(local1561_0)
         : Math.round((25000L + Math.floorMod(local1561_0.hashCode(), 900) * 1000L) / 1000.0) * 1000L;
   }

   private AutoBuySettingsScreen.Nested0372 method1137(String local1562_1) {
      return new AutoBuySettingsScreen.Nested0372(method1136(local1562_1), Math.round(method1136(local1562_1) * 1.12 / 1000.0) * 1000L);
   }

   public void method1138(long local1563_1) {
      if (this.field03138 == 0L) {
         this.field03138 = local1563_1 + 6000L + this.field03137.nextInt(8000);
      }

      if (local1563_1 >= this.field03138 && !this.field03122.isEmpty()) {
         this.field03138 = local1563_1 + 6000L + this.field03137.nextInt(8000);
         this.field03124[this.field03137.nextInt(this.field03124.length)] = Math.max(
            0.12F, Math.min(1.0F, field03111[this.field03137.nextInt(this.field03124.length)] + (this.field03137.nextFloat() - 0.35F) * 0.25F)
         );
      }
   }

   public static AutoBuySettingsScreen method1139() {
      if (autoBuySettingsScreen == null) {
         autoBuySettingsScreen = new AutoBuySettingsScreen();
      }

      return autoBuySettingsScreen;
   }

   public void method1140(String local1565_1) {
      if (this.field03122.remove(local1565_1) == null) {
         this.field03122.put(local1565_1, this.method1137(local1565_1));
      } else if (local1565_1.equals(this.field03133)) {
         this.field03133 = null;
         this.field03134 = System.currentTimeMillis();
      }

      this.field03125.put(local1565_1, System.currentTimeMillis());
   }

   static {
      field03113.put("Элитры Крушителя", 2400000L);
      field03113.put("Сфера Афины", 1600000L);
      field03113.put("Чарка", 1250000L);
      field03113.put("Меч Крушителя", 890000L);
      field03113.put("Незеритовый слиток", 720000L);
      field03113.put("Булава Крушителя", 540000L);
      field03113.put("Спавнер", 480000L);
      field03113.put("Талисман Крушителя", 310000L);
   }

   public void method1141(String local1567_1, long local1567_2) {
      this.field03123
         .add(
            0,
            new AutoBuySettingsScreen.Nested0373(
               AuctionItemCatalog.method1055(local1567_1.replaceAll("\\s+[xх]\\d+$", "").trim()) == null
                  ? new AutoBuyItem.Nested0523().method4227(local1567_1.replaceAll("\\s+[xх]\\d+$", "").trim()).method4221(Items.PAPER).method4222()
                  : AuctionItemCatalog.method1055(local1567_1.replaceAll("\\s+[xх]\\d+$", "").trim()),
               local1567_1.matches(".*\\s+[xх]\\d+$") ? Integer.parseInt(local1567_1.replaceAll(".*\\s+[xх](\\d+)$", "$1")) : 1,
               LocalTime.now().format(field03112),
               local1567_2
            )
         );
      if (this.field03123.size() > 40) {
         this.field03123.remove(this.field03123.size() - 1);
      }

      this.field03131 += local1567_2;
   }

   public boolean method1142(String local1568_1) {
      return this.field03122.containsKey(local1568_1);
   }

   private AutoBuySettingsScreen() {
      AuctionItemCatalog.method1056();
      System.arraycopy(field03111, 0, this.field03124, 0, this.field03124.length);

      for (String local1569_4 : new String[]{
         "Шлем Крушителя",
         "Нагрудник Крушителя",
         "Поножи Крушителя",
         "Ботинки Крушителя",
         "Меч Крушителя",
         "Булава Крушителя",
         "Сфера Афины",
         "Талисман Крушителя",
         "Чарка",
         "Элитры Крушителя",
         "Хлопушка",
         "Шалкер"
      }) {
         if (AuctionItemCatalog.method1055(local1569_4) != null) {
            this.field03122.put(local1569_4, this.method1137(local1569_4));
         }
      }

      this.field03117.withPlaceholder("$0");
      this.field03118.withPlaceholder("$0");
      this.field03119.withPlaceholder("0%");
   }

   public ArrayList<AutoBuyItem> method1143() {
      if (!this.field03115.getValue().toLowerCase(Locale.ROOT).trim().equals(this.field03136)) {
         this.field03136 = this.field03115.getValue().toLowerCase(Locale.ROOT).trim();
         this.field03135 = System.currentTimeMillis();
         this.field03127.targetOffset = 0.0F;
      }

      ArrayList<AutoBuyItem> local1570_1 = new ArrayList<>();

      for (AutoBuyItem autoBuyItem : AuctionItemCatalog.method1077()) {
         if (this.field03136.isEmpty()
            || autoBuyItem.name().toLowerCase(Locale.ROOT).contains(this.field03136)
            || autoBuyItem.category().toLowerCase(Locale.ROOT).contains(this.field03136)) {
            local1570_1.add(autoBuyItem);
         }
      }

      return local1570_1;
   }

   public static final class Nested0372 {
      public AutoBuySettingsScreen.Nested0374 field03214;
      public AutoBuySettingsScreen.Nested0374 field03215;

      Nested0372(long local1571_1, long local1571_3) {
         this.field03214 = new AutoBuySettingsScreen.Nested0374("Buy Price (Max)", Long.toString(local1571_1), 9);
         this.field03215 = new AutoBuySettingsScreen.Nested0374("Sell Price (Min)", Long.toString(local1571_3), 9);
         this.field03214.withPlaceholder("$0");
         this.field03215.withPlaceholder("$0");
      }
   }

   public record Nested0373(AutoBuyItem item, int count, String time, long priceCoins) {
   }

   public static final class Nested0374 extends TextSetting {
      @Override
      public void insertText(String local1580_1) {
         super.insertText(local1580_1 == null ? "" : local1580_1.replaceAll("\\D", ""));
      }

      public Nested0374(String initialName, String local1581_2, int local1581_3) {
         super(initialName, "", local1581_2, local1581_3);
      }
   }
}
