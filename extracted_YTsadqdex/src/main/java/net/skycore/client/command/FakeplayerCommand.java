package net.skycore.client.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.ClientTickEvent;
import net.skycore.client.util.WorldPlayerArgument;

public final class FakeplayerCommand extends ClientCommand {
   private static final String CRYSTAL_AURA_NAME = "CrystalAura";
   private final List<Player> fakePlayers = new ArrayList<>();
   private long lastCrystalBoomTick = -1L;
   private static final AtomicInteger NEXT_ENTITY_ID = new AtomicInteger(1000000000);
   public static final FakeplayerCommand INSTANCE = new FakeplayerCommand();
   private static final Minecraft mc = Minecraft.getInstance();
   private Vec3 lastCrystalBoomPos;
   private static final float CRYSTAL_EXPLOSION_POWER = 6.0F;

   private int addFakePlayer(String text, FakeplayerCommand.FakeMode fakeMode) {
      if (mc.level instanceof ClientLevel level && mc.player != null) {
         String text2 = text != null ? text : (fakeMode == FakeplayerCommand.FakeMode.CRYSTAL_AURA ? "CrystalAura" : "FakePlayer" + (this.fakePlayers.size() + 1));
         if (fakeMode == FakeplayerCommand.FakeMode.CRYSTAL_AURA) {
            this.removeFakePlayer("CrystalAura", true);
         }

         if (fakeMode == FakeplayerCommand.FakeMode.ELYTRA_TARGET) {
            this.removeFakePlayer("ElytraTarget", true);
         }

         FakeplayerCommand.FakePlayer fakePlayer = new FakeplayerCommand.FakePlayer(level, new GameProfile(UUID.randomUUID(), text2), fakeMode);
         Vec3 position = this.resolveSpawnPos(fakeMode);
         fakePlayer.setPos(position.x, position.y, position.z);
         fakePlayer.setYRot(mc.player.getYRot());
         fakePlayer.setXRot(mc.player.getXRot());
         fakePlayer.setYHeadRot(mc.player.getYHeadRot());
         fakePlayer.yBodyRot = mc.player.yBodyRot;
         fakePlayer.setDeltaMovement(Vec3.ZERO);
         fakePlayer.setOnGround(true);
         fakePlayer.setId(NEXT_ENTITY_ID.getAndIncrement());
         level.addEntity(fakePlayer);
         this.fakePlayers.add(fakePlayer);
         this.equipArmor(fakePlayer);
         if (fakeMode == FakeplayerCommand.FakeMode.ELYTRA_TARGET) {
            fakePlayer.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
            fakePlayer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.FIREWORK_ROCKET));
            fakePlayer.flightOrigin = position;
            fakePlayer.setXRot(-10.0F);
            fakePlayer.setOnGround(false);
            fakePlayer.setNoGravity(true);
            fakePlayer.startFallFlying();
            fakePlayer.setPose(Pose.FALL_FLYING);
         }

         if (fakeMode == FakeplayerCommand.FakeMode.CRYSTAL_AURA) {
            ChatMessages.displayInfo(
               "CrystalAura fake: "
                  + text2
                  + " — CrystalAura target; crystals pass through it x: "
                  + this.format(fakePlayer.getX())
                  + " y: "
                  + this.format(fakePlayer.getY())
                  + " z: "
                  + this.format(fakePlayer.getZ())
            );
         } else {
            ChatMessages.displayInfo(
               "Spawned " + text2 + " x: " + this.format(fakePlayer.getX()) + " y: " + this.format(fakePlayer.getY()) + " z: " + this.format(fakePlayer.getZ())
            );
         }

