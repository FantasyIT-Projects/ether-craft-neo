package studio.fantasyit.ether_craft.register;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;

import java.util.function.Supplier;

public class BlockRegistry {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, EtherCraft.MODID);
    public static final DeferredHolder<Block, @NotNull EtherProcessFactoryBlock> ETHER_PROCESS_FACTORY = BLOCKS.register("ether_process_factory", EtherProcessFactoryBlock::new);

    public static void register(IEventBus modbus) {
        BLOCKS.register(modbus);
    }
}
