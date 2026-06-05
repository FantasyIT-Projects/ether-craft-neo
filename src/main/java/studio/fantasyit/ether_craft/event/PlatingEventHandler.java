package studio.fantasyit.ether_craft.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

import java.util.List;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class PlatingEventHandler {

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        for (ItemStack stack : getPlatedEquipment(player)) {
            List<PlatingData> data = PlatingUtil.getPlatingData(stack);
            for (PlatingData d : data) {
                IPlatingEffect effect = PlatingManager.getEffect(d.id());
                if (effect != null) {
                    effect.onHoldTick(d, stack, player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onUseItem(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;
        ItemStack stack = event.getItem();
        triggerPlating(stack, player, (effect, data, s, p) -> effect.onUse(data, s, p));
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        Player player = event.getEntity();
        ItemStack stack = player.getMainHandItem();
        triggerPlating(stack, player, (effect, data, s, p) -> {
            if (effect.onAttack(data, s, p, event.getTarget())) {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = player.getMainHandItem();
        triggerPlating(stack, player, (effect, data, s, p) -> {
            if (effect.onBreakBlock(data, s, p, event.getPos(), event.getState())) {
                event.setCanceled(true);
            }
        });
    }

    @SubscribeEvent
    public static void onUseOnBlock(UseItemOnBlockEvent event) {
        if (event.getLevel().isClientSide()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack stack = event.getItemStack();
        triggerPlating(stack, player, (effect, data, s, p) ->
                effect.onUseOnBlock(data, s, p, event.getPos(), event.getLevel().getBlockState(event.getPos())));
    }

    private static void triggerPlating(ItemStack stack, Player player, PlatingTrigger trigger) {
        List<PlatingData> data = PlatingUtil.getPlatingData(stack);
        if (data.isEmpty()) return;
        for (PlatingData d : data) {
            IPlatingEffect effect = PlatingManager.getEffect(d.id());
            if (effect != null) {
                trigger.apply(effect, d, stack, player);
            }
        }
    }

    private static ItemStack[] getPlatedEquipment(Player player) {
        return new ItemStack[]{
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS),
                player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET)
        };
    }

    @FunctionalInterface
    private interface PlatingTrigger {
        void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, Player player);
    }
}