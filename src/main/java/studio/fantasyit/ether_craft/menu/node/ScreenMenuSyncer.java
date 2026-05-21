package studio.fantasyit.ether_craft.menu.node;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScreenMenuSyncer<T> {
    public T last = null;
    public Supplier<T> supplier;
    public Consumer<T> changed;
    public ScreenMenuSyncer(Supplier<T> supplier, Consumer<T> changed) {
        this.supplier = supplier;
        this.changed = changed;
    }
    public void sync() {
        T now = supplier.get();
        if (last == null || !last.equals(now)) {
            changed.accept(now);
            last = now;
        }
    }
}
