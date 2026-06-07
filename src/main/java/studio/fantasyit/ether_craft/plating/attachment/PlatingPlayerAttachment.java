package studio.fantasyit.ether_craft.plating.attachment;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class PlatingPlayerAttachment {
    public boolean soulActive;
    @Nullable
    public Vec3 soulCameraPos;
    public long lastOnGroundTick;

    public PlatingPlayerAttachment() {
        this.soulActive = false;
        this.soulCameraPos = null;
        this.lastOnGroundTick = -100;
    }
}
