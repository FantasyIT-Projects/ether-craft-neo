package studio.fantasyit.ether_craft.factory.special;

public sealed interface SpecialRecipeMatchResult {

    record NotSpecial() implements SpecialRecipeMatchResult {
    }
    record NoMatch() implements SpecialRecipeMatchResult {
    }
    record Matched(SpecialRecipe recipe) implements SpecialRecipeMatchResult {
    }
}