package de.jagenka.mixin;

import com.mojang.authlib.GameProfile;
import de.jagenka.UserRegistry;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin
{
    @Shadow
    @Nullable GameProfile profile;

    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "addToServer", at = @At("HEAD"))
    void saveProfileToCache(ServerPlayerEntity player, CallbackInfo ci)
    {
        discordExecutor.submit(() ->
        {
            assert this.profile != null;
            UserRegistry.INSTANCE.saveToCache(this.profile);
        });
    }
}
