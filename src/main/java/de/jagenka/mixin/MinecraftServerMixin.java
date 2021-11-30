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

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
    @Inject(method = "loadWorld", at = @At("HEAD"))
    private void serverLoaded(CallbackInfo ci)
    {
        HackfleischDiskursMod.onServerLoaded((MinecraftServer) (Object) this);
    }

    @Inject(method = "sendSystemMessage", at = @At("TAIL"))
    public void sendSystemMessage(Text message, UUID sender, CallbackInfo info)
    {
        DiscordBot.handleSystemMessages(message.getString());
    }
}
