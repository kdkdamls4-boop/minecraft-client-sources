package deluxe.client.module.misc;

import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.setting.BooleanSetting;
import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.MouseSensitivity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket.Handler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.OverlayMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(name = "AdminShard", description = "Читает детект античита над хотбаром и пишет, какие ротации его двигают.", category = ModuleCategory.MISC)
public final class AdminShardModule extends Module {
   private static AdminShardModule adminShardModule;
   private static final Pattern field09956 = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*%");
   private static final Pattern field09957 = Pattern.compile("([+\\-−]?)\\s*(\\d+(?:[.,]\\d+)?)");
   private static final String[] field09958 = new String[]{
      "yaw ср",
      "yaw макс",
      "pitch ср",
      "pitch макс",
      "рывок yaw",
      "рывок pitch",
      "pitch стоит %",
      "GCD %",
      "ошибка °",
      "удары",
      "в цель %",
      "снап удара",
      "дистанция",
      "ротаций/тик"
   };
   public BooleanSetting field09959;
   public BooleanSetting field09960;
   private final double[] field09961 = new double[field09958.length];
   private final double[] field09962 = new double[field09958.length];
   private int field09963;
   private int field09964;
   private int field09965;
   private int field09966;
   private int field09967;
   private int field09968;
   private int field09969;
   private int field09970;
   private int field09971;
   private double field09972;
   private double field09973;
   private double field09974;
   private double field09975;
   private double field09976;
   private double field09977;
   private double field09978;
   private double field09979;
   private double field09980;
   private float field09981;
   private float field09982;
   private float field09983;
   private float field09984;
   private float field09985 = -1.0F;
   private float field09986 = -1.0F;
   private boolean field09987;
   private String field09988 = "";
   private BufferedWriter field09989;
   private BufferedWriter field09990;
   private final EventListener<PlayerTickEvent> field09991 = playerTickEvent -> this.field09963++;
   private final EventListener<PacketEvent> field09992 = packetEvent -> {
      if (mc.player != null) {
         if (packetEvent.isIncoming()) {
            Text text = packetEvent.getPacket() instanceof OverlayMessageS2CPacket overlayMessageS2CPacket
               ? overlayMessageS2CPacket.text()
               : (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && gameMessageS2CPacket.overlay() ? gameMessageS2CPacket.content() : null);
            if (text != null) {
               String string = text.getString();
               mc.execute(() -> this.method2929(string));
            }
         } else {
            if (packetEvent.getPacket() instanceof PlayerMoveC2SPacket playerMoveC2SPacket && playerMoveC2SPacket.changesLook()) {
               this.method2931(playerMoveC2SPacket.getYaw(0.0F), playerMoveC2SPacket.getPitch(0.0F));
            } else if (packetEvent.getPacket() instanceof PlayerInteractEntityC2SPacket playerInteractEntityC2SPacket) {
               playerInteractEntityC2SPacket.handle(new Handler() {
                  @Override
                  public void interact(Hand hand) {
                  }

                  @Override
                  public void interactAt(Hand hand, Vec3d vec3d) {
                  }

                  @Override
                  public void attack() {
                     AdminShardModule.this.method2927();
                  }
               });
            }
         }
      }
   };

   private AdminShardModule() {
      this.field09959 = this.addSetting(new BooleanSetting("Писать в чат", "Каждое изменение буфера с разбором ротаций за это время.", true));
      this.field09960 = this.addSetting(new BooleanSetting("Лог тиков", "Каждый пакет ротации и удара в ticks.csv.", true));
   }

   private static boolean method2922(float local4636_0, float local4636_1) {
      float local4636_2 = Math.abs(local4636_0) / local4636_1;
      return Math.abs(local4636_2 - Math.round(local4636_2)) < 0.02F + local4636_2 * 1.0E-4F;
   }

