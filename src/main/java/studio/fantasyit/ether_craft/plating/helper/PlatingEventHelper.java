package studio.fantasyit.ether_craft.plating.helper;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingEventTrigger;

import java.util.List;

public class PlatingEventHelper {

    public static ItemStack[] getPlatedEquipment(LivingEntity entity) {
        return new ItemStack[]{
                entity.getMainHandItem(),
                entity.getOffhandItem(),
                entity.getItemBySlot(EquipmentSlot.HEAD),
                entity.getItemBySlot(EquipmentSlot.CHEST),
                entity.getItemBySlot(EquipmentSlot.LEGS),
                entity.getItemBySlot(EquipmentSlot.FEET)
        };
    }

    @Nullable
    public static IPlatingEffect getEffect(Identifier id) {
        return PlatingManager.getEffect(id);
    }


    public static void forEachPlating(ItemStack stack, TriConsumer<IPlatingEffect, PlatingData, ItemStack> trigger) {
        List<PlatingData> data = PlatingUtil.getPlatingData(stack);
        if (data.isEmpty()) return;
        for (PlatingData d : data) {
            IPlatingEffect effect = PlatingManager.getEffect(d.id());
            if (effect != null) {
                trigger.accept(effect, d, stack);
            }
        }
    }

    public static void forEachPlatingOnEquipment(LivingEntity entity, TriConsumer<IPlatingEffect, PlatingData, ItemStack> trigger) {
        for (ItemStack stack : getPlatedEquipment(entity)) {
            forEachPlating(stack, trigger);
        }
    }

    public static <T extends Event, D extends IPlatingEventTrigger<T>> void doEventTrigger(LivingEntity entity, T event, Class<D> dClass) {
        forEachPlatingOnEquipment(entity, (a, b, c) -> {
            if (dClass.isInstance(a)) {
                D a1 = dClass.cast(a);
                a1.apply(a, b, c, entity, event);
            }
        });
    }


    public static <T extends Event, D extends IPlatingEventTrigger<T>> void doEventTrigger(LivingEntity entity, ItemStack itemStack, T event, Class<D> dClass) {
        forEachPlating(itemStack, (a, b, c) -> {
            if (dClass.isInstance(a)) {
                D a1 = dClass.cast(a);
                a1.apply(a, b, c, entity, event);
            }
        });
    }
}
