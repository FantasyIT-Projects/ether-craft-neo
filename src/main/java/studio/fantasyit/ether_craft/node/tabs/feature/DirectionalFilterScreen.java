package studio.fantasyit.ether_craft.node.tabs.feature;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.joml.Vector2i;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.base.widget.IASwitchButton;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeScreen;
import studio.fantasyit.ether_craft.menu.node.ScreenMenuSyncer;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegClient;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.PluginMenuContext;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFeature;
import studio.fantasyit.ether_craft.node.plugins.feature.AbstractDirectionalFilterFeature;
import studio.fantasyit.ether_craft.node.tabs.BaseEtherNodeTabWidgetProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DirectionalFilterScreen extends BaseEtherNodeTabWidgetProvider<AbstractDirectionalFilterFeature> {

    private static final Map<Direction, Vector2i> DIRECTION_POSITION = Map.of(
            Direction.UP, new Vector2i(28, 31),
            Direction.NORTH, new Vector2i(28, 12),
            Direction.SOUTH, new Vector2i(28, 50),
            Direction.EAST, new Vector2i(48, 31),
            Direction.WEST, new Vector2i(8, 31),
            Direction.DOWN, new Vector2i(68, 31)
    );
    private static final Map<Direction, ImageAsset> DIRECTION_ICON = Map.of(
            Direction.UP, EtherAdaptNodeAsset.BTN_ICON_U,
            Direction.NORTH, EtherAdaptNodeAsset.BTN_ICON_N,
            Direction.SOUTH, EtherAdaptNodeAsset.BTN_ICON_S,
            Direction.EAST, EtherAdaptNodeAsset.BTN_ICON_E,
            Direction.WEST, EtherAdaptNodeAsset.BTN_ICON_W,
            Direction.DOWN, EtherAdaptNodeAsset.BTN_ICON_D
    );
    private Map<Direction, IASwitchButton> directionButton;

    public DirectionalFilterScreen(PluginMenuContext<AbstractDirectionalFilterFeature> context, EtherAdaptNodeScreen screen) {
        super(context, screen);
        screen.registerMenuSyncer(new ScreenMenuSyncer<>(() -> context.plugin.direction, v -> {
            for (Direction d : Direction.values()) {
                if (directionButton.get(d).isDown()) {
                    directionButton.get(d).setDown(false);
                }
            }
            if (v != null)
                directionButton.get(v).setDown(true);
        }));
    }

    @Override
    public void createWidget() {
        directionButton = new HashMap<>();
        for (Direction direction : Direction.values()) {
            IASwitchButton button = new IASwitchButton(
                    lx(DIRECTION_POSITION.get(direction).x),
                    ly(DIRECTION_POSITION.get(direction).y),
                    EtherAdaptNodeAsset.BTN_BLANK,
                    EtherAdaptNodeAsset.BTN_BLANK_HOVER,
                    EtherAdaptNodeAsset.BTN_BLANK_DOWN,
                    EtherAdaptNodeAsset.BTN_BLANK_DOWN_HOVER,
                    null,
                    Component.translatable("menu.ether_craft.node.directional_filter.direction." + direction.getName().toLowerCase()),
                    Component.translatable("menu.ether_craft.node.directional_filter.cancel"),
                    (b) -> {
                        if (!b) return trySelectBtn(direction);
                        plugin.direction = null;
                        ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                                AbstractDirectionalFeature.SYNC_DIRECTION,
                                plugin.installedId.id(),
                                -1
                        ));
                        return true;
                    }
            );
            button.setDown(direction.equals(this.plugin.direction));
            directionButton.put(direction, button);
            this.screen.addRenderableWidget(button);
        }
        FilterGuiRegClient.widget(screen, () -> plugin.filter.whitelist, AbstractDirectionalFilterFeature.FILTER_PREFIX);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Map<Direction, InstalledPlugin> featureAttachedDirection = screen.getMenu().entity.featureAttachedDirection;
        for (Direction direction : Direction.values()) {
            if (featureAttachedDirection.containsKey(direction)) {
                ItemStack icon = screen.getMenu().entity.getItemByInstalled(featureAttachedDirection.get(direction));
                graphics.item(icon, lx(DIRECTION_POSITION.get(direction).x), ly(DIRECTION_POSITION.get(direction).y));
            } else {
                DIRECTION_ICON.get(direction).blit(graphics, lx(DIRECTION_POSITION.get(direction).x), ly(DIRECTION_POSITION.get(direction).y));
            }
        }
    }

    private boolean trySelectBtn(Direction direction) {
        if (directionButton == null)
            return false;
        Map<Direction, InstalledPlugin> featureAttachedDirection = Objects.requireNonNull(screen.getMenu().entity).featureAttachedDirection;
        if (featureAttachedDirection.containsKey(direction)) {
            return false;
        }
        for (Direction d : Direction.values()) {
            if (directionButton.get(d).isDown()) {
                directionButton.get(d).setDown(false);
            }
        }
        plugin.direction = direction;
        ClientPacketDistributor.sendToServer(new SyncScreenDataC2S(
                AbstractDirectionalFeature.SYNC_DIRECTION,
                plugin.installedId.id(),
                direction.ordinal()
        ));
        return true;
    }
}
