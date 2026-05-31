package studio.fantasyit.ether_craft.node.plugins.feature;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class RedstoneSwitchUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("redstone_switch");
    public static final Identifier SYNC_MODE = EtherCraft.id("redstone_switch/mode");

    public boolean workWithSignal = true;

    public RedstoneSwitchUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public boolean preTick() {
        if (nodeEntity.getLevel() == null) return true;
        boolean hasSignal = nodeEntity.getLevel().hasNeighborSignal(nodeEntity.getBlockPos());
        return workWithSignal ? hasSignal : !hasSignal;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putBoolean("rswWorkWithSignal", workWithSignal);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        workWithSignal = input.getBooleanOr("rswWorkWithSignal", true);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        if (message.id().equals(SYNC_MODE)) {
            workWithSignal = message.data() == 1;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addDataSlot(new BaseDataSlot(() -> workWithSignal ? 1 : 0, t -> workWithSignal = t == 1));
    }
}
