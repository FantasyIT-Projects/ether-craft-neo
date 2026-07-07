package studio.fantasyit.ether_craft.integration.jei.screen;

import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.NonNull;
import studio.fantasyit.ether_craft.network.c2s.SetFilterSlotC2S;

public record FilterSlotTarget<I>(int menu, int i, int x, int y) implements IGhostIngredientHandler.Target<I> {
    @Override
    public Rect2i getArea() {
        return new Rect2i(x, y, 16, 16);
    }

    @Override
    public void accept(@NonNull I ingredient) {
        if (ingredient instanceof ItemStack it) {
            ClientPacketDistributor.sendToServer(new SetFilterSlotC2S(menu, i, it));
        }
    }

}
