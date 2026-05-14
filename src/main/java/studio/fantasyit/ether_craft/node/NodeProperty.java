package studio.fantasyit.ether_craft.node;

import studio.fantasyit.ether_craft.Config;

public class NodeProperty {
    public int maxEther;
    public int slotUnlock;

    public int streamPreventDecay;
    public NodeProperty() {
        reset();
    }

    public void reset(){
        this.maxEther = Config.nodeDefMaxEther;
        this.slotUnlock = 0;
        this.streamPreventDecay = 0;
    }
}
