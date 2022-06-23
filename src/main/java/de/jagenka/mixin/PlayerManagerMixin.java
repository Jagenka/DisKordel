package de.jagenka.mixin;

import de.jagenka.DiscordBot;
import net.minecraft.network.message.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.text.Text;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Lnet/minecraft/util/registry/RegistryKey;)V", at = @At("TAIL"))
    private void broadcast(Text message, RegistryKey<MessageType> typeKey, CallbackInfo ci)
    {
        discordExecutor.submit(() -> DiscordBot.handleSystemMessages(message));
    }
}
