package studio.fantasyit.ether_craft.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

public class EtherProcessWorkingChip {
    public static final EtherProcessWorkingChip DUMMY = new EtherProcessWorkingChip();
    public static Codec<EtherProcessWorkingChip> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.CODEC.fieldOf("item").forGetter(t -> t.item),
            Codec.LONG.fieldOf("ether").forGetter(t -> t.ether),
            Codec.LONG.fieldOf("maxEther").forGetter(t -> t.maxEther),
            Codec.INT.fieldOf("etherDecay").forGetter(t -> t.etherDecay),
            Codec.LONG.fieldOf("etherRequire").forGetter(t -> t.etherRequire),
            Codec.LONG.fieldOf("etherConsume").forGetter(t -> t.etherConsume),
            Codec.INT.optionalFieldOf("durability", 0).forGetter(t -> t.durability),
            EtherProcessChipManager.ProcessChipEffectConfig.CODEC.fieldOf("effect").orElse(EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT).forGetter(t -> t.effect)
    ).apply(i, EtherProcessWorkingChip::new));

    public ItemStack item;
    //以太存储量
    public long ether;
    //最大以太存储量
    public long maxEther;
    //以太衰减周期（w）
    public int etherDecay;
    //加工以太需求（开始加工的以太需求量）
    public long etherRequire;
    //加工以太消耗
    public long etherConsume;
    //effect
    public EtherProcessChipManager.ProcessChipEffectConfig effect;

    protected long[] decayCircle;
    protected int head;

    public boolean destroyed = false;
    public int durability;
    public int maxDurability;
    public @Nullable IProcessChipBehavior behavior;

    private EtherProcessWorkingChip() {
        this(ItemStack.EMPTY, 0, 0, 1, 0, 0);
    }

    public EtherProcessWorkingChip(ItemStack item) {
        this(item, 0);
    }

    public EtherProcessWorkingChip(ItemStack item, long beforeEther) {
        Identifier id = item.get(DataComponentRegistry.CHIP_ID);
        EtherProcessChipManager.ProcessChipRecord r = null;
        if (id != null)
            r = EtherProcessChipManager.get(id);
        this.item = item;
        if (r == null) {
            this.maxEther = 0;
            this.etherDecay = 1;
            this.etherRequire = 0;
            this.etherConsume = 0;
            this.maxDurability = 0;
            this.effect = EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT;
        } else {
            this.maxEther = r.maxEther();
            this.etherDecay = r.etherDecay();
            this.etherRequire = r.etherRequire();
            this.etherConsume = r.etherConsume();
            this.maxDurability = r.maxDurability();
            if (r.behavior().isPresent())
                this.behavior = EtherProcessChipManager.getBehavior(r.behavior().get());
            this.effect = r.effect();
        }
        this.durability = resolveDurability(item, this.maxDurability);
        init();
        this.addEther(beforeEther);
    }

    /**
     * @param item
     * @param ether        当前以太存储量
     * @param maxEther     最大以太存储量
     * @param etherDecay   以太衰减周期（w）
     * @param etherRequire 加工以太需求（开始加工的以太需求量）
     * @param etherConsume 加工以太消耗
     */
    public EtherProcessWorkingChip(ItemStack item, long ether, long maxEther, int etherDecay, long etherRequire, long etherConsume) {
        this(item, ether, maxEther, etherDecay, etherRequire, etherConsume, 0, EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT);
    }

    public EtherProcessWorkingChip(ItemStack item, long ether, long maxEther, int etherDecay, long etherRequire, long etherConsume, int durability, EtherProcessChipManager.ProcessChipEffectConfig effect) {
        this.item = item;
        this.ether = ether;
        this.maxEther = maxEther;
        this.etherDecay = etherDecay;
        this.etherRequire = etherRequire;
        this.etherConsume = etherConsume;
        this.durability = durability;
        this.maxDurability = 0;
        this.effect = effect;
        init();
    }

    private static int resolveDurability(ItemStack item, int maxDurability) {
        if (maxDurability <= 0) return 0;
        Integer stored = item.get(DataComponentRegistry.DURABILITY);
        if (stored != null) return stored;
        return maxDurability;
    }

    protected void init() {
        decayCircle = new long[etherDecay];
        head = 0;
    }

    public void destory() {
        destroyed = true;
    }

    /**
     * 扣除耐久度，同时回写 ItemStack 组件
     */
    public void damage(int amount) {
        if (maxDurability <= 0) return;
        durability = Math.max(0, durability - amount);
        item.set(DataComponentRegistry.DURABILITY, durability);
        if (durability <= 0) destory();
    }

    /**
     * 物品Tick (decay)
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
        return !destroyed && ether >= etherRequire && (maxDurability <= 0 || durability > 0);
    }

    /**
     * 消耗以太
     *
     * @return 是否成功消耗
     */
    public boolean consume() {
        if (canWork()) {
            long restToConsume = etherConsume;
            for (int i = etherDecay - 1; i >= 0; i--) {
                long toCost = Math.min(restToConsume, decayCircle[(i + head) % etherDecay]);
                decayCircle[(i + head) % etherDecay] -= toCost;
                restToConsume -= toCost;
                if (restToConsume == 0) {
                    break;
                }
            }
            ether -= etherConsume - restToConsume;
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
    public long addEther(long ether) {
        if (destroyed) {
            return ether;
        }
        long added = ether;
        if (this.ether + added > this.maxEther) {
            added = this.maxEther - this.ether;
        }
        if (added > this.maxEther * 2 / etherDecay)
            added = this.maxEther * 2 / etherDecay;
        if (added <= 0) {
            return ether;
        }
        this.ether += added;
        if (etherDecay != 0)
            this.decayCircle[(head + etherDecay - 1) % etherDecay] += added;
        return ether - added;
    }

    public boolean canConsume() {
        return !destroyed && ether >= etherConsume && (maxDurability <= 0 || durability > 0);
    }
}
