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
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.node.NodePluginManager;
import studio.fantasyit.ether_craft.node.tip.NodePluginTipManager;
import studio.fantasyit.ether_craft.node.tip.TipInfo;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.Optional;

public class NodePluginInfoCategory implements IRecipeCategory<NodePluginInfoRecipe> {
    private static final int WIDTH = 140;
    private static final int HEIGHT = 56;
    private static final int ITEM_X = 4;
    private static final int TEXT_X = 28;
    private static final int LINE1_Y = 6;
    private static final int LINE2_Y = 20;
    private static final int LINE3_Y = 34;

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
        Optional<TipInfo> tipOpt = NodePluginTipManager.INSTANCE.getTip(recipe.pluginId());
        if (tipOpt.isPresent()) {
            TipInfo tip = tipOpt.get();
            int itemY = (HEIGHT - 18) / 2;

            var inputSlot = builder.addInputSlot(ITEM_X, itemY);
            for (Ingredient ingredient : tip.availableIngredients()) {
                inputSlot.add(ingredient);
            }
            inputSlot.setStandardSlotBackground();

            var outputSlot = builder.addOutputSlot(ITEM_X + 22, itemY);
            for (ItemStack stack : tip.producibleItems()) {
                outputSlot.add(stack);
            }
            outputSlot.setStandardSlotBackground();
        } else {
            int itemY = (HEIGHT - 18) / 2;
            builder.addInputSlot(ITEM_X, itemY)
                    .add(new ItemStack(recipe.icon()))
                    .setStandardSlotBackground();
        }
    }

    @Override
    public void draw(NodePluginInfoRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphicsExtractor graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;

        Optional<TipInfo> tipOpt = NodePluginTipManager.INSTANCE.getTip(recipe.pluginId());

        String typeKey = "jei.ether_craft.plugin_type." + recipe.pluginType().getSerializedName();
        ChatFormatting color = pluginTypeColor(recipe.pluginType());

        Component typeLabel = Component.translatable(typeKey).withStyle(color);
        Component idLine = Component.literal(recipe.pluginId().toString()).withStyle(ChatFormatting.GRAY);

        String descKey = "jei.ether_craft.plugin." + recipe.pluginId().getNamespace() + "." + recipe.pluginId().getPath().replace('/', '.');
        Component desc = Component.translatable(descKey).withStyle(ChatFormatting.WHITE);

        int textX = tipOpt.isPresent() ? 50 : TEXT_X;
        int availableTextWidth = WIDTH - textX - 4;

        int typeWidth = font.width(typeLabel);
        graphics.text(font, typeLabel, textX, LINE1_Y, 0xFFFFFFFF);

        graphics.textWithWordWrap(font, desc, textX, LINE2_Y, availableTextWidth, 0xffffffff);

        if (tipOpt.isPresent()) {
            TipInfo tip = tipOpt.get();
            if (!tip.concepts().isEmpty()) {
                Component conceptsLabel = Component.translatable("jei.ether_craft.node_plugin_info.concepts")
                        .withStyle(ChatFormatting.GRAY);
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (var concept : tip.concepts()) {
                    if (!first) sb.append(", ");
                    first = false;
                    sb.append(Component.translatable("tip.ether_craft.concept." + concept.getSerializedName()).getString());
                }
                Component conceptsText = Component.literal(sb.toString()).withStyle(ChatFormatting.AQUA);
                conceptsLabel = conceptsLabel.copy().append(" ").append(conceptsText);
                graphics.textWithWordWrap(font, conceptsLabel, textX, LINE3_Y, availableTextWidth, 0xffffffff);
            }
        }
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
}
