package studio.fantasyit.ether_craft.plating.helper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public class PlatingChargingUtil {

    private static final EquipmentSlot[] ARMOR_STAND_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };

    public static void tryChargeArmorStand(IEtherStreamLike stream, ArmorStand stand) {
        if (stream.getEther() <= 0) return;

        for (EquipmentSlot slot : ARMOR_STAND_SLOTS) {
            ItemStack stack = stand.getItemBySlot(slot);
            if (tryChargeItem(stream, stack)) {
                stand.setItemSlot(slot, stack);
            }
        }
    }

    public static void tryChargeShelf(IEtherStreamLike stream, ShelfBlockEntity shelf) {
        if (stream.getEther() <= 0) return;

        for (int i = 0; i < shelf.getContainerSize(); i++) {
            ItemStack stack = shelf.getItem(i);
            if (tryChargeItem(stream, stack)) {
                shelf.setItem(i, stack);
            }
        }
    }

    private static final EquipmentSlot[] PLAYER_CHARGE_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    };

    public static void tryChargePlayer(IEtherStreamLike stream, Player player) {
        if (stream.getEther() <= 0) return;
        for (EquipmentSlot slot : PLAYER_CHARGE_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            tryChargeItem(stream, stack);
            if (stream.getEther() <= 0) break;
        }
    }

    public static void tryChargeEntity(IEtherStreamLike stream, Entity entity) {
        if (entity instanceof ArmorStand as)
            tryChargeArmorStand(stream, as);
        else if (entity instanceof Player player)
            tryChargePlayer(stream, player);
    }

    private static boolean tryChargeItem(IEtherStreamLike stream, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!PlatingUtil.hasPlating(stack) && !PlatingUtil.isPlatingInProgress(stack)) return false;

        int ether = stream.getEther();
        if (ether <= 0) return false;

        int amount = Math.min(ether, Config.platingMaxEtherReceive);
        PlatingUtil.addEther(stack, amount);
        stream.consumeEther(amount);
        return true;
    }
}
