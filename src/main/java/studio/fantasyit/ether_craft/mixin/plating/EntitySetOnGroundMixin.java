package studio.fantasyit.ether_craft.mixin.plating;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
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
        if (movement.y > 0) return result;
        if (result.y > movement.y) {
            PlatingEventHelper.forEachPlatingOnEquipment(player, (a, b, c) -> {
                if (a instanceof IPlatingVirtualWalkableProvider vwp)
                    vwp.tickOnBlock(b, c, player.level(), player, blockPosition());
            });
            Vec3 fPos = position().add(movement);
            ether_craft$lastOnGroundPos = BlockPos.containing(fPos);
            return result;
        }
        double ey = getY();
        double[] provided = {result.y};
        PlatingEventHelper.forEachPlatingOnEquipment(player, (a, b, c) -> {
            if (a instanceof IPlatingVirtualWalkableProvider vwp) {
                int i = vwp.providerVirtualWalkableAt(b, c, player.level(), player, player.blockPosition().below(), ether_craft$lastOnGroundPos);
                double df = (i + 1) - ey;
                if (provided[0] < df) provided[0] = df;
            }
        });
        provided[0] = Math.min(0, provided[0]);
        return new Vec3(result.x, provided[0], result.z);
    }
}
