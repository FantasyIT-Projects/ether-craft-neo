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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.factory.EtherProcessFactoryRecipe;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramLayout;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeDiagramSpec;
import studio.fantasyit.ether_craft.recipe.factory.render.data.TreeLayoutCalculator;

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
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ContextMap ctx = SlotDisplayContext.fromLevel(level);

        TreeLayout layout = TreeLayout.compute(recipe.json);

        for (TreeLayout.Entry e : layout.inputs) {
            builder.addInputSlot(e.x(), e.y())
                    .addItemStacks(TreeLayout.resolveSizedIngredient(e.ingredient(), ctx))
                    .setStandardSlotBackground();
        }
        for (TreeLayout.ChipEntry e : layout.chips) {
            builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, e.x(), e.y())
                    .addItemStacks(TreeLayout.resolveSizedIngredient(e.ingredient(), ctx))
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

        TreeDiagramSpec spec = TreeDiagramSpec.fromJson(recipe.json);
        TreeDiagramLayout layout = TreeLayoutCalculator.compute(spec);
        TreeLayout treeLayout = TreeLayout.compute(recipe.json);

        JEITreeSlottedWidget widget = new JEITreeSlottedWidget(layout, allSlots);

        int idx = 0;
        for (TreeLayout.Entry e : treeLayout.inputs) {
            if (idx < allSlots.size()) {
                IRecipeSlotDrawable slot = allSlots.get(idx);
                slot.setPosition(e.x(), e.y());
                widget.registerSlot(e.id(), slot);
                idx++;
            }
        }
        for (TreeLayout.ChipEntry c : treeLayout.chips) {
            if (idx < allSlots.size()) {
                IRecipeSlotDrawable slot = allSlots.get(idx);
                slot.setPosition(c.x(), c.y());
                widget.registerSlot(c.parentId() + "_" + idx, slot);
                idx++;
            }
        }
        int outX = treeLayout.outputX;
        for (int j = 0; j < recipe.json.output().item().size(); j++) {
            if (idx < allSlots.size()) {
                IRecipeSlotDrawable slot = allSlots.get(idx);
                slot.setPosition(outX, treeLayout.outputY + j * (TreeLayout.SLOT_SIZE_OUTPUT + 2));
                widget.registerSlot("output_" + j, slot);
                idx++;
            }
        }

        builder.addSlottedWidget(widget, allSlots);
        builder.addInputHandler(widget);
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
