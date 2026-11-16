package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.GuiScale;
import deluxe.client.render.FontText;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.util.SoundPlayer;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;

@ModuleInfo(name = "ArmorBar", description = "Броня на уровне хотбара или над голодом ванильными текстурами.", category = ModuleCategory.RENDER)
public final class ArmorBarModule extends Module {
   private static ArmorBarModule armorBarModule;
   private static final EquipmentSlot[] field14186 = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static final Identifier[] field14187 = new Identifier[]{
      PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE,
      PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
      PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE,
      PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE
   };
   private static final String[] field14188 = new String[]{"Шлем", "Нагрудник", "Поножи", "Ботинки"};
   public ModeSetting modeSetting;
   public BooleanSetting booleanSetting;
   private final boolean[] field14191 = new boolean[4];
   private final EventListener<HudRenderEvent> field14192 = hudRenderEvent -> {
      if (mc.player != null && !mc.options.hudHidden) {
         for (int index = 0; index < 4; index++) {
            if (this.modeSetting.isSelected("Тип 1")) {
               this.method4128(
                  mc.player.getEquippedStack(field14186[index]),
                  (
                        mc.player.getMainArm() == Arm.LEFT
                           ? hudRenderEvent.method4462() / 2.0F - 91.0F - 4.0F - 82.0F
                           : hudRenderEvent.method4462() / 2.0F + 91.0F + 4.0F
                     )
                     + 3.0F
                     + index * 20.0F,
                  hudRenderEvent.method4460() - 22.0F + 3.0F
               );
            } else {
               this.method4128(
                  mc.player.getEquippedStack(field14186[index]),
                  hudRenderEvent.method4462() / 2.0F + 91.0F - 70.0F + index * 18.0F,
                  hudRenderEvent.method4460() - 57.0F - (Math.round(mc.player.getHungerManager().getSaturationLevel()) > 0 ? 10.0F : 0.0F)
               );
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> field14193 = playerTickEvent -> {
      if (mc.player != null && this.booleanSetting.isChecked()) {
         for (int index2 = 0; index2 < 4; index2++) {
            ItemStack equippedStack = mc.player.getEquippedStack(field14186[index2]);
            if (!equippedStack.isEmpty() && equippedStack.isDamageable()) {
               if (1.0F - (float)equippedStack.getDamage() / equippedStack.getMaxDamage() > 0.3F) {
                  this.field14191[index2] = false;
               } else if (!this.field14191[index2]) {
                  this.field14191[index2] = true;
                  NotificationHud.method2688(
                     this.category.getIcon(),
                     field14188[index2],
                     "прочность " + Math.round(100.0F * (1.0F - (float)equippedStack.getDamage() / equippedStack.getMaxDamage())) + "%, скоро сломается",
                     true
                  );
                  SoundPlayer.method3158("notify_type1_enabled");
               }
            } else {
               this.field14191[index2] = false;
            }
         }
      }
   };

   public void method4124(DrawContext drawContext, RenderTickCounter renderTickCounter) {
      if (mc.player != null) {
         if (this.modeSetting.isSelected("Тип 1")) {
            this.method4130(drawContext);
         } else {
            this.method4129(drawContext);
         }
      }
   }

   public static ArmorBarModule method4127() {
      if (armorBarModule == null) {
         armorBarModule = new ArmorBarModule();
      }

      return armorBarModule;
   }

   private ArmorBarModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Тип", "Панель сбоку от хотбара.", "Тип 1", "Тип 1"));
      this.modeSetting.withVisibility(() -> false);
      this.booleanSetting = this.addSetting(new BooleanSetting("Уведомлять о прочности", "Уведомление, когда прочность элемента брони падает ниже 30%.", true));
   }

   private void method4128(ItemStack itemStack, float local6902_2, float local6902_3) {
      if (!itemStack.isEmpty() && itemStack.isDamageable()) {
         float local6902_4 = GuiScale.method0797() / GuiScale.method0794();
         String local6902_5 = Math.round(100.0F * (1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())) + "%";
         float local6902_6 = 5.0F * local6902_4;
         float local6902_7 = 0.7F * local6902_4;
         float local6902_8 = (local6902_2 + 1.0F) * local6902_4;
         float local6902_9 = (local6902_3 + 2.5F) * local6902_4 - local6902_6 / 2.0F;
         int local6902_10 = Math.round(100.0F * (1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())) > 50
            ? -12517568
            : (Math.round(100.0F * (1.0F - (float)itemStack.getDamage() / itemStack.getMaxDamage())) > 30 ? -20440 : -50384);
         FontText.method1397(FontFace.BOLD, local6902_5, local6902_8 - local6902_7, local6902_9, local6902_6, -16777216);
         FontText.method1397(FontFace.BOLD, local6902_5, local6902_8 + local6902_7, local6902_9, local6902_6, -16777216);
         FontText.method1397(FontFace.BOLD, local6902_5, local6902_8, local6902_9 - local6902_7, local6902_6, -16777216);
         FontText.method1397(FontFace.BOLD, local6902_5, local6902_8, local6902_9 + local6902_7, local6902_6, -16777216);
         FontText.method1397(FontFace.BOLD, local6902_5, local6902_8, local6902_9, local6902_6, local6902_10);
      }
   }

   private void method4129(DrawContext drawContext) {
      float local6903_2 = drawContext.getScaledWindowWidth() / 2.0F + 91.0F - 70.0F;
      float local6903_3 = drawContext.getScaledWindowHeight()
         - 57.0F
         - (Math.round(mc.player.getHungerManager().getSaturationLevel()) > 0 ? 10.0F : 0.0F);

      for (int index = 0; index < 4; index++) {
         if (mc.player.getEquippedStack(field14186[index]).isEmpty()) {
            drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, field14187[index], (int)local6903_2 + index * 18, (int)local6903_3, 16, 16);
         } else {
            drawContext.drawItem(mc.player.getEquippedStack(field14186[index]), (int)local6903_2 + index * 18, (int)local6903_3);
            drawContext.drawStackOverlay(
               mc.textRenderer, mc.player.getEquippedStack(field14186[index]), (int)local6903_2 + index * 18, (int)local6903_3
            );
         }
      }
   }

   private void method4130(DrawContext drawContext) {
      float local6904_2 = mc.player.getMainArm() == Arm.LEFT
         ? drawContext.getScaledWindowWidth() / 2.0F - 91.0F - 4.0F - 82.0F
         : drawContext.getScaledWindowWidth() / 2.0F + 91.0F + 4.0F;
      float local6904_3 = drawContext.getScaledWindowHeight() - 22.0F;
      drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("hud/hotbar"), 182, 22, 0, 0, (int)local6904_2, (int)local6904_3, 81, 22);
      drawContext.drawGuiTexture(
         RenderPipelines.GUI_TEXTURED, Identifier.ofVanilla("hud/hotbar"), 182, 22, 181, 0, (int)local6904_2 + 81, (int)local6904_3, 1, 22
      );

      for (int index = 0; index < 4; index++) {
         drawContext.drawItem(mc.player.getEquippedStack(field14186[index]), (int)local6904_2 + 3 + index * 20, (int)local6904_3 + 3);
         if (!mc.player.getEquippedStack(field14186[index]).isEmpty()) {
            drawContext.drawStackOverlay(
               mc.textRenderer,
               mc.player.getEquippedStack(field14186[index]),
               (int)local6904_2 + 3 + index * 20,
               (int)local6904_3 + 3
            );
         }
      }
   }
}
