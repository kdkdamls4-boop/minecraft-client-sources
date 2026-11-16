package deluxe.client.render;

import deluxe.client.storage.AccountEntry;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.entity.player.SkinTextures;

public final class AccountPreviewRenderer {
   private static PlayerEntityModel field00014;
   private static PlayerEntityModel field00015;
   private static final ConcurrentHashMap<String, Supplier<SkinTextures>> field00016 = new ConcurrentHashMap<>();
   private static final ConcurrentHashMap<String, Boolean> field00017 = new ConcurrentHashMap<>();
   private static final HttpClient HTTP = HttpClient.newHttpClient();
   private static final Pattern field00018 = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
   private static final Pattern field00019 = Pattern.compile(
      "\\{[^{}]*?\"name\"\\s*:\\s*\"textures\"[^{}]*?\"value\"\\s*:\\s*\"([^\"]+)\"(?:[^{}]*?\"signature\"\\s*:\\s*\"([^\"]+)\")?[^{}]*?\\}"
   );
   private static int field00020;

   private AccountPreviewRenderer() {
   }

   private static void method0453(PlayerEntityModel playerEntityModel) {
      float local501_1 = 0.05F;
      playerEntityModel.leftArm.pitch = local501_1;
      playerEntityModel.rightArm.pitch = -local501_1;
      playerEntityModel.leftLeg.pitch = -local501_1;
      playerEntityModel.rightLeg.pitch = local501_1;
      playerEntityModel.leftSleeve.pitch = local501_1;
      playerEntityModel.rightSleeve.pitch = -local501_1;
      playerEntityModel.leftPants.pitch = -local501_1;
      playerEntityModel.rightPants.pitch = local501_1;
      playerEntityModel.head.pitch = 0.0F;
      playerEntityModel.hat.pitch = 0.0F;
   }

   public static void method0454(DrawContext drawContext, AccountEntry accountEntry, float local502_2, float local502_3, float local502_4, float local502_5) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         try {
            method0458(minecraftClient);
         } catch (RuntimeException exception) {
            return;
         }

         SkinTextures skinTextures = method0456(accountEntry.method3455(), accountEntry.method3450());
         PlayerEntityModel playerEntityModel = skinTextures.model() == PlayerSkinType.SLIM ? field00015 : field00014;
         method0453(playerEntityModel);
         drawContext.addPlayerSkin(
            playerEntityModel,
            skinTextures.body().texturePath(),
            Math.round(local502_5 / local502_2),
            -10.0F,
            20.0F,
            -40.0F,
            Math.round(local502_3 / local502_2) - Math.round(Math.round(local502_5 / local502_2) * 0.8F),
            Math.round(local502_4 / local502_2) - Math.round(Math.round(local502_5 / local502_2) * 0.8F),
            Math.round(local502_3 / local502_2) + Math.round(Math.round(local502_5 / local502_2) * 0.8F),
            Math.round(local502_4 / local502_2) + Math.round(Math.round(local502_5 / local502_2) * 0.8F)
         );
      }
   }

   private static void method0455(MinecraftClient minecraftClient, String local503_1) {
      if (local503_1.matches("[A-Za-z0-9_]{1,16}")) {
         if (field00017.putIfAbsent(local503_1, Boolean.TRUE) == null) {
            Thread thread = new Thread(
               () -> {
                  try {
                     Matcher matcher = field00018.matcher(method0457("https://api.mojang.com/users/profiles/minecraft/" + local503_1));
                     if (!matcher.find()) {
                        return;
                     }

                     Matcher matcher2 = field00019.matcher(
                        method0457("https://sessionserver.mojang.com/session/minecraft/profile/" + matcher.group(1) + "?unsigned=false")
                     );
                     if (!matcher2.find()) {
                        return;
                     }

                     GameProfile gameProfile = new GameProfile(
                        UUID.fromString(matcher.group(1).replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5")), local503_1
                     );
                     gameProfile.properties()
                        .put(
                           "textures",
                           matcher2.group(2) != null
                              ? new Property("textures", matcher2.group(1), matcher2.group(2))
                              : new Property("textures", matcher2.group(1))
                        );
                     minecraftClient.execute(() -> field00016.put(local503_1, minecraftClient.getSkinProvider().supplySkinTextures(gameProfile, false)));
                  } catch (Exception exception) {
                  }
               },
               "deluxe-skin-" + local503_1
            );
            thread.setDaemon(true);
            thread.start();
         }
      }
   }

   public static SkinTextures method0456(String local504_0, UUID uUID) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Supplier<SkinTextures> local504_3 = field00016.get(local504_0);
      if (local504_3 == null) {
         local504_3 = minecraftClient.getSkinProvider().supplySkinTextures(new GameProfile(uUID, local504_0), false);
         field00016.put(local504_0, local504_3);
         method0455(minecraftClient, local504_0);
      }

      return local504_3.get();
   }

   private static String method0457(String local505_0) throws Exception {
      HttpResponse<InputStream> local505_1 = HTTP.send(
         HttpRequest.newBuilder(URI.create(local505_0)).header("Accept", "application/json").GET().build(), BodyHandlers.ofInputStream()
      );
      if (local505_1.statusCode() != 200) {
         return "";
      }

      try (InputStream inputStream = local505_1.body()) {
         return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }
   }

   private static void method0458(MinecraftClient minecraftClient) {
      if (field00014 == null) {
         field00014 = new PlayerEntityModel(minecraftClient.getLoadedEntityModels().getModelPart(EntityModelLayers.PLAYER), false);
         field00015 = new PlayerEntityModel(minecraftClient.getLoadedEntityModels().getModelPart(new net.minecraft.client.render.entity.model.EntityModelLayer(net.minecraft.util.Identifier.ofVanilla("player_slim"), "main")), true);
      }
   }

   public static void method0459(DrawContext drawContext, ArrayList<AccountEntry> local508_1) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         try {
            method0458(minecraftClient);
         } catch (RuntimeException exception) {
            return;
         }

         SkinTextures skinTextures = method0456("jeb_", AccountEntry.method3449("jeb_"));
         Render2D.method4677(skinTextures.body().texturePath().toString(), 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, 1.0F, 0.125F, 0.125F, 0.25F, 0.25F, -1);
         if (!local508_1.isEmpty()) {
            skinTextures = method0456(
               local508_1.get(Math.floorMod(field00020, local508_1.size())).method3455(),
               local508_1.get(Math.floorMod(field00020++, local508_1.size())).method3450()
            );
         }

         PlayerEntityModel playerEntityModel = skinTextures.model() == PlayerSkinType.SLIM ? field00015 : field00014;
         method0453(playerEntityModel);

         try {
            drawContext.addPlayerSkin(playerEntityModel, skinTextures.body().texturePath(), 24.0F, -10.0F, 20.0F, -40.0F, 0, 0, 24, 24);
         } catch (RuntimeException exception2) {
         }
      }
   }
}
