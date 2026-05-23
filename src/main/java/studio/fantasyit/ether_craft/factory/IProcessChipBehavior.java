package studio.fantasyit.ether_craft.factory;

import studio.fantasyit.ether_craft.block.factory.EtherProcessFactoryEntity;

public interface IProcessChipBehavior {
    void onTick(EtherProcessWorkingChip chip, EtherProcessFactoryEntity factory);
}
