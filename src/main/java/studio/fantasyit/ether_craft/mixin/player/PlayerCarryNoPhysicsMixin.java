package studio.fantasyit.ether_craft.mixin.player;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@Mixin(Player.class)
public abstract class PlayerCarryNoPhysicsMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isSpectator()Z", ordinal = 0))
    private boolean ether_craft$carriedByEtherStream(boolean original) {
        return original || ((Player) (Object) this).getData(AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM.get());
    }
}
