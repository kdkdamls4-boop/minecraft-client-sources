package deluxe.client.scripting;

import deluxe.client.MinecraftAccess;
import deluxe.client.util.Stopwatch;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;

public final class LuaClientApi {
   private LuaClientApi() {
   }

   public static final class Nested0436 {
      public static final LuaClientApi.Nested0436 field08655 = new LuaClientApi.Nested0436(-1);
      public static final LuaClientApi.Nested0436 field08656 = new LuaClientApi.Nested0436(-4144960);
      public static final LuaClientApi.Nested0436 field08657 = new LuaClientApi.Nested0436(-8355712);
      public static final LuaClientApi.Nested0436 field08658 = new LuaClientApi.Nested0436(-12566464);
      public static final LuaClientApi.Nested0436 field08659 = new LuaClientApi.Nested0436(-16777216);
      public static final LuaClientApi.Nested0436 field08660 = new LuaClientApi.Nested0436(-65536);
      public static final LuaClientApi.Nested0436 field08661 = new LuaClientApi.Nested0436(-20561);
      public static final LuaClientApi.Nested0436 field08662 = new LuaClientApi.Nested0436(-14336);
      public static final LuaClientApi.Nested0436 field08663 = new LuaClientApi.Nested0436(-256);
      public static final LuaClientApi.Nested0436 field08664 = new LuaClientApi.Nested0436(-16711936);
      public static final LuaClientApi.Nested0436 field08665 = new LuaClientApi.Nested0436(-65281);
      public static final LuaClientApi.Nested0436 field08666 = new LuaClientApi.Nested0436(-16711681);
      public static final LuaClientApi.Nested0436 field08667 = new LuaClientApi.Nested0436(-16776961);
      private int field08668;
      private int field08669;
      private int field08670;
      private int field08671;

      public Nested0436(double local3951_1, double local3951_3, double local3951_5, double local3951_7) {
         this.field08668 = (int)(local3951_1 * 255.0);
         this.field08669 = (int)(local3951_3 * 255.0);
         this.field08670 = (int)(local3951_5 * 255.0);
         this.field08671 = (int)(local3951_7 * 255.0);
      }

      public float method2517() {
         return this.field08670;
      }

      public float method2518() {
         return this.field08669;
      }

      public int method2519() {
         return (this.field08671 & 0xFF) << 24 | (this.field08668 & 0xFF) << 16 | (this.field08669 & 0xFF) << 8 | this.field08670 & 0xFF;
      }

      public Nested0436(double local3956_1, double local3956_3, double local3956_5) {
         this(local3956_1, local3956_3, local3956_5, 1.0);
      }

      public float method2520() {
         return this.field08668;
      }

      public Nested0436(int local3958_1) {
         this.field08671 = local3958_1 >> 24 & 0xFF;
         this.field08668 = local3958_1 >> 16 & 0xFF;
         this.field08669 = local3958_1 >> 8 & 0xFF;
         this.field08670 = local3958_1 & 0xFF;
      }

      public float method2521() {
         return this.field08671;
      }
   }

   public static final class Nested0437 {
      private final Stopwatch stopwatch = new Stopwatch();

      public boolean method2522(long local3960_1) {
         return this.stopwatch.hasExceededMillis(local3960_1);
      }

      public void method2523() {
         this.stopwatch.reset();
      }

      public long method2524() {
         return this.stopwatch.elapsedMillis();
      }
   }

   public static final class Nested0438 {
      private final MatrixStack field08686;

      public void method2525(double local3964_1, double local3964_3, double local3964_5) {
         this.field08686.translate(local3964_1, local3964_3, local3964_5);
      }

      public void method2526(float local3965_1, float local3965_2, float local3965_3) {
         this.method2531(local3965_1);
         this.method2528(local3965_2);
         this.method2530(local3965_3);
      }

      public MatrixStack method2527() {
         return this.field08686;
      }

      public Nested0438(MatrixStack matrixStack) {
         this.field08686 = matrixStack;
      }

      public void method2528(float local3968_1) {
         this.field08686.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(local3968_1));
      }

      public void method2529(float local3969_1, float local3969_2, float local3969_3) {
         this.field08686.scale(local3969_1, local3969_2, local3969_3);
      }

