package studio.fantasyit.ether_craft.mixin.pt;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import mezz.jei.api.recipe.category.IRecipeCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.teacon.powertool.client.gui.JEIRecipeDisplayScreen;
import studio.fantasyit.ether_craft.integration.jei.EtherProcessCategory;
import studio.fantasyit.ether_craft.integration.powertool.WrappedRecipeCategory;

@Mixin(JEIRecipeDisplayScreen.class)
public class JEIRecipeDisplayScreenMixin {
    @ModifyVariable(method = "updateRecipeLayout(Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;)Lmezz/jei/api/gui/IRecipeLayoutDrawable;", at = @At(value = "INVOKE_ASSIGN", shift = At.Shift.AFTER, target = "Lmezz/jei/api/recipe/IRecipeManager;getRecipeCategory(Lmezz/jei/api/recipe/types/IRecipeType;)Lmezz/jei/api/recipe/category/IRecipeCategory;"), name = "category", require = 0)
    private static IRecipeCategory<?> updateRecipeDisplay(IRecipeCategory<?> category) {
        if (category instanceof EtherProcessCategory)
            return new WrappedRecipeCategory<>(category);
        return category;
    }
}
