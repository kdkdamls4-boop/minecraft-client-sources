package deluxe.client.util;

import java.util.ArrayList;
import java.util.Arrays;

public class ClientBrand {
   private String field01485;
   private String field01486;
   private ArrayList<String> field01487;

   public String method0826() {
      return this.field01485;
   }

   public String method0827() {
      return this.field01486;
   }

   public String method0828(String local1106_1) {
      return this.field01486.formatted(local1106_1);
   }

   public ClientBrand(String local1107_1, String local1107_2, String... stringArray) {
      this.field01485 = local1107_1;
      this.field01486 = local1107_2;
      this.field01487 = new ArrayList<>(Arrays.asList(stringArray));
   }

   public ArrayList<String> method0829() {
      return this.field01487;
   }
}
