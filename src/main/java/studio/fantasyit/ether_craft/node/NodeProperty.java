package studio.fantasyit.ether_craft.node;

import studio.fantasyit.ether_craft.Config;

public class NodeProperty {
    public int maxEther;
    public int slotUnlock;

    public boolean enableFilter;
    public boolean itemifyEther;
    public boolean specialRenderer;
    public int specialLevels;
    public boolean receiveRedstoneSignal;
    public boolean sendRedstoneSignal;

    public NodeProperty() {
        reset();
    }

    public void reset(){
        this.maxEther = Config.nodeDefaultMaxEther;
        this.slotUnlock = 0;
        this.enableFilter = false;
        this.itemifyEther = false;
        this.specialRenderer = false;
        this.specialLevels = 0;
        this.receiveRedstoneSignal = false;
        this.sendRedstoneSignal = false;
    }
}
