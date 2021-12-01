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

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin
{
//    @Inject(method = "sendMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/MessageType;Ljava/util/UUID;)V", at = @At("TAIL"))
//    public void sendMessage(Text message, MessageType type, UUID sender, CallbackInfo info)
//    {
//        System.out.println(type + ": " + message);
//    }

    @Inject(method = "onDeath", at = @At("HEAD"))
    public void onDeath(DamageSource source, CallbackInfo info)
    {
        //game rule ignored
        LivingEntity livingEntity = (LivingEntity) (Object) this;
        Text deathMessage = livingEntity.getDamageTracker().getDeathMessage();
        DiscordBot.handleDeathMessages(deathMessage.getString());
    }
}
