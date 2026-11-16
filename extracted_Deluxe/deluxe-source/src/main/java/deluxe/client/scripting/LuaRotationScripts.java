package deluxe.client.scripting;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.MinecraftAccess;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import net.minecraft.entity.LivingEntity;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public final class LuaRotationScripts implements MinecraftAccess {
   private static final LinkedHashMap<String, LuaFunction> field01384 = new LinkedHashMap<>();
   private final ArrayList<String> field01385 = new ArrayList<>();

   public static boolean method0772(String local1037_0) {
      return field01384.get(local1037_0) != null;
   }

   public boolean method0773(LuaClientApi.Nested0441 local1039_1, float local1039_2) {
      return local1039_1 != null && mc.player != null
         ? local1039_1.method2548()
               .getEntityPos()
               .add(0.0, local1039_1.method2548().getStandingEyeHeight() - 0.3F, 0.0)
               .subtract(mc.player.getEyePos())
               .horizontalLengthSquared()
            < local1039_2
         : false;
   }

   public void method0774() {
      for (String local1040_2 : new ArrayList<>(this.field01385)) {
         this.method0777(local1040_2);
      }
   }

   public boolean method0775(LuaClientApi.Nested0441 local1041_1) {
      return local1041_1 != null && mc.player != null
         ? local1041_1.method2548()
               .getEntityPos()
               .add(0.0, local1041_1.method2548().getStandingEyeHeight() - 0.3F, 0.0)
               .subtract(mc.player.getEyePos())
               .horizontalLengthSquared()
            < 0.0925
         : false;
   }

   public static void method0776(String local1042_0, LivingEntity livingEntity, boolean local1042_2, float local1042_3) {
      if (field01384.get(local1042_0) != null && livingEntity != null) {
         field01384.get(local1042_0)
            .call(CoerceJavaToLua.coerce(new LuaClientApi.Nested0441(livingEntity)), LuaValue.valueOf(local1042_2), LuaValue.valueOf(local1042_3));
      }
   }

   public void method0777(String local1043_1) {
      field01384.remove(local1043_1);
      this.field01385.remove(local1043_1);
      if (AttackAuraModule.method1029().field02093.isSelected(local1043_1)) {
         AttackAuraModule.method1029().field02093.withSelectedOption("FunTime");
      }

      AttackAuraModule.method1029().field02093.getOptions().remove(local1043_1);
   }

   public void method0778(String local1044_1, LuaFunction luaFunction) {
      field01384.put(local1044_1, luaFunction);
      if (!this.field01385.contains(local1044_1)) {
         this.field01385.add(local1044_1);
      }

      if (!AttackAuraModule.method1029().field02093.getOptions().contains(local1044_1)) {
         AttackAuraModule.method1029().field02093.getOptions().add(local1044_1);
      }
   }
}
