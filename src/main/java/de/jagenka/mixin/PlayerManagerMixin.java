package de.jagenka.mixin;

import de.jagenka.MinecraftHandler;
import de.jagenka.config.Config;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void saveUUIDOnPlayerConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci)
    {
        discordExecutor.submit(() ->
        {
            Config.INSTANCE.storeUUIDForPlayerName(player.getName().getString(), player.getUuid());
        });
    }

    @Inject(method = "broadcast(Lnet/minecraft/network/message/SignedMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/network/ServerPlayerEntity;Lnet/minecraft/network/message/MessageType$Parameters;)V", at = @At("TAIL"))
    private void onChatMessage(SignedMessage message, Predicate<ServerPlayerEntity> shouldSendFiltered, ServerPlayerEntity sender, MessageType.Parameters params, CallbackInfo ci)
    {
        discordExecutor.submit(() -> MinecraftHandler.handleMinecraftChatMessage(message.getContent(), sender));
        //System.out.println("chatmsg");
    }

    @Inject(method = "broadcast(Lnet/minecraft/text/Text;Ljava/util/function/Function;Z)V", at = @At("TAIL"))
    private void onSystemMessage(Text message, Function<ServerPlayerEntity, Text> playerMessageFactory, boolean overlay, CallbackInfo ci)
    {
        discordExecutor.submit(() -> MinecraftHandler.handleMinecraftSystemMessage(message));
        //System.out.println("sysmsg");
    }
}
