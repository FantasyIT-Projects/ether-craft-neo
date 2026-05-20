package studio.fantasyit.ether_craft.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.ItemRegistry;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.List;

public class WrenchEventHandler {

    public static void register() {
        NeoForge.EVENT_BUS.register(WrenchEventHandler.class);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack heldItem = event.getItemStack();
        if (!heldItem.is(ItemRegistry.WRENCH))
            return;

        Player player = event.getEntity();
        if (!player.isShiftKeyDown())
            return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(Tags.ETHER_WRENCHABLE))
            return;

        event.setUseBlock(TriState.FALSE);
        event.setUseItem(TriState.FALSE);
        if (!level.isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
            ServerLevel serverLevel = (ServerLevel) level;
            BlockEntity be = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, be);

            level.destroyBlock(pos, false);

            for (ItemStack drop : drops) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }
        }else{
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
