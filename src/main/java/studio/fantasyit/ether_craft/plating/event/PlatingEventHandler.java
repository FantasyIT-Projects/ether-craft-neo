package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.trigger.*;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class PlatingEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingHoldTickTrigger holdTick) {
                holdTick.onHoldTick(data, stack, player);
            }
        });
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        for (ItemStack stack : PlatingEventHelper.getPlatedEquipment(player)) {
            PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
                if (effect instanceof IPlatingRightClickTrigger) {
                    event.setCanceled(true);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingUseTrigger use) {
                use.onUse(data, s, p);
            }
        });
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingAttackTrigger attack) {
                if (attack.onAttack(data, s, p, event.getTarget())) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingBreakBlockTrigger breakBlock) {
                if (breakBlock.onBreakBlock(data, s, p, event.getPos(), event.getState())) {
                    event.setCanceled(true);
                }
            }
        });
    }

    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = event.getItemStack();
        PlatingEventHelper.forEachPlating(stack, player, (effect, data, s, p) -> {
            if (effect instanceof IPlatingUseOnBlockTrigger useOnBlock) {
                useOnBlock.onUseOnBlock(data, s, p, event.getPos(), event.getLevel().getBlockState(event.getPos()));
            }
        });
    }

    @SubscribeEvent
    public static void onArrowShot(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player player)) return;
        if (!(player.level() instanceof ServerLevel)) return;

        ItemStack held = player.getMainHandItem();
        if (!(held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem)) {
            held = player.getOffhandItem();
            if (!(held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem)) return;
        }

        PlatingEventHelper.forEachPlating(held, player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingArrowShotTrigger arrowShot) {
                arrowShot.onArrowShot(data, stack, player, arrow);
            }
        });
    }

    public static boolean tryJump(Player player) {
        if (player.level().isClientSide()) return false;

        boolean[] result = {false};
        PlatingEventHelper.forEachPlatingOnEquipment(player, (effect, data, stack, p) -> {
            if (effect instanceof IPlatingJumpTrigger jump) {
                if (jump.canJump(data, stack, player)) {
                    result[0] = true;
                }
            }
        });
        return result[0];
    }
}
