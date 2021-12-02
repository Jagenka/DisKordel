package de.jagenka.mixin;

import de.jagenka.DiscordBot;
import de.jagenka.HackfleischDiskursMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci)
    {
        HackfleischDiskursMod.onServerLoaded((MinecraftServer) (Object) this);
    }

    @Inject(method = "sendSystemMessage", at = @At("TAIL"))
    public void sendSystemMessage(Text message, UUID sender, CallbackInfo info)
    {
        discordExecutor.submit(() -> DiscordBot.handleSystemMessages(message.getString(), sender));
    }
}
