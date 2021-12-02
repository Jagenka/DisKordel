package de.jagenka.mixin;

import de.jagenka.DiscordBot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onDeath(DamageSource source, CallbackInfo info)
    {
        discordExecutor.submit(() ->
        {
            //game rule ignored
            LivingEntity livingEntity = (LivingEntity) (Object) this;
            Text deathMessage = livingEntity.getDamageTracker().getDeathMessage();
            DiscordBot.handleDeathMessages(deathMessage.getString());
        });
    }
}
