package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;

public class EtherProcessFactoryScreen extends AbstractContainerScreen<@NotNull EtherProcessFactoryContainerMenu> {
    EtherProcessFactoryEntity be;
    private final Identifier background;
    int imageWidth;
    int imageHeight;
    public EtherProcessFactoryScreen(EtherProcessFactoryContainerMenu p_97741_, Inventory p_97742_, Component p_97743_) {
        super(p_97741_, p_97742_, p_97743_);
        background = Identifier.fromNamespaceAndPath(EtherCraft.MODID, "textures/gui/processor_gui.png");
        be = (EtherProcessFactoryEntity) p_97741_.entity;
        imageHeight = 256;
        imageWidth = 237;
        inventoryLabelY = imageHeight - 81;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        int y = 7;
        for(int i=0;i<be.processingRecipes.length;i++){
            graphics.text(font, Component.literal(String.valueOf((be.processingProgress[i]))), 350, y, 0xffffff);
            y+= 18;
        }
        graphics.text(font, Component.literal("Ether:"+be.getEther()), 100, 200, 0x000000);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int relX = (this.width - this.imageWidth) / 2;
        int relY = (this.height - this.imageHeight) / 2;
        graphics.blit(background, relX, relY, relX+imageWidth, relY+imageHeight, 0, 0, this.imageWidth, this.imageHeight);
    }
}
