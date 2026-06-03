package studio.fantasyit.ether_craft.node;

import studio.fantasyit.ether_craft.Config;

public class NodeProperty {
    public int maxEther;
    public int slotUnlock;

    public boolean enableFilter;
    public boolean itemifyEther;

    public NodeProperty() {
        reset();
    }

    public void reset(){
        this.maxEther = Config.nodeDefaultMaxEther;
        this.slotUnlock = 0;
        this.enableFilter = false;
        this.itemifyEther = false;
    }
}
