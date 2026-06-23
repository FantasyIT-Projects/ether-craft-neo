package studio.fantasyit.ether_craft.integration.guideme;

import guideme.Guides;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;

public class GuideMeFunctions {
    public static ItemStack getGuide() {
        return Guides.createGuideItem(EtherCraft.id("guide"));
    }
}
