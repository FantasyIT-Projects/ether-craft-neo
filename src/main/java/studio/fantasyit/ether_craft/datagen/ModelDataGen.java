package studio.fantasyit.ether_craft.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator;
import net.minecraft.client.data.models.blockstates.PropertyDispatch;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.model.generators.blockstate.CustomBlockStateModelBuilder;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.glass.render.EtherGlassUnbakedModel;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
import studio.fantasyit.ether_craft.block.node.render.EtherAdaptNodeUnbakedModel;
import studio.fantasyit.ether_craft.item.renderer.AnswerItemOverlaySMR;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.Optional;

public class ModelDataGen extends ModelProvider {
    private final ResourceManager rm;

    public ModelDataGen(PackOutput output, ResourceManager rm) {
        super(output, EtherCraft.MODID);
        this.rm = rm;
    }

    public static final ModelTemplate ITEM_SIMPLE = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("item/paper")),
            Optional.empty(),
            TextureSlot.LAYER0
    );

    public static final TextureSlot T0 = TextureSlot.create("0", TextureSlot.TEXTURE);
    public static final ModelTemplate BLOCK_FACES = new ModelTemplate(
            Optional.of(EtherCraft.id("_block_faces_base")),
            Optional.empty(),
            T0, TextureSlot.PARTICLE
    );
    public static final TexturedModel.Provider BLOCK_FACES_PROVIDER = TexturedModel.createDefault(
            (a) -> new TextureMapping()
                    .put(T0, new Material(BuiltInRegistries.BLOCK.getKey(a).withPrefix("block/")))
                    .put(TextureSlot.PARTICLE, new Material(BuiltInRegistries.BLOCK.getKey(a).withPrefix("block/"))),
            BLOCK_FACES
    );

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ItemRegistry.PROCESS_CHIP_ITEM.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.LOGO.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.ETHER.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.ETHER_CREATIVE.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.WRENCH.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.BLADE.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.DIAMOND_NEEDLE.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.ETHER_CRYSTAL.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.ETHERPHILIC_BOWL.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.GOLD_SCREW.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.INACTIVATED_ETHER.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.VACUUM_PIPE.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.ETHER_INGOT.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.WARDEN_HEART.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.CHEESE.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.ETHER_DUST.get(), ITEM_SIMPLE);
        ClientItem.Properties oversizedProps = new ClientItem.Properties(false, true, 1.0f);

        Identifier flat5x5 = itemModels.createFlatItemModel(ItemRegistry.ANSWER_GRID_5X5.get(), ITEM_SIMPLE);
        itemModels.itemModelOutput.accept(ItemRegistry.ANSWER_GRID_5X5.get(),
                ItemModelUtils.specialModel(flat5x5, new AnswerItemOverlaySMR.Unbaked(flat5x5)),
                oversizedProps);

        Identifier flat7x7 = itemModels.createFlatItemModel(ItemRegistry.ANSWER_GRID_7X7.get(), ITEM_SIMPLE);
        itemModels.itemModelOutput.accept(ItemRegistry.ANSWER_GRID_7X7.get(),
                ItemModelUtils.specialModel(flat7x7, new AnswerItemOverlaySMR.Unbaked(flat7x7)),
                oversizedProps);

        Identifier flat9x9 = itemModels.createFlatItemModel(ItemRegistry.ANSWER_GRID_9X9.get(), ITEM_SIMPLE);
        itemModels.itemModelOutput.accept(ItemRegistry.ANSWER_GRID_9X9.get(),
                ItemModelUtils.specialModel(flat9x9, new AnswerItemOverlaySMR.Unbaked(flat9x9)),
                oversizedProps);
        rm.listResources("ether_process_chip", t -> t.getPath().endsWith(".json")).forEach((_id, resource) -> {
            String path = _id.getPath();
            Identifier id = Identifier.fromNamespaceAndPath(
                    _id.getNamespace(),
                    path.replace("ether_process_chip/", "").replace(".json", "")
            );
            Identifier identifier = ITEM_SIMPLE.create(id.withPrefix("item/"), new TextureMapping().put(TextureSlot.LAYER0, new Material(id.withPrefix("item/"))), itemModels.modelOutput);
            itemModels.itemModelOutput.register(id, new ClientItem(ItemModelUtils.plainModel(identifier), new ClientItem.Properties(false, false, 1)));
        });

        //guide book
        Identifier guideBook = EtherCraft.id("guide_book");
        Identifier identifier = ITEM_SIMPLE.create(guideBook.withPrefix("item/"), new TextureMapping().put(TextureSlot.LAYER0, new Material(guideBook.withPrefix("item/"))), itemModels.modelOutput);
        itemModels.itemModelOutput.register(guideBook, new ClientItem(ItemModelUtils.plainModel(identifier), new ClientItem.Properties(false, false, 1)));

        generateProcessFactoryModels(blockModels, itemModels);

        generateAdaptNodeModels(blockModels, itemModels);

        blockModels.createTrivialCube(BlockRegistry.ETHER_BLOCK.get());
        blockModels.createTrivialCube(BlockRegistry.ETHER_ORE.get());
        blockModels.createTrivialCube(BlockRegistry.DEEPSLATE_ETHER_ORE.get());
        blockModels.createTrivialCube(BlockRegistry.NETHER_ETHER_ORE.get());
        blockModels.createTrivialCube(BlockRegistry.INACTIVATED_ETHER_BLOCK.get());
        blockModels.createTrivialCube(BlockRegistry.SMOOTH_INACTIVATED_ETHER_BLOCK.get());
        blockModels.createTrivialCube(BlockRegistry.ETHER_DECO_1.get());
        blockModels.createTrivialCube(BlockRegistry.ETHER_DECO_2.get());
        blockModels.createTrivialCube(BlockRegistry.ETHER_DECO_3.get());
        blockModels.createTrivialCube(BlockRegistry.ETHER_DECO_4.get());

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(BlockRegistry.CHEESE_BLOCK.get(),
                        BlockModelGenerators.variant(new Variant(EtherCraft.id("block/cheese_block"))))
        );

        // 以太玻璃 - 连接纹理
        var etherGlassCustom = MultiVariant.of(new CustomBlockStateModelBuilder.Simple(new EtherGlassUnbakedModel()));
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(BlockRegistry.ETHER_GLASS.get(), etherGlassCustom)
        );
    }

    private void generateProcessFactoryModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var processFactory = BlockRegistry.ETHER_PROCESS_FACTORY.get();
        Identifier[] levelModelIds = new Identifier[5];

        for (int level = 1; level <= 4; level++) {
            TextureMapping texMapping = new TextureMapping()
                    .put(TextureSlot.NORTH, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_front")))
                    .put(TextureSlot.SOUTH, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_back")))
                    .put(TextureSlot.EAST, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_right")))
                    .put(TextureSlot.WEST, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_left")))
                    .put(TextureSlot.UP, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_top")))
                    .put(TextureSlot.DOWN, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_bottom")))
                    .put(TextureSlot.PARTICLE, new Material(EtherCraft.id("block/factory/lv" + level + "/ether_process_factory_lv" + level + "_front")));
            levelModelIds[level] = ModelTemplates.CUBE.createWithSuffix(
                    processFactory, "_lv_" + level, texMapping, blockModels.modelOutput);
        }


        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(processFactory)
                        .with(PropertyDispatch.initial(EtherProcessFactoryBlock.LEVEL)
                                .select(1, BlockModelGenerators.variant(new Variant(levelModelIds[1])))
                                .select(2, BlockModelGenerators.variant(new Variant(levelModelIds[2])))
                                .select(3, BlockModelGenerators.variant(new Variant(levelModelIds[3])))
                                .select(4, BlockModelGenerators.variant(new Variant(levelModelIds[4])))
                        ).with(PropertyDispatch.modify(EtherProcessFactoryBlock.FACING)
                                .select(Direction.NORTH, BlockModelGenerators.NOP)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST, BlockModelGenerators.Y_ROT_270)
                                .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                                .select(Direction.UP, BlockModelGenerators.NOP)
                                .select(Direction.DOWN, BlockModelGenerators.NOP)
                        ));

        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_1.get(),
                ItemModelUtils.plainModel(levelModelIds[1]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_2.get(),
                ItemModelUtils.plainModel(levelModelIds[2]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_3.get(),
                ItemModelUtils.plainModel(levelModelIds[3]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_PROCESS_FACTORY_ITEM_LV_4.get(),
                ItemModelUtils.plainModel(levelModelIds[4]));
    }

    private void generateAdaptNodeModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        var adaptNode = BlockRegistry.ETHER_ADAPT_NODE.get();
        Identifier[] levelModelIds = new Identifier[4];

        for (int level = 1; level <= 3; level++) {
            TextureMapping texMapping = new TextureMapping()
                    .put(TextureSlot.ALL, new Material(EtherCraft.id("block/node/ether_adapt_node_lv" + level)));
            levelModelIds[level] = ModelTemplates.CUBE_ALL.createWithSuffix(
                    adaptNode, "_lv_" + level, texMapping, blockModels.modelOutput);
        }
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get(),
                ItemModelUtils.plainModel(levelModelIds[1]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_2.get(),
                ItemModelUtils.plainModel(levelModelIds[2]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_3.get(),
                ItemModelUtils.plainModel(levelModelIds[3]));
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(BlockRegistry.ETHER_ADAPT_NODE.get(),
                                MultiVariant.of(new CustomBlockStateModelBuilder.Simple(new EtherAdaptNodeUnbakedModel()))
                        )
                        .with(PropertyDispatch.modify(EtherAdaptNodeBlock.LEVEL).generate(t -> BlockModelGenerators.NOP))
                        .with(PropertyDispatch.modify(EtherAdaptNodeBlock.FACING).generate(t -> BlockModelGenerators.NOP))
        );
    }
}
