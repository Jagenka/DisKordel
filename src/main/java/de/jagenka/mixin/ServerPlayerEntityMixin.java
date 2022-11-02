package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "onDeath", at = @At("TAIL"))
    private void increaseDeathStat(DamageSource damageSource, CallbackInfo ci)
    {
        discordExecutor.submit(() -> MinecraftHandler.increaseDeathStat(((ServerPlayerEntity) (Object) this).getName().getString()));
    }
}
