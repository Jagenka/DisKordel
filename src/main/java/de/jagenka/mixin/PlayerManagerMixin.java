package de.jagenka.mixin;

import de.jagenka.stats.PlayerStatManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.stat.ServerStatHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "createStatHandler", at = @At("RETURN"))
    void saveStatHandlerToCache(PlayerEntity player, CallbackInfoReturnable<ServerStatHandler> cir)
    {
        PlayerStatManager.INSTANCE.updateStatHandler(player.getUuid(), cir.getReturnValue());
    }
}
