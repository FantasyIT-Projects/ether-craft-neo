package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.stream.IEntityGetter;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

import java.util.Optional;

public class EtherStreamDisplayTimeCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("display_time");

    public static final Codec<EtherStreamDisplayTimeCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("end_pos").forGetter(c -> Optional.ofNullable(c.endPos))
    ).apply(instance, opt -> {
        EtherStreamDisplayTimeCapability cap = new EtherStreamDisplayTimeCapability();
        cap.endPos = opt.orElse(null);
        return cap;
    }));

    @Nullable
    private BlockPos endPos;

    public EtherStreamDisplayTimeCapability() {
    }

    public EtherStreamDisplayTimeCapability(@Nullable BlockPos endPos) {
        this.endPos = endPos;
    }

    @Nullable
    public BlockPos getEndPos() {
        return endPos;
    }

    public void setEndPos(@Nullable BlockPos endPos) {
        this.endPos = endPos;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void tick(IEtherStreamLike streamEntity, IEntityGetter entityGetter) {
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
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
    }

    @Override
    public void serialize(ValueOutput output) {
        if (endPos != null)
            output.store("end_pos", BlockPos.CODEC, endPos);
    }

    @Override
    public void deserialize(ValueInput input) {
        endPos = input.read("end_pos", BlockPos.CODEC).orElse(null);
    }
}
