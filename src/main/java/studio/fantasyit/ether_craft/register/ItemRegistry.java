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
import studio.fantasyit.ether_craft.item.EtherAdaptNodeBlockItem;
import studio.fantasyit.ether_craft.item.EtherProcessFactoryBlockItem;
import studio.fantasyit.ether_craft.item.ProcessChipItem;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, EtherCraft.MODID);

    public static <T extends BlockItem> DeferredHolder<Item, @NotNull Item> block(DeferredHolder<Block, ? extends Block> block) {
        return ITEMS.register(block.getId().getPath(), (r) -> new BlockItem(block.get(), new Item.Properties().setId(ResourceKey.create(Registries.ITEM, r))));
    }

    public static final DeferredHolder<Item, @NotNull Item> ETHER = ITEMS.register("ether", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_CREATIVE = ITEMS.register("ether_creative", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull ProcessChipItem> PROCESS_CHIP_ITEM = ITEMS.register("process_chip", ProcessChipItem::new);
    public static final DeferredHolder<Item, @NotNull ProcessChipItem> DIRECT_INPUT_ITEM_CHIP = ITEMS.register("direct_input", ProcessChipItem::new);

    public static final DeferredHolder<Item, @NotNull Item> WRENCH = ITEMS.register("wrench", i -> new Item(new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> BLADE = ITEMS.register("blade", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> DIAMOND_NEEDLE = ITEMS.register("diamond_needle", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_CRYSTAL = ITEMS.register("ether_crystal", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> ETHERPHILIC_BOWL = ITEMS.register("etherphilic_bowl", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> GOLD_SCREW = ITEMS.register("gold_screw", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> INACTIVATED_ETHER = ITEMS.register("inactivated_ether", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> VACUUM_PIPE = ITEMS.register("vacuum_pipe", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_INGOT = ITEMS.register("ether_ingot", i -> new Item(new Item.Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, i))));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_STREAM_EMITTER_ITEM = block(BlockRegistry.ETHER_STREAM_EMITTER);
    public static final DeferredHolder<Item, @NotNull Item> ETHER_PROCESS_FACTORY_ITEM_LV_1 = ITEMS.register("ether_process_factory_lv_1", EtherProcessFactoryBlockItem.withLevel(1));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_PROCESS_FACTORY_ITEM_LV_2 = ITEMS.register("ether_process_factory_lv_2", EtherProcessFactoryBlockItem.withLevel(2));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_PROCESS_FACTORY_ITEM_LV_3 = ITEMS.register("ether_process_factory_lv_3", EtherProcessFactoryBlockItem.withLevel(3));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_PROCESS_FACTORY_ITEM_LV_4 = ITEMS.register("ether_process_factory_lv_4", EtherProcessFactoryBlockItem.withLevel(4));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_ADAPT_NODE_ITEM_LV_1 = ITEMS.register("ether_adapt_node_lv_1", EtherAdaptNodeBlockItem.withLevel(1));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_ADAPT_NODE_ITEM_LV_2 = ITEMS.register("ether_adapt_node_lv_2", EtherAdaptNodeBlockItem.withLevel(2));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_ADAPT_NODE_ITEM_LV_3 = ITEMS.register("ether_adapt_node_lv_3", EtherAdaptNodeBlockItem.withLevel(3));
    public static final DeferredHolder<Item, @NotNull Item> ETHER_BLOCK_ITEM = block(BlockRegistry.ETHER_BLOCK);
    public static final DeferredHolder<Item, @NotNull Item> ETHER_ORE_ITEM = block(BlockRegistry.ETHER_ORE);
    public static final DeferredHolder<Item, @NotNull Item> DEEPSLATE_ETHER_ORE_ITEM = block(BlockRegistry.DEEPSLATE_ETHER_ORE);
    public static final DeferredHolder<Item, @NotNull Item> NETHER_ETHER_ORE_ITEM = block(BlockRegistry.NETHER_ETHER_ORE);
    public static final DeferredHolder<Item, @NotNull Item> ETHER_GLASS_ITEM = block(BlockRegistry.ETHER_GLASS);

    public static void register(IEventBus modbus) {
        ITEMS.register(modbus);
    }
}
