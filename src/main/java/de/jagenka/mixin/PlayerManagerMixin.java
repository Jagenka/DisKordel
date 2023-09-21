package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import de.jagenka.PlayerStatManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "createStatHandler", at = @At("RETURN"))
    void saveStatHandlerToCache(PlayerEntity player, CallbackInfoReturnable<ServerStatHandler> cir)
    {
        PlayerStatManager.INSTANCE.updateStatHandler(player.getUuid(), cir.getReturnValue());
    }

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    void handleLoginMessage(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci)
    {
        MinecraftHandler.INSTANCE.handleLoginMessage(player, server);
    }
}
