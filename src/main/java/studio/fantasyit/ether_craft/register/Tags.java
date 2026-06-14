package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import studio.fantasyit.ether_craft.EtherCraft;

public class Tags {
    public static final TagKey<Item> PROCESS_CHIP =TagKey.create(Registries.ITEM,EtherCraft.id("ether_process_chip"));
    public static final TagKey<Block> ETHER_STREAM_PASS_THROUGH =TagKey.create(Registries.BLOCK,EtherCraft.id("ether_stream_pass_through"));
    public static final TagKey<Block> ETHER_MACHINE =TagKey.create(Registries.BLOCK,EtherCraft.id("ether_machine"));
    public static final TagKey<Block> ETHER_WRENCHABLE =TagKey.create(Registries.BLOCK,EtherCraft.id("ether_wrenchable"));
    public static final TagKey<Item> CONSUMABLE_EQUIPMENTS =TagKey.create(Registries.ITEM,EtherCraft.id("consumable_equipments"));
    public static final TagKey<Item> VINES =TagKey.create(Registries.ITEM,EtherCraft.id("vines"));
    public static final TagKey<Block> CROP_ACCELERATABLE =TagKey.create(Registries.BLOCK,EtherCraft.id("crop_acceleratable"));
    public static final TagKey<Item> PLANT_FLOATING =TagKey.create(Registries.ITEM,EtherCraft.id("plant_floating"));
    public static final TagKey<EntityType<?>> ETHER_STREAM_PASS_THROUGH_ENTITY =TagKey.create(Registries.ENTITY_TYPE,EtherCraft.id("ether_stream_pass_through"));
}
