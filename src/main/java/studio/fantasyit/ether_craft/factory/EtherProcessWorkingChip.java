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
            EtherProcessChipManager.ProcessChipEffectConfig.CODEC.fieldOf("effect").orElse(EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT).forGetter(t -> t.effect),
            Codec.DOUBLE.fieldOf("etherFrac").orElse(0.0).forGetter(t -> t.etherFrac)
    ).apply(i, EtherProcessWorkingChip::new));

    public ItemStack item;
    public long ether;
    public double etherFrac;
    public long storage;
    public long etherConsume;
    public EtherProcessChipManager.ProcessChipEffectConfig effect;
    public double reservePer;

    private EtherProcessWorkingChip() {
        this(ItemStack.EMPTY, 0, 0, 0);
    }

    public EtherProcessWorkingChip(ItemStack item) {
        this(item, 0);
    }

    public EtherProcessWorkingChip(ItemStack item, long beforeEther) {
        this(item, (double) beforeEther);
    }

    public EtherProcessWorkingChip(ItemStack item, double beforeEther) {
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
        setEther(Math.max(0, beforeEther));
        this.reservePer = calcReservePer(this.etherConsume);
    }

    public EtherProcessWorkingChip(ItemStack item, long ether, long storage, long etherConsume) {
        this(item, ether, storage, etherConsume, EtherProcessChipManager.ProcessChipEffectConfig.DEFAULT);
    }

    public EtherProcessWorkingChip(ItemStack item, long ether, long storage, long etherConsume, EtherProcessChipManager.ProcessChipEffectConfig effect) {
        this(item, ether, storage, etherConsume, effect, 0.0);
    }

    public EtherProcessWorkingChip(ItemStack item, long ether, long storage, long etherConsume, EtherProcessChipManager.ProcessChipEffectConfig effect, double etherFrac) {
        this.item = item;
        this.ether = ether;
        this.etherFrac = etherFrac;
        this.storage = storage;
        this.etherConsume = etherConsume;
        this.effect = effect;
        this.reservePer = calcReservePer(etherConsume);
        normalizeFrac();
    }

    public void refreshReservePer() {
        this.reservePer = calcReservePer(this.etherConsume);
    }

    private static double calcReservePer(long etherConsume) {
        double base = Config.factoryReserveMultiplier * etherConsume;
        if (Config.factoryFloatCalc)
            return Math.max((double) Config.factoryMinReservePer, base);
        return Math.max(Config.factoryMinReservePer, Math.round(base));
    }

    /** 总以太（整数部分 + 浮点部分） */
    public double etherTotal() {
        return ether + etherFrac;
    }

    /** 设置总以太，拆分为整数部分与浮点部分并归一化 */
    public void setEther(double total) {
        if (total <= 0) {
            ether = 0;
            etherFrac = 0;
            return;
        }
        ether = (long) total;
        etherFrac = total - ether;
        normalizeFrac();
    }

    /** 增加以太（允许小数），整合累加并归一化 */
    public void addEther(double amount) {
        if (amount <= 0) return;
        setEther(etherTotal() + amount);
    }

    public long addEther(long ether) {
        addEther((double) ether);
        return 0;
    }

    public void subtractEther(double amount) {
        if (amount <= 0) return;
        setEther(etherTotal() - amount);
    }

    private void normalizeFrac() {
        if (etherFrac >= 1.0 - 1e-9) {
            ether += 1;
            etherFrac -= 1;
        } else if (etherFrac < 0) {
            ether -= 1;
            etherFrac += 1;
        }
        if (Math.abs(etherFrac) < 1e-9) etherFrac = 0;
    }

    /**
     * 维持基础曲线 base(e)：
     * e <= consume: base = e * a
     * e >  consume: base = consume*a * (1 + overshoot*y*exp(λ(1-y))), y = (x-1)/(ratio-1), x = e/consume
     */
    public double baseCost() {
        double t = etherConsume;
        double a = Config.factoryBaseRatio;
        double e = etherTotal();
        if (e <= t) return e * a;
        if (t <= 0) return 0;
        double r = Config.factoryPeakRatio;
        double denom = r - 1;
        double y = (e / t - 1) / denom;
        double h = Config.factoryOvershoot * y * Math.exp(Config.factoryDecayLambda * (1 - y));
        return t * a * (1 + h);
    }

    /**
     * 速度倍率 p(e) = max(1, 1 + floor(e/storage))
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
     * 每 tick 维持开销（浮点模式：倍率 p 仍取整，仅最终开销不取整），仅用于浮点计算
     */
    public double maintainCostFloat() {
        double e = etherTotal();
        if (e <= 0) return 0;
        if (e <= etherConsume)
            return e * Config.factoryBaseRatio * speedMul();
        return baseCost() * speedMul();
    }

    /**
     * 每 tick 扣除维持开销（浮点模式走不取整开销）
     */
    public void tickMaintain() {
        if (etherTotal() <= 0) return;
        if (Config.factoryFloatCalc)
            subtractEther(maintainCostFloat());
        else
            ether = Math.max(0, ether - maintainCost());
    }

    /**
     * 当前是否可以工作：以太不足 consume 直接停止（整数判定，保持现状）
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

    public boolean canConsume() {
        return ether >= etherConsume;
    }
}
