package studio.fantasyit.ether_craft.stream.cap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import studio.fantasyit.ether_craft.stream.EtherConsumer;
import studio.fantasyit.ether_craft.stream.IEtherStreamLike;

public class EtherStreamCostReducerCapability implements IStreamCapability {
    public static final Identifier ID = EtherCraft.id("cost_reducer");

    public static final Codec<EtherStreamCostReducerCapability> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("levels").forGetter(c -> c.levels)
    ).apply(instance, EtherStreamCostReducerCapability::new));

    private int levels;

    public EtherStreamCostReducerCapability() {
        this.levels = 0;
    }

    public EtherStreamCostReducerCapability(int levels) {
        this.levels = levels;
    }

    public void incrementLevel() {
        this.levels++;
    }

    public int getLevels() {
        return levels;
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public void getConsumption(EtherConsumer consumer, IEtherStreamLike entity) {
        consumer.multiplyGlobalFactor(1.0 / Math.pow(2, levels));
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
    public void onDestroy(IEtherStreamLike streamEntity, @Nullable HitResult hitResult) {
    }

    @Override
    public void serialize(ValueOutput output) {
        output.store("levels", Codec.INT, levels);
    }

    @Override
    public void deserialize(ValueInput input) {
        levels = input.read("levels", Codec.INT).orElse(0);
    }
}
