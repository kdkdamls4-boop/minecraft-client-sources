package deluxe.client.module.combat;

import deluxe.client.event.BlockBreakEvent;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.event.HudRenderEvent;
import deluxe.mixin.MinecraftClientMixin6;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.setting.MultiSelectSetting;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.module.render.ProjectilePredictionModule;
import deluxe.client.util.AimPointUtils;
import deluxe.client.RotationAngles;
import deluxe.client.RotationManager;
import deluxe.client.RotationSpeed;
import deluxe.client.module.player.ServerHelperModule;

import java.util.HashMap;
import java.util.LinkedList;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.TridentItem;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

@ModuleInfo(name = "ProjectileHelper", description = "Доводит прицел по цели при стрельбе из лука и трезубца.", category = ModuleCategory.COMBAT)
public final class ProjectileHelperModule extends Module {
   private static ProjectileHelperModule projectileHelperModule;
   public BooleanSetting field01190;
   public MultiSelectSetting multiSelectSetting;
   public ModeSetting modeSetting;
   public BooleanSetting field01193;
   public BooleanSetting field01194;
   private final HashMap<LivingEntity, LinkedList<Vec3d>> field01195 = new HashMap<>();
   private LivingEntity field01196;
   private int field01197;
   private boolean field01198 = true;
   private boolean field01199;
   private float field01200;
   private float field01201;
   private boolean field01202;
   private int field01203;
   private float field01204;
   private float field01205;
   private RotationAngles rotationAngles;
   private boolean field01207;
   private boolean field01208;
   private int field01209;
   private boolean field01210;
   private final EventListener<PlayerTickEvent> field01211 = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         boolean isPressed = mc.options.attackKey.isPressed();
         if (this.method0735() && isPressed && !this.field01199) {
            this.field01198 = !this.field01198;
         }

