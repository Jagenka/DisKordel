package de.jagenka.mixin;

import net.minecraft.server.filter.TextStream;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//@Mixin(ServerPlayNetworkHandler.class)
//public class ServerPlayNetworkHandlerMixin
//{
//    @Inject(method = "handleMessage(Lnet/minecraft/server/filter/TextStream.Message;)V", at = @At("HEAD"))
//    public void interceptMessage(TextStream.Message message, CallbackInfo info)
//    {
//
//    }
//}