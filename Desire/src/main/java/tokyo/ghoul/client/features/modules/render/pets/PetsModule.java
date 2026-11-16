package tokyo.ghoul.client.features.modules.render.pets;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.system.backend.Configurable;

public class PetsModule extends Configurable {
    @Getter private static final PetsModule instance = new PetsModule();

    private static final Identifier TEXTURE_TAKSA = Identifier.fromNamespaceAndPath("desire", "textures/entity/richidog/taksa.png");
    private static final Identifier TEXTURE_DJ = Identifier.fromNamespaceAndPath("desire", "textures/entity/richidog/djekrussel.png");
    private static final Identifier TEXTURE_DRAGON = Identifier.fromNamespaceAndPath("desire", "textures/entity/richidog/dragon.png");
    private static final Identifier TEXTURE_HAT = Identifier.fromNamespaceAndPath("desire", "textures/entity/richidog/propeller_hat.png");
    private static final String DRAGON = "Dragon";

    private final ModeSetting variant = new ModeSetting("Стиль").value("Dachshund").values("Dachshund", "DJ", DRAGON);
    private final BooleanSetting hat = new BooleanSetting("Шляпа").value(true);

    private final PetDogController brain = new PetDogController();
    private final PetDragonController dragonBrain = new PetDragonController();
    private PetDogModel model;
    private PetDragonModel dragonModel;
    private PropellerHatModel hatModel;

    public PetsModule() {
        addSettings(variant, hat);
    }

    public void reset() {
        brain.reset();
        dragonBrain.reset();
    }

    @Override
    public void onEvent() {
        EventListener tick = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            if (isDragonSelected()) {
                dragonBrain.setEntity(mc.player);
                dragonBrain.onUpdate();
                return;
            }
            brain.setEntity(mc.player);
            brain.onUpdate();
        }));

        EventListener input = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || event.action() == GLFW.GLFW_RELEASE) return;
            if (!event.mouse() || (event.key() != 0 && event.key() != 1)) return;
            if (isDragonSelected()) return;
            if (brain.isTrickActive() || !isLookingAtPet()) return;
            brain.startTrick();
        }));

        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null || mc.level == null || event.cameraState() == null) return;
            ensureModels();
            Vec3 petPos = isDragonSelected() ? dragonBrain.getPos() : brain.getPos();
            if (petPos == null) return;
            Vec3 cameraPos = event.cameraState().pos;
            float tickDelta = event.partialTicks();
            float animationTick = mc.player.tickCount + tickDelta;
            PoseStack poseStack = event.matrixStack();
            if (isDragonSelected()) {
                dragonModel.setRotationAngles(animationTick, dragonBrain);
                poseStack.pushPose();
                poseStack.translate(petPos.x - cameraPos.x, petPos.y - cameraPos.y, petPos.z - cameraPos.z);
                event.collector().submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(TEXTURE_DRAGON), (pose, consumer) -> {
                    PoseStack tmp = new PoseStack();
                    tmp.last().pose().set(pose.pose());
                    tmp.last().normal().set(pose.normal());
                    dragonModel.render(tmp, consumer, TEXTURE_DRAGON, dragonBrain, animationTick);
                });
                poseStack.popPose();
                return;
            }
            model.setRotationAngles(animationTick, brain);
            poseStack.pushPose();
            poseStack.translate(petPos.x - cameraPos.x, petPos.y + brain.getTrickYOffset() - cameraPos.y, petPos.z - cameraPos.z);
            Identifier tex = variant.is("DJ") ? TEXTURE_DJ : TEXTURE_TAKSA;
            event.collector().submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(tex), (pose, consumer) -> {
                PoseStack tmp = new PoseStack();
                tmp.last().pose().set(pose.pose());
                tmp.last().normal().set(pose.normal());
                model.render(tmp, consumer, tex, brain, animationTick);
                if (hat.getValue()) {
                    model.renderHat(tmp, consumer, TEXTURE_HAT, brain, hatModel, animationTick);
                }
            });
            poseStack.popPose();
        }));

        addEvents(tick, input, render);
    }

    private void ensureModels() {
        if (model == null) model = new PetDogModel(PetDogModel.getTexturedModelData().bakeRoot());
        if (dragonModel == null) dragonModel = new PetDragonModel(PetDragonModel.getTexturedModelData().bakeRoot());
        if (hatModel == null) hatModel = new PropellerHatModel(PropellerHatModel.getTexturedModelData().bakeRoot());
    }

    private boolean isLookingAtPet() {
        Vec3 petPos = isDragonSelected() ? dragonBrain.getPos() : brain.getPos();
        if (petPos == null || Double.isNaN(petPos.x) || Double.isNaN(petPos.y) || Double.isNaN(petPos.z)) return false;
        Vec3 eyePos = mc.player.position().add(0.0, mc.player.getEyeHeight(), 0.0);
        Vec3 look = mc.player.getLookAngle().normalize();
        Vec3 center = petPos.add(0.0, isDragonSelected() ? 1.35 : 0.75 + brain.getTrickYOffset(), 0.0);
        double along = center.subtract(eyePos).dot(look);
        if (along < 0.0 || along > 5.0) return false;
        Vec3 closest = eyePos.add(look.scale(along));
        double radius = 0.7 + Mth.clamp((float) (along / 5.0), 0.0f, 1.0f) * 0.25;
        return closest.distanceToSqr(center) <= radius * radius;
    }

    private boolean isDragonSelected() {
        return variant.is(DRAGON);
    }
}
