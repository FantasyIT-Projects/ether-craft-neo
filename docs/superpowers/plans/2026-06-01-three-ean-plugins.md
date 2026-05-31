# 三个 EAN 插件升级 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 EtherAdaptNode 添加三个新插件升级：红石信号（比较器）、红石开关（红石粉）、销毁（熔岩桶）

**Architecture:** 两个新方法加入 AbstractNodePlugin 基类（preTick/handleOverflow），分别由 EtherAdaptNodeEntity.tickServer() 和 insert() 委托调用。三个插件各司其职，红石信号继承 AbstractDirectionalFeature 占用一个面并重写 Block 的红石输出方法。

**Tech Stack:** NeoForge 1.26.1.2, Java 25, DeferredRegister, ValueIO 序列化

---

### 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 修改 | `node/plugins/base/AbstractNodePlugin.java` | 新增 preTick/handleOverflow 默认方法 |
| 修改 | `block/node/EtherPluginUpgradeContainer.java` | 新增 preTick/handleOverflow 遍历委托 |
| 修改 | `block/node/EtherAdaptNodeEntity.java` | tickServer preTick 门控、insert handleOverflow、getAnalogOutputSignal |
| 修改 | `block/node/EtherAdaptNodeBlock.java` | hasAnalogOutputSignal/getAnalogOutputSignal |
| 修改 | `node/NodePluginManager.java` | 注册 3 个新 PluginInfo |
| 新建 | `node/plugins/feature/FeatureRedstoneSignal.java` | 红石信号比较器 feature |
| 新建 | `node/plugins/upgrade/RedstoneSwitchUpgrade.java` | 红石开关 upgrade |
| 新建 | `node/plugins/upgrade/DestructionUpgrade.java` | 销毁 upgrade |

---

### Task 1: 为 AbstractNodePlugin 添加 preTick() 和 handleOverflow()

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/plugins/base/AbstractNodePlugin.java`

- [ ] **Step 1: 在 AbstractNodePlugin 末尾前添加两个默认方法**

在 `makeContext` 方法之后、闭合 `}` 之前插入：

```java
public boolean preTick() {
    return true;
}

public int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
    return 0;
}
```

注意 `ItemResource` 和 `TransactionContext` 已在文件开头 import。

---

### Task 2: 为 EtherPluginUpgradeContainer 添加 preTick() 和 handleOverflow() 委托

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/block/node/EtherPluginUpgradeContainer.java`

- [ ] **Step 1: 在 import 区域添加 ItemResource 和 TransactionContext**

```java
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
```

- [ ] **Step 2: 在 modifyNodeProperty 方法之后（L132 之后）添加两个委托方法**

```java
public boolean preTick() {
    for (AbstractNodePlugin plugin : this.plugin) {
        if (plugin != null && !plugin.preTick())
            return false;
    }
    return true;
}

public int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
    int consumed = 0;
    for (AbstractNodePlugin plugin : this.plugin) {
        if (plugin != null)
            consumed += plugin.handleOverflow(resource, amount - consumed, transaction);
    }
    return consumed;
}
```

---

