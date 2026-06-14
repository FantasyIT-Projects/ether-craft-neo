package studio.fantasyit.ether_craft.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.register.Tags;

import java.util.concurrent.CompletableFuture;

public class TagGenEntity extends EntityTypeTagsProvider {
    protected TagGenEntity(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, EtherCraft.MODID);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(Tags.ETHER_STREAM_PASS_THROUGH_ENTITY)
                .add(EntityType.ARROW)
                .add(EntityType.SPECTRAL_ARROW)
                .add(EntityType.ENDER_PEARL)
                .add(EntityType.EYE_OF_ENDER)
                .add(EntityType.WIND_CHARGE);
    }
}
