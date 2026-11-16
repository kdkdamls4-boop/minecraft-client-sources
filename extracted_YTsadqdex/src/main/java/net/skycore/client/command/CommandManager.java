package net.skycore.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.util.SendDonationHelper;

public final class CommandManager {
   private final List<ClientCommand> commands;
   private String prefix = ".";
   private final CommandDispatcher<ClientSuggestionProvider> dispatcher = new CommandDispatcher<>();

   public boolean tryExecute(String text) {
      if (text == null || this.prefix == null || this.prefix.isEmpty()) {
         return false;
      }

      if (!text.startsWith(this.prefix)) {
         return false;
      }

      String text2 = text.substring(this.prefix.length()).trim();
      if (text2.isEmpty()) {
         return true;
      }

      ClientSuggestionProvider source = this.getSource();
      if (source == null) {
         SendDonationHelper.displayError("Not connected");
         return true;
      }

      try {
         this.dispatcher.execute(text2, source);
      } catch (CommandSyntaxException failure) {
         String message = failure.getRawMessage() != null ? failure.getRawMessage().getString() : failure.getMessage();
         SendDonationHelper.displayError(message != null ? message : "Command error");
      } catch (Exception failure2) {
         SendDonationHelper.displayError("Error: " + failure2.getMessage());
      }

      return true;
   }

   public CommandManager() {
      this.commands = new ArrayList<>();
      this.registerCommand(new HelpCommand(this));
      this.registerCommand(new BindCommand());
      this.registerCommand(new CfgCommand());
      this.registerCommand(new VclipCommand());
      this.registerCommand(new HclipCommand());
      this.registerCommand(new FriendCommand());
      this.registerCommand(new CustomnameCommand());
      this.registerCommand(new MacroCommand());
      this.registerCommand(new RctCommand());
      this.registerCommand(new StaffCommand());
      this.registerCommand(new GpsCommand());
      this.registerCommand(new OnlineCommand());
      this.registerCommand(new PartyCommand());
      this.registerCommand(new IrcCommand());
      this.registerCommand(new NeuroCommand());
      this.registerCommand(new PrefixCommand());
      this.registerCommand(FakeplayerCommand.INSTANCE);
      this.registerCommand(new TpCommand());
      this.registerCommand(new ViewmodelCommand());
      this.registerCommand(new ViewmodelCommand("vm"));
      this.registerCommand(new WaypointCommand("way"));
      this.registerCommand(new WaypointCommand("waypoint"));
   }

   public void setPrefix(String prefix2) {
      if (validatePrefix(prefix2) == null) {
         this.prefix = prefix2;
      }
   }

   public void registerCommand(ClientCommand clientCommand) {
      if (clientCommand != null) {
         clientCommand.register(this.dispatcher);
         this.commands.add(clientCommand);
      }
   }

   public void loadPrefix(String prefix2) {
      if (validatePrefix(prefix2) == null) {
         this.prefix = prefix2;
      }
   }

   public CommandDispatcher<ClientSuggestionProvider> getDispatcher() {
      return this.dispatcher;
   }

   public ClientSuggestionProvider getSource() {
      ClientPacketListener connection = Minecraft.getInstance().getConnection();
      ClientSuggestionProvider clientSuggestionProvider;
      if (connection != null) {
         clientSuggestionProvider = connection.getSuggestionsProvider();
      } else {
         clientSuggestionProvider = null;
      }

      return clientSuggestionProvider;
   }

   public String getPrefix() {
      return this.prefix;
   }

   public List<ClientCommand> getCommands() {
      return List.copyOf(this.commands);
   }

   public static String validatePrefix(String text) {
      if (text != null) {
         if (!text.isEmpty()) {
            if (text.length() > 5) {
               return "Prefix is too long (max. 5 characters)";
            }

            if (text.contains(" ")) {
               return "Prefix cannot contain spaces";
            }

            if (text.startsWith("/")) {
               return "Prefix cannot start with /";
            }

            return null;
         }
      }

      return "Prefix cannot be empty";
   }
}
