package studio.fantasyit.ether_craft.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import studio.fantasyit.ether_craft.EtherCraft;

public class Tags {
    public static final TagKey<Item> PROCESS_CHIP =TagKey.create(Registries.ITEM,EtherCraft.id("ether_process_chip"));
}
