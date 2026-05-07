package studio.fantasyit.ether_craft.block.base;

import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

public class EtherJournal extends SnapshotJournal<Long> {
    private final EtherContainer etherContainer;
    public EtherJournal(EtherContainer etherContainer) {
        super();
        this.etherContainer = etherContainer;
    }
    @Override
    protected Long createSnapshot() {
        return etherContainer.getEther();
    }

    @Override
    protected void revertToSnapshot(Long snapshot) {
        etherContainer.setEther(snapshot);
    }

    @Override
    protected void onRootCommit(Long originalState) {
        if (originalState != etherContainer.getEther())
            etherContainer.syncClient();
    }
}
