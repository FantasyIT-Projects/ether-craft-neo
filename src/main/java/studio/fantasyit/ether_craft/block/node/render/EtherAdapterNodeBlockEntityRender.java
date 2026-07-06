package studio.fantasyit.ether_craft.block.node.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.node.PluginRenderManager;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

import java.util.Map;

public class EtherAdapterNodeBlockEntityRender implements BlockEntityRenderer<EtherAdaptNodeEntity, EtherAdapterNodeRenderState> {

    public EtherAdapterNodeBlockEntityRender(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public EtherAdapterNodeRenderState createRenderState() {
        return new EtherAdapterNodeRenderState();
    }

    @Override
    public void extractRenderState(EtherAdaptNodeEntity blockEntity, EtherAdapterNodeRenderState state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        EtherAdapterNodeRenderState.extractBase(blockEntity, state, breakProgress);
        state.setLevel(blockEntity.getBlockState().getValueOrElse(EtherAdaptNodeBlock.LEVEL, 1));
        state.blockState = blockEntity.getBlockState();
        state.extractPackedLight(blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity);
        Direction fd = blockEntity.getBlockState().getValueOrElse(EtherAdaptNodeBlock.FACING, Direction.NORTH);
        if (blockEntity.functionPlugin != null && Direction.Plane.HORIZONTAL.test(fd))
            PluginRenderManager.Instance.render(fd, blockEntity.functionPlugin, blockEntity, state);
        for (Map.Entry<Direction, InstalledPlugin> s : blockEntity.featureAttachedDirection.entrySet()) {
            PluginRenderManager.Instance.render(s.getKey(), s.getValue(), blockEntity, state);
        }
    }

    @Override
    public void submit(EtherAdapterNodeRenderState state, PoseStack poseStack, @NotNull SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        state.submit(poseStack, submitNodeCollector, camera);
    }
}
