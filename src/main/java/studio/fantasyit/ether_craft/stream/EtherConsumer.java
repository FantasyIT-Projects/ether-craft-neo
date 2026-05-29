package studio.fantasyit.ether_craft.stream;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.stream.cap.IStreamCapability;

import java.util.List;

public class EtherConsumer {
    private double baseFactor;
    private double factorByTime;
    private int capConsumptionSum;
    private boolean dirty = true;

    public int getTotalConsumption(int ether, int tickCount) {
        double factor = baseFactor + factorByTime * tickCount;
        return (int) Math.ceil(Math.ceil(factor * ether) + capConsumptionSum);
    }

    public void markDirty() {
        this.dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void recompute(List<IStreamCapability> caps) {
        this.baseFactor = Config.etherStreamConsumptionFactor;
        this.factorByTime = Config.etherStreamConsumptionByTimeFactor;
        this.capConsumptionSum = 0;
        for (IStreamCapability cap : caps) {
            cap.getConsumption(this);
        }
        this.dirty = false;
    }

    public void addConsumption(int amount) {
        this.capConsumptionSum += amount;
    }

    public void addBaseFactor(double d) {
        this.baseFactor += d;
    }

    public void addFactorByTime(double d) {
        this.factorByTime += d;
    }

    public State toState() {
        return new State(baseFactor, factorByTime, capConsumptionSum);
    }

    public void fromState(State s) {
        this.baseFactor = s.baseFactor();
        this.factorByTime = s.factorByTime();
        this.capConsumptionSum = s.capConsumptionSum();
        this.dirty = false;
    }

    public record State(double baseFactor, double factorByTime, int capConsumptionSum) {
        public static final StreamCodec<RegistryFriendlyByteBuf, State> CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, State::baseFactor,
                ByteBufCodecs.DOUBLE, State::factorByTime,
                ByteBufCodecs.INT, State::capConsumptionSum,
                State::new
        );
    }
}
