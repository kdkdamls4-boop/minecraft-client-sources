package net.skycore.client.command;

import net.minecraft.network.chat.Component;
import net.skycore.client.util.SendDonationHelper;

public final class ChatMessages {
   public static void displayError(String text) {
      SendDonationHelper.displayError(text);
   }

   private ChatMessages() {
   }

   public static void send(Component text) {
      SendDonationHelper.send(text);
   }

   public static void displayInfo(String text) {
      SendDonationHelper.displayInfo(text);
   }
}
