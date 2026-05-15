# MainPageDummyPlugin 过滤器功能设计

## 目标

为 EtherAdaptNode 主页面（MainPageDummyPlugin）的 normalStorage 区域（7x3 格子）添加每格独立的过滤器功能，允许用户配置每个格子允许进入的物品类型。

## 架构

```
EtherAdaptNodeEntity (BE)
  ├─ ItemFilter normalStorageFilter (size=27, 每格一个过滤槽)
  ├─ isValid() 检查过滤规则
  └─ saveAdditional/loadAdditional 持久化
        ↓ getter 暴露
MainPageDummyPlugin
  ├─ MainPageContext (custom PluginMenuContext)
  │    ├─ List<FilterSlot> filterSlots
  │    └─ List<BaseSlot> mainSlots
  └─ registerSlots() 注册双套格子（正常 + 过滤，同一位置）
        ↓ makeContext()
MainPageProvider (UI)
  ├─ IASwitchButton（FILTER_PANEL 上半部）
  ├─ 切换 filterSlots ↔ mainSlots active
  └─ 不渲染 FILTER_ICON
```

## 修改文件

### 1. EtherAdaptNodeEntity.java

- 新增字段：`ItemFilter normalStorageFilter = new ItemFilter(27, this::setChanged)`
- 构造函数初始化（置空所有过滤槽）
- `isValid(int index, ItemResource resource)` 中：
  - 对普通格子（index >= 1），取出 `normalStorageFilter.getItem(index - 1)`
  - 若过滤槽为空则放行；若不为空且物品不匹配过滤项则返回 false
- `saveAdditional()` 中序列化 normalStorageFilter
- `loadAdditional()` 中反序列化 normalStorageFilter
- 添加 getter：`public ItemFilter getNormalStorageFilter()`

### 2. MainPageDummyPlugin.java

- 新增内部类 `MainPageContext extends PluginMenuContext<MainPageDummyPlugin>`：
  - `List<FilterSlot> filterSlots`
  - `List<Slot> mainSlots`
  - 构造函数调用 `plugin.registerSlotsWithContext(menu, this)`
- `registerSlots()` 重构为 `registerSlotsWithContext(menu, context)`：
  - 保留原有 ether slot、function slot、feature upgrade slots 注册
  - normalStorage 区域 (76,9, 7x3) 注册 `RangeLimitSlot`（正常）→ 加入 context.mainSlots
  - normalStorage 区域 (76,9, 7x3) 注册 `FilterSlot(nodeEntity.normalStorageFilter, slotIndex, ...)`（初始 inactive）→ 加入 context.filterSlots
- 覆盖 `makeContext()` 返回 `new MainPageContext(menu, this)`
- 覆盖 `inputFilter()`：检查 normalStorageFilter 的每个非空槽是否匹配传入 resource

### 3. MainPageProvider.java

- `createWidget()` 中：
  - 在 `lx(146)` / `ly(131)`（FILTER_PANEL 上半部，16x16 按钮区域）添加 `IASwitchButton`
  - 按钮状态标识当前是否处于过滤模式
  - 点击回调：`context.filterSlots.forEach(s -> s.setActive(toggle))` 和 `context.mainSlots.forEach(s -> s.setActive(!toggle))`
- `extractBackground()` 中：
  - 渲染 `FILTER_PANEL` 背景（`lx(145), ly(130)`)

### 4. 不修改的文件

- `EtherAdaptNodeAsset.java` — FILTER_PANEL 资产已存在
- `EtherAdaptNodeContainerMenu.java` — 现有模式兼容
- `EtherAdaptNodeScreen.java` — 委托给 tabProvider 无需改动
- `FilterSlot.java` — 现有实现可复用

## 过滤行为

- 非过滤模式：normalStorage 所有格子正常接受物品
- 过滤模式配置中：normalStorage 格子切换为 FilterSlot，可放入物品标记过滤规则（存入 normalStorageFilter）
- 运行时过滤：每次 BE 的 `isValid()` / `insert()` 调用都会经过 `inputFilter()` → 检查对应格子的过滤规则
- 空过滤槽 = 放行所有物品；非空过滤槽 = 只接受匹配物品

## 数据流

```
用户点击过滤按钮 → IASwitchButton onClick
  → mainSlots.forEach(setActive(false))
  → filterSlots.forEach(setActive(true))

用户放入过滤物品 → FilterSlot.safeInsert()
  → ItemFilter.setItem() → setChanged() → BE 标记脏数据

物品输入尝试 → BE.insert() → plugin.inputFilter(resource)
  → MainPageDummyPlugin 检查 normalStorageFilter.getItem(slotIndex)
  → 匹配或空 → 放行；不匹配 → 拒绝
```

## 持久化

- 使用 `ValueInput/ValueOutput` 序列化体系（与现有 BE 一致）
- `normalStorageFilter` 作为 ItemFilter 通过 `serialize()`/`deserialize()` 保存/加载
