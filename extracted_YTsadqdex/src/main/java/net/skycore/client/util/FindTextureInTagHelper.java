package net.skycore.client.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.TypedEntityData;

public final class FindTextureInTagHelper {
   private static final ThreadLocal<Boolean> FIXING = ThreadLocal.withInitial(() -> {
      byte flag = 0;
      flag = 0;
      byte flag2 = -1;
      return false;
   });

   private static String findTextureInTag(Tag tag) {
      ArrayList<String> strings = new ArrayList();
      collectTextureCandidates(tag, strings);
      String text = null;

      for (String text2 : strings) {
         if (looksLikeTexturePayload(text2)) {
            text = text2;
         }
      }

      return text;
   }

   private static ResolvableProfile fixResolvableProfile(ResolvableProfile resolvableProfile) {
      GameProfile gameProfile = resolvableProfile.partialProfile();
      PropertyMap lookup = gameProfile.properties();
      String text = gameProfile.name();
      int value = text != null && !text.isBlank() ? 0 : 1;
      PropertyMap lookup2 = lookup != null && !lookup.isEmpty() ? fixPropertyMap(lookup) : lookup;
      int value2 = lookup2 != lookup ? 1 : 0;
      if (value == 0 && value2 == 0) {
         return resolvableProfile;
      }

      String text2 = value != 0 ? "skycore" : text;
      PropertyMap lookup3 = lookup2 != null ? lookup2 : (lookup != null ? lookup : new PropertyMap(ImmutableMultimap.of()));
      return ResolvableProfile.createResolved(new GameProfile(gameProfile.id(), text2, lookup3));
   }

   public static void normalize(CompoundTag compoundTag) {
      if (compoundTag != null) {
         String textureInTag = findTextureInTag(compoundTag);
         if (textureInTag != null) {
            UUID uUID = readUuid(compoundTag.get("id"));
            if (uUID == null) {
               uUID = readUuid(compoundTag.get("Id"));
            }

            CompoundTag compoundTag2 = profileTagFromTexture(textureInTag, uUID);
            compoundTag.remove("SkullOwner");
            compoundTag.remove("Owner");
            compoundTag.remove("Profile");
            compoundTag.put("profile", compoundTag2);
         } else {
            CompoundTag compound = getCompound(compoundTag, "profile", "Profile");
            if (compound != null) {
               normalizeProfileCompound(compound);
            }
         }
      }
   }

   private static Property fixProperty(Property property, boolean condition) {
      if (property == null) {
         return null;
      }

      String text = property.name();
      String text2 = property.value();
      String text3 = property.signature();
      byte flag = 0;
      if ((text2 == null || text2.isBlank()) && text3 != null && looksLikeTexturePayload(text3)) {
         text2 = text3;
         text3 = null;
         text = "textures";
         flag = 1;
      }

      if (!"textures".equals(text) && looksLikeTexturePayload(text2)) {
         text = "textures";
         flag = 1;
      }

      if ("textures".equals(text) && text2 != null) {
         String text4 = sanitizeTextureValue(text2);
         if (!text4.equals(text2)) {
            text2 = text4;
            text3 = null;
            flag = 1;
         }
      }

      if (condition && text3 != null) {
         text3 = null;
         flag = 1;
      }

      if (flag == 0) {
         return property;
      } else {
         return text3 != null ? new Property(text, text2, text3) : new Property(text, text2);
      }
   }

   private static boolean sanitizeModernProperties(CompoundTag compoundTag) {
      if (compoundTag.get("properties") instanceof ListTag elements && !elements.isEmpty()) {
         byte flag = 0;

         for (int value = 0; value < elements.size(); value++) {
            if (elements.get(value) instanceof CompoundTag compoundTag2) {
               String text = firstString(compoundTag2, "value", "Value");
               String text2 = firstString(compoundTag2, "signature", "Signature");
               if ((text == null || text.isBlank()) && text2 != null && looksLikeTexturePayload(text2)) {
                  text = text2;
                  compoundTag2.remove("signature");
                  compoundTag2.remove("Signature");
                  flag = 1;
               }

               if (text != null) {
                  compoundTag2.remove("Value");
                  compoundTag2.putString("value", sanitizeTextureValue(text));
                  flag = 1;
               }

               if (compoundTag2.contains("signature") || compoundTag2.contains("Signature")) {
                  compoundTag2.remove("signature");
                  compoundTag2.remove("Signature");
                  flag = 1;
               }

               if (!compoundTag2.contains("name") && compoundTag2.contains("Name")) {
                  compoundTag2.getString("Name").ifPresent(text3 -> compoundTag2.putString("name", text3));
                  compoundTag2.remove("Name");
                  flag = 1;
               }

               if (!compoundTag2.contains("name")) {
                  compoundTag2.putString("name", "textures");
                  flag = 1;
               }
            }
         }

         return (flag != 0);
      } else {
         return false;
      }
   }

