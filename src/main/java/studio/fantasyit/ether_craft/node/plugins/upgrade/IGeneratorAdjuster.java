package studio.fantasyit.ether_craft.node.plugins.upgrade;

public interface IGeneratorAdjuster {
    record AdjustedParameters(int burnTicks, int preTick) {
    }

    AdjustedParameters adjust(AdjustedParameters adjustedParameters);
}
