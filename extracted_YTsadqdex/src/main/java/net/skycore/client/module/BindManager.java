package net.skycore.client.module;

import java.util.IdentityHashMap;
import java.util.Map;

public final class BindManager {
   private final Map<ClientModule, Boolean> states = new IdentityHashMap<>();

   public void update(ClientModule clientModule, boolean condition) {
      boolean orDefault = this.states.getOrDefault(clientModule, false);
      if (clientModule.getBindMode() == BindMode.HOLD) {
         clientModule.setEnabled(condition);
      } else if (condition && !orDefault) {
         clientModule.toggle();
      }

      var states2 = this.states;
      Boolean condition2 = condition;
      states2.put(clientModule, condition2);
   }

   public void clear() {
      this.states.clear();
   }
}
