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

    private static final GridSplitter MAIN_MACHINE_ATLAS = new GridSplitter(16, 256, 256, EtherCraft.id("textures/block/node/ether_adapt_node_atlas.png"));

    public static final AtlasUV BLANK_FACE_LV1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV BLANK_FACE_LV2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV BLANK_FACE_LV3 = MAIN_MACHINE_ATLAS.next();
    public static final GroupAtlasUV BLANK_FACE = new GroupAtlasUV(BLANK_FACE_LV1, BLANK_FACE_LV2, BLANK_FACE_LV3);

    public static final AtlasUV FEATURE_EMITTER = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FEATURE_DROPPER = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FEATURE_INTERACT_EXTRACT = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FEATURE_INTERACT_INSERT = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ACCELERATE = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_CRAFTING_TOP = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_CRAFTING_BOTTOM = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_ITEM_CONSUME_GENERATOR = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_WORKING = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_5 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_6 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_7 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_8 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_9 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV ETHER_FILL_10 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_STONE = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_WOOD = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_BASALT = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_1 = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_4 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_5 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_6 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_7 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING_8 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_ETHER_CONVERT = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_1 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_5 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_6 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_7 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ETHER_CONVERT_WORKING_8 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_MAGNET_BOT_1 = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_MAGNET_BOT_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_MAGNET_BOT_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_MAGNET_BOT_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_MAGNET_BOT_5 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_MAGNET_TOP_1 = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_MAGNET_TOP_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_MAGNET_TOP_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_MAGNET_TOP_4 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_MAGNET_TOP_5 = MAIN_MACHINE_ATLAS.next();

    public static final AtlasUV FUNCTION_ENCHANT_1 = MAIN_MACHINE_ATLAS.nextRow().next();
    public static final AtlasUV FUNCTION_ENCHANT_2 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ENCHANT_3 = MAIN_MACHINE_ATLAS.next();
    public static final AtlasUV FUNCTION_ENCHANT_4 = MAIN_MACHINE_ATLAS.next();


    public static final GroupAtlasUV ETHER_FILL = new GroupAtlasUV(
            ETHER_FILL_1,
            ETHER_FILL_2,
            ETHER_FILL_3,
            ETHER_FILL_4,
            ETHER_FILL_5,
            ETHER_FILL_6,
            ETHER_FILL_7,
            ETHER_FILL_8,
            ETHER_FILL_9,
            ETHER_FILL_10
    );
    public static final GroupAtlasUV FUNCTION_MAGNET_BOT = new GroupAtlasUV(
            FUNCTION_MAGNET_BOT_1,
            FUNCTION_MAGNET_BOT_2,
            FUNCTION_MAGNET_BOT_3,
            FUNCTION_MAGNET_BOT_4,
            FUNCTION_MAGNET_BOT_5
    );
    public static final GroupAtlasUV FUNCTION_MAGNET_TOP = new GroupAtlasUV(
            FUNCTION_MAGNET_TOP_1,
            FUNCTION_MAGNET_TOP_2,
            FUNCTION_MAGNET_TOP_3,
            FUNCTION_MAGNET_TOP_4,
            FUNCTION_MAGNET_TOP_5
    );
    public static final GroupAtlasUV FUNCTION_ENCHANT = new GroupAtlasUV(
            FUNCTION_ENCHANT_1,
            FUNCTION_ENCHANT_2,
            FUNCTION_ENCHANT_3,
            FUNCTION_ENCHANT_4
    );
    public static final GroupAtlasUV FUNCTION_EQUIPMENT_CONSUME_WORKING = new GroupAtlasUV(
            FUNCTION_EQUIPMENT_CONSUME_WORKING_1,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_2,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_3,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_4,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_5,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_6,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_7,
            FUNCTION_EQUIPMENT_CONSUME_WORKING_8
    );
    public static final GroupAtlasUV FUNCTION_ETHER_CONVERT_WORKING = new GroupAtlasUV(
            FUNCTION_ETHER_CONVERT_WORKING_1,
            FUNCTION_ETHER_CONVERT_WORKING_2,
            FUNCTION_ETHER_CONVERT_WORKING_3,
            FUNCTION_ETHER_CONVERT_WORKING_4,
            FUNCTION_ETHER_CONVERT_WORKING_5,
            FUNCTION_ETHER_CONVERT_WORKING_6,
            FUNCTION_ETHER_CONVERT_WORKING_7,
            FUNCTION_ETHER_CONVERT_WORKING_8
    );
    public static final GroupAtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_LAVA = new GroupAtlasUV(
            FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_1,
            FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_2,
            FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_3,
            FUNCTION_ITEM_CONSUME_MATERIAL_LAVA_4
    );
    public static final GroupAtlasUV FUNCTION_ITEM_CONSUME_MATERIAL_FIRE = new GroupAtlasUV(
            FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_1,
            FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_2,
            FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_3,
            FUNCTION_ITEM_CONSUME_MATERIAL_FIRE_4
    );
}
