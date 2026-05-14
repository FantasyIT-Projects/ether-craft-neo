package studio.fantasyit.ether_craft.stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.entity.EtherStreamEntity;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamBreakBlockCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("block_breaker");
    private final List<ItemStack> tools = new ArrayList<>();

    public void addTool(ItemStack tool) {
        tools.add(tool.copy());
    }

    public boolean hasTools() {
        return !tools.isEmpty();
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int getConsumption() {
        return 0;
    }

    @Override
    public void tick(EtherStreamEntity streamEntity) {
    }

    @Override
    public boolean hitEntity(ServerLevel level, EtherStreamEntity streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, EtherStreamEntity streamEntity, BlockHitResult hit, BlockState blockState) {
        if (tools.isEmpty() || blockState.isAir()) return false;
        if (blockState.is(Tags.ETHER_MACHINE)) return false;
        if (blockState.getDestroySpeed(level, hit.getBlockPos()) < 0) return false;

        ItemStack bestTool = findBestTool(level, hit.getBlockPos(), blockState);
        if (bestTool.isEmpty()) return false;
        BlockPos pos = hit.getBlockPos();

        float hardness = blockState.getDestroySpeed(level, pos);
        int effLevel = bestTool.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY));
        int cost = Math.max(1, (int) (hardness * Config.breakBlockHardnessMultiplier) - effLevel * Config.breakBlockEfficiencyDivisor);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(blockState, level, pos, blockEntity, null, bestTool);
        level.removeBlock(pos, false);

        streamEntity.consumeEther(cost);

        Optional<IStreamCapability> optStorage = streamEntity.getCapability(EtherStreamStorageCapability.ID);
        if (optStorage.isPresent() && optStorage.get() instanceof EtherStreamStorageCapability storage) {
            SimpleContainer dropContainer = new SimpleContainer(drops.size());
            for (int i = 0; i < drops.size(); i++) {
                dropContainer.setItem(i, drops.get(i));
            }
            ContainerOps.tryPlaceToItemHandler(dropContainer, storage.handler);
            for (int i = 0; i < dropContainer.getContainerSize(); i++) {
                ItemStack remaining = dropContainer.getItem(i);
                if (!remaining.isEmpty()) {
                    Block.popResource(level, pos, remaining);
                }
            }
        } else {
            for (ItemStack drop : drops) {
                Block.popResource(level, pos, drop);
            }
        }
        return true;
    }

    private ItemStack findBestTool(ServerLevel level, BlockPos pos, BlockState state) {
        ItemStack best = ItemStack.EMPTY;
        float bestSpeed = 0;
        for (ItemStack tool : tools) {
            if (!tool.isEmpty() && tool.isCorrectToolForDrops(state)) {
                float speed = tool.getDestroySpeed(state);
                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    best = tool;
                }
            }
        }
        return best;
    }

    @Override
    public void onDestroy(EtherStreamEntity streamEntity) {
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("tools", ItemStack.OPTIONAL_CODEC.listOf(), tools);
    }

    @Override
    public void deserialize(ValueInput input) {
        tools.clear();
        input.read("tools", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(tools::addAll);
    }
}
