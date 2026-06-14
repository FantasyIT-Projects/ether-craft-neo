package studio.fantasyit.ether_craft.plating.helper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.ArrayList;
import java.util.List;

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

        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR_STAND_SLOTS) {
            items.add(stand.getItemBySlot(slot));
        }
        distributeCharge(stream, items);
        for (int i = 0; i < ARMOR_STAND_SLOTS.length; i++) {
            stand.setItemSlot(ARMOR_STAND_SLOTS[i], items.get(i));
        }
    }

    public static void tryChargeShelf(IEtherStreamLike stream, ShelfBlockEntity shelf) {
        if (stream.getEther() <= 0) return;

        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < shelf.getContainerSize(); i++) {
            items.add(shelf.getItem(i));
        }
        distributeCharge(stream, items);
        for (int i = 0; i < shelf.getContainerSize(); i++) {
            shelf.setItem(i, items.get(i));
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
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : PLAYER_CHARGE_SLOTS) {
            items.add(player.getItemBySlot(slot));
        }
        distributeCharge(stream, items);
    }

    public static void tryChargeEntity(IEtherStreamLike stream, Entity entity) {
        if (entity instanceof ArmorStand as)
            tryChargeArmorStand(stream, as);
        else if (entity instanceof Player player)
            tryChargePlayer(stream, player);
    }

    private static void distributeCharge(IEtherStreamLike stream, List<ItemStack> items) {
        if (stream.getEther() <= 0) return;

        List<ItemStack> chargeable = new ArrayList<>();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) continue;
            if (!PlatingUtil.hasPlating(stack) && !PlatingUtil.isPlatingInProgress(stack)) continue;
            chargeable.add(stack);
        }

        if (chargeable.isEmpty()) return;

        int totalEther = stream.getEther();
        int count = chargeable.size();
        int perItem = Math.min(totalEther / count, Config.platingMaxEtherReceive);

        if (perItem <= 0) return;

        for (ItemStack stack : chargeable) {
            PlatingUtil.addEther(stack, perItem);
            stream.consumeEther(perItem);
        }
    }
}
