# Quick Create 网络包优化 — 设计文档

## 1. 背景

### 1.1 当前流程

`VirtualEtherStreamHolder.syncAll()` 中，每 tick 对所有 `markToSyncCreation` 的流发送 `EtherStreamInitialCreateS2C` 包到其 tracking 玩家。该包包含：

| 字段 | 大小（约） |
|------|------------|
| `posDir` | ~12 bytes |
| `streamId` (VAR_INT) | 1-5 bytes |
| `startOffset` (FLOAT) | 4 bytes |
| `startSpeed` (FLOAT) | 4 bytes |
| `ether` (VAR_INT) | 1-5 bytes |
| `EtherConsumer.State` (5 floats + 1 int) | ~24 bytes |
| `syncedData` (list) | 不定，通常 0-100+ bytes |

完整包约 30-150+ bytes。

### 1.2 优化机会

发射器持续发出流时，连续流常共享完全相同的参数（offset、speed、ether、consumer state、synced data）。对这些玩家发送完整包是冗余的——客户端已经知道上一个流的所有信息，只需要知道"再来一个同样的"。

---

## 2. 设计目标

- 对已收到上一个完整创建包的玩家，发送极小包（仅 `posDir`）
- 客户端自行推导 streamId（上次 ID + 1）并克隆缓存参数
- 严格确保安全：任何不确定情况退回到完整包

---

## 3. 新增及修改文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `network/s2c/EtherStreamQuickCreateS2C.java` | **新建** | 最简 S2C 包，仅含 `posDir` |
| `stream/data/CachedEtherStreamEntry.java` | **新建** | IEtherStreamEntryLike 不可变缓存 |
| `network/Network.java` | 修改 | 注册新包 |
| `stream/vholder/VirtualEtherStreamHolder.java` | 修改 | 跟踪 + 拆分发送逻辑 |
| `stream/client/data/ClientVESHData.java` | 修改 | 缓存 + handleQuickCreate |

---

## 4. 新增类型

### 4.1 `EtherStreamQuickCreateS2C`

```java
public record EtherStreamQuickCreateS2C(
    AutoIndexPosDir posDir,
    Optional<Integer> tickCount,   // 可选 firstTick：覆盖继承的初始 tick 计数
    Optional<Integer> ether         // 可选 ether：覆盖继承的初始以太量
) implements CustomPacketPayload, IEtherQuickCreator {
    // 掩码字节布局（8 bit 全利用）：
    //   bit0 (0x01) = firstTick 存在；bit1~6 (0x3E) = firstTick 值（6 bit，0~63，内嵌）
    //   bit7 (0x80) = ether 存在；ether 值在掩码后按 VAR_INT 写（仅 bit7 置位时）
}
```

- 基础包体：`posDir` + 1 byte 掩码；firstTick 存在时值内嵌于掩码（零额外字节），ether 存在时掩码后跟 VAR_INT
- 客户端自行推算 streamId = `lastCreateStreamId + 1`
- 两可选字段为空（与上次快照一致）时包体仅比旧版多 1 byte 全零掩码

### 4.2 `CachedEtherStreamEntry`

```java
public record CachedEtherStreamEntry(
    int streamId,
    float startOffset,
    float startSpeed,
    int ether,
    int tickCount,
    EtherConsumer.State consumerState,
    List<IEtherStreamSyncedData> syncedData
) implements IEtherStreamEntryLike {

    /** 从网络包或其他 IEtherStreamEntryLike 构造不可变缓存 */
    public static CachedEtherStreamEntry from(IEtherStreamEntryLike source) {
        return new CachedEtherStreamEntry(
            source.streamId(),
            source.startOffset(),
            source.startSpeed(),
            source.ether(),
            source.tickCount(),
            source.consumerState(),
            List.copyOf(source.syncedData())
        );
    }

    /** 派生一个只改 ID 的副本 */
    public CachedEtherStreamEntry withId(int newId) {
        return new CachedEtherStreamEntry(
            newId, startOffset, startSpeed, ether, tickCount, consumerState, syncedData
        );
    }
}
```

- 实现 `IEtherStreamEntryLike`，可直接传入 `new ClientStreamEntry(posDir, cachedEntry)`
- record 自带不可变性，所有字段均不可变或只读
- `consumerState` 是 record，不可变
- `syncedData` 中 `IEtherStreamSyncedData` 对象由网络包反序列化后在客户端是独立堆对象，客户端从不修改（只做 `Map::put`/`Map::clear`），安全直接缓存
- `List.copyOf()` 仅防外部修改列表结构，内部对象共享引用即可

