package studio.fantasyit.ether_craft.node.tip;

import com.mojang.serialization.Codec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public enum TipConcept implements StringRepresentable {
    ETHER_FLOW("ether_flow"),
    ETHER_STORAGE("ether_storage"),
    LOGISTICS("logistics"),
    WORLD_INTERACTION("world_interaction"),
    DECORATION("decoration"),
    ETHER_PRODUCTION("ether_production"),
    CRAFTING("crafting");

    public static final Codec<TipConcept> CODEC = StringRepresentable.fromEnum(TipConcept::values);
    public static final StreamCodec<FriendlyByteBuf, TipConcept> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public TipConcept decode(FriendlyByteBuf buf) {
                    return buf.readEnum(TipConcept.class);
                }
                @Override
                public void encode(FriendlyByteBuf buf, TipConcept val) {
                    buf.writeEnum(val);
                }
            };

    private final String name;

    TipConcept(String name) {
        this.name = name;
    }

    @Override
    public @NotNull String getSerializedName() {
        return name;
    }
}
