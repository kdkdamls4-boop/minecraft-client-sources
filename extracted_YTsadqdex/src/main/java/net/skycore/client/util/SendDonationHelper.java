package net.skycore.client.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.skycore.client.SkycoreClient;

public final class SendDonationHelper {
   private static final String PREFIX = "SkyCore Client ⇨";

   public static void sendDonation(String text) {
      sendChannel("DONATE", text);
   }

   public static void sendChannel(String text, Component text2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && text2 != null) {
         String text3 = text != null && !text.isBlank() ? text : "IRC";
         MutableComponent mutableComponent = Component.empty();
         MutableComponent mutableComponent2 = gradientTag("[" + text3 + "]");
         MutableComponent mutableComponent3 = mutableComponent.append(mutableComponent2);
         MutableComponent mutableComponent4 = Component.literal(" » ");
         MutableComponent mutableComponent5 = mutableComponent4.withStyle(ChatFormatting.DARK_GRAY);
         MutableComponent mutableComponent6 = mutableComponent3.append(mutableComponent5).append(text2);
         instance.player.sendSystemMessage(mutableComponent6);
      }
   }

   public static void displayError(String text) {
      MutableComponent mutableComponent = Component.literal(text);
      MutableComponent mutableComponent2 = mutableComponent.withStyle(ChatFormatting.RED);
      send(mutableComponent2);
   }

   public static void sendDonation(Component text) {
      sendChannel("DONATE", text);
   }

   public static void sendParty(String text) {
      sendChannel("PARTY", text);
   }

   public static void send(Component text) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         LocalPlayer player = instance.player;
         MutableComponent mutableComponent = prefix();
         MutableComponent mutableComponent2 = mutableComponent.append(Component.literal(" "));
         player.sendSystemMessage(mutableComponent2.append(text));
      }
   }

   private SendDonationHelper() {
   }

   public static void sendParty(Component text) {
      sendChannel("PARTY", text);
   }

   public static void sendIrc(Component text) {
      sendChannel("IRC", text);
   }

   public static void sendChannel(String text, String text2) {
      if (text2 != null) {
         if (!text2.isBlank()) {
            MutableComponent mutableComponent = Component.literal(text2);
            MutableComponent mutableComponent2 = mutableComponent.withStyle(ChatFormatting.RESET);
            sendChannel(text, mutableComponent2);
            return;
         }
      }
   }

   private static MutableComponent gradientTag(String text) {
      int computedValue = SkycoreClient.themes() != null ? SkycoreClient.themes().accent() : -10008600;
      int computedValue2 = WriteHexByteHelper.lerp(computedValue, -16777216, 0.45F);
      MutableComponent mutableComponent = Component.empty();
      int computedValue3 = Math.max(text.length() - 1, 1);

      for (int value = 0; value < text.length(); value++) {
         float computedValue4 = (float)value / computedValue3;
         int color = WriteHexByteHelper.lerp(computedValue, computedValue2, computedValue4) & 16777215;
         mutableComponent.append(Component.literal(String.valueOf(text.charAt(value))).setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true)));
      }

      return mutableComponent;
   }

   public static void displayInfo(String text) {
      MutableComponent mutableComponent = Component.literal(text);
      MutableComponent mutableComponent2 = mutableComponent.withStyle(ChatFormatting.GRAY);
      send(mutableComponent2);
   }

   public static void displayWarning(String text) {
      MutableComponent mutableComponent = Component.literal(text);
      MutableComponent mutableComponent2 = mutableComponent.withStyle(ChatFormatting.YELLOW);
      send(mutableComponent2);
   }

   public static MutableComponent prefix() {
      int computedValue = SkycoreClient.themes() != null ? SkycoreClient.themes().accent() : -10008600;
      int computedValue2 = WriteHexByteHelper.lerp(computedValue, -16777216, 0.45F);
      MutableComponent mutableComponent = Component.empty();
      int computedValue3 = Math.max("SkyCore Client ⇨".length() - 1, 1);

      for (int value = 0; value < "SkyCore Client ⇨".length(); value++) {
         char character = "SkyCore Client ⇨".charAt(value);
         MutableComponent mutableComponent2 = Component.literal(String.valueOf(character));
         if (character == 8680) {
            mutableComponent2.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(8947848)));
         } else {
            float computedValue4 = (float)value / computedValue3;
            int color = WriteHexByteHelper.lerp(computedValue, computedValue2, computedValue4) & 16777215;
            mutableComponent2.setStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)).withBold(true));
         }

         mutableComponent.append(mutableComponent2);
      }

      return mutableComponent;
   }

   public static void sendIrc(String text) {
      sendChannel("IRC", text);
   }
}
