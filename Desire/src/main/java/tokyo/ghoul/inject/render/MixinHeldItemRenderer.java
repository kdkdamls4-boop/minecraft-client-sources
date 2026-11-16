package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.features.modules.render.SwingAnimationModule;
import tokyo.ghoul.client.features.modules.render.ViewModelModule;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinHeldItemRenderer {
    @Shadow @Final private Minecraft minecraft;

    @Shadow
    private void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, float inverseArmHeight, float attackValue, HumanoidArm arm) {
        throw new AssertionError();
    }

    @Shadow
    public void renderItem(
            LivingEntity mob,
            ItemStack itemStack,
            ItemDisplayContext type,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords
    ) {
        throw new AssertionError();
    }

    @Inject(method = "submitHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V",
            at = @At("HEAD"))
    private void beginHandsCapture(float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
        tokyo.ghoul.client.features.modules.render.HandsModule.getInstance().m_8115();
    }

    @Unique private boolean holdMyItemsLeft;
    @Unique private float holdMyItemsPrevSwingProgress;

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void onRenderItem(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci
    ) {
        if (SwingAnimationModule.getInstance().isHmi()) {
            this.fullNewModeSubmitArmWithItem(
                    player, tickProgress, pitch, hand, swingProgress, item, equipProgress, matrices, queue, light, ci);
        }
    }


    @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
    private void applySwingAnimation(float swingProgress, PoseStack poseStack, int invert, HumanoidArm arm, CallbackInfo ci) {
        if (SwingAnimationModule.getInstance().applySwing(poseStack, arm, swingProgress)) {
            ci.cancel();
        }
    }

    @Inject(method = "applyItemArmAttackTransform", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaAttackTransform(PoseStack poseStack, HumanoidArm arm, float attackValue, CallbackInfo ci) {
        if (SwingAnimationModule.getInstance().overridesVanillaSwing()) {
            ci.cancel();
        }
    }

    @ModifyVariable(method = "applyItemArmTransform", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float removeEquipProgress(float inverseArmHeight) {
        return SwingAnimationModule.getInstance().overridesVanillaSwing() ? 0.0F : inverseArmHeight;
    }

    @ModifyConstant(method = "applyEatTransform", constant = @Constant(floatValue = 0.1F))
    private float modifyEatBobbing(float vanilla) {
        return SwingAnimationModule.getInstance().eatBobbing(vanilla);
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER, ordinal = 0))
    private void applyViewModel(
            AbstractClientPlayer player,
            float tickProgress,
            float pitch,
            InteractionHand hand,
            float swingProgress,
            ItemStack item,
            float equipProgress,
            PoseStack matrices,
            SubmitNodeCollector queue,
            int light,
            CallbackInfo ci
    ) {
        ViewModelModule.getInstance().apply(matrices, player, hand);
    }

    @Unique
    private void fullNewModeSubmitArmWithItem(
            AbstractClientPlayer player,
            float frameInterp,
            float xRot,
            InteractionHand hand,
            float attack,
            ItemStack itemStack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            CallbackInfo ci
    ) {
        SwingAnimationModule swing = SwingAnimationModule.getInstance();
        if (this.minecraft.player == null) return;
        if (player.isScoping() || player.isAutoSpinAttack()) return;

        boolean isMainHand = hand == InteractionHand.MAIN_HAND;
        HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
        if (itemStack.has(DataComponents.MAP_ID) || itemStack.is(Items.CROSSBOW)) return;
        if (!isMainHand && !(swing.newShowLeftArm.getValue() && !player.isInvisible())) return;

        poseStack.pushPose();
        if (itemStack.isEmpty()) {
            if (!player.isInvisible()) {
                this.renderNewModeEmptyHand(player, attack, inverseArmHeight, poseStack, submitNodeCollector, lightCoords, arm);
            }
            poseStack.popPose();
            ci.cancel();
            return;
        }

        ViewModelModule viewModel = ViewModelModule.getInstance();
        if (viewModel.isEnabled() && !(player.isUsingItem() && player.getUsedItemHand() == hand)) {
            poseStack.translate(viewModel.offsetX(arm), viewModel.offsetY(arm), viewModel.offsetZ(arm));
        }

        boolean usingThisHand = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
        if (usingThisHand) {
            switch (itemStack.getUseAnimation()) {
                case EAT, DRINK -> this.applyNewModeEatDrinkHandPose(poseStack, arm, frameInterp, itemStack, player);
                case BLOCK -> this.applyNewModeBlockHandPose(poseStack, arm, frameInterp, itemStack, player);
                default -> {
                    poseStack.popPose();
                    return;
                }
            }
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, 0.0F, attack, arm);
        } else {
            if (isMainHand) {
                this.newModeApplySwing(swing, attack, itemStack, poseStack, arm);
            } else {
                poseStack.translate(swing.leftX.getValue(), swing.leftZ.getValue(), swing.leftY.getValue());
            }
            this.applyNewModeHandBase(poseStack, inverseArmHeight, arm);
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, 0.0F, 0.0F, arm);
        }

        this.applyNewModeCommonItemAnchor(poseStack, arm);
        this.applyNewModeItemPose(poseStack, arm, itemStack);
        this.renderItem(
                player,
                itemStack,
                arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                poseStack,
                submitNodeCollector,
                lightCoords
        );
        poseStack.popPose();
        ci.cancel();
    }

    @Unique
    private void newModeApplySwing(SwingAnimationModule swing, float attack, ItemStack stack, PoseStack poseStack, HumanoidArm arm) {
        if (stack.isEmpty()) return;
        if (swing.auraOnly.getValue() && !auraTargetAlive()) return;
        if (attack > 0.0F && this.holdMyItemsPrevSwingProgress == 0.0F) {
            this.holdMyItemsLeft = !this.holdMyItemsLeft;
        }
        this.holdMyItemsPrevSwingProgress = attack;

        if (arm == HumanoidArm.RIGHT) {
            poseStack.translate(swing.rightX.getValue(), swing.rightY.getValue(), swing.rightZ.getValue());
        } else {
            poseStack.translate(swing.leftX.getValue(), swing.leftZ.getValue(), swing.leftY.getValue());
        }

        float swingRot = getHoldMyItemsSwingRot(attack);
        float swingEase = holdMyItemsEase(Mth.sin(attack * (float) Math.PI));
        float ll = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        boolean hasAuraTarget = auraTargetAlive();
        boolean forwardHandsAttack = swing.attackMode.is("Forward") && hasAuraTarget;
        boolean normalHandsAttack = swing.attackMode.is("Normal") && hasAuraTarget;
        boolean isShield = stack.getItem() instanceof ShieldItem;
        boolean isSpear = stack.getItem() instanceof TridentItem;
        boolean isTool = stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.AXES)
                || stack.is(ItemTags.SHOVELS)
                || isSpear;

        if (stack.is(ItemTags.SWORDS) && forwardHandsAttack) {
            poseStack.translate(0.12D * ll * swingRot, 0.04D * swingRot, -0.95D * swingEase);
            poseStack.translate(0.02D * ll * swingEase, 0.10D * swingEase, -0.10D * swingRot);
            poseStack.mulPose(Axis.YP.rotationDegrees(8.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(14.0F * swingRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-18.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(-32.0F * swingEase));
        } else if (stack.is(ItemTags.SWORDS) && normalHandsAttack) {
            poseStack.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.1D * swingEase);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F * swingEase));
            poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swingEase * ll));
        } else if ((this.holdMyItemsLeft || stack.is(ItemTags.AXES) || isShield || isSpear) && !stack.is(ItemTags.SHOVELS)) {
            if (stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)) {
                poseStack.translate(0.8D * ll * swingRot, 0.3D * swingRot, -0.5D * swingEase);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot * ll));
                poseStack.mulPose(Axis.XP.rotationDegrees(20.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-70.0F * swingRot * ll));
                poseStack.mulPose(Axis.XP.rotationDegrees((stack.is(ItemTags.SWORDS) ? -40.0F : -30.0F) * swingEase));
            } else if (isSpear) {
                poseStack.translate(0.0F, 0.0F, 0.45F * swingRot);
                poseStack.translate(-0.25F * ll * swingEase, -0.35F * swingRot, -0.6F * swingEase);
                poseStack.translate(0.0F, 0.1F * swingEase, 0.0F);
                poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot * ll));
                poseStack.mulPose(Axis.ZP.rotationDegrees(30.0F * swingRot * ll));
            } else if (isTool) {
                poseStack.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.5D * swingEase);
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * ll));
                poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F * swingEase));
            } else if (!isShield) {
                poseStack.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.1D * swingEase);
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * ll));
                poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F * swingEase));
                poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swingEase * ll));
            } else {
                poseStack.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.2D * swingEase);
                poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * swingRot));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * ll));
                poseStack.mulPose(Axis.XP.rotationDegrees(-20.0F * swingEase));
            }
        } else if (stack.is(ItemTags.SHOVELS)) {
            poseStack.translate(0.0D, 0.15D * swingRot, -0.25D * swingRot);
            poseStack.translate(0.0D, 0.0D, -0.2D * swingEase);
            poseStack.mulPose(Axis.YP.rotationDegrees(15.0F * swingRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(-35.0F * swingRot));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingEase));
        } else if (stack.is(ItemTags.SWORDS)) {
            poseStack.translate(-0.55D * ll * swingRot, -0.8D * swingRot, -0.77D * swingEase);
            poseStack.mulPose(Axis.YP.rotationDegrees(5.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(70.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(-50.0F * swingEase));
        } else if (isTool) {
            poseStack.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.5D * swingEase);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-20.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F * swingEase));
        } else {
            poseStack.translate(0.1D * ll * swingRot, 0.1D * swingRot, -0.1D * swingEase);
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F * swingRot));
            poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F * swingRot * ll));
            poseStack.mulPose(Axis.XP.rotationDegrees(-40.0F * swingEase));
            poseStack.mulPose(Axis.YP.rotationDegrees(10.0F * swingEase * ll));
        }
    }

    @Unique
    private void renderNewModeEmptyHand(
            AbstractClientPlayer player,
            float attack,
            float inverseArmHeight,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int lightCoords,
            HumanoidArm arm
    ) {
        boolean movementPose = player.isSwimming() || player.isVisuallySwimming() || (!player.onGround() && player.onClimbable());
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;

        if (arm == HumanoidArm.LEFT) {
            float swingRot = getHoldMyItemsSwingRot(attack);
            float swing = holdMyItemsEase(Mth.sin(attack * (float) Math.PI));
            if (movementPose) {
                this.applyNewModeHandBase(poseStack, inverseArmHeight, arm);
                poseStack.translate(0.05F, -0.08F, -0.1F);
                poseStack.mulPose(Axis.YP.rotationDegrees(12.0F));
            } else {
                poseStack.translate(-0.1F, -0.08F - inverseArmHeight * 0.05F, -0.12F);
                poseStack.translate(-0.03F * swing, 0.04F * swingRot, -0.05F * swing);
                poseStack.mulPose(Axis.YP.rotationDegrees(18.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-10.0F));
                poseStack.mulPose(Axis.XP.rotationDegrees(-12.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(8.0F * swing));
                poseStack.mulPose(Axis.XP.rotationDegrees(-6.0F * swingRot));
            }
            this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, inverseArmHeight * 0.35F, 0.0F, arm);
            return;
        }

        if (movementPose) {
            this.applyNewModeHandBase(poseStack, inverseArmHeight, arm);
        } else {
            float swingRot = getHoldMyItemsSwingRot(attack);
            float swing = holdMyItemsEase(Mth.sin(attack * (float) Math.PI));
            poseStack.translate(0.0F, 0.2F * swingRot, 0.15F * swingRot);
            poseStack.translate(0.1F * direction * swing, 0.15F * swing, -0.45F * swing);
            poseStack.mulPose(Axis.YP.rotationDegrees(35.0F * swing * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(-30.0F * swing));
            poseStack.mulPose(Axis.YP.rotationDegrees(-10.0F * swingRot * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * swingRot));
        }
        this.renderPlayerArm(poseStack, submitNodeCollector, lightCoords, 0.0F, 0.0F, arm);
    }

    @Unique
    private void applyNewModeHandBase(PoseStack poseStack, float inverseArmHeight, HumanoidArm arm) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(direction, -inverseArmHeight * 0.3F, 0.3F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * direction));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * direction));
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);
    }

    @Unique
    private void applyNewModeEatDrinkHandPose(PoseStack poseStack, HumanoidArm arm, float frameInterp, ItemStack itemStack, Player player) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float useTicks = (float) itemStack.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
        float progress = Mth.clamp(useTicks / 5.0F, 0.0F, 1.0F);
        float wobble = Mth.sin(useTicks / 2.0F * (float) Math.PI) / 10.0F;

        poseStack.translate(direction, 0.1F, 0.3F);
        poseStack.translate(0.2F * direction * progress, -0.7F * progress, -0.2F * progress);
        poseStack.translate(0.0F, -0.2F * wobble, -0.2F * wobble);
        poseStack.translate(0.0F, 0.1F * holdMyItemsEase(Mth.sin(progress * (float) Math.PI)), 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * direction));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * direction));
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.scale(0.9F, 0.9F, 0.9F);
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * progress * direction));
    }

    @Unique
    private void applyNewModeBlockHandPose(PoseStack poseStack, HumanoidArm arm, float frameInterp, ItemStack itemStack, Player player) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        float useTicks = (float) itemStack.getUseDuration(player) - ((float) player.getUseItemRemainingTicks() - frameInterp + 1.0F);
        float blockProgress = Mth.clamp(useTicks / 4.0F, 0.0F, 1.0F);
        float settleProgress = Mth.clamp(useTicks / 6.0F, 0.0F, 1.0F);

        poseStack.translate(0.0F, -0.2F, 0.0F);
        poseStack.translate(direction, 0.0F, 0.3F);
        poseStack.translate(0.7F * blockProgress * direction, 0.0F, -1.3F * blockProgress);
        poseStack.translate(-0.2F * direction * settleProgress, 0.0F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F * Mth.sin(settleProgress * (float) Math.PI)));
        poseStack.mulPose(Axis.YP.rotationDegrees(70.0F * blockProgress * direction));
        poseStack.mulPose(Axis.YP.rotationDegrees(45.0F * direction));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-40.0F * direction));
        poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(5.0F * direction * blockProgress));
        poseStack.mulPose(Axis.XP.rotationDegrees(-10.0F * blockProgress));
        poseStack.translate(0.0F, 0.0F, -0.2F * blockProgress);
        poseStack.scale(0.9F, 0.9F, 0.9F);
    }

    @Unique
    private void applyNewModeCommonItemAnchor(PoseStack poseStack, HumanoidArm arm) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;
        poseStack.translate(-0.3F * direction, 0.65F, -0.1F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-65.0F * direction));
        poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
    }

    @Unique
    private boolean isNewModeWeaponLike(ItemStack stack) {
        Item item = stack.getItem();
        ItemUseAnimation useAnimation = stack.getUseAnimation();
        return stack.is(ItemTags.SWORDS)
                || stack.is(ItemTags.AXES)
                || item instanceof ShieldItem
                || item instanceof TridentItem
                || item instanceof FishingRodItem
                || useAnimation == ItemUseAnimation.BOW
                || useAnimation == ItemUseAnimation.BLOCK
                || useAnimation == ItemUseAnimation.TRIDENT
                || useAnimation == ItemUseAnimation.SPEAR
                || item == Items.SHEARS
                || item == Items.CARROT_ON_A_STICK
                || item == Items.WARPED_FUNGUS_ON_A_STICK;
    }

    @Unique
    private boolean isNewModeTorch(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.TORCH || item == Items.SOUL_TORCH || item == Items.REDSTONE_TORCH;
    }

    @Unique
    private boolean isNewModeThinItem(ItemStack stack) {
        Item item = stack.getItem();
        Block block = Block.byItem(item);
        return item == Items.STRING
                || item == Items.REDSTONE
                || item == Items.LEVER
                || item == Items.TRIPWIRE_HOOK
                || block.defaultBlockState().is(BlockTags.RAILS)
                || block.defaultBlockState().is(BlockTags.CLIMBABLE)
                || block.defaultBlockState().is(BlockTags.DOORS)
                || block.defaultBlockState().is(BlockTags.BUTTONS)
                || block.defaultBlockState().is(BlockTags.PRESSURE_PLATES);
    }

    @Unique
    private boolean isNewModeSmallItem(ItemStack stack) {
        Item item = stack.getItem();
        return item.components().has(DataComponents.FOOD)
                || item instanceof BucketItem
                || (!isNewModeWeaponLike(stack) && !(item instanceof BlockItem));
    }

    @Unique
    private void applyNewModeItemPose(PoseStack poseStack, HumanoidArm arm, ItemStack stack) {
        int direction = arm == HumanoidArm.RIGHT ? 1 : -1;

        if (isNewModeTorch(stack)) {
            poseStack.scale(1.5F, 1.5F, 1.5F);
            poseStack.mulPose(Axis.YN.rotationDegrees(25.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F * direction));
            poseStack.translate(0.2F * direction, 0.2F, 0.05F);
            return;
        }

        if (isNewModeSmallItem(stack)) {
            poseStack.mulPose(Axis.YN.rotationDegrees(5.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F * direction));
            poseStack.translate(0.0F, -0.05F, -0.1F);
            poseStack.scale(0.7F, 0.7F, 0.7F);
            return;
        }

        if (isNewModeThinItem(stack)) {
            poseStack.translate(0.0F, 0.0F, -0.1F);
            poseStack.mulPose(Axis.YN.rotationDegrees(5.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(15.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F * direction));
            return;
        }

        if (stack.getItem() instanceof BlockItem) {
            poseStack.mulPose(Axis.YN.rotationDegrees(25.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(5.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(75.0F * direction));
            poseStack.translate(0.2F * direction, 0.2F, 0.05F);
            return;
        }

        ItemUseAnimation useAnimation = stack.getUseAnimation();
        if (useAnimation == ItemUseAnimation.BLOCK) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(160.0F * direction));
            poseStack.mulPose(Axis.YP.rotationDegrees(-60.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F));
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.translate(0.15F * direction, arm == HumanoidArm.RIGHT ? 0.35F : 0.45F, arm == HumanoidArm.RIGHT ? -0.15F : -0.1F);
            poseStack.translate(0.17F * direction, 0.0F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F * direction));
            return;
        }

        if (useAnimation == ItemUseAnimation.TRIDENT || useAnimation == ItemUseAnimation.SPEAR) {
            poseStack.mulPose(Axis.YN.rotationDegrees(75.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F * direction));
            poseStack.translate(-0.3F * direction, 0.0F, 0.0F);
        } else {
            poseStack.mulPose(Axis.YN.rotationDegrees(75.0F * direction));
            poseStack.mulPose(Axis.XP.rotationDegrees(70.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(45.0F * direction));
        }
        poseStack.scale(1.2F, 1.2F, 1.2F);
        if (useAnimation == ItemUseAnimation.BOW) {
            poseStack.translate(-0.1F * direction, -0.2F, 0.0F);
        }
    }

    @Unique
    private boolean auraTargetAlive() {
        AuraModule aura = AuraModule.getInstance();
        return aura.isEnabled() && aura.target != null && aura.target.isAlive();
    }

    @Unique
    private static float holdMyItemsEase(float value) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        if (value < 0.5F) {
            float doubled = 2.0F * value;
            return doubled * doubled * ((c2 + 1.0F) * doubled - c2) * 0.5F;
        }
        float shifted = 2.0F * value - 2.0F;
        return (shifted * shifted * ((c2 + 1.0F) * shifted + c2) + 2.0F) * 0.5F;
    }

    @Unique
    private static float getHoldMyItemsSwingRot(float swingProgress) {
        if (swingProgress < 0.6F) {
            return Mth.sin(Mth.clamp(swingProgress, 0.0F, 0.12506F) * 12.56F);
        }
        return Mth.sin(Mth.clamp(swingProgress, 0.62532F, 0.75038F) * 12.56F);
    }
}
