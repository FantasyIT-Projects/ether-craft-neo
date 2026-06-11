package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.Tags;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.util.ContainerOps;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamBreakBlockCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("block_breaker");

    public static final Codec<EtherStreamBreakBlockCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("tools").forGetter(c -> c.tools)
    ).apply(instance, tools -> {
        EtherStreamBreakBlockCapability cap = new EtherStreamBreakBlockCapability();
        cap.tools.addAll(tools);
        return cap;
    }));

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
    public void tick(@UnknownNullability IEtherStreamLike streamEntity) {
        if (streamEntity.level() instanceof ServerLevel level) {
            BlockState blockState = level.getBlockState(streamEntity.blockPosition());
            if (blockState.isAir()) return;
            if (isHoePrefer(blockState)) {
                BlockHitResult bh = new BlockHitResult(
                        streamEntity.position(),
                        streamEntity.getDirection().getOpposite(),
                        streamEntity.blockPosition(),
                        true
                );
                this.hitBlock(level, streamEntity, bh, blockState);
            }
        }
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        if (tools.isEmpty() || blockState.isAir()) return false;
        if (blockState.is(Tags.ETHER_MACHINE)) return false;
        if (blockState.getDestroySpeed(level, hit.getBlockPos()) < 0) return false;

        ItemStack bestTool = findBestTool(level, hit.getBlockPos(), blockState);
        if (bestTool.isEmpty()) return false;
        BlockPos pos = hit.getBlockPos();

        float hardness = blockState.getDestroySpeed(level, pos);
        //TODO 附魔
        int cost = Math.max(1, (int) (hardness * Config.etherStreamBreakBlockHardnessMultiplier) + Config.etherStreamBreakBlockConstantCost);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        List<ItemStack> drops = Block.getDrops(blockState, level, pos, blockEntity, null, bestTool);

        if (isHoe(bestTool) && hasAgeProperty(blockState)) {
            ItemStack seedItem = findReplantSeed(drops, blockState);
            if (!seedItem.isEmpty()) {
                seedItem.shrink(1);
                level.setBlock(pos, createReplantedState(blockState), 3);
            } else {
                level.removeBlock(pos, false);
            }
        } else if (isHoe(bestTool) && isBerry(blockState)) {
            BlockState blockState1 = blockState.setValue(SweetBerryBushBlock.AGE, 0);
            level.setBlockAndUpdate(pos, blockState1);
        } else if (isHoe(bestTool) && isGlowBerry(blockState)) {
            BlockState blockState1 = blockState.setValue(CaveVinesPlantBlock.BERRIES, false);
            level.setBlockAndUpdate(pos, blockState1);
        } else if (isHoe(bestTool) && isCocoa(blockState)) {
            BlockState blockState1 = blockState.setValue(CocoaBlock.AGE, 0);
            level.setBlockAndUpdate(pos, blockState1);
        } else {
            level.removeBlock(pos, false);
        }

        drops.removeIf(ItemStack::isEmpty);

        streamEntity.consumeEther(cost);

        Optional<IStreamCapability> optStorage = streamEntity.getCapability(EtherStreamStorageCapability.ID);
        if (optStorage.isPresent() && optStorage.get() instanceof
                EtherStreamStorageCapability storage) {
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

    private static boolean isHoe(ItemStack tool) {
        return tool.is(ItemTags.HOES);
    }


    private static boolean isHoePrefer(BlockState state) {
        return hasAgeProperty(state) || isBerry(state) || isGlowBerry(state) || isCocoa(state);
    }

    private static boolean hasAgeProperty(BlockState state) {
        if (state.getBlock() instanceof CropBlock cb) {
            return cb.getAge(state) == cb.getMaxAge();
        } else if (state.getBlock() instanceof SugarCaneBlock) {
            return true;
        }
        return false;
    }

    private static boolean isCocoa(BlockState state) {
        return state.getBlock() instanceof CocoaBlock && state.hasProperty(CocoaBlock.AGE) && state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
    }

    private static boolean isBerry(BlockState state) {
        return state.getBlock() instanceof SweetBerryBushBlock sb && state.hasProperty(SweetBerryBushBlock.AGE) && state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE;
    }

    private static boolean isGlowBerry(BlockState state) {
        return state.getBlock() instanceof CaveVines cvp && state.hasProperty(CaveVinesPlantBlock.BERRIES) && state.getValue(CaveVinesPlantBlock.BERRIES);
    }


    private static ItemStack findReplantSeed(List<ItemStack> drops, BlockState targetBlockState) {
        if (drops.size() <= 1)
            return ItemStack.EMPTY;
        Block targetBlock = targetBlockState.getBlock();
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && drop.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == targetBlock) {
                return drop;
            }
        }
        return ItemStack.EMPTY;
    }

    private static BlockState createReplantedState(BlockState originalState) {
        Block block = originalState.getBlock();
        if (block instanceof CropBlock cropBlock) {
            return cropBlock.getStateForAge(0);
        }
        return originalState;
    }

    private ItemStack findBestTool(ServerLevel level, BlockPos pos, BlockState state) {
        if (isHoePrefer(state)) {
            for (ItemStack tool : tools) {
                if (!tool.isEmpty() && isHoe(tool)) {
                    return tool;
                }
            }
        }
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
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
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
