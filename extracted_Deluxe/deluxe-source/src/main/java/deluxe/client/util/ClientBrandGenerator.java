package deluxe.client.util;

import deluxe.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import net.minecraft.entity.player.PlayerModelPart;
import net.minecraft.network.packet.c2s.common.SyncedClientOptions;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.util.Arm;

public class ClientBrandGenerator implements MinecraftAccess {
   private final Random field10410 = new Random();
   private final ArrayList<ClientBrand> field10411 = new ArrayList<>(
      Arrays.asList(
         new ClientBrand("Vanilla", "vanilla", "1.21.11", "1.21.10", "1.21.9", "1.21.8"),
         new ClientBrand("OptiFine", "vanilla", "1.21.11_HD_U_J7", "1.21.9_HD_U_J6", "1.21.8_HD_U_J5"),
         new ClientBrand("Fabric", "fabric", "0.18.0", "0.17.2", "0.16.14"),
         new ClientBrand("Forge", "forge", "58.0.12", "57.1.4", "56.0.34"),
         new ClientBrand("NeoForge", "neoforge", "21.11.14", "21.11.7", "21.10.29"),
         new ClientBrand("Quilt", "quilt", "0.29.1", "0.28.3"),
         new ClientBrand("Lunar Client", "lunarclient:v%s", "3.4.11-8f21a0c", "3.4.9-b7d1a02", "3.3.7-c40e91d"),
         new ClientBrand("LabyMod", "labymod:%s", "4.2.41", "4.2.35", "4.2.28"),
         new ClientBrand("Feather", "feather:%s", "1.9.4", "1.9.1", "1.8.7"),
         new ClientBrand("CheatBreaker", "cheatbreaker", "1.8.9", "1.12.2")
      )
   );
   private final ArrayList<String> field10412 = new ArrayList<>(
      Arrays.asList("ru_ru", "ru_ru", "ru_ru", "ru_ru", "ru_ru", "ru_ru", "ru_ru", "en_us", "en_us", "uk_ua", "be_by", "kk_kz", "en_gb")
   );

   public ClientIdentity method3066() {
      ClientBrand clientBrand = this.field10411.get(this.field10410.nextInt(this.field10411.size()));
      SyncedClientOptions syncedClientOptions = mc.options == null ? SyncedClientOptions.createDefault() : mc.options.getSyncedOptions();
      int local4897_3 = syncedClientOptions.playerModelParts();
      if (this.field10410.nextInt(3) == 0) {
         for (PlayerModelPart playerModelPart : PlayerModelPart.values()) {
            local4897_3 = this.field10410.nextInt(4) == 0 ? local4897_3 & ~playerModelPart.getBitFlag() : local4897_3 | playerModelPart.getBitFlag();
         }
      }

      return new ClientIdentity(
         clientBrand,
         clientBrand.method0829().get(this.field10410.nextInt(clientBrand.method0829().size())),
         new SyncedClientOptions(
            this.field10412.get(this.field10410.nextInt(this.field10412.size())),
            Math.min(32, syncedClientOptions.viewDistance() + this.field10410.nextInt(5)),
            syncedClientOptions.chatVisibility(),
            syncedClientOptions.chatColorsEnabled(),
            local4897_3,
            this.field10410.nextInt(9) == 0 ? Arm.LEFT : Arm.RIGHT,
            this.field10410.nextBoolean(),
            this.field10410.nextBoolean(),
            ParticlesMode.values()[this.field10410.nextInt(ParticlesMode.values().length)]
         )
      );
   }
}
