package studio.fantasyit.ether_craft.stream.data;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.EtherCraft;

public record EtherStreamDisplayItemData(ItemStack itemStack) implements IEtherStreamSyncedData {
    public static Identifier ID = EtherCraft.id("ether_stream_display_item_data");

    public static MapCodec<EtherStreamDisplayItemData> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(EtherStreamDisplayItemData::itemStack)
    ).apply(i, EtherStreamDisplayItemData::new));

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void toBuffer(FriendlyByteBuf writer) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) writer, itemStack);
    }

    public static EtherStreamDisplayItemData fromBuffer(FriendlyByteBuf buffer) {
        return new EtherStreamDisplayItemData(ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer));
    }
}
