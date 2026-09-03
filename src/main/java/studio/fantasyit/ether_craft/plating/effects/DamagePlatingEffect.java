package studio.fantasyit.ether_craft.plating.effects;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.data.PlatingData;
import studio.fantasyit.ether_craft.plating.helper.PlatingUtil;
import studio.fantasyit.ether_craft.plating.trigger.event.IPlatingAttackTrigger;
import studio.fantasyit.ether_craft.plating.trigger.inst.IInstanceTrigger;
import studio.fantasyit.ether_craft.plating.trigger.inst.IPlatingClearingTrigger;

import java.util.List;

public class DamagePlatingEffect implements IPlatingEffect, IInstanceTrigger, IPlatingAttackTrigger, IPlatingClearingTrigger {
    public static final Identifier ID = EtherCraft.id("damage");

    private static final Identifier ATTR_ID = ID.withSuffix("/attribute");

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void onPlatted(PlatingData data, ItemStack stack) {
        ItemAttributeModifiers itemAttributeModifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (itemAttributeModifiers == null) {
            itemAttributeModifiers = ItemAttributeModifiers.builder()
                    .add(
                            Attributes.ATTACK_DAMAGE,
                            new AttributeModifier(ATTR_ID, data.effect(), AttributeModifier.Operation.ADD_VALUE),
                            EquipmentSlotGroup.HAND
                    )
                    .build();
        } else {
            itemAttributeModifiers = itemAttributeModifiers.withModifierAdded(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ATTR_ID, data.effect(), AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.HAND
            );
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, itemAttributeModifiers);
    }


    @Override
    public void apply(IPlatingEffect effect, PlatingData data, ItemStack stack, LivingEntity entity, AttackEntityEvent event) {
        if (!event.isCanceled()) return;
        PlatingUtil.extractEtherWithEntityContext(entity, stack, Config.platingDamageCost);
        if (!PlatingUtil.canExtractEther(stack, Config.platingDamageCost)) {
            clearModifier(stack);
        }
    }

    private void clearModifier(ItemStack stack) {
        ItemAttributeModifiers itemAttributeModifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (itemAttributeModifiers != null) {
            List<ItemAttributeModifiers.Entry> list = itemAttributeModifiers.modifiers().stream().filter(t -> !t.modifier().is(ATTR_ID)).toList();
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, new ItemAttributeModifiers(list));
        }
    }

    @Override
    public void onClear(PlatingData data, ItemStack stack) {
        clearModifier(stack);
    }
}