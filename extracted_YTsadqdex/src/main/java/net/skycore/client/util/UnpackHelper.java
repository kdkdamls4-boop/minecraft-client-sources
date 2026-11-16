package net.skycore.client.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

public final class UnpackHelper {
   private static final Gson GSON = new Gson();

   public static MinecraftProfileTextures unpack(Property property) {
      if (property == null) {
         return MinecraftProfileTextures.EMPTY;
      }

      Property property2 = FindTextureInTagHelper.fixPropertyKeepSignature(property);
      String text = property2 != null ? property2.value() : property.value();
      if (text != null && !text.isBlank()) {
         try {
            byte[] byteValues = FindTextureInTagHelper.decodeBase64(text);
            return byteValues == null ? fromRawJson(text.trim()) : fromRawJson(new String(byteValues, StandardCharsets.UTF_8));
         } catch (Exception failure) {
            return MinecraftProfileTextures.EMPTY;
         }
      } else {
         return MinecraftProfileTextures.EMPTY;
      }
   }

   private UnpackHelper() {
   }

   private static boolean isTextureHash(String text) {
      if (text != null && text.length() >= 32 && text.length() <= 128) {
         for (int value = 0; value < text.length(); value++) {
            char character = text.charAt(value);
            int value2 = (character < '0' || character > '9') && (character < 'a' || character > 'f') && (character < 'A' || character > 'F') ? 0 : 1;
            if (value2 == 0) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static MinecraftProfileTexture readTexture(JsonObject json, String... stringValues) {
      JsonObject json2 = null;

      for (String text : stringValues) {
         if (json.has(text) && json.get(text).isJsonObject()) {
            json2 = json.getAsJsonObject(text);
            break;
         }
      }

      if (json2 == null) {
         return null;
      }

      String text2 = null;
      if (json2.has("url") && json2.get("url").isJsonPrimitive()) {
         text2 = json2.get("url").getAsString();
      } else if (json2.has("URL") && json2.get("URL").isJsonPrimitive()) {
         text2 = json2.get("URL").getAsString();
      }

      if (text2 != null && !text2.isBlank()) {
         text2 = text2.trim().replace("\\/", "/");
         String text3 = text2.toLowerCase(Locale.ROOT);
         if (!text3.startsWith("http://") && !text3.startsWith("https://")) {
            if (!isTextureHash(text2)) {
               return null;
            }

            text2 = "https://textures.minecraft.net/texture/" + text2;
         }

         java.util.Map<String, String> lookup = null;
         if (json2.has("metadata") && json2.get("metadata").isJsonObject()) {
            lookup = new HashMap();
            JsonObject asJsonObject = json2.getAsJsonObject("metadata");

            for (Entry entry : asJsonObject.entrySet()) {
               if (((JsonElement)entry.getValue()).isJsonPrimitive()) {
                  lookup.put((String)entry.getKey(), ((JsonElement)entry.getValue()).getAsString());
               }
            }
         }

         return new MinecraftProfileTexture(text2, lookup);
      } else {
         return null;
      }
   }

   private static MinecraftProfileTextures fromRawJson(String text) {
      try {
         JsonObject json = GSON.fromJson(text, JsonObject.class);
         if (json == null) {
            return MinecraftProfileTextures.EMPTY;
         }

         JsonObject json2 = null;
         if (json.has("textures") && json.get("textures").isJsonObject()) {
            json2 = json.getAsJsonObject("textures");
         }

         if (json2 == null) {
            return MinecraftProfileTextures.EMPTY;
         }

         MinecraftProfileTexture minecraftProfileTexture = readTexture(json2, "SKIN", "skin");
         return minecraftProfileTexture == null
            ? MinecraftProfileTextures.EMPTY
            : new MinecraftProfileTextures(minecraftProfileTexture, readTexture(json2, "CAPE", "cape"), readTexture(json2, "ELYTRA", "elytra"), SignatureState.UNSIGNED);
      } catch (Exception failure) {
         return MinecraftProfileTextures.EMPTY;
      }
   }
}
