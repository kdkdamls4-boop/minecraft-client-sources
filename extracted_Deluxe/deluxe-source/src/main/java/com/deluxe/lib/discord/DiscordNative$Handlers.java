package com.deluxe.lib.discord;

import deluxe.client.util.DiscordRpcLibrary;
import ru.dreamix.fabricloader.VMBridge;

import com.sun.jna.Structure;
import java.util.List;

public class DiscordNative$Handlers extends Structure {
   public DiscordRpcLibrary.Nested0456 ready;
   public DiscordRpcLibrary.Nested0457 disconnected;
   public DiscordRpcLibrary.Nested0457 errored;
   public DiscordRpcLibrary.Nested0458 joinGame;
   public DiscordRpcLibrary.Nested0458 spectateGame;
   public DiscordRpcLibrary.Nested0455 joinRequest;

   static {
      VMBridge.identifyClass(DiscordNative$Handlers.class, "xtUTdsvN");
   }

   protected List<String> getFieldOrder() {
      return List.of("ready", "disconnected", "errored", "joinGame", "spectateGame", "joinRequest");
   }
}
