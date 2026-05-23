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
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.function.Consumer;
import java.util.function.Function;

public class EtherProcessFactoryBlockItem extends BlockItem {
    private final int level;

    public static Function<Identifier, EtherProcessFactoryBlockItem> withLevel(int level) {
        return (id) -> new EtherProcessFactoryBlockItem(id, level);
    }

    public EtherProcessFactoryBlockItem(Identifier id, int level) {
        super(BlockRegistry.ETHER_PROCESS_FACTORY.get(), new Properties().setId(ResourceKey.create(Registries.ITEM, id)));
        this.level = level;
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState bs = super.getPlacementState(context);
        return bs == null ? null : bs.setValue(EtherProcessFactoryBlock.LEVEL, level);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext ctx, TooltipDisplay display, Consumer<Component> tooltipAdder, TooltipFlag flag) {
        super.appendHoverText(stack, ctx, display, tooltipAdder, flag);
        EtherProcessFactoryEntity.appendTooltipLines(stack, level, ctx, flag, tooltipAdder);
    }
}
