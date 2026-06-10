package studio.fantasyit.ether_craft.plating.trigger.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record TriggerOnNotExistRecord(Set<Identifier> applied) {
    public static final Codec<TriggerOnNotExistRecord> CODEC = Identifier.CODEC.listOf().fieldOf("applied").xmap(TriggerOnNotExistRecord::new, TriggerOnNotExistRecord::getAppliedAsList).codec();
    public static final StreamCodec<ByteBuf, TriggerOnNotExistRecord> STREAM_CODEC = ByteBufCodecs.collection(HashSet::new, Identifier.STREAM_CODEC).map(
            TriggerOnNotExistRecord::new,
            t -> new HashSet<>(t.applied())
    );

    public List<Identifier> getAppliedAsList() {
        return applied.stream().toList();
    }

    public TriggerOnNotExistRecord(List<Identifier> applied) {
        this(new HashSet<>(applied));
    }

    public TriggerOnNotExistRecord copyWithNew(Identifier applied) {
        Set<Identifier> newApplied = new HashSet<>(this.applied);
        newApplied.add(applied);
        return new TriggerOnNotExistRecord(newApplied);
    }

    public TriggerOnNotExistRecord copyWithRemoved(Set<Identifier> applied) {
        Set<Identifier> newApplied = new HashSet<>(this.applied);
        newApplied.removeAll(applied);
        return new TriggerOnNotExistRecord(newApplied);
    }
}
