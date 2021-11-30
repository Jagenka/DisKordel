package de.jagenka.mixin;

import de.jagenka.DiscordBot;
import net.minecraft.network.ClientConnection;
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
        String message = text.getString();
        String[] split = message.split(">");
        DiscordBot.sendMessageFromMinecraft(split[0].substring(1), split[1].trim());
    }

    @Inject(method = "onPlayerConnect(Lnet/minecraft/network/ClientConnection;Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("TAIL"))
    public void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo info)
    {
        DiscordBot.onPlayerLogin(player.getDisplayName().asString());
    }

    @Inject(method = "remove(Lnet/minecraft/server/network/ServerPlayerEntity;)V", at = @At("TAIL"))
    public void onPlayerRemove(ServerPlayerEntity player, CallbackInfo info)
    {
        DiscordBot.onPlayerLeave(player.getDisplayName().asString());
    }
}
