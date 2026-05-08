package studio.fantasyit.ether_craft.block.node;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public class EtherAdapterNodeBlockEntityRender implements BlockEntityRenderer<EtherAdaptNodeEntity, EtherAdapterNodeRenderState> {
    @Override
    public EtherAdapterNodeRenderState createRenderState() {
        return new EtherAdapterNodeRenderState();
    }

    @Override
    public void extractRenderState(EtherAdaptNodeEntity blockEntity, EtherAdapterNodeRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

    }

    @Override
    public void submit(EtherAdapterNodeRenderState state, PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
//        submitNodeCollector.submit();
    }
}
