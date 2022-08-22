package de.jagenka.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin
{
//    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("TAIL"))
//    public void sendMessage(Text message, MessageType type, UUID sender, CallbackInfo info)
//    {
//        System.out.println(type + ": " + message);
//    }

    ExecutorService discordExecutor = Executors.newSingleThreadExecutor();
}
