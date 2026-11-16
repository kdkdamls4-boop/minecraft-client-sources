package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.RunSetting;

@ModuleRegister(name = "View Model", description = "Позиция предметов в руке", category = Category.RENDER)
public class ViewModelModule extends Module {
    @Getter private static final ViewModelModule instance = new ViewModelModule();

    public final SliderSetting rightX = new SliderSetting("Right X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting rightY = new SliderSetting("Right Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting rightZ = new SliderSetting("Right Z").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftX = new SliderSetting("Left X").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftY = new SliderSetting("Left Y").value(0f).range(-2f, 2f).step(0.1f);
    public final SliderSetting leftZ = new SliderSetting("Left Z").value(0f).range(-2f, 2f).step(0.1f);

    private final RunSetting reset = new RunSetting("Reset position").value(this::resetPos);

    public ViewModelModule() {
        addSettings(rightX, rightY, rightZ, leftX, leftY, leftZ, reset);
    }

    @Override
    public void onEvent() {

    }

    public float offsetX(HumanoidArm arm) {
        return (arm == HumanoidArm.RIGHT ? rightX : leftX).getValue();
    }

    public float offsetY(HumanoidArm arm) {
        return (arm == HumanoidArm.RIGHT ? rightY : leftY).getValue();
    }

    public float offsetZ(HumanoidArm arm) {
        return (arm == HumanoidArm.RIGHT ? rightZ : leftZ).getValue();
    }

    public void apply(PoseStack poseStack, AbstractClientPlayer player, InteractionHand hand) {
        if (!isEnabled() || poseStack == null || player == null) return;
        if (player.isUsingItem() && player.getUsedItemHand() == hand) return;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        poseStack.translate(offsetX(arm), offsetY(arm), offsetZ(arm));
    }

    private void resetPos() {
        getSettings().forEach(setting -> {
            if (setting instanceof SliderSetting f) {
                f.setValue(0f);
            }
        });
    }
}