      public void method2530(float local3970_1) {
         this.field08686.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(local3970_1));
      }

      public void method2531(float local3971_1) {
         this.field08686.multiply(RotationAxis.POSITIVE_X.rotationDegrees(local3971_1));
      }
   }

   public static final class Nested0439 {
      private final StatusEffectInstance field08687;
      private final String field08688;
      private final String field08689;

      public Nested0439(StatusEffectInstance statusEffectInstance, String local3972_2, String local3972_3) {
         this.field08687 = statusEffectInstance;
         this.field08689 = local3972_3;
         this.field08688 = local3972_2;
      }

      public StatusEffectInstance method2532() {
         return this.field08687;
      }

      public String method2533() {
         return this.field08689;
      }

      public String method2534() {
         return this.field08688;
      }
   }

   public static final class Nested0440 {
      public double field08690;
      public double field08691;
      public double field08692;

      public void method2535(double local3976_1, double local3976_3, double local3976_5) {
         this.field08690 = local3976_1;
         this.field08691 = local3976_3;
         this.field08692 = local3976_5;
      }

      public Nested0440(double local3977_1, double local3977_3, double local3977_5) {
         this.field08690 = local3977_1;
         this.field08691 = local3977_3;
         this.field08692 = local3977_5;
      }
   }

   public static final class Nested0441 extends LuaClientApi.Nested0442 {
      private final LivingEntity field08693;

      public LivingEntity method2548() {
         return this.field08693;
      }

      public boolean method2537() {
         return this.field08693.isBlocking();
      }

      public int method2538() {
         return this.field08693.hurtTime;
      }

      public float method2539() {
         return this.field08693.getHealth();
      }

      public float method2540() {
         return this.field08693.getMaxHealth();
      }

      public float method2541() {
         return this.field08693.getAbsorptionAmount();
      }

      public float method2542() {
         return this.field08693.bodyYaw;
      }

      public float method2543() {
         return this.field08693.getArmor();
      }

      public String method2544() {
         return this.field08693.getOffHandStack().getItem().getTranslationKey();
      }

      public Nested0441(LivingEntity entity) {
         super(entity);
         this.field08693 = entity;
      }

      public String method2545() {
         return this.field08693.getMainHandStack().getItem().getTranslationKey();
      }

      public int method2546() {
         return this.field08693.hurtTime;
      }

      public boolean method2547() {
         return this.field08693.isUsingItem();
      }
   }

   public static class Nested0442 implements MinecraftAccess {
      private final Entity field08694;

      public boolean method2549() {
         return this.field08694.isTouchingWater();
      }

      public boolean method2550() {
         return this.field08694.isGlowing();
      }

      public LuaClientApi.Nested0440 method2551() {
         return new LuaClientApi.Nested0440(this.field08694.getX(), this.field08694.getY(), this.field08694.getZ());
      }

      public float method2552() {
         return (float)this.field08694.fallDistance;
      }

      public Nested0442(Entity entity) {
         this.field08694 = entity;
      }

      public double method2553(LuaClientApi.Nested0442 local3997_1) {
         return local3997_1 == null ? -1.0 : this.field08694.distanceTo(local3997_1.method2548());
      }

      public boolean method2554() {
         return this.field08694.isSneaking();
      }

      public LuaClientApi.Nested0440 method2555() {
         return new LuaClientApi.Nested0440(this.field08694.getVelocity().x, this.field08694.getVelocity().y, this.field08694.getVelocity().z);
      }

      public boolean method2556() {
         return this.field08694 instanceof PlayerEntity;
      }

      public boolean method2557() {
         return this.field08694.isSprinting();
      }

      public Entity method2548() {
         return this.field08694;
      }

      public boolean method2558() {
         return this.field08694.isAlive();
      }

      public int method2559() {
         return this.field08694.getId();
      }

      public boolean method2560() {
         return this.field08694.isInLava();
      }

      public String method2561() {
         return this.field08694.getName().getString();
      }

      public float method2562() {
         return this.field08694.getPitch();
      }

      public float method2563() {
         return this.field08694.getYaw();
      }

      public boolean method2564() {
         return this.field08694.isOnGround();
      }

      public boolean method2565() {
         return this.field08694.isInvisible();
      }

      public String method2566() {
         return this.field08694.getType().getTranslationKey();
      }
   }

   public static final class Nested0443 {
      private float field08696;
      private float field08697;
      private long field08698;
      private long field08699 = 1L;

      public void method2567(float local4012_1, int local4012_2) {
         if (this.field08697 != local4012_1) {
            this.field08696 = this.method2570();
            this.field08697 = local4012_1;
            this.field08698 = System.currentTimeMillis();
            this.field08699 = Math.max(1, local4012_2);
         }
      }

      public boolean method2568() {
         return System.currentTimeMillis() - this.field08698 >= this.field08699;
      }

      public boolean method2569() {
         return this.method2568();
      }

      public float method2570() {
         float local4016_1 = Math.min(1.0F, (float)(System.currentTimeMillis() - this.field08698) / (float)this.field08699);
         return this.field08696 + (this.field08697 - this.field08696) * (1.0F - (1.0F - local4016_1) * (1.0F - local4016_1));
      }

      public float method2571() {
         return this.method2570();
      }
   }
}
