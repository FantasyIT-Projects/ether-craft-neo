package studio.fantasyit.ether_craft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作台芯片的数据组件值。
 *
 * @param recipe 目标原版合成配方 id。
 * @param grid   工作台网格内容：3×3、行优先、长度 9、允许空气。
 *               界面回填直接逐格还原（空格子天然保留）；非 shaped/shapeless 配方的兜底匹配
 *               用 {@link #marked()} 忽略空气后比较物品多重集。
 */
public record CraftTableChipData(Identifier recipe, List<ItemStack> grid) {
    public static final int SIZE = 3;
    public static final int CELLS = SIZE * SIZE;

    public static final Codec<CraftTableChipData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("recipe").forGetter(CraftTableChipData::recipe),
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("grid").forGetter(CraftTableChipData::grid)
    ).apply(inst, CraftTableChipData::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, CraftTableChipData> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, CraftTableChipData::recipe,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), CraftTableChipData::grid,
            CraftTableChipData::new
    );

    /** 网格里的非空物品（行优先）。 */
    public List<ItemStack> marked() {
        List<ItemStack> list = new ArrayList<>(CELLS);
        for (ItemStack stack : grid)
            if (!stack.isEmpty()) list.add(stack);
        return list;
    }
}
