package studio.fantasyit.ether_craft.datagen;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.factory.EtherProcessChipManager;
import studio.fantasyit.ether_craft.register.BlockRegistry;
import studio.fantasyit.ether_craft.register.ItemRegistry;

import java.util.Optional;

public class ModelDataGen extends ModelProvider {
    public ModelDataGen(PackOutput output) {
        super(output, EtherCraft.MODID);
    }

    public static final ModelTemplate ITEM_SIMPLE = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("item/paper")),
            Optional.of("_simple"),
            TextureSlot.LAYER0
    );
    public static final ModelTemplate BLOCK_ALL = new ModelTemplate(
            Optional.of(Identifier.withDefaultNamespace("block/cube_all")),
            Optional.of("_all"),
            TextureSlot.ALL, TextureSlot.PARTICLE
    );

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        itemModels.generateFlatItem(ItemRegistry.PROCESS_CHIP_ITEM.get(), ITEM_SIMPLE);
        itemModels.generateFlatItem(ItemRegistry.DIRECT_INPUT_ITEM_CHIP.get(), ITEM_SIMPLE);
        EtherProcessChipManager.foreach((id, record) -> {
            ITEM_SIMPLE.create(id, new TextureMapping().put(TextureSlot.ALL, new Material(id.withPrefix("item/"))), itemModels.modelOutput);
        });
        blockModels.createTrivialCube(BlockRegistry.ETHER_PROCESS_FACTORY.get());
    }
}
