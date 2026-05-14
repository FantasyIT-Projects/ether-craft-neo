package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFilterFeature;
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureContainerInteract;

public class ContainerInteractScreen extends DirectionalFilterScreen {
    private IASwitchButton modeButton;

    public ContainerInteractScreen(AbstractDirectionalFilterFeature menuContext, EtherAdaptNodeScreen screen) {
        super(menuContext, screen);
    }

    @Override
    public void createWidget() {
        super.createWidget();
        FeatureContainerInteract plugin = (FeatureContainerInteract) context;
        modeButton = new IASwitchButton(
                lx(15), ly(104),
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                EtherAdaptNodeAsset.BTN_BLANK,
                EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                null,
                Component.translatable("ether_craft.gui.node.container_interact.extract"),
                Component.translatable("ether_craft.gui.node.container_interact.insert"),
                (down) -> {
                    ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                            FeatureContainerInteract.SYNC_EXTRACT_MODE,
                            plugin.installedId.id(),
                            down ? 1 : 0
                    ));
                    return true;
                }
        );
        modeButton.setDown(plugin.extractMode);
        screen.addRenderableWidget(modeButton);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractWidgetRenderState(graphics, mouseX, mouseY, a);
        FeatureContainerInteract plugin = (FeatureContainerInteract) context;
        ImageAsset icon = plugin.extractMode
                ? EtherAdaptNodeAsset.BTN_ICON_EXTRACT
                : EtherAdaptNodeAsset.BTN_ICON_INSERT;
        icon.blit(graphics, modeButton.getX(), modeButton.getY());
    }
}
