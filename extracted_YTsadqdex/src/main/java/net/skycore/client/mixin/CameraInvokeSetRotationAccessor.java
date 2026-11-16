package net.skycore.client.mixin;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraInvokeSetRotationAccessor {
   @Invoker("setRotation")
   void skycore$invokeSetRotation(float value, float value2);
}
