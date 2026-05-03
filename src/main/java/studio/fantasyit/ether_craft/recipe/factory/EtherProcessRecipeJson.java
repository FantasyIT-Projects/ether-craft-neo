package studio.fantasyit.ether_craft.recipe.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record EtherProcessRecipeJson(
        List<InputEntry> input,
        List<OutputEntry> output,
        List<ProcessEntry> process
) {
    public record InputEntry(String id, SizedIngredient item, @Nullable String next) {
    }

    public record OutputEntry(String id, ItemStack item) {
    }

    public record ProcessEntry(String id, List<SizedIngredient> item, List<ChipRecord> chip, @Nullable String next) {
    }

    public record ChipRecord(Identifier id) {
    }

    public static final Codec<ChipRecord> CHIP_RECORD_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Identifier.CODEC.fieldOf("id").forGetter(ChipRecord::id)
            ).apply(inst, ChipRecord::new));
    public static final Codec<EtherProcessRecipeJson.InputEntry> INPUT_ENTRY_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.fieldOf("id").forGetter(EtherProcessRecipeJson.InputEntry::id),
                    SizedIngredient.NESTED_CODEC.fieldOf("item").forGetter(EtherProcessRecipeJson.InputEntry::item),
                    Codec.STRING.optionalFieldOf("next").forGetter(e -> Optional.ofNullable(e.next()))
            ).apply(inst, (id, item, next) -> new EtherProcessRecipeJson.InputEntry(id, item, next.orElse(null))));

    public static final Codec<EtherProcessRecipeJson.OutputEntry> OUTPUT_ENTRY_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.fieldOf("id").forGetter(EtherProcessRecipeJson.OutputEntry::id),
                    ItemStack.CODEC.fieldOf("item").forGetter(EtherProcessRecipeJson.OutputEntry::item)
            ).apply(inst, EtherProcessRecipeJson.OutputEntry::new));

    public static final Codec<EtherProcessRecipeJson.ProcessEntry> PROCESS_ENTRY_CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.fieldOf("id").forGetter(EtherProcessRecipeJson.ProcessEntry::id),
                    SizedIngredient.NESTED_CODEC.listOf().fieldOf("item").orElse(List.of()).forGetter(EtherProcessRecipeJson.ProcessEntry::item),
                    CHIP_RECORD_CODEC.listOf().fieldOf("chip").orElse(List.of()).forGetter(EtherProcessRecipeJson.ProcessEntry::chip),
                    Codec.STRING.optionalFieldOf("next").forGetter(e -> Optional.ofNullable(e.next()))
            ).apply(inst, (id, item, chip, next) -> new EtherProcessRecipeJson.ProcessEntry(id, item, chip, next.orElse(null))));


    public static final MapCodec<EtherProcessRecipeJson> MAP_CODEC =
            RecordCodecBuilder.mapCodec(inst -> inst.group(
                    Codec.list(INPUT_ENTRY_CODEC).fieldOf("input").forGetter(EtherProcessRecipeJson::input),
                    Codec.list(OUTPUT_ENTRY_CODEC).fieldOf("output").forGetter(EtherProcessRecipeJson::output),
                    Codec.list(PROCESS_ENTRY_CODEC).fieldOf("process").forGetter(EtherProcessRecipeJson::process)
            ).apply(inst, EtherProcessRecipeJson::new));

    private static StreamCodec<ByteBuf, String> nullable() {
        return ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).map(opt -> opt.orElse(null), Optional::ofNullable);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, ChipRecord> CHIP_RECORD_STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC,
                    ChipRecord::id,
                    ChipRecord::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, InputEntry> INPUT_ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, InputEntry::id,
                    SizedIngredient.STREAM_CODEC, InputEntry::item,
                    nullable(), InputEntry::next,
                    InputEntry::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, OutputEntry> OUTPUT_ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, OutputEntry::id,
                    ItemStack.STREAM_CODEC, OutputEntry::item,
                    OutputEntry::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessEntry> PROCESS_ENTRY_STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ProcessEntry::id,
                    ByteBufCodecs.collection(ArrayList::new, SizedIngredient.STREAM_CODEC), ProcessEntry::item,
                    ByteBufCodecs.collection(ArrayList::new, CHIP_RECORD_STREAM_CODEC), ProcessEntry::chip,
                    nullable(), ProcessEntry::next,
                    ProcessEntry::new
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, EtherProcessRecipeJson> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.collection(ArrayList::new, INPUT_ENTRY_STREAM_CODEC), EtherProcessRecipeJson::input,
                    ByteBufCodecs.collection(ArrayList::new, OUTPUT_ENTRY_STREAM_CODEC), EtherProcessRecipeJson::output,
                    ByteBufCodecs.collection(ArrayList::new, PROCESS_ENTRY_STREAM_CODEC), EtherProcessRecipeJson::process,
                    EtherProcessRecipeJson::new
            );

}