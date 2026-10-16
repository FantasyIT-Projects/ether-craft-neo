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
 * @param recipe 目标原版合成配方 id，用于解析 {@link net.minecraft.world.item.crafting.CraftingRecipe}。
 * @param inputs 期望的输入物品列表。仅当配方不是 shaped/shapeless（走 fallback 分支）时参与匹配：
 *               与实际输入做<b>网格无关、组件无关</b>的匹配（不要求一一对应，实际侧允许富余）。
 *               shaped/shapeless 由配方自身的原料列表决定，本字段只用于展示。
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
