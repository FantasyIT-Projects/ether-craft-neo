package studio.fantasyit.ether_craft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
@EventBusSubscriber(modid = EtherCraft.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue ETHER_CONVERT = BUILDER
            .comment("How many ether value to gain from one ether item")
            .defineInRange("ether.convert", 100, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue NODE_DEF_MAX_ETHER = BUILDER
            .comment("Max ether value of Ether Adapt Node by default")
            .defineInRange("node.def.max", 6400, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_FURNACE_BURNTIME_FACTOR = BUILDER
            .comment("...")
            .defineInRange("node.furnace.factor", 1, 1, 100);
    private static final ModConfigSpec.IntValue NODE_FURNACE_ETHER_PRE_TICK = BUILDER
            .comment("...")
            .defineInRange("node.furnace.ether_pre_tick", 10, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_MAGNET_CONSUME_PRE_STACK = BUILDER
            .comment("Ether the magnet function will consume when picking up one stack")
            .defineInRange("node.magnet.consume_pre_stack", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_CONTAINER_INTERACT_ETHER_PRE_ITEM = BUILDER
            .comment("Ether consumed per item transferred by ContainerInteract feature")
            .defineInRange("node.container_interact.ether_pre_item", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> NODE_LEVEL_SLOT_ARR = BUILDER
            .comment("Upgrade slots pre level")
            .defineList("node.up_slot", () -> List.of(2, 4, 6), () -> 0, t -> {
                try {
                    if (t instanceof Integer) return true;
                    Integer.parseInt(t.toString());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
    private static final ModConfigSpec.IntValue NODE_PROCESS_MAX_PROGRESS = BUILDER
            .comment("Max progress ticks for Node Process function to complete one recipe")
            .defineInRange("node.process.max_progress", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue BREAK_BLOCK_HARDNESS_MULTIPLIER = BUILDER
            .comment("Multiplier for block hardness in ether consumption per block break")
            .defineInRange("break_block.hardness_multiplier", 20, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue BREAK_BLOCK_EFFICIENCY_DIVISOR = BUILDER
            .comment("How much ether to reduce per level of Efficiency enchantment")
            .defineInRange("break_block.efficiency_divisor", 3, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DAMAGE_ETHER_MULTIPLIER = BUILDER
            .comment("Ether consumed per point of damage dealt")
            .defineInRange("damage.ether_multiplier", 5, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue BREAK_BLOCK_CONSTANT_COST = BUILDER
            .comment("Constant ether cost added per block break on top of the formula")
            .defineInRange("break_block.constant_cost", 0, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue EMITTER_MIN_ETHER_MIN = BUILDER
            .comment("Minimum value for the emitter minimum ether slider")
            .defineInRange("emitter.min_ether.min", 0, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue EMITTER_MIN_ETHER_MAX = BUILDER
            .comment("Maximum value for the emitter minimum ether slider")
            .defineInRange("emitter.min_ether.max", 100000, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue DAMAGE_CONSTANT_COST = BUILDER
            .comment("Constant ether cost added per damage instance on top of the formula")
            .defineInRange("damage.constant_cost", 0, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue EQUIPMENT_ETHER_COEFFICIENT = BUILDER
            .comment("Coefficient for ether generation from equipment consumption")
            .defineInRange("equipment_generator.ether_coefficient", 10, 1, 10000);
    private static final ModConfigSpec.IntValue ETHER_CONVERTER_COEFFICIENT = BUILDER
            .comment("Ether generated per item by the Ether Converter function plugin")
            .defineInRange("ether_converter.coefficient", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue ETHER_INACTIVATE_TICK = BUILDER
            .comment("Tick to wait before inactivating an ether inactivate convert")
            .defineInRange("ether_inactivate_convert.tick", 60, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue STREAM_MAX_TICK = BUILDER
            .comment("Maximum existence time of an Ether Stream entity in ticks (default 1200 = 60 seconds)")
            .defineInRange("stream.max_tick", 1200, 1, Integer.MAX_VALUE);
    static final ModConfigSpec SPEC = BUILDER.build();

    public static int etherConvert;
    public static int nodeDefMaxEther;
    public static int nodeFurnaceBurntimeFactor;
    public static int nodeFurnaceEtherPreTick;
    public static List<Integer> nodeLevelSlotArr;
    public static int nodeMagnetConsumePreStack;
    public static int containerInteractEtherPreItem;
    public static int breakBlockHardnessMultiplier;
    public static int breakBlockEfficiencyDivisor;
    public static int nodeProcessMaxProgress;
    public static int damageEtherMultiplier;
    public static int emitterMinEtherMin;
    public static int emitterMinEtherMax;
    public static int breakBlockConstantCost;
    public static int damageConstantCost;
    public static int equipmentEtherCoefficient;
    public static int etherConverterCoefficient;
    public static int etherInactivateTick;
    public static int streamMaxTick;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        etherConvert = ETHER_CONVERT.get();
        nodeDefMaxEther = NODE_DEF_MAX_ETHER.get();
        nodeFurnaceBurntimeFactor = NODE_FURNACE_BURNTIME_FACTOR.get();
        nodeFurnaceEtherPreTick = NODE_FURNACE_ETHER_PRE_TICK.get();
        nodeLevelSlotArr = NODE_LEVEL_SLOT_ARR.get().stream().map(t -> (Integer) t).toList();
        nodeMagnetConsumePreStack = NODE_MAGNET_CONSUME_PRE_STACK.get();
        containerInteractEtherPreItem = NODE_CONTAINER_INTERACT_ETHER_PRE_ITEM.get();
        breakBlockHardnessMultiplier = BREAK_BLOCK_HARDNESS_MULTIPLIER.get();
        breakBlockEfficiencyDivisor = BREAK_BLOCK_EFFICIENCY_DIVISOR.get();
        nodeProcessMaxProgress = NODE_PROCESS_MAX_PROGRESS.get();
        emitterMinEtherMin = EMITTER_MIN_ETHER_MIN.get();
        emitterMinEtherMax = EMITTER_MIN_ETHER_MAX.get();
        damageEtherMultiplier = DAMAGE_ETHER_MULTIPLIER.get();
        breakBlockConstantCost = BREAK_BLOCK_CONSTANT_COST.get();
        damageConstantCost = DAMAGE_CONSTANT_COST.get();
        equipmentEtherCoefficient = EQUIPMENT_ETHER_COEFFICIENT.get();
        etherConverterCoefficient = ETHER_CONVERTER_COEFFICIENT.get();
        etherInactivateTick = ETHER_INACTIVATE_TICK.get();
        streamMaxTick = STREAM_MAX_TICK.get();
    }
}
