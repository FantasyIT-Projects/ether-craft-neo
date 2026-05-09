package studio.fantasyit.ether_craft.node.filter;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.menu.base.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.base.FilterSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;

public class FilterGuiRegCommon {
    public static final Identifier SYNC_FILTER = EtherCraft.id("filter/whitelist");
    public static void slots(EtherAdaptNodeContainerMenu menu, ItemFilter filter) {
        menu.addDataSlot(new BaseDataSlot(() -> filter.whitelist ? 1 : 0, (a) -> filter.whitelist = (a == 1)));
        menu.addSlotAreaDraw(filter, 0, 43, 76, 7, 18, 3, 18, (a, b, c, d, e, f) -> new FilterSlot((ItemFilter) a, b, c, d));
    }

    public static void sync(SyncScreenDataC2S message, ItemFilter filter) {
        if (message.id().equals(SYNC_FILTER)) {
            filter.whitelist = message.data() == 1;
        }
    }
}
