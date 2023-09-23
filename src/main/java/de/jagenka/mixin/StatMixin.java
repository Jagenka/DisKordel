package de.jagenka.mixin;

import de.jagenka.Util;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatFormatter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Stat.class)
public class StatMixin
{
    @Shadow
    @Final
    public StatFormatter formatter;

    @Inject(method = "format", at = @At("HEAD"), cancellable = true)
    private void formatTimeDifferently(int value, CallbackInfoReturnable<String> cir)
    {
        if (formatter == StatFormatter.TIME)
        {
            cir.setReturnValue(Util.INSTANCE.ticksToPrettyString(value));
            cir.cancel();
        }
    }
}
