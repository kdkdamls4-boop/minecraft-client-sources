package deluxe.client.util;

import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.util.Arm;

public class ClientIdentity {
   private ClientBrand clientBrand;
   private String field10627;
   private SyncedClientOptions field10628;

   public String method3109() {
      return "%s %s (%s) · %s · %d чанков · %s"
         .formatted(
            this.clientBrand.method0826(),
            this.field10627,
            this.clientBrand.method0828(this.field10627),
            this.field10628.language(),
            this.field10628.viewDistance(),
            this.field10628.mainArm() == Arm.LEFT ? "левша" : "правша"
         );
   }

   public SyncedClientOptions method3110() {
      return this.field10628;
   }

   public String method3111() {
      return this.field10627;
   }

   public ClientIdentity(ClientBrand initialClientBrand, String local4966_2, SyncedClientOptions syncedClientOptions) {
      this.clientBrand = initialClientBrand;
      this.field10627 = local4966_2;
      this.field10628 = syncedClientOptions;
   }

   public ClientBrand getClientBrand() {
      return this.clientBrand;
   }
}
