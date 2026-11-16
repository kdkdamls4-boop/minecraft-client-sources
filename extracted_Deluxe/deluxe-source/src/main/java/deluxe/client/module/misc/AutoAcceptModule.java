package deluxe.client.module.misc;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiChoiceSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.ui.hud.NotificationHud;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "AutoAccept", description = "Сам принимает телепорты, приглашения в клан и командные дуэли.", category = ModuleCategory.MISC)
public final class AutoAcceptModule extends Module {
   private static AutoAcceptModule autoAcceptModule;
   public MultiChoiceSetting multiChoiceSetting;
   public BooleanSetting booleanSetting;
   private static final String[] field12879 = new String[]{
      "просит телепортироваться",
      "хочет телепортироваться",
      "Заявка буудет автоматически отменена через",
      "просит телепортироваться к Вам.",
      "120 секунд",
      "has requested to teleport",
      "teleport to you",
      "This request will timeout after",
      "120 seconds"
   };
   private static final Pattern field12880 = Pattern.compile("\\[⚔] (\\w+) приглашает Вас в клан");
   private static final Pattern field12881 = Pattern.compile("\\[⚔] Игрок (.+?) приглашает вас в свою дуэльную команду!");
   private long field12882;
   private boolean field12883;
   public final EventListener<PlayerTickEvent> field12884 = playerTickEvent -> {
      if (this.field12883 && mc.player != null && mc.currentScreen != null) {
         if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler) {
            if (mc.currentScreen.getTitle().getString().contains("Приглашение в команду")) {
               Inventory inventory = genericContainerScreenHandler.getInventory();

               for (int index = 0; index < inventory.size(); index++) {
                  if (inventory.getStack(index).getItem() == Items.GREEN_STAINED_GLASS_PANE) {
                     InventoryUtils.method0547(index, 0, SlotActionType.QUICK_MOVE, true);
                     mc.player.closeHandledScreen();
                     break;
                  }
               }
            }

            this.field12883 = false;
         }
      }
   };

   private boolean method3703(String local6140_1) {
      for (String local6140_3 : FriendStore.method3432()) {
         if (local6140_1.toLowerCase(Locale.ROOT).contains(local6140_3.toLowerCase(Locale.ROOT))) {
            return true;
         }
      }

      return false;
   }

   private AutoAcceptModule() {
      this.multiChoiceSetting = this.addSetting(
         new MultiChoiceSetting("Принимать что/куда?", "Какие приглашения принимать самому.")
            .withOptions("Телепортации", "В клан", "В командные дуэли")
            .withSelection("Телепортации", "В клан", "В командные дуэли")
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Только друзья", "Принимать, только если в сообщении есть ник друга.", true));
   }

   public static AutoAcceptModule method3705() {
      if (autoAcceptModule == null) {
         autoAcceptModule = new AutoAcceptModule();
      }

      return autoAcceptModule;
   }

   public void method3706(String local6144_1) {
      if (this.isEnabled() && mc.player != null && local6144_1 != null) {
         if (!this.booleanSetting.isChecked() || this.method3703(local6144_1)) {
            if (this.multiChoiceSetting.matchesOption("Телепортации")) {
               for (String local6144_5 : field12879) {
                  if (local6144_1.contains(local6144_5)) {
                     this.field12882 = System.currentTimeMillis();
                     mc.player.networkHandler.sendChatCommand("tpaccept");
                     NotificationHud.method2688(this.category.getIcon(), "AutoAccept", "Принял телепорт", true);
                     return;
                  }
               }
            }

            if (this.multiChoiceSetting.matchesOption("В клан")) {
               Matcher matcher = field12880.matcher(local6144_1);
               if (matcher.find()) {
                  this.field12882 = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("clan accept " + matcher.group(1));
                  NotificationHud.method2688(this.category.getIcon(), "AutoAccept", "Принял в клан от " + matcher.group(1), true);
                  return;
               }
            }

            if (this.multiChoiceSetting.matchesOption("В командные дуэли")) {
               Matcher matcher2 = field12881.matcher(local6144_1);
               if (matcher2.find()) {
                  this.field12882 = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("duel team accept " + matcher2.group(1));
                  this.field12883 = true;
                  NotificationHud.method2688(this.category.getIcon(), "AutoAccept", "Принял дуэль от " + matcher2.group(1), true);
               }
            }
         }
      }
   }
}
