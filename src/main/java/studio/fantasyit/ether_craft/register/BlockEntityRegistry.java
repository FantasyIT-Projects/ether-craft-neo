package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.block.node.EtherStreamEmitterEntity;

public class BlockEntityRegistry {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES_TYPE = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, EtherCraft.MODID);
    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull EtherProcessFactoryEntity>> ETHER_PROCESS_FACTORY_ENTITY = BLOCK_ENTITIES_TYPE.register("ether_process_factory", () -> new BlockEntityType<>(EtherProcessFactoryEntity::new, BlockRegistry.ETHER_PROCESS_FACTORY.get()));
    public static final DeferredHolder<BlockEntityType<?>, @NotNull BlockEntityType<@NotNull EtherStreamEmitterEntity>> ETHER_NODE_ENTITY = BLOCK_ENTITIES_TYPE.register("ether_node", () -> new BlockEntityType<>(EtherStreamEmitterEntity::new, BlockRegistry.ETHER_STREAM_EMITTER.get()));
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES_TYPE.register(eventBus);
    }
}
