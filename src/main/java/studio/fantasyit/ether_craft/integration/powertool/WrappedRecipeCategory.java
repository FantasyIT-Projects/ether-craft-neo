package studio.fantasyit.ether_craft.integration.powertool;


import com.mojang.serialization.Codec;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.integration.jei.EtherProcessCategory;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeLayoutCalculator;

import java.util.List;

public class WrappedRecipeCategory<T> implements IRecipeCategory<T> {

    private final IRecipeCategory<T> original;
    private int canvasW = 0;
    private int canvasH = 0;

    public WrappedRecipeCategory(IRecipeCategory<T> original) {
        this.original = original;
    }

    @Override
    public IRecipeType<T> getRecipeType() {
        return original.getRecipeType();
    }

    @Override
    public Component getTitle() {
        return original.getTitle();
    }

    @Override
    public int getWidth() {
        return canvasW > 0 ? canvasW : original.getWidth();
    }

    @Override
    public int getHeight() {
        return canvasH > 0 ? canvasH : original.getHeight();
    }

    @Override
    @Nullable
    public IDrawable getIcon() {
        return original.getIcon();
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T recipe, IFocusGroup focuses) {
        original.setRecipe(builder, recipe, focuses);
        if (recipe instanceof EtherProcessCategory.EtherProcessFactoryRecipeWrapper wrapper) {
            if(wrapper.recipe() != null && wrapper.recipe().json != null) {
                TreeDiagramSpec spec = TreeDiagramSpec.fromJson(wrapper.recipe().json);
                TreeDiagramLayout layout = TreeLayoutCalculator.compute(spec);
                this.canvasW = layout.canvasWidth;
                this.canvasH = layout.canvasHeight;
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T recipe, IFocusGroup focuses) {
        if (original instanceof EtherProcessCategory epc && recipe instanceof EtherProcessCategory.EtherProcessFactoryRecipeWrapper re)
            epc.createRecipeExtras(builder, re, focuses, true);
        else
            original.createRecipeExtras(builder, recipe, focuses);
    }

    @Override
    public void onDisplayedIngredientsUpdate(T recipe, List<IRecipeSlotDrawable> recipeSlots, IFocusGroup focuses) {
        original.onDisplayedIngredientsUpdate(recipe, recipeSlots, focuses);
    }

    @Override
    public boolean isHandled(T recipe) {
        return original.isHandled(recipe);
    }

    @Override
    @Nullable
    public Identifier getIdentifier(T recipe) {
        return original.getIdentifier(recipe);
    }

    @Override
    public Codec<T> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return original.getCodec(codecHelper, recipeManager);
    }

    @Override
    public boolean needsRecipeBorder() {
        return original.needsRecipeBorder();
    }
}