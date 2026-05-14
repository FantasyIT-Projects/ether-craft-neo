# Language Key Reference for AI Auto-Generation

> 本文档列出项目中所有需要动态生成（由字段拼接而来）或非字面量引用的语言键，供 AI 自动生成 `en_us.json` 条目使用。

---

## 一、Minecraft / NeoForge 框架自动生成的键

这些键由 NeoForge 根据注册名自动推导，**代码中不可见**，但必须在语言文件中定义，否则游戏中会显示原始 registry ID。

### 1.1 方块 (Blocks)

命名规则: `block.<modid>.<registry_id>`

| 键 | 注册位置 | 注册 ID |
|---|---|---|
| `block.ether_craft.ether_process_factory` | `BlockRegistry.java:16` | `ether_process_factory` |
| `block.ether_craft.ether_stream_emitter` | `BlockRegistry.java:17` | `ether_stream_emitter` |
| `block.ether_craft.ether_adapt_node` | `BlockRegistry.java:18` | `ether_adapt_node` |

### 1.2 物品 (Items)

命名规则: `item.<modid>.<registry_id>`

| 键 | 注册位置 | 注册 ID | 备注 |
|---|---|---|---|
| `item.ether_craft.ether` | `ItemRegistry.java:25` | `ether` | |
| `item.ether_craft.ether_creative` | `ItemRegistry.java:26` | `ether_creative` | |
| `item.ether_craft.process_chip` | `ItemRegistry.java:27` | `process_chip` | |
| `item.ether_craft.direct_input` | `ItemRegistry.java:28` | `direct_input` | |
| `item.ether_craft.wrench` | `ItemRegistry.java:30` | `wrench` | |
| `item.ether_craft.ether_stream_emitter` | `ItemRegistry.java:31` | `ether_stream_emitter` | 通过 `block()` 方法，ID 来自方块 |
| `item.ether_craft.ether_process_factory_lv_1` | `ItemRegistry.java:32` | `ether_process_factory_lv_1` | |
| `item.ether_craft.ether_process_factory_lv_2` | `ItemRegistry.java:33` | `ether_process_factory_lv_2` | |
| `item.ether_craft.ether_process_factory_lv_3` | `ItemRegistry.java:34` | `ether_process_factory_lv_3` | |
| `item.ether_craft.ether_process_factory_lv_4` | `ItemRegistry.java:35` | `ether_process_factory_lv_4` | |
| `item.ether_craft.ether_adapt_node_lv_1` | `ItemRegistry.java:36` | `ether_adapt_node_lv_1` | |
| `item.ether_craft.ether_adapt_node_lv_2` | `ItemRegistry.java:37` | `ether_adapt_node_lv_2` | |
| `item.ether_craft.ether_adapt_node_lv_3` | `ItemRegistry.java:38` | `ether_adapt_node_lv_3` | |

---

## 二、运行时拼接生成的动态键 (Dynamic Concatenation)

### 2.1 处理芯片 Tooltip 键 — 来自数据组件 ID

**代码位置:** `ProcessChipItem.java:46-47`

```java
String baseKey = "tooltip." + id.getNamespace() + "." + id.getPath();
builder.accept(Component.translatable(baseKey));
```

**拼接规则:** `"tooltip." + <namespace> + "." + <chip_id>`

**动态来源:** `itemStack.get(DataComponentRegistry.CHIP_ID)` — 来自物品数据组件，芯片 ID 由数据包定义。

**已知芯片 ID (来自 `data/ether_craft/ether_process_chip/` 目录):**

| 芯片 ID 文件名 | 生成的键 |
|---|---|
| `separator_chip.json` | `tooltip.ether_craft.separator_chip` |
| `heating_chip.json` | `tooltip.ether_craft.heating_chip` |
| `stamping_chip.json` | `tooltip.ether_craft.stamping_chip` |
| `fan_chip.json` | `tooltip.ether_craft.fan_chip` |
| `cutting_chip.json` | `tooltip.ether_craft.cutting_chip` |

> **注意:** 芯片数据包可在运行时通过数据包系统扩展，因此可能会有额外的未知键。AI 生成时应扫描 `data/**/ether_process_chip/*.json` 获取所有已知 ID。

### 2.2 方向筛选器 Tooltip 键 — 来自 Direction 枚举

**代码位置:** `DirectionalFilterScreen.java:59`

```java
Component.translatable("menu.ether_craft.node.directional_filter.direction." + direction.getName().toLowerCase())
```

**拼接规则:** `"menu.ether_craft.node.directional_filter.direction." + <direction_lowercase>`

