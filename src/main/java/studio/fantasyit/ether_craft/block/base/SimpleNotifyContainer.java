package studio.fantasyit.ether_craft.block.base;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;

public class SimpleNotifyContainer extends SimpleContainer {
    private final BlockEntity container;

    public SimpleNotifyContainer(int size, BlockEntity container) {
        super(size);
        this.container = container;
    }

    @Override
    public void setChanged() {
        this.container.setChanged();
    }
}
