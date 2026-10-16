package studio.fantasyit.ether_craft.factory.special;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;

/** 一条特殊配方：分类 + id + 已构建的工厂树。 */
public record SpecialRecipe(Identifier categoryId, Identifier id, EtherProcessFactoryRecipe recipe) {
}
