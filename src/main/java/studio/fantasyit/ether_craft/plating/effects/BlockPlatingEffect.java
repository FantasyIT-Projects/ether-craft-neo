package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.inst.IInstanceTrigger;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingBlockingTrigger;
import studio.fantasyit.ether_craft.plating.trigger.IWithoutContextPlayerTicking;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.List;
import java.util.Optional;

public class BlockPlatingEffect implements IPlatingEffect, IInstanceTrigger, IWithoutContextPlayerTicking, IPlatingBlockingTrigger {
    public static final Identifier ID = EtherCraft.id("block");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onPlatted(PlatingData data, ItemStack stack) {
        stack.set(DataComponentRegistry.TEMP_BLOCKING, true);
        stack.set(DataComponents.BLOCKS_ATTACKS, new BlocksAttacks(
                0.0f,
                1.0f,
                List.of(new BlocksAttacks.DamageReduction(
                        90f,
                        Optional.empty(),
                        0f,
                        (float) Config.platingBlockDamageReduction
                )),
                BlocksAttacks.ItemDamageFunction.DEFAULT,
                Optional.empty(),
                Optional.of(SoundEvents.SHIELD_BLOCK),
                Optional.of(SoundEvents.SHIELD_BREAK)
        ));
    }

    private static void removeBlocking(ItemStack stack) {
        stack.remove(DataComponentRegistry.TEMP_BLOCKING);
        stack.remove(DataComponents.BLOCKS_ATTACKS);
    }

    @Override
    public void tickPlayer(LivingEntity entity) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (stack.has(DataComponents.BLOCKS_ATTACKS) && stack.has(DataComponentRegistry.TEMP_BLOCKING)) {
                if (!PlatingUtil.hasPlating(stack, ID)) {
                    removeBlocking(stack);
                }
            }
        }
    }

    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, LivingShieldBlockEvent event) {
        if (!event.getBlocked()) return;
        PlatingUtil.extractEther(stack, Config.platingBlockEtherPerTick);
        if (!PlatingUtil.canExtractEther(stack, Config.platingBlockEtherPerTick)) {
            removeBlocking(stack);
        }
    }
}
