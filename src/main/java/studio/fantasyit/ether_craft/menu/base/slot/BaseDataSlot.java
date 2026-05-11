package studio.fantasyit.ether_craft.menu.base.slot;

import net.minecraft.world.inventory.DataSlot;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class BaseDataSlot extends DataSlot {
    Supplier<Integer> getter;
    Consumer<Integer> setter;
    public BaseDataSlot(Supplier<Integer> getter, Consumer<Integer> setter) {
        this.getter = getter;
        this.setter = setter;
    }
    @Override
    public int get() {
        return getter.get();
    }

    @Override
    public void set(int p_39402_) {
        setter.accept(p_39402_);
    }
}
