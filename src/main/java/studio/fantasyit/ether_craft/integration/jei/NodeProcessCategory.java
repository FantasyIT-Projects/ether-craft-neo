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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.recipe.node.NodeProcessRecipe;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.List;

public class NodeProcessCategory implements IRecipeCategory<NodeProcessRecipe> {
    private static final int WIDTH = 116;
    private static final int HEIGHT = 54;
    private static final int SLOT_SIZE = 18;
    private static final int GRID_X = 1;
    private static final int GRID_Y = 1;
    private static final int ARROW_X = 60;
    private static final int ARROW_Y = 19;
    private static final int OUTPUT_X = 95;
    private static final int OUTPUT_Y = 19;
    private static final int TEXT_X = 55;
    private static final int TEXT_Y = 38;
    private static final int LINE_COLOR = 0xFFAAAAAA;

    private final IDrawable icon;

    public NodeProcessCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get())
        );
    }

    @Override
    public IRecipeType<NodeProcessRecipe> getRecipeType() {
        return JEIPlugin.NODE_PROCESS_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ether_craft.node_process");
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
    public void setRecipe(IRecipeLayoutBuilder builder, NodeProcessRecipe recipe, IFocusGroup focuses) {
        List<SizedIngredient> ingredients = recipe.ingredients;
        int count = Math.min(ingredients.size(), 9);

        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int x = GRID_X + col * SLOT_SIZE;
            int y = GRID_Y + row * SLOT_SIZE;

            var slot = builder.addInputSlot(x, y)
                    .setStandardSlotBackground();

            if (i < count) {
                SizedIngredient sized = ingredients.get(i);
                slot.add(sized.ingredient());
            }
        }

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .add(recipe.result.create())
                .setOutputSlotBackground();
    }

    @Override
    public void draw(NodeProcessRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        drawArrow(graphics);

        int cost = recipe.etherCost;
        if (cost > 0) {
            Font font = Minecraft.getInstance().font;
            Component text = Component.translatable("jei.ether_craft.node_process.ether_cost", cost)
                    .withStyle(ChatFormatting.DARK_AQUA);
            int textWidth = font.width(text);
            graphics.text(font, text, TEXT_X - textWidth / 2, TEXT_Y, 0xFFFFFFFF);
        }
    }

    @Override
    public Codec<NodeProcessRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return NodeProcessRecipe.CODEC.codec();
    }

    @Override
    public @Nullable Identifier getIdentifier(NodeProcessRecipe recipe) {
        return null;
    }

    private static void drawArrow(GuiGraphicsExtractor graphics) {
        int x = ARROW_X;
        int y = ARROW_Y + SLOT_SIZE / 2;
        int len = OUTPUT_X - ARROW_X - SLOT_SIZE + 1;
        graphics.fill(x, y - 1, x + len, y, LINE_COLOR);
        graphics.fill(x + len - 4, y - 3, x + len, y, LINE_COLOR);
        graphics.fill(x + len - 4, y + 1, x + len, y + 4, LINE_COLOR);
    }
}
