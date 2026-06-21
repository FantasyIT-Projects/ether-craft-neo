package studio.fantasyit.ether_craft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.List;

public class ItemWrench extends Item {
    public ItemWrench(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        if (!player.isShiftKeyDown())
            return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        if (!state.is(Tags.ETHER_WRENCHABLE))
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            ServerLevel serverLevel = (ServerLevel) level;
            BlockEntity be = level.getBlockEntity(pos);
            List<ItemStack> drops = Block.getDrops(state, serverLevel, pos, be);

            level.destroyBlock(pos, false);

            for (ItemStack drop : drops) {
                if (!player.getInventory().add(drop)) {
                    player.drop(drop, false);
                }
            }

            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }
}
