package studio.fantasyit.ether_craft.stream.idx;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.PosDir;

public class AutoIndexPosDir {
    int idx;
    boolean hasInit = false;
    @Nullable PosDir posDir = null;

    public AutoIndexPosDir(int idx) {
        this.idx = idx;
    }

    public AutoIndexPosDir(PosDir posDir) {
        this.idx = -1;
        this.posDir = posDir;
    }

    public void init(ReverseIndexMappingManager imm) {
        if (hasInit || posDir != null) {
            return;
        }
        posDir = imm.getById(idx);
        hasInit = true;
    }

    public boolean valid() {
        return posDir != null;
    }

    @Nullable
    public PosDir resolve(ReverseIndexMappingManager imm) {
        init(imm);
        return posDir;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, AutoIndexPosDir> STREAM_CODEC = StreamCodec.of(
            (b, t) -> {
                if (t.idx != -1)
                    b.writeVarInt(t.idx + 6);
                else {
                    b.writeVarInt(t.posDir.dir().ordinal());
                    b.writeBlockPos(t.posDir.pos());
                }
            },
            (b) -> {
                int id = b.readVarInt();
                if (id >= 6) {
                    return new AutoIndexPosDir(id - 6);
                } else {
                    return new AutoIndexPosDir(new PosDir(b.readBlockPos(), Direction.values()[id]));
                }
            }
    );

    public boolean hasIndex() {
        return idx != -1;
    }
}
