package studio.fantasyit.ether_craft.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import studio.fantasyit.ether_craft.EtherCraft;
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
        rm.listResources("ether_process_chip", t -> t.getPath().endsWith(".json")).forEach((_id, resource) -> {
            String path = _id.getPath();
            Identifier id = Identifier.fromNamespaceAndPath(
                    _id.getNamespace(),
                    path.replace("ether_process_chip/", "").replace(".json", "")
            );
            Identifier identifier = ITEM_SIMPLE.create(id.withPrefix("item/"), new TextureMapping().put(TextureSlot.LAYER0, new Material(id.withPrefix("item/"))), itemModels.modelOutput);
            itemModels.itemModelOutput.register(id, new ClientItem(ItemModelUtils.plainModel(identifier), new ClientItem.Properties(false, false, 1)));
        });

        blockModels.createTrivialBlock(
                BlockRegistry.ETHER_PROCESS_FACTORY.get(), BLOCK_FACES_PROVIDER);
    }
}
