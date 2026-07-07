package studio.fantasyit.ether_craft.mixin.plating;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {

    @Inject(method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"), cancellable = true)
    private void ether_craft$absorbDurabilityWithEther(int amount, ServerLevel level, LivingEntity player, Consumer<Item> onBreak, CallbackInfo ci) {
        ItemStack self = (ItemStack) (Object) this;
        if (!PlatingUtil.hasPlating(self)) return;
        int ether = PlatingUtil.getEther(self);
        if (ether <= 0) return;

        int etherCost = amount * Config.platingDurabilityAbsorptionEtherPerDurability;
        if (ether >= etherCost) {
            PlatingUtil.extractEtherWithEntityContext(player, self, etherCost);
            ci.cancel();
        } else {
            PlatingUtil.extractEtherWithEntityContext(player, self, ether);
        }
    }
}
