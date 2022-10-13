package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci)
    {
        MinecraftHandler.onServerLoaded((MinecraftServer) (Object) this);
    }
}
