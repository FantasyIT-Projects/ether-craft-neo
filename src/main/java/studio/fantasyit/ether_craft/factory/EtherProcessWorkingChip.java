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
        double x = ether / t;
        double r = Config.factoryPeakRatio;
        double y = (x - 1) / (r - 1);
        double h = Config.factoryOvershoot * y * Math.exp(Config.factoryDecayLambda * (1 - y));
        return t * a * (1 + h);
    }

    /**
     * 速度倍率 p(e) = floor(1 + e/storage)（始终取整）
     */
    public double speedMul() {
        if (storage <= 0) return 1;
        return Math.max(1, Math.floor(1 + (double) ether / storage));
    }

    /**
     * 每 tick 维持开销 C(e) = base(e) * p(e)
     */
    public long maintainCost() {
        return Math.round(baseCost() * speedMul());
    }

    /**
     * 每 tick 扣除维持开销
     */
    public void tickMaintain() {
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