   private static PropertyMap fixPropertyMap(PropertyMap lookup) {
      Builder builder = ImmutableMultimap.builder();
      byte flag = 0;
      Iterator iterator = lookup.values().iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return flag != 0 ? new PropertyMap(builder.build()) : lookup;
         }

         Property element = (Property)iterator.next();
         Property property = fixProperty(element);
         if (property != element) {
            flag = 1;
         }

         String text = property.name();
         builder.put(text, property);
      }
   }

   public static String bestTextureValue(ItemStack itemStack) {
      FindTextureInTagHelper.TextureHit bestTextureHit = findBestTextureHit(itemStack);
      String text;
      if (bestTextureHit != null) {
         text = bestTextureHit.value();
      } else {
         text = null;
      }

      return text;
   }

   private static FindTextureInTagHelper.TextureHit findTextureInDisplay(ItemStack itemStack) {
      ItemLore itemLore = itemStack.get(DataComponents.LORE);
      if (itemLore != null) {
         for (Component text : itemLore.lines()) {
            String string = text.getString();
            FindTextureInTagHelper.TextureHit textureHit = textureHitFromText(string);
            if (textureHit != null) {
               return textureHit;
            }
         }
      }

      Component text2 = itemStack.get(DataComponents.CUSTOM_NAME);
      if (text2 != null) {
         return textureHitFromText(text2.getString());
      } else {
         return null;
      }
   }

   private static FindTextureInTagHelper.TextureHit scanLegacyTag(CompoundTag compoundTag) {
      if (compoundTag == null) {
         return null;
      }

      FindTextureInTagHelper.TextureHit textureHit = extractFromSkullOwner(getCompound(compoundTag, "SkullOwner", "Owner", "profile", "Profile"));
      if (textureHit != null) {
         return textureHit;
      }

      CompoundTag compound = getCompound(compoundTag, "tag", "Tag");
      if (compound != null) {
         textureHit = extractFromSkullOwner(getCompound(compound, "SkullOwner", "Owner", "profile", "Profile"));
         if (textureHit != null) {
            return textureHit;
         }

         FindTextureInTagHelper.TextureHit textureInCompound = findTextureInCompound(compound);
         if (textureInCompound != null) {
            return textureInCompound;
         }
      }

      CompoundTag compound2 = getCompound(compoundTag, "BlockEntityTag", "block_entity_data", "blockEntityTag");
      if (compound2 != null) {
         textureHit = extractFromSkullOwner(getCompound(compound2, "SkullOwner", "Owner", "profile", "Profile"));
         if (textureHit != null) {
            return textureHit;
         }

         FindTextureInTagHelper.TextureHit textureInCompound2 = findTextureInCompound(compound2);
         if (textureInCompound2 != null) {
            return textureInCompound2;
         }
      }

      return findTextureInCompound(compoundTag);
   }

   public static String padBase64(String text) {
      if (text != null && !text.isEmpty()) {
         int value = text.length() % 4;
         if (value == 0) {
            return text;
         }

         int intermediate = 4 - value;
         return text + "=".repeat(intermediate);
      } else {
         return text;
      }
   }

   private static void collectTextureCandidates(Tag tag, List<String> strings) {
      if (tag != null) {
         if (tag instanceof StringTag stringTag) {
            String text = stringTag.value();
            if (looksLikeTexturePayload(text)) {
               strings.add(text);
            }
         } else if (tag instanceof CompoundTag compoundTag) {
            for (String text2 : List.of("value", "Value", "textures", "Textures")) {
               Tag tag2 = compoundTag.get(text2);
               if (tag2 != null) {
                  collectTextureCandidates(tag2, strings);
               }
            }

            for (String text3 : compoundTag.keySet()) {
               if (!text3.equals("value") && !text3.equals("Value") && !text3.equals("textures") && !text3.equals("Textures")) {
                  collectTextureCandidates(compoundTag.get(text3), strings);
               }
            }
         } else {
            if (tag instanceof ListTag) {
               for (Tag tag3 : (ListTag)tag) {
                  collectTextureCandidates(tag3, strings);
               }
            }
         }
      }
   }

   private static FindTextureInTagHelper.TextureHit findTextureViaBlob(ItemStack itemStack) {
      try {
         String text = null;
         String text2 = null;
         Pattern pattern = Pattern.compile("(eyJ[A-Za-z0-9_\\-/=+]{40,}|ewog[A-Za-z0-9_\\-/=+]{40,})");
         Pattern pattern2 = Pattern.compile("https?://textures\\.minecraft\\.net/texture/[a-fA-F0-9]{32,128}", 2);

         for (String text3 : orderedBlobParts(itemStack)) {
            if (text3 != null && text3.length() >= 20) {
               String text4 = text3.replace("\\/", "/");
               Matcher matcher = pattern2.matcher(text4);

               while (matcher.find()) {
                  text2 = matcher.group();
               }

               Matcher matcher2 = pattern.matcher(text4);

               while (matcher2.find()) {
                  String text5 = matcher2.group().replace(" ", "");
                  if (looksLikeTexturePayload(text5)) {
                     text = text5;
                  }
               }
            }
         }

         if (text != null) {
            return new FindTextureInTagHelper.TextureHit(text, null);
         }

         if (text2 != null) {
            return new FindTextureInTagHelper.TextureHit(text2, null);
         }
      } catch (Throwable failure) {
      }

      return null;
   }

   public static void fixItemStack(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty() && itemStack.is(Items.PLAYER_HEAD)) {
         if (!Boolean.TRUE.equals(FIXING.get())) {
            FIXING.set(true);

            try {
               FindTextureInTagHelper.TextureHit bestTextureHit = findBestTextureHit(itemStack);
               if (bestTextureHit == null) {
                  ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
                  if (resolvableProfile == null) {
                     return;
                  }

                  ResolvableProfile resolvableProfile2 = fixResolvableProfile(resolvableProfile);
                  if (resolvableProfile2 != null && resolvableProfile2 != resolvableProfile) {
                     itemStack.set(DataComponents.PROFILE, resolvableProfile2);
                  }

                  ensureTextureUniqueProfile(itemStack);
                  TextureHashHelper.preloadFromStack(itemStack);
                  return;
               }

               applyTextureProfile(itemStack, bestTextureHit.value(), bestTextureHit.uuid());
               TextureHashHelper.preloadFromStack(itemStack);
            } finally {
               FIXING.set(false);
            }
         }
      }
   }

   private static FindTextureInTagHelper.TextureHit findTextureFromProfileOnly(ItemStack itemStack) {
      ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
      if (resolvableProfile == null) {
         return null;
      }

      PropertyMap lookup = resolvableProfile.partialProfile().properties();
      if (lookup == null) {
         return null;
      }

      for (Property property : lookup.values()) {
         Property property2 = fixProperty(property);
         if (property2 != null && looksLikeTexturePayload(property2.value())) {
            return new FindTextureInTagHelper.TextureHit(property2.value(), resolvableProfile.partialProfile().id());
         }
      }

      return null;
   }

   private static FindTextureInTagHelper.TextureHit findTextureInLegacyComponents(ItemStack itemStack) {
      CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
      if (customData != null) {
         CompoundTag compoundTag = customData.copyTag();
         FindTextureInTagHelper.TextureHit textureHit = scanLegacyTag(compoundTag);
         if (textureHit != null) {
            return textureHit;
         }
      }

      TypedEntityData typedEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
      if (typedEntityData != null) {
         CompoundTag compoundTag2 = typedEntityData.copyTagWithoutId();
         return scanLegacyTag(compoundTag2);
      } else {
         return null;
      }
   }

   private static String sanitizeTextureValue(String text) {
      String text2 = text == null ? "" : text.trim();
      if (isRawTextureUrl(text2)) {
         return encodeTexturesJson(text2);
      }

      if (isTextureHash(text2)) {
         return encodeTexturesJson("https://textures.minecraft.net/texture/" + text2);
      }

      String text3 = padBase64(text2.replace("\r", "").replace("\n", "").replace(" ", ""));
      byte[] byteValues = decodeBase64(text3);
      if (byteValues == null) {
         return text3;
      }

      String text4 = new String(byteValues, StandardCharsets.UTF_8);
      String text5 = text4.replace("http://textures.minecraft.net", "https://textures.minecraft.net")
         .replace("http:\\/\\/textures.minecraft.net", "https:\\/\\/textures.minecraft.net");
      return padBase64(Base64.getEncoder().encodeToString(text5.getBytes(StandardCharsets.UTF_8)));
   }

   private static void addRewritten(ListTag elements, String text, Tag tag) {
      if (tag instanceof CompoundTag compoundTag) {
         String text2 = firstString(compoundTag, "value", "Value");
         String text3 = firstString(compoundTag, "signature", "Signature");
         if ((text2 == null || text2.isBlank()) && text3 != null && looksLikeTexturePayload(text3)) {
            text2 = text3;
         }

         if (text2 != null) {
            CompoundTag compoundTag2 = new CompoundTag();
            compoundTag2.putString("name", text);
            compoundTag2.putString("value", sanitizeTextureValue(text2));
            elements.add(compoundTag2);
         }
      }
   }

   private static UUID readUuid(Tag tag) {
      if (tag instanceof StringTag stringTag) {
         try {
            return UUID.fromString(stringTag.value());
         } catch (IllegalArgumentException failure) {
            return null;
         }
      } else if (tag instanceof IntArrayTag intArrayTag) {
         int[] asIntArray = intArrayTag.getAsIntArray();
         if (asIntArray.length != 4) {
            return null;
         }

         long timestamp = (long)asIntArray[0] << 32 | asIntArray[1] & 4294967295L;
         long timestamp2 = (long)asIntArray[2] << 32 | asIntArray[3] & 4294967295L;
         return new UUID(timestamp, timestamp2);
      } else {
         return null;
      }
   }

   public static void ensureTextureUniqueProfile(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         if (itemStack.is(Items.PLAYER_HEAD)) {
            ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
            if (resolvableProfile == null) {
               return;
            }

            GameProfile gameProfile = withTextureUniqueId(resolvableProfile.partialProfile());
            if (gameProfile != null && gameProfile != resolvableProfile.partialProfile()) {
               DataComponentType pROFILE = DataComponents.PROFILE;
               ResolvableProfile resolvableProfile2 = ResolvableProfile.createResolved(gameProfile);
               itemStack.set(pROFILE, resolvableProfile2);
               return;
            }

            return;
         }
      }
   }

   private FindTextureInTagHelper() {
   }

   private static boolean looksLikeTexturePayload(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = text.trim();
         if (!isRawTextureUrl(text2) && !isTextureHash(text2)) {
            if (text2.length() < 20) {
               return false;
            }

            if (!text2.startsWith("eyJ") && !text2.startsWith("ewog")) {
               byte[] byteValues = decodeBase64(text2);
               if (byteValues == null) {
                  return false;
               }

               String text3 = new String(byteValues, StandardCharsets.UTF_8);
               return text3.contains("textures") && (text3.contains("SKIN") || text3.contains("url"));
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public static GameProfile withTextureUniqueId(GameProfile gameProfile) {
      if (gameProfile == null) {
         return null;
      }

      PropertyMap lookup = gameProfile.properties();
      if (lookup != null && !lookup.isEmpty()) {
         String text = null;

         for (Property property : lookup.values()) {
            Property property2 = fixPropertyKeepSignature(property);
            if (property2 != null && looksLikeTexturePayload(property2.value())) {
               text = property2.value();
               break;
            }
         }

         if (text == null) {
            String text2 = TextureHashHelper.extractUrl(gameProfile);
            if (text2 == null) {
               return gameProfile;
            }

            UUID uUID = TextureHashHelper.profileIdForUrl(text2);
            if (uUID.equals(gameProfile.id())) {
               return gameProfile;
            }

            String text3 = gameProfile.name();
            if (text3 == null || text3.isBlank()) {
               text3 = "skycore";
            }

            return new GameProfile(uUID, text3, lookup);
         } else {
            UUID uUID2 = profileIdForTextureValue(text);
            if (uUID2.equals(gameProfile.id())) {
               return gameProfile;
            }

            String text4 = gameProfile.name();
            if (text4 == null || text4.isBlank()) {
               text4 = "skycore";
            }

            return new GameProfile(uUID2, text4, lookup);
         }
      } else {
         return gameProfile;
      }
   }

   private static CompoundTag getCompound(CompoundTag compoundTag, String... stringValues) {

      for (String text : stringValues) {
         if (compoundTag.get(text) instanceof CompoundTag compoundTag2) {
            return compoundTag2;
         }
      }

      return null;
   }

   private static IntArrayTag intArrayFromUuid(UUID uUID) {
      long mostSignificantBits = uUID.getMostSignificantBits();
      long leastSignificantBits = uUID.getLeastSignificantBits();
      int[] intValues = new int[]{(int)(mostSignificantBits >> 32), (int)mostSignificantBits, (int)(leastSignificantBits >> 32), (int)leastSignificantBits};
      return new IntArrayTag(intValues);
   }

   private static String firstString(CompoundTag compoundTag, String... stringValues) {
      for (String text : stringValues) {
         if (compoundTag.get(text) instanceof StringTag stringTag) {
            return stringTag.value();
         }
      }

      return null;
   }

   private static boolean normalizeId(CompoundTag compoundTag) {
      Tag tag = compoundTag.get("id");
      String text = "id";
      if (tag == null) {
         tag = compoundTag.get("Id");
         text = "Id";
      }

      if (tag instanceof StringTag stringTag) {
         UUID uUID;
         try {
            uUID = UUID.fromString(stringTag.value());
         } catch (IllegalArgumentException failure) {
            return false;
         }

         if (!"id".equals(text)) {
            compoundTag.remove(text);
         }

         compoundTag.put("id", intArrayFromUuid(uUID));
         return true;
      } else {
         return false;
      }
   }

   public static Property fixProperty(Property property) {
      return fixProperty(property, true);
   }

   private static boolean hasTextureProperty(PropertyMap lookup) {
      Iterator iterator = lookup.values().iterator();

      while (iterator.hasNext()) {
         Property element = (Property)iterator.next();
         Property property = fixPropertyKeepSignature(element);
         if (property != null) {
            if (looksLikeTexturePayload(property.value())) {
               return true;
            }
         }
      }

      return false;
   }

   public static UUID profileIdForTextureValue(String text) {
      String text2 = sanitizeTextureValue(text);
      String text3 = "PastaSkull:" + text2;
      byte[] bytes = text3.getBytes(StandardCharsets.UTF_8);
      return UUID.nameUUIDFromBytes(bytes);
   }

   private static FindTextureInTagHelper.TextureHit findBestTextureHit(ItemStack itemStack) {
      FindTextureInTagHelper.TextureHit textureInLegacyComponents = findTextureInLegacyComponents(itemStack);
      if (textureInLegacyComponents == null) {
         textureInLegacyComponents = findTextureFromProfileOnly(itemStack);
      }

      if (textureInLegacyComponents == null) {
         textureInLegacyComponents = findTextureInDisplay(itemStack);
      }

      if (textureInLegacyComponents == null) {
         textureInLegacyComponents = findTextureViaBlob(itemStack);
      }

      return textureInLegacyComponents;
   }

   private static String encodeTexturesJson(String text) {
      String text2 = "{\"textures\":{\"SKIN\":{\"url\":\"" + text.replace("\"", "") + "\"}}}";
      Encoder encoder = Base64.getEncoder();
      byte[] bytes = text2.getBytes(StandardCharsets.UTF_8);
      String text3 = encoder.encodeToString(bytes);
      return padBase64(text3);
   }

   private static FindTextureInTagHelper.TextureHit findTextureInCompound(CompoundTag compoundTag) {
      if (compoundTag == null) {
         return null;
      }

      String textureInTag = findTextureInTag(compoundTag);
      if (textureInTag == null) {
         return null;
      }

      UUID uUID = null;
      CompoundTag compound = getCompound(compoundTag, "SkullOwner", "Owner", "profile", "Profile");
      if (compound != null) {
         uUID = readUuid(compound.get("Id"));
         if (uUID == null) {
            uUID = readUuid(compound.get("id"));
         }
      }

      CompoundTag compound2 = getCompound(compoundTag, "BlockEntityTag", "block_entity_data");
      if (uUID == null && compound2 != null) {
         CompoundTag compound3 = getCompound(compound2, "profile", "Profile", "SkullOwner");
         if (compound3 != null) {
            uUID = readUuid(compound3.get("id"));
            if (uUID == null) {
               uUID = readUuid(compound3.get("Id"));
            }
         }
      }

      return new FindTextureInTagHelper.TextureHit(textureInTag, uUID);
   }

   private static void applyTextureProfile(ItemStack itemStack, String text, UUID uUID) {
      String text2 = sanitizeTextureValue(text);
      UUID uUID2 = profileIdForTextureValue(text);
      PropertyMap lookup = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", text2)));
      GameProfile gameProfile = new GameProfile(uUID2, "skycore", lookup);
      itemStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(gameProfile));
      String text3 = TextureHashHelper.extractUrl(gameProfile);
      if (text3 == null) {
         text3 = TextureHashHelper.extractUrlFromTextureValue(text);
      }

      if (text3 == null) {
         text3 = TextureHashHelper.extractUrlFromTextureValue(text2);
      }

      if (text3 != null) {
         TextureHashHelper.noteAppliedUrl(gameProfile, text3);
         TextureHashHelper.request(text3);
      }
   }

   private static FindTextureInTagHelper.TextureHit extractFromSkullOwner(CompoundTag compoundTag) {
      if (compoundTag == null) {
         return null;
      }

      UUID uUID = readUuid(compoundTag.get("Id"));
      if (uUID == null) {
         uUID = readUuid(compoundTag.get("id"));
      }

      CompoundTag compound = getCompound(compoundTag, "Properties", "properties");
      if (compound != null) {
         Tag tag = compound.get("textures");
         if (tag == null) {
            tag = compound.get("Textures");
         }

         if (tag instanceof ListTag) {
            for (Tag tag2 : (ListTag)tag) {
               if (tag2 instanceof CompoundTag compoundTag2) {
                  String text = firstString(compoundTag2, "Value", "value");
                  if (text != null && looksLikeTexturePayload(text)) {
                     return new FindTextureInTagHelper.TextureHit(text, uUID);
                  }

                  String text2 = firstString(compoundTag2, "Signature", "signature");
                  if ((text == null || text.isBlank()) && text2 != null && looksLikeTexturePayload(text2)) {
                     return new FindTextureInTagHelper.TextureHit(text2, uUID);
                  }
               } else if (tag2 instanceof StringTag stringTag && looksLikeTexturePayload(stringTag.value())) {
                  return new FindTextureInTagHelper.TextureHit(stringTag.value(), uUID);
               }
            }
         } else if (tag instanceof StringTag stringTag2 && looksLikeTexturePayload(stringTag2.value())) {
            return new FindTextureInTagHelper.TextureHit(stringTag2.value(), uUID);
         }
      }

      String text3 = firstString(compoundTag, "Value", "value");
      if (text3 != null && looksLikeTexturePayload(text3)) {
         return new FindTextureInTagHelper.TextureHit(text3, uUID);
      }

      String textureInTag = findTextureInTag(compoundTag);
      return textureInTag != null ? new FindTextureInTagHelper.TextureHit(textureInTag, uUID) : null;
   }

   public static byte[] decodeBase64(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = text.trim().replace("\r", "");
         String text3 = text2.replace("\n", "").replace(" ", "");
         String text4 = padBase64(text3);

         try {
            Decoder decoder = Base64.getDecoder();
            return decoder.decode(text4);
         } catch (IllegalArgumentException failure) {
            try {
               return Base64.getUrlDecoder().decode(text4);
            } catch (IllegalArgumentException failure2) {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private static List<String> orderedBlobParts(ItemStack itemStack) {
      ArrayList elements = new ArrayList();
      CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
      if (customData != null) {
         elements.add(customData.copyTag().toString());
      }

      TypedEntityData typedEntityData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
      if (typedEntityData != null) {
         elements.add(typedEntityData.copyTagWithoutId().toString());
      }

      ItemLore itemLore = itemStack.get(DataComponents.LORE);
      if (itemLore != null) {
         StringBuilder stringBuilder = new StringBuilder();

         for (Component text : itemLore.lines()) {
            stringBuilder.append(text.getString()).append('\n');
         }

         elements.add(stringBuilder.toString());
      }

      Component text2 = itemStack.get(DataComponents.CUSTOM_NAME);
      if (text2 != null) {
         elements.add(text2.getString());
      }

      try {
         StringBuilder stringBuilder2 = new StringBuilder(512);
         stringBuilder2.append(itemStack);

         for (TypedDataComponent typedDataComponent : itemStack.getComponents()) {
            stringBuilder2.append(typedDataComponent.type()).append('=').append(typedDataComponent.value()).append(';');
         }

         ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
         if (resolvableProfile != null) {
            stringBuilder2.append(resolvableProfile);
         }

         elements.add(stringBuilder2.toString());
      } catch (Throwable failure) {
      }

      return elements;
   }

   private static boolean isRawTextureUrl(String text) {
      if (text != null) {
         if (text.length() >= 12) {
            String text2 = text.toLowerCase();
            if (!text2.startsWith("http://")) {
               if (!text2.startsWith("https://")) {
                  return false;
               }
            }

            return true;
         }
      }

      return false;
   }

   private static boolean isAlreadyModernList(Tag tag) {
      return tag instanceof ListTag elements && !elements.isEmpty()
         ? elements.get(0) instanceof CompoundTag compoundTag
            && (compoundTag.contains("name") || compoundTag.contains("Name"))
            && (compoundTag.contains("value") || compoundTag.contains("Value") || compoundTag.contains("signature") || compoundTag.contains("Signature"))
         : false;
   }

   private static CompoundTag profileTagFromTexture(String text, UUID uUID) {
      CompoundTag compoundTag = new CompoundTag();
      UUID uUID2 = profileIdForTextureValue(text);
      IntArrayTag intArrayTag = intArrayFromUuid(uUID2);
      compoundTag.put("id", intArrayTag);
      ListTag elements = new ListTag();
      CompoundTag compoundTag2 = new CompoundTag();
      compoundTag2.putString("name", "textures");
      compoundTag2.putString("value", sanitizeTextureValue(text));
      elements.add(compoundTag2);
      compoundTag.put("properties", elements);
      return compoundTag;
   }

   private static boolean normalizeProperties(CompoundTag compoundTag) {
      String text = compoundTag.contains("properties") ? "properties" : (compoundTag.contains("Properties") ? "Properties" : null);
      if (text == null) {
         return false;
      } else {
         Tag tag = compoundTag.get(text);
         if (isAlreadyModernList(tag)) {
            return false;
         } else if (!(tag instanceof CompoundTag compoundTag2)) {
            return false;
         } else {
            ListTag elements = new ListTag();

            for (String text2 : compoundTag2.keySet()) {
               Tag tag2 = compoundTag2.get(text2);
               if (tag2 instanceof ListTag) {
                  for (Tag tag3 : (ListTag)tag2) {
                     addRewritten(elements, text2, tag3);
                  }
               } else if (tag2 instanceof CompoundTag compoundTag3) {
                  addRewritten(elements, text2, compoundTag3);
               } else if (tag2 instanceof StringTag stringTag) {
                  CompoundTag compoundTag4 = new CompoundTag();
                  compoundTag4.putString("name", text2);
                  compoundTag4.putString("value", sanitizeTextureValue(stringTag.value()));
                  elements.add(compoundTag4);
               }
            }

            if (elements.isEmpty()) {
               return false;
            }

            compoundTag.remove(text);
            compoundTag.put("properties", elements);
            return true;
         }
      }
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

   private static void normalizeProfileCompound(CompoundTag compoundTag) {
      normalizeId(compoundTag);
      normalizeProperties(compoundTag);
      sanitizeModernProperties(compoundTag);
   }

   private static FindTextureInTagHelper.TextureHit textureHitFromText(String text) {
      if (text != null && !text.isBlank()) {
         Matcher matcher = Pattern.compile("(eyJ[A-Za-z0-9_\\-/=+]{40,}|ewog[A-Za-z0-9_\\-/=+]{40,})").matcher(text);
         String text2 = null;

         while (matcher.find()) {
            String text3 = matcher.group().replace(" ", "");
            if (looksLikeTexturePayload(text3)) {
               text2 = text3;
            }
         }

         if (text2 != null) {
            return new FindTextureInTagHelper.TextureHit(text2, null);
         }

         String text4 = TextureHashHelper.extractUrlFromBlob(text);
         return text4 != null ? new FindTextureInTagHelper.TextureHit(text4, null) : null;
      } else {
         return null;
      }
   }

   public static Property fixPropertyKeepSignature(Property property) {
      return fixProperty(property, false);
   }

   public record TextureHit(String value, UUID uuid) {
      public UUID uuid() {
         return this.uuid;
      }
   }
}
