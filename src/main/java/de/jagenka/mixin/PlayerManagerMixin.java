package de.jagenka.mixin;

import com.mojang.authlib.GameProfile;
import de.jagenka.PlayerStatManager;
import de.jagenka.UserRegistry;
import net.minecraft.class_8792;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.ServerStatHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "createStatHandler", at = @At("RETURN"))
    void saveStatHandlerToCache(PlayerEntity player, CallbackInfoReturnable<ServerStatHandler> cir)
    {
        PlayerStatManager.INSTANCE.updateStatHandler(player.getUuid(), cir.getReturnValue());
    }

    @Inject(method = "onPlayerConnect", at = @At("HEAD"))
    void saveNewPlayersProfileToCache(ClientConnection connection, ServerPlayerEntity player, class_8792 arg, CallbackInfo ci)
    {
        GameProfile profile = player.getGameProfile();

        discordExecutor.submit(() ->
        {
            assert profile != null;
            UserRegistry.INSTANCE.saveToCache(profile);
        });
    }
}
