package deluxe.client.module.misc;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;

import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.entity.boss.BossBar.Color;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

@ModuleInfo(name = "YTMODE", description = "Прячет фортуну, маскирует боссбар под дуэль и рисует невидимкам 1000 хп.", category = ModuleCategory.MISC)
public final class YTMODEModule extends Module {
   private static YTMODEModule yTMODEModule;
   public ModeSetting modeSetting;
   private long field04403;
   private ServerInfo field04404;
   private final EventListener<PlayerTickEvent> field04405 = playerTickEvent -> {
      if (this.field04403 != 0L && System.currentTimeMillis() >= this.field04403 && this.field04404 != null && mc.world != null) {
         this.field04403 = 0L;
         ServerInfo serverInfo = this.field04404;
         mc.send(() -> {
            if (mc.world != null) {
               mc.world.disconnect(Text.literal("YTMODE: смена скина"));
               mc.disconnectWithProgressScreen();
               ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(serverInfo.address), serverInfo, false, null);
            }
         });
      }
   };

   public Text method1422() {
      return this.modeSetting.isSelected("2x2")
         ? ClientChat.method2262("Командная дуэль", 47084, 3047398)
         : ClientChat.method2262("Обычная дуэль", 47084, 3047398);
   }

   public Color method1424() {
      return Color.BLUE;
   }

   public boolean method1425() {
      return this.isEnabled() && !this.modeSetting.isSelected("Rtp");
   }

   private YTMODEModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим", "Подо что маскируется картинка.", "1x1", "1x1", "Rtp", "2x2"));
   }

   public static YTMODEModule method1426() {
      if (yTMODEModule == null) {
         yTMODEModule = new YTMODEModule();
      }

      return yTMODEModule;
   }

   public boolean method1427(ScoreboardEntry scoreboardEntry) {
      return mc.world == null
         ? false
         : Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name())
            .getString()
            .toLowerCase()
            .contains("фортун");
   }

   @Override
   protected void onEnable() {
      if (mc.player != null && mc.getNetworkHandler() != null && !mc.isConnectedToLocalServer()) {
         mc.player.networkHandler.sendChatCommand("skin s_FunTime_1");
         this.field04404 = mc.getCurrentServerEntry();
         this.field04403 = System.currentTimeMillis() + 1000L;
      }
   }

   public boolean method1428(String local2102_1) {
      return !local2102_1.contains("\n")
            || !local2102_1.toLowerCase().contains("spookytime") && !local2102_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("**")
         ? !local2102_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().isEmpty()
            && "╔╠╚".indexOf(local2102_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().charAt(0)) >= 0
         : true;
   }

   @Override
   protected void onDisable() {
      this.field04403 = 0L;
      this.field04404 = null;
   }
}