         return 1;
      } else {
         ChatMessages.displayInfo("World or player unavailable");
         return 0;
      }
   }

   public List<Player> getFakePlayers() {
      return this.fakePlayers;
   }

   private float smoothRotate(Player player, float value, boolean condition) {
      float value2;
      if (condition) {
         value2 = player.getYRot();
      } else {
         value2 = player.getXRot();
      }

      float value3 = value2;
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      float computedValue = 0.8F + threadLocalRandom.nextFloat() * 0.2F;
      return Mth.lerp(computedValue, value3, value3 + value);
   }

   private int listFakePlayers() {
      if (this.fakePlayers.isEmpty()) {
         ChatMessages.displayInfo("No fake players");
         return 1;
      }

      StringBuilder stringBuilder = new StringBuilder("Fake players:");

      for (Player player : this.fakePlayers) {
         String text = player instanceof FakeplayerCommand.FakePlayer fakePlayer && fakePlayer.isCrystalAura()
            ? " [crystalaura]"
            : (player instanceof FakeplayerCommand.FakePlayer fakePlayer2 && fakePlayer2.isElytraTarget() ? " [elytratarget]" : "");
         stringBuilder.append("\n")
            .append(player.getScoreboardName())
            .append(text)
            .append(" x: ")
            .append(this.format(player.getX()))
            .append(" y: ")
            .append(this.format(player.getY()))
            .append(" z: ")
            .append(this.format(player.getZ()));
      }

      ChatMessages.displayInfo(stringBuilder.toString());
      return 1;
   }

   public void onCrystalExploded(Vec3 position) {
      if (!this.fakePlayers.isEmpty() && position != null) {
         if (!mc.isSameThread()) {
            Vec3 position2 = position;
            mc.execute(() -> {
               this.applyCrystalExplosion(position2);
            });
         } else {
            this.applyCrystalExplosion(position);
         }
      }
   }

   private Vec3 resolveSpawnPos(FakeplayerCommand.FakeMode fakeMode) {
      if (fakeMode == FakeplayerCommand.FakeMode.ELYTRA_TARGET) {
         if (mc.player != null) {
            double x = mc.player.getX();
            double z = mc.player.getZ();
            return new Vec3(x, 90.0, z);
         }
      }

      if (fakeMode == FakeplayerCommand.FakeMode.CRYSTAL_AURA) {
         if (mc.player != null) {
            float yRot = mc.player.getYRot();
            double value = yRot;
            double angleRadians = Math.toRadians(value);
            double value2 = 2.5;
            double x2 = mc.player.getX();
            double computedValue = x2 - Math.sin(angleRadians) * value2;
            double y = mc.player.getY();
            return new Vec3(computedValue, y, mc.player.getZ() + Math.cos(angleRadians) * value2);
         }
      }

      return mc.player.position();
   }

   private void updateFakePlayer(Player player) {
      if (!(player instanceof FakeplayerCommand.FakePlayer fakePlayer && fakePlayer.isElytraTarget())) {
         player.noPhysics = false;
         if (!(
            player instanceof FakeplayerCommand.FakePlayer fakePlayer2 && fakePlayer2.isCrystalAura() && (!fakePlayer2.onGround() || fakePlayer2.getDeltaMovement().lengthSqr() > 0.04)
         )) {
            Vec3 eyePosition = mc.player.getEyePosition().subtract(player.getEyePosition());
            if (!(eyePosition.lengthSqr() < 1.0E-4)) {
               float x = (float)(Math.toDegrees(Math.atan2(eyePosition.z, eyePosition.x)) - 90.0);
               float y = (float)(-Math.toDegrees(Math.asin(Mth.clamp(eyePosition.y / eyePosition.length(), -1.0, 1.0))));
               float yRot = Mth.wrapDegrees(x - player.getYRot());
               float computedValue = 90.0F + (ThreadLocalRandom.current().nextFloat() * 24.0F - 12.0F);
               float yRot2 = Mth.clamp(this.smoothRotate(player, yRot, true) - player.getYRot(), -computedValue, computedValue);
               float xRot = Mth.clamp(this.smoothRotate(player, y - player.getXRot(), false) - player.getXRot(), -computedValue, computedValue);
               player.setYRot(player.getYRot() + yRot2);
               player.setXRot(player.getXRot() + xRot);
               player.setYHeadRot(player.getYRot());
               if (!(player instanceof FakeplayerCommand.FakePlayer fakePlayer3 && fakePlayer3.isCrystalAura())) {
                  player.setSwimming(true);
               }
            }
         }
      }
   }

   private void applyCrystalExplosion(Vec3 lastCrystalBoomPos2) {
      if (!this.fakePlayers.isEmpty() && mc.level != null) {
         long gameTime = mc.level.getGameTime();
         if (gameTime != this.lastCrystalBoomTick || this.lastCrystalBoomPos == null || !(this.lastCrystalBoomPos.distanceToSqr(lastCrystalBoomPos2) < 0.25)) {
            this.lastCrystalBoomTick = gameTime;
            this.lastCrystalBoomPos = lastCrystalBoomPos2;

            for (Player player : this.fakePlayers) {
               if (player instanceof FakeplayerCommand.FakePlayer fakePlayer && !fakePlayer.isRemoved()) {
                  fakePlayer.damageFromCrystal(lastCrystalBoomPos2);
               }
            }
         }
      }
   }

   private void equipArmor(Player player) {
      if (mc.level != null) {
         Reference orThrow = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
         ItemStack itemStack = new ItemStack(Items.NETHERITE_HELMET);
         ItemStack itemStack2 = new ItemStack(Items.NETHERITE_CHESTPLATE);
         ItemStack itemStack3 = new ItemStack(Items.NETHERITE_LEGGINGS);
         ItemStack itemStack4 = new ItemStack(Items.NETHERITE_BOOTS);
         itemStack.enchant(orThrow, 1);
         itemStack2.enchant(orThrow, 1);
         itemStack3.enchant(orThrow, 1);
         itemStack4.enchant(orThrow, 1);
         player.setItemSlot(EquipmentSlot.HEAD, itemStack);
         player.setItemSlot(EquipmentSlot.CHEST, itemStack2);
         player.setItemSlot(EquipmentSlot.LEGS, itemStack3);
         player.setItemSlot(EquipmentSlot.FEET, itemStack4);
         ItemStack itemStack5 = new ItemStack(Items.TOTEM_OF_UNDYING);
         itemStack5.enchant(orThrow, 1);
         player.setItemSlot(EquipmentSlot.OFFHAND, itemStack5);
      }
   }

   private String format(double value) {
      Locale rOOT = Locale.ROOT;
      Object[] objectValues = new Object[1];
      objectValues[0] = value;
      return String.format(rOOT, "%.1f", objectValues);
   }

   public static boolean isFake(Entity entity) {
      return entity instanceof FakeplayerCommand.FakePlayer;
   }

   @Override
   public void execute(LiteralArgumentBuilder<ClientSuggestionProvider> literalArgumentBuilder) {
      LiteralArgumentBuilder literalArgumentBuilder2 = literal("add");
      LiteralArgumentBuilder literalArgumentBuilder3 = (LiteralArgumentBuilder)literalArgumentBuilder2.executes(commandContext -> {
         return this.addFakePlayer(null, FakeplayerCommand.FakeMode.NORMAL);
      });
      StringArgumentType stringArgumentType = StringArgumentType.word();
      ArgumentBuilder string = arg("name", stringArgumentType).executes(commandContext2 -> {
         String string2 = StringArgumentType.getString(commandContext2, "name");
         return this.addFakePlayer(string2, FakeplayerCommand.FakeMode.NORMAL);
      });
      ArgumentBuilder argumentBuilder = literalArgumentBuilder3.then(string);
      literalArgumentBuilder.then(argumentBuilder);
      ArgumentBuilder argumentBuilder2 = literal("crystalaura").executes(commandContext3 -> {
         return this.addFakePlayer("CrystalAura", FakeplayerCommand.FakeMode.CRYSTAL_AURA);
      });
      literalArgumentBuilder.then(argumentBuilder2);
      literalArgumentBuilder.then(literal("elytratarget").executes(commandContext4 -> {
         return this.addFakePlayer("ElytraTarget", FakeplayerCommand.FakeMode.ELYTRA_TARGET);
      }));
      LiteralArgumentBuilder literalArgumentBuilder4 = literal("clear");
      Command command = commandContext5 -> {
         return this.clearFakePlayers();
      };
      literalArgumentBuilder.then(literalArgumentBuilder4.executes(command));
      LiteralArgumentBuilder literalArgumentBuilder5 = literal("remove");
      WorldPlayerArgument elements = WorldPlayerArgument.create(this.fakePlayers);
      string = arg("name", elements);
      string = string.executes(commandContext6 -> {
         String argument = (String)commandContext6.getArgument("name", String.class);
         return this.removeFakePlayer(argument, false);
      });
      ArgumentBuilder argumentBuilder3 = literalArgumentBuilder5.then(string);
      literalArgumentBuilder.then(argumentBuilder3);
      LiteralArgumentBuilder literalArgumentBuilder6 = literal("list");
      ArgumentBuilder argumentBuilder4 = literalArgumentBuilder6.executes(commandContext7 -> {
         return this.listFakePlayers();
      });
      literalArgumentBuilder.then(argumentBuilder4);
      literalArgumentBuilder.executes(commandContext8 -> {
         ChatMessages.displayInfo("fakeplayer add [name] | crystalaura | elytratarget | remove <name> | clear | list");
         return 1;
      });
   }

   private void onTick(ClientTickEvent event) {
      if (mc.player != null && mc.level != null) {
         this.fakePlayers.removeIf(Entity::isRemoved);

         for (Player player : this.fakePlayers) {
            if (player.level() != mc.level) {
               mc.level.removeEntity(player.getId(), RemovalReason.KILLED);
            } else {
               this.updateFakePlayer(player);
            }
         }
      } else {
         if (!this.fakePlayers.isEmpty()) {
            this.fakePlayers.clear();
         }
      }
   }

   private int removeFakePlayer(String text, boolean condition) {
      if (mc.level == null) {
         return 0;
      }

      Iterator iterator = this.fakePlayers.iterator();

      while (iterator.hasNext()) {
         Player element = (Player)iterator.next();
         if (element.getGameProfile().name().equalsIgnoreCase(text)) {
            mc.level.removeEntity(element.getId(), RemovalReason.KILLED);
            iterator.remove();
            if (!condition) {
               ChatMessages.displayInfo("Removed " + text);
            }

            return 1;
         }
      }

      if (!condition && !text.equalsIgnoreCase("CrystalAura")) {
         ChatMessages.displayInfo("Player " + text + " not found");
      }

      return 0;
   }

   private FakeplayerCommand() {
      super("fakeplayer");
      SkycoreClient.events().subscribe(ClientTickEvent.class, this::onTick);
   }

   private int clearFakePlayers() {
      int value = 1;
      value = (byte)1;
      if (mc.level == null) {
         return 0;
      }

      int size = this.fakePlayers.size();
      Iterator iterator = this.fakePlayers.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            this.fakePlayers.clear();
            String text = "Fake players removed: " + size;
            ChatMessages.displayInfo(text);
            return 1;
         }

         Player element = (Player)iterator.next();
         value = (byte)1;
         ClientLevel level = mc.level;
         value = element.getId();
         level.removeEntity(value, RemovalReason.KILLED);
      }
   }

   enum FakeMode {
      NORMAL,
      CRYSTAL_AURA,
      ELYTRA_TARGET;
   }

   public static final class FakePlayer extends RemotePlayer {
      private int nextFlightTurn;
      private float flightPitch = -10.0F;
      private final FakeplayerCommand.FakeMode mode;
      private Vec3 flightOrigin;
      private float flightYaw;
      private int crystalHurtCooldown;
      private FireworkRocketEntity flightRocket;
      private int flightTicks;

      public boolean isCrystalAura() {
         return this.mode == FakeplayerCommand.FakeMode.CRYSTAL_AURA;
      }

      public void damageFromCrystal(Vec3 position) {
         if (this.crystalHurtCooldown <= 0) {
            float damage = this.calculateCrystalDamage(position);
            if (damage <= 0.0F) {
               if (this.isCrystalAura()) {
                  this.applyCrystalKnockback(position, 0.45F);
               }
            } else {
               this.crystalHurtCooldown = 8;
               float intermediate = this.isCrystalAura() ? 1.15F : 1.0F;
               this.applyCrystalKnockback(position, intermediate);
               DamageSource damageSource = this.damageSources().explosion(null, null);
               this.applyLocalDamage(damage, damageSource, false);
            }
         }
      }

      private float calculateCrystalDamage(Vec3 position) {
         float distance = 12.0F;
         double computedValue = Math.sqrt(this.distanceToSqr(position)) / distance;
         if (computedValue > 1.0) {
            return 0.0F;
         }

         float explosionExposure = this.getExplosionExposure(position);
         if (explosionExposure <= 0.0F) {
            return 0.0F;
         }

         double computedValue2 = (1.0 - computedValue) * explosionExposure;
         float computedValue3 = (float)((computedValue2 * computedValue2 + computedValue2) / 2.0 * 7.0 * distance + 1.0);
         float armorValue = this.getArmorValue();
         float attributeValue = (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
         float computedValue4 = 2.0F + attributeValue / 4.0F;
         float clampedValue = Mth.clamp(armorValue - computedValue3 / computedValue4, armorValue * 0.2F, 20.0F);
         return computedValue3 * (1.0F - clampedValue / 25.0F);
      }

      public FakePlayer(ClientLevel level, GameProfile gameProfile, FakeplayerCommand.FakeMode mode2) {
         super(level, gameProfile);
         this.mode = mode2;
         if (mode2 == FakeplayerCommand.FakeMode.CRYSTAL_AURA) {
            this.setNoGravity(false);
            this.noPhysics = false;
         }
      }

      private void tickElytraFlight() {
         super.tick();
         this.flightTicks++;
         this.setNoGravity(true);
         this.noPhysics = false;
         this.setOnGround(false);
         this.setSwimming(false);
         this.startFallFlying();
         this.setPose(Pose.FALL_FLYING);
         this.fallDistance = 0.0;
         Vec3 position = this.flightOrigin.subtract(this.position());
         if (this.flightTicks >= this.nextFlightTurn) {
            this.flightYaw = this.getYRot() + this.getRandom().nextFloat() * 140.0F - 70.0F;
            this.flightPitch = this.getRandom().nextFloat() * 30.0F - 20.0F;
            this.nextFlightTurn = this.flightTicks + 50 + this.getRandom().nextInt(60);
         }

         if (position.horizontalDistanceSqr() > 2025.0 || this.horizontalCollision) {
            this.flightYaw = (float)(Math.toDegrees(Math.atan2(position.z, position.x)) - 90.0);
         }

         float y = Mth.clamp(this.flightPitch - (float)position.y * 2.0F, -30.0F, 20.0F);
         this.setYRot(this.getYRot() + Mth.clamp(Mth.wrapDegrees(this.flightYaw - this.getYRot()) * 0.08F, -2.5F, 2.5F));
         this.setXRot(this.getXRot() + Mth.clamp((y - this.getXRot()) * 0.08F, -1.0F, 1.0F));
         this.setYHeadRot(this.getYRot());
         this.yBodyRot = this.getYRot();
         double angle = this.flightTicks >= 300 && this.flightTicks % 300 < 30 ? 1.25 : 0.65;
         Vec3 lookAngle = this.getLookAngle().scale(angle);
         Vec3 deltaMovement = this.getDeltaMovement().scale(0.9).add(lookAngle.scale(0.1));
         this.setDeltaMovement(deltaMovement);
         this.move(MoverType.SELF, deltaMovement);
         if (this.flightTicks % 300 == 0) {
            this.swing(InteractionHand.MAIN_HAND);
            this.flightRocket = new FireworkRocketEntity(this.level(), this.getMainHandItem(), this) {
               @Override
               public void tick() {
                  super.tick();
                  if (this.tickCount >= 30 || FakePlayer.this.isRemoved()) {
                     this.discard();
                  }
               }
            };
            this.flightRocket.setId(FakeplayerCommand.NEXT_ENTITY_ID.getAndIncrement());
            ((ClientLevel)this.level()).addEntity(this.flightRocket);
         }
      }

      private boolean hasGroundSupport() {
         AABB boundingBox = this.getBoundingBox();
         AABB bounds = new AABB(boundingBox.minX + 0.05, boundingBox.minY - 0.08, boundingBox.minZ + 0.05, boundingBox.maxX - 0.05, boundingBox.minY + 0.01, boundingBox.maxZ - 0.05);
         int roundedDown = Mth.floor(bounds.minX);
         int roundedDown2 = Mth.floor(bounds.maxX);
         int roundedDown3 = Mth.floor(bounds.minY);
         int roundedDown4 = Mth.floor(bounds.maxY);
         int roundedDown5 = Mth.floor(bounds.minZ);
         int roundedDown6 = Mth.floor(bounds.maxZ);
         MutableBlockPos mutablePos = new MutableBlockPos();

         for (int index = roundedDown; index <= roundedDown2; index++) {
            for (int index2 = roundedDown3; index2 <= roundedDown4; index2++) {
               for (int index3 = roundedDown5; index3 <= roundedDown6; index3++) {
                  mutablePos.set(index, index2, index3);
                  BlockState blockState = this.level().getBlockState(mutablePos);
                  if (!blockState.isAir() && !blockState.canBeReplaced() && !blockState.getCollisionShape(this.level(), mutablePos).isEmpty()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }

      public boolean isElytraTarget() {
         return this.mode == FakeplayerCommand.FakeMode.ELYTRA_TARGET;
      }

      private void applyCrystalKnockback(Vec3 position, float value) {
         float distance = 12.0F;
         double computedValue = Math.sqrt(this.distanceToSqr(position)) / distance;
         if (!(computedValue > 1.0)) {
            float explosionExposure = this.getExplosionExposure(position);
            if (!(explosionExposure <= 0.0F) || this.isCrystalAura()) {
               if (this.isCrystalAura() && explosionExposure <= 0.0F) {
                  explosionExposure = 0.65F;
               }

               double computedValue2 = (1.0 - computedValue) * explosionExposure * value;
               Vec3 boundingBox = this.getBoundingBox().getCenter().subtract(position);
               double length = boundingBox.length();
               if (this.isCrystalAura()) {
                  double delta;
                  double delta2;
                  if (length < 1.0E-4) {
                     delta = (this.getRandom().nextDouble() - 0.5) * 0.35;
                     delta2 = (this.getRandom().nextDouble() - 0.5) * 0.35;
                  } else {
                     delta = boundingBox.x / length * computedValue2 * 0.85;
                     delta2 = boundingBox.z / length * computedValue2 * 0.85;
                  }

                  double delta3 = -0.35 - computedValue2 * 0.55;
                  if (position.y < this.getY() + 0.2) {
                     delta3 = -0.55 - computedValue2 * 0.75;
                  }

                  this.setDeltaMovement(delta, delta3, delta2);
               } else if (length < 1.0E-4) {
                  this.setDeltaMovement(
                     this.getDeltaMovement().add((this.getRandom().nextDouble() - 0.5) * 0.15, 0.4 * computedValue2, (this.getRandom().nextDouble() - 0.5) * 0.15)
                  );
               } else {
                  Vec3 position2 = boundingBox.multiply(1.0 / length, 1.0 / length, 1.0 / length);
                  this.setDeltaMovement(this.getDeltaMovement().add(position2.x * computedValue2, position2.y * computedValue2, position2.z * computedValue2));
               }

               this.hurtMarked = true;
               this.setOnGround(false);
               this.setNoGravity(false);
               this.noPhysics = false;
            }
         }
      }

      @Override
      public ItemStack getItemInHand(InteractionHand interactionHand) {
         ItemStack itemStack;
         if (interactionHand == InteractionHand.OFF_HAND) {
            itemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
         } else {
            itemStack = super.getItemInHand(interactionHand);
         }

         return itemStack;
      }

      private boolean useTotem() {
         this.setHealth(5.0F);
         this.removeAllEffects();
         this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 4));
         this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
         this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
         this.handleEntityEvent((byte)35);
         if (FakeplayerCommand.mc.player != null) {
            FakeplayerCommand.mc.player.playSound(SoundEvents.TOTEM_USE, 0.5F, this.getVoicePitch());
         }

         for (int value = 0; value < 30; value++) {
            this.level()
               .addParticle(
                  ParticleTypes.TOTEM_OF_UNDYING,
                  this.getX(),
                  this.getY(0.5),
                  this.getZ(),
                  (this.getRandom().nextDouble() - 0.5) * 0.5,
                  this.getRandom().nextDouble() * 0.5,
                  (this.getRandom().nextDouble() - 0.5) * 0.5
               );
         }

         if (this.isCrystalAura()) {
            this.setOnGround(false);
            this.setNoGravity(false);
            this.setDeltaMovement(this.getDeltaMovement().x * 0.5, Math.min(this.getDeltaMovement().y, -0.45), this.getDeltaMovement().z * 0.5);
         }

         return true;
      }

      @Override
      public void remove(RemovalReason removalReason) {
         if (this.flightRocket != null) {
            this.flightRocket.discard();
            this.flightRocket = null;
         }

         super.remove(removalReason);
      }

      private float getExplosionExposure(Vec3 position) {
         AABB boundingBox = this.getBoundingBox();
         double xsize = 1.0 / (boundingBox.getXsize() * 2.0 + 1.0);
         double ysize = 1.0 / (boundingBox.getYsize() * 2.0 + 1.0);
         double zsize = 1.0 / (boundingBox.getZsize() * 2.0 + 1.0);
         if (!(xsize <= 0.0) && !(ysize <= 0.0) && !(zsize <= 0.0)) {
            double index = (1.0 - Math.floor(1.0 / xsize) * xsize) / 2.0;
            double index2 = (1.0 - Math.floor(1.0 / zsize) * zsize) / 2.0;
            int value = 0;
            int value2 = 0;

            for (double index3 = 0.0; index3 <= 1.0; index3 += xsize) {
               for (double index4 = 0.0; index4 <= 1.0; index4 += ysize) {
                  for (double index5 = 0.0; index5 <= 1.0; index5 += zsize) {
                     Vec3 position2 = new Vec3(
                        Mth.lerp(index3, boundingBox.minX, boundingBox.maxX) + index, Mth.lerp(index4, boundingBox.minY, boundingBox.maxY), Mth.lerp(index5, boundingBox.minZ, boundingBox.maxZ) + index2
                     );
                     BlockHitResult blockHitResult = this.level().clip(new ClipContext(position2, position, Block.COLLIDER, Fluid.NONE, this));
                     if (blockHitResult.getType() == Type.MISS) {
                        value++;
                     }

                     value2++;
                  }
               }
            }

            return value2 == 0 ? 0.0F : (float)value / value2;
         } else {
            return 0.0F;
         }
      }

      private float calculateMeleeDamage(DamageSource damageSource) {
         if (damageSource.getEntity() instanceof Player player) {
            float attributeValue = (float)player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float attackStrengthScale = player.getAttackStrengthScale(0.5F);
            attributeValue *= 0.2F + attackStrengthScale * attackStrengthScale * 0.8F;
            ItemStack mainHandItem = player.getMainHandItem();
            if (mainHandItem.is(ItemTags.SWORDS)) {
               attributeValue += 6.0F;
            }

            return Math.max(1.0F, attributeValue);
         } else {
            return 2.0F;
         }
      }

      private void tickCrystalFallPhysics() {
         this.baseTick();
         this.setNoGravity(false);
         this.noPhysics = false;
         this.setSwimming(false);
         this.setSprinting(false);
         boolean groundSupport = this.hasGroundSupport();
         this.setOnGround(groundSupport);
         Vec3 deltaMovement = this.getDeltaMovement();
         if (!groundSupport) {
            deltaMovement = deltaMovement.add(0.0, -0.08, 0.0);
            if (deltaMovement.y < -3.92) {
               deltaMovement = new Vec3(deltaMovement.x, -3.92, deltaMovement.z);
            }
         } else if (deltaMovement.y < 0.0) {
            deltaMovement = new Vec3(deltaMovement.x, 0.0, deltaMovement.z);
         }

         float value = groundSupport ? 0.6F : 0.91F;
         deltaMovement = new Vec3(deltaMovement.x * value, deltaMovement.y * 0.98, deltaMovement.z * value);
         if (Math.abs(deltaMovement.x) < 0.003) {
            deltaMovement = new Vec3(0.0, deltaMovement.y, deltaMovement.z);
         }

         if (Math.abs(deltaMovement.z) < 0.003) {
            deltaMovement = new Vec3(deltaMovement.x, deltaMovement.y, 0.0);
         }

         this.setDeltaMovement(deltaMovement);
         this.move(MoverType.SELF, this.getDeltaMovement());
         if (this.verticalCollision && this.getDeltaMovement().y < 0.0 && this.hasGroundSupport()) {
            this.setOnGround(true);
            Vec3 deltaMovement2 = this.getDeltaMovement();
            this.setDeltaMovement(deltaMovement2.x, 0.0, deltaMovement2.z);
         } else if (!this.hasGroundSupport()) {
            this.setOnGround(false);
            if (this.getDeltaMovement().y > -0.1) {
               this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.12, 0.0));
            }
         }

         this.fallDistance = this.onGround() ? 0.0 : this.fallDistance + Math.max(0.0, -this.getDeltaMovement().y);
      }

      public FakePlayer(ClientLevel level, GameProfile gameProfile) {
         this(level, gameProfile, FakeplayerCommand.FakeMode.NORMAL);
      }

      @Override
      public boolean hurtClient(DamageSource damageSource) {
         float damage = this.calculateMeleeDamage(damageSource);
         if (damage <= 0.0F) {
            return false;
         }

         this.applyLocalDamage(damage, damageSource, true);
         return true;
      }

      @Override
      public void tick() {
         if (this.crystalHurtCooldown > 0) {
            this.crystalHurtCooldown--;
         }

         if (this.isElytraTarget()) {
            this.tickElytraFlight();
         } else {
            if (this.isCrystalAura()) {
               this.tickCrystalFallPhysics();
            } else {
               super.tick();
               this.baseTick();
               this.aiStep();
            }
         }
      }

      private void applyLocalDamage(float value, DamageSource damageSource, boolean condition) {
         float health = this.getHealth();
         float computedValue = health - value;
         this.hurtDuration = 10;
         this.hurtTime = 10;
         this.invulnerableTime = 20;
         this.walkAnimation.setSpeed(1.5F);
         this.handleDamageEvent(damageSource);
         if (this.isCrystalAura()) {
            if (computedValue <= 1.0F) {
               this.setHealth(20.0F);
               this.removeAllEffects();
               this.handleEntityEvent((byte)35);
               if (FakeplayerCommand.mc.player != null) {
                  FakeplayerCommand.mc.player.playSound(SoundEvents.TOTEM_USE, 0.35F, this.getVoicePitch());
               }
            } else {
               this.setHealth(computedValue);
            }
         } else if (computedValue <= 0.0F) {
            if (!this.useTotem()) {
               this.setHealth(0.0F);
               this.remove(RemovalReason.KILLED);
            }
         } else {
            this.setHealth(computedValue);
         }

         if (condition && FakeplayerCommand.mc.player != null) {
            float attackStrengthScale = FakeplayerCommand.mc.player.getAttackStrengthScale(0.5F);
            int value2 = attackStrengthScale > 0.9F ? 1 : 0;
            int value3 = value2 != 0
                  && FakeplayerCommand.mc.player.fallDistance > 0.0
                  && !FakeplayerCommand.mc.player.onGround()
                  && !FakeplayerCommand.mc.player.onClimbable()
                  && !FakeplayerCommand.mc.player.isInWater()
                  && !FakeplayerCommand.mc.player.isPassenger()
                  && !FakeplayerCommand.mc.player.isSprinting()
               ? 1
               : 0;
            byte flag = 0;
            if (value2 != 0 && value3 == 0 && FakeplayerCommand.mc.player.onGround()) {
               double deltaMovement = FakeplayerCommand.mc.player.getDeltaMovement().horizontalDistanceSqr();
               double speed = FakeplayerCommand.mc.player.getSpeed() * 2.5;
               if (deltaMovement < Mth.square(speed) && FakeplayerCommand.mc.player.getItemInHand(InteractionHand.MAIN_HAND).is(ItemTags.SWORDS)) {
                  flag = 1;
               }
            }

            if (value3 != 0) {
               FakeplayerCommand.mc.player.playSound(SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundVolume(), this.getVoicePitch());
            } else if (flag != 0) {
               FakeplayerCommand.mc.player.playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundVolume(), this.getVoicePitch());
            } else if (value2 != 0) {
               FakeplayerCommand.mc.player.playSound(SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundVolume(), this.getVoicePitch());
            } else {
               FakeplayerCommand.mc.player.playSound(SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundVolume(), this.getVoicePitch());
            }
         }
      }
   }
}
