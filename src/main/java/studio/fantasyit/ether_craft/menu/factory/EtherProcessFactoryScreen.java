package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;
import studio.fantasyit.ether_craft.util.UIUtil;

import java.util.List;

public class EtherProcessFactoryScreen extends AbstractContainerScreen<@NotNull EtherProcessFactoryContainerMenu> {
    EtherProcessFactoryEntity be;
    private final Identifier background;

    public EtherProcessFactoryScreen(EtherProcessFactoryContainerMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_, 237, 256);
        background = Identifier.fromNamespaceAndPath(EtherCraft.MODID, "textures/gui/processor_gui.png");
        be = (EtherProcessFactoryEntity) p_97741_.entity;
        inventoryLabelY = imageHeight - 81;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.text(font, Component.literal("Ether:" + be.getEther()), getLeftPos() + 5, getTopPos() + 200, 0xffffffff);
        graphics.text(font, Component.literal("Spd:" + be.pressureBonus), getLeftPos() + 5, getTopPos() + 220, 0xffffffff);
        graphics.text(font, Component.literal("Leak:" + be.leak), getLeftPos() + 5, getTopPos() + 240, 0xffffffff);

        for (int i = 0; i < EtherProcessFactoryEntity.ROWS; i++) {
            for (int j = 0; j < EtherProcessFactoryEntity.COLS; j++) {
                ItemStack chipItem = be.internalContainer.getItem(i * EtherProcessFactoryEntity.COLS + j);
                EtherProcessChipManager.ProcessChipRecord r = EtherProcessChipManager.get(chipItem);
                if (r == null) continue;
                int ether = be.currentEther[i][j];
                int color = 0xff26c6da; //RGB#f57f17
                if (ether >= r.maxEther() - r.etherConsume() * 2)
                    color = 0xff81c784; //RGB#81c784
                if (ether < r.etherConsume())
                    color = 0xffe65100;
                if (ether < r.etherRequire())
                    color = 0xffff3d00;

                graphics.fill(
                        getLeftPos() + 38 + j * 18 + 2,
                        getTopPos() + 6 + i * 18 + 2,
                        getLeftPos() + 38 + j * 18 + 5,
                        getTopPos() + 6 + i * 18 + 5,
                        color
                );
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(background, getLeftPos(), getTopPos(), getLeftPos() + imageWidth, getTopPos() + imageHeight, 0, this.imageWidth / 255.0F, 0, this.imageHeight / 255.0F);
        for (int i = 0; i < be.processingInputs.length; i++) {
            int progress = be.processingProgress[i];
            if (progress == 0) continue;
            int progressRealWidth = (int) (1.0 * progress / EtherProcessFactoryEntity.MAX_PROGRESS * 18 * EtherProcessFactoryEntity.COLS);
            for (int j = 0; j < EtherProcessFactoryEntity.COLS; j++) {
                for (int k = 0; k < EtherProcessFactoryEntity.ROWS; k++) {
                    if (be.pathBelongings[k][j] != i) continue;
                    int fillWid = Math.min(Math.max(0, progressRealWidth - j * 18), 18);
                    graphics.fill(
                            getLeftPos() + 38 + j * 18,
                            getTopPos() + 6 + k * 18,
                            getLeftPos() + 38 + j * 18 + fillWid,
                            getTopPos() + 6 + k * 18 + 18,
                            0x80c5e1a5
                    );
                }
            }
        }
        for (int i = 0; i < be.processingRecipes.length; i++) {
            ItemStack it = be.possibleResults.getItem(i);
            if (it.isEmpty()) continue;
            if (be.outputContainer.getItem(i).isEmpty())
                UIUtil.renderItemStackSlotPlaceholder(graphics, it, getLeftPos() + 206, getTopPos() + 6 + i * 18);
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.menu.getCarried().isEmpty())
            if (hoveredSlot != null && hoveredSlot.hasItem() && menu.internalSlotMapping.containsKey(hoveredSlot.index)) {
                Vector2i v = menu.internalSlotMapping.get(hoveredSlot.index);
                ItemStack item = this.hoveredSlot.getItem();
                List<Component> oTooltip = this.getTooltipFromContainerItem(item);
                oTooltip.add(Component.translatable("tooltip.ether_craft.process_chip_ether", be.currentEther[v.y][v.x]).withStyle(ChatFormatting.BOLD));
                graphics.setTooltipForNextFrame(this.font, oTooltip, item.getTooltipImage(), item, mouseX, mouseY, item.get(DataComponents.TOOLTIP_STYLE));
                return;
            }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
