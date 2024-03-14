package de.jagenka.mixin;

import de.jagenka.config.DiskordelLanguage;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Language.class)
public class LanguageMixin
{
    @Inject(method = "getInstance", at = @At("HEAD"), cancellable = true)
    private static void overrideLanguage(CallbackInfoReturnable<Language> cir)
    {
        cir.setReturnValue(DiskordelLanguage.INSTANCE);
        cir.cancel();
    }
}
