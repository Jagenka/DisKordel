package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin
{
    @Inject(method = "onDeath", at = @At("HEAD"))
    void handleDeathMessage(DamageSource damageSource, CallbackInfo ci)
    {
        MinecraftHandler.INSTANCE.handleDeathMessage((ServerPlayerEntity) (Object) this);
    }
}
