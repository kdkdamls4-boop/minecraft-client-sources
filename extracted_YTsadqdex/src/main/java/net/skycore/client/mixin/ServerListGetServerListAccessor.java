package net.skycore.client.mixin;

import java.util.List;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerList.class)
public interface ServerListGetServerListAccessor {
   @Accessor("serverList")
   List<ServerData> skycore$getServerList();

   @Accessor("hiddenServerList")
   List<ServerData> skycore$getHiddenServerList();
}
