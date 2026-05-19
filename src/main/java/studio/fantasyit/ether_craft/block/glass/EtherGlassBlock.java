package studio.fantasyit.ether_craft.block.glass;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.NotNull;
import studio.fantasyit.ether_craft.register.BlockRegistry;

import java.util.List;

public class EtherGlassBlock extends TransparentBlock {
    public EtherGlassBlock(Identifier identifier) {
        super(Properties.of()
                .strength(1f)
                .sound(SoundType.GLASS)
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .setId(ResourceKey.create(Registries.BLOCK, identifier)));
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, @NotNull LootParams.Builder params) {
        return List.of(new ItemStack(BlockRegistry.ETHER_GLASS.get()));
    }
}
