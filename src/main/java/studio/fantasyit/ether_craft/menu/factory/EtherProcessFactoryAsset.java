package studio.fantasyit.ether_craft.menu.factory;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.menu.base.ImageAsset;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeAsset;

public class EtherProcessFactoryAsset {
    public static Identifier BACKGROUND = Identifier.fromNamespaceAndPath(EtherCraft.MODID, "textures/gui/ether_process_factory.png");
    public static ImageAsset PLAYER_INV = ImageAsset.from4Point(BACKGROUND, 0, 0, 175, 89);
    public static ImageAsset MAIN_BG = ImageAsset.from4Point(BACKGROUND, 176, 0, 191, 15);
    public static ImageAsset BTN_NORMAL = ImageAsset.from4Point(BACKGROUND, 192, 0, 207, 15);
    public static ImageAsset BTN_HOVER = ImageAsset.gridOffset(BTN_NORMAL, 1, 0);
    public static ImageAsset BTN_DOWN = ImageAsset.gridOffset(BTN_NORMAL, 2, 0);
    public static ImageAsset BTN_DOWN_HOVER = ImageAsset.gridOffset(BTN_NORMAL, 3, 0);

    public static ImageAsset SLOT = ImageAsset.from4Point(BACKGROUND, 176, 16, 193, 33);

    public static ImageAsset ARROW = ImageAsset.from4Point(BACKGROUND, 194, 16, 207, 25);
    public static ImageAsset BAR = ImageAsset.from4Point(BACKGROUND, 194, 26, 211, 29);
    public static ImageAsset FILTER = ImageAsset.from4Point(BACKGROUND, 208, 16, 215, 24);

    public static final ImageAsset PENCIL_ON = EtherAdaptNodeAsset.PENCIL_ON;
    public static final ImageAsset PENCIL_OFF = EtherAdaptNodeAsset.PENCIL_OFF;
}
