package studio.fantasyit.ether_craft.stream.idx;

import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.stream.PosDir;

public class AutoIndexPosDir {
    /**
     * byte id 域上限（含）：首字节 6~254 表达 idx 0~248，共 IDX_LIMIT 个；
     * 255 为 varint 转义（存 idx - IDX_LIMIT），0~5 为全量模式的方向。
     */
    public static final int IDX_LIMIT = 249;

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
                if (t.idx != -1) {
                    if (t.idx < IDX_LIMIT)
                        b.writeByte(t.idx + 6);
                    else {
                        b.writeByte(255);
                        b.writeVarInt(t.idx - IDX_LIMIT);
                    }
                } else {
                    b.writeByte(t.posDir.dir().ordinal());
                    b.writeBlockPos(t.posDir.pos());
                }
            },
            (b) -> {
                int v = b.readUnsignedByte();
                if (v >= 6) {
                    if (v == 255) {
                        return new AutoIndexPosDir(b.readVarInt() + IDX_LIMIT);
                    }
                    return new AutoIndexPosDir(v - 6);
                } else {
                    return new AutoIndexPosDir(new PosDir(b.readBlockPos(), Direction.values()[v]));
                }
            }
    );

    public boolean hasIndex() {
        return idx != -1;
    }
}
