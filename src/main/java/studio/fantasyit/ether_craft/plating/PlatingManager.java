package studio.fantasyit.ether_craft.plating;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.plating.effects.DamagePlatingEffect;
import studio.fantasyit.ether_craft.plating.effects.IPlatingEffect;

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
    }
}
