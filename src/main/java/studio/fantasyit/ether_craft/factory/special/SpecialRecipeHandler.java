package studio.fantasyit.ether_craft.factory.special;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import studio.fantasyit.ether_craft.base.TreeLike;
import studio.fantasyit.ether_craft.recipe.factory.EtherFactoryRecipeInput;
import studio.fantasyit.ether_craft.recipe.factory.RecipeNode;

import java.util.List;
import java.util.Optional;

/**
 * 特殊配方处理器：负责一类"特殊配方树"的生成与匹配。
 *
 * <p>处理器在 {@link SpecialRecipeManager} 中按"特殊芯片物品"注册。
 * 当一棵待匹配的树里出现该芯片物品时，这棵树即被判定为"特殊配方树"，
 * 并交由本处理器决定是否匹配某条特殊配方。
 */
public interface SpecialRecipeHandler {

    /** 本处理器负责的系列分类 id。 */
    Identifier categoryId();

    /**
     * tagUpdate 回调。
     *
     * <p>需要预生成的处理器在此返回整套配方；惰性处理器只清理自己的缓存后返回 {@link List#of()}。
     * 返回值会被 {@link SpecialRecipeManager} 索引，供 {@link SpecialRecipeManager#getRecipe} 查询。
     */
    List<SpecialRecipe> onReload(RecipeManager manager);

    /**
     * 判定该树是否匹配本处理器的特殊配方。
     *
     * @return 命中则返回配方；{@link Optional#empty()} 表示"确认是特殊配方树但没有匹配"
     */
    Optional<SpecialRecipe> match(TreeLike<List<Integer>, RecipeNode> tree,
                                  EtherFactoryRecipeInput input,
                                  Level level,
                                  RecipeManager recipeManager);
}
