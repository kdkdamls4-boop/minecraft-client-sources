package recovered.fabric.identity;

public final class DeviceIdentity {
   private static final String value = "offline-clean-hwid";

   private DeviceIdentity() {
   }

   public static String get() {
      return value;
   }
}
