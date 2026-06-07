package studio.fantasyit.ether_craft.plating;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.effects.CoyoteTimePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.DashPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.HighJumpPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.NoGravityPlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.SoulProjectionPlatingEffect;

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
        register(EtherCraft.id("soul_projection"), new SoulProjectionPlatingEffect());
        register(EtherCraft.id("no_gravity"), new NoGravityPlatingEffect());
        register(EtherCraft.id("coyote_time"), new CoyoteTimePlatingEffect());
    }
}
