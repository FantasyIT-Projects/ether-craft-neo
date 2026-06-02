package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import studio.fantasyit.ether_craft.network.c2s.UncarryC2S;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.data.EtherStreamCarryingEntityData;

public class EtherStreamCarriedEntityLogic implements IEtherStreamExtraClientLogic {
    @Override
    public boolean shouldDelayDeath(ClientStreamEntry entry) {
        return true;
    }

    @Override
    public void onTick(ClientStreamEntry entry) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer localPlayer = mc.player;
        if (localPlayer == null) return;

        EtherStreamCarryingEntityData data = (EtherStreamCarryingEntityData)
                entry.getSyncedData(EtherStreamCarryingEntityData.ID);
        if (data == null) return;
        if (!data.entityUUID().equals(localPlayer.getUUID())) return;
        if (entry.isDying || entry.removed) return;

        Vec3 currentPos = entry.getCurrentPosition();
        localPlayer.setPos(currentPos.x, currentPos.y - localPlayer.getEyeHeight(), currentPos.z);

        if (mc.options.keyShift.isDown()) {
            ClientPacketDistributor.sendToServer(new UncarryC2S(data.posDir(), data.streamId()));
        }
    }

    @Override
    public void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera,
                         PoseStack poseStack, SubmitNodeCollector collector) {
    }
}
