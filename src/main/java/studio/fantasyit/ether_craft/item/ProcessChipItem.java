package studio.fantasyit.ether_craft.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.Objects;
import java.util.function.Consumer;

public class ProcessChipItem extends Item {
    public static final Identifier SEPARATOR = EtherCraft.id("separator_chip");

    public ProcessChipItem(Identifier identifier) {
        super(new Properties().setId(ResourceKey.create(Registries.ITEM, identifier)));
    }

    public static ItemStack getStackFor(Identifier id) {
        ItemStack stack = ItemRegistry.PROCESS_CHIP_ITEM.get().getDefaultInstance().copy();
        stack.set(DataComponentRegistry.CHIP_ID, id);
        stack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                id.getNamespace(),
                id.getPath()
        ));
        return stack;
    }

    public static boolean isSeparator(ItemStack stack) {
        return stack.is(ItemRegistry.PROCESS_CHIP_ITEM) && Objects.equals(stack.get(DataComponentRegistry.CHIP_ID), SEPARATOR);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        Identifier id = itemStack.get(DataComponentRegistry.CHIP_ID);
        if (id == null) return;
        String baseKey = "tooltip." + id.getNamespace() + "." + id.getPath();
        builder.accept(Component.translatable(baseKey));

        EtherProcessChipManager.ProcessChipRecord r = EtherProcessChipManager.get(id);
        if (r == null) return;
        builder.accept(Component.translatable("tooltip.ether_craft.ether_process_chip.max_ether", r.maxEther()));
        builder.accept(Component.translatable("tooltip.ether_craft.ether_process_chip.ether_decay", r.etherDecay()));
        builder.accept(Component.translatable("tooltip.ether_craft.ether_process_chip.ether_require", r.etherRequire()));
        builder.accept(Component.translatable("tooltip.ether_craft.ether_process_chip.ether_consume", r.etherConsume()));
    }

    @Override
    public void onCraftedBy(ItemStack itemStack, Player player) {
        super.onCraftedBy(itemStack, player);
        Identifier identifier = itemStack.get(DataComponentRegistry.CHIP_ID);
        if (identifier != null)
            itemStack.set(DataComponents.ITEM_MODEL, Identifier.fromNamespaceAndPath(
                    identifier.getNamespace(),
                    identifier.getPath()
            ));
    }
}
