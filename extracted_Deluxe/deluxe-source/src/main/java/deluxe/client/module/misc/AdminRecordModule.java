package deluxe.client.module.misc;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.MinecraftAccess;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.module.combat.AttackController;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.MouseSensitivity;
import deluxe.client.RotationManager;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.registry.Registries;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "AdminRecord",
   description = "Пишет твои движения мышью по yaw и pitch, чтобы разобрать, как двигается рука.",
   category = ModuleCategory.MISC
)
public final class AdminRecordModule extends Module {
   private static AdminRecordModule adminRecordModule;
   public BooleanSetting field02293;
   public BooleanSetting field02294;
   private float field02295;
   private float field02296;
   private float field02297;
   private float field02298;
   private int field02299;
   private int field02300;
   private int field02301;
   private int field02302;
   private boolean field02303;
   private Path field02304;
   private BufferedWriter field02305;
   private final EventListener<PlayerTickEvent> field02306 = playerTickEvent -> {
      if (mc.player != null && this.field02303) {
         if (mc.player.age - this.field02299 == 2) {
            this.method1048(mc.player.age - 1, 0.0F, 0.0F);
         }
      }
   };
   private final EventListener<PacketEvent> field02307 = packetEvent -> {
      if (mc.player != null && packetEvent.isOutgoing()) {
         if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
            this.method1049(playerMoveC2SPacket.getYaw(0.0F), playerMoveC2SPacket.getPitch(0.0F));
         } else if (packetEvent.getPacket() instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
            playerInteractEntityC2SPacket.handle(new Handler() {
               @Override
               public void interactAt(Hand hand, Vec3d vec3d) {
               }

               @Override
               public void interact(Hand hand) {
               }

               @Override
               public void attack() {
                  AdminRecordModule.this.field02301 = MinecraftAccess.mc.player.age;
               }
            });
         }
      }
   };

   private void method1042() {
      try {
         if (this.field02305 != null) {
            this.field02305.close();
         }
      } catch (IOException exception) {
      }

      this.field02305 = null;
   }

   private void method1043(String local1419_1) {
      try {
         if (this.field02305 == null) {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("adminrecord");
            Files.createDirectories(path);
            this.field02304 = path.resolve("rec-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".csv");
            this.field02305 = Files.newBufferedWriter(this.field02304, StandardCharsets.UTF_8);
            this.field02305
               .write(
                  "time;tick;yaw;pitch;dyaw;dpitch;stepsYaw;stepsPitch;target;distance;flat;errYaw;errPitch;height;velYaw;velPitch;targetGround;ground;sprint;attack;strike;cooldown;msToReady;sinceAttack;swing;hurt;item;attackSpeed\n"
               );
            ClientChat.method2267("&7AdminRecord пишет в &f" + this.field02304);
         }

         this.field02305.write(local1419_1);
         if (++this.field02302 % 40 == 0) {
            this.field02305.flush();
         }
      } catch (IOException exception) {
         this.method1042();
         ClientChat.method2267("&cAdminRecord: запись не идёт — " + exception.getMessage());
      }
   }

   public static AdminRecordModule method1044() {
      if (adminRecordModule == null) {
         adminRecordModule = new AdminRecordModule();
      }

      return adminRecordModule;
   }

   private LivingEntity method1046() {
      if (AttackAuraModule.method1029().isEnabled() && AttackAuraModule.method1029().method1005() != null) {
         return AttackAuraModule.method1029().method1005();
      }

      LivingEntity livingEntity = null;

      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && playerEntity.isAlive()
            && mc.player.distanceTo(playerEntity) < 6.0F
            && (livingEntity == null || mc.player.distanceTo(playerEntity) < mc.player.distanceTo(livingEntity))) {
            livingEntity = playerEntity;
         }
      }

      return livingEntity;
   }

   @Override
   protected void onDisable() {
      ClientChat.method2267(
         this.field02305 == null
            ? "&7AdminRecord: ничего не записано"
            : String.format("&7AdminRecord: записано &f%d &7тиков в &f%s", this.field02302, this.field02304)
      );
      this.method1042();
   }

   private void method1048(int local1425_1, float local1425_2, float local1425_3) {
      if (mc.player != null && mc.world != null) {
         if (!this.field02293.isChecked() || !RotationManager.method0658().method0667()) {
            LivingEntity entity = this.method1046();
            if (entity != null || !this.field02294.isChecked()) {
               float local1425_5 = MouseSensitivity.method4408();
               float local1425_6 = 0.0F;
               float local1425_7 = 0.0F;
               float local1425_8 = 0.0F;
               float local1425_9 = 0.0F;
               float local1425_10 = 0.0F;
               double local1425_11 = -1.0;
               double local1425_13 = -1.0;
               if (entity != null) {
                  Vec3d eyePos = mc.player.getEyePos();
                  Vec3d center = entity.getBoundingBox().getCenter();
                  local1425_13 = Math.hypot(center.x - eyePos.x, center.z - eyePos.z);
                  local1425_11 = EntityUtils.method2816(entity);
                  local1425_10 = (float)(center.y - eyePos.y);
                  float local1425_17 = (float)Math.toDegrees(Math.atan2(center.z - eyePos.z, center.x - eyePos.x)) - 90.0F;
                  float local1425_18 = (float)(-Math.toDegrees(Math.atan2(center.y - eyePos.y, Math.max(0.6, local1425_13))));
                  local1425_6 = MathHelper.wrapDegrees(local1425_17 - this.field02295);
                  local1425_7 = local1425_18 - this.field02296;
                  if (local1425_1 - this.field02300 == 1) {
                     local1425_8 = MathHelper.wrapDegrees(local1425_17 - this.field02297);
                     local1425_9 = local1425_18 - this.field02298;
                  }

                  this.field02297 = local1425_17;
                  this.field02298 = local1425_18;
                  this.field02300 = local1425_1;
               }

               this.method1043(
                  String.format(
                     Locale.ROOT,
                     "%d;%d;%.3f;%.3f;%.3f;%.3f;%d;%d;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f;%d;%d;%d;%d;%d;%.3f;%.0f;%d;%d;%d;%s;%.2f%n",
                     System.currentTimeMillis(),
                     local1425_1,
                     this.field02295,
                     this.field02296,
                     local1425_2,
                     local1425_3,
                     Math.round(local1425_2 / local1425_5),
                     Math.round(local1425_3 / local1425_5),
                     entity == null ? "-" : entity.getName().getString(),
                     local1425_11,
                     local1425_13,
                     local1425_6,
                     local1425_7,
                     local1425_10,
                     local1425_8,
                     local1425_9,
                     entity != null && entity.isOnGround() ? 1 : 0,
                     mc.player.isOnGround() ? 1 : 0,
                     mc.player.isSprinting() ? 1 : 0,
                     this.field02301 == local1425_1 ? 1 : 0,
                     AttackController.method1169(-150L) ? 1 : 0,
                     mc.player.getAttackCooldownProgress(0.0F),
                     Math.max(
                        0.0F,
                        (0.9F - mc.player.getAttackCooldownProgress(0.0F))
                           * 1000.0F
                           / (float)mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED)
                     ),
                     Math.min(100, local1425_1 - this.field02301),
                     mc.player.handSwingTicks,
                     entity == null ? -1 : entity.hurtTime,
                     Registries.ITEM.getId(mc.player.getMainHandStack().getItem()).getPath(),
                     mc.player.getAttributeValue(EntityAttributes.ATTACK_SPEED)
                  )
               );
            }
         }
      }
   }

   private AdminRecordModule() {
      this.field02293 = this.addSetting(new BooleanSetting("Только вручную", "Не писать тики, где прицел ведёт аура или другой модуль.", true));
      this.field02294 = this.addSetting(new BooleanSetting("Только рядом с целью", "Писать только когда рядом есть игрок в 6 блоках.", true));
   }

   private void method1049(float local1427_1, float local1427_2) {
      if (!this.field02303) {
         this.field02295 = local1427_1;
         this.field02296 = local1427_2;
         this.field02303 = true;
         this.field02299 = mc.player.age;
      } else {
         float local1427_3 = MathHelper.wrapDegrees(local1427_1 - this.field02295);
         float local1427_4 = local1427_2 - this.field02296;
         this.field02295 = local1427_1;
         this.field02296 = local1427_2;
         this.field02299 = mc.player.age;
         this.method1048(mc.player.age, local1427_3, local1427_4);
      }
   }

   @Override
   protected void onEnable() {
      this.field02303 = false;
      this.field02302 = 0;
      this.field02299 = this.field02300 = this.field02301 = -10;
   }
}
