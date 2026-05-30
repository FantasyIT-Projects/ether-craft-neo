package studio.fantasyit.ether_craft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.menu.grid.answer.AnswerFetchMenu;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGrid;
import studio.fantasyit.ether_craft.recipe.grid.EtherProcessFactoryGridInput;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.register.RecipeTypeRegistry;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class EtherProcessRecipeAnswerItem extends Item {
    public final int width;
    public final int height;

    public static Function<Identifier, EtherProcessRecipeAnswerItem> getConstructorWithShape(int width, int height) {
        return id -> new EtherProcessRecipeAnswerItem(id, width, height);
    }

    public EtherProcessRecipeAnswerItem(Identifier id, int width, int height) {
        super(new Properties().setId(ResourceKey.create(BuiltInRegistries.ITEM.key(), id)));
        this.width = width;
        this.height = height;
    }

    public EtherProcessFactoryGridInput getInput(ItemStack stack) {
        return new EtherProcessFactoryGridInput(stack, width, height);
    }

    public List<EtherProcessFactoryGrid> getCompatibleGrids(ItemStack stack, ServerLevel level) {
        EtherProcessFactoryGridInput input = getInput(stack);
        RecipeManager recipeAccess = level.getServer().getRecipeManager();
        return recipeAccess.getRecipes()
                .stream().filter(r -> r.value().getType() == RecipeTypeRegistry.ETHER_PROCESS_FACTORY_GRID.get())
                .map(r -> (EtherProcessFactoryGrid) r.value())
                .filter(r -> r.matches(input, level)).toList();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        ItemStackTemplate target = stack.get(DataComponentRegistry.TARGET);
        if (target != null) {
            builder.accept(Component.translatable("tooltip.ether_craft.ether_process_recipe_answer.target",
                    target.create().getHoverName()).withStyle(ChatFormatting.AQUA));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (level.isClientSide()) {
            if (held.has(DataComponentRegistry.GRID)) {
                List<List<ItemStack>> grid = held.get(DataComponentRegistry.GRID);
                ViewGridScreenCreator.createAndSetScreen(grid);
            }
            return InteractionResult.SUCCESS;
        }
        if (!held.has(DataComponentRegistry.GRID)) {
            player.openMenu(new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("gui.ether_craft.answer_fetch");
                }

                @Override
                public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                    return false;
                }

                @Override
                public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
                    buf.writeEnum(hand);
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inv, Player p) {
                    return new AnswerFetchMenu(windowId, p, hand);
                }
            });
        }
        return InteractionResult.SUCCESS;
    }
}
