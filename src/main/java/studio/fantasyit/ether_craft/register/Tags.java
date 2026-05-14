package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import studio.fantasyit.ether_craft.EtherCraft;

public class Tags {
    public static final TagKey<Item> PROCESS_CHIP =TagKey.create(Registries.ITEM,EtherCraft.id("ether_process_chip"));
    public static final TagKey<Block> ETHER_STREAM_PASS_THROUGH =TagKey.create(Registries.BLOCK,EtherCraft.id("ether_stream_pass_through"));
    public static final TagKey<Block> ETHER_MACHINE =TagKey.create(Registries.BLOCK,EtherCraft.id("ether_machine"));
}
