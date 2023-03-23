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

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin
{
    @Shadow
    @Nullable GameProfile profile;

    @Inject(method = "addToServer", at = @At("HEAD"))
    void saveProfileToCache(ServerPlayerEntity player, CallbackInfo ci)
    {
        assert this.profile != null;
        UserRegistry.INSTANCE.saveToCache(this.profile);
    }
}
