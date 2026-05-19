package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.ArrayList;
import java.util.List;

public class EtherProcessCategory implements IRecipeCategory<EtherProcessFactoryRecipe> {
    private final IDrawable icon;
    private final IRecipeType<EtherProcessFactoryRecipe> recipeType;
    private final Component title;

    public EtherProcessCategory(IGuiHelper guiHelper,
                                 IRecipeType<EtherProcessFactoryRecipe> recipeType,
                                 Component title,
                                 ItemStack iconStack) {
        this.recipeType = recipeType;
        this.title = title;
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                iconStack
        );
    }

    @Override
    public IRecipeType<EtherProcessFactoryRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public int getWidth() {
        return TreeLayout.WIDTH;
    }

    @Override
    public int getHeight() {
        return TreeLayout.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EtherProcessFactoryRecipe recipe, IFocusGroup focuses) {
        TreeLayout layout = TreeLayout.compute(recipe.json);

        for (TreeLayout.Entry e : layout.inputs) {
            builder.addInputSlot(e.x(), e.y())
                    .add(e.ingredient())
                    .setStandardSlotBackground();
        }
        for (TreeLayout.ChipEntry e : layout.chips) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, e.x(), e.y())
                    .add(e.ingredient())
                    .setStandardSlotBackground();
        }
        int outX = layout.outputX;
        for (var item : recipe.json.output().item()) {
            builder.addOutputSlot(outX, layout.outputY)
                    .add(item)
                    .setOutputSlotBackground();
            outX += TreeLayout.SLOT_SIZE + 2;
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, EtherProcessFactoryRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotDrawablesView slotsView = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> inputSlots = slotsView.getSlots(RecipeIngredientRole.INPUT);
        List<IRecipeSlotDrawable> chipSlots = slotsView.getSlots(RecipeIngredientRole.CRAFTING_STATION);
        List<IRecipeSlotDrawable> outputSlots = slotsView.getSlots(RecipeIngredientRole.OUTPUT);
        List<IRecipeSlotDrawable> allSlots = new ArrayList<>();
        allSlots.addAll(inputSlots);
        allSlots.addAll(chipSlots);
        allSlots.addAll(outputSlots);

        TreeLayout layout = TreeLayout.compute(recipe.json);

        List<Integer> worldXs = new ArrayList<>();
        List<Integer> worldYs = new ArrayList<>();
        int idx = 0;
        for (TreeLayout.Entry e : layout.inputs) {
            worldXs.add(e.x());
            worldYs.add(e.y());
            idx++;
        }
        for (TreeLayout.ChipEntry c : layout.chips) {
            worldXs.add(c.x());
            worldYs.add(c.y());
            idx++;
        }
        int outCount = recipe.json.output().item().size();
        for (int j = 0; j < outCount; j++) {
            worldXs.add(layout.outputX + j * (TreeLayout.SLOT_SIZE + 2));
            worldYs.add(layout.outputY);
        }

        ViewportTransform vt = new ViewportTransform(
                layout.canvasWidth, layout.canvasHeight,
                allSlots, worldXs, worldYs, layout.edges);
        builder.addSlottedWidget(vt, allSlots);
        builder.addInputHandler(vt);
    }

    @Override
    public void draw(EtherProcessFactoryRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
    }

    @Override
    public Codec<EtherProcessFactoryRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return EtherProcessFactoryRecipe.CODEC.codec();
    }

    @Override
    public @Nullable Identifier getIdentifier(EtherProcessFactoryRecipe recipe) {
        return null;
    }
}
