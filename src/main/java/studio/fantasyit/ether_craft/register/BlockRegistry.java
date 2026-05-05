package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.node.EtherStreamEmitterBlock;

public class BlockRegistry {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, EtherCraft.MODID);
    public static final DeferredHolder<Block, @NotNull EtherProcessFactoryBlock> ETHER_PROCESS_FACTORY = BLOCKS.register("ether_process_factory", EtherProcessFactoryBlock::new);
    public static final DeferredHolder<Block, @NotNull EtherStreamEmitterBlock> ETHER_STREAM_EMITTER = BLOCKS.register("ether_stream_emitter", EtherStreamEmitterBlock::new);

    public static void register(IEventBus modbus) {
        BLOCKS.register(modbus);
    }
}
