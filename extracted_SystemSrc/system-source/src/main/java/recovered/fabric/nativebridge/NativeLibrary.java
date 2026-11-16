package recovered.fabric.nativebridge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

public final class NativeLibrary {
   private NativeLibrary() {
   }

   public static boolean enabled() {
      // Security cleanup: systemdlc.dll contained hardcoded external IP (83.169.216.188), Winsock/mbedTLS, and hardware queries.
      return false;
   }

   public static Path extract() throws IOException {
      throw new IOException("Native library removed during security cleanup");
   }
}
