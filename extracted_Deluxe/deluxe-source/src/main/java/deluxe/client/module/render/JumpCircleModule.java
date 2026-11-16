package deluxe.client.module.render;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.setting.ColorSetting;
import deluxe.client.setting.SectionSetting;

import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "JumpCircle", description = "Светящийся круг на земле в месте прыжка.", category = ModuleCategory.RENDER)
public final class JumpCircleModule extends Module {
   private static JumpCircleModule jumpCircleModule;
   public LegacyNumberSetting field01489;
   public LegacyNumberSetting field01490;
   public LegacyNumberSetting field01491;
   public ModeSetting modeSetting;
   public BooleanSetting field01493;
   public LegacyNumberSetting field01494;
   public BooleanSetting field01495;
   public ColorSetting colorSetting;
   private final CopyOnWriteArrayList<JumpCircleModule.Nested0362> field01497 = new CopyOnWriteArrayList<>();
   private boolean field01498;
   private final EventListener<PlayerTickEvent> field01499 = playerTickEvent -> {
      if (mc.player != null) {
         if (this.field01498 && !mc.player.isOnGround() && mc.player.getVelocity().y > 0.3) {
            if (this.field01497.size() >= 16) {
               this.field01497.remove(0);
            }

            float[] floatArray = new float[32];
            Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.lastRenderY + 0.6, mc.player.getZ());
            double local1110_4 = this.field01489.getValue() + this.field01490.getValue() * 2.0F + 1.0F;

            for (int index = 0; index < 32; index++) {
               HitResult hitResult = mc.world
                  .raycast(
                     new RaycastContext(
                        vec3d,
                        new Vec3d(
                           vec3d.x + Math.cos(index * Math.PI / 16.0) * local1110_4,
                           vec3d.y,
                           vec3d.z + Math.sin(index * Math.PI / 16.0) * local1110_4
                        ),
                        ShapeType.COLLIDER,
                        FluidHandling.NONE,
                        mc.player
                     )
                  );
               floatArray[index] = hitResult.getType() == Type.MISS ? 999.0F : (float)hitResult.getPos().distanceTo(vec3d);
            }

            this.field01497
               .add(
                  new JumpCircleModule.Nested0362(
                     mc.player.getX(), mc.player.lastRenderY, mc.player.getZ(), System.currentTimeMillis(), floatArray
                  )
               );
         }

         this.field01498 = mc.player.isOnGround();
         this.field01497.removeIf(local1115_1 -> (float)(System.currentTimeMillis() - local1115_1.bornMs()) > this.field01494.getValue());
      }
   };

   private JumpCircleModule() {
      this.addSetting(new SectionSetting("Кольцо"));
      this.field01489 = this.addSetting(new LegacyNumberSetting("Радиус", "Радиус круга в блоках.", 1.5, 0.5, 4.0, 0.1));
      this.field01490 = this.addSetting(new LegacyNumberSetting("Свечение", "Мягкость дистанс-поля кольца.", 0.35, 0.05, 1.5, 0.05));
      this.field01493 = this.addSetting(new BooleanSetting("Волна радиуса", "Радиус расходится волной от точки прыжка, а не статичен.", true));
      this.field01494 = this.addSetting(new LegacyNumberSetting("Время жизни", "Сколько миллисекунд живёт круг.", 600.0, 300.0, 3000.0, 50.0));
      this.addSetting(new SectionSetting("Искажение"));
      this.field01491 = this.addSetting(new LegacyNumberSetting("Искажение", "Сила преломления картинки.", 0.35, 0.0, 1.0, 0.05));
      this.modeSetting = this.addSetting(
         new ModeSetting("Эффект", "Как искажать картинку: волна вокруг кольца или вода с тенью.", "Волна", "Волна", "Вода")
      );
      this.addSetting(new SectionSetting("Цвет"));
      this.field01495 = this.addSetting(new BooleanSetting("Свой цвет", "Использовать свой цвет вместо ACCENT.", false));
      this.colorSetting = this.addSetting(new ColorSetting("Цвет", "Цвет круга."));
      this.colorSetting.withVisibility(() -> this.field01495.isChecked());
   }

   public CopyOnWriteArrayList<JumpCircleModule.Nested0362> method0831() {
      return this.field01497;
   }

   @Override
   protected void onDisable() {
      this.field01497.clear();
   }

   public static JumpCircleModule method0833() {
      if (jumpCircleModule == null) {
         jumpCircleModule = new JumpCircleModule();
      }

      return jumpCircleModule;
   }

   public float method0835(JumpCircleModule.Nested0362 local1116_1) {
      return Math.min(1.0F, (float)(System.currentTimeMillis() - local1116_1.bornMs()) / this.field01494.getValue());
   }

   public record Nested0362(double x, double y, double z, long bornMs, float[] wallDistBlocks) {
   }
}
