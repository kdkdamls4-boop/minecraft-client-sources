package client.network;

import b.Boot;
import client.transform.NativeBridgeUtil;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class SocketClient {
   public final Consumer<JsonObject> consumer;
   private WebSocket webSocket;
   private boolean flag;

   public SocketClient(Consumer consumer2) {
      this.consumer = consumer2;
   }

   public boolean isFlag() {
      return this.flag;
   }

   public void onStringJsonObject(String text, JsonObject jsonObject) {
      WebSocket websocket = this.webSocket;
      if (websocket != null) {
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("op", "pub");
         jsonobject.addProperty("topic", text);
         jsonobject.add("data", jsonObject);
         websocket.sendText(jsonobject.toString(), true);
      }
   }

   public void update() {
      this.flag = true;
      WebSocket websocket = this.webSocket;
      if (websocket != null) {
         try {
            websocket.sendClose(1000, "bye");
         } catch (Exception exception) {
         }
      }

      this.webSocket = null;
   }

   public boolean check() {
      // Remote websocket connection and HWID/session transmission neutralized
      return false;
   }

   public void onString(String text) {
      WebSocket websocket = this.webSocket;
      if (websocket != null) {
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("op", "sub");
         jsonobject.addProperty("topic", text);
         websocket.sendText(jsonobject.toString(), true);
      }
   }
}
