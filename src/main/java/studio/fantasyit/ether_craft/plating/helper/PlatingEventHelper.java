package studio.fantasyit.ether_craft.plating.helper;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import org.apache.logging.log4j.util.TriConsumer;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.PlatingManager;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingEventTrigger;
import studio.fantasyit.ether_craft.plating.trigger.inst.IEffectStartAndEndTrigger;
import studio.fantasyit.ether_craft.plating.trigger.data.TriggerOnNotExistRecord;
import studio.fantasyit.ether_craft.register.AttachmentDataRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public static void doEffectStartEndTrigger(LivingEntity entity) {
        Map<Identifier, PlatingData> platingDataMap = new HashMap<>();
        Map<Identifier, IEffectStartAndEndTrigger> triggerMap = new HashMap<>();
        Set<Identifier> currentIds = new HashSet<>();

        forEachPlatingOnEquipment(entity, (effect, data, stack) -> {
            if (effect instanceof IEffectStartAndEndTrigger trigger) {
                currentIds.add(data.id());
                platingDataMap.putIfAbsent(data.id(), data);
                triggerMap.putIfAbsent(data.id(), trigger);
            }
        });

        TriggerOnNotExistRecord record = entity.getExistingData(AttachmentDataRegistry.TRIGGER_ON_NOT_EXIST_RECORD.get())
                .orElse(new TriggerOnNotExistRecord(new HashSet<>()));

        if (currentIds.isEmpty() && record.applied().isEmpty()) return;

        Set<Identifier> toAdd = new HashSet<>(currentIds);
        toAdd.removeAll(record.applied());

        Set<Identifier> toRemove = new HashSet<>(record.applied());
        toRemove.removeAll(currentIds);

        for (Identifier id : toAdd) {
            IEffectStartAndEndTrigger trigger = triggerMap.get(id);
            if (trigger != null) {
                trigger.onEffectStarts(entity, platingDataMap.get(id));
            }
        }

        for (Identifier id : toRemove) {
            IPlatingEffect effect = PlatingManager.getEffect(id);
            if (effect instanceof IEffectStartAndEndTrigger trigger) {
                trigger.onEffectEnds(entity);
            }
        }

        Set<Identifier> newApplied = new HashSet<>(record.applied());
        newApplied.addAll(toAdd);
        newApplied.removeAll(toRemove);
        entity.setData(AttachmentDataRegistry.TRIGGER_ON_NOT_EXIST_RECORD.get(), new TriggerOnNotExistRecord(newApplied));
    }
}
