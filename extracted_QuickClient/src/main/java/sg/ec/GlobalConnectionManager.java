// Original binary: sg/ec/IAGMUtg9AkptMrSV
package sg.ec;

import java.net.URI;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;

// $VF: Compiled from s
public class GlobalConnectionManager implements MinecraftAccess {
   private static final int field08652 = 3000;
   public static String field08653 = GlobalConnectionManager.field08676;
   private static final ScheduledExecutorService field08654 = Executors.newSingleThreadScheduledExecutor(var0 -> {
      Thread var1 = new Thread(var0, GlobalConnectionManager.field08675);
      var1.setDaemon(true);
      return var1;
   });
   private static final ScheduledExecutorService field08655 = Executors.newSingleThreadScheduledExecutor(var0 -> {
      Thread var1 = new Thread(var0, GlobalConnectionManager.field08674);
      var1.setDaemon(true);
      return var1;
   });
   private static final long field08656 = 0;
   private static volatile boolean field08657;
   private static volatile boolean field08658;
   private static volatile boolean field08659;
   private static volatile boolean field08660;
   public static volatile List<AnarchyServerInfo> field08661 = List.of();
   public static volatile List<FunTimeServerData> field08662 = List.of();
   public static volatile long field08663 = 0L;
   private static final String field08664 = "DEV";
   // Security fix: Neutralized remote C2 WebSocket URL
   private static final String field08665 = "ws://127.0.0.1:0";
   private static final double field08666 = 10000.0;
   private static final long field08667 = 5L;
   private static final long field08668 = 5L;
   private static final String field08669 = "Client Disconnected";
   private static final long field08670 = 80L;
   private static final long field08671 = 150L;
   private static final long field08672 = 50L;
   private static final long field08673 = 3000L;
   private static final String field08674 = "Globals-AutoSave";
   private static final String field08675 = "Globals-Reconnect";
   private static final String field08676 = "QUICK";

   public static boolean method03845() {
      return field08660;
   }

   public static void method03846(List<AnarchyServerInfo> var0, List<FunTimeServerData> var1) {
      field08661 = var0 != null ? List.copyOf(var0) : List.of();
      field08662 = var1 != null ? List.copyOf(var1) : List.of();
      field08663 = System.currentTimeMillis();
   }

   private static void method03848() {
      // Security fix: Disabled background timer for cloud auto-save
   }

   public static void method03849() {
      // Security fix: Disabled automatic cloud storage upload
   }

   public static void method03850() {
      field08660 = true;
   }

   public static void method03852(GlobalPacket<GlobalPacketHandler, ?> var0) {
      GlobalWebSocketClient var1 = QuickClient.getInstance().getClient();
      if (var1 != null) {
         var1.getManager().method01823(var0);
      }
   }

   public static void method03853() {
      field08658 = true;
      GlobalWebSocketClient var0 = QuickClient.getInstance().getClient();
      if (var0 != null) {
         var0.closeConnection(777, field08669);
      }

      QuickClient.getInstance().setClient(null);
   }

   public static void method03855() {
      if (!field08658) {
         QuickClient.getInstance().setClient(null);
         method03859();
      }
   }

   public static void method03856() {
      if (!field08658) {
         GlobalWebSocketClient var0 = QuickClient.getInstance().getClient();
         if (var0 == null || !var0.isOpen()) {
            method03859();
         }
      }
   }

   public static void method03857() {
      field08658 = true;
      GlobalWebSocketClient var0 = QuickClient.getInstance().getClient();
      if (var0 != null) {
         try {
            if (var0.isOpen() && ActiveConfigTracker.field06857.method03063()) {
               var0.getManager().method01808();
            }

            boolean var1 = var0.isOpen() && field08660 && !DeferredCloudDataLoader.INSTANCE.method02275();
            List var2 = var1 ? StorageManager.method11277() : List.of();
            if (!var2.isEmpty()) {
               var0.getManager().method01803(var2);
            }

            try {
               Thread.sleep(var2.isEmpty() ? field08670 : field08671);
            } catch (InterruptedException var6) {
               Thread.currentThread().interrupt();
            }

            var0.close();
            long var3 = System.currentTimeMillis() + (var2.isEmpty() ? 1000 : 3000);

            while (var0.isOpen() && System.currentTimeMillis() < var3) {
               try {
                  Thread.sleep(field08672);
               } catch (InterruptedException var7) {
                  Thread.currentThread().interrupt();
                  break;
               }
            }
         } catch (Exception var8) {
         }
      }

      QuickClient.getInstance().setClient(null);
   }

   public static GlobalPlayer method03858() {
      GlobalWebSocketClient var0 = QuickClient.getInstance().getClient();
      return var0 == null ? null : var0.getClientPlayer();
   }

   private static void method03859() {
      // Security fix: Disabled automatic reconnect to remote C2
   }

   public static void method03860() {
      // Security fix: Disabled remote C2 connection
   }
}
