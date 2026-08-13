package studio.fantasyit.ether_craft.block.base;

import net.minecraft.world.SimpleContainer;

public class SimpleNotifyContainer extends SimpleContainer {
    private final Runnable onChanged;

    public SimpleNotifyContainer(int size, Runnable onChanged) {
        super(size);
        this.onChanged = onChanged;
    }

    @Override
    public void setChanged() {
        onChanged.run();
    }
}
