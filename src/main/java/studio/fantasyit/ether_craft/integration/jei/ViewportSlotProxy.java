package studio.fantasyit.ether_craft.integration.jei;

import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleSupplier;
import java.util.stream.Stream;

@SuppressWarnings("removal")
class ViewportSlotProxy implements IRecipeSlotDrawable {
    private final IRecipeSlotDrawable delegate;
    private final DoubleSupplier panX;
    private final DoubleSupplier panY;
    private final DoubleSupplier zoom;

    ViewportSlotProxy(IRecipeSlotDrawable delegate, DoubleSupplier panX, DoubleSupplier panY, DoubleSupplier zoom) {
        this.delegate = delegate;
        this.panX = panX;
        this.panY = panY;
        this.zoom = zoom;
    }

    @Override
    public void draw(GuiGraphicsExtractor guiGraphics) {
        delegate.draw(guiGraphics);
    }

    @Override
    public void drawHoverOverlays(GuiGraphicsExtractor guiGraphics) {
        guiGraphics.pose().pushMatrix();
        //如果你不进行这个缩放，那么至少在
        guiGraphics.pose().translate((float) panX.getAsDouble(), (float) panY.getAsDouble());
        guiGraphics.pose().scale((float) zoom.getAsDouble(), (float) zoom.getAsDouble());
        delegate.drawHoverOverlays(guiGraphics);
        guiGraphics.pose().popMatrix();
    }

    @Override
    @Deprecated
    public List<Component> getTooltip() {
        return delegate.getTooltip();
    }

    @Override
    @Deprecated
    public void getTooltip(ITooltipBuilder tooltipBuilder) {
        delegate.getTooltip(tooltipBuilder);
    }

    @Override
    public void drawTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        delegate.drawTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return delegate.isMouseOver(mouseX, mouseY);
    }

    @Override
    public void setPosition(int x, int y) {
        delegate.setPosition(x, y);
    }

    @Override
    public IIngredientAcceptor<?> createDisplayOverrides() {
        return delegate.createDisplayOverrides();
    }

    @Override
    public void clearDisplayOverrides() {
        delegate.clearDisplayOverrides();
    }

    @Override
    public Rect2i getAreaIncludingBackground() {
        Rect2i world = delegate.getAreaIncludingBackground();
        double z = zoom.getAsDouble();
        double px = panX.getAsDouble();
        double py = panY.getAsDouble();
        int screenX = (int) (world.getX() * z + px);
        int screenY = (int) (world.getY() * z + py);
        int screenW = (int) (world.getWidth() * z);
        int screenH = (int) (world.getHeight() * z);
        return new Rect2i(screenX, screenY, screenW, screenH);
    }

    @Override
    public Stream<ITypedIngredient<?>> getAllIngredients() {
        return delegate.getAllIngredients();
    }

    @Override
    public List<@Nullable ITypedIngredient<?>> getAllIngredientsList() {
        return delegate.getAllIngredientsList();
    }

    @Override
    public Optional<ITypedIngredient<?>> getDisplayedIngredient() {
        return delegate.getDisplayedIngredient();
    }

    @Override
    public RecipeIngredientRole getRole() {
        return delegate.getRole();
    }

    @Override
    public void drawHighlight(GuiGraphicsExtractor guiGraphics, int color) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate((float) panX.getAsDouble(), (float) panY.getAsDouble());
        guiGraphics.pose().scale((float) zoom.getAsDouble(), (float) zoom.getAsDouble());
        delegate.drawHighlight(guiGraphics, color);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public Optional<String> getSlotName() {
        return delegate.getSlotName();
    }
}
