package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.Colors;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.util.GuiScale;
import deluxe.client.setting.ColorSetting;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.render.Render2D;
import deluxe.client.RotationAngles;

import java.awt.Color;
import java.util.HashMap;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;

@ModuleInfo(name = "Arrows", description = "Кольцо стрелок вокруг прицела показывает, куда повернуться до игрока.", category = ModuleCategory.RENDER)
public final class ArrowsModule extends Module {
   private static ArrowsModule arrowsModule;
   public MultiSelectSetting multiSelectSetting;
   public ColorSetting colorSetting;
   public ModeSetting modeSetting;
   public BooleanSetting field10637;
   public LegacyNumberSetting field10638;
   public LegacyNumberSetting field10639;
   public BooleanSetting field10640;
   public LegacyNumberSetting field10641;
   public BooleanSetting field10642;
   public LegacyNumberSetting field10643;
   private float field10644 = 50.0F;
   private final HashMap<Integer, float[]> field10645 = new HashMap<>();
   private final EventListener<HudRenderEvent> field10646 = hudRenderEvent -> {
      if (mc.player != null && mc.world != null) {
         float local4978_2 = MathHelper.clamp(52.0F / Math.max(1, mc.getCurrentFps()), 0.0F, 1.0F);
         float local4978_3 = hudRenderEvent.method4462() / 2.0F * GuiScale.method0797() / GuiScale.method0794();
         float local4978_4 = hudRenderEvent.method4460() / 2.0F * GuiScale.method0797() / GuiScale.method0794();
         this.field10644 = this.field10644
            + (
                  (
                        this.field10642.isChecked() && mc.currentScreen instanceof HandledScreen
                           ? this.field10643.getValue()
                           : (this.field10640.isChecked() && mc.player.isSprinting() ? this.field10641.getValue() : this.field10639.getValue())
                     )
                     - this.field10644
               )
               * local4978_2;
         float local4978_5 = (float)Math.toRadians(RotationAngles.method4366());
         HudRenderContext.method4333(hudRenderEvent.method4459());
         if (this.field10637.isChecked()) {
            HudRenderContext.method4335(
               local4978_3, local4978_4, 1.0F, MathHelper.cos(Math.toRadians(MathHelper.clamp(-RotationAngles.method4364() + 90.0F, -60.0F, 60.0F)))
            );
         }

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity livingEntity && this.method3123(livingEntity)) {
               double local4978_9 = livingEntity.getLerpedPos(hudRenderEvent.method4461()).x - mc.gameRenderer.getCamera().getCameraPos().x;
               double local4978_11 = livingEntity.getLerpedPos(hudRenderEvent.method4461()).z - mc.gameRenderer.getCamera().getCameraPos().z;
               float local4978_13 = (float)Math.toDegrees(
                  Math.atan2(
                     -(local4978_11 * MathHelper.cos(local4978_5) - local4978_9 * MathHelper.sin(local4978_5)),
                     -(local4978_9 * MathHelper.cos(local4978_5) + local4978_11 * MathHelper.sin(local4978_5))
                  )
               );
               float local4978_14 = livingEntity instanceof PlayerEntity playerEntity && FriendStore.method3431(playerEntity.getGameProfile().name())
                  ? 1.0F
                  : 0.0F;
               float[] floatArray = this.field10645.computeIfAbsent(livingEntity.getId(), local4973_2 -> new float[]{local4978_13, 0.0F, local4978_14});
               floatArray[0] = MathHelper.lerpAngleDegrees(local4978_2, floatArray[0], local4978_13);
               floatArray[1] += (1.0F - floatArray[1]) * local4978_2;
               floatArray[2] += (local4978_14 - floatArray[2]) * local4978_2;
               this.method3122(floatArray, local4978_3, local4978_4);
            }
         }