**动态来源:** `net.minecraft.core.Direction` 枚举的 `.getName().toLowerCase()`，共 6 个值。

**生成的键:**

| Direction | 生成的键 |
|---|---|
| UP | `menu.ether_craft.node.directional_filter.direction.up` |
| DOWN | `menu.ether_craft.node.directional_filter.direction.down` |
| NORTH | `menu.ether_craft.node.directional_filter.direction.north` |
| SOUTH | `menu.ether_craft.node.directional_filter.direction.south` |
| EAST | `menu.ether_craft.node.directional_filter.direction.east` |
| WEST | `menu.ether_craft.node.directional_filter.direction.west` |

### 2.3 磁铁功能标签键 — 来自数组遍历

**代码位置:** `MagnetFunctionScreen.java:71-74`

```java
String[] labels = {"cx", "cy", "cz", "sx", "sy", "sz"};
// ...
Component.translatable("menu.ether_craft.node.magnet." + labels[i])
```

**拼接规则:** `"menu.ether_craft.node.magnet." + <label>`

**动态来源:** 硬编码字符串数组 `{"cx", "cy", "cz", "sx", "sy", "sz"}`。

**生成的键:**

| labels[i] | 生成的键 |
|---|---|
| cx | `menu.ether_craft.node.magnet.cx` |
| cy | `menu.ether_craft.node.magnet.cy` |
| cz | `menu.ether_craft.node.magnet.cz` |
| sx | `menu.ether_craft.node.magnet.sx` |
| sy | `menu.ether_craft.node.magnet.sy` |
| sz | `menu.ether_craft.node.magnet.sz` |

---

## 三、通过变量间接引用的静态键 (Indirect Static Keys)

### 3.1 创造模式标签页标题

**代码位置:** `CreativeTabRegistry.java:15,20`

```java
public static final String TAB_NAME = "ether_craft_tab_main";
// ...
.title(Component.translatable(TAB_NAME))
```

**键:** `ether_craft_tab_main`

> 键本身是静态的，但通过常量变量 `TAB_NAME` 间接引用，可能被遗漏。

---

## 四、硬编码英文文本 — 应转为可翻译键 (Hardcoded Literals)

这些使用 `Component.literal()` 拼接运行时的值，无法本地化。如需支持多语言，应改为 `Component.translatable()`。

### 4.1 工厂屏幕调试信息

**代码位置:** `EtherProcessFactoryScreen.java:67-69`

```java
Component.literal("Ether:" + be.getEther())   // 第 67 行 — 建议键: menu.ether_craft.factory.debug.ether
Component.literal("Spd:" + be.pressureBonus)   // 第 68 行 — 建议键: menu.ether_craft.factory.debug.speed
Component.literal("Leak:" + be.leak)           // 第 69 行 — 建议键: menu.ether_craft.factory.debug.leak
```

**拼接规则:** `"<prefix>:" + <int/long value>`

**建议替换键:**
- `menu.ether_craft.factory.debug.ether` → "Ether: %d"
- `menu.ether_craft.factory.debug.speed` → "Spd: %d"
- `menu.ether_craft.factory.debug.leak` → "Leak: %d"

---

## 五、当前已定义的静态键 (对照用)

以下是 `en_us.json` 中**已存在**的所有键，供参考哪些已被覆盖。

| 键 | 当前值 |
|---|---|
| `itemGroup.ether_craft` | Example Mod Tab |
| `block.ether_craft.example_block` | Example Block |
| `item.ether_craft.example_item` | Example Item |
| `tooltip.ether_craft.ether_process_chip.max_ether` | Max [%d] |
| `tooltip.ether_craft.ether_process_chip.ether_decay` | Decay [%d] |
| `tooltip.ether_craft.ether_process_chip.ether_require` | Require [%d] |
| `tooltip.ether_craft.ether_process_chip.ether_consume` | Consume [%d] |
| `tooltip.ether_craft.process_chip_ether` | Contains [%d] E |
| `menu.ether_craft.node.directional_filter.cancel` | Cancel |
| `ether_craft.gui.node.filter.using_black_list` | Using Blacklist |
| `ether_craft.gui.node.filter.using_white_list` | Using Whitelist |
| `menu.ether_craft.factory.filter` | Setting Filter |
| `menu.ether_craft.ether_bar_tooltip` | Ether: %d |
| `menu.ether_craft.node.magnet.cx` | X |
| `menu.ether_craft.node.magnet.cy` | Y |
| `menu.ether_craft.node.magnet.cz` | Z |
| `menu.ether_craft.node.magnet.sx` | rX |
| `menu.ether_craft.node.magnet.sy` | rY |
| `menu.ether_craft.node.magnet.sz` | rZ |
| `menu.ether_craft.node.magnet.value` | %d |
| `ether_craft.gui.node.container_interact.extract` | Extract |
| `ether_craft.gui.node.container_interact.insert` | Insert |

