package studio.fantasyit.ether_craft.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class LogoItem extends Item {
    public LogoItem(Identifier identifier) {
        super(new Properties().stacksTo(64).setId(ResourceKey.create(Registries.ITEM, identifier)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("tooltip.ether_craft.logo.desc"));
        builder.accept(Component.translatable("tooltip.ether_craft.logo.warning"));
    }
}
