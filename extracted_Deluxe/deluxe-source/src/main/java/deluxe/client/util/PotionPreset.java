package deluxe.client.util;

import deluxe.client.ui.SmoothScroll;

import deluxe.client.setting.KeybindSetting;
import deluxe.client.MinecraftAccess;
import deluxe.client.module.player.ServerHelperModule;
import deluxe.client.setting.TextSetting;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public final class PotionPreset implements MinecraftAccess {
   private static PotionPreset potionPreset;
   public TextSetting field07356 = (new TextSetting("Имя пресета", "Уникальное имя данного пресета", "", 7) {}).withPlaceholder("Введите текст...");
   public TextSetting field07357 = new TextSetting("Поиск", "").withPlaceholder("Поиск");
   public KeybindSetting keybindSetting = new KeybindSetting("Горячая клавиша", "Зелье выбрасываются при нажатии бинда");
   public ItemStack[] field07359 = new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
   public String[] field07360 = new String[6];
   public int field07361 = -1;
   public ArrayList<PotionPreset.Nested0421> field07362 = new ArrayList<>();
   public SmoothScroll field07363 = new SmoothScroll();
   public SmoothScroll field07364 = new SmoothScroll();
   public float[] field07365;
   public float[] field07366;

   public ArrayList<ServerHelperModule.Nested0360> method2163() {
      ArrayList<ServerHelperModule.Nested0360> local3393_1 = new ArrayList<>();

      for (ServerHelperModule.Nested0360 local3393_3 : ServerHelperModule.method0707().method0690()) {
         if (local3393_3.method0710() == null
            && (
               this.field07357.getValue().trim().isEmpty()
                  || local3393_3.getKeybindSetting().name.toLowerCase(Locale.ROOT).contains(this.field07357.getValue().toLowerCase(Locale.ROOT).trim())
            )) {
            local3393_1.add(local3393_3);
         }
      }

      return local3393_1;
   }

   public void method2164(int local3394_1, String local3394_2) {
      if (local3394_1 >= 0 && local3394_1 < 6) {
         this.field07359[local3394_1] = method2168(local3394_2);
         this.field07360[local3394_1] = local3394_2;
         this.field07361 = -1;
         this.field07357.setValue("");
      }
   }

   public int method2165() {
      for (int index = 0; index < 6; index++) {
         if (this.field07359[index].isEmpty()) {
            return index;
         }
      }

      return -1;
   }

   public static PotionPreset method2166() {
      if (potionPreset == null) {
         potionPreset = new PotionPreset();
      }

      return potionPreset;
   }

   public void method2167() {
      JsonArray jsonArray = new JsonArray();

      for (PotionPreset.Nested0421 local3397_3 : this.field07362) {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("name", local3397_3.field07389);
         jsonObject.addProperty("active", local3397_3.field07390);
         jsonObject.addProperty("key", local3397_3.keybindSetting.getKeyCode());
         JsonArray jsonArray2 = new JsonArray();

         for (PotionPreset.Nested0423 local3397_7 : local3397_3.field07392) {
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("kind", local3397_7.field07396);
            jsonObject2.addProperty("name", local3397_7.field07397);
            jsonArray2.add(jsonObject2);
         }

         jsonObject.add("potions", jsonArray2);
         jsonArray.add(jsonObject);
      }

      try {
         Files.createDirectories(FabricLoader.getInstance().getConfigDir().resolve("deluxe"));
         Files.writeString(
            FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("potioncombiner.json"),
            new GsonBuilder().setPrettyPrinting().create().toJson(jsonArray)
         );
      } catch (Exception exception) {
      }
   }

   public static ItemStack method2168(String local3398_0) {
      ItemStack itemStack = new ItemStack(Items.SPLASH_POTION);
      if (ServerItemType.method2460(local3398_0) != null) {
         itemStack.set(
            DataComponentTypes.POTION_CONTENTS,
            new PotionContentsComponent(
               Optional.empty(), Optional.of(ServerItemType.method2460(local3398_0).method2458() | 0xFF000000), List.of(), Optional.empty()
            )
         );
      } else if (local3398_0.toLowerCase(Locale.ROOT).contains("прыгуч")) {
         itemStack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(-131196), List.of(), Optional.empty()));
      }

      return itemStack;
   }

   public void method2169() {
      PotionPreset.Nested0421 local3399_1 = new PotionPreset.Nested0421();
      local3399_1.field07389 = this.field07356.getValue().trim().isEmpty()
         ? "Прес " + (this.field07362.size() + 1)
         : this.field07356.getValue().trim().substring(0, Math.min(7, this.field07356.getValue().trim().length()));
      local3399_1.keybindSetting.setBinding(this.keybindSetting.getBinding());

      for (String local3399_5 : this.field07360) {
         if (local3399_5 != null) {
            local3399_1.field07392.add(new PotionPreset.Nested0423("splash", local3399_5));
         }
      }

      if (!local3399_1.field07392.isEmpty()) {
         this.field07362.add(local3399_1);
         this.field07356.setValue("");
         this.keybindSetting.setBinding(null);

         for (int index = 0; index < 6; index++) {
            this.field07359[index] = ItemStack.EMPTY;
         }

         for (int index2 = 0; index2 < 6; index2++) {
            this.field07360[index2] = null;
         }

         this.method2167();
      }
   }

   private PotionPreset() {
      this.method2170();
   }

   private void method2170() {
      try {
         if (!Files.exists(FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("potioncombiner.json"))) {
            return;
         }

         for (JsonElement jsonElement : JsonParser.parseString(
               Files.readString(FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("potioncombiner.json"))
            )
            .getAsJsonArray()) {
            PotionPreset.Nested0421 local3401_3 = new PotionPreset.Nested0421();
            local3401_3.field07389 = jsonElement.getAsJsonObject().get("name").getAsString();
            local3401_3.field07390 = jsonElement.getAsJsonObject().get("active").getAsBoolean();
            local3401_3.keybindSetting.withKeyCode(jsonElement.getAsJsonObject().get("key").getAsInt());

            for (JsonElement jsonElement2 : jsonElement.getAsJsonObject().getAsJsonArray("potions")) {
               local3401_3.field07392
                  .add(
                     new PotionPreset.Nested0423(
                        jsonElement2.getAsJsonObject().get("kind").getAsString(), jsonElement2.getAsJsonObject().get("name").getAsString()
                     )
                  );
            }

            this.field07362.add(local3401_3);
         }
      } catch (Exception exception) {
      }
   }

   public static final class Nested0421 {
      public String field07389 = "";
      public boolean field07390 = true;
      public KeybindSetting keybindSetting = new KeybindSetting("Горячая клавиша", "Зелье выбрасываются при нажатии бинда");
      public ArrayList<PotionPreset.Nested0423> field07392 = new ArrayList<>();
      public boolean field07393;
   }

   public static final class Nested0423 {
      public String field07396;
      public String field07397;

      Nested0423(String local3404_1, String local3404_2) {
         this.field07396 = local3404_1;
         this.field07397 = local3404_2;
      }

      public ItemStack method2171() {
         return PotionPreset.method2168(this.field07397);
      }

      public static String method2172(ItemStack itemStack) {
         return itemStack.isOf(Items.SPLASH_POTION) ? "splash" : (itemStack.isOf(Items.LINGERING_POTION) ? "lingering" : "potion");
      }
   }
}
