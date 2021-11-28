package de.jagenka.mixin;

import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatMessageC2SPacket.class)
public class ChatMessageMixin
{
    @Shadow
    @Final
    private String chatMessage;

    @Inject(method = "getChatMessage()Ljava/lang/String;", at = @At("RETURN"))
    void getChatMessage(CallbackInfoReturnable<String> info)
    {
        System.out.println(this.chatMessage);
    }
}
