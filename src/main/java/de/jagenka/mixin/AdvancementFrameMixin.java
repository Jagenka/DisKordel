package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementFrame.class)
public abstract class AdvancementFrameMixin
{
    @Shadow
    @Final
    private String id;

    @Inject(method = "getChatAnnouncementText", at = @At(value = "HEAD"))
    private void onAdvancementGet(AdvancementEntry advancementEntry, ServerPlayerEntity player, CallbackInfoReturnable<MutableText> cir)
    {
        String advancementName = advancementEntry.value().name().orElseGet(() -> (Text.of(advancementEntry.id().toString()))).getString();

        MinecraftHandler.handleAdvancementGet(
                Text.translatable(
                        "chat.type.advancement." + this.id,
                        player.getName(),
                        advancementName
                )
        );
    }
}
