package deluxe.client.module.render;

import deluxe.client.ui.hud.TargetHud;
import deluxe.client.util.TargetHudStyle;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PacketEvent;
import deluxe.client.ui.ClickGuiStyle;
import deluxe.client.setting.ColorSetting;
import deluxe.client.util.TargetHudStyle;
import deluxe.client.setting.TextSetting;
import deluxe.client.module.player.UseTrackerModule;

import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;

@ModuleInfo(name = "Interface", description = "Какие элементы худа показывать.", category = ModuleCategory.RENDER)
public final class InterfaceModule extends Module {
   private static InterfaceModule interfaceModule;
   public ColorSetting colorSetting;
   public MultiSelectSetting field07917;
   public LegacyNumberSetting field07918;
   public BooleanSetting field07919;
   public MultiSelectSetting field07920;
   public TextSetting textSetting;
   public BooleanSetting field07922;
   public BooleanSetting field07923;
   public BooleanSetting field07924;
   public LegacyNumberSetting field07925;
   private int field07926;
   private final EventListener<PacketEvent> field07927 = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof PlaySoundS2CPacket playSoundS2CPacket) {
         mc.execute(
            () -> {
               if (mc.player != null && mc.world != null && this.isEnabled()) {
                  TargetHudStyle.method3848(
                     mc.player, playSoundS2CPacket.getX(), playSoundS2CPacket.getY(), playSoundS2CPacket.getZ(), playSoundS2CPacket.getSound().value().id().toString()
                  );
               }
            }
         );
      }
   };

   private InterfaceModule() {
      this.colorSetting = this.addSetting(new ColorSetting("Тема", "Акцентный цвет всего клиента.").setValue(-12092417));
      this.field07917 = this.addSetting(
         new MultiSelectSetting(
            "Элементы",
            "Какие элементы худа рисовать.",
            new String[]{"watermark", "keybinds", "serverbinds", "potions", "cooldowns", "staffs", "target", "inventory", "Target", "toasts"},
            "watermark",
            "keybinds",
            "serverbinds",
            "potions",
            "cooldowns",
            "staffs",
            "target",
            "Target",
            "toasts"
         )
      );
      this.field07918 = this.addSetting(
         new LegacyNumberSetting("Затемнение слота кд", "Насколько затемнять слот хотбара под цифрой кулдауна.", 25.0, 0.0, 100.0, 1.0)
      );
      this.field07918.withVisibility(() -> this.field07917.matchesOption("cooldowns"));
      this.field07919 = this.addSetting(new BooleanSetting("Отцентрировать", "Держать ватермарку по центру сверху и уступать боссбару.", false));
      this.field07919.withVisibility(() -> false);
      this.field07920 = this.addSetting(
         new MultiSelectSetting(
            "Компоненты",
            "Что показывать в ватермарке.",
            new String[]{"Аватарка", "Логин", "ФПС", "Пинг", "Координаты", "Время"},
            "Аватарка",
            "Логин",
            "ФПС",
            "Время"
         )
      );
      this.field07920.withVisibility(() -> false);
      this.textSetting = this.addSetting(new TextSetting("Порядок ватермарки", "Порядок компонентов ватермарки."));
      this.textSetting.withVisibility(() -> false);
      this.field07922 = this.addSetting(
         new BooleanSetting("Показывать донатные зелья", "Показывать в зельях донатные зелья FunTime и SpookyTime вместо их эффектов.", true)
      );
      this.field07922.withVisibility(() -> false);
      this.field07923 = this.addSetting(
         new BooleanSetting("Партиклы из головы при ударе", "При ударе по цели из её головы в TargetHud вылетают светящиеся частицы.", true)
      );
      this.field07923.withVisibility(() -> false);
      this.field07924 = this.addSetting(new BooleanSetting("Звуки модулей", "Проигрывать звук при включении и выключении модуля.", true));
      this.field07925 = this.addSetting(new LegacyNumberSetting("Громкость звуков", "Громкость звуков модулей в процентах.", 12.0, 0.0, 100.0, 1.0));
      this.field07925.withVisibility(() -> this.field07924.isChecked());
   }

   public boolean method2243(String local3516_1) {
      return local3516_1.equals("arrows")
         ? UseTrackerModule.method1800().isEnabled() && UseTrackerModule.method1800().field05846.isChecked()
         : this.isEnabled() && this.field07917.matchesOption(local3516_1);
   }

   public static InterfaceModule method2247() {
      if (interfaceModule == null) {
         interfaceModule = new InterfaceModule();
      }

      return interfaceModule;
   }

   public void method2250() {
      int local3523_1 = 0xFF000000 | this.colorSetting.getValue() & 16777215;
      if (local3523_1 != this.field07926) {
         this.field07926 = local3523_1;
         ClickGuiStyle.method3187(local3523_1);
      }
   }

   public boolean method2251() {
      return this.isEnabled() && this.field07919.isChecked();
   }
}
