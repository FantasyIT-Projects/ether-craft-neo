package studio.fantasyit.ether_craft.menu.base;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class ImageAsset {
    public Identifier location;
    public int u, v, w, h, iw, ih;
    float u0, v0, u1, v1;

    public ImageAsset(Identifier location, int u, int v, int w, int h, int iw, int ih) {
        this.location = location;
        this.u = u;
        this.v = v;
        this.w = w;
        this.h = h;
        this.iw = iw;
        this.ih = ih;
        u0 = u / (float) iw;
        v0 = v / (float) ih;
        u1 = (u + w) / (float) iw;
        v1 = (v + h) / (float) ih;
    }

    public static ImageAsset from4Point(Identifier location, int u, int v, int u1, int v1) {
        return new ImageAsset(location, u, v, u1 - u + 1, v1 - v + 1);
    }

    public ImageAsset(Identifier location, int u, int v, int w, int h) {
        this(location, u, v, w, h, 256, 256);
    }

    public ImageAsset(Identifier location, int w, int h) {
        this(location, 0, 0, w, h);
    }

    public void blit(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blit(location, x, y, x + w, y + h, u0, u1, v0, v1);
    }
}
