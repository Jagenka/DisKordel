package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvancementType.class)
public abstract class AdvancementTypeMixin
{
    @Shadow
    @Final
    private String name;

    @Inject(method = "createAnnouncement", at = @At(value = "HEAD"))
    private void onAdvancementGet(AdvancementHolder advancementEntry, ServerPlayer player, CallbackInfoReturnable<MutableComponent> cir)
    {
        String advancementName = advancementEntry.value().name().orElseGet(() -> (Component.nullToEmpty(advancementEntry.id().toString()))).getString();

        MinecraftHandler.handleAdvancementGet(
                Component.translatable(
                        "chat.type.advancement." + this.name,
                        player.getName(),
                        advancementName
                )
        );
    }
}
