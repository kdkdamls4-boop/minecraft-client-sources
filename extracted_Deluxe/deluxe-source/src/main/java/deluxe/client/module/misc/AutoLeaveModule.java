package deluxe.client.module.misc;

import deluxe.client.setting.BooleanSetting;
import deluxe.mixin.ClientCommonNetworkHandlerMixin;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.setting.NumericTextSetting;
import deluxe.client.ui.hud.StaffHud;
import deluxe.client.ui.hud.NotificationHud;

import java.util.Locale;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@ModuleInfo(name = "AutoLeave", description = "Сам уходит с сервера: мало ХП, модератор в табе или чужой игрок рядом.", category = ModuleCategory.MISC)
public final class AutoLeaveModule extends Module {
   private static AutoLeaveModule autoLeaveModule;
   public MultiSelectSetting multiSelectSetting;
   public BooleanSetting booleanSetting;
   public NumericTextSetting numericTextSetting;
   public LegacyNumberSetting field15035;
   public LegacyNumberSetting field15036;
   private boolean field15037;
   private final EventListener<PlayerTickEvent> field15038 = playerTickEvent -> {
      if (!this.field15037
         && mc.player != null
         && mc.world != null
         && mc.getNetworkHandler() != null
         && !mc.isConnectedToLocalServer()) {
         if (this.multiSelectSetting.matchesOption("Мало ХП") && mc.player.getHealth() <= this.field15035.getValue()) {
            this.method4404("Здоровье " + String.format(Locale.US, "%.1f", mc.player.getHealth()) + " хп");
         } else {
            if (this.multiSelectSetting.matchesOption("Модератор")) {
               for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
                  if (StaffHud.field07789.matcher(playerListEntry.getProfile().name()).matches()) {
                     Team team = mc.world.getScoreboard().getScoreHolderTeam(playerListEntry.getProfile().name());
                     String local7421_5 = team != null && !team.getPrefix().getString().trim().isEmpty()
                        ? team.getPrefix().getString().trim()
                        : (
                           playerListEntry.getDisplayName() == null
                              ? ""
                              : playerListEntry.getDisplayName().getString().replace(playerListEntry.getProfile().name(), "").trim()
                        );
                     if (!local7421_5.isEmpty() && StaffHud.field07790.matcher(local7421_5.toLowerCase(Locale.ROOT)).matches()) {
                        this.method4404("Модератор " + playerListEntry.getProfile().name() + " в табе");
                        return;
                     }
                  }
               }
            }

            if (this.multiSelectSetting.matchesOption("Игрок рядом")) {
               for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
                  if (abstractClientPlayerEntity != mc.player
                     && !FriendStore.method3431(abstractClientPlayerEntity.getGameProfile().name())
                     && !(abstractClientPlayerEntity.distanceTo(mc.player) > this.field15036.getValue())) {
                     this.method4404(
                        "Игрок "
                           + abstractClientPlayerEntity.getGameProfile().name()
                           + " в "
                           + String.format(Locale.US, "%.1f", abstractClientPlayerEntity.distanceTo(mc.player))
                           + " бл"
                     );
                     return;
                  }
               }
            }
         }
      }
   };

   private void method4404(String local7417_1) {
      this.field15037 = true;
      if (this.booleanSetting.isChecked()) {
         ((ClientCommonNetworkHandlerMixin)mc.player.networkHandler).connection().disconnect(Text.literal("[AutoLeave] " + local7417_1));
      } else if (!this.numericTextSetting.getText().isBlank()) {
         mc.player
            .networkHandler
            .sendChatCommand(this.numericTextSetting.getText().startsWith("/") ? this.numericTextSetting.getText().substring(1) : this.numericTextSetting.getText());
         NotificationHud.method2688(this.category.getIcon(), "AutoLeave", local7417_1, true);
      }

      this.setEnabled(false);
   }

   @Override
   protected void onEnable() {
      this.field15037 = false;
   }

   private AutoLeaveModule() {
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Условия", "Когда уходить: мало ХП, модератор появился в табе, не-друг подошёл близко.", new String[]{"Мало ХП", "Модератор", "Игрок рядом"}
         )
      );
      this.booleanSetting = this.addSetting(new BooleanSetting("Разрыв соединения", "Полностью выйти с сервера вместо команды.", false));
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Команда", "Команда ухода, когда разрыв выключен.", "/hub", 32));
      this.numericTextSetting.withVisibility(() -> !this.booleanSetting.isChecked());
      this.field15035 = this.addSetting(new LegacyNumberSetting("Мин. ХП", "Уходить, когда здоровье не выше этого.", 5.0, 1.0, 20.0, 0.5));
      this.field15035.withVisibility(() -> this.multiSelectSetting.matchesOption("Мало ХП"));
      this.field15036 = this.addSetting(new LegacyNumberSetting("Дистанция", "Уходить, когда не-друг ближе этого расстояния.", 10.0, 1.0, 100.0, 1.0));
      this.field15036.withVisibility(() -> this.multiSelectSetting.matchesOption("Игрок рядом"));
   }

   public static AutoLeaveModule method4407() {
      if (autoLeaveModule == null) {
         autoLeaveModule = new AutoLeaveModule();
      }

      return autoLeaveModule;
   }
}
