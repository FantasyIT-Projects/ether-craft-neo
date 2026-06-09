package studio.fantasyit.ether_craft.plating.helper;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

import java.util.ArrayList;
import java.util.List;

public class PlatingUtil {

    public static int getEther(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.PLATING_ETHER, 0);
    }

    public static boolean canExtractEther(ItemStack stack, int amount) {
        return getEther(stack) >= amount && hasPlating(stack);
    }

    public static boolean extractEther(ItemStack stack, int amount) {
        if (!canExtractEther(stack, amount)) return false;
        int current = getEther(stack);
        int remaining = current - amount;
        if (remaining <= 0) {
            clearPlating(stack);
        } else {
            stack.set(DataComponentRegistry.PLATING_ETHER, remaining);
        }
        return true;
    }

    public static void addEther(ItemStack stack, int amount) {
        int current = getEther(stack);
        stack.set(DataComponentRegistry.PLATING_ETHER, current + amount);
    }

    public static boolean hasPlating(ItemStack stack) {
        return stack.has(DataComponentRegistry.PLATING_DATA) || stack.has(DataComponentRegistry.PLATING_IN_PROGRESS);
    }

    public static boolean hasPlating(ItemStack stack, Identifier id) {
        for (PlatingData d : getPlatingData(stack)) {
            if (d.id().equals(id)) return true;
        }
        return false;
    }

    public static void clearPlating(ItemStack stack) {
        stack.remove(DataComponentRegistry.PLATING_DATA);
        stack.remove(DataComponentRegistry.PLATING_ETHER);
        stack.remove(DataComponentRegistry.PLATING_IN_PROGRESS);
        stack.remove(DataComponentRegistry.PLATING_START_TIME);
    }

    public static boolean canPlate(ItemStack stack) {
        return !stack.has(DataComponentRegistry.PLATING_DATA)
                && !stack.has(DataComponentRegistry.PLATING_IN_PROGRESS);
    }

    public static boolean isPlatingInProgress(ItemStack stack) {
        return stack.has(DataComponentRegistry.PLATING_IN_PROGRESS);
    }

    public static void startPlating(ItemStack stack, List<Identifier> effectIds, long gameTime) {
        stack.set(DataComponentRegistry.PLATING_IN_PROGRESS, effectIds);
        stack.set(DataComponentRegistry.PLATING_START_TIME, gameTime);
        stack.set(DataComponentRegistry.PLATING_ETHER, 0);
    }

    public static void overwritePlating(ItemStack stack, List<Identifier> effectIds, long gameTime) {
        startPlating(stack, effectIds, gameTime);
    }

    public static List<Identifier> getInProgress(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.PLATING_IN_PROGRESS, List.of());
    }

    public static List<PlatingData> getPlatingData(ItemStack stack) {
        return stack.getOrDefault(DataComponentRegistry.PLATING_DATA, List.of());
    }

    public static void updatePlatingData(ItemStack stack, PlatingData updated) {
        List<PlatingData> list = new ArrayList<>(getPlatingData(stack));
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(updated.id())) {
                list.set(i, updated);
                break;
            }
        }
        stack.set(DataComponentRegistry.PLATING_DATA, List.copyOf(list));
    }
}