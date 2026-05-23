package studio.fantasyit.ether_craft.block.node.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;

import java.util.ArrayList;
import java.util.List;


public class EtherAdapterNodeRenderState extends BlockEntityRenderState {
    EtherAdapterNodeAtlas.AtlasUV[] sides = new EtherAdapterNodeAtlas.AtlasUV[]{
            EtherAdapterNodeAtlas.BOTTOM,
            EtherAdapterNodeAtlas.TOP,
            EtherAdapterNodeAtlas.SIDE,
            EtherAdapterNodeAtlas.SIDE,
            EtherAdapterNodeAtlas.SIDE,
            EtherAdapterNodeAtlas.SIDE
    };
    int[] packedLightSides = new int[6];

    public BlockState blockState;

    List<EtherAdapterNodeAtlas.AtlasUV> overlays = new ArrayList<>();
    List<Direction> overlayDirections = new ArrayList<>();

    public int getNeighborLight(Level level, BlockPos pos) {
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);
        return LightCoordsUtil.pack(block, sky);
    }

    public void extractPackedLight(Level level, BlockPos pos, EtherAdaptNodeEntity blockEntity) {
        BlockPos.MutableBlockPos mutable = pos.mutable();
        for (int i = 0; i < sides.length; i++) {
            mutable.set(pos).move(Direction.values()[i]);
            if (level != null)
                packedLightSides[i] = getNeighborLight(level, mutable);
            else
                packedLightSides[i] = this.lightCoords;
        }
    }

    public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        int overlay = OverlayTexture.NO_OVERLAY;

        for (int i = 0; i < sides.length; i++) {
            EtherAdapterNodeAtlas.AtlasUV atlasUV = sides[i];
            int light = packedLightSides[i];
            if (atlasUV != null) {
                Direction dir = Direction.values()[i];
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RenderTypes.textPolygonOffset(atlasUV.atlas),
                        (pose, buffer) -> renderFace(
                                dir, pose, buffer, light, overlay,
                                atlasUV.u0, atlasUV.v0, atlasUV.u1, atlasUV.v1
                        )
                );
            }
        }

        for (int i = 0; i < overlays.size(); i++) {
            EtherAdapterNodeAtlas.AtlasUV atlasUV = overlays.get(i);
            Direction dir = overlayDirections.get(i);
            int light = packedLightSides[dir.ordinal()];
            submitNodeCollector.submitCustomGeometry(
                    poseStack,
                    RenderTypes.textPolygonOffset(atlasUV.atlas),
                    (pose, buffer) -> renderFace(
                            dir, pose, buffer, light, overlay,
                            atlasUV.u0, atlasUV.v0, atlasUV.u1, atlasUV.v1
                    )
            );
        }

        if (this.breakProgress != null) {
            BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(this.blockState);
            submitNodeCollector.submitBreakingBlockModel(poseStack, model, this.blockState.getSeed(this.blockPos), this.breakProgress.progress());
        }
    }

    private void renderFace(Direction direction,
                            PoseStack.Pose pose,
                            VertexConsumer buffer,
                            int light, int overlay,
                            float u0, float v0, float u1, float v1) {
        int color = ARGB.gray(CardinalLighting.DEFAULT.byFace(direction));

        switch (direction) {
            case DOWN -> { // Y=0, 法线 (0,-1,0)
                addQuadVertex(buffer, pose, 0, 0, 0, u0, v1, 0, -1, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 0, 0, u1, v1, 0, -1, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 0, 1, u1, v0, 0, -1, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 0, 1, u0, v0, 0, -1, 0, color, light, overlay);
            }
            case UP -> { // Y=1, 法线 (0,1,0)
                addQuadVertex(buffer, pose, 0, 1, 0, u0, v0, 0, 1, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 1, 1, u0, v1, 0, 1, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 1, 1, u1, v1, 0, 1, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 1, 0, u1, v0, 0, 1, 0, color, light, overlay);
            }
            case NORTH -> { // Z=0, 法线 (0,0,-1)
                addQuadVertex(buffer, pose, 0, 1, 0, u1, v0, 0, 0, -1, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 1, 0, u0, v0, 0, 0, -1, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 0, 0, u0, v1, 0, 0, -1, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 0, 0, u1, v1, 0, 0, -1, color, light, overlay);
            }
            case SOUTH -> { // Z=1, 法线 (0,0,1)
                addQuadVertex(buffer, pose, 1, 1, 1, u0, v0, 0, 0, 1, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 1, 1, u1, v0, 0, 0, 1, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 0, 1, u1, v1, 0, 0, 1, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 0, 1, u0, v1, 0, 0, 1, color, light, overlay);
            }
            case WEST -> { // X=0, 法线 (-1,0,0)
                addQuadVertex(buffer, pose, 0, 1, 1, u0, v0, -1, 0, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 1, 0, u1, v0, -1, 0, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 0, 0, u1, v1, -1, 0, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 0, 0, 1, u0, v1, -1, 0, 0, color, light, overlay);
            }
            case EAST -> { // X=1, 法线 (1,0,0)
                addQuadVertex(buffer, pose, 1, 1, 0, u0, v0, 1, 0, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 1, 1, u1, v0, 1, 0, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 0, 1, u1, v1, 1, 0, 0, color, light, overlay);
                addQuadVertex(buffer, pose, 1, 0, 0, u0, v1, 1, 0, 0, color, light, overlay);
            }
        }
    }

    private void addQuadVertex(VertexConsumer buffer, PoseStack.Pose pose,
                               float x, float y, float z,
                               float u, float v,
                               float nx, float ny, float nz,
                               int color, int light, int overlay) {
        buffer.addVertex(pose, x, y, z);
        buffer.setColor(color);
        buffer.setUv(u, v);
        buffer.setOverlay(overlay);
        buffer.setLight(light);
        buffer.setNormal(pose, nx, ny, nz);
    }

    public void setSideAtlas(Direction key, EtherAdapterNodeAtlas.AtlasUV apply) {
        sides[key.ordinal()] = apply;
    }

    public void addOverlay(Direction face, EtherAdapterNodeAtlas.AtlasUV atlasUV) {
        overlays.add(atlasUV);
        overlayDirections.add(face);
    }
}