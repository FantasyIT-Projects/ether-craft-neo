package studio.fantasyit.ether_craft.node.plugins;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.IFilterSwitchable;
import studio.fantasyit.ether_craft.menu.base.RangeLimitPlaceContainer;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.base.slot.FilterSlot;
import studio.fantasyit.ether_craft.menu.factory.slot.SingleStackSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.menu.node.slot.OversizedEtherSlot;
import studio.fantasyit.ether_craft.menu.node.slot.RangeLimitFilterSlot;
import studio.fantasyit.ether_craft.node.NodeProperty;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;

import java.util.ArrayList;
import java.util.List;

public class MainPageDummyPlugin extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("main_page_dummy");
    private boolean filterActive = false;

    public MainPageDummyPlugin(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }


    @Override
    public void modifyNodeProperty(NodeProperty nodeProperty) {

    }


    @Override
    public void saveAdditional(ValueOutput output) {

    }

    @Override
    public void loadAdditional(ValueInput input) {

    }

    public static int[][] SLOT_POS = {
            {92, 49},
            {110, 49},
            {110, 31},
            {128, 31},
            {128, 13},
            {146, 13}
    };

    @Override
    public PluginMenuContext<?> makeContext(EtherAdaptNodeContainerMenu menu) {
        return new MainPageContext(menu, this);
    }

    public void registerSlotsWithContext(EtherAdaptNodeContainerMenu menu, MainPageContext ctx) {
        menu.addSlot(new OversizedEtherSlot(nodeEntity.etherStorage, 0, 28, 19));
        ctx.functionStorage = (SingleStackSlot) menu.addSlot(new SingleStackSlot(nodeEntity.functionStorage, 0, 28, 45));

        int slots = nodeEntity.getUpgradeCount();
        ctx.normalStorage = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            SingleStackSlot slot = (SingleStackSlot) menu.addSlotDraw(new SingleStackSlot(nodeEntity.featureUpgradeStorage, i, SLOT_POS[i][0], SLOT_POS[i][1]));
            ctx.normalStorage.add(slot);
        }

        menu.addSlotArea(nodeEntity.normalStorage, 0, 8, 76, 9, 18, 3, 18,
                (a, b, c, d, e, f) -> new RangeLimitFilterSlot((RangeLimitPlaceContainer) a, nodeEntity.normalStorageFilter, b, c, d),
                (s, i, j) -> {
                    menu.toDrawSlot.add(s);
                    ctx.mainSlots.add((RangeLimitFilterSlot) s);
                });

        menu.addSlotArea(nodeEntity.normalStorageFilter, 0, 8, 76, 9, 18, 3, 18,
                (a, b, c, d, e, f) -> {
                    FilterSlot fs = new FilterSlot((ItemFilter) a, b, c, d);
                    fs.setActive(false);
                    return fs;
                },
                (s, i, j) -> {
                    menu.toDrawSlot.add(s);
                    ctx.filterSlots.add((FilterSlot) s);
                });

        menu.addDataSlot(new BaseDataSlot(nodeEntity.normalStorage::getAccessibleCount, nodeEntity.normalStorage::setAccessibleCount));
    }

    public static class MainPageContext extends PluginMenuContext<MainPageDummyPlugin> implements IFilterSwitchable {
        public List<FilterSlot> filterSlots = new ArrayList<>();
        public List<RangeLimitFilterSlot> mainSlots = new ArrayList<>();
        public SingleStackSlot functionStorage;
        public List<SingleStackSlot> normalStorage;
        public boolean filterActive = false;

        public MainPageContext(EtherAdaptNodeContainerMenu menu, MainPageDummyPlugin plugin) {
            super(menu, plugin);
            plugin.registerSlotsWithContext(menu, this);
        }

        @Override
        public boolean isFilterActive() {
            return filterActive;
        }

        @Override
        public void setFilterActive(boolean active) {
            this.filterActive = active;
        }
    }
}
