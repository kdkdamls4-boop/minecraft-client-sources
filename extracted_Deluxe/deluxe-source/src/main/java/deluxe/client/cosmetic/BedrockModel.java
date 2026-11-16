package deluxe.client.cosmetic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BedrockModel {
   public List<BedrockBone> field03648 = new ArrayList<>();
   public int field03649 = 64;
   public int field03650 = 64;

   private BedrockBone method1307(String local1816_1, BedrockBone bedrockBone) {
      if (bedrockBone.field01589.equals(local1816_1)) {
         return bedrockBone;
      }

      for (BedrockBone bedrockBone2 : bedrockBone.field01587) {
         BedrockBone bedrockBone3 = this.method1307(local1816_1, bedrockBone2);
         if (bedrockBone3 != null) {
            return bedrockBone3;
         }
      }

      return null;
   }

   public Optional<BedrockBone> method1308(String local1817_1) {
      for (BedrockBone bedrockBone : this.field03648) {
         BedrockBone bedrockBone2 = this.method1307(local1817_1, bedrockBone);
         if (bedrockBone2 != null) {
            return Optional.of(bedrockBone2);
         }
      }

      return Optional.empty();
   }
}