   private void method2923(
      String local4637_1,
      float local4637_2,
      float local4637_3,
      float local4637_4,
      float local4637_5,
      boolean local4637_6,
      LivingEntity entity,
      float local4637_8,
      boolean local4637_9
   ) {
      if (this.field09960.isChecked() && mc.player != null) {
         this.method2936(
            false,
            String.format(
               Locale.ROOT,
               "%d;%d;%s;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%d;%s;%.2f;%.2f;%d;%d;%d;%s;%.0f;%.2f%n",
               System.currentTimeMillis(),
               mc.player.age,
               local4637_1,
               this.field09981,
               this.field09982,
               local4637_2,
               local4637_3,
               local4637_4,
               local4637_5,
               local4637_6 ? 1 : 0,
               entity == null ? "-" : entity.getName().getString(),
               entity == null ? -1.0 : EntityUtils.method2816(entity),
               local4637_8,
               local4637_9 ? 1 : 0,
               mc.player.isSprinting() ? 1 : 0,
               mc.player.isOnGround() ? 1 : 0,
               AttackAuraModule.method1029().isEnabled() ? AttackAuraModule.method1029().field02093.getSelectedOption() : "-",
               this.field09985,
               this.field09986
            )
         );
      }
   }

   @Override
   protected void onDisable() {
      this.method2925();
      this.method2933();
   }

   private static float method2924(String local4640_0) {
      return Float.parseFloat(local4640_0.replace(',', '.'));
   }

   private void method2925() {
      if (this.field09970 + this.field09971 != 0) {
         StringBuilder stringBuilder = new StringBuilder(String.format(Locale.ROOT, "# рост %d, падение %d%n", this.field09970, this.field09971));
         ClientChat.method2267(
            String.format(Locale.ROOT, "&fAdminShard: &cрост %d &7/ &aпадение %d &7(среднее за окно)", this.field09970, this.field09971)
         );

         for (int index = 0; index < field09958.length; index++) {
            double local4641_3 = this.field09970 == 0 ? 0.0 : this.field09961[index] / this.field09970;
            double local4641_5 = this.field09971 == 0 ? 0.0 : this.field09962[index] / this.field09971;
            double local4641_7 = Math.abs(local4641_3 - local4641_5) / Math.max(0.001, Math.max(Math.abs(local4641_3), Math.abs(local4641_5)));
            ClientChat.method2267(
               String.format(Locale.ROOT, "&7%s: &c%.2f &7/ &a%.2f%s", field09958[index], local4641_3, local4641_5, local4641_7 > 0.25 ? " &6◀" : "")
            );
            stringBuilder.append(String.format(Locale.ROOT, "# %s;%.3f;%.3f;%.2f%n", field09958[index], local4641_3, local4641_5, local4641_7));
         }

         this.method2936(true, stringBuilder.toString());
      }
   }

   @Override
   protected void onEnable() {
      this.method2926();
      this.field09987 = false;
      this.field09988 = "";
      this.field09985 = this.field09986 = -1.0F;
      Arrays.fill(this.field09961, 0.0);
      Arrays.fill(this.field09962, 0.0);
      this.field09970 = this.field09971 = 0;
   }

   private void method2926() {
      this.field09963 = this.field09964 = this.field09965 = this.field09966 = this.field09967 = this.field09968 = this.field09969 = 0;
      this.field09972 = this.field09973 = this.field09974 = this.field09975 = this.field09976 = this.field09977 = this.field09978 = this.field09979 = this.field09980 = 0.0;
   }

   private void method2927() {
      LivingEntity entity = this.method2935();
      boolean local4644_2 = entity != null && this.field09987 && EntityUtils.method2803(entity, 6.0, this.field09981, this.field09982);
      this.field09967++;
      if (local4644_2) {
         this.field09968++;
      }

      this.field09980 = this.field09980 + Math.hypot(this.field09983, this.field09984);
      this.method2923(
         "attack",
         this.field09983,
         this.field09984,
         0.0F,
         0.0F,
         true,
         entity,
         entity == null ? -1.0F : method2932(entity, this.field09981, this.field09982),
         local4644_2
      );
   }

   private double[] method2928() {
      int local4645_1 = Math.max(1, this.field09964);
      int local4645_2 = Math.max(1, this.field09969);
      int local4645_3 = Math.max(1, this.field09967);
      return new double[]{
         this.field09972 / local4645_1,
         this.field09973,
         this.field09974 / local4645_1,
         this.field09975,
         this.field09976 / local4645_1,
         this.field09977 / local4645_1,
         this.field09965 * 100.0 / local4645_1,
         this.field09966 * 100.0 / local4645_1,
         this.field09978 / local4645_2,
         this.field09967,
         this.field09968 * 100.0 / local4645_3,
         this.field09980 / local4645_3,
         this.field09979 / local4645_2,
         (double)this.field09964 / Math.max(1, this.field09963)
      };
   }

