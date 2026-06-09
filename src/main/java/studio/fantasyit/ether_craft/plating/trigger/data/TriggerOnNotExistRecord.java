package studio.fantasyit.ether_craft.plating.trigger.data;

import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

public record TriggerOnNotExistRecord(Set<Identifier> applied) {
    public TriggerOnNotExistRecord copyWithNew(Identifier applied) {
        Set<Identifier> newApplied = new HashSet<>(this.applied);
        newApplied.add(applied);
        return new TriggerOnNotExistRecord(newApplied);
    }

    public TriggerOnNotExistRecord copyWithRemoved(Set<Identifier> applied) {
        Set<Identifier> newApplied = new HashSet<>(this.applied);
        newApplied.removeAll(applied);
        return new TriggerOnNotExistRecord(newApplied);
    }
}
