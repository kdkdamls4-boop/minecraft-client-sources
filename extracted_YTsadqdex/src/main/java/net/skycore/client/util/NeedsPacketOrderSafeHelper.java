package net.skycore.client.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class NeedsPacketOrderSafeHelper {
   public static final int PROTO_1_17_1 = 756;
   private static long nextRefreshAt;
   private static final long CACHE_MS = 500L;
   public static final int PROTO_1_21_4 = 769;
   public static final int PROTO_1_20_5 = 766;
   private static Integer cachedNativeId;
   private static boolean cachedViaPresent;
   private static volatile boolean nativeBypassActive;
   private static Integer cachedProtocolId;
   public static final int PROTO_1_19 = 759;
   public static final int PROTO_1_21_2 = 768;
   public static final int PROTO_1_17 = 755;

   public static boolean needsPacketOrderSafe() {
      Integer nativeProtocolId = getNativeProtocolId();
      if (nativeProtocolId != null) {
         if (nativeProtocolId < 768) {
            return false;
         }
      }

      return true;
   }

   private NeedsPacketOrderSafeHelper() {
   }

   public static void resetNativeBypass() {
      nativeBypassActive = false;
   }

   public static boolean setTargetProtocol(int targetProtocol) {
      try {
         Class element = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
         Object value = null;
         if (targetProtocol == 756) {
            try {
               value = element.getField("v1_17_1").get(null);
            } catch (Throwable failure) {
            }
         }

         if (value == null) {
            value = element.getMethod("getProtocol", int.class).invoke(null, targetProtocol);
         }

         if (value == null) {
            return false;
         }

         try {
            Class element2 = Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl");
            Object field = element2.getField("INSTANCE").get(null);
            if (field != null) {
               element2.getMethod("setTargetVersion", element).invoke(field, value);
               invalidateCache();
               return true;
            }
         } catch (Throwable failure2) {
         }

         Class element3 = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
         element3.getMethod("setTargetVersion", element).invoke(null, value);
         invalidateCache();
         return true;
      } catch (Throwable failure3) {
         return false;
      }
   }

   private static Object resolveNativeVersionObject() {

      try {
         Class element = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
         Field field = element.getField("NATIVE_VERSION");
         return field.get(null);
      } catch (Throwable failure) {
         try {
            Class element2 = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
            Integer value = resolveNativeId();
            if (value != null) {
               Class[] classValues = new Class[1];
               classValues[0] = int.class;
               Method method = element2.getMethod("getProtocol", classValues);
               Object[] objectValues = new Object[]{value};
               return method.invoke(null, objectValues);
            }
         } catch (Throwable failure2) {
         }

         return null;
      }
   }

   private static Integer tryViaFabricPlusImpl() {

      try {
         Class element = Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl");
         Field field = element.getField("INSTANCE");
         Object value = field.get(null);
         if (value == null) {
            return null;
         }

         Class[] classValues = new Class[0];
         Object method = element.getMethod("getTargetVersion", classValues).invoke(value);
         return readVersion(method);
      } catch (Throwable failure) {
         return null;
      }
   }

   private static Integer tryProtocolTranslator() {

      try {
         Class element = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
         Object method = element.getMethod("getTargetVersion").invoke(null);
         return readVersion(method);
      } catch (Throwable failure) {
         return null;
      }
   }

   private static void ensureNativeTargetVersion(boolean condition) {
      Object value = resolveNativeVersionObject();
      if (value != null) {
         try {
            Class element = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");

            try {
               Class element2 = Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl");
               Object field = element2.getField("INSTANCE").get(null);
               if (field != null) {
                  element2.getMethod("setTargetVersion", element).invoke(field, value);
               }
            } catch (Throwable failure) {
            }

            Class element3 = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
            element3.getMethod("setTargetVersion", element, boolean.class).invoke(null, value, condition);
            invalidateCache();
         } catch (Throwable failure2) {
         }
      }
   }

   public static boolean isNonNativeProtocol() {
      Integer targetProtocolId = getTargetProtocolId();
      Integer nativeProtocolId = getNativeProtocolId();
      if (targetProtocolId == null) {
         return false;
      } else {
         return nativeProtocolId == null ? targetProtocolId < 768 : !targetProtocolId.equals(nativeProtocolId);
      }
   }

   private static Integer resolveNativeId() {

      try {
         Class element = Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
         Field field = element.getField("NATIVE_VERSION");
         Object value = field.get(null);
         return readVersion(value);
      } catch (Throwable failure) {
         return null;
      }
   }

   public static boolean shouldBypassViaPipeline() {
      return nativeBypassActive;
   }

   public static boolean needsReallyWorld1214Fixes() {
      return needsEndTickSafe() && ServerAddressMatcher.isReallyWorld();
   }

   public static boolean isTarget1214() {
      Integer targetProtocolId = getTargetProtocolId();
      if (targetProtocolId != null) {
         if (targetProtocolId == 769) {
            return true;
         }
      }

      return false;
   }

   public static boolean isTarget1171() {
      Integer targetProtocolId = getTargetProtocolId();
      if (targetProtocolId != null) {
         if (targetProtocolId == 756) {
            return true;
         }
      }

      return false;
   }

   public static boolean isReallyWorldAddress(String... stringValues) {
      if (stringValues == null) {
         return false;
      }

      for (String text : stringValues) {
         if (text != null && !text.isEmpty()) {
            String text2 = text.toLowerCase();
            if (text2.contains("reallyworld") || text2.contains("playrw")) {
               return true;
            }
         }
      }

      return false;
   }

   private static void invalidateCache() {
      nextRefreshAt = 0L;
      cachedProtocolId = null;
   }

   public static boolean isTargetProtocolBelowOneNineteen() {
      Integer targetProtocolId = getTargetProtocolId();
      if (targetProtocolId != null) {
         if (targetProtocolId < 759) {
            return true;
         }
      }

      return false;
   }

   public static boolean isViaFabricPlusPresent() {
      refresh();
      return cachedViaPresent;
   }

   public static boolean needsInteractMoveSync() {
      if (!isViaTranslationActive()) {
         return false;
      }

      Integer targetProtocolId = getTargetProtocolId();
      return targetProtocolId != null && targetProtocolId >= 755 && targetProtocolId <= 766;
   }

   public static Integer getTargetProtocolId() {
      refresh();
      return cachedProtocolId;
   }

   public static boolean needsReallyWorld1171Takeoff() {
      return ServerAddressMatcher.isReallyWorld() && isTarget1171();
   }

   public static boolean needsSkycoreActionStop() {
      Integer targetProtocolId = getTargetProtocolId();
      if (isViaFabricPlusPresent() && targetProtocolId != null && targetProtocolId >= 769) {
         if (targetProtocolId < 1000000) {
            return true;
         }
      }

      return false;
   }

   private static Integer resolveProtocolId() {
      Integer value = tryViaFabricPlusImpl();
      return value != null ? value : tryProtocolTranslator();
   }

   public static boolean needsEndTickSafe() {
      if (!isViaTranslationActive()) {
         return false;
      }

      Integer targetProtocolId = getTargetProtocolId();
      return targetProtocolId != null && targetProtocolId >= 768;
   }

   public static boolean isViaTranslationActive() {
      return isViaFabricPlusPresent() && !nativeBypassActive && isNonNativeProtocol();
   }

   private static boolean detectViaPresent() {

      try {
         Class.forName("com.viaversion.viafabricplus.ViaFabricPlusImpl");
         return true;
      } catch (Throwable failure) {
         try {
            Class.forName("com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator");
            return true;
         } catch (Throwable failure2) {
            return false;
         }
      }
   }

   private static Integer readVersion(Object value) {
      if (value == null) {
         return null;
      }

      try {
         if (value.getClass().getMethod("getVersion").invoke(value) instanceof Number number) {
            return number.intValue();
         }
      } catch (Throwable failure) {
      }

      try {
         for (Method method : value.getClass().getMethods()) {
            if (Modifier.isPublic(method.getModifiers()) && method.getParameterCount() == 0) {
               Class returnType = method.getReturnType();
               if (returnType == int.class || returnType == Integer.class) {
                  String name = method.getName().toLowerCase();
                  if ((name.contains("version") || name.contains("protocol")) && method.invoke(value) instanceof Number number2) {
                     int result = number2.intValue();
                     if (result > 0 && result < 10000) {
                        return result;
                     }
                  }
               }
            }
         }
      } catch (Throwable failure2) {
      }

      return null;
   }

   public static boolean isTarget117x() {
      Integer targetProtocolId = getTargetProtocolId();
      if (targetProtocolId != null) {
         if (targetProtocolId >= 755 && targetProtocolId <= 756) {
            return true;
         }
      }

      return false;
   }

   private static void refresh() {
      long now = System.currentTimeMillis();
      if (now >= nextRefreshAt) {
         long timestamp = now + 500L;
         nextRefreshAt = timestamp;
         boolean detectViaPresent = detectViaPresent();
         cachedViaPresent = detectViaPresent;
         cachedNativeId = resolveNativeId();
         cachedProtocolId = resolveProtocolId();
      }
   }

   public static boolean isNativeTarget() {
      Integer targetProtocolId = getTargetProtocolId();
      Integer nativeProtocolId = getNativeProtocolId();
      if (targetProtocolId == null) {
         return false;
      }

      if (nativeProtocolId != null) {
         if (targetProtocolId.equals(nativeProtocolId)) {
            return true;
         }
      }

      return false;
   }

   public static void applyNativeBypassOnConnection(Object value) {
      if (shouldBypassViaPipeline() && value != null) {
         Object value2 = resolveNativeVersionObject();
         if (value2 != null) {
            try {
               Class element = Class.forName("com.viaversion.viaversion.api.protocol.version.ProtocolVersion");
               Class element2 = Class.forName("com.viaversion.viafabricplus.injection.access.core.IConnection");
               Class[] classValues = new Class[]{element};
               element2.getMethod("viaFabricPlus$setTargetVersion", classValues).invoke(value, value2);
            } catch (Throwable failure) {
            }
         }
      }
   }

   public static void prepareViaForConnect() {
      invalidateCache();
      if (!isViaFabricPlusPresent()) {
         nativeBypassActive = false;
      } else {
         if (isNativeTarget()) {
            nativeBypassActive = true;
            ensureNativeTargetVersion(true);
         } else {
            nativeBypassActive = false;
         }
      }
   }

   public static Integer getNativeProtocolId() {
      refresh();
      return cachedNativeId;
   }
}