         this.field01199 = isPressed;
         if (!this.method0735()) {
            this.field01198 = true;
            this.field01196 = null;
            this.field01197 = 0;
            this.field01209 = 0;
            if (this.field01202) {
               if (!this.field01207) {
                  mc.player.setYaw(mc.player.headYaw = this.field01204);
                  mc.player.setPitch(this.field01205);
               }

               this.field01202 = false;
            }
         } else if (!this.field01198) {
            this.field01196 = null;
            this.field01197 = 0;
            this.field01209 = 0;
         } else if (mc.player.getMainHandStack().getItem() instanceof CrossbowItem) {
            this.method0726();
            if (this.field01196 == null) {
               this.field01209 = 0;
            } else {
               this.method0730(this.field01196);
               if (this.field01209 != 0) {
                  this.field01209++;
                  RotationManager.method0658()
                     .method0648(AimPointUtils.method4450(this.method0723()), new RotationSpeed(360.0F, 360.0F, 30.0F, 30.0F), 2L);
                  mc.player.setYaw(mc.player.headYaw = AimPointUtils.method4450(this.method0723()).method4362());
                  mc.player.setPitch(MathHelper.clamp(AimPointUtils.method4450(this.method0723()).method4365(), -90.0F, 90.0F));
                  if (mc.interactionManager != null) {
                     this.field01210 = true;
                     ((MinecraftClientMixin6)mc).invokeStartUseItem();
                     this.field01210 = false;
                     if (this.method0731()) {
                        RotationManager.method0658().method0649();
                     }

                     this.field01209 = 0;
                  }
               }
            }
         } else if (!this.modeSetting.isSelected("Снап")) {
            this.method0726();
            if (this.field01196 != null) {
               this.method0738();
            }
         } else if (!this.field01202) {
            this.method0726();
            if (this.field01196 != null) {
               this.method0730(this.field01196);
            }
         } else if (!this.field01207) {
            if (++this.field01203 >= 2 && mc.interactionManager != null) {
               this.field01208 = true;
               mc.interactionManager.stopUsingItem(mc.player);
               this.field01208 = false;
               mc.player.setYaw(mc.player.headYaw = this.field01204);
               mc.player.setPitch(this.field01205);
               this.field01202 = false;
               this.field01196 = null;
            }
         } else {
            RotationManager.method0658().method0648(this.rotationAngles, new RotationSpeed(360.0F, 360.0F, 30.0F, 30.0F), 2L);
            this.field01203++;
            boolean local966_3 = Math.abs(MathHelper.wrapDegrees(this.rotationAngles.method4362() - mc.player.getYaw())) <= 1.0F
               && Math.abs(this.rotationAngles.method4365() - mc.player.getPitch()) <= 1.0F;
            if ((local966_3 && this.field01203 >= 2 || this.field01203 >= 10) && mc.interactionManager != null) {
               this.field01208 = true;
               mc.interactionManager.stopUsingItem(mc.player);
               this.field01208 = false;
               if (this.method0731()) {
                  RotationManager.method0658().method0649();
               }

               this.field01202 = false;
               this.field01196 = null;
            }
         }
      }
   };
   private final EventListener<BlockBreakEvent> field01212 = blockBreakEvent -> {
      if (this.isEnabled()
         && !this.field01208
         && mc.player != null
         && mc.world != null
         && this.field01190.isChecked()
         && this.method0731()
         && this.method0735()
         && this.field01198) {
         RotationManager.method0658().method0649();
      }

      if (this.isEnabled() && !this.field01208 && this.modeSetting.isSelected("Снап") && mc.player != null && mc.world != null) {
         if (!(mc.player.getMainHandStack().getItem() instanceof CrossbowItem)) {
            if (this.method0735() && this.field01198) {
               if (!this.field01202) {
                  this.method0726();
                  if (this.field01196 == null) {
                     return;
                  }

                  this.rotationAngles = AimPointUtils.method4450(this.method0723());
                  this.field01207 = this.field01193.isChecked();
                  if (this.field01207) {
                     RotationManager.method0658().method0648(this.rotationAngles, new RotationSpeed(360.0F, 360.0F, 30.0F, 30.0F), 2L);
                  } else {
                     this.field01204 = mc.player.getYaw();
                     this.field01205 = mc.player.getPitch();
                     mc.player.setYaw(mc.player.headYaw = this.rotationAngles.method4362());
                     mc.player.setPitch(this.rotationAngles.method4365());
                  }

                  this.field01202 = true;
                  this.field01203 = 0;
               }

               blockBreakEvent.method4424(true);
            }
         }
      }
   };
   private final EventListener<HudRenderEvent> field01213 = hudRenderEvent -> {
      if (this.field01194.isChecked()) {
         this.field01201 = this.field01201 + ((this.method0735() && this.field01198 && this.field01196 != null ? 1.0F : 0.0F) - this.field01201) * 0.2F;
         if (!(this.field01201 < 0.01F)) {
            ;
         }
      }
   };

   private Vec3d method0723() {
      ItemStack itemStack = mc.player.getMainHandStack();
      double local965_2 = this.method0728(itemStack);
      double local965_4 = this.method0741(itemStack);
      if (!(local965_2 <= 0.0) && !(local965_4 <= 0.0)) {
         int local965_6 = mc.player.isUsingItem() ? 1 : 0;
         Vec3d velocity = mc.player.getVelocity();
         Vec3d vec3d = mc.player.getCameraPosVec(1.0F).add(velocity.multiply(local965_6));
         Vec3d vec3d2 = itemStack.getItem() instanceof CrossbowItem ? Vec3d.ZERO : this.method0730(this.field01196);
         Box box = this.field01196.getBoundingBox().offset(vec3d2.multiply(local965_6));
         Vec3d vec3d3 = this.method0729(vec3d, box, this.field01196);
         ProjectileHelperModule.Nested0361 local965_12 = null;

         for (int index = 0; index < 4; index++) {
            Vec3d vec3d4 = vec3d3.subtract(vec3d);
            if (local965_12 != null) {
               vec3d4 = vec3d4.subtract(velocity.multiply(local965_12.flightTicks()));
            }

            local965_12 = this.method0725(vec3d4, local965_2, local965_4);
            if (local965_12 == null) {
               break;
            }

            Vec3d vec3d5 = this.method0729(vec3d, box.offset(vec3d2.multiply(local965_12.flightTicks())), this.field01196);
            if (vec3d5.squaredDistanceTo(vec3d3) <= 1.0E-4) {
               vec3d3 = vec3d5;
               break;
            }

            vec3d3 = vec3d5;
         }

         if (local965_12 != null) {
            ProjectileHelperModule.Nested0361 local965_13 = this.method0725(vec3d3.subtract(vec3d), local965_2, local965_4);
            if (local965_13 != null) {
               local965_12 = local965_13;
            }
         }

         if (local965_12 == null) {
            Vec3d vec3d6 = vec3d3.subtract(vec3d);
            return vec3d6.lengthSquared() > 1.0E-6 ? vec3d6.normalize() : new Vec3d(0.0, -1.0, 0.0);
         } else {
            return local965_12.launchDir();
         }
      } else {
         return this.field01196.getBoundingBox().getCenter().subtract(mc.player.getCameraPosVec(1.0F)).normalize();
      }
   }

   private ProjectileHelperModule.Nested0361 method0725(Vec3d vec3d, double local967_2, double local967_4) {
      double local967_6 = Math.hypot(vec3d.x, vec3d.z);
      double local967_8 = vec3d.y;
      double local967_10 = local967_2 * local967_2;
      double local967_12 = local967_10 * local967_10 - local967_4 * (local967_4 * local967_6 * local967_6 + 2.0 * local967_8 * local967_10);
      if (local967_12 < 0.0) {
         return null;
      }

      double local967_14 = Math.sqrt(local967_12);
      double local967_16 = (local967_10 - local967_14) / (local967_4 * local967_6);
      double local967_18 = (local967_10 + local967_14) / (local967_4 * local967_6);
      double local967_20 = !Double.isNaN(local967_16) && !Double.isInfinite(local967_16) ? local967_16 : local967_18;
      if (!Double.isNaN(local967_20) && !Double.isInfinite(local967_20)) {
         double local967_22 = Math.atan(local967_20);
         if (local967_22 < -Math.PI / 2 || local967_22 > Math.PI / 2) {
            local967_22 = Math.atan(local967_18);
         }

         double local967_24 = local967_6 / (local967_2 * Math.cos(local967_22));
         return !Double.isNaN(local967_24) && !Double.isInfinite(local967_24) && !(local967_24 <= 0.0)
            ? new ProjectileHelperModule.Nested0361(new Vec3d(vec3d.x, local967_6 * Math.tan(local967_22), vec3d.z).normalize(), local967_24)
            : null;
      } else {
         return null;
      }
   }

   private void method0726() {
      LivingEntity livingEntity = null;
      double local968_2 = Double.MAX_VALUE;

      for (AbstractClientPlayerEntity livingEntity2 : mc.world.getPlayers()) {
         if (livingEntity2 != mc.player && !FriendStore.method3431(livingEntity2.getNameForScoreboard())) {
            double local968_6 = this.method0737(livingEntity2) * mc.player.distanceTo(livingEntity2);
            if (local968_6 < local968_2) {
               local968_2 = local968_6;
               livingEntity = livingEntity2;
            }
         }
      }

      if (this.field01196 == null) {
         this.field01196 = livingEntity;
         this.field01197 = 0;
      } else if (livingEntity != null && livingEntity != this.field01196 && ++this.field01197 >= 3) {
         this.field01196 = livingEntity;
         this.field01197 = 0;
      } else if (livingEntity == this.field01196) {
         this.field01197 = 0;
      }
   }

   private double method0728(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0;
      } else if (itemStack.getItem() == Items.BOW && mc.player.isUsingItem()) {
         return 3.0F * BowItem.getPullProgress(mc.player.getItemUseTime());
      } else if (itemStack.getItem() instanceof CrossbowItem) {
         return CrossbowItem.isCharged(itemStack) ? 3.15 : 0.0;
      } else {
         return itemStack.getItem() instanceof TridentItem && mc.player.isUsingItem() ? 2.5 : 0.0;
      }
   }

   private Vec3d method0729(Vec3d vec3d, Box box, LivingEntity livingEntity) {
      Vec3d center = box.getCenter();
      Vec3d vec3d2 = livingEntity.getRotationVec(mc.getRenderTickCounter().getTickProgress(true));
      double local971_6 = Math.min(box.getLengthX(), box.getLengthZ()) * 0.5;
      Vec3d vec3d3 = new Vec3d(
         MathHelper.clamp(center.x + vec3d2.x * local971_6, box.minX, box.maxX),
         MathHelper.clamp(center.y + vec3d2.y * local971_6, box.minY, box.maxY),
         MathHelper.clamp(center.z + vec3d2.z * local971_6, box.minZ, box.maxZ)
      );
      if (!this.method0745(vec3d, vec3d3)) {
         return vec3d3;
      }

      if (!this.method0745(vec3d, center)) {
         return center;
      }

      double local971_9 = local971_6 * 0.6;
      Vec3d[] vec3dArray = new Vec3d[]{vec3d2.multiply(local971_9), vec3d2.multiply(local971_9 * 0.5), vec3d2.multiply(-local971_9 * 0.5)};

      for (Vec3d vec3d4 : vec3dArray) {
         Vec3d vec3d5 = center.add(vec3d4.x, vec3d4.y, vec3d4.z);
         if (box.contains(vec3d5) && !this.method0745(vec3d, vec3d5)) {
            return vec3d5;
         }
      }

      Vec3d[] vec3dArray2 = new Vec3d[]{
         new Vec3d(0.0, 0.25, 0.0),
         new Vec3d(0.0, -0.25, 0.0),
         new Vec3d(0.25, 0.0, 0.0),
         new Vec3d(-0.25, 0.0, 0.0),
         new Vec3d(0.0, 0.0, 0.25),
         new Vec3d(0.0, 0.0, -0.25),
         new Vec3d(0.18, 0.2, 0.18),
         new Vec3d(-0.18, 0.2, 0.18),
         new Vec3d(0.18, 0.2, -0.18),
         new Vec3d(-0.18, 0.2, -0.18),
         new Vec3d(0.18, -0.1, 0.18),
         new Vec3d(-0.18, -0.1, 0.18),
         new Vec3d(0.18, -0.1, -0.18),
         new Vec3d(-0.18, -0.1, -0.18)
      };

      for (Vec3d vec3d6 : vec3dArray2) {
         Vec3d vec3d7 = center.add(
            box.getLengthX() * vec3d6.x, box.getLengthY() * vec3d6.y, box.getLengthZ() * vec3d6.z
         );
         if (box.contains(vec3d7) && !this.method0745(vec3d, vec3d7)) {
            return vec3d7;
         }
      }

      return vec3d3;
   }

   private Vec3d method0730(LivingEntity livingEntity) {
      this.field01195.keySet().removeIf(livingEntity2 -> livingEntity2.isRemoved() || livingEntity2 != livingEntity && livingEntity2.squaredDistanceTo(livingEntity) > 4096.0);
      LinkedList<Vec3d> local972_2 = this.field01195.computeIfAbsent(livingEntity, livingEntity3 -> new LinkedList<>());
      if (local972_2.size() >= 5) {
         local972_2.pollFirst();
      }

      local972_2.add(livingEntity.getEntityPos());
      if (local972_2.size() < 2) {
         return Vec3d.ZERO;
      }

      Vec3d vec3d = Vec3d.ZERO;

      for (int index = 1; index < local972_2.size(); index++) {
         Vec3d vec3d2 = local972_2.get(index).subtract(local972_2.get(index - 1));
         vec3d = vec3d.add(vec3d2.x, 0.0, vec3d2.z);
      }

      return vec3d.multiply(1.0 / (local972_2.size() - 1));
   }

   public boolean method0731() {
      return ServerHelperModule.method0707().isEnabled() && ServerHelperModule.method0707().modeSetting.isSelected("SpookyTime");
   }

   private ProjectileHelperModule() {
      this.field01190 = this.addSetting(new BooleanSetting("Доводка прицела", "Выключи, чтобы модуль только рисовал предикт и не трогал прицел.", true));
      this.multiSelectSetting = this.addSetting(
         new MultiSelectSetting(
            "Помогать с", "Из какого оружия доводить прицел.", new String[]{"Трезубцем", "Луком", "Арбалетом"}, "Трезубцем", "Луком", "Арбалетом"
         )
      );
      this.multiSelectSetting.withVisibility(() -> this.field01190.isChecked());
      this.modeSetting = this.addSetting(
         new ModeSetting("Стиль наводки", "Снап доворачивает мгновенно в точку, плавная ведёт с разбросом.", "Плавная", "Плавная", "Снап")
      );
      this.modeSetting.withVisibility(() -> this.field01190.isChecked());
      this.field01193 = this.addSetting(
         new BooleanSetting("Клиентская ротация", "Снап ведёт камеру ротатором, выкл тихо разворачивает на один тик.", true)
            .withVisibility(() -> this.field01190.isChecked() && this.modeSetting.isSelected("Снап"))
      );
      this.field01194 = this.addSetting(new BooleanSetting("Отображение подсказки", "Пишет по центру, как отключить доводку.", false));
   }

   private boolean method0735() {
      return false;
   }

   public boolean method0736() {
      return false;
   }

   @Override
   protected void onEnable() {
      ProjectilePredictionModule.method1623().setEnabledWithoutCallbacks(true);
   }

   private double method0737(LivingEntity livingEntity) {
      Vec3d vec3d = new Vec3d(mc.player.getX(), mc.player.getY() + mc.player.getStandingEyeHeight(), mc.player.getZ());
      Box boundingBox = livingEntity.getBoundingBox();
      Vec3d vec3d2 = new Vec3d(
         Math.max(boundingBox.minX, Math.min(vec3d.x, boundingBox.maxX)),
         Math.max(boundingBox.minY, Math.min(vec3d.y, boundingBox.maxY)),
         Math.max(boundingBox.minZ, Math.min(vec3d.z, boundingBox.maxZ))
      );
      Vec3d vec3d3 = new RotationAngles(mc.player.getYaw(), mc.player.getPitch()).method4363();
      return Math.toDegrees(Math.acos(vec3d3.dotProduct(vec3d2.subtract(vec3d).normalize())));
   }

   private void method0738() {
      Vec3d vec3d = this.method0723();
      RotationAngles currentRotationAngles = AimPointUtils.method4450(vec3d);
      float local982_3 = currentRotationAngles.method4362();
      float local982_4 = currentRotationAngles.method4365();
      if (this.method0731()) {
         RotationManager.method0658()
            .method0648(
               new RotationAngles(local982_3, MathHelper.clamp(local982_4, -90.0F, 90.0F)), new RotationSpeed(160.0F, 160.0F, 30.0F, 30.0F), 2L
            );
      } else {
         float local982_5 = mc.player.age + mc.getRenderTickCounter().getTickProgress(true);
         float local982_6 = Math.abs(MathHelper.wrapDegrees(local982_3 - mc.player.getYaw()));
         float local982_7 = Math.abs(MathHelper.wrapDegrees(local982_4 - mc.player.getPitch()));
         float local982_8 = (float)Math.hypot(local982_6, local982_7);
         float local982_9 = MathHelper.clamp(local982_8 <= 8.0F ? 0.0F : (local982_8 - 8.0F) / 26.0F, 0.0F, 1.0F);
         this.field01200 = MathHelper.clamp(MathHelper.lerp(0.2F, this.field01200, local982_9), 0.05F, 22.0F);
         float local982_10 = MathHelper.clamp(mc.player.getItemUseTimeLeft(), 1, 6);
         float local982_11 = (float)(
               Math.sin(local982_5 * 0.8F) * 15.0
                  + Math.sin(local982_5 * 0.04 + 17.2) * 1.5
                  + Math.sin(local982_5 * 0.11 + 5.8) * 3.0
                  + Math.sin(local982_5 * 0.07 + 12.3) * 1.0
            )
            / local982_10
            * this.field01200;
         float local982_12 = (float)(Math.sin(local982_5 * 0.4) * 15.0 + Math.sin(local982_5 * 0.03 + 54.1) * 0.5)
            / MathHelper.clamp(mc.player.getItemUseTimeLeft(), 1, 10)
            * this.field01200;
         RotationManager.method0658()
            .method0648(
               new RotationAngles(local982_3 + local982_11, MathHelper.clamp(local982_4 + local982_12, -90.0F, 90.0F)),
               new RotationSpeed(160.0F, 160.0F, 30.0F, 30.0F),
               2L
            );
      }
   }

   public LivingEntity method0740() {
      return this.field01196;
   }

   private double method0741(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0;
      } else if (itemStack.getItem() == Items.BOW) {
         return 0.05;
      } else if (itemStack.getItem() instanceof CrossbowItem) {
         return 0.05;
      } else {
         return itemStack.getItem() instanceof TridentItem ? 0.05 : 0.0;
      }
   }

   @Override
   protected void onDisable() {
      ProjectilePredictionModule.method1623().setEnabledWithoutCallbacks(false);
      if (this.field01202 && !this.field01207 && mc.player != null) {
         mc.player.setYaw(mc.player.headYaw = this.field01204);
         mc.player.setPitch(this.field01205);
      }

      this.field01196 = null;
      this.field01197 = 0;
      this.field01198 = true;
      this.field01209 = 0;
      this.field01202 = false;
      this.field01203 = 0;
      this.field01208 = false;
      this.field01200 = 0.0F;
      this.field01201 = 0.0F;
      this.field01195.clear();
   }

   public static ProjectileHelperModule method0744() {
      if (projectileHelperModule == null) {
         projectileHelperModule = new ProjectileHelperModule();
      }

      return projectileHelperModule;
   }

   private boolean method0745(Vec3d vec3d, Vec3d vec3d2) {
      return mc.world == null
         ? false
         : mc.world.raycast(new RaycastContext(vec3d, vec3d2, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType()
            == Type.BLOCK;
   }

   private record Nested0361(Vec3d launchDir, double flightTicks) {
   }
}
