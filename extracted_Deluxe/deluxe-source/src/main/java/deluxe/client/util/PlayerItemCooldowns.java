package deluxe.client.util;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class PlayerItemCooldowns {
   private static final Map<UUID, PlayerItemCooldowns.Nested0448> field09209 = new HashMap<>();
   private static ClientWorld field09210;

   public static Map<ServerItemType, Long> method2679(AbstractClientPlayerEntity abstractClientPlayerEntity) {
      PlayerItemCooldowns.Nested0448 local4179_1 = field09209.get(abstractClientPlayerEntity.getUuid());
      if (local4179_1 != null && local4179_1.entityId() == abstractClientPlayerEntity.getId()) {
         if (abstractClientPlayerEntity != MinecraftClient.getInstance().player) {
            return local4179_1.expiresAtMs();
         }

         EnumMap<ServerItemType, Long> local4179_2 = new EnumMap<>(ServerItemType.class);
         long local4179_3 = System.currentTimeMillis();

         for (ServerItemType serverItemType : local4179_1.expiresAtMs().keySet()) {
            int local4179_7 = 0;

            for (ServerItemType.Nested0434 local4179_9 : serverItemType.method2461()) {
               StatusEffectInstance statusEffectInstance = abstractClientPlayerEntity.getStatusEffect(local4179_9.effect());
               if (statusEffectInstance == null || !ServerItemType.method2467(statusEffectInstance.getAmplifier(), local4179_9.amplifier())) {
                  local4179_7 = 0;
                  break;
               }

               local4179_7 = Math.max(local4179_7, statusEffectInstance.getDuration());
            }

            if (local4179_7 > 0) {
               local4179_2.put(serverItemType, local4179_3 + local4179_7 * 50L);
            }
         }

         return local4179_2;
      } else {
         return Map.of();
      }
   }

   private PlayerItemCooldowns() {
   }

   public static void method2682(ServerItemType serverItemType, BlockPos blockPos) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient.world != null && ServerItemType.method2469()) {
         method2683(minecraftClient.world);
         Vec3d vec3d = blockPos.toCenterPos();
         long local4184_4 = System.currentTimeMillis();

         for (AbstractClientPlayerEntity abstractClientPlayerEntity : minecraftClient.world.getPlayers()) {
            if (abstractClientPlayerEntity.isAlive()) {
               int local4184_8 = serverItemType.method2464(ServerItemType.method2468(abstractClientPlayerEntity.getBoundingBox(), vec3d));
               if (local4184_8 > 0) {
                  PlayerItemCooldowns.Nested0448 local4184_9 = field09209.get(abstractClientPlayerEntity.getUuid());
                  if (local4184_9 == null || local4184_9.entityId() != abstractClientPlayerEntity.getId()) {
                     local4184_9 = new PlayerItemCooldowns.Nested0448(abstractClientPlayerEntity.getId(), new EnumMap<>(ServerItemType.class));
                     field09209.put(abstractClientPlayerEntity.getUuid(), local4184_9);
                  }

                  local4184_9.expiresAtMs().merge(serverItemType, local4184_4 + local4184_8 * 50L, Math::max);
               }
            }
         }
      }
   }

   public static void method2683(ClientWorld clientWorld) {
      if (clientWorld != field09210) {
         field09209.clear();
         field09210 = clientWorld;
      }

      if (!field09209.isEmpty()) {
         long local4185_1 = System.currentTimeMillis();
         field09209.entrySet()
            .removeIf(
               local4183_3 -> {
                  PlayerEntity playerEntity = clientWorld.getPlayerByUuid(local4183_3.getKey());
                  if (playerEntity != null
                     && !playerEntity.isRemoved()
                     && !(playerEntity.getHealth() <= 0.0F)
                     && playerEntity.getId() == local4183_3.getValue().entityId()) {
                     local4183_3.getValue().expiresAtMs().values().removeIf(local4182_2 -> local4182_2 <= local4185_1);
                     return local4183_3.getValue().expiresAtMs().isEmpty();
                  } else {
                     return true;
                  }
               }
            );
      }
   }

   private record Nested0448(int entityId, EnumMap<ServerItemType, Long> expiresAtMs) {
   }
}
