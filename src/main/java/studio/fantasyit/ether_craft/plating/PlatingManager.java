package studio.fantasyit.ether_craft.plating;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.plating.effects.AntiDarknessPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.AntiSonicBoomPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.BlockPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.BreakToInventoryPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CamouflagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CoyoteTimePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CritDamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CritPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DashPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.EtherStreamBreakPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.EtherStreamDamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.EtherStreamDashPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.EthicPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HeadHuntPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HighJumpPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.KillToInventoryPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.NoGravityPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.SilentStepPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.StoneAbsorbPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.TrackingPlatingEffect;

import java.util.HashMap;
import java.util.Map;

public class PlatingManager {
    private static final Map<Identifier, IPlatingEffect> EFFECTS = new HashMap<>();

    public static void register(Identifier id, IPlatingEffect effect) {
        EFFECTS.put(id, effect);
    }

    @Nullable
    public static IPlatingEffect getEffect(Identifier id) {
        return EFFECTS.get(id);
    }

    public static void init() {
        register(DamagePlatingEffect.ID, new DamagePlatingEffect());
        register(DashPlatingEffect.ID, new DashPlatingEffect());
        register(HighJumpPlatingEffect.ID, new HighJumpPlatingEffect());
        register(NoGravityPlatingEffect.ID, new NoGravityPlatingEffect());
        register(CoyoteTimePlatingEffect.ID, new CoyoteTimePlatingEffect());
        register(CamouflagePlatingEffect.ID, new CamouflagePlatingEffect());
        register(BlockPlatingEffect.ID, new BlockPlatingEffect());
        register(CritPlatingEffect.ID, new CritPlatingEffect());
        register(CritDamagePlatingEffect.ID, new CritDamagePlatingEffect());
        register(HeadHuntPlatingEffect.ID, new HeadHuntPlatingEffect());
        register(TrackingPlatingEffect.ID, new TrackingPlatingEffect());
        register(BreakToInventoryPlatingEffect.ID, new BreakToInventoryPlatingEffect());
        register(KillToInventoryPlatingEffect.ID, new KillToInventoryPlatingEffect());
        register(StoneAbsorbPlatingEffect.ID, new StoneAbsorbPlatingEffect());
        register(EtherStreamDashPlatingEffect.ID, new EtherStreamDashPlatingEffect());
        register(EtherStreamDamagePlatingEffect.ID, new EtherStreamDamagePlatingEffect());
        register(EtherStreamBreakPlatingEffect.ID, new EtherStreamBreakPlatingEffect());
        register(AntiDarknessPlatingEffect.ID, new AntiDarknessPlatingEffect());
        register(EthicPlatingEffect.ID, new EthicPlatingEffect());
        register(AntiSonicBoomPlatingEffect.ID, new AntiSonicBoomPlatingEffect());
        register(SilentStepPlatingEffect.ID, new SilentStepPlatingEffect());
    }
}
