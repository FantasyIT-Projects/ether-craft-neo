package studio.fantasyit.ether_craft.node.plugins.function;

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

public class FunctionCreativeEther extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("generator/creative");
    public static final Identifier ID_FUNC = EtherCraft.id("generator/creative_f");
    public static final Identifier SYNC_VALUE = EtherCraft.id("generator/creative/sync");

    public int fillAmount = 1;

    public FunctionCreativeEther(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
        this.fillAmount = (int) nodeEntity.getMaxEther();
    }

    @Override
    public void tickWork() {
        if (nodeEntity.getEther() < nodeEntity.getMaxEther())
            nodeEntity.receiveEther(fillAmount);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fillAmount = input.getIntOr("fillAmount", (int) nodeEntity.getMaxEther());
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("fillAmount", fillAmount);
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> fillAmount, t -> fillAmount = t));
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        if (message.id().equals(SYNC_VALUE)) {
            fillAmount = Math.max(1, Math.min(message.data(), (int) nodeEntity.getMaxEther()));
        }
    }
}
