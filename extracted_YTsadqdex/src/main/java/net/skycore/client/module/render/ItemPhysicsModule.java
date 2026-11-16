package net.skycore.client.module.render;

import java.lang.reflect.Field;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;

public final class ItemPhysicsModule extends ClientModule {
   public static final ItemPhysicsModule INSTANCE = new ItemPhysicsModule();

   @Override
   protected void onEnable() {
      this.syncModState();
   }

   private static Object publicField(Object value, String text) throws ReflectiveOperationException {
      Class element = value.getClass();
      Field field = element.getField(text);
      return field.get(value);
   }

   @Override
   protected void onDisable() {
      this.syncModState();
   }

   @Override
   protected void onTick() {
      this.syncModState();
   }

   private void syncModState() {
      try {
         Class element = Class.forName("team.creative.itemphysic.ItemPhysic");
         Object field = element.getField("CONFIG").get(null);
         if (field == null) {
            return;
         }

         Object value = publicField(field, "rendering");
         if (value != null) {
            setBoolean(value, "vanillaRendering", !this.isEnabled());
            setBoolean(value, "disableThrowHUD", true);
         }

         Object value2 = publicField(field, "throwConfig");
         if (value2 != null) {
            setBoolean(value2, "enabled", false);
         }

         Class element2 = Class.forName("team.creative.itemphysic.client.ItemPhysicClient");
         element2.getField("throwCharge").setInt(null, 0);
      } catch (Throwable failure) {
      }
   }

   private static void setBoolean(Object value, String text, boolean condition) throws ReflectiveOperationException {
      Field field = value.getClass().getField(text);
      field.setBoolean(value, condition);
   }

   private ItemPhysicsModule() {
      super("ItemPhysics", "Makes dropped items lie naturally on the ground", ModuleCategory.RENDER);
      this.syncModState();
   }
}
