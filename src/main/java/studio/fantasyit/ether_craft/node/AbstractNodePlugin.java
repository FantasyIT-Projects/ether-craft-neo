package studio.fantasyit.ether_craft.node;

import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;

public abstract class AbstractNodePlugin {
    protected final EtherAdaptNodeEntity nodeEntity;

    public AbstractNodePlugin(EtherAdaptNodeEntity nodeEntity) {
        this.nodeEntity = nodeEntity;
    }

    public abstract void modifyNodeProperty(NodeProperty nodeProperty);

    public abstract void tick();

    public abstract void saveAdditional(ValueOutput output);

    public abstract void loadAdditional(ValueInput input);

    public boolean inputFilter(ItemResource resource) {
        return true;
    }

    public boolean outputFilter(ItemResource resource) {
        return true;
    }

    public int earlyHandleInput(ItemResource resource, int amount, TransactionContext context) {
        return 0;
    }

    public void onDestroy() {
    }

    public void onWrenchRotate(Direction.Axis axis) {
    }

    public void registerSlots(EtherAdaptNodeContainerMenu menu){
    }
}