---

## 5. 服务端逻辑

### 5.1 新增字段 (`VirtualEtherStreamHolder`)

```java
Int2IntOpenHashMap playerLastCreateId = new Int2IntOpenHashMap();  // playerId → streamId
```

`Int2IntOpenHashMap` (fastutil) 是专为 int→int 设计的 hash map，每条目约 16 bytes。

### 5.2 `syncAll()` 创建段新增逻辑

```
输入: stream N，tracking players = T[]

step 1 — 判断是否可用 quick create:
  prev = findStreamById(N - 1)
  if prev == null or prev.markToRemove:
      → quick_eligible = false
  else:
      → quick_eligible = params_match(N, prev)

step 2 — 分发:
  if quick_eligible:
      对每个 P in T:
          if playerLastCreateId.get(P) == N-1:
              加入 QuickCreate 队列
          else:
              加入 InitialCreate 队列（该玩家上次没收到完整包）
      更新 playerLastCreateId: for each P in T → put(P, N)
  else:
      全部加入 InitialCreate 队列
      更新 playerLastCreateId: for each P in T → put(P, N)

step 3 — 发送:
  有 QuickCreate 队列 → 对相关玩家发送 EtherStreamQuickCreateS2C
  有 InitialCreate 队列 → 按现有逻辑发送 EtherStreamInitialCreateS2C
```

### 5.3 三个退出门（确保安全）

| 条件 | 行为 |
|------|------|
| `findStreamById(N-1) == null` | 全量 InitialCreate |
| `prev.markToRemove == true` | 全量 InitialCreate |
| `params_match(N, prev) == false` | 全量 InitialCreate |

三个条件任一不满足，全部玩家发送完整包，零退化。

### 5.4 参数比较 (`params_match`)

```java
boolean params_match(VirtualEtherStream a, VirtualEtherStream b) {
    return Float.compare(a.startOffset, b.startOffset) == 0
        && Float.compare(a.startSpeed, b.startSpeed) == 0
        && a.consumer.toState().equals(b.consumer.toState())
        && a.toSyncData.isEmpty() && b.toSyncData.isEmpty();
}
```

`EtherConsumer.State` 是 record，自带 `equals()`。syncedData 仅当两端均为空时才视为匹配——非空时走全量包，零序列化开销。

> **2026-08 变更**：不再比较 `a.getEther() == b.getEther()` 与初始 `tickCount`（firstTick）。tickCount/ether 与上次不同时，改为在 QuickCreate 包中携带覆盖值（见 §4.1 掩码，firstTick 6-bit 内嵌、ether VAR_INT），其余条件不满足仍走全量。
> 值域保护：需携带 firstTick 且 `tickCount >= 64`（6-bit 上限）时回退全量 InitialCreate。
（`startOffset`/`startSpeed` 字段已是 package-private，同 package 内 `VirtualEtherStreamHolder` 可直接访问。）

### 5.5 `updateNoLongerTracking()` 中清理

```java
// 现有逻辑
for (VirtualEtherStream ves : streams)
    if (ves.markToRemove)
        for (int pid : ves.trackingPlayers)
            trackingPlayers.addTo(pid, -1);

// 新增：清理退出的玩家
trackingPlayers.int2IntEntrySet().removeIf(e -> {
    if (e.getIntValue() <= 0) {
        playerLastCreateId.remove(e.getIntKey());  // ← 新增
        return true;
    }
    return false;
});
```

玩家退出 tracking 范围后清除其 `playerLastCreateId` 条目。下次重新进入时走 batch create 流程（不依赖此优化），不会错误收到 QuickCreate。

### 5.6 `syncAndStartTrackingByPlayer()` — 不改动

新玩家加入走 batch create，不走 syncAll 创建流程，不受影响。

---

## 6. 客户端逻辑

### 6.1 缓存字段 (`ClientVESHData`)

```java
CachedEtherStreamEntry lastCreateEntry = null;
```

`ClientVESHData` 是 per-PosDir 单例的（`entries` map keyed by `PosDir`），每个 posDir 独立缓存。

### 6.2 `handleCreate(EtherStreamInitialCreateS2C)` — 补充缓存

现有逻辑末尾新增：

```java
this.lastCreateEntry = CachedEtherStreamEntry.from(msg);
```

### 6.3 `handleQuickCreate(PosDir, IEtherQuickCreator)` — 新增

