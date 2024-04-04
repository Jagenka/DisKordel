package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageTracker.class)
public class PlayerDyingMixin
{
    @Shadow
    @Final
    private LivingEntity entity;

    @Inject(method = "getDeathMessage", at = @At("RETURN"))
    private void getDeathMessage(CallbackInfoReturnable<Text> cir)
    {
        if (this.entity instanceof PlayerEntity)
        {
            MinecraftHandler.handleDeathMessage(cir.getReturnValue().getString());
        }
    }
}
