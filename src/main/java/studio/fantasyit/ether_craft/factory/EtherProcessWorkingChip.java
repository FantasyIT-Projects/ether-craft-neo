package studio.fantasyit.ether_craft.factory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import studio.fantasyit.ether_craft.Config;
import studio.fantasyit.ether_craft.register.DataComponentRegistry;

public class EtherProcessWorkingChip {
    public static final EtherProcessWorkingChip DUMMY = new EtherProcessWorkingChip();
    public static Codec<EtherProcessWorkingChip> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(t -> t.item),
            Codec.LONG.fieldOf("ether").forGetter(t -> t.ether),
            Codec.LONG.fieldOf("storage").forGetter(t -> t.storage),
            Codec.LONG.fieldOf("etherConsume").forGetter(t -> t.etherConsume),
            EtherProcessChipManager.ProcessChipEffectConfig.CODEC.fieldOf("effect").orElse(EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT).forGetter(t -> t.effect)
    ).apply(i, EtherProcessWorkingChip::new));

    public ItemStack item;
    public long ether;
    public long storage;
    public long etherConsume;
    public EtherProcessChipManager.ProcessChipEffectConfig effect;
    public long reservePer;

    private EtherProcessWorkingChip() {
        this(ItemStack.EMPTY, 0, 0, 0);
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
            this.storage = 0;
            this.etherConsume = 0;
            this.effect = EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT;
        } else {
            this.storage = r.storage();
            this.etherConsume = r.etherConsume();
            this.effect = r.effect();
        }
        this.ether = Math.max(0, beforeEther);
        this.reservePer = calcReservePer(this.etherConsume);
    }

    public EtherProcessWorkingChip(ItemStack item, long ether, long storage, long etherConsume) {
        this(item, ether, storage, etherConsume, EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT);
    }

    public EtherProcessWorkingChip(ItemStack item, long ether, long storage, long etherConsume, EtherProcessChipManager.ProcessChipEffectConfig effect) {
        this.item = item;
        this.ether = ether;
        this.storage = storage;
        this.etherConsume = etherConsume;
        this.effect = effect;
        this.reservePer = calcReservePer(etherConsume);
    }

    public void refreshReservePer() {
        this.reservePer = calcReservePer(this.etherConsume);
    }

    private static long calcReservePer(long etherConsume) {
        return Math.max(Config.factoryMinReservePer, Math.round(Config.factoryReserveMultiplier * etherConsume));
    }

    /**
     * 维持基础曲线 base(e)：
     * e <= consume: base = e * a
     * e >  consume: base = consume*a * (1 + overshoot*y*exp(λ(1-y))), y = (x-1)/(ratio-1), x = e/consume
     */
    public double baseCost() {
        double t = etherConsume;
        double a = Config.factoryBaseRatio;
        if (ether <= t) return ether * a;
        if (t <= 0) return 0;
        double r = Config.factoryPeakRatio;
        double denom = r - 1;
        double y = (ether / t - 1) / denom;
        double h = Config.factoryOvershoot * y * Math.exp(Config.factoryDecayLambda * (1 - y));
        return t * a * (1 + h);
    }

    /**
     * 速度倍率 p(e) = max(1, 1 + floor(e/storage))（整数运算，与 floor(1 + e/storage) 等价）
     */
    public long speedMul() {
        if (storage <= 0) return 1;
        return Math.max(1, 1 + ether / storage);
    }

    /**
     * 每 tick 维持开销 C(e) = base(e) * p(e)
     */
    public long maintainCost() {
        if (ether <= 0) return 0;
        if (ether <= etherConsume)
            return Math.round(ether * Config.factoryBaseRatio * speedMul());
        return Math.round(baseCost() * speedMul());
    }

    /**
     * 每 tick 扣除维持开销
     */
    public void tickMaintain() {
        if (ether <= 0) return;
        ether = Math.max(0, ether - maintainCost());
    }

    /**
     * 当前是否可以工作：以太不足 consume 直接停止
     */
    public boolean canWork() {
        return ether >= etherConsume;
    }

    /**
     * 消耗以太（加工扣款）
     *
     * @return 是否成功消耗
     */
    public boolean consume() {
        if (canWork()) {
            ether -= etherConsume;
            return true;
        }
        return false;
    }

    public long addEther(long ether) {
        this.ether += ether;
        return 0;
    }

    public boolean canConsume() {
        return ether >= etherConsume;
    }
}
