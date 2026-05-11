package studio.fantasyit.ether_craft.block.node.render;

import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;

public class EtherAdapterNodeAtlas {
    public static class GroupAtlasUV {
        public final AtlasUV[] uvs;

        public GroupAtlasUV(AtlasUV... uvs) {
            this.uvs = uvs;
        }

        public AtlasUV get(int index) {
            return uvs[index % uvs.length];
        }
    }

    public static class AtlasUV {
        public final Identifier atlas;
        public final int x;
        public final int y;
        public final int w;
        public final int h;
        public final int iw;
        public final int ih;
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;

        public AtlasUV(Identifier atlas, int x, int y, int w, int h, int iw, int ih) {
            this.atlas = atlas;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.iw = iw;
            this.ih = ih;
            this.u0 = x / (float) iw;
            this.v0 = y / (float) ih;
            this.u1 = (x + w) / (float) iw;
            this.v1 = (y + h) / (float) ih;
        }
    }

    public static class GridSplitter {
        public final int gridSize;
        public final int iw;
        public final int ih;
        public final Identifier atlas;
        int x;
        int y;

        public GridSplitter(int gridSize, int iw, int ih, Identifier atlas) {
            this.gridSize = gridSize;
            this.iw = iw;
            this.ih = ih;
            this.atlas = atlas;
        }

        public AtlasUV next() {
            if (x >= iw) {
                x = 0;
                y += gridSize;
            }
            if (y >= ih) {
                throw new IllegalStateException("No more space in atlas");
            }
            AtlasUV uv = new AtlasUV(atlas, x, y, gridSize, gridSize, iw, ih);
            x += gridSize;
            return uv;
        }

        public GridSplitter nextRow() {
            y += gridSize;
            x = 0;
            return this;
        }

        public GridSplitter skip(int count) {
            x += gridSize * count;
            return this;
        }
    }

    private static final GridSplitter MAIN_MACHINE_ATLAS = new GridSplitter(16, 160, 160, EtherCraft.id("textures/block/ether_adapt_node_atlas.png"));
    public static final AtlasUV BOTTOM = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV TOP = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV SIDE = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV TRANSPARENT = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FEATURE_DROPPER_ZIMING = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FEATURE_STREAM_EMITTER_SIDE = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_BURNER_EMPTY = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_BURNER_WORKING = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_BURNER_FULL = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FEATURE_MAGNET_SIDE = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FEATURE_DROPPER_SIDE = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FEATURE_CONTAINER_INT_SIDE = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FEATURE_MAGNET_BOTTOM = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FEATURE_DROPPER_BOTTOM = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FEATURE_CONTAINER_INT_BOTTOM = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FEATURE_STREAM_EMITTER_TOP = MAIN_MACHINE_ATLAS.skip(1).next();
    public static final AtlasUV FEATURE_STREAM_EMITTER_BOTTOM = MAIN_MACHINE_ATLAS.next();


    public static final AtlasUV FEATURE_MAGNET_TOP = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FEATURE_DROPPER_TOP = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FEATURE_CONTAINER_INT_TOP = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_0 = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_5 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_6 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_TRANSFORM_WORKING_7 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV OVERLAY_FUNCTION_BURNER_STONE = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_WOOD = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_DEEPSLATE = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV OVERLAY_FUNCTION_BURNER_COAL_0 = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_COAL_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_COAL_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_COAL_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_LAVA_0 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_LAVA_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_LAVA_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_LAVA_3 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_0 = MAIN_MACHINE_ATLAS.nextRow().nextRow().next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_5 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_6 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_7 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_8 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV OVERLAY_FUNCTION_BURNER_FILL_9 = MAIN_MACHINE_ATLAS.next();


    public static final GroupAtlasUV FUNCTION_TRANSFORM_WORKING = new GroupAtlasUV(
            FUNCTION_TRANSFORM_WORKING_0,
            FUNCTION_TRANSFORM_WORKING_1,
            FUNCTION_TRANSFORM_WORKING_2,
            FUNCTION_TRANSFORM_WORKING_3,
            FUNCTION_TRANSFORM_WORKING_4,
            FUNCTION_TRANSFORM_WORKING_5,
            FUNCTION_TRANSFORM_WORKING_6,
            FUNCTION_TRANSFORM_WORKING_7
    );
    public static final GroupAtlasUV OVERLAY_FUNCTION_BURNER_COAL = new GroupAtlasUV(
            OVERLAY_FUNCTION_BURNER_COAL_0,
            OVERLAY_FUNCTION_BURNER_COAL_1,
            OVERLAY_FUNCTION_BURNER_COAL_2,
            OVERLAY_FUNCTION_BURNER_COAL_3
    );
    public static final GroupAtlasUV OVERLAY_FUNCTION_BURNER_LAVA = new GroupAtlasUV(
            OVERLAY_FUNCTION_BURNER_LAVA_0,
            OVERLAY_FUNCTION_BURNER_LAVA_1,
            OVERLAY_FUNCTION_BURNER_LAVA_2,
            OVERLAY_FUNCTION_BURNER_LAVA_3
    );
    public static final GroupAtlasUV OVERLAY_FUNCTION_BURNER_FILL = new GroupAtlasUV(
            OVERLAY_FUNCTION_BURNER_FILL_0,
            OVERLAY_FUNCTION_BURNER_FILL_1,
            OVERLAY_FUNCTION_BURNER_FILL_2,
            OVERLAY_FUNCTION_BURNER_FILL_3,
            OVERLAY_FUNCTION_BURNER_FILL_4,
            OVERLAY_FUNCTION_BURNER_FILL_5,
            OVERLAY_FUNCTION_BURNER_FILL_6,
            OVERLAY_FUNCTION_BURNER_FILL_7,
            OVERLAY_FUNCTION_BURNER_FILL_8,
            OVERLAY_FUNCTION_BURNER_FILL_9
    );
}
