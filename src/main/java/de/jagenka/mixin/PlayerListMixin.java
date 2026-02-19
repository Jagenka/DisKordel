package de.jagenka.mixin;

import de.jagenka.stats.PlayerStatManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(PlayerList.class)
public class PlayerListMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();
    @Shadow
    @Final
    private MinecraftServer server;

    @Inject(method = "getPlayerStats", at = @At("RETURN"))
    void saveStatHandlerToCache(Player player, CallbackInfoReturnable<ServerStatsCounter> cir)
    {
        PlayerStatManager.INSTANCE.updateStatHandler(player.getUUID(), cir.getReturnValue());
    }
}
