package studio.fantasyit.ether_craft.integration.jei;

import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;

import java.util.Optional;

/**
 * 工厂界面的 JEI「+」配方转移：把任意原版合成配方按图案写进工作台网格（3×3 标记格）。
 *
 * <p>自定义实现而非 {@code createBasicRecipeTransferHandler}：标记格只记录"样本"，
 * 不应搬运/扣减玩家背包物品，所以这里直接把配方原料按图案写成标记。
 */
public class CraftGridRecipeTransferHandler
        implements IRecipeTransferHandler<EtherProcessFactoryContainerMenu, RecipeHolder<CraftingRecipe>> {
    private final IRecipeTransferHandlerHelper helper;

    public CraftGridRecipeTransferHandler(IRecipeTransferHandlerHelper helper) {
        this.helper = helper;
    }

    @Override
    public Class<? extends EtherProcessFactoryContainerMenu> getContainerClass() {
        return EtherProcessFactoryContainerMenu.class;
    }

    @Override
    public Optional<MenuType<EtherProcessFactoryContainerMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public IRecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
        return RecipeTypes.CRAFTING;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(EtherProcessFactoryContainerMenu menu,
                                                         RecipeHolder<CraftingRecipe> recipe,
                                                         IRecipeSlotsView recipeSlots,
                                                         Player player,
                                                         boolean maxTransfer,
                                                         boolean doTransfer) {
        if (menu.editingChipSlot < 0)
            return helper.createUserErrorWithTooltip(Component.translatable("jei.ether_craft.craft_grid.closed"));
        if (doTransfer)
            menu.applyRecipeToCraftGrid(recipe.value());
        return null;
    }
}
