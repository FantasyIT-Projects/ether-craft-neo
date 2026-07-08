package studio.fantasyit.ether_craft.stream;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.List;

public class EtherConsumer {
    private float baseFactor;
    private float factorByTime;
    private int capConsumptionSum;
    private float globalFactor;
    private boolean isInEtherGlass;
    private boolean dirty = true;

    public int getTotalConsumption(int ether, int tickCount) {
        if (isInEtherGlass)
            tickCount = 0;
        float factor = baseFactor + factorByTime * tickCount;
        int amount = (int) Math.ceil(Math.ceil(Math.ceil(factor * ether) + capConsumptionSum) * globalFactor);
        if (isInEtherGlass)
            amount -= Config.etherGlassPreventConsume;
        return Math.max(0, amount);
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void recompute(IEtherStreamLike iEtherStreamLike, List<IStreamCapability> caps) {
        this.baseFactor = (float) Config.etherStreamConsumptionFactor;
        this.factorByTime = (float) Config.etherStreamConsumptionByTimeFactor;
        this.factorByTime *= (float) (iEtherStreamLike.deltaMovement().length() / 0.055);
        this.capConsumptionSum = 0;
        this.globalFactor = 1.0f;
        for (IStreamCapability cap : caps) {
            cap.getConsumption(this, iEtherStreamLike);
        }
        this.dirty = false;
    }

    public void addConsumption(int amount) {
        this.capConsumptionSum += amount;
    }

    public void addBaseFactor(float d) {
        this.baseFactor += d;
    }

    public void addFactorByTime(float d) {
        this.factorByTime += d;
    }

    public void multiplyGlobalFactor(float d) {
        this.globalFactor *= d;
    }

    public State toState() {
        return new State(baseFactor, factorByTime, capConsumptionSum, globalFactor, isInEtherGlass);
    }

    public void fromState(State s) {
        this.baseFactor = s.baseFactor();
        this.factorByTime = s.factorByTime();
        this.capConsumptionSum = s.capConsumptionSum();
        this.globalFactor = s.globalFactor();
        this.isInEtherGlass = s.isInEtherGlass();
        this.dirty = false;
    }

    public void setIsInEtherGlass(boolean isEtherGlass2) {
        isInEtherGlass = isEtherGlass2;
        this.dirty = true;
    }

    public record State(float baseFactor, float factorByTime, int capConsumptionSum, float globalFactor,
                        boolean isInEtherGlass) {
        public static final Codec<State> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.FLOAT.fieldOf("baseFactor").forGetter(State::baseFactor),
                Codec.FLOAT.fieldOf("factorByTime").forGetter(State::factorByTime),
                Codec.INT.fieldOf("capConsumptionSum").forGetter(State::capConsumptionSum),
                Codec.FLOAT.fieldOf("globalFactor").forGetter(State::globalFactor),
                Codec.BOOL.fieldOf("isInEtherGlass").forGetter(State::isInEtherGlass)
        ).apply(instance, State::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, State> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, State::baseFactor,
                ByteBufCodecs.FLOAT, State::factorByTime,
                ByteBufCodecs.INT, State::capConsumptionSum,
                ByteBufCodecs.FLOAT, State::globalFactor,
                ByteBufCodecs.BOOL, State::isInEtherGlass,
                State::new
        );
    }
}
