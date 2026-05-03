package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.item.ProcessChipItem;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, EtherCraft.MODID);

    public static <T extends BlockItem> DeferredHolder<Item, @NotNull Item> block(DeferredHolder<Block,? extends Block> block){
        return ITEMS.register(block.getId().getPath(), (r) -> new BlockItem(block.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM,r))));
    }

    public static final DeferredHolder<Item, @NotNull ProcessChipItem> PROCESS_CHIP_ITEM = ITEMS.register("process_chip", ProcessChipItem::new);
    public static final DeferredHolder<Item, @NotNull ProcessChipItem> DIRECT_INPUT_ITEM_CHIP = ITEMS.register("direct_input", ProcessChipItem::new);

    public static final DeferredHolder<Item, @NotNull Item> ETHER_PROCESS_FACTORY_ITEM = block(BlockRegistry.ETHER_PROCESS_FACTORY);
    public static void register(IEventBus modbus) {
        ITEMS.register(modbus);
    }
}