   private void method2929(String local4646_1) {
      if (mc.player != null && this.isEnabled() && !local4646_1.equals(this.field09988)) {
         this.field09988 = local4646_1;
         this.method2923("text " + local4646_1.replace(';', ',').replace('\n', ' '), 0.0F, 0.0F, 0.0F, 0.0F, true, null, -1.0F, false);
         Matcher matcher = field09956.matcher(local4646_1);
         if (matcher.find()) {
            float local4646_3 = method2924(matcher.group(1));
            float local4646_4 = Float.NaN;
            float local4646_5 = Float.NaN;
            Matcher matcher2 = field09957.matcher(local4646_1.substring(matcher.end()));

            while (matcher2.find()) {
               float local4646_7 = method2924(matcher2.group(2));
               if (!matcher2.group(1).isEmpty() && Float.isNaN(local4646_4)) {
                  local4646_4 = matcher2.group(1).equals("+") ? local4646_7 : -local4646_7;
               } else {
                  local4646_5 = local4646_7;
               }
            }

            if (!Float.isNaN(local4646_5)) {
               float local4646_7 = this.field09986 < 0.0F ? (Float.isNaN(local4646_4) ? 0.0F : local4646_4) : local4646_5 - this.field09986;
               if (Float.isNaN(local4646_4)) {
                  local4646_4 = local4646_7;
               }

               this.field09985 = local4646_3;
               if (!(this.field09986 >= 0.0F) || local4646_7 != 0.0F) {
                  this.field09986 = local4646_5;
                  double[] doubleArray = this.method2928();
                  if (local4646_7 > 0.0F) {
                     this.field09970++;
                  }

                  if (local4646_7 < 0.0F) {
                     this.field09971++;
                  }

                  for (int index = 0; index < doubleArray.length; index++) {
                     if (local4646_7 > 0.0F) {
                        this.field09961[index] = this.field09961[index] + doubleArray[index];
                     }

                     if (local4646_7 < 0.0F) {
                        this.field09962[index] = this.field09962[index] + doubleArray[index];
                     }
                  }

                  if (this.field09959.isChecked()) {
                     ClientChat.method2267(
                        String.format(
                           Locale.ROOT,
                           "%s%+.2f &7→ &e%.2f &7(%.0f%%) &8| &7yaw &f%.1f/%.1f &7pitch &f%.1f/%.1f &7рывок &f%.1f/%.1f &7удары &f%d &7в цель &f%.0f%% &7ошибка &f%.1f° &7GCD &f%.0f%%",
                           local4646_7 > 0.0F ? "&c" : "&a",
                           local4646_7,
                           local4646_5,
                           local4646_3,
                           doubleArray[0],
                           doubleArray[1],
                           doubleArray[2],
                           doubleArray[3],
                           doubleArray[4],
                           doubleArray[5],
                           this.field09967,
                           doubleArray[10],
                           doubleArray[8],
                           doubleArray[7]
                        )
                     );
                  }

                  StringBuilder stringBuilder = new StringBuilder(
                     String.format(
                        Locale.ROOT,
                        "%d;%.2f;%.2f;%.2f;%.2f;%d",
                        System.currentTimeMillis(),
                        local4646_3,
                        local4646_4,
                        local4646_7,
                        local4646_5,
                        this.field09963
                     )
                  );

                  for (double local4646_13 : doubleArray) {
                     stringBuilder.append(String.format(Locale.ROOT, ";%.3f", local4646_13));
                  }

                  this.method2936(
                     true,
                     stringBuilder.append(';')
                        .append(AttackAuraModule.method1029().isEnabled() ? AttackAuraModule.method1029().field02093.getSelectedOption() : "-")
                        .append(';')
                        .append(local4646_1.replace(';', ',').replace('\n', ' '))
                        .append('\n')
                        .toString()
                  );
                  this.method2926();
               }
            }
         }
      }
   }