---

## 六、AI 自动生成语言键通用指南

### 6.1 语言键发现清单

AI 扫描代码时应按以下优先级搜索所有需要语言键的位置：

1. **搜索 `Component.translatable()` 调用** — grep `Component\.translatable\(`，提取键参数。若参数为非字面量（含 `+`、变量、方法调用），则为**动态键**，需展开枚举/数组/数据源获取所有可能值。

2. **搜索 `Component.literal()` 调用** — grep `Component\.literal\(`，标记为**硬编码文本**。若含 `+` 拼接运行时值，建议转为 `Component.translatable(key, args...)`。

3. **扫描注册表类** (`*Registry.java`) — 对每个 `DeferredRegister.register("id", ...)` 调用，推导框架自动生成的键：
   - 方块: `block.<modid>.<registry_id>`
   - 物品: `item.<modid>.<registry_id>`
   - 创造模式标签页: `itemGroup.<modid>` (如果未显式设置 title)
   - 实体类型: `entity.<modid>.<registry_id>`
   - 方块实体: `block.<modid>.<registry_id>` (BlockEntity 通常跟随方块名)

4. **扫描数据驱动内容** — 检查 `data/<modid>/` 目录下的 JSON 定义，确认是否有基于数据 ID 构造的 tooltip/名称键（如 `ProcessChipItem.java:46` 的模式）。

### 6.2 动态键拼接模式

本项目出现的动态键拼接模式（同类项目通用）：

| 模式 | 检测方式 | 示例 |
|---|---|---|
| `key + enum.getName()` | 循环遍历枚举的 `Component.translatable()` 调用 | `"prefix." + direction.getName().toLowerCase()` |
| `key + array[i]` | 循环内 `Component.translatable()` 含数组索引 | `"prefix." + labels[i]` |
| `key + Identifier` | `Component.translatable()` 含 `id.getNamespace()` / `id.getPath()` | `"prefix." + id.getNamespace() + "." + id.getPath()` |
| 变量引用 | `Component.translatable(CONSTANT_NAME)` | `Component.translatable(TAB_NAME)` |

### 6.3 键命名规范

> `modid` = `ether_craft`，以下用 `modid` 表示通用模式。

| 前缀 | 用途 | 参数 | 示例 |
|---|---|---|---|
| `block.<modid>.<id>` | 方块显示名 | 无 | `block.ether_craft.ether_adapt_node` |
| `item.<modid>.<id>` | 物品显示名 | 无 | `item.ether_craft.wrench` |
| `itemGroup.<modid>` | 创造模式标签页 | 无 | `itemGroup.ether_craft` |
| `tooltip.<modid>.<id>` | 物品简单 tooltip | `%d`/`%s` | `tooltip.ether_craft.process_chip_ether` |
| `tooltip.<modid>.<id>.<field>` | 物品多字段 tooltip | `%d` | `tooltip.ether_craft.ether_process_chip.max_ether` |
| `menu.<modid>.<screen>.<widget>` | GUI 控件文本 | 按需 | `menu.ether_craft.factory.filter` |
| `menu.<modid>.<screen>.debug.<field>` | GUI 调试信息 | `%d` | `menu.ether_craft.factory.debug.ether` |
| `menu.<modid>.node.<widget>.<label>` | 节点 GUI 标签 | `%d` | `menu.ether_craft.node.magnet.cx` |
| `<modid>.gui.node.<feature>.<state>` | 节点功能状态切换 | 无 | `ether_craft.gui.node.filter.using_black_list` |

### 6.4 参数占位符规范

- `%d` — 整数 (int, long)
- `%s` — 字符串
- `%f` — 浮点数
- `[%d]` — 装饰性方括号内的数值 (仅视觉分隔，不影响格式化)
- Minecraft `Component.translatable(key, args...)` 底层使用 `String.format()`，占位符语法与 Java 标准一致。

### 6.5 与现有 lang 文件合并

生成新键时：
1. 读取当前 `src/main/resources/assets/<modid>/lang/en_us.json`
2. 将新键**追加**到 JSON 末尾，保持键的字母/层级排序
3. 不要覆盖或删除已有键
4. 对动态键来源的数据文件（如芯片 JSON），同步检查 `data/<modid>/` 目录下的文件列表以确认所有可能的 ID
