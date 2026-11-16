package deluxe.client.module.player;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.util.InputBinding;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.InventorySlotManager;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

@ModuleInfo(name = "ClickAction", description = "Бросок привязанных предметов из хотбара или инвентаря по клавише.", category = ModuleCategory.PLAYER)
public final class ClickActionModule extends Module {
   private static ClickActionModule clickActionModule;
   public KeybindSetting field01558;
   public KeybindSetting field01559;
   public BooleanSetting booleanSetting;
   public KeybindSetting field01561;
   private boolean field01562;
   private boolean field01563;
   private int field01564 = -1;
   private int field01565 = -1;
   private int field01566 = -1;
   private boolean field01567;
   private final EventListener<PlayerTickEvent> field01568 = playerTickEvent -> {
      if (mc.player != null && mc.interactionManager != null) {
         this.method0842();
         if (mc.currentScreen == null && !mc.player.isUsingItem()) {
            if (this.field01558.getBinding().method2749(mc.getWindow().getHandle()) && !this.field01562) {
               this.method0837(Items.ENDER_PEARL);
            }

            this.field01562 = this.field01558.getBinding().method2749(mc.getWindow().getHandle());
            if (this.field01559.getBinding().method2749(mc.getWindow().getHandle())) {
               this.method0839();
            } else {
               this.method0838();
            }
         } else {
            this.field01562 = this.field01558.getBinding().method2749(mc.getWindow().getHandle());
            this.method0838();
         }
      }
   };

   private void method0837(Item item) {
      if (!InventorySlotManager.method4176().method4185(item.getDefaultStack())) {
         NotificationHud.method2691(
            this.category.getIcon(),
            item.getDefaultStack().getName().getString(),
            Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
            true
         );
      } else if (!mc.player.getItemCooldownManager().isCoolingDown(item.getDefaultStack())) {
         InventorySlotManager.method4176().method4192(item.getDefaultStack());
      }
   }

   private ClickActionModule() {
      this.field01558 = this.addSetting(new KeybindSetting("Эндер жемчуг", "Кидает жемчуг по нажатию бинда."));
      this.field01559 = new KeybindSetting("Пузырьки опыта", "Кидает пузырьки, пока бинд зажат.");
      this.booleanSetting = this.addSetting(new BooleanSetting("Друг по клику", "Клик по игроку выбранной кнопкой добавляет или убирает его из друзей.", false));
      this.field01561 = this.addSetting(new KeybindSetting("Кнопка друга", "Какой кнопкой отмечать друга.", InputBinding.mouseButton(2)));
      this.field01561.withVisibility(() -> this.booleanSetting.isChecked());
   }

   private void method0838() {
      this.field01567 = false;
      if (this.field01564 != -1) {
         InventoryUtils.method0553(this.field01564, false);
         this.field01564 = -1;
      }

      if (this.field01565 != -1) {
         if (this.field01566 == -1) {
            if (!InventoryActionQueue.method2656().method2665() && !InventorySlotManager.method4176().method4179()) {
               this.field01565 = -1;
            }
         } else if (!InventoryActionQueue.method2656().method2665()
            && !InventorySlotManager.method4176().method4179()
            && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
            InventoryActionQueue.method2656().method2662(this.field01565, this.field01566, 1);
            this.field01566 = -1;
            this.field01565 = -1;
         }
      }
   }

   private void method0839() {
      if (this.field01565 != -1) {
         if (this.field01566 == -1) {
            if (!InventoryActionQueue.method2656().method2665() && !InventorySlotManager.method4176().method4179()) {
               this.field01565 = -1;
            }
         } else if (InventoryUtils.method0554() != this.field01566) {
            InventoryUtils.method0553(this.field01566, false);
         } else if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            this.method0838();
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
            InventoryUtils.method0542(Hand.MAIN_HAND);
         }
      } else if (this.field01564 != -1) {
         if (mc.player.getMainHandStack().getItem() != Items.EXPERIENCE_BOTTLE) {
            InventoryUtils.method0553(this.field01564, false);
            this.field01564 = -1;
         } else if (!mc.player.getItemCooldownManager().isCoolingDown(mc.player.getMainHandStack())) {
            InventoryUtils.method0542(Hand.MAIN_HAND);
         }
      } else if (InventoryUtils.method0539(Items.EXPERIENCE_BOTTLE) != -1) {
         this.field01564 = InventoryUtils.method0554();
         InventoryUtils.method0553(InventoryUtils.method0539(Items.EXPERIENCE_BOTTLE), false);
      } else if (InventoryUtils.method0551(Items.EXPERIENCE_BOTTLE) != -1) {
         this.field01565 = InventoryUtils.method0551(Items.EXPERIENCE_BOTTLE);
         this.field01566 = InventoryUtils.method0554();
         InventoryActionQueue.method2656().method2662(this.field01565, this.field01566, 1);
      } else {
         if (!InventoryUtils.method0534(Items.EXPERIENCE_BOTTLE) && !this.field01567) {
            this.field01567 = true;
            NotificationHud.method2691(
               this.category.getIcon(),
               Items.EXPERIENCE_BOTTLE.getDefaultStack().getName().getString(),
               Text.literal("нет в инвентаре").setStyle(Style.EMPTY.withColor(Formatting.RED)),
               true
            );
         }
      }
   }

   @Override
   protected void onDisable() {
      this.field01562 = false;
      this.method0838();
   }

   public static ClickActionModule method0841() {
      if (clickActionModule == null) {
         clickActionModule = new ClickActionModule();
      }

      return clickActionModule;
   }

   private void method0842() {
      if (this.booleanSetting.isChecked() && mc.currentScreen == null) {
         boolean local1136_1 = this.field01561.isBound() && this.field01561.getBinding().method2749(mc.getWindow().getHandle());
         if (local1136_1 && !this.field01563 && mc.targetedEntity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity && abstractClientPlayerEntity != mc.player
            )
          {
            if (FriendStore.method3431(abstractClientPlayerEntity.getGameProfile().name())) {
               FriendStore.method3436(abstractClientPlayerEntity.getGameProfile().name());
               NotificationHud.method2688(this.category.getIcon(), "ClickAction", abstractClientPlayerEntity.getGameProfile().name() + " больше не друг", true);
            } else {
               FriendStore.method3434(abstractClientPlayerEntity.getGameProfile().name());
               NotificationHud.method2688(this.category.getIcon(), "ClickAction", abstractClientPlayerEntity.getGameProfile().name() + " теперь друг", true);
            }
         }

         this.field01563 = local1136_1;
      } else {
         this.field01563 = false;
      }
   }
}