   private void method2931(float local4648_1, float local4648_2) {
      if (!this.field09987) {
         this.field09981 = local4648_1;
         this.field09982 = local4648_2;
         this.field09987 = true;
      } else {
         float local4648_3 = MathHelper.wrapDegrees(local4648_1 - this.field09981);
         float local4648_4 = local4648_2 - this.field09982;
         float local4648_5 = Math.abs(local4648_3 - this.field09983);
         float local4648_6 = Math.abs(local4648_4 - this.field09984);
         float local4648_7 = MouseSensitivity.method4408();
         boolean local4648_8 = method2922(local4648_3, local4648_7) && method2922(local4648_4, local4648_7);
         this.field09981 = local4648_1;
         this.field09982 = local4648_2;
         this.field09983 = local4648_3;
         this.field09984 = local4648_4;
         this.field09964++;
         this.field09972 = this.field09972 + Math.abs(local4648_3);
         this.field09973 = Math.max(this.field09973, Math.abs(local4648_3));
         this.field09974 = this.field09974 + Math.abs(local4648_4);
         this.field09975 = Math.max(this.field09975, Math.abs(local4648_4));
         this.field09976 += local4648_5;
         this.field09977 += local4648_6;
         if (local4648_4 == 0.0F && local4648_3 != 0.0F) {
            this.field09965++;
         }

         if (local4648_8) {
            this.field09966++;
         }

         LivingEntity entity = this.method2935();
         float local4648_10 = entity == null ? -1.0F : method2932(entity, local4648_1, local4648_2);
         if (entity != null) {
            this.field09978 += local4648_10;
            this.field09979 = this.field09979 + EntityUtils.method2816(entity);
            this.field09969++;
         }

         this.method2923(
            "rot",
            local4648_3,
            local4648_4,
            local4648_5,
            local4648_6,
            local4648_8,
            entity,
            local4648_10,
            entity != null && EntityUtils.method2803(entity, 6.0, local4648_1, local4648_2)
         );
      }
   }

   private static float method2932(LivingEntity livingEntity, float local4649_1, float local4649_2) {
      float[] floatArray = EntityUtils.method2814(livingEntity.getBoundingBox().getCenter(), mc.player.getEyePos());
      return (float)Math.hypot(MathHelper.wrapDegrees(floatArray[0] - local4649_1), floatArray[1] - local4649_2);
   }

   private void method2933() {
      try {
         if (this.field09990 != null) {
            this.field09990.close();
         }

         if (this.field09989 != null) {
            this.field09989.close();
         }
      } catch (IOException exception) {
      }

      this.field09990 = this.field09989 = null;
   }

   public static AdminShardModule method2934() {
      if (adminShardModule == null) {
         adminShardModule = new AdminShardModule();
      }

      return adminShardModule;
   }

   private LivingEntity method2935() {
      if (AttackAuraModule.method1029().isEnabled() && AttackAuraModule.method1029().method1005() != null) {
         return AttackAuraModule.method1029().method1005();
      }

      LivingEntity livingEntity = null;

      for (PlayerEntity playerEntity : mc.world.getPlayers()) {
         if (playerEntity != mc.player
            && playerEntity.isAlive()
            && mc.player.distanceTo(playerEntity) < 8.0F
            && (livingEntity == null || mc.player.distanceTo(playerEntity) < mc.player.distanceTo(livingEntity))) {
            livingEntity = playerEntity;
         }
      }

      return livingEntity;
   }

   private void method2936(boolean local4653_1, String local4653_2) {
      try {
         if (this.field09990 == null) {
            Path path = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("adminshard");
            Files.createDirectories(path);
            String local4653_4 = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            this.field09990 = Files.newBufferedWriter(path.resolve("events-" + local4653_4 + ".csv"), StandardCharsets.UTF_8);
            this.field09990.write("time;percent;delta;change;buffer;ticks;" + String.join(";", field09958) + ";rotation;text\n");
            this.field09989 = Files.newBufferedWriter(path.resolve("ticks-" + local4653_4 + ".csv"), StandardCharsets.UTF_8);
            this.field09989
               .write("time;tick;type;yaw;pitch;dyaw;dpitch;accelYaw;accelPitch;gcd;target;distance;aimError;onTarget;sprint;ground;rotation;percent;buffer\n");
            ClientChat.method2267("&7AdminShard пишет в &f" + path);
         }

         if (local4653_1) {
            this.field09990.write(local4653_2);
            this.field09990.flush();
            this.field09989.flush();
         } else {
            this.field09989.write(local4653_2);
         }
      } catch (IOException exception) {
         this.method2933();
         ClientChat.method2267("&cAdminShard: лог не пишется — " + exception.getMessage());
      }
   }
}
