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
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.register.ItemRegistry;

public class NodePluginInfoCategory implements IRecipeCategory<NodePluginInfoRecipe> {
    private static final int WIDTH = 140;
    private static final int HEIGHT = 56;
    private static final int ITEM_X = 4;
    private static final int TEXT_X = 28;
    private static final int LINE1_Y = 6;
    private static final int LINE2_Y = 20;

    private final IDrawable icon;

    public NodePluginInfoCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK,
                new ItemStack(ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get())
        );
    }

    @Override
    public IRecipeType<NodePluginInfoRecipe> getRecipeType() {
        return JEIPlugin.NODE_PLUGIN_INFO_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.ether_craft.node_plugin_info");
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
    public void setRecipe(IRecipeLayoutBuilder builder, NodePluginInfoRecipe recipe, IFocusGroup focuses) {
        int itemY = (HEIGHT - 18) / 2;
        builder.addInputSlot(ITEM_X, itemY)
                .add(new ItemStack(recipe.icon()))
                .setStandardSlotBackground();
    }

    @Override
    public void draw(NodePluginInfoRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        String typeKey = "jei.ether_craft.plugin_type." + recipe.pluginType().getSerializedName();
        ChatFormatting color = pluginTypeColor(recipe.pluginType());

        Component typeLabel = Component.translatable(typeKey).withStyle(color);
        Component idLine = Component.literal(recipe.pluginId().toString()).withStyle(ChatFormatting.GRAY);

        String descKey = "jei.ether_craft.plugin." + recipe.pluginId().getNamespace() + "." + recipe.pluginId().getPath();
        Component desc = Component.translatable(descKey).withStyle(ChatFormatting.WHITE);

        int typeWidth = font.width(typeLabel);
        int availableTextWidth = WIDTH - TEXT_X - 4;

        graphics.text(font, typeLabel, TEXT_X, LINE1_Y, 0xFFFFFFFF);

        graphics.textWithWordWrap(font, desc, TEXT_X, LINE2_Y, availableTextWidth, 0xffffffff);
    }

    @Override
    public Codec<NodePluginInfoRecipe> getCodec(ICodecHelper codecHelper, IRecipeManager recipeManager) {
        return NodePluginInfoRecipe.CODEC;
    }

    @Override
    public @Nullable Identifier getIdentifier(NodePluginInfoRecipe recipe) {
        return recipe.pluginId();
    }

    private static ChatFormatting pluginTypeColor(NodePluginManager.PluginType type) {
        return switch (type) {
            case FUNCTION -> ChatFormatting.GOLD;
            case FEATURE -> ChatFormatting.BLUE;
            case UPGRADE -> ChatFormatting.GREEN;
            case DUMMY -> ChatFormatting.GRAY;
        };
    }

    private static java.util.List<String> wrapText(Font font, String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        for (String word : words) {
            String testLine = currentLine.isEmpty() ? word : currentLine + " " + word;
            if (font.width(testLine) <= maxWidth) {
                if (!currentLine.isEmpty()) currentLine.append(" ");
                currentLine.append(word);
            } else {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                }
                currentLine = new StringBuilder(word);
            }
        }
        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }
}
