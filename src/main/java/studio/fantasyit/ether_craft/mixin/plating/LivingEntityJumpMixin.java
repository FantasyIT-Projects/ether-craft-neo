package studio.fantasyit.ether_craft.mixin.plating;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.fantasyit.ether_craft.plating.event.PlatingEventHandler;

@Mixin(LivingEntity.class)
public abstract class LivingEntityJumpMixin {

    @Invoker("getJumpPower")
    public abstract float invokeGetJumpPower();

    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void ether_craft$onJumpFromGround(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        if (player.onGround()) return;

        if (PlatingEventHandler.tryJump(player)) {
            self.setDeltaMovement(self.getDeltaMovement().add(0, this.invokeGetJumpPower(), 0));
            self.needsSync = true;
            ci.cancel();
        }
    }
}
