package deluxe.client.ui.hud;

import deluxe.client.render.GuiEffectsState;
import deluxe.client.util.GuiScale;
import deluxe.client.ui.ScreenClosingTransition;
import deluxe.client.util.TargetHudStyle;

import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.event.EventPriority;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleManager;
import deluxe.client.render.Render2D;
import deluxe.client.module.render.StructureInfoModule;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.MessageScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

public final class HudLayout {
   private static final HudLayout hudLayout = new HudLayout();
   private final Map<Module, Boolean> field15486 = new IdentityHashMap<>();
   private boolean field15487;
   private long field15488 = System.nanoTime() / 1000000L;
   private float field15489;
   private float field15490;
   @EventPriority(Integer.MIN_VALUE)
   private final EventListener<HudRenderEvent> field15491 = event -> {
      GuiEffectsState.method3976(false);
      InterfaceModule.method2247().method2250();
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && minecraftClient.player != null && minecraftClient.world != null) {
         if (!(minecraftClient.currentScreen instanceof LevelLoadingScreen)
            && !(minecraftClient.currentScreen instanceof ProgressScreen)
            && !(minecraftClient.currentScreen instanceof MessageScreen)) {
            long local7721_3 = System.nanoTime() / 1000000L;
            float local7721_5 = Math.min(0.05F, (float)(local7721_3 - this.field15488) / 1000.0F);
            this.field15488 = local7721_3;
            float local7721_6 = GuiScale.method0797() / GuiScale.method0794();
            float local7721_7 = event.method4462() * local7721_6;
            float local7721_8 = event.method4460() * local7721_6;
            HudRenderContext.method4334(minecraftClient, local7721_5);
            HudRenderContext.method4333(event.method4459());
            HudManager.method3412();
            HudManager.method3404(
               (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local7721_6),
               (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local7721_6),
               HudRenderContext.method4343(),
               local7721_7,
               local7721_8
            );
            NotificationHud.method2684(HudRenderContext.method4343());
            this.method4566();
            if (StructureInfoModule.method3231().isEnabled()) {
               StructureInfoModule.method3231().onHudRender(event);
               Render2D.method4692(event.method4459());
            }

            if (!minecraftClient.options.hudHidden && InterfaceModule.method2247().method2243("cooldowns")) {
               CooldownsHud.method4562(minecraftClient, event.method4462(), event.method4460());
               Render2D.method4692(event.method4459());
            }

            String local7721_9 = "";
            ArrayList<float[]> local7721_10 = new ArrayList<>();

            for (String local7721_12 : HudManager.method3381()) {
               float[] floatArray = (!HudManager.method3409(local7721_12) || !minecraftClient.options.hudHidden)
                     && InterfaceModule.method2247().method2243(local7721_12)
                  ? HudManager.method3388(local7721_12)
                  : null;
               if (floatArray != null) {
                  for (float[] floatArray2 : local7721_10) {
                     if (floatArray[0] < floatArray2[0] + floatArray2[2]
                        && floatArray[0] + floatArray[2] > floatArray2[0]
                        && floatArray[1] < floatArray2[1] + floatArray2[3]
                        && floatArray[1] + floatArray[3] > floatArray2[1]) {
                        local7721_9 = local7721_12;
                     }
                  }

                  local7721_10.add(floatArray);
               }
            }

            boolean local7721_11 = false;

            for (String local7721_13 : new ArrayList<>(HudManager.method3381())) {
               if ((!HudManager.method3409(local7721_13) || !minecraftClient.options.hudHidden) && InterfaceModule.method2247().method2243(local7721_13)) {
                  if (minecraftClient.currentScreen == null && local7721_13.equals(local7721_9)) {
                     GuiEffectsState.method3976(true);
                     event.method4459().createNewRootLayer();
                     event.method4459().applyBlur();
                     Render2D.method4649(1);
                     local7721_11 = true;
                  } else {
                     event.method4459().createNewRootLayer();
                  }

                  float local7721_14 = HudManager.method3407(local7721_13);
                  float[] floatArray3 = HudManager.method3394(local7721_13);
                  boolean local7721_16 = Math.abs(local7721_14 - 1.0F) > 0.001F && floatArray3 != null;
                  if (local7721_16) {
                     HudRenderContext.method4336(floatArray3[0], floatArray3[1], local7721_14);
                  }

                  switch (local7721_13) {
                     case "watermark":
                        WatermarkHud.method2501(minecraftClient, 12.0F, 12.0F);
                        break;
                     case "keybinds":
                        KeybindsHud.method2647(12.0F, local7721_8 - 12.0F);
                        break;
                     case "potions":
                        this.field15489 = PotionsHud.method3203(minecraftClient, local7721_7 - 12.0F, 12.0F);
                        break;
                     case "cooldowns":
                        this.field15490 = CooldownsHud.method4558(
                           minecraftClient, local7721_7 - 12.0F, 12.0F + (!HudManager.method3419("potions") ? this.field15489 : 0.0F)
                        );
                        break;
                     case "staffs":
                        StaffHud.method2234(
                           local7721_7 - 12.0F,
                           12.0F
                              + (!HudManager.method3419("potions") ? this.field15489 : 0.0F)
                              + (!HudManager.method3419("cooldowns") ? this.field15490 : 0.0F)
                        );
                        break;
                     case "serverbinds":
                        ServerKeybindsHud.method4004(minecraftClient, local7721_7 / 2.0F, local7721_8 - 34.0F);
                        break;
                     case "inventory":
                        InventoryHud.method1258(minecraftClient, 12.0F, local7721_8 / 2.0F - 20.0F);
                        break;
                     case "Target":
                        TargetHudStyle.method3850(minecraftClient, 12.0F, local7721_8 / 2.0F + 30.0F);
                        break;
                     case "target":
                        TargetHud.method0486(minecraftClient, local7721_7, local7721_8, local7721_5);
                        break;
                     case "arrows":
                        ArrowsHud.method0836(local7721_7, local7721_8);
                  }

                  if (local7721_16) {
                     HudRenderContext.method4337();
                  }
               }
            }

            if (local7721_11) {
               Render2D.method4649(0);
            }

            event.method4459().createNewRootLayer();
            if (InterfaceModule.method2247().method2243("toasts")) {
               boolean local7721_12 = Math.abs(HudManager.method3407("toasts") - 1.0F) > 0.001F && HudManager.method3394("toasts") != null;
               if (local7721_12) {
                  HudRenderContext.method4336(
                     HudManager.method3394("toasts")[0], HudManager.method3394("toasts")[1], HudManager.method3407("toasts")
                  );
               }

               NotificationHud.method2687(local7721_7, local7721_8);
               if (local7721_12) {
                  HudRenderContext.method4337();
               }
            }

            event.method4459().createNewRootLayer();
            HudManager.method3414(
               (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local7721_6),
               (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local7721_6),
               HudRenderContext.method4343()
            );
            if (HudElementTypes.method4847()) {
               event.method4459().createNewRootLayer();
            }

            HudElementTypes.method4841(
               (float)(minecraftClient.mouse.getScaledX(minecraftClient.getWindow()) * local7721_6),
               (float)(minecraftClient.mouse.getScaledY(minecraftClient.getWindow()) * local7721_6),
               HudRenderContext.method4343()
            );
            if (!ScreenClosingTransition.method1457()) {
               Render2D.method4692(event.method4459());
               ScreenClosingTransition.method1459(event.method4459());
            }
         }
      } else {
         this.field15487 = false;
         this.field15486.clear();
      }
   };

   private void method4566() {
      boolean local7719_1 = !this.field15487;

      for (Module module : ModuleManager.getModules()) {
         Boolean local7719_4 = this.field15486.put(module, module.isEnabled());
         if (!local7719_1 && local7719_4 != null && local7719_4 != module.isEnabled()) {
            NotificationHud.method2688(
               module.category.getIcon(), module.name, module.isEnabled() ? "is Enabled!" : "is Disabled!", module.isEnabled()
            );
         }
      }

      this.field15487 = true;
   }

   private HudLayout() {
   }

   public static void method4568() {
      EventBus.getInstance().subscribe(hudLayout);
   }
}
