package studio.fantasyit.ether_craft.stream.cap;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;
import studio.fantasyit.ether_craft.stream.vholder.VirtualEtherStream;


@SuppressWarnings("deprecation")
public class EtherStreamLabelCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("label");

    @Nullable
    private Component label;
    @Nullable
    private Vec3 startPos;
    private int color = 0xFFFFFFFF;

    @Nullable
    public Component getLabel() {
        return label;
    }

    public void setLabel(@Nullable Component label) {
        this.label = label;
    }

    @Nullable
    public Vec3 getStartPos() {
        return startPos;
    }

    public void setStartPos(@Nullable Vec3 startPos) {
        this.startPos = startPos;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public int getConsumption() {
        return 0;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity) {
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
    public void onDestroy(IEtherStreamLike streamEntity) {
    }

    @Override
    public void serialize(ValueOutput output) {
        if (label != null) {
            output.store("label", ComponentSerialization.CODEC, label);
        }
        if (startPos != null) {
            output.putDouble("startX", startPos.x);
            output.putDouble("startY", startPos.y);
            output.putDouble("startZ", startPos.z);
        }
        output.putInt("color", color);
    }

    @Override
    public void deserialize(ValueInput input) {
        this.label = input.read("label", ComponentSerialization.CODEC).orElse(null);
        double sx = input.getDoubleOr("startX", Double.NaN);
        double sy = input.getDoubleOr("startY", Double.NaN);
        double sz = input.getDoubleOr("startZ", Double.NaN);
        if (!Double.isNaN(sx) && !Double.isNaN(sy) && !Double.isNaN(sz)) {
            this.startPos = new Vec3(sx, sy, sz);
        } else {
            this.startPos = null;
        }
        this.color = input.getIntOr("color", 0xFFFFFFFF);
    }

    @Override
    public void firstTick(@UnknownNullability IEtherStreamLike etherStreamEntity) {
        setStartPos(etherStreamEntity.position());
        if (etherStreamEntity instanceof VirtualEtherStream ves) {
            ves.setLabel(getLabel(), getColor());
        }
    }
}
