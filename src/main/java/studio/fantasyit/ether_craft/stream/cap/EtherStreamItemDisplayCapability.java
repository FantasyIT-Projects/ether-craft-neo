package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.data.EtherStreamDisplayItemData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EtherStreamItemDisplayCapability implements IStreamCapability {
    public static Identifier ID = EtherCraft.id("item_display");
    public static Codec<EtherStreamItemDisplayCapability> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(EtherStreamItemDisplayCapability::getItemStack)
    ).apply(i, EtherStreamItemDisplayCapability::new));

    @Override
    public Identifier getId() {
        return ID;
    }

    ItemStack cachedItemStack;


    public EtherStreamItemDisplayCapability() {
        this(ItemStack.EMPTY);
    }

    public EtherStreamItemDisplayCapability(ItemStack i) {
        cachedItemStack = i;
    }

    public ItemStack getItemStack() {
        return cachedItemStack;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
        if (streamEntity.tickCount() % 10 != 0) return;
        int idx = streamEntity.tickCount() / 10;
        Optional<IStreamCapability> capability = streamEntity.getCapability(EtherStreamStorageCapability.ID);
        ItemStack ni = ItemStack.EMPTY;
        if (capability.isPresent()) {
            EtherStreamStorageCapability cap = (EtherStreamStorageCapability) capability.get();
            List<ItemStack> depItem = new ArrayList<>();
            for (int i = 0; i < cap.getContainerSize(); i++) {
                @NotNull ItemStack item = cap.getItem(i);
                boolean found = false;
                for (ItemStack ei : depItem)
                    if (ItemStack.isSameItemSameComponents(item, ei)) {
                        found = true;
                        break;
                    }
                if (!found) {
                    depItem.add(item);
                }
            }
            if (!depItem.isEmpty()) {
                ni = depItem.get(idx % depItem.size());
            }
        }

        if (!ItemStack.isSameItemSameComponents(ni, cachedItemStack)) {
            cachedItemStack = ni;
            streamEntity.setSyncedData(new EtherStreamDisplayItemData(cachedItemStack));
        }
    }

    @Override
    public boolean hitEntity(ServerLevel level, IEtherStreamLike streamEntity, EntityHitResult hit, Entity entity) {
        return false;
    }

    @Override
    public boolean hitBlock(ServerLevel level, IEtherStreamLike streamEntity, BlockHitResult hit, BlockState blockState) {
        return false;
    }

    @Override
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {

    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("item", ItemStack.OPTIONAL_CODEC, cachedItemStack);
    }

    @Override
    public void deserialize(ValueInput input) {
        cachedItemStack = input.read("item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
    }
}
