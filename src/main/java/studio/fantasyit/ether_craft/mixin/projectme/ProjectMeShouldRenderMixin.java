package studio.fantasyit.ether_craft.mixin.projectme;

import cn.zbx1425.projectme.ProjectMe;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import studio.fantasyit.ether_craft.plating.data.CamouflageState;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import static studio.fantasyit.ether_craft.register.AttachmentDataRegistry.TAKEN_BY_ETHER_STREAM;

@Mixin(ProjectMe.class)
public class ProjectMeShouldRenderMixin {
    @Inject(method = "computePlayerVisibility", at = @At("RETURN"), cancellable = true, require = 0)
    private static void shouldRender(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (player.hasData(TAKEN_BY_ETHER_STREAM) && player.getData(TAKEN_BY_ETHER_STREAM)) {
            cir.setReturnValue(false);
        } else {
            CamouflageState data = player.getExistingData(AttachmentDataRegistry.CAMOUFLAGE_STATE).orElse(null);
            if (data != null && data.isActive())
                cir.setReturnValue(false);
        }
    }
}
