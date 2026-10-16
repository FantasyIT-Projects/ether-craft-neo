package studio.fantasyit.ether_craft.menu.factory.chip;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.factory.EtherProcessFactoryContainerMenu;
import studio.fantasyit.ether_craft.recipe.factory.PathNode;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;
import studio.fantasyit.ether_craft.util.UIUtil;

public class ChannelChipRenderer implements ChipSlotRenderer {
    /**
     * 箭头条带厚度：上/下取 16xN，左/右取 Nx16。
     */
    public static final int ARROW_STRIP = 3;

    private final ImageAsset background;
    private final ImageAsset icon;
    private final ImageAsset closedArrow;
    private final ImageAsset inArrow;
    private final ImageAsset outArrow;
    private final boolean alwaysPreview;
    private final Identifier chipId;

    public ChannelChipRenderer(Identifier texture, boolean alwaysPreview, Identifier chipId) {
        this.background = new ImageAsset(texture, 0, 0, 16, 16, 80, 16);
        this.icon = new ImageAsset(texture, 16, 0, 16, 16, 80, 16);
        this.closedArrow = new ImageAsset(texture, 32, 0, 16, 16, 80, 16);
        this.inArrow = new ImageAsset(texture, 48, 0, 16, 16, 80, 16);
        this.outArrow = new ImageAsset(texture, 64, 0, 16, 16, 80, 16);
        this.alwaysPreview = alwaysPreview;
        this.chipId = chipId;
    }

    @Override
    public boolean predicate(ItemStack itemStack) {
        return chipId.equals(itemStack.get(DataComponentRegistry.CHIP_ID));
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, EtherProcessFactoryContainerMenu menu,
                       int mouseX, int mouseY, int slotX, int slotY, int screenX, int screenY, ItemStack stack) {
        background.blit(graphics, slotX, slotY);

        ItemStack preview = previewOf(menu, slotX, slotY);
        if (!preview.isEmpty() && (alwaysPreview || ChipSlotRenderManager.isHoveringItemArea(mouseX, mouseY, screenX, screenY)))
            UIUtil.renderItemStackSlotPlaceholder(graphics, preview, slotX, slotY);
        else
            icon.blit(graphics, slotX, slotY);

        renderArrows(graphics, menu, slotX, slotY);
    }

    private ItemStack previewOf(EtherProcessFactoryContainerMenu menu, int slotX, int slotY) {
        Vector2i cell = menu.internalCellAt(slotX, slotY);
        if (cell == null) return ItemStack.EMPTY;
        return menu.entity.possibleIntermediateResults.getItem(cell.y * menu.entity.COLS + cell.x);
    }

    private void renderArrows(GuiGraphicsExtractor graphics, EtherProcessFactoryContainerMenu menu, int slotX, int slotY) {
        Vector2i cell = menu.internalCellAt(slotX, slotY);
        ChipChannelArrows.ArrowKind[] kinds = cell == null
                ? null
                : ChipChannelArrows.edges(menu.entity.pathBelongings, menu.entity.pathDepth, cell.y, cell.x);
        for (PathNode.Direction direction : PathNode.Direction.values()) {
            ChipChannelArrows.ArrowKind kind = kinds == null
                    ? ChipChannelArrows.ArrowKind.CLOSED
                    : kinds[direction.ordinal()];
            drawStrip(graphics, slotX, slotY, direction, switch (kind) {
                case IN -> inArrow;
                case OUT -> outArrow;
                case CLOSED -> closedArrow;
            });
        }
    }

    private void drawStrip(GuiGraphicsExtractor graphics, int slotX, int slotY,
                           PathNode.Direction direction, ImageAsset arrow) {
        switch (direction) {
            case UP -> arrow.blit(graphics, slotX, slotY, 0, 0, 16, ARROW_STRIP);
            case DOWN -> arrow.blit(graphics, slotX, slotY + 16 - ARROW_STRIP,
                    0, 16 - ARROW_STRIP, 16, ARROW_STRIP);
            case LEFT -> arrow.blit(graphics, slotX, slotY, 0, 0, ARROW_STRIP, 16);
            case RIGHT -> arrow.blit(graphics, slotX + 16 - ARROW_STRIP, slotY,
                    16 - ARROW_STRIP, 0, ARROW_STRIP, 16);
        }
    }
}
