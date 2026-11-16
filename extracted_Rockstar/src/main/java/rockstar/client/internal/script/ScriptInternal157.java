package rockstar.client.internal.script;







import rockstar.client.setting.*;
import rockstar.client.server.*;
import rockstar.client.i18n.*;
import rockstar.client.event.*;
import rockstar.client.internal.network.*;
import rockstar.client.*;
import rockstar.modules.combat.*;
import rockstar.modules.movement.*;
import rockstar.modules.visual.*;
import rockstar.modules.player.*;
import rockstar.modules.other.*;

import globals.client.Information;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import pyrock.events.game.InternalAttackEvent;
import pyrock.events.game.SendMessageEvent;
import pyrock.events.network.ReceivePacketEvent;
import pyrock.events.network.SendPacketEvent;
import pyrock.events.network.ServerConnectionEvent;
import pyrock.events.player.ClientPlayerTickEvent;
import rockstar.profile.Profile;

public class ScriptInternal157 {
   private ModeSetting.InternalType0088 internalField0237;
   private int internalField0227;
   private boolean internalField0277;
   private final List<ModeSetting.InternalType0088> internalField0416 = new ArrayList<>();
   private final EventListener<ClientPlayerTickEvent> internalField0157 = localValue1 -> {};
   private final EventListener<ServerConnectionEvent> internalField0158 = localValue1 -> {};
   private final EventListener<ReceivePacketEvent> internalField1028 = localValue1 -> {};
   private final EventListener<SendMessageEvent> internalField1029 = localValue1 -> {};
   private final EventListener<InternalAttackEvent> internalField1030 = localValue1 -> {};
   private final EventListener<SendPacketEvent> internalField1027 = localValue0 -> {};

   public ScriptInternal157() {
      // Neutralized - security audit: telemetry and snitch logger removed
   }

   public String internalMethod03612() {
      StringBuilder localValue1 = new StringBuilder();

      for (ModeSetting.InternalType0088 localValue3 : this.internalField0416) {
         localValue1.append(LanguageManager.internalMethod07214(localValue3.getName()));
         if (localValue3 != this.internalField0416.getLast()) {
            localValue1.append(", ");
         }
      }

      return localValue1.toString();
   }

   public String internalMethod00162() {
      String localValue1 = "null";
      if (Information.getPreferUser() != null) {
         localValue1 = Information.getPreferUser().username();
      }

      return localValue1;
   }

   public String[] internalMethod00291() {
      return new String[]{"postmarketOS"};
   }
}
