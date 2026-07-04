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
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryBlock;
import studio.fantasyit.ether_craft.block.glass.render.EtherGlassUnbakedModel;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeBlock;
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


        //发射器
        Identifier modelLoc = BLOCK_FACES_PROVIDER.create(BlockRegistry.ETHER_STREAM_EMITTER.get(), blockModels.modelOutput);
        Variant variant = new Variant(modelLoc);
        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(
                        BlockRegistry.ETHER_STREAM_EMITTER.get(),
                        BlockModelGenerators.variant(variant)
                ).with(
                        PropertyDispatch.modify(BlockStateProperties.FACING)
                                .select(Direction.NORTH, BlockModelGenerators.NOP)
                                .select(Direction.SOUTH, BlockModelGenerators.Y_ROT_180)
                                .select(Direction.WEST, BlockModelGenerators.Y_ROT_270)
                                .select(Direction.EAST, BlockModelGenerators.Y_ROT_90)
                                .select(Direction.UP, BlockModelGenerators.X_ROT_270)
                                .select(Direction.DOWN, BlockModelGenerators.X_ROT_90)
                )
        );

        generateAdaptNodeModels(blockModels, itemModels);

        blockModels.createTrivialCube(BlockRegistry.ETHER_BLOCK.get());
        blockModels.createTrivialCube(BlockRegistry.ETHER_ORE.get());
        blockModels.createTrivialCube(BlockRegistry.DEEPSLATE_ETHER_ORE.get());
        blockModels.createTrivialCube(BlockRegistry.NETHER_ETHER_ORE.get());
        blockModels.createTrivialCube(BlockRegistry.INACTIVATED_ETHER_BLOCK.get());
        blockModels.createTrivialCube(BlockRegistry.SMOOTH_INACTIVATED_ETHER_BLOCK.get());

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
            int rowV = (level - 1) * 4;
            TextureSlot texSlot = TextureSlot.create("tex", TextureSlot.TEXTURE);

            ExtendedModelTemplateBuilder builder = ExtendedModelTemplateBuilder.builder()
                    .parent(Identifier.withDefaultNamespace("block/cube"))
                    .requiredTextureSlot(texSlot)
                    .requiredTextureSlot(TextureSlot.PARTICLE)
                    .element(elem -> elem
                            .from(0, 0, 0).to(16, 16, 16)
                            .face(Direction.UP, face -> face.texture(texSlot)
                                    .uvs(0, rowV, 4, rowV + 4).cullface(Direction.UP))
                            .face(Direction.DOWN, face -> face.texture(texSlot)
                                    .uvs(8, rowV, 12, rowV + 4).cullface(Direction.DOWN))
                            .face(Direction.NORTH, face -> face.texture(texSlot)
                                    .uvs(4, rowV, 8, rowV + 4).cullface(Direction.NORTH))
                            .face(Direction.SOUTH, face -> face.texture(texSlot)
                                    .uvs(4, rowV, 8, rowV + 4).cullface(Direction.SOUTH))
                            .face(Direction.WEST, face -> face.texture(texSlot)
                                    .uvs(4, rowV, 8, rowV + 4).cullface(Direction.WEST))
                            .face(Direction.EAST, face -> face.texture(texSlot)
                                    .uvs(4, rowV, 8, rowV + 4).cullface(Direction.EAST)));

            String suffix = "_lv_" + level;
            TextureMapping texMapping = new TextureMapping()
                    .put(texSlot, new Material(EtherCraft.id("block/factory/ether_process_factory")))
                    .put(TextureSlot.PARTICLE, new Material(EtherCraft.id("block/factory/ether_process_factory_breaking")));
            levelModelIds[level] = builder.build().createWithSuffix(
                    processFactory, suffix, texMapping, blockModels.modelOutput);
        }

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(processFactory)
                        .with(PropertyDispatch.initial(EtherProcessFactoryBlock.LEVEL)
                                .generate(level -> {
                                    int clamped = Math.min(Math.max(level, 1), 4);
                                    return BlockModelGenerators.variant(
                                            new Variant(levelModelIds[clamped]));
                                })));

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

        blockModels.blockStateOutput.accept(
                MultiVariantGenerator.dispatch(adaptNode)
                        .with(PropertyDispatch.initial(EtherAdaptNodeBlock.LEVEL)
                                .generate(level -> {
                                    int clamped = Math.min(Math.max(level, 1), 3);
                                    return BlockModelGenerators.variant(
                                            new Variant(levelModelIds[clamped]));
                                })));

        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_1.get(),
                ItemModelUtils.plainModel(levelModelIds[1]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_2.get(),
                ItemModelUtils.plainModel(levelModelIds[2]));
        itemModels.itemModelOutput.accept(
                ItemRegistry.ETHER_ADAPT_NODE_ITEM_LV_3.get(),
                ItemModelUtils.plainModel(levelModelIds[3]));
    }
}
