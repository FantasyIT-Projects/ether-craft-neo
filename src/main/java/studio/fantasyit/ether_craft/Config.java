package studio.fantasyit.ether_craft;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

@EventBusSubscriber(modid = EtherCraft.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // ===== ether — Core ether mechanics =====

    private static final ModConfigSpec.IntValue ETHER_CONVERT = BUILDER
            .comment("How many ether value to gain from one ether item")
            .defineInRange("ether.convert", 100, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ETHER_INACTIVATE_CONVERT_TICK = BUILDER
            .comment("Ticks an ether item entity must wait before auto-converting to inactivated ether")
            .defineInRange("ether.inactivate_convert_tick", 100, 1, Integer.MAX_VALUE);

    // ===== node — Ether Adapt Node =====

    private static final ModConfigSpec.IntValue NODE_DEFAULT_MAX_ETHER = BUILDER
            .comment("Default max ether value of Ether Adapt Node")
            .defineInRange("node.default_max_ether", 6400, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> NODE_UPGRADE_SLOTS = BUILDER
            .comment("Number of upgrade slots per level")
            .defineList("node.upgrade_slots", () -> List.of(2, 4, 6), () -> 0, t -> {
                try {
                    if (t instanceof Integer) return true;
                    Integer.parseInt(t.toString());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });

    // -- node.furnace --

    private static final ModConfigSpec.IntValue NODE_FURNACE_BURN_TIME_FACTOR = BUILDER
            .comment("Divisor applied to item burn time (higher = faster fuel consumption)")
            .defineInRange("node.furnace.burn_time_factor", 4, 1, 100);

    private static final ModConfigSpec.IntValue NODE_FURNACE_ETHER_PER_TICK = BUILDER
            .comment("Ether generated per tick while furnace is actively burning fuel")
            .defineInRange("node.furnace.ether_per_tick", 25, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue NODE_BLAST_FURNACE_ETHER_PER_TICK = BUILDER
            .comment("Ether generated per tick while blast furnace is actively burning fuel")
            .defineInRange("node.furnace.ether_per_tick_blast_furnace", 30, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_BLAST_FURNACE_BURN_TIME_FACTOR = BUILDER
            .comment("Divisor applied to item burn time (higher = faster fuel consumption)")
            .defineInRange("node.furnace.burn_time_factor_blast_furnace", 4, 1, 100);

    // -- node.magnet --

    private static final ModConfigSpec.IntValue NODE_MAGNET_ETHER_PER_STACK = BUILDER
            .comment("Ether consumed by magnet function when picking up one stack of items")
            .defineInRange("node.magnet.ether_per_stack", 100, 1, Integer.MAX_VALUE);

    // -- node.container_interact --

    private static final ModConfigSpec.IntValue NODE_CONTAINER_INTERACT_ETHER_PER_ITEM = BUILDER
            .comment("Ether consumed per item transferred by ContainerInteract feature")
            .defineInRange("node.container_interact.ether_per_item", 10, 1, Integer.MAX_VALUE);

    // -- node.dropper_thrower --

    private static final ModConfigSpec.IntValue NODE_DROPPER_THROWER_ETHER_PER_ITEM = BUILDER
            .comment("Ether consumed per item thrown by DropperThrower feature")
            .defineInRange("node.dropper_thrower.ether_per_item", 10, 1, Integer.MAX_VALUE);

    // -- node.process --

    private static final ModConfigSpec.IntValue NODE_PROCESS_MAX_PROGRESS = BUILDER
            .comment("Max progress ticks for Node Process function to complete one recipe")
            .defineInRange("node.process.max_progress", 100, 1, Integer.MAX_VALUE);

    // -- node.ether_converter --

    private static final ModConfigSpec.IntValue NODE_ETHER_CONVERTER_COEFFICIENT = BUILDER
            .comment("Ether generated per item consumed by the Ether Converter function plugin")
            .defineInRange("node.ether_converter.coefficient", 100, 1, Integer.MAX_VALUE);

    // -- node.equipment_generator --

    private static final ModConfigSpec.IntValue NODE_EQUIPMENT_GENERATOR_BURN_TICK = BUILDER
            .comment("Burn tick provided by each equipment in Equipment Generator plugin")
            .defineInRange("node.equipment_generator.burn_tick", 100, 1, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue NODE_EQUIPMENT_GENERATOR_COEFFICIENT = BUILDER
            .comment("Coefficient for ether generation from equipment consumption in Equipment Generator plugin")
            .defineInRange("node.equipment_generator.coefficient", 50, 1, 10000);
    private static final ModConfigSpec.IntValue NODE_EQUIPMENT_GENERATOR_BASE_AMOUNT = BUILDER
            .comment("Base amount of ether generated by Equipment Generator plugin")
            .defineInRange("node.equipment_generator.base_amount", 5, 1, Integer.MAX_VALUE);

    // -- node.growth_accelerator --

    private static final ModConfigSpec.IntValue NODE_GROWTH_ACCELERATOR_ETHER_COST = BUILDER
            .comment("Ether consumed per crop block accelerated by GrowthAccelerator function plugin")
            .defineInRange("node.growth_accelerator.ether_cost", 500, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue NODE_GROWTH_ACCELERATOR_RANGE = BUILDER
            .comment("Manhattan distance range for GrowthAccelerator to scan for eligible crop blocks")
            .defineInRange("node.growth_accelerator.range", 1, 1, 16);

    // -- node.emitter --

    private static final ModConfigSpec.IntValue NODE_EMITTER_MIN_ETHER_MIN = BUILDER
            .comment("Minimum bound for the emitter minimum ether slider")
            .defineInRange("node.emitter.min_ether.min", 0, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue NODE_EMITTER_MIN_ETHER_MAX = BUILDER
            .comment("Maximum bound for the emitter minimum ether slider")
            .defineInRange("node.emitter.min_ether.max", 100000, 0, Integer.MAX_VALUE);

    // -- node.enchanter --

    private static final ModConfigSpec.ConfigValue<List<? extends Integer>> NODE_ENCHANTER_ETHER_COSTS = BUILDER
            .comment("Ether cost for Enchanter plugin levels 1, 2, and 3")
            .defineList("node.enchanter.ether_costs", () -> List.of(6000, 12000, 24000), () -> 0, t -> {
                try {
                    if (t instanceof Integer) return true;
                    Integer.parseInt(t.toString());
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
            });

    private static final ModConfigSpec.IntValue NODE_ENCHANTER_MAX_PROGRESS = BUILDER
            .comment("Max progress ticks for Enchanter plugin to complete enchanting (20 ticks = 1 second)")
            .defineInRange("node.enchanter.max_progress", 100, 1, Integer.MAX_VALUE);

    // ===== ether_stream — Ether Stream entity =====

    private static final ModConfigSpec.IntValue ETHER_STREAM_MAX_TICK = BUILDER
            .comment("Maximum lifetime of an Ether Stream entity in ticks (20 ticks = 1 second)")
            .defineInRange("ether_stream.max_tick", 1200, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.DoubleValue ETHER_STREAM_GLASS_TRANSFORM_CHANCE = BUILDER
            .comment("The chance when glass will be transformed when ether stream passes")
            .defineInRange("ether_stream.glass_transform_chance", 0.5f, 0, 1);

    private static final ModConfigSpec.DoubleValue ETHER_STREAM_CONSUMPTION_FACTOR = BUILDER
            .comment("Base ether consumption per tick as a fraction of total ether stored in the stream")
            .defineInRange("ether_stream.consumption_factor", 0.005, 0, 10);

    private static final ModConfigSpec.DoubleValue ETHER_STREAM_CONSUMPTION_BY_TIME_FACTOR = BUILDER
            .comment("Additional ether consumption factor added per tick of the stream's lifetime (accumulates over time)")
            .defineInRange("ether_stream.consumption_factor_by_time", 0.0001, 0, 10);

    private static final ModConfigSpec.IntValue ETHER_GLASS_PREVENT_CONSUME = BUILDER
            .comment("...")
            .defineInRange("ether_stream.ether_glass_prevent_consume", 20, 0, Integer.MAX_VALUE);

    // -- ether_stream.break_block --

    private static final ModConfigSpec.IntValue ETHER_STREAM_BREAK_BLOCK_HARDNESS_MULTIPLIER = BUILDER
            .comment("Multiplier for block hardness in ether cost calculation per block break")
            .defineInRange("ether_stream.break_block.hardness_multiplier", 20, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ETHER_STREAM_BREAK_BLOCK_EFFICIENCY_DIVISOR = BUILDER
            .comment("Ether cost reduction per level of Efficiency enchantment when breaking blocks")
            .defineInRange("ether_stream.break_block.efficiency_divisor", 3, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ETHER_STREAM_BREAK_BLOCK_CONSTANT_COST = BUILDER
            .comment("Flat ether cost added to every block break on top of the hardness-based formula")
            .defineInRange("ether_stream.break_block.constant_cost", 0, 0, Integer.MAX_VALUE);

    // -- ether_stream.damage --

    private static final ModConfigSpec.IntValue ETHER_STREAM_DAMAGE_ETHER_MULTIPLIER = BUILDER
            .comment("Ether consumed per point of damage dealt by Ether Stream")
            .defineInRange("ether_stream.damage.ether_multiplier", 5, 1, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue ETHER_STREAM_DAMAGE_CONSTANT_COST = BUILDER
            .comment("Flat ether cost added to every damage instance on top of the damage-based formula")
            .defineInRange("ether_stream.damage.constant_cost", 0, 0, Integer.MAX_VALUE);

    // -- ether_stream.growth_accelerator --

    private static final ModConfigSpec.IntValue ETHER_STREAM_GROWTH_ACCELERATOR_ETHER_COST = BUILDER
            .comment("Ether consumed per crop block accelerated by Ether Stream Growth Accelerator capability")
            .defineInRange("ether_stream.growth_accelerator.ether_cost", 1000, 1, Integer.MAX_VALUE);

    // -- ether_stream.upgrade --
    private static final ModConfigSpec.DoubleValue ETHER_STORAGE_MULTIPLIER = BUILDER
            .comment("The multipiler each ether storage upgrade plugin will provide")
            .defineInRange("node.upgrade.ether_storage_multipiler", 2.5, 1, 200);

    public static final ModConfigSpec.IntValue ETHER_PROCESS_ETHER_CONSUME_PRE_UNMATCHED = BUILDER
            .comment("The ether consumed per tick before the node process upgrade is matched")
            .defineInRange("node.process.ether_consume_pre_unmatched", 1, 0, Integer.MAX_VALUE);

    // -- node.auto_supply --

    private static final ModConfigSpec.IntValue NODE_AUTO_SUPPLY_THRESHOLD = BUILDER
            .comment("Ether level below which the auto supply upgrade activates and starts providing ether")
            .defineInRange("node.auto_supply.threshold", 1000, 0, Integer.MAX_VALUE);

    private static final ModConfigSpec.IntValue NODE_AUTO_SUPPLY_ETHER_PER_TICK = BUILDER
            .comment("Ether provided per tick by a single auto supply upgrade")
            .defineInRange("node.auto_supply.ether_per_tick", 5, 1, Integer.MAX_VALUE);

    // ===== plating =====
    private static final ModConfigSpec.IntValue PLATING_DURATION_TICKS = BUILDER
            .comment("Duration in ticks for plating to complete (20 ticks = 1 second)")
            .defineInRange("plating.duration_ticks", 100, 1, 12000);
    private static final ModConfigSpec.IntValue PLATING_MAX_ETHER_RECEIVE = BUILDER
            .comment("Max amount to receive from one ether stream")
            .defineInRange("plating.max_ether_receive", 6400, 1, Integer.MAX_VALUE);

    // -- plating.dash --
    private static final ModConfigSpec.IntValue PLATING_DASH_CD_TICKS = BUILDER
            .comment("Cooldown ticks for Dash plating effect")
            .defineInRange("plating.dash.cd_ticks", 20, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_DASH_ETHER_COST = BUILDER
            .comment("Ether cost for Dash plating effect")
            .defineInRange("plating.dash.ether_cost", 500, 0, Integer.MAX_VALUE);

    // -- plating.high_jump --
    private static final ModConfigSpec.IntValue PLATING_HIGH_JUMP_CD_TICKS = BUILDER
            .comment("Cooldown ticks for High Jump plating effect")
            .defineInRange("plating.high_jump.cd_ticks", 20, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_HIGH_JUMP_ETHER_COST = BUILDER
            .comment("Ether cost for High Jump plating effect")
            .defineInRange("plating.high_jump.ether_cost", 1000, 0, Integer.MAX_VALUE);

    // -- plating.soul --
    private static final ModConfigSpec.IntValue PLATING_SOUL_ETHER_PER_TICK = BUILDER
            .comment("Ether consumed per tick by Soul Projection plating effect")
            .defineInRange("plating.soul.ether_per_tick", 1, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue PLATING_SOUL_MAX_RANGE = BUILDER
            .comment("Maximum range in blocks for Soul Projection camera")
            .defineInRange("plating.soul.max_range", 64, 1, 512);

    // -- plating.no_gravity --
    private static final ModConfigSpec.IntValue PLATING_NO_GRAVITY_ETHER_PER_ARROW = BUILDER
            .comment("Ether cost per arrow for No Gravity plating effect")
            .defineInRange("plating.no_gravity.ether_per_arrow", 125, 0, Integer.MAX_VALUE);

    // -- plating.coyote_time --
    private static final ModConfigSpec.IntValue PLATING_COYOTE_TIME_ETHER_PER_JUMP = BUILDER
            .comment("Ether cost per delayed jump for Coyote Time plating effect")
            .defineInRange("plating.coyote_time.ether_per_jump", 5, 0, Integer.MAX_VALUE);

    // -- plating.camouflage --
    private static final ModConfigSpec.IntValue PLATING_CAMOUFLAGE_STAND_DURATION = BUILDER
            .comment("Ticks the player must stand still before Camouflage plating activates (20 ticks = 1 second)")
            .defineInRange("plating.camouflage.stand_duration", 100, 1, 12000);
    private static final ModConfigSpec.IntValue PLATING_CAMOUFLAGE_ETHER_COST = BUILDER
            .comment("Ether consumed by Camouflage")
            .defineInRange("plating.camouflage.ether_per_tick", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue PLATING_CAMOUFLAGE_SPEED_THRESHOLD = BUILDER
            .comment("The max speed that allows user to enter camouflage status.")
            .defineInRange("plating.camouflage.speed_threshold", 0.05, 0, 20);
    private static final ModConfigSpec.IntValue PLATING_CAMOUFLAGE_GAIN_ETHER_PER_TICK = BUILDER
            .comment("The ether gain in camouflage status")
            .defineInRange("plating.camouflage.gain_per_tick", 2, 0, Integer.MAX_VALUE);

    // -- plating.block --
    private static final ModConfigSpec.DoubleValue PLATING_BLOCK_DAMAGE_REDUCTION = BUILDER
            .comment("Fraction of frontal damage reduced while blocking (0~1)")
            .defineInRange("plating.block.damage_reduction", 0.5, 0, 1);
    private static final ModConfigSpec.IntValue PLATING_BLOCK_ETHER_PER_TICK = BUILDER
            .comment("Ether consumed per tick while blocking")
            .defineInRange("plating.block.ether_per_tick", 500, 0, Integer.MAX_VALUE);

    // -- plating.crit --
    private static final ModConfigSpec.IntValue PLATING_CRIT_ETHER_PER_ATTACK = BUILDER
            .comment("Ether consumed per attack by Crit plating")
            .defineInRange("plating.crit.ether_per_attack", 75, 0, Integer.MAX_VALUE);

    // -- plating.crit_damage --
    private static final ModConfigSpec.IntValue PLATING_CRIT_DAMAGE_ETHER_PER_ATTACK = BUILDER
            .comment("Ether consumed per attack by Crit Damage plating")
            .defineInRange("plating.crit_damage.ether_per_attack", 75, 0, Integer.MAX_VALUE);

    // -- plating.head_hunt --
    private static final ModConfigSpec.IntValue PLATING_HEAD_HUNT_ETHER_PER_KILL = BUILDER
            .comment("Ether consumed per kill by Head Hunt plating")
            .defineInRange("plating.head_hunt.ether_per_kill", 5, 0, Integer.MAX_VALUE);

    // -- plating.tracking --
    private static final ModConfigSpec.IntValue PLATING_TRACKING_ETHER_PER_ARROW = BUILDER
            .comment("Ether consumed per arrow by Tracking plating")
            .defineInRange("plating.tracking.ether_per_arrow", 125, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue PLATING_TRACKING_RANGE = BUILDER
            .comment("Search radius in blocks for arrow tracking")
            .defineInRange("plating.tracking.range", 16.0, 0, 32);

    // -- plating.break_to_inv --
    private static final ModConfigSpec.IntValue PLATING_BREAK_TO_INV_ETHER_PER_BLOCK = BUILDER
            .comment("Ether consumed per block broken by Break to Inventory plating")
            .defineInRange("plating.break_to_inv.ether_per_block", 125, 0, Integer.MAX_VALUE);

    // -- plating.kill_to_inv --
    private static final ModConfigSpec.IntValue PLATING_KILL_TO_INV_ETHER_PER_KILL = BUILDER
            .comment("Ether consumed per kill by Kill to Inventory plating")
            .defineInRange("plating.kill_to_inv.ether_per_kill", 125, 0, Integer.MAX_VALUE);

    // -- plating.stone_absorb --
    private static final ModConfigSpec.IntValue PLATING_STONE_ABSORB_ETHER_PER_BLOCK = BUILDER
            .comment("Ether gained per stone-type block broken by Stone Absorb plating")
            .defineInRange("plating.stone_absorb.ether_per_block", 125, 0, Integer.MAX_VALUE);

    // -- plating.ether_stream_dash --
    private static final ModConfigSpec.IntValue PLATING_ETHER_STREAM_DASH_CD_TICKS = BUILDER
            .comment("Cooldown ticks for Ether Stream Dash plating effect")
            .defineInRange("plating.ether_stream_dash.cd_ticks", 5, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_ETHER_STREAM_DASH_ETHER_COST = BUILDER
            .comment("Ether cost for Ether Stream Dash plating effect")
            .defineInRange("plating.ether_stream_dash.ether_cost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue PLATING_ETHER_STREAM_DASH_SPEED = BUILDER
            .comment("Speed multiplier for Ether Stream Dash")
            .defineInRange("plating.ether_stream_dash.speed", 0.25, 0.1, 10.0);
    private static final ModConfigSpec.DoubleValue PLATING_ETHER_STREAM_DASH_FASTER_SPEED = BUILDER
            .comment("Speed multiplier for Ether Stream Dash Faster")
            .defineInRange("plating.ether_stream_dash.speed_faster", 2.5, 0.1, 10.0);

    // -- plating.ether_stream_damage --
    private static final ModConfigSpec.IntValue PLATING_ETHER_STREAM_DAMAGE_CD_TICKS = BUILDER
            .comment("Cooldown ticks for Ether Stream Damage plating effect")
            .defineInRange("plating.ether_stream_damage.cd_ticks", 2, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_ETHER_STREAM_DAMAGE_ETHER_COST = BUILDER
            .comment("Ether cost for Ether Stream Damage plating effect")
            .defineInRange("plating.ether_stream_damage.ether_cost", 50, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue PLATING_ETHER_STREAM_DAMAGE_SPEED = BUILDER
            .comment("Speed multiplier for Ether Stream Damage")
            .defineInRange("plating.ether_stream_damage.speed", 1.0, 0.01, 10.0);

    // -- plating.ether_stream_break --
    private static final ModConfigSpec.IntValue PLATING_ETHER_STREAM_BREAK_CD_TICKS = BUILDER
            .comment("Cooldown ticks for Ether Stream Break plating effect")
            .defineInRange("plating.ether_stream_break.cd_ticks", 5, 0, 12000);
    private static final ModConfigSpec.IntValue PLATING_ETHER_STREAM_BREAK_ETHER_COST = BUILDER
            .comment("Ether cost for Ether Stream Break plating effect")
            .defineInRange("plating.ether_stream_break.ether_cost", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.DoubleValue PLATING_ETHER_STREAM_BREAK_SPEED = BUILDER
            .comment("Speed multiplier for Ether Stream Break")
            .defineInRange("plating.ether_stream_break.speed", 1.0, 0.01, 10.0);

    // -- plating.anti_darkness --
    private static final ModConfigSpec.IntValue PLATING_ANTI_DARKNESS_ETHER_PER_BLOCK = BUILDER
            .comment("Ether consumed when blocking Darkness or Blindness effect")
            .defineInRange("plating.anti_darkness.ether_per_block", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue PLATING_ANTI_DARKNESS_ETHER_PER_TICK = BUILDER
            .comment("Ether consumed per tick for Night Vision")
            .defineInRange("plating.anti_darkness.ether_per_tick", 1, 0, Integer.MAX_VALUE);

    // -- plating.ethic --
    private static final ModConfigSpec.IntValue PLATING_ETHIC_ETHER_PER_USE = BUILDER
            .comment("Ether consumed per ethical action")
            .defineInRange("plating.ethic.ether_per_use", 500, 0, Integer.MAX_VALUE);
    private static final ModConfigSpec.IntValue PLATING_ETHIC_CD = BUILDER
            .comment("Cd of ethical action")
            .defineInRange("plating.ethic.cd", 20, 0, Integer.MAX_VALUE);

    // -- plating.anti_sonic_boom --
    private static final ModConfigSpec.IntValue PLATING_ANTI_SONIC_BOOM_ETHER_PER_BLOCK = BUILDER
            .comment("Ether consumed when blocking sonic boom damage")
            .defineInRange("plating.anti_sonic_boom.ether_per_block", 5000, 0, Integer.MAX_VALUE);

    // -- plating.silent_step --
    private static final ModConfigSpec.IntValue PLATING_SILENT_STEP_ETHER_PER_TICK = BUILDER
            .comment("Ether consumed per tick when suppressing vibration events")
            .defineInRange("plating.silent_step.ether_per_tick", 1, 0, Integer.MAX_VALUE);

    // -- plating.durability_absorption --
    private static final ModConfigSpec.IntValue PLATING_DURABILITY_ABSORPTION_ETHER_PER_DURABILITY = BUILDER
            .comment("Ether consumed per durability point absorbed. Minimum 1.")
            .defineInRange("plating.durability_absorption.ether_per_durability", 500, 1, Integer.MAX_VALUE);


    static final ModConfigSpec SPEC = BUILDER.build();

    public static int etherConvert;
    public static int etherInactivateConvertTick;
    public static int nodeDefaultMaxEther;
    public static List<Integer> nodeUpgradeSlots;
    public static int nodeFurnaceBurnTimeFactor;
    public static int nodeFurnaceEtherPerTick;
    public static int nodeBlastFurnaceBurnTimeFactor;
    public static int nodeBlastFurnaceEtherPerTick;
    public static int nodeMagnetEtherPerStack;
    public static int nodeContainerInteractEtherPerItem;
    public static int nodeDropperThrowerEtherPerItem;
    public static int nodeProcessMaxProgress;
    public static int nodeEtherConverterCoefficient;
    public static int nodeEquipmentGeneratorBurnTick;
    public static int nodeEquipmentGeneratorCoefficient;
    public static int nodeEquipmentGeneratorBaseAmount;
    public static int nodeGrowthAcceleratorEtherCost;
    public static int nodeGrowthAcceleratorRange;
    public static int nodeEmitterMinEtherMin;
    public static int nodeEmitterMinEtherMax;
    public static int etherStreamMaxTick;
    public static double etherStreamGlassTransformChance;
    public static double etherStreamConsumptionFactor;
    public static double etherStreamConsumptionByTimeFactor;
    public static int etherStreamBreakBlockHardnessMultiplier;
    public static int etherStreamBreakBlockEfficiencyDivisor;
    public static int etherStreamBreakBlockConstantCost;
    public static int etherStreamDamageEtherMultiplier;
    public static int etherStreamDamageConstantCost;
    public static int etherStreamGrowthAcceleratorEtherCost;
    public static List<Integer> nodeEnchanterEtherCosts;
    public static int nodeEnchanterMaxProgress;
    public static double etherStorageMultiplier;
    public static int etherGlassPreventConsume;
    public static int nodeProcessEtherConsumePreUnmatched;
    public static int etherAutoSupplyThreshold;
    public static int etherAutoSupplyEtherPerTick;
    public static int platingDurationTicks;
    public static int platingMaxEtherReceive;
    public static int platingDashCdTicks;
    public static int platingDashEtherCost;
    public static int platingHighJumpCdTicks;
    public static int platingHighJumpEtherCost;
    public static int platingSoulEtherPerTick;
    public static int platingSoulMaxRange;
    public static int platingNoGravityEtherPerArrow;
    public static int platingCoyoteTimeEtherPerJump;
    public static int platingCamouflageStandDuration;
    public static double platingCamouflageSpeedThreshold;
    public static int platingCamouflageGainEtherPerTick;
    public static int platingCamouflageEtherCost;
    public static double platingBlockDamageReduction;
    public static int platingBlockEtherPerTick;
    public static int platingCritEtherPerAttack;
    public static int platingCritDamageEtherPerAttack;
    public static int platingHeadHuntEtherPerKill;
    public static int platingTrackingEtherPerArrow;
    public static double platingTrackingRange;
    public static int platingBreakToInvEtherPerBlock;
    public static int platingKillToInvEtherPerKill;
    public static int platingStoneAbsorbEtherPerBlock;
    public static int platingEtherStreamDashCdTicks;
    public static int platingEtherStreamDashEtherCost;
    public static double platingEtherStreamDashSpeed;
    public static double platingEtherStreamDashFasterSpeed;
    public static int platingEtherStreamDamageCdTicks;
    public static int platingEtherStreamDamageEtherCost;
    public static double platingEtherStreamDamageSpeed;
    public static int platingEtherStreamBreakCdTicks;
    public static int platingEtherStreamBreakEtherCost;
    public static double platingEtherStreamBreakSpeed;
    public static int platingAntiDarknessEtherPerBlock;
    public static int platingAntiDarknessEtherPerTick;
    public static int platingEthicEtherPerUse;
    public static int platingEthicCD;
    public static int platingAntiSonicBoomEtherPerBlock;
    public static int platingSilentStepEtherPerTick;
    public static int platingDurabilityAbsorptionEtherPerDurability;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        etherConvert = ETHER_CONVERT.get();
        etherInactivateConvertTick = ETHER_INACTIVATE_CONVERT_TICK.get();
        nodeDefaultMaxEther = NODE_DEFAULT_MAX_ETHER.get();
        nodeUpgradeSlots = NODE_UPGRADE_SLOTS.get().stream().map(t -> (Integer) t).toList();
        nodeFurnaceBurnTimeFactor = NODE_FURNACE_BURN_TIME_FACTOR.get();
        nodeFurnaceEtherPerTick = NODE_FURNACE_ETHER_PER_TICK.get();
        nodeBlastFurnaceBurnTimeFactor = NODE_BLAST_FURNACE_BURN_TIME_FACTOR.get();
        nodeBlastFurnaceEtherPerTick = NODE_BLAST_FURNACE_ETHER_PER_TICK.get();
        nodeMagnetEtherPerStack = NODE_MAGNET_ETHER_PER_STACK.get();
        nodeContainerInteractEtherPerItem = NODE_CONTAINER_INTERACT_ETHER_PER_ITEM.get();
        nodeDropperThrowerEtherPerItem = NODE_DROPPER_THROWER_ETHER_PER_ITEM.get();
        nodeProcessMaxProgress = NODE_PROCESS_MAX_PROGRESS.get();
        nodeEtherConverterCoefficient = NODE_ETHER_CONVERTER_COEFFICIENT.get();
        nodeEquipmentGeneratorBurnTick = NODE_EQUIPMENT_GENERATOR_BURN_TICK.get();
        nodeEquipmentGeneratorCoefficient = NODE_EQUIPMENT_GENERATOR_COEFFICIENT.get();
        nodeEquipmentGeneratorBaseAmount = NODE_EQUIPMENT_GENERATOR_BASE_AMOUNT.get();
        nodeGrowthAcceleratorEtherCost = NODE_GROWTH_ACCELERATOR_ETHER_COST.get();
        nodeGrowthAcceleratorRange = NODE_GROWTH_ACCELERATOR_RANGE.get();
        nodeEmitterMinEtherMin = NODE_EMITTER_MIN_ETHER_MIN.get();
        nodeEmitterMinEtherMax = NODE_EMITTER_MIN_ETHER_MAX.get();
        etherStreamMaxTick = ETHER_STREAM_MAX_TICK.get();
        etherStreamGlassTransformChance = ETHER_STREAM_GLASS_TRANSFORM_CHANCE.get();
        etherStreamConsumptionFactor = ETHER_STREAM_CONSUMPTION_FACTOR.get();
        etherStreamConsumptionByTimeFactor = ETHER_STREAM_CONSUMPTION_BY_TIME_FACTOR.get();
        etherStreamBreakBlockHardnessMultiplier = ETHER_STREAM_BREAK_BLOCK_HARDNESS_MULTIPLIER.get();
        etherStreamBreakBlockEfficiencyDivisor = ETHER_STREAM_BREAK_BLOCK_EFFICIENCY_DIVISOR.get();
        etherStreamBreakBlockConstantCost = ETHER_STREAM_BREAK_BLOCK_CONSTANT_COST.get();
        etherStreamDamageEtherMultiplier = ETHER_STREAM_DAMAGE_ETHER_MULTIPLIER.get();
        etherStreamDamageConstantCost = ETHER_STREAM_DAMAGE_CONSTANT_COST.get();
        etherStreamGrowthAcceleratorEtherCost = ETHER_STREAM_GROWTH_ACCELERATOR_ETHER_COST.get();
        nodeEnchanterEtherCosts = NODE_ENCHANTER_ETHER_COSTS.get().stream().map(t -> (Integer) t).toList();
        nodeEnchanterMaxProgress = NODE_ENCHANTER_MAX_PROGRESS.get();
        etherStorageMultiplier = ETHER_STORAGE_MULTIPLIER.get();
        etherGlassPreventConsume = ETHER_GLASS_PREVENT_CONSUME.get();
        nodeProcessEtherConsumePreUnmatched = ETHER_PROCESS_ETHER_CONSUME_PRE_UNMATCHED.get();
        etherAutoSupplyThreshold = NODE_AUTO_SUPPLY_THRESHOLD.get();
        etherAutoSupplyEtherPerTick = NODE_AUTO_SUPPLY_ETHER_PER_TICK.get();
        platingDurationTicks = PLATING_DURATION_TICKS.get();
        platingMaxEtherReceive = PLATING_MAX_ETHER_RECEIVE.get();
        platingDashCdTicks = PLATING_DASH_CD_TICKS.get();
        platingDashEtherCost = PLATING_DASH_ETHER_COST.get();
        platingHighJumpCdTicks = PLATING_HIGH_JUMP_CD_TICKS.get();
        platingHighJumpEtherCost = PLATING_HIGH_JUMP_ETHER_COST.get();
        platingSoulEtherPerTick = PLATING_SOUL_ETHER_PER_TICK.get();
        platingSoulMaxRange = PLATING_SOUL_MAX_RANGE.get();
        platingNoGravityEtherPerArrow = PLATING_NO_GRAVITY_ETHER_PER_ARROW.get();
        platingCoyoteTimeEtherPerJump = PLATING_COYOTE_TIME_ETHER_PER_JUMP.get();
        platingCamouflageStandDuration = PLATING_CAMOUFLAGE_STAND_DURATION.get();
        platingCamouflageEtherCost = PLATING_CAMOUFLAGE_ETHER_COST.get();
        platingCamouflageGainEtherPerTick = PLATING_CAMOUFLAGE_GAIN_ETHER_PER_TICK.get();
        platingCamouflageSpeedThreshold = PLATING_CAMOUFLAGE_SPEED_THRESHOLD.get();
        platingBlockDamageReduction = PLATING_BLOCK_DAMAGE_REDUCTION.get();
        platingBlockEtherPerTick = PLATING_BLOCK_ETHER_PER_TICK.get();
        platingCritEtherPerAttack = PLATING_CRIT_ETHER_PER_ATTACK.get();
        platingCritDamageEtherPerAttack = PLATING_CRIT_DAMAGE_ETHER_PER_ATTACK.get();
        platingHeadHuntEtherPerKill = PLATING_HEAD_HUNT_ETHER_PER_KILL.get();
        platingTrackingEtherPerArrow = PLATING_TRACKING_ETHER_PER_ARROW.get();
        platingTrackingRange = PLATING_TRACKING_RANGE.get();
        platingBreakToInvEtherPerBlock = PLATING_BREAK_TO_INV_ETHER_PER_BLOCK.get();
        platingKillToInvEtherPerKill = PLATING_KILL_TO_INV_ETHER_PER_KILL.get();
        platingStoneAbsorbEtherPerBlock = PLATING_STONE_ABSORB_ETHER_PER_BLOCK.get();
        platingEtherStreamDashCdTicks = PLATING_ETHER_STREAM_DASH_CD_TICKS.get();
        platingEtherStreamDashEtherCost = PLATING_ETHER_STREAM_DASH_ETHER_COST.get();
        platingEtherStreamDashSpeed = PLATING_ETHER_STREAM_DASH_SPEED.get();
        platingEtherStreamDashFasterSpeed = PLATING_ETHER_STREAM_DASH_FASTER_SPEED.get();
        platingEtherStreamDamageCdTicks = PLATING_ETHER_STREAM_DAMAGE_CD_TICKS.get();
        platingEtherStreamDamageEtherCost = PLATING_ETHER_STREAM_DAMAGE_ETHER_COST.get();
        platingEtherStreamDamageSpeed = PLATING_ETHER_STREAM_DAMAGE_SPEED.get();
        platingEtherStreamBreakCdTicks = PLATING_ETHER_STREAM_BREAK_CD_TICKS.get();
        platingEtherStreamBreakEtherCost = PLATING_ETHER_STREAM_BREAK_ETHER_COST.get();
        platingEtherStreamBreakSpeed = PLATING_ETHER_STREAM_BREAK_SPEED.get();
        platingAntiDarknessEtherPerBlock = PLATING_ANTI_DARKNESS_ETHER_PER_BLOCK.get();
        platingAntiDarknessEtherPerTick = PLATING_ANTI_DARKNESS_ETHER_PER_TICK.get();
        platingEthicEtherPerUse = PLATING_ETHIC_ETHER_PER_USE.get();
        platingEthicCD = PLATING_ETHIC_CD.get();
        platingAntiSonicBoomEtherPerBlock = PLATING_ANTI_SONIC_BOOM_ETHER_PER_BLOCK.get();
        platingSilentStepEtherPerTick = PLATING_SILENT_STEP_ETHER_PER_TICK.get();
        platingDurabilityAbsorptionEtherPerDurability = PLATING_DURABILITY_ABSORPTION_ETHER_PER_DURABILITY.get();
    }
}
