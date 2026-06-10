package studio.fantasyit.ether_craft.integration.jei;

import com.mojang.serialization.Codec;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.ICodecHelper;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.data.PlatingEffectFormula;
import studio.fantasyit.ether_craft.recipe.plating.PlatingRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.List;

public class PlatingCategory implements IRecipeCategory<PlatingRecipe> {
    private static final int WIDTH = 130;
    private static final int HEIGHT = 58;
    private static final int SLOT_SIZE = 18;
    private static final int INPUT_START_X = 1;
    private static final int INPUT_Y = 1;
    private static final int MAX_INPUTS = 5;
    private static final int OUTPUT_X = 105;
    private static final int OUTPUT_Y = 1;
    private static final int TEXT_X = 4;
    private static final int TEXT_Y = 24;
    private static final int FORMULA_Y = 38;
    private static final int LINE_COLOR = 0xFFAAAAAA;

    private final IDrawable icon;

    public PlatingCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ItemRegistry.ETHER_STREAM_EMITTER_ITEM.get())
        );
    }

    @Override
    public IRecipeType<PlatingRecipe> getRecipeType() {
        return JEIPlugin.PLATING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ether_craft.plating");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, PlatingRecipe recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        ContextMap ctx = SlotDisplayContext.fromLevel(level);

        List<SizedIngredient> ingredients = recipe.input;
        int count = Math.min(ingredients.size(), MAX_INPUTS);

        for (int i = 0; i < count; i++) {
            int x = INPUT_START_X + i * (SLOT_SIZE + 1);
            SizedIngredient sized = ingredients.get(i);
            builder.addInputSlot(x, INPUT_Y)
                    .addItemStacks(TreeLayout.resolveSizedIngredient(sized, ctx))
                    .setStandardSlotBackground();
        }

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .add(recipe.filter.display().resolveForFirstStack(ctx))
                .setOutputSlotBackground();
    }

    @Override
    public void draw(PlatingRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        List<SizedIngredient> ingredients = recipe.input;
        int count = Math.min(ingredients.size(), MAX_INPUTS);
        if (count > 0) {
            int lastSlotX = INPUT_START_X + (count - 1) * (SLOT_SIZE + 1);
            int arrowStartX = lastSlotX + SLOT_SIZE;
            int arrowEndX = OUTPUT_X;
            int arrowY = INPUT_Y + SLOT_SIZE / 2;
            drawArrow(graphics, arrowStartX, arrowEndX, arrowY);
        }

        Font font = Minecraft.getInstance().font;
        String effectKey = "jei.ether_craft.plating.effect." + recipe.effectId.getNamespace() + "." + recipe.effectId.getPath();
        Component text = Component.translatable(effectKey).withStyle(ChatFormatting.DARK_AQUA);
        int availableTextWidth = WIDTH - TEXT_X - 4;
        graphics.textWithWordWrap(font, text, TEXT_X, TEXT_Y, availableTextWidth, 0xFFFFFFFF);

        PlatingEffectFormula formula = recipe.values;
        Component formulaText = Component.translatable("jei.ether_craft.plating.formula",
                String.format("%.0f", formula.a1()),
                String.format("%.0f", formula.a2()),
                String.format("%.2f", formula.a3()),
                String.format("%.2f", formula.a4())
        ).withStyle(ChatFormatting.GRAY);
        graphics.textWithWordWrap(font, formulaText, TEXT_X, FORMULA_Y, availableTextWidth, 0xFFAAAAAA);
    }

    @Override
    public Codec<PlatingRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return PlatingRecipe.CODEC.codec();
    }

    @Override
    public @Nullable Identifier getIdentifier(PlatingRecipe recipe) {
        return null;
    }

    private static void drawArrow(GuiGraphicsExtractor graphics, int startX, int endX, int y) {
        int len = endX - startX;
        graphics.fill(startX, y - 1, startX + len, y, LINE_COLOR);
        graphics.fill(startX + len - 4, y - 3, startX + len, y, LINE_COLOR);
        graphics.fill(startX + len - 4, y + 1, startX + len, y + 4, LINE_COLOR);
    }
}
