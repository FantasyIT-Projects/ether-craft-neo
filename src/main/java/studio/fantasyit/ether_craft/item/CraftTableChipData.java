package studio.fantasyit.ether_craft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.List;

/**
 * 工作台芯片的数据组件值。
 *
 * @param recipe 目标原版合成配方 id。<b>唯一参与匹配的字段</b>。
 * @param inputs 展示/编辑用的输入物品列表，<b>不参与匹配</b>。
 */
public record CraftTableChipData(Identifier recipe, List<ItemStackTemplate> inputs) {
    public static final Codec<CraftTableChipData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("recipe").forGetter(CraftTableChipData::recipe),
            ItemStackTemplate.CODEC.listOf().optionalFieldOf("inputs", List.of()).forGetter(CraftTableChipData::inputs)
    ).apply(inst, CraftTableChipData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftTableChipData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CraftTableChipData::recipe,
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), CraftTableChipData::inputs,
            CraftTableChipData::new
    );
}
