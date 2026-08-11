package studio.fantasyit.ether_craft.mixin.player;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@Mixin(AbstractClientPlayer.class)
public abstract class ClientPlayerCarryNoBobMixin {
    @ModifyArg(method = "updateBob", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarState;updateBob(F)V"))
    private float ether_craft$carriedByEtherStream(float tBob) {
        if (((Player) (Object) this).getData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM.get()))
            return 0;
        return tBob;
    }
}
