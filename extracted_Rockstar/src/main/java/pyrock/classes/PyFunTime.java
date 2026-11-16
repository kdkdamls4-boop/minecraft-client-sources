package pyrock.classes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jep.python.PyCallable;
import org.jetbrains.annotations.Nullable;
import rockstar.client.MinecraftClientAccess;
import rockstar.client.RockstarClient;
import rockstar.client.internal.script.ScriptInternal083;
import rockstar.client.internal.script.ScriptInternal085;

public class PyFunTime {
   public boolean ready() {
      return false;
   }

   public List<Map<String, Object>> events() {
      return List.of();
   }

   public List<Map<String, Object>> mines() {
      return List.of();
   }

   public List<Map<String, Object>> copperDungeons() {
      return List.of();
   }

   public List<Map<String, Object>> wardenCities() {
      return List.of();
   }

   @Nullable
   public Map<String, Object> systemInfo() {
      return null;
   }

   public void fetchEvents(@Nullable Map<String, String> localValue1, PyCallable localValue2) {
      deliver(CompletableFuture.completedFuture(null), localValue2, localValue0 -> null);
   }

   public void fetchMines(@Nullable Map<String, String> localValue1, PyCallable localValue2) {
      deliver(CompletableFuture.completedFuture(null), localValue2, localValue0 -> null);
   }

   public void fetchCopperDungeon(PyCallable localValue1) {
      deliver(CompletableFuture.completedFuture(null), localValue1, localValue0 -> null);
   }

   public void fetchWardenCity(PyCallable localValue1) {
      deliver(CompletableFuture.completedFuture(null), localValue1, localValue0 -> null);
   }

   public void fetchPlayers(@Nullable Map<String, String> localValue1, PyCallable localValue2) {
      deliver(CompletableFuture.completedFuture(null), localValue2, localValue0 -> null);
   }

   public void fetchBans(@Nullable Map<String, String> localValue1, PyCallable localValue2) {
      deliver(CompletableFuture.completedFuture(null), localValue2, localValue0 -> null);
   }

   public void solveCaptcha(String localValue1, PyCallable localValue2) {
      deliver(CompletableFuture.completedFuture(null), localValue2, localValue0 -> null);
   }

   private static <T> void deliver(CompletableFuture<T> localValue0, PyCallable localValue1, Function<T, Object> localValue2) {
      if (localValue1 != null) {
         ScriptInternal083 localValue3 = ScriptInternal083.internalMethod00581();
         localValue0.whenComplete((localValue3x, localValue4) -> {
            Object localValue5;
            try {
               localValue5 = localValue4 == null && localValue3x != null ? localValue2.apply(localValue3x) : null;
            } catch (Throwable localValue7) {
               localValue5 = null;
            }

            Object localValue6 = localValue5;
            MinecraftClientAccess.internalField0149.execute(() -> {
               if (localValue3 == null || localValue3.internalMethod08681()) {
                  if (ScriptInternal085.internalMethod08724()) {
                     try (AutoCloseable localValue3xx = ScriptInternal083.internalMethod02561(localValue3)) {
                        localValue1.call(new Object[]{localValue6});
                     } catch (Exception localValue8) {
                        RockstarClient.internalField0572.error("Python error in funtime callback:", localValue8);
                     }
                  }
               }
            });
         });
      }
   }
}
