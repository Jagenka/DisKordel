package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin
{
    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    void handleLogoutMessage(Text reason, CallbackInfo ci)
    {
        MinecraftHandler.INSTANCE.handleLogoutMessage(player);
    }
}
