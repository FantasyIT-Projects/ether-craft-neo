package studio.fantasyit.ether_craft.stream.client.extra;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import studio.fantasyit.ether_craft.stream.client.data.ClientStreamEntry;
import studio.fantasyit.ether_craft.stream.data.EtherStreamDisplayItemData;

public class EtherStreamDisplayItemLogic implements IEtherStreamExtraClientLogic {
    @Override
    public boolean shouldAttach(ClientStreamEntry entry) {
        return entry.getSyncedData(EtherStreamDisplayItemData.ID) != null;
    }

    @Override
    public void onRender(ClientStreamEntry stream, Vec3 currentPos, CameraRenderState camera, PoseStack poseStack, SubmitNodeCollector collector) {
        EtherStreamDisplayItemData syncedData = (EtherStreamDisplayItemData) stream.getSyncedData(EtherStreamDisplayItemData.ID);
        if (syncedData == null) return;
        ItemStack is = syncedData.itemStack();
        if (is.isEmpty()) return;

        float dx = (float) (currentPos.x() - camera.pos.x);
        float dy = (float) (currentPos.y() - camera.pos.y);
        float dz = (float) (currentPos.z() - camera.pos.z);
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = Minecraft.getInstance().level;

        if (level != null) {
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            poseStack.scale(0.2f, 0.2f, 0.2f);
            poseStack.mulPose(Axis.YP.rotationDegrees(stream.tickCount * 2));
            int lightCoords = LevelRenderer.getLightCoords(level, BlockPos.containing(currentPos));
            ItemStackRenderState state = new ItemStackRenderState();
            Minecraft.getInstance().getItemModelResolver().appendItemLayers(state, is, ItemDisplayContext.NONE, level, null, 0);
            state.submit(poseStack, collector, lightCoords, 0, 0);

            poseStack.popPose();
        }
    }

    @Override
    public boolean shouldRender(ClientStreamEntry entry) {
        EtherStreamDisplayItemData syncedData = (EtherStreamDisplayItemData) entry.getSyncedData(EtherStreamDisplayItemData.ID);
        if (syncedData == null) return true;
        ItemStack is = syncedData.itemStack();
        return is.isEmpty();
    }
}
