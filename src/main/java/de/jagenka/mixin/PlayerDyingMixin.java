package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CombatTracker.class)
public class PlayerDyingMixin
{
    @Shadow
    @Final
    private LivingEntity mob;

    @Inject(method = "getDeathMessage", at = @At("RETURN"))
    private void getDeathMessage(CallbackInfoReturnable<Component> cir)
    {
        if (this.mob instanceof Player)
        {
            MinecraftHandler.handleDeathMessage(cir.getReturnValue().getString());
        }
    }
}
