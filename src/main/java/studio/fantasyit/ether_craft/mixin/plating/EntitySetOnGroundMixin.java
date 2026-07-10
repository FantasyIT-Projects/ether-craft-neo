package studio.fantasyit.ether_craft.mixin.plating;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import studio.fantasyit.ether_craft.plating.client.CoyoteTimeAudioPlayer;
import studio.fantasyit.ether_craft.plating.effects.CoyoteTimePlatingEffect;
import studio.fantasyit.ether_craft.plating.helper.PlatingEventHelper;
import studio.fantasyit.ether_craft.plating.trigger.IPlatingVirtualWalkableProvider;

@Mixin(Entity.class)
public abstract class EntitySetOnGroundMixin {
    @Shadow
    private boolean onGround;

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract BlockPos blockPosition();

    @Shadow
    public abstract Vec3 position();

    @Unique
    private BlockPos ether_craft$lastOnGroundPos = null;

    @ModifyReturnValue(method = "collide", at = @At(value = "RETURN"))
    private Vec3 ether_craft$virtual_ground_supplier(Vec3 result, Vec3 movement) {
        if (!(((Object) this) instanceof Player player)) return result;
        if (player.getAbilities().flying) return result;
        if (movement.y >= 0) return result;
        if (result.y > movement.y) {
            PlatingEventHelper.forEachPlatingOnEquipment(player, (a, b, c) -> {
                if (a instanceof IPlatingVirtualWalkableProvider vwp)
                    vwp.tickOnBlock(b, c, player.level(), player, blockPosition());
            });
            Vec3 fPos = position().add(movement);
            ether_craft$lastOnGroundPos = BlockPos.containing(fPos);
            if (player.level().isClientSide()) {
                CoyoteTimeAudioPlayer.stop(player);
            }
            return result;
        }
        double ey = getY();
        double[] provided = {result.y};
        boolean[] coyoteActive = {false};
        PlatingEventHelper.forEachPlatingOnEquipment(player, (a, b, c) -> {
            if (a instanceof IPlatingVirtualWalkableProvider vwp) {
                int i = vwp.providerVirtualWalkableAt(b, c, player.level(), player, player.blockPosition().below(), ether_craft$lastOnGroundPos, movement);
                double df = (i + 1) - ey;
                if (provided[0] < df) provided[0] = df;
                if (a instanceof CoyoteTimePlatingEffect && i != Integer.MIN_VALUE) {
                    coyoteActive[0] = true;
                }
            }
        });
        provided[0] = Math.min(0, provided[0]);
        if (player.level().isClientSide()) {
            if (coyoteActive[0]) {
                CoyoteTimeAudioPlayer.start(player);
            } else {
                CoyoteTimeAudioPlayer.stop(player);
            }
        }
        return new Vec3(result.x, provided[0], result.z);
    }
}
