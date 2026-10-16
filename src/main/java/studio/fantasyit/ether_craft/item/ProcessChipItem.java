package studio.fantasyit.ether_craft.item;

import net.minecraft.ChatFormatting;
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
import org.jspecify.annotations.NonNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public class ProcessChipItem extends Item {
    public static final Identifier SEPARATOR = EtherCraft.id("separator_chip");
    public static final Identifier CONCAT = EtherCraft.id("concat_chip");

    /**
     * 芯片 id → 承载该芯片的物品。
     * 普通芯片全部由 {@link ItemRegistry#PROCESS_CHIP_ITEM} 承载；特殊芯片可以注册成自己的物品。
     */
    private static final Map<Identifier, ProcessChipItem> CHIP_ITEMS = new HashMap<>();

    public ProcessChipItem(Identifier identifier) {
        super(new Properties().setId(ResourceKey.create(Registries.ITEM, identifier)));
        CHIP_ITEMS.put(identifier, this);
    }

    /** 取得承载指定芯片 id 的物品；未注册的芯片回落到通用处理芯片。 */
    public static ProcessChipItem itemForChipId(Identifier chipId) {
        ProcessChipItem item = CHIP_ITEMS.get(chipId);
        return item != null ? item : ItemRegistry.PROCESS_CHIP_ITEM.get();
    }

    public static ItemStack getStackFor(Identifier id) {
        ItemStack stack = itemForChipId(id).getDefaultInstance().copy();
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

    public static boolean isConcat(ItemStack stack) {
        return stack.is(ItemRegistry.PROCESS_CHIP_ITEM) && Objects.equals(stack.get(DataComponentRegistry.CHIP_ID), CONCAT);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        Identifier id = itemStack.get(DataComponentRegistry.CHIP_ID);
        if (id == null) return;
        EtherProcessChipManager.ProcessChipRecord r = EtherProcessChipManager.get(id);
        if (r == null) return;
        if (tooltipFlag.isAdvanced()) {
            builder.accept(Component.literal(id.toString()).withStyle(ChatFormatting.DARK_GRAY));
            builder.accept(Component.translatable("tooltip.ether_craft.ether_process_chip.storage", r.storage()));
        }
        builder.accept(Component.translatable("tooltip.ether_craft.ether_process_chip.ether_consume", r.etherConsume()));
    }

    @Override
    public @NonNull Component getName(ItemStack itemStack) {
        Identifier id = itemStack.get(DataComponentRegistry.CHIP_ID);
        if (id == null) return super.getName(itemStack);
        String baseKey = "tooltip." + id.getNamespace() + "." + id.getPath();
        return Component.translatable(baseKey);
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
