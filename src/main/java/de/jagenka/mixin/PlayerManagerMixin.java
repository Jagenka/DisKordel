package de.jagenka.mixin;

import de.jagenka.DiscordBot;
import net.minecraft.network.MessageType;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.function.Function;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("HEAD"))
    public void broadcast(Text text, Function<ServerPlayerEntity, Text> playerMessageFactory, MessageType playerMessageType, UUID sender, CallbackInfo info)
    {
        DiscordBot.sendMessage(text.getString()); //get player and message separately
    }
}
