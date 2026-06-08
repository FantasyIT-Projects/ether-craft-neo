package studio.fantasyit.ether_craft.plating.trigger;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

public interface IPlatingWorldRenderTrigger extends IPlatingEffect {
    void onWorldRender(PlatingData data, ItemStack stack, Player player, SubmitCustomGeometryEvent event);
}
