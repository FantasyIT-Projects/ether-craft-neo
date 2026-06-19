package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.emitter.EtherStreamEmitterBlock;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.glass.EtherGlassBlock;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, EtherCraft.MODID);
    public static final DeferredHolder<Block, @NotNull EtherProcessFactoryBlock> ETHER_PROCESS_FACTORY = BLOCKS.register("ether_process_factory", EtherProcessFactoryBlock::new);
    public static final DeferredHolder<Block, @NotNull EtherStreamEmitterBlock> ETHER_STREAM_EMITTER = BLOCKS.register("ether_stream_emitter", EtherStreamEmitterBlock::new);
    public static final DeferredHolder<Block, @NotNull EtherAdaptNodeBlock> ETHER_ADAPT_NODE = BLOCKS.register("ether_adapt_node", EtherAdaptNodeBlock.constructWithLevel(1));
    public static final DeferredHolder<Block, @NotNull EtherGlassBlock> ETHER_GLASS = BLOCKS.register("ether_glass", EtherGlassBlock::new);

    public static final DeferredHolder<Block, @NotNull Block> ETHER_BLOCK = BLOCKS.register("ether_block", r -> new Block(Block.Properties.of().strength(5f).requiresCorrectToolForDrops().sound(SoundType.METAL).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), r))));
    public static final DeferredHolder<Block, @NotNull Block> ETHER_ORE = BLOCKS.register("ether_ore", r -> new Block(Block.Properties.of().strength(3f).requiresCorrectToolForDrops().sound(SoundType.STONE).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), r))));
    public static final DeferredHolder<Block, @NotNull Block> DEEPSLATE_ETHER_ORE = BLOCKS.register("deepslate_ether_ore", r -> new Block(Block.Properties.of().strength(4.5f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), r))));
    public static final DeferredHolder<Block, @NotNull Block> NETHER_ETHER_ORE = BLOCKS.register("nether_ether_ore", r -> new Block(Block.Properties.of().strength(3f).requiresCorrectToolForDrops().sound(SoundType.NETHER_ORE).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), r))));
    public static final DeferredHolder<Block, @NotNull Block> INACTIVATED_ETHER_BLOCK = BLOCKS.register("inactivated_ether_block", r -> new Block(Block.Properties.of().strength(5f).requiresCorrectToolForDrops().sound(SoundType.STONE).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), r))));
    public static final DeferredHolder<Block, @NotNull Block> SMOOTH_INACTIVATED_ETHER_BLOCK = BLOCKS.register("smooth_inactivated_ether_block", r -> new Block(Block.Properties.of().strength(5f).requiresCorrectToolForDrops().sound(SoundType.STONE).setId(ResourceKey.create(BuiltInRegistries.BLOCK.key(), r))));

    public static void register(IEventBus modbus) {
        BLOCKS.register(modbus);
    }
}
