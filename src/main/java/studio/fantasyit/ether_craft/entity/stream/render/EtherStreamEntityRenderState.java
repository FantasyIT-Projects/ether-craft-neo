package studio.fantasyit.ether_craft.entity.stream.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.entity.stream.EtherStreamEntity;

public class EtherStreamEntityRenderState extends EntityRenderState {
    public final double[] tailX;
    public final double[] tailY;
    public final double[] tailZ;
    public final float[] tailSize;
    public int tailCount;

    @Nullable
    public Component label;
    @Nullable
    public Vec3 startPos;
    public Vec3 motion = Vec3.ZERO;
    public int labelColor = 0xFFFFFFFF;
    public boolean dying;
    public int deathTick;
    public float speed;
    @Nullable
    public Vec3 deathPos;

    public EtherStreamEntityRenderState() {
        tailX = new double[EtherStreamEntity.MAX_TAIL];
        tailY = new double[EtherStreamEntity.MAX_TAIL];
        tailZ = new double[EtherStreamEntity.MAX_TAIL];
        tailSize = new float[EtherStreamEntity.MAX_TAIL];
    }
}
