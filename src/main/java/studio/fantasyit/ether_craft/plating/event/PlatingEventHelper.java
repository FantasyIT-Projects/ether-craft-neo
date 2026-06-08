package studio.fantasyit.ether_craft.plating.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

import java.util.List;

public class PlatingEventHelper {

    public static ItemStack[] getPlatedEquipment(Player player) {
        return new ItemStack[]{
                player.getMainHandItem(),
                player.getOffhandItem(),
                player.getItemBySlot(EquipmentSlot.HEAD),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.FEET)
        };
    }

    @Nullable
    public static IPlatingEffect getEffect(Identifier id) {
        return PlatingManager.getEffect(id);
    }

    @FunctionalInterface
    public interface PlatingTrigger {
        void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, Player player);
    }

    public static void forEachPlating(ItemStack stack, Player player, PlatingTrigger trigger) {
        List<PlatingData> data = PlatingUtil.getPlatingData(stack);
        if (data.isEmpty()) return;
        for (PlatingData d : data) {
            IPlatingEffect effect = PlatingManager.getEffect(d.id());
            if (effect != null) {
                trigger.apply(effect, d, stack, player);
            }
        }
    }

    public static void forEachPlatingOnEquipment(Player player, PlatingTrigger trigger) {
        for (ItemStack stack : getPlatedEquipment(player)) {
            forEachPlating(stack, player, trigger);
        }
    }
}