```java
// ClientVESHData.handleQuickCreate
public void handleQuickCreate(PosDir posDir, IEtherQuickCreator msg) {
    if (level.get() == null) return;
    ClientVESHEntry entry = entries.get(posDir);  // 不用 createOrGet，不存在则跳过
    if (entry == null || !entry.hasLast()) return;
    if (entry.streams.containsKey(entry.getLastCreateStreamId() + 1)) return;
    // 派生新条目：id+1，按需覆盖 tickCount(firstTick)/ether
    IEtherStreamEntryLike quickEntry = entry.getFromLastAndUpdate(msg.tickCount(), msg.ether());
    entry.addStream(quickEntry.streamId(), posDir, quickEntry);
    // ... EtherStreamSyncMarker.record(QUICK_CREATE, ...)
}

// ClientVESHEntry.getFromLastAndUpdate
public IEtherStreamEntryLike getFromLastAndUpdate(Optional<Integer> tickCount, Optional<Integer> ether) {
    lastCreateEntry = lastCreateEntry.withNext(
        lastCreateEntry.streamId() + 1, tickCount.orElse(null), ether.orElse(null));
    return lastCreateEntry;
}
```

- `entries.get()` 替代 `createOrGet()`：QuickCreate 依赖前一个 InitialCreate 已创建 CVESHEntry，不存在说明客户端/服务端状态不一致，静默跳过
- `withNext(newId, tickCount, ether)` 派生新 entry：仅改 streamId，null 覆盖值沿用缓存（2026-08 起支持按需覆盖）
- `lastCreateEntry` 指针前进

### 6.4 `handleCreate(EtherStreamBatchCreateS2C)` — 不改动

Batch create 不更新 `lastCreateEntry` 缓存，只由逐 tick 的 syncAll 创建流程负责。

---

## 7. 开销分析

### 7.1 `playerLastCreateId`

| 操作 | 触发时机 | 频率 | 复杂度 |
|------|----------|------|--------|
| `get(P)` | 创建流时分发 | 每流 × 每玩家 | O(1) hash 读 |
| `put(P, N)` | 创建流后更新 | 每流 × 每玩家 | O(1) hash 写 |
| `remove(P)` | 玩家退出 tracking | 极少（通常 0） | O(1) hash 删 |

每个新建流的净开销 = `T × (1 get + 1 put)`，T 通常 1-10 玩家。Minecraft 50ms tick 预算中完全可忽略。内存：~16 bytes × 玩家数。

### 7.2 `params_match`

- 浮点比较：`Float.compare` × 2，常数时间
- `EtherConsumer.State.equals()`：5 字段 record 比较
- syncedData：两次 `isEmpty()` 调用，零开销（非空直接退回到全量）

### 7.3 `CachedEtherStreamEntry`

- `.from()` 时 `List.copyOf()` 一次拷贝列表结构，内部对象共享引用（客户端不修改）
- `withNext()` 零拷贝，改 streamId 并按需覆盖 tickCount/ether（null 沿用当前值）

---

## 8. 边界情况

| 情况 | 处理 |
|------|------|
| 第一个流 | lastCreateId=-1，条件不满足 → 全量 |
| 连续流中途参数变化 | `params_match` 失败 → 全量 |
| 连续流 firstTick 不同（offset/speed/consumer 等仍一致） | QuickCreate 携带 firstTick 覆盖值（内嵌掩码，6-bit） |
| 连续流 ether 不同（其余一致） | QuickCreate 携带 ether 覆盖值（掩码后 VAR_INT） |
| firstTick ≥ 64（simulateInterval∈[33,40] 极端） | 无法内嵌编码 → 回退全量 |
| 流有 syncedData（label/entity/item） | syncedData 非空 → 全量 |
| 上一个流已死亡 | `prev.markToRemove` → 全量，`playerLastCreateId` 覆盖 |
| 新玩家中途进入范围 | `playerLastCreateId` 无记录 → InitialCreate 分支 |
| 玩家离开再回来 | `playerLastCreateId` 已清理 → 走 batch create 或 InitialCreate |
| 服务端重启 | 无影响，`playerLastCreateId` 重建 |
| `ClientStreamEntry` 构造消耗 syncedData | 每次构造前深拷贝，不消耗缓存 |

---

## 9. 实现步骤

1. 创建 `CachedEtherStreamEntry`
2. 创建 `EtherStreamQuickCreateS2C`
3. 在 `Network.java` 注册新包
4. 修改 `VirtualEtherStreamHolder` — 加字段 + `syncAll` 拆分逻辑 + `updateNoLongerTracking` 清理
5. 修改 `ClientVESHData` — 加缓存字段 + `handleQuickCreate` + 修改 `handleCreate(InitialCreate)` 写缓存
6. `idea_build_project` 验证编译
