package studio.fantasyit.ether_craft.node.plugins.function;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

public class FunctionMute extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("mute");
    public static final Identifier SYNC_RX = EtherCraft.id("mute/rx");
    public static final Identifier SYNC_RY = EtherCraft.id("mute/ry");
    public static final Identifier SYNC_RZ = EtherCraft.id("mute/rz");

    public int rx = Config.nodeMuteMaxRange;
    public int ry = Config.nodeMuteMaxRange;
    public int rz = Config.nodeMuteMaxRange;

    public FunctionMute(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rx = input.getIntOr("rx", Config.nodeMuteMaxRange);
        ry = input.getIntOr("ry", Config.nodeMuteMaxRange);
        rz = input.getIntOr("rz", Config.nodeMuteMaxRange);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("rx", rx);
        output.putInt("ry", ry);
        output.putInt("rz", rz);
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> rx, t -> rx = t));
        menu.addDataSlot(new BaseDataSlot(() -> ry, t -> ry = t));
        menu.addDataSlot(new BaseDataSlot(() -> rz, t -> rz = t));
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        if (message.id().equals(SYNC_RX)) {
            rx = Math.clamp(message.data(), 0, Config.nodeMuteMaxRange);
            nodeEntity.pluginUpdate();
        } else if (message.id().equals(SYNC_RY)) {
            ry = Math.clamp(message.data(), 0, Config.nodeMuteMaxRange);
            nodeEntity.pluginUpdate();
        } else if (message.id().equals(SYNC_RZ)) {
            rz = Math.clamp(message.data(), 0, Config.nodeMuteMaxRange);
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void tickWork() {
        if (nodeEntity.getLevel() == null)
            return;
        var muteSources = nodeEntity.getLevel().getData(AttachmentDataRegistry.LEVEL_MUTE_SOURCE);
        int volume = (2 * rx + 1) * (2 * ry + 1) * (2 * rz + 1);
        int cost = Math.ceilDiv(volume, 16) * Config.nodeMuteEtherCostPer16Block;
        if (nodeEntity.getEther() < cost)
            return;

        nodeEntity.extractEther(cost);
        muteSources.notifyBlock(nodeEntity.getLevel(), nodeEntity.getBlockPos(), rx, ry, rz);
    }
}
