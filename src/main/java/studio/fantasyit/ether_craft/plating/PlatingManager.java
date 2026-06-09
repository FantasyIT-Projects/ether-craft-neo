package studio.fantasyit.ether_craft.plating;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.effects.BlockPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.BreakToInventoryPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CamouflagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CoyoteTimePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CritDamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.CritPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DashPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HeadHuntPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HighJumpPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.KillToInventoryPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.NoGravityPlatingEffect;
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
        register(EtherCraft.id("damage"), new DamagePlatingEffect());
        register(EtherCraft.id("dash"), new DashPlatingEffect());
        register(EtherCraft.id("high_jump"), new HighJumpPlatingEffect());
        register(EtherCraft.id("no_gravity"), new NoGravityPlatingEffect());
        register(EtherCraft.id("coyote_time"), new CoyoteTimePlatingEffect());
        register(EtherCraft.id("camouflage"), new CamouflagePlatingEffect());
        register(EtherCraft.id("block"), new BlockPlatingEffect());
        register(EtherCraft.id("crit"), new CritPlatingEffect());
        register(EtherCraft.id("crit_damage"), new CritDamagePlatingEffect());
        register(EtherCraft.id("head_hunt"), new HeadHuntPlatingEffect());
        register(EtherCraft.id("tracking"), new TrackingPlatingEffect());
        register(EtherCraft.id("break_to_inv"), new BreakToInventoryPlatingEffect());
        register(EtherCraft.id("kill_to_inv"), new KillToInventoryPlatingEffect());
        register(EtherCraft.id("stone_absorb"), new StoneAbsorbPlatingEffect());
    }
}
