package studio.fantasyit.ether_craft.block.factory;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EtherProcessWorkingChip {
    public ItemStack item;
    //以太存储量
    public int ether;
    //最大以太存储量
    public int maxEther;
    //以太衰减周期（w）
    public int etherDecay;
    //加工以太需求（开始加工的以太需求量）
    public int etherRequire;
    //加工以太消耗
    public int etherConsume;
    protected int[] decayCircle;
    protected int head;

    public EtherProcessWorkingChip(ItemStack item) {
        this.item = item;
        //TODO 获取动态数据
        this(1,1,1,1);
    }
    /**
     * @param maxEther     最大以太存储量
     * @param etherDecay   以太衰减周期（w）
     * @param etherRequire 加工以太需求（开始加工的以太需求量）
     * @param etherConsume 加工以太消耗
     */
    public EtherProcessWorkingChip(int maxEther, int etherDecay, int etherRequire, int etherConsume) {
        this.maxEther = maxEther;
        this.etherDecay = etherDecay;
        this.etherRequire = etherRequire;
        this.etherConsume = etherConsume;
        init();
    }


    protected void init() {
        decayCircle = new int[etherDecay];
        head = 0;
    }

    /**
     * 物品Tick
     */
    public void tick() {
        if (decayCircle[head] > 0) {
            ether -= decayCircle[head];
            decayCircle[head] = 0;
        }
        head = (head + 1) % etherDecay;
    }

    /**
     * 获取当前元件是否可以工作
     *
     * @return boolean 可否工作
     */
    public boolean canWork() {
        return ether >= etherRequire;
    }

    /**
     * 消耗以太
     *
     * @return 是否成功消耗
     */
    public boolean consume() {
        if (canWork()) {
            int restToConsume = etherConsume;
            for (int i = 0; i < etherDecay; i++) {
                int toCost = Math.min(restToConsume, decayCircle[(i + head) % etherDecay]);
                decayCircle[(i + head) % etherDecay] -= toCost;
                restToConsume -= toCost;
                if (restToConsume == 0) {
                    break;
                }
            }
            ether -= etherRequire;
            return true;
        }
        return false;
    }

    /**
     * 添加以太
     *
     * @param ether 输入以太量
     * @return 剩余未添加的以太量
     */
    public int addEther(int ether) {
        int added = ether;
        if (this.ether + added > this.maxEther) {
            added = this.maxEther - this.ether;
        }
        this.ether += added;
        this.decayCircle[(head + etherDecay - 1) % etherDecay] += added;
        return ether - added;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.putInt("ether", ether);
        compoundTag.putInt("maxEther", maxEther);
        compoundTag.putInt("etherDecay", etherDecay);
        compoundTag.putInt("etherRequire", etherRequire);
        compoundTag.putInt("etherConsume", etherConsume);
        compoundTag.putIntArray("decayCircle", decayCircle);
        compoundTag.putInt("decayCircleHead", head);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ether = nbt.getInt("ether");
        maxEther = nbt.getInt("maxEther");
        etherDecay = nbt.getInt("etherDecay");
        etherRequire = nbt.getInt("etherRequire");
        etherConsume = nbt.getInt("etherConsume");
        decayCircle = nbt.getIntArray("decayCircle").clone();
        head = nbt.getInt("decayCircleHead");
    }

    @Override
    public void appendHoverText(@NotNull ItemStack p_41421_, @Nullable Level p_41422_, @NotNull List<Component> p_41423_, @NotNull TooltipFlag p_41424_) {
        super.appendHoverText(p_41421_, p_41422_, p_41423_, p_41424_);
        p_41423_.add(Component.literal("ether:" + ether + "/" + maxEther));
    }
}
