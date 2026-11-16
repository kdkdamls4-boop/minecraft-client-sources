package deluxe.client.module.render;

import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.setting.SectionSetting;

import net.minecraft.client.sound.SoundInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.sound.SoundCategory;

@ModuleInfo(name = "NoRender", description = "Отключение назойливых визуальных эффектов и звуков.", category = ModuleCategory.RENDER)
public final class NoRenderModule extends Module {
   private static NoRenderModule noRenderModule;
   public MultiSelectSetting field12021;
   public MultiSelectSetting field12022;
   private final EventListener<PlayerTickEvent> field12023 = playerTickEvent -> {
      if (mc.player != null && this.method3427("Эффекты вардена")) {
         mc.player.removeStatusEffect(StatusEffects.DARKNESS);
      }
   };

   public boolean method3425(SoundInstance soundInstance) {
      if (!this.isEnabled()) {
         return false;
      } else {
         String path = soundInstance.getId().getPath();
         if (this.field12022.matchesOption("Пластинки") && soundInstance.getCategory() == SoundCategory.RECORDS) {
            return true;
         } else if (this.field12022.matchesOption("Портал") && path.startsWith("block.portal.")) {
            return true;
         } else if (this.field12022.matchesOption("Иссушитель") && path.startsWith("entity.wither.")) {
            return true;
         } else if (this.field12022.matchesOption("Варден") && path.startsWith("entity.warden.")) {
            return true;
         } else if (this.field12022.matchesOption("Шипы") && path.equals("enchant.thorns.hit")) {
            return true;
         } else {
            return !this.field12022.matchesOption("Опыт")
                  || !path.equals("entity.experience_orb.pickup")
                  || mc.player != null
                     && !(
                        (soundInstance.getX() - mc.player.getX()) * (soundInstance.getX() - mc.player.getX())
                              + (soundInstance.getY() - mc.player.getY()) * (soundInstance.getY() - mc.player.getY())
                              + (soundInstance.getZ() - mc.player.getZ()) * (soundInstance.getZ() - mc.player.getZ())
                           > 4.0
                     )
               ? this.field12022.matchesOption("Копание") && path.startsWith("block.") && (path.endsWith(".hit") || path.endsWith(".break"))
               : true;
         }
      }
   }

   public static NoRenderModule method3426() {
      if (noRenderModule == null) {
         noRenderModule = new NoRenderModule();
      }

      return noRenderModule;
   }

   private NoRenderModule() {
      this.addSetting(new SectionSetting("Отключение оверлея"));
      this.field12021 = this.addSetting(
         new MultiSelectSetting(
            "Оверлеи",
            "Какие эффекты отключать.",
            new String[]{
               "Темнота",
               "Туман слепоты",
               "Тряска экрана",
               "Камера при уроне",
               "Эффекты FOV",
               "Оверлей лавы",
               "Оверлей огня",
               "Оверлей воды",
               "Тыква на голове",
               "Затемнение блоков",
               "Иссушающие сердца",
               "Эффекты вардена",
               "Индикатор атаки",
               "Скорборд",
               "Анимация тотема"
            },
            "Темнота",
            "Тряска экрана"
         )
      );
      this.addSetting(new SectionSetting("Отключение звуков"));
      this.field12022 = this.addSetting(
         new MultiSelectSetting(
            "Звуки", "Какие звуки заглушать.", new String[]{"Опыт", "Пластинки", "Портал", "Иссушитель", "Шипы", "Копание", "Варден"}, "Опыт", "Портал"
         )
      );
   }

   public boolean method3427(String local5588_1) {
      return this.isEnabled() && this.field12021.matchesOption(local5588_1);
   }
}
