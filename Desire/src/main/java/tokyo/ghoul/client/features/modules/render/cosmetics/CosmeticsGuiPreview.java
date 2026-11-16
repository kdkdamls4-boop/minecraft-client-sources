package tokyo.ghoul.client.features.modules.render.cosmetics;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.client.ui.clickgui.ClickGuiTab;
import tokyo.ghoul.client.ui.clickgui.ClickGui;

import java.util.IdentityHashMap;

public final class CosmeticsGuiPreview {
    public static final int PREVIEW = -1;

    private static final IdentityHashMap<Object, Integer> BOUND = new IdentityHashMap<>();
    private static final IdentityHashMap<Object, String> FORCED_MODELS = new IdentityHashMap<>();
    private static Integer pending;
    private static String pendingForceModel;

    private CosmeticsGuiPreview() {
    }

    public static void clearTiles() {
        BOUND.clear();
        FORCED_MODELS.clear();
        pending = null;
        pendingForceModel = null;
    }

    public static void pendingTile(int entryIndex) {
        pending = entryIndex >= 0 ? entryIndex : null;
        pendingForceModel = null;
    }

    public static void pendingPreview() {
        pending = PREVIEW;
        pendingForceModel = null;
    }

    public static void pendingModelPreview(String modelName) {
        pending = PREVIEW;
        pendingForceModel = modelName;
    }

    public static void clearPending() {
        pending = null;
        pendingForceModel = null;
    }

    public static void bindIfPending(Object state) {
        if (pending == null || state == null) return;
        BOUND.put(state, pending);
        if (pendingForceModel != null) {
            FORCED_MODELS.put(state, pendingForceModel);
            stripAccessories(state);
        } else if (pending == PREVIEW && CosmeticsModule.getInstance().customModels.getValue()) {
            stripAccessories(state);
        }
        pending = null;
        pendingForceModel = null;
    }

    private static void stripAccessories(Object state) {
        if (!(state instanceof AvatarRenderState avatar)) return;
        avatar.rightHandItemState.clear();
        avatar.leftHandItemState.clear();
        avatar.heldOnHead.clear();
        avatar.rightHandItemStack = ItemStack.EMPTY;
        avatar.leftHandItemStack = ItemStack.EMPTY;
        avatar.headEquipment = ItemStack.EMPTY;
        avatar.chestEquipment = ItemStack.EMPTY;
        avatar.legsEquipment = ItemStack.EMPTY;
        avatar.feetEquipment = ItemStack.EMPTY;
        avatar.showCape = false;
        avatar.parrotOnLeftShoulder = null;
        avatar.parrotOnRightShoulder = null;
        avatar.arrowCount = 0;
        avatar.stingerCount = 0;
        avatar.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        avatar.leftArmPose = HumanoidModel.ArmPose.EMPTY;
    }

    public static Integer peekBound(Object state) {
        return state == null ? null : BOUND.get(state);
    }

    public static String forcedModelFor(Object state) {
        return state == null ? null : FORCED_MODELS.get(state);
    }

    public static int[] resolveEntries(CosmeticsModule cosmetics) {
        int[] selected = cosmetics.selected();
        if (cosmetics.customModels.getValue()) {
            java.util.Arrays.fill(selected, -1);
        }
        return selected;
    }

    public static boolean shouldDrawCosmetics(CosmeticsModule cosmetics) {
        if (cosmetics.customModels.getValue()) return false;
        if (cosmetics.isEnabled()) return true;
        ClickGui gui = ClickGui.current();
        return gui != null && gui.isOpen() && gui.getTab() == ClickGuiTab.COSMETICS;
    }
}
