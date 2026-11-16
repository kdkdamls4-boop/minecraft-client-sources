package deluxe.client.module.misc;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.Stopwatch;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "ChestStealer", description = "Автоматически забирает все предметы из сундуков.", category = ModuleCategory.MISC)
public final class ChestStealerModule extends Module {
   private static ChestStealerModule chestStealerModule;
   public ModeSetting modeSetting;
   public BooleanSetting field01792;
   public BooleanSetting field01793;
   private final Stopwatch stopwatch = new Stopwatch();
   private boolean field01795;
   public final EventListener<PlayerTickEvent> field01796 = playerTickEvent -> {
      if (mc.player != null && mc.currentScreen != null) {
         if (!this.modeSetting.isSelected("FunTime") || !mc.currentScreen.getTitle().getString().contains("☃ Аукцион ")) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
               Inventory inventory = genericContainerScreenHandler.getInventory();
               ArrayList arrayList = new ArrayList();

               for (int index = 0; index < inventory.size(); index++) {
                  if (!inventory.getStack(index).isEmpty()) {
                     arrayList.add(index);
                  }
               }

               if (!arrayList.isEmpty() && this.stopwatch.hasExceededMillis(this.modeSetting.isSelected("FunTime") ? 400L : 10L)) {
                  InventoryUtils.method0547(
                     (Integer)arrayList.get(ThreadLocalRandom.current().nextInt(arrayList.size())), 0, SlotActionType.QUICK_MOVE, false
                  );
                  this.field01795 = true;
                  this.stopwatch.reset();
               }

               if (this.field01793.isChecked() && this.field01795) {
                  boolean local1244_5 = false;

                  for (int index2 = 0; index2 < inventory.size(); index2++) {
                     if (!inventory.getStack(index2).isEmpty()) {
                        local1244_5 = true;
                        break;
                     }
                  }

                  if (!local1244_5) {
                     mc.player.closeHandledScreen();
                     this.field01795 = false;
                  }
               }
            }
         }
      }
   };

   private ChestStealerModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим использования", "Пауза между кликами под сервер.", "FunTime", "Vanilla", "FunTime"));
      this.field01792 = this.addSetting(new BooleanSetting("Отключить хитбоксы", "Не целиться в сущности, пока модуль включён.", true));
      this.field01793 = this.addSetting(new BooleanSetting("Авто-закрытие", "Закрывает сундук, когда он опустел.", true));
   }

   public static ChestStealerModule method0936() {
      if (chestStealerModule == null) {
         chestStealerModule = new ChestStealerModule();
      }

      return chestStealerModule;
   }
}