         this.field10645.entrySet().removeIf(local4972_4 -> {
            if (mc.world.getEntityById(local4972_4.getKey()) instanceof LivingEntity livingEntity2 && this.method3123(livingEntity2)) {
               return false;
            } else {
               local4972_4.getValue()[1] -= ((float[])local4972_4.getValue())[1] * local4978_2;
               this.method3122(local4972_4.getValue(), local4978_3, local4978_4);
               return local4972_4.getValue()[1] < 0.01F;
            }
         });
         if (this.field10637.isChecked()) {
            HudRenderContext.method4337();
         }
      }
   };

   private ArrowsModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting("Кого показывать", "На кого вести стрелки.", new String[]{"Игроки", "Друзья", "Мобы"}, "Игроки", "Друзья")
      );
      this.colorSetting = this.addSetting(new ColorSetting("Цвет друзей", "Каким цветом вести стрелку на друга.", new Color(60, 232, 107)));
      this.field10637 = this.addSetting(new BooleanSetting("Рендерить в 3д", "Кладёт кольцо стрелок на плоскость по наклону взгляда.", false));
      this.field10638 = this.addSetting(new LegacyNumberSetting("Размер", "Размер стрелки.", 15.0, 6.0, 40.0, 0.5));
      this.field10639 = this.addSetting(new LegacyNumberSetting("Радиус", "Радиус кольца стрелок.", 50.0, 25.0, 150.0, 0.1));
      this.field10640 = this.addSetting(new BooleanSetting("Менять при спринте", "Разводить кольцо шире, пока бежишь.", true));
      this.field10641 = this.addSetting(new LegacyNumberSetting("Радиус при спринте", "Радиус кольца во время спринта.", 100.0, 25.0, 150.0, 0.1));
      this.field10641.withVisibility(() -> this.field10640.isChecked());
      this.field10642 = this.addSetting(new BooleanSetting("Менять в меню", "Разводить кольцо шире с открытым инвентарём или контейнером.", true));
      this.field10643 = this.addSetting(
         new LegacyNumberSetting("Радиус в меню", "Радиус кольца с открытым инвентарём или контейнером.", 130.0, 25.0, 150.0, 0.1)
      );
      this.field10643.withVisibility(() -> this.field10642.isChecked());
   }

   @Override
   protected void onDisable() {
      this.field10645.clear();
   }

   public static ArrowsModule method3119() {
      if (arrowsModule == null) {
         arrowsModule = new ArrowsModule();
      }

      return arrowsModule;
   }

   private void method3122(float[] floatArray, float local4980_2, float local4980_3) {
      float local4980_4 = local4980_2 + this.field10644 * MathHelper.cos(Math.toRadians(floatArray[0])) - this.field10638.getValue() / 2.0F;
      float local4980_5 = local4980_3 + this.field10644 * MathHelper.sin(Math.toRadians(floatArray[0])) - this.field10638.getValue() / 2.0F;
      Render2D.method4673(
         "assets/deluxe/textures/images/arrow_nav_hollow.png",
         local4980_4,
         local4980_5,
         this.field10638.getValue(),
         0.0F,
         floatArray[0] + 90.0F,
         local4980_4 + this.field10638.getValue() / 2.0F,
         local4980_5 + this.field10638.getValue() / 2.0F,
         Colors.method3727(
            Colors.method3723(ThemeColors.field13669, this.colorSetting.getValue(), floatArray[2]), floatArray[1]
         )
      );
   }

   private boolean method3123(LivingEntity livingEntity) {
      if (livingEntity == mc.player || livingEntity instanceof ArmorStandEntity || !livingEntity.isAlive()) {
         return false;
      } else if (livingEntity instanceof PlayerEntity playerEntity) {
         return FriendStore.method3431(playerEntity.getGameProfile().name())
            ? this.multiSelectSetting.matchesOption("Друзья")
            : this.multiSelectSetting.matchesOption("Игроки");
      } else {
         return this.multiSelectSetting.matchesOption("Мобы");
      }
   }
}
