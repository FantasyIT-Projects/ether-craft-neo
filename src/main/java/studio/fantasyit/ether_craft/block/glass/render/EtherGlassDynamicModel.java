package studio.fantasyit.ether_craft.block.glass.render;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.DynamicBlockStateModel;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class EtherGlassDynamicModel implements DynamicBlockStateModel {
    private static final Map<Direction, Pair> FACE_LOCAL_DIRS = new EnumMap<>(Direction.class);

    static {
        FACE_LOCAL_DIRS.put(Direction.UP, new Pair(Direction.SOUTH, Direction.EAST));
        FACE_LOCAL_DIRS.put(Direction.DOWN, new Pair(Direction.SOUTH, Direction.EAST));
        FACE_LOCAL_DIRS.put(Direction.NORTH, new Pair(Direction.UP, Direction.WEST));
        FACE_LOCAL_DIRS.put(Direction.SOUTH, new Pair(Direction.UP, Direction.EAST));
        FACE_LOCAL_DIRS.put(Direction.WEST, new Pair(Direction.UP, Direction.SOUTH));
        FACE_LOCAL_DIRS.put(Direction.EAST, new Pair(Direction.UP, Direction.NORTH));
    }

    private final Material.Baked particleMaterial;
    private final TextureAtlasSprite[] s;

    public EtherGlassDynamicModel(TextureAtlasSprite[] bg) {
        this.s = bg;
        this.particleMaterial = new Material.Baked(bg[0], true);
    }

    @Override
    public void collectParts(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random, List<BlockStateModelPart> parts) {
        for (Direction face : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(face));
            if (neighbor.is(state.getBlock())) {
                continue;
            }

            Pair local = FACE_LOCAL_DIRS.get(face);
            Direction faceTop = local.top;
            Direction faceBottom = faceTop.getOpposite();
            Direction faceLeft = local.left;
            Direction faceRight = faceLeft.getOpposite();

            boolean topConn = level.getBlockState(pos.relative(faceTop)).is(state.getBlock());
            boolean botConn = level.getBlockState(pos.relative(faceBottom)).is(state.getBlock());
            boolean leftConn = level.getBlockState(pos.relative(faceLeft)).is(state.getBlock());
            boolean rightConn = level.getBlockState(pos.relative(faceRight)).is(state.getBlock());

            List<BakedQuad> quads = new ArrayList<>();

            int idx = !topConn ? 1 : 0;
            idx = (idx << 1) | (!botConn ? 1 : 0);
            idx = (idx << 1) | (!rightConn ? 1 : 0);
            idx = (idx << 1) | (!leftConn ? 1 : 0);
            quads.add(buildQuad(face, 0, 0, 1, 1, s[idx]));
            parts.add(new FacePart(quads, face));
        }
    }


    private static BakedQuad buildQuad(Direction face, float u0, float v0, float u1, float v1, TextureAtlasSprite sprite) {
        BakedQuad.MaterialInfo matInfo = new BakedQuad.MaterialInfo(
                sprite,
                ChunkSectionLayer.TRANSLUCENT,
                Sheets.translucentBlockSheet(),
                -1, false, 0, false
        );

        return switch (face) {
            case DOWN -> new BakedQuad(
                    new Vector3f(u0, 0, 1 - v0), new Vector3f(u0, 0, 1 - v1),
                    new Vector3f(u1, 0, 1 - v1), new Vector3f(u1, 0, 1 - v0),
                    packUV(sprite, 1 - u0, v0),
                    packUV(sprite, 1 - u0, v1),
                    packUV(sprite, 1 - u1, v1),
                    packUV(sprite, 1 - u1, v0),
                    face, matInfo);
            case UP -> new BakedQuad(
                    new Vector3f(u0, 1, 1 - v0), new Vector3f(u1, 1, 1 - v0),
                    new Vector3f(u1, 1, 1 - v1), new Vector3f(u0, 1, 1 - v1),
                    packUV(sprite, u0, v0),
                    packUV(sprite, u1, v0),
                    packUV(sprite, u1, v1),
                    packUV(sprite, u0, v1),
                    face, matInfo);
            case NORTH -> new BakedQuad(
                    new Vector3f(1 - u0, 1 - v0, 0), new Vector3f(1 - u0, 1 - v1, 0),
                    new Vector3f(1 - u1, 1 - v1, 0), new Vector3f(1 - u1, 1 - v0, 0),
                    packUV(sprite, u0, v0),
                    packUV(sprite, u0, v1),
                    packUV(sprite, u1, v1),
                    packUV(sprite, u1, v0),
                    face, matInfo);
            case SOUTH -> new BakedQuad(
                    new Vector3f(u0, 1 - v0, 1), new Vector3f(u0, 1 - v1, 1),
                    new Vector3f(u1, 1 - v1, 1), new Vector3f(u1, 1 - v0, 1),
                    packUV(sprite, u0, v0),
                    packUV(sprite, u0, v1),
                    packUV(sprite, u1, v1),
                    packUV(sprite, u1, v0),
                    face, matInfo);
            case WEST -> new BakedQuad(
                    new Vector3f(0, 1 - v0, u0), new Vector3f(0, 1 - v1, u0),
                    new Vector3f(0, 1 - v1, u1), new Vector3f(0, 1 - v0, u1),
                    packUV(sprite, u0, v0),
                    packUV(sprite, u0, v1),
                    packUV(sprite, u1, v1),
                    packUV(sprite, u1, v0),
                    face, matInfo);
            case EAST -> new BakedQuad(
                    new Vector3f(1, 1 - v0, 1 - u0), new Vector3f(1, 1 - v1, 1 - u0),
                    new Vector3f(1, 1 - v1, 1 - u1), new Vector3f(1, 1 - v0, 1 - u1),
                    packUV(sprite, u0, v0),
                    packUV(sprite, u0, v1),
                    packUV(sprite, u1, v1),
                    packUV(sprite, u1, v0),
                    face, matInfo);
        };
    }

    private static long packUV(TextureAtlasSprite sprite, float u, float v) {
        float atlasU = sprite.getU(u);
        float atlasV = sprite.getV(v);
        return (long) Float.floatToRawIntBits(atlasU) << 32 | (long) Float.floatToRawIntBits(atlasV) & 0xFFFFFFFFL;
    }

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        String[] keys = new String[6];
        for (Direction dir : Direction.values()) {
            keys[dir.ordinal()] = level.getBlockState(pos.relative(dir)).getBlock().getDescriptionId();
        }
        return keys;
    }

    @Override
    public Material.Baked particleMaterial() {
        return particleMaterial;
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return particleMaterial;
    }

    @Override
    public @BakedQuad.MaterialFlags int materialFlags() {
        return BakedQuad.FLAG_TRANSLUCENT;
    }

    /**
     * Simple BlockStateModelPart for one face with pre-built quads.
     */
    private record FacePart(List<BakedQuad> quads, Direction face) implements BlockStateModelPart {
        @Override
        public List<BakedQuad> getQuads(@Nullable Direction side) {
            if (side == null || side == face) {
                return quads;
            }
            return List.of();
        }

        @Override
        public boolean useAmbientOcclusion() {
            return false;
        }

        @Override
        public Material.Baked particleMaterial() {
            return new Material.Baked(quads.getFirst().materialInfo().sprite(), true);
        }

        @Override
        public @BakedQuad.MaterialFlags int materialFlags() {
            return BakedQuad.FLAG_TRANSLUCENT;
        }
    }

    private record Pair(Direction top, Direction left) {
    }
}
