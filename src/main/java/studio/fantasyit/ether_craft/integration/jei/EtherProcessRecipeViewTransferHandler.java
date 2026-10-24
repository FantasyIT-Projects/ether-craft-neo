package studio.fantasyit.ether_craft.integration.jei;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryScreen;

import java.util.Optional;

/**
 * 以太加工配方 → 工厂 GUI 的「+」配方转移：不搬运任何物品，只按配方新建一个树形配方窗口。
 *
 * <p>纯客户端：配方已同步到客户端，因此 {@code doTransfer} 时直接在界面上开窗，不产生网络包；
 * 模拟阶段（{@code doTransfer == false}）返回 {@code null}，让 JEI 认为这次转移可用。
 */
public class EtherProcessRecipeViewTransferHandler
        implements IRecipeTransferHandler<EtherProcessFactoryContainerMenu,
        EtherProcessCategory.EtherProcessFactoryRecipeWrapper> {
    private final IRecipeType<EtherProcessCategory.EtherProcessFactoryRecipeWrapper> recipeType;

    public EtherProcessRecipeViewTransferHandler(
            IRecipeType<EtherProcessCategory.EtherProcessFactoryRecipeWrapper> recipeType) {
        this.recipeType = recipeType;
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
    public IRecipeType<EtherProcessCategory.EtherProcessFactoryRecipeWrapper> getRecipeType() {
        return recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(EtherProcessFactoryContainerMenu menu,
                                                         EtherProcessCategory.EtherProcessFactoryRecipeWrapper recipe,
                                                         IRecipeSlotsView recipeSlots,
                                                         Player player,
                                                         boolean maxTransfer,
                                                         boolean doTransfer) {
        if (!doTransfer) return null;
        // 注意：JEI 打开配方页时会把 Minecraft.screen 换成自己的 RecipesGui，
        // 因此这里不能用 Minecraft.getInstance().screen 取工厂界面。
        EtherProcessFactoryScreen screen = EtherProcessFactoryScreen.current();
        // 双重保险：只有"当前界面持有的正是这个 menu"时才开窗，避免界面已关掉还往死界面上挂窗口
        if (screen != null && screen.getMenu() == menu) {
            screen.openRecipeWindow(recipe.id(), recipe.recipe());
        }
        return null;
    }
}
