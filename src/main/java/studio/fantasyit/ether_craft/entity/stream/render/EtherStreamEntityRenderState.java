package studio.fantasyit.ether_craft.entity.stream.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;

public class EtherStreamEntityRenderState extends EntityRenderState {
    public final double[] tailX;
    public final double[] tailY;
    public final double[] tailZ;
    public final float[] tailSize;
    public int tailCount;

    public EtherStreamEntityRenderState() {
        tailX = new double[EtherStreamEntity.MAX_TAIL];
        tailY = new double[EtherStreamEntity.MAX_TAIL];
        tailZ = new double[EtherStreamEntity.MAX_TAIL];
        tailSize = new float[EtherStreamEntity.MAX_TAIL];
    }
}
