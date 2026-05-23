package studio.fantasyit.ether_craft.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.function.Consumer;
import java.util.function.Function;

public class EtherAdaptNodeBlockItem extends BlockItem {
    private final int level;

    public static Function<Identifier, EtherAdaptNodeBlockItem> withLevel(int level) {
        return (id) -> new EtherAdaptNodeBlockItem(id, level);
    }

    public EtherAdaptNodeBlockItem(Identifier id, int level) {
        super(BlockRegistry.ETHER_ADAPT_NODE.get(), new Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        this.level = level;
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState bs = super.getPlacementState(context);
        return bs == null ? null : bs.setValue(EtherAdaptNodeBlock.LEVEL, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, tooltipAdder, flag);
        EtherAdaptNodeEntity.appendTooltipLines(stack, level, ctx, flag, tooltipAdder);
    }
}