### Task 3: 修改 EtherAdaptNodeEntity — preTick 门控、handleOverflow、getAnalogOutputSignal

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/block/node/EtherAdaptNodeEntity.java`

- [ ] **Step 1: 在 tickServer() 顶部添加 preTick 门控**

找到 `tickServer()` 方法（L143），在现有代码之前插入门控：

```java
@Override
public void tickServer() {
    if (!functionStorage.preTick() || !featureUpgradeStorage.preTick())
        return;
    functionStorage.tickInput();
    // ... 剩余不变 ...
```

- [ ] **Step 2: 修改 insert() 方法，在末尾添加 handleOverflow 调用**

替换 L248-L264 的 `insert` 方法：

```java
@Override
public int insert(int index, ItemResource resource, int amount, @NotNull TransactionContext transaction) {
    if (index == 0)
        return etherStorage.insert(index, resource, amount, transaction);
    if (resource.is(ItemRegistry.ETHER))
        return 0;
    if (index - 1 >= nodeProperty.slotUnlock)
        return 0;
    if (!isValid(index, resource))
        return 0;
    int earlyCosted = 0;
    for (AbstractNodePlugin plugin : getPlugins()) {
        earlyCosted += plugin.earlyHandleInput(resource, amount - earlyCosted, transaction);
        if (earlyCosted >= amount)
            return earlyCosted;
    }
    int handlerInserted = normalHandler.insert(index - 1, resource, amount - earlyCosted, transaction);
    int overflow = amount - earlyCosted - handlerInserted;
    int overflowConsumed = 0;
    for (AbstractNodePlugin plugin : getPlugins()) {
        overflowConsumed += plugin.handleOverflow(resource, overflow - overflowConsumed, transaction);
        if (overflowConsumed >= overflow)
            break;
    }
    return handlerInserted + earlyCosted + overflowConsumed;
}
```

- [ ] **Step 3: 添加 getAnalogOutputSignal() 方法**

在 `pluginUpdate()` 方法之后（L517 之后）添加：

```java
public int getAnalogOutputSignal() {
    for (AbstractNodePlugin plugin : getPlugins()) {
        if (plugin instanceof FeatureRedstoneSignal rss && rss.enabled) {
            return rss.getSignal();
        }
    }
    return 0;
}
```

这需要 import `FeatureRedstoneSignal`。在 import 区域添加：

```java
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureRedstoneSignal;
```

---

### Task 4: 修改 EtherAdaptNodeBlock — 红石比较器输出

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/block/node/EtherAdaptNodeBlock.java`

- [ ] **Step 1: 在 createBlockStateDefinition 之后添加两个红石方法**

在 L61（`createBlockStateDefinition` 结束）之后添加：

```java
@Override
protected boolean hasAnalogOutputSignal(@NotNull BlockState state) {
    return true;
}

@Override
protected int getAnalogOutputSignal(@NotNull BlockState state, Level level, @NotNull BlockPos pos) {
    if (level.getBlockEntity(pos) instanceof EtherAdaptNodeEntity eane)
        return eane.getAnalogOutputSignal();
    return 0;
}
```

注意 `Level` 已 import（L15），`@NotNull` 注意匹配文件现有风格。

---

### Task 5: 创建 FeatureRedstoneSignal

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/node/plugins/feature/FeatureRedstoneSignal.java`

- [ ] **Step 1: 创建完整文件**

```java
package studio.fantasyit.ether_craft.node.plugins.feature;

import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;

public class FeatureRedstoneSignal extends AbstractDirectionalFeature {
    public static final Identifier ID = EtherCraft.id("redstone_signal");
    public static final Identifier SYNC_MODE = EtherCraft.id("redstone_signal/mode");
    public static final Identifier SYNC_ENABLED = EtherCraft.id("redstone_signal/enabled");

    public enum SignalMode {
        ETHER, INVENTORY;
        public static final Codec<SignalMode> CODEC = Codec.STRING.xmap(
                s -> s.equals("INVENTORY") ? INVENTORY : ETHER,
                m -> m == INVENTORY ? "INVENTORY" : "ETHER"
        );
    }

    public SignalMode mode = SignalMode.ETHER;
    public boolean enabled = true;

    public FeatureRedstoneSignal(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    public int getSignal() {
        if (!enabled) return 0;
        if (mode == SignalMode.ETHER) {
            long ether = nodeEntity.getEther();
            long maxEther = nodeEntity.getMaxEther();
            if (maxEther <= 0) return 0;
            return (int) (ether * 15 / maxEther);
        } else {
            int unlocked = nodeEntity.nodeProperty.slotUnlock;
            if (unlocked <= 0) return 0;
            int filled = 0;
            for (int i = 0; i < unlocked; i++) {
                if (!nodeEntity.normalStorage.getItem(i).isEmpty())
                    filled++;
            }
            return filled * 15 / unlocked;
        }
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("rssMode", SignalMode.CODEC, mode);
        output.putBoolean("rssEnabled", enabled);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = input.read("rssMode", SignalMode.CODEC).orElse(SignalMode.ETHER);
        enabled = input.getBooleanOr("rssEnabled", true);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        super.syncScreenData(message);
        if (message.id().equals(SYNC_MODE)) {
            mode = message.data() == 1 ? SignalMode.INVENTORY : SignalMode.ETHER;
            nodeEntity.pluginUpdate();
        }
        if (message.id().equals(SYNC_ENABLED)) {
            enabled = message.data() == 1;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        super.registerSlots(menu);
        menu.addDataSlot(new BaseDataSlot(() -> mode == SignalMode.INVENTORY ? 1 : 0, t -> mode = (t == 1 ? SignalMode.INVENTORY : SignalMode.ETHER)));
        menu.addDataSlot(new BaseDataSlot(() -> enabled ? 1 : 0, t -> enabled = t == 1));
    }
}
```

---

### Task 6: 创建 RedstoneSwitchUpgrade

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/RedstoneSwitchUpgrade.java`

- [ ] **Step 1: 创建完整文件**

```java
package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class RedstoneSwitchUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("redstone_switch");
    public static final Identifier SYNC_MODE = EtherCraft.id("redstone_switch/mode");

    public boolean workWithSignal = true;

    public RedstoneSwitchUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public boolean preTick() {
        if (nodeEntity.getLevel() == null) return true;
        boolean hasSignal = nodeEntity.getLevel().hasNeighborSignal(nodeEntity.getBlockPos());
        return workWithSignal ? hasSignal : !hasSignal;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putBoolean("rswWorkWithSignal", workWithSignal);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        workWithSignal = input.getBooleanOr("rswWorkWithSignal", true);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        if (message.id().equals(SYNC_MODE)) {
            workWithSignal = message.data() == 1;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        menu.addDataSlot(new BaseDataSlot(() -> workWithSignal ? 1 : 0, t -> workWithSignal = t == 1));
    }
}
```

---

### Task 7: 创建 DestructionUpgrade

**Files:**
- Create: `src/main/java/studio/fantasyit/ether_craft/node/plugins/upgrade/DestructionUpgrade.java`

- [ ] **Step 1: 创建完整文件**

```java
package studio.fantasyit.ether_craft.node.plugins.upgrade;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import studio.fantasyit.ether_craft.EtherCraft;
import studio.fantasyit.ether_craft.block.base.ItemFilter;
import studio.fantasyit.ether_craft.block.node.EtherAdaptNodeEntity;
import studio.fantasyit.ether_craft.menu.base.slot.BaseDataSlot;
import studio.fantasyit.ether_craft.menu.node.EtherAdaptNodeContainerMenu;
import studio.fantasyit.ether_craft.network.c2s.SyncScreenDataC2S;
import studio.fantasyit.ether_craft.node.filter.FilterGuiRegCommon;
import studio.fantasyit.ether_craft.node.plugins.InstalledPlugin;
import studio.fantasyit.ether_craft.node.plugins.base.AbstractNodePlugin;

public class DestructionUpgrade extends AbstractNodePlugin {
    public static final Identifier ID = EtherCraft.id("destruction");
    public static final Identifier SYNC_MODE = EtherCraft.id("destruction/mode");

    public enum DestroyMode {
        OVERFLOW, ALL
    }

    public ItemFilter filter = new ItemFilter(21, nodeEntity::setChanged);
    public DestroyMode destroyMode = DestroyMode.OVERFLOW;

    public DestructionUpgrade(EtherAdaptNodeEntity nodeEntity, InstalledPlugin installedId) {
        super(nodeEntity, installedId);
    }

    @Override
    public int earlyHandleInput(ItemResource resource, int amount, TransactionContext context) {
        if (destroyMode != DestroyMode.ALL) return 0;
        if (!filter.accepts(resource)) return 0;
        return amount;
    }

    @Override
    public int handleOverflow(ItemResource resource, int amount, TransactionContext transaction) {
        if (destroyMode != DestroyMode.OVERFLOW) return 0;
        if (!filter.accepts(resource)) return 0;
        return amount;
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        output.putBoolean("desMode", destroyMode == DestroyMode.ALL);
        filter.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        destroyMode = input.getBooleanOr("desMode", false) ? DestroyMode.ALL : DestroyMode.OVERFLOW;
        filter.deserialize(input);
    }

    @Override
    public void syncScreenData(SyncScreenDataC2S message) {
        FilterGuiRegCommon.sync(message, filter);
        if (message.id().equals(SYNC_MODE)) {
            destroyMode = message.data() == 1 ? DestroyMode.ALL : DestroyMode.OVERFLOW;
            nodeEntity.pluginUpdate();
        }
    }

    @Override
    public void registerSlots(EtherAdaptNodeContainerMenu menu) {
        FilterGuiRegCommon.slots(menu, filter);
        menu.addDataSlot(new BaseDataSlot(() -> destroyMode == DestroyMode.ALL ? 1 : 0, t -> destroyMode = (t == 1 ? DestroyMode.ALL : DestroyMode.OVERFLOW)));
    }
}
```

---

### Task 8: 在 NodePluginManager 注册三个插件

**Files:**
- Modify: `src/main/java/studio/fantasyit/ether_craft/node/NodePluginManager.java`

- [ ] **Step 1: 在 import 区域添加新插件类的 import**

```java
import studio.fantasyit.ether_craft.node.plugins.feature.FeatureRedstoneSignal;
import studio.fantasyit.ether_craft.node.plugins.upgrade.RedstoneSwitchUpgrade;
import studio.fantasyit.ether_craft.node.plugins.upgrade.DestructionUpgrade;
```

- [ ] **Step 2: 在 ALL_PLUGINS static 块末尾（L96 之后，`}` 之前）添加三个注册**

```java
ALL_PLUGINS.add(new PluginInfo(PluginType.FEATURE, FeatureRedstoneSignal.ID, FeatureRedstoneSignal::new, t -> t.is(Items.COMPARATOR), Items.COMPARATOR));
ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, RedstoneSwitchUpgrade.ID, RedstoneSwitchUpgrade::new, t -> t.is(Items.REDSTONE), Items.REDSTONE));
ALL_PLUGINS.add(new PluginInfo(PluginType.UPGRADE, DestructionUpgrade.ID, DestructionUpgrade::new, t -> t.is(Items.LAVA_BUCKET), Items.LAVA_BUCKET));
```

---

### Task 9: 构建验证

**Files:** 无新建

- [ ] **Step 1: 运行 Gradle 构建**

使用 IDE build 工具编译项目并确认无错误：

```
idea_build_project(rebuild=true)
```

预期：BUILD SUCCESSFUL，无编译错误。

---

### 自审清单

1. **Spec coverage:** 三个插件各自的核心逻辑已覆盖 — 红石信号的两种模式和开关、红石开关的 preTick 门控和 workWithSignal 切换、销毁升级的 ALL（earlyHandleInput）和 OVERFLOW（handleOverflow）两种模式及 ItemFilter。AbstractNodePlugin 两个新方法的默认实现、Container 的委托、Entity 的门控和 insert 修改、Block 的红石输出均已覆盖。

2. **Placeholder scan:** 无 TBD/TODO/占位符。每步都有完整代码。

3. **Type consistency:** preTick 签名一致（`boolean preTick()`），handleOverflow 签名一致（`int handleOverflow(ItemResource, int, TransactionContext)`）。getAnalogOutputSignal 在 Entity 和 Block 中的调用链一致。

4. **Import 检查:** 所有新文件和修改文件的 import 已对照现有代码检查。`ValueOutput.storeNullable` 用于 Direction（继承自 AbstractDirectionalFeature 已有），`ValueOutput.store` 用于 Codec 序列化。`ValueInput.read/getBooleanOr` 模式与现有代码一致。
