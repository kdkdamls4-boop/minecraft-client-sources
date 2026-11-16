package tokyo.ghoul.client.account;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import tokyo.ghoul.client.ui.menu.MenuConfigStore;
import tokyo.ghoul.inject.accessor.MinecraftAccessor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class AccountSwitcher {
    private static boolean startupApplied;

    private AccountSwitcher() {
    }

    public static void applyStartupAccount() {
        if (startupApplied) {
            return;
        }
        startupApplied = true;
        String name = MenuConfigStore.getString("selectedAccount", "");
        if (name.isEmpty() || !name.matches("^[a-zA-Z0-9_]{3,16}$")) {
            return;
        }
        switchTo(name);
    }

    public static void switchTo(String name) {
        Minecraft mc = Minecraft.getInstance();
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        User user = new User(name, uuid, "0", Optional.empty(), Optional.empty());

        MinecraftAccessor accessor = (MinecraftAccessor) mc;
        accessor.setUser(user);
        accessor.setProfileFuture(CompletableFuture.completedFuture(
                new ProfileResult(new GameProfile(uuid, name))));
        mc.updateTitle();
    }
}
