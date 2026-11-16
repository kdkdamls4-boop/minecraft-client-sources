package tokyo.ghoul.inject.accessor;

import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("user")
    @Mutable
    void setUser(User user);

    @Accessor("profileFuture")
    @Mutable
    void setProfileFuture(CompletableFuture<ProfileResult> profileFuture);

    @Accessor("rightClickDelay")
    void setRightClickDelay(int delay);

    @Invoker("updateLevelInEngines")
    void ghoul$updateLevelInEngines(ClientLevel level, boolean stopSound);
}
