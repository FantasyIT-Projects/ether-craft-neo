package studio.fantasyit.ether_craft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.List;
import java.util.function.Consumer;

/**
 * 工作台芯片：只携带数据，匹配逻辑在
 * {@link studio.fantasyit.ether_craft.factory.special.CraftTableChipHandler}。
 *
 * <p>默认元数据（见 {@code data/ether_craft/ether_process_chip/craft_table_chip.json}）：
 * 影响当前格子、不阻挡通路、不影响周围格子，即放在通路上（与连接芯片同类）。
 *
 * <p>配方在以太加工中心界面里编辑：右键机器内部网格中的芯片会弹出 3×3 工作台网格浮层。
 */
public class CraftTableChipItem extends ProcessChipItem {
    public static final Identifier CHIP_ID = EtherCraft.id("craft_table_chip");
    public static final Identifier CATEGORY = EtherCraft.id("special/craft_table");

    public CraftTableChipItem(Identifier identifier) {
        super(identifier);
    }

    /** 造一颗已配置的工作台芯片（{@code grid} 为 3×3 行优先、长度 9、允许空气）。 */
    public static ItemStack getStack(Identifier recipeId, List<ItemStack> grid) {
        ItemStack stack = ItemRegistry.CRAFT_TABLE_CHIP_ITEM.get().getDefaultInstance().copy();
        stack.set(DataComponentRegistry.CHIP_ID, CHIP_ID);
        stack.set(DataComponents.ITEM_MODEL, CHIP_ID);
        stack.set(DataComponentRegistry.CRAFT_TABLE_CHIP_DATA, new CraftTableChipData(recipeId, grid));
        return stack;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, context, display, builder, tooltipFlag);
        CraftTableChipData data = itemStack.get(DataComponentRegistry.CRAFT_TABLE_CHIP_DATA);
        if (data == null) {
            builder.accept(Component.translatable("tooltip.ether_craft.craft_table_chip.unset")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        builder.accept(Component.translatable("tooltip.ether_craft.craft_table_chip.recipe", data.recipe().toString())
                .withStyle(ChatFormatting.AQUA));
        for (ItemStack input : data.marked()) {
            builder.accept(Component.literal("  ").append(input.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}
