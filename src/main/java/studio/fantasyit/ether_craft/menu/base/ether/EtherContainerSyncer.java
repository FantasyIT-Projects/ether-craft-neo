package studio.fantasyit.ether_craft.menu.base.ether;

import net.minecraft.world.inventory.DataSlot;
import studio.fantasyit.ether_craft.block.base.EtherContainer;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;

import java.util.function.Consumer;

public class EtherContainerSyncer {
    private final EtherContainer container;

    public EtherContainerSyncer(EtherContainer container, Consumer<DataSlot> menu) {
        this.container = container;
        menu.accept(new BaseDataSlot(this::getLow, this::setLow));
        menu.accept(new BaseDataSlot(this::getHigh, this::setHigh));
    }

    public int getLow() {
        return (int) (container.getEther() & 0xffffffffL);
    }

    public int getHigh() {
        return (int) (container.getEther() >>> 32);
    }

    public void setLow(int low) {
        container.setEtherNoUpdate(low & 0xFFFFFFFFL | ((long) getHigh() << 32));
    }

    public void setHigh(int high) {
        container.setEtherNoUpdate(((long) high << 32) | getLow() & 0xFFFFFFFFL);
    }
}
