package net.skycore.client.module.misc;

import java.util.Locale;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class AutoMessageModule extends ClientModule {
   private static final String[] INSULT_MESSAGES = new String[]{
      "You got outplayed by Skycore 26.2",
      "Better luck next time — Skycore 26.2",
      "Want to play like that? Try Skycore 26.2",
      "Back to spawn — Skycore 26.2",
      "Skycore 26.2 won that fight"
   };
   private final Random random = new Random();
   private static final String KILL_MARKER = "вы убили";
   public static final AutoMessageModule INSTANCE = new AutoMessageModule();

   private static String chatText(Packet<?> packet) {
      if (packet instanceof ClientboundSystemChatPacket clientboundSystemChatPacket) {
         Component text = clientboundSystemChatPacket.content();
         String string = text.getString();
         return strip(string);
      } else if (packet instanceof ClientboundDisguisedChatPacket clientboundDisguisedChatPacket) {
         String string2 = clientboundDisguisedChatPacket.message().getString();
         return strip(string2);
      } else {
         return null;
      }
   }

   private void onPacket(PacketReceiveEvent packetReceiveEvent) {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         LocalPlayer player = instance.player;
         if (player != null && instance.level != null) {
            String packet = chatText(packetReceiveEvent.getPacket());
            if (packet != null && packet.toLowerCase(Locale.ROOT).contains("вы убили")) {
               String text = extractPlayerNameFromKillMessage(packet);
               if (text != null && !text.isEmpty()) {
                  String text2 = INSULT_MESSAGES[this.random.nextInt(INSULT_MESSAGES.length)];
                  player.connection.sendChat("! -ezz " + text + " " + text2);
               }
            }
         }
      }
   }

   private static String extractPlayerNameFromKillMessage(String text) {
      int value = text.toLowerCase(Locale.ROOT).indexOf("вы убили");
      if (value == -1) {
         return null;
      }

      String text2 = text.substring(value + "вы убили".length()).trim();
      if (text2.isEmpty()) {
         return null;
      }

      text2 = text2.replaceAll("^[!.,:;\\s]+", "");
      String[] stringValues = text2.split("\\s+");
      if (stringValues.length == 0) {
         return null;
      }

      String text3 = stringValues[0].trim().replaceAll("[!.,:;]+$", "");
      return text3.isEmpty() ? null : text3;
   }

   @Override
   public boolean affectsGameplay() {
      return true;
   }

   private static String strip(String text) {
      if (text == null) {
         return null;
      }

      return ChatFormatting.stripFormatting(text);
   }

   private AutoMessageModule() {
      super("Auto Message", "Automatically sends a message after a kill", ModuleCategory.MISC);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onPacket);
   }
}
