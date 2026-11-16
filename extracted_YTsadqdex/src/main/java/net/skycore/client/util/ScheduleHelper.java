package net.skycore.client.util;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionQueue;
import net.skycore.client.config.MacroManager;
import net.skycore.client.event.KeyEvent;
import net.skycore.client.event.MouseButtonEvent;
import net.skycore.client.module.KeyBinds;
import net.skycore.client.module.combat.ArrowSwapModule;
import net.skycore.client.module.combat.AutoSwapModule;
import net.skycore.client.module.combat.AutoTotemModule;
import net.skycore.client.module.misc.ReallyWorldHelperModule;
import net.skycore.client.module.player.ClickPearlModule;

public final class ScheduleHelper {
   private final ActionQueue queue = new ActionQueue();
   public static final ScheduleHelper INSTANCE = new ScheduleHelper();

   public static void schedule(Runnable runnable) {
      if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         INSTANCE.queue.submit(runnable);
      } else {
         runnable.run();
      }
   }

   public static void beforeMovement() {
      INSTANCE.queue.beforeMovement();
   }

   public static boolean isBlockingSprint() {
      return needsSprintReset();
   }

   private void onMouse(MouseButtonEvent mouseButtonEvent) {
      if (mouseButtonEvent.pressed() && mouseButtonEvent.client().gui.screen() == null) {
         int computedValue = 1000 + mouseButtonEvent.button().button();
         if (!KeyBinds.isPrimaryMouseButton(computedValue)) {
            if (!ClickPearlModule.INSTANCE.isBusy()
               && !ArrowSwapModule.INSTANCE.isBusy()
               && (!ClickPearlModule.INSTANCE.isEnabled() || computedValue != ClickPearlModule.INSTANCE.clickKey.get())
               && (!ArrowSwapModule.INSTANCE.isEnabled() || ArrowSwapModule.INSTANCE.swapKey.get() == -1 || computedValue != ArrowSwapModule.INSTANCE.swapKey.get())) {
               this.queueMacro(computedValue);
            }
         }
      }
   }

   private ScheduleHelper() {
      SkycoreClient.events().subscribe(KeyEvent.class, this::onKey);
      SkycoreClient.events().subscribe(MouseButtonEvent.class, this::onMouse);
   }

   private void onKey(KeyEvent keyEvent) {
      if (keyEvent.pressed() && keyEvent.client().gui.screen() == null) {
         if (!(keyEvent.client().gui.screen() instanceof ChatScreen)) {
            int value = keyEvent.key().key();
            if (value != 0 && value != 256) {
               if (!ClickPearlModule.INSTANCE.isBusy()
                  && !ArrowSwapModule.INSTANCE.isBusy()
                  && (!ClickPearlModule.INSTANCE.isEnabled() || value != ClickPearlModule.INSTANCE.clickKey.get())
                  && (!ArrowSwapModule.INSTANCE.isEnabled() || ArrowSwapModule.INSTANCE.swapKey.get() == -1 || value != ArrowSwapModule.INSTANCE.swapKey.get())) {
                  this.queueMacro(value);
               }
            }
         }
      }
   }

   private void queueMacro(int index) {
      ArrayList<String> strings = new ArrayList();

      for (MacroManager.Macro macro : SkycoreClient.macros().getMacros()) {
         if (macro.keyCode() == index) {
            String text = macro.text();
            if (text != null && !text.isEmpty()) {
               strings.add(text);
            }
         }
      }

      if (!strings.isEmpty()) {
         for (String text2 : strings) {
            if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
               this.queue.submit(() -> {
                  this.runCommand(text2);
               });
            } else {
               this.runCommand(text2);
            }
         }
      }
   }

   public static void onClientTickEnd() {
      INSTANCE.queue.tickEnd();
   }

   public static void cancel() {
      INSTANCE.queue.clear();
      ClickPearlModule.INSTANCE.cancelPendingAction();
      ArrowSwapModule.INSTANCE.cancelPendingAction();
      AutoSwapModule.INSTANCE.cancelPendingAction();
      ReallyWorldHelperModule.INSTANCE.cancelPendingAction();
      var optional = SkycoreClient.modules().get(AutoTotemModule.class);
      Consumer<net.skycore.client.module.combat.AutoTotemModule> consumer = AutoTotemModule::cancelPendingAction;
      optional.ifPresent(consumer);
   }

   public static void onClientTickStart() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (instance.level != null) {
            if (!NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
               INSTANCE.queue.drainWithoutStop();
               return;
            }

            INSTANCE.queue.tickStart();
            return;
         }
      }

      cancel();
   }

   private void runCommand(String text) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.getConnection() != null) {
         String text2 = text == null ? "" : text.trim();
         if (!text2.isEmpty()) {
            String prefix = SkycoreClient.commands() != null ? SkycoreClient.commands().getPrefix() : ".";
            if (prefix != null && !prefix.isEmpty() && text2.startsWith(prefix)) {
               SkycoreClient.commands().tryExecute(text2);
            } else if (text2.startsWith("/")) {
               instance.player.connection.sendCommand(text2.substring(1).trim());
            } else {
               instance.player.connection.sendChat(text2);
            }
         }
      }
   }

   public static boolean needsSprintReset() {
      if (NeedsPacketOrderSafeHelper.needsSkycoreActionStop()) {
         if (INSTANCE.queue.isStopping()) {
            return true;
         }
      }

      return false;
   }

   public static void init() {
   }
}
