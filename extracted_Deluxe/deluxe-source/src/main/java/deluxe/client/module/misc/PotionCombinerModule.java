package deluxe.client.module.misc;

import deluxe.client.ui.panel.PotionPresetPanel;

import deluxe.client.util.ClientChat;
import deluxe.client.ui.DeluxePotionCombinerScreen;
import deluxe.client.event.EventListener;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.PotionPreset;
import deluxe.client.ui.panel.PotionPresetPanel;
import deluxe.client.util.InventoryActionQueue;
import deluxe.client.util.InventorySlotManager;
import deluxe.client.module.player.ServerHelperModule;

import java.util.ArrayDeque;
import java.util.Random;
import net.minecraft.client.gui.screen.ChatScreen;

@ModuleInfo(name = "PotionCombiner", description = "Комбинирует бафы SpookyTime: по бинду кидает зелья пресета по очереди.", category = ModuleCategory.MISC)
public final class PotionCombinerModule extends Module {
   private static PotionCombinerModule potionCombinerModule;
   public KeybindSetting keybindSetting;
   private final Random field01178 = new Random();
   private final ArrayDeque<String> field01179 = new ArrayDeque<>();
   private boolean field01180;
   private long field01181;
   private long field01182;
   private String field01183 = "";
   private final EventListener<PlayerTickEvent> field01184 = playerTickEvent -> {
      if (mc.player != null && mc.getWindow() != null) {
         if (this.keybindSetting.isBound()
            && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle())
            && !this.field01180
            && !(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof DeluxePotionCombinerScreen)) {
            PotionPresetPanel.method1316().method1327();
            mc.setScreen(DeluxePotionCombinerScreen.instance());
         }

         this.field01180 = this.keybindSetting.isBound() && this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         if (mc.world != null && mc.interactionManager != null) {
            this.method0720();
            this.method0717();
         }
      }
   };

   private void method0717() {
      if (!this.field01179.isEmpty() && mc.currentScreen == null && System.currentTimeMillis() >= this.field01181) {
         if (!InventorySlotManager.method4176().method4179() && !InventoryActionQueue.method2656().method2665()) {
            String local955_1 = this.field01179.poll();

            for (ServerHelperModule.Nested0360 local955_3 : ServerHelperModule.method0707().method0690()) {
               if (local955_3.method0710() == null && local955_3.getKeybindSetting().name.equals(local955_1)) {
                  ServerHelperModule.method0707().method0703();
                  ServerHelperModule.method0707().method0701(local955_3);
                  break;
               }
            }

            this.field01181 = System.currentTimeMillis() + 80L + this.field01178.nextInt(40);
         }
      }
   }

   private void method0718(String local956_1) {
      this.method0719(local956_1);
      if (this.isEnabled()) {
         this.toggle();
      }
   }

   @Override
   protected void onDisable() {
      this.field01179.clear();
   }

   @Override
   protected void onEnable() {
      this.field01183 = "";
      this.field01182 = 0L;
   }

   private PotionCombinerModule() {
      this.keybindSetting = this.addSetting(new KeybindSetting("Открыть меню", "Клавиша, которая открывает окно Potion Combiner."));
   }

   private void method0719(String local960_1) {
      if (local960_1 != null && !local960_1.isBlank()) {
         if (!local960_1.equals(this.field01183) || System.currentTimeMillis() >= this.field01182) {
            ClientChat.method2267("&8[&dPotionCombiner&8] &f" + local960_1);
            this.field01183 = local960_1;
            this.field01182 = System.currentTimeMillis() + 2500L;
         }
      }
   }

   private void method0720() {
      for (PotionPreset.Nested0421 local961_2 : PotionPreset.method2166().field07362) {
         boolean local961_3 = local961_2.keybindSetting.isBound()
            && local961_2.keybindSetting.getBinding().method2749(mc.getWindow().getHandle())
            && mc.currentScreen == null;
         if (local961_2.field07390 && local961_3 && !local961_2.field07393 && this.field01179.isEmpty()) {
            for (PotionPreset.Nested0423 local961_5 : local961_2.field07392) {
               this.field01179.add(local961_5.field07397);
            }

            this.field01181 = System.currentTimeMillis();
            this.method0719("Комбо &d" + local961_2.field07389 + "&f: кидаю " + local961_2.field07392.size() + " зелья по очереди.");
         }

         local961_2.field07393 = local961_3;
      }
   }

   public static PotionCombinerModule method0722() {
      if (potionCombinerModule == null) {
         potionCombinerModule = new PotionCombinerModule();
      }

      return potionCombinerModule;
   }
}
