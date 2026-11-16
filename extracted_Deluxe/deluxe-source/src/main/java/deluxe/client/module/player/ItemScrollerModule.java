package deluxe.client.module.player;

import deluxe.client.event.EventListener;
import deluxe.mixin.HandledScreenMixin;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.NumberSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;

@ModuleInfo(name = "ItemScroller", description = "Шифт с ЛКМ мгновенно перекидывает предметы под курсором.", category = ModuleCategory.PLAYER)
public final class ItemScrollerModule extends Module {
   private static ItemScrollerModule itemScrollerModule;
   private final NumberSetting numberSetting = this.addSetting(
      new NumberSetting("Задержка", "Задержка между перекидываниями.").withRange(0.0F, 500.0F).withStep(10.0F).setValue(50.0F).withSuffix("ms")
   );
   private long field13728;
   private final EventListener<PlayerTickEvent> field13729 = playerTickEvent -> {
      if (mc.player != null && mc.currentScreen instanceof HandledScreen<?> local6663_2) {
         if (InputUtil.isKeyPressed(mc.getWindow(), 340) || InputUtil.isKeyPressed(mc.getWindow(), 344)) {
            if (GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), 0) == 1) {
               if (!((float)(System.currentTimeMillis() - this.field13728) < this.numberSetting.floatValue())) {
                  Slot slot = ((HandledScreenMixin)local6663_2).hoveredSlot();
                  if (slot != null && slot.hasStack()) {
                     this.field13728 = System.currentTimeMillis();
                     InventoryUtils.method0547(slot.id, 0, SlotActionType.QUICK_MOVE, false);
                  }
               }
            }
         }
      }
   };

   private ItemScrollerModule() {
   }

   public static ItemScrollerModule method3994() {
      if (itemScrollerModule == null) {
         itemScrollerModule = new ItemScrollerModule();
      }

      return itemScrollerModule;
   }
}
