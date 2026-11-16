package deluxe.client.cosmetic;

import deluxe.client.cosmetic.CosmeticAnimationLoader;
import deluxe.client.cosmetic.CosmeticCatalog;
import deluxe.client.cosmetic.CosmeticCategory;
import deluxe.client.cosmetic.CosmeticEntry;
import deluxe.client.cosmetic.CosmeticModel;
import deluxe.client.cosmetic.CosmeticPreviewAtlas;
import deluxe.client.cosmetic.CosmeticStore;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import net.minecraft.util.Identifier;

public final class CosmeticWarmup {
   private static final Set<String> field09950 = ConcurrentHashMap.newKeySet();
   private static final Map<String, Long> field09951 = new ConcurrentHashMap<>();
   private static final ExecutorService field09952 = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "deluxe-cosmetic-warmup");
      thread.setDaemon(true);
      thread.setPriority(1);
      return thread;
   });

   public static void method2911() {
      for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
         method2912(CosmeticStore.method3519(cosmeticCategory));
      }
   }

   public static void method2912(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry != null) {
         method2916(
            cosmeticEntry.modelResource(),
            () -> cosmeticEntry.placement() != null ? CosmeticModel.method1873(cosmeticEntry) : CosmeticAnimationLoader.method3960(cosmeticEntry)
         );
      }
   }

   public static void method2913() {
      method2916("previews/atlas/index.json", CosmeticPreviewAtlas::method3627);
   }

   private CosmeticWarmup() {
   }

   public static void method2914(Identifier identifier, String local4627_1) {
      method2916(local4627_1, () -> CosmeticPreviewAtlas.method3626(identifier, local4627_1));
   }

   private static void method2916(String local4629_0, BooleanSupplier booleanSupplier) {
      Long local4629_2 = field09951.get(local4629_0);
      if (local4629_2 != null && System.currentTimeMillis() - local4629_2 >= 30000L && field09951.remove(local4629_0, local4629_2)) {
         field09950.remove(local4629_0);
      }

      if (field09950.add(local4629_0)) {
         field09952.execute(() -> {
            if (!booleanSupplier.getAsBoolean()) {
               field09951.put(local4629_0, System.currentTimeMillis());
            }
         });
      }
   }

   public static void method2917() {
      method2916("catalog.json", CosmeticCatalog::method0516);
   }
}
