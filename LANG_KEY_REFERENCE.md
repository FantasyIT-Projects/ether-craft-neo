# 翻译键查找与生成指南

> 如何系统性地发现项目中所有需要翻译的语言键，以及如何为 `en_us.json` / `zh_cn.json` 生成正确的条目。
>
> **最后更新:** 2026-05-20

---

## 一、翻译键来源分类

一个 Minecraft NeoForge 模组的翻译键来自以下四类：

| 来源 | 代码中可见? | 示例 |
|---|---|---|
| **框架自动生成** | 否 — 由注册 ID 推导 | `block.<modid>.<id>`, `item.<modid>.<id>` |
| **动态拼接** | 是 — `Component.translatable(key + variable)` | 枚举/数组/Identifier 拼接 |
| **静态字面量** | 是 — `Component.translatable("key")` | 直接写在代码中的键 |
| **间接引用** | 是 — `Component.translatable(CONSTANT)` | 通过常量变量引用 |

---

## 二、查找流程（按优先级执行）

### 第 1 步：扫描注册表类，推导框架自动生成的键

**目标文件:** `*Registry.java`

对每个 `DeferredRegister.register("id", ...)`，NeoForge 会按注册类型自动生成键：

| 注册类型 | 自动生成键格式 | 注意事项 |
|---|---|---|---|
| `BLOCK` | `block.<modid>.<id>` | 方块本身。若方块也在 `ItemRegistry` 中注册了 BlockItem，还需要对应的 `item.<modid>.<id>` |
| `ITEM` | `item.<modid>.<id>` | **所有 Item（包括 BlockItem）都应有此键**。通用 BlockItem 不会自动继承 block 键 |
| `CREATIVE_MODE_TAB` | `itemGroup.<modid>` | 除非显式设置了 `.title(Component.translatable(...))` |

> **关键规则：每个在 `ItemRegistry` 中注册的 Item（无论是纯物品还是 BlockItem），都必须有对应的 `item.<modid>.<id>` 翻译键。** BlockItem 不会自动回退到 `block.<modid>.<id>`——若不提供 item 键，物品栏中会显示原始 registry ID。

**当前项目示例 (ether_craft):**

- 纯物品 (`ether`, `wrench`, `blade`, ...) → `item.ether_craft.<id>`
- 通用 BlockItem (`ether_block`, `ether_ore`, `ether_glass`, ...) → 同时需要 `block.ether_craft.<id>` 和 `item.ether_craft.<id>`
- 自定义 BlockItem 子类 (factory lv1-4, node lv1-3) → `item.ether_craft.<id>`（方块使用 `block.ether_craft.ether_process_factory` 等）
- `CreativeTabRegistry` 使用 `TAB_NAME = "ether_craft_tab_main"` → 需要 `ether_craft_tab_main`，而非 `itemGroup.ether_craft`

**命令:**
```bash
# 查找所有已注册的方块
grep "BLOCKS.register" src/main/java/**/register/BlockRegistry.java

# 查找所有已注册的物品
grep "ITEMS.register" src/main/java/**/register/ItemRegistry.java
```

### 第 2 步：搜索所有 `Component.translatable()` 调用

**命令:**
```bash
rg "Component\.translatable\(" src/main/java/ -n
```

对每个调用，提取第一个参数：
- **若为字符串字面量**（如 `"menu.ether_craft.factory.filter"`）→ 直接加入列表
- **若含 `+` 拼接**（如 `"prefix." + id.getNamespace() + "." + id.getPath()`）→ 进入第 3 步
- **若为常量引用**（如 `Component.translatable(TAB_NAME)`）→ 追溯常量定义

### 第 3 步：展开动态拼接键

动态键需要追溯拼接源获取所有可能值：

#### 3a. 枚举遍历

```java
// 模式: "prefix." + enumValue.getName().toLowerCase()
Component.translatable("menu.ether_craft.node.directional_filter.direction." + direction.getName().toLowerCase())
```

**做法:** 找到枚举类（IDE: Ctrl+Click 类型），列出所有枚举常量。本例中 `Direction` 有 6 个值，故生成 6 个键。

#### 3b. 数组遍历

```java
// 模式: "prefix." + array[i]
String[] labels = {"cx", "cy", "cz", "sx", "sy", "sz"};
Component.translatable("menu.ether_craft.node.magnet." + labels[i])
```

**做法:** 找到数组定义，每个元素生成一个键。

#### 3c. Identifier / 数据组件拼接

```java
// 模式: "prefix." + id.getNamespace() + "." + id.getPath()
String baseKey = "tooltip." + id.getNamespace() + "." + id.getPath();
```

**做法:**
1. 追溯 `id` 的来源（数据组件、数据包、注册表）
2. 扫描数据目录获取所有可能的 ID

```bash
# 扫描芯片数据文件
find src/main/resources/data -path "*/ether_process_chip/*.json" -exec basename {} .json \;
```

#### 3d. ExtraRecipeProvider / 插件注册

```java
// 模式: "jei.ether_craft.plugin." + namespace + "." + pluginPath
String descKey = "jei.ether_craft.plugin." + recipe.pluginId().getNamespace() + "." + recipe.pluginId().getPath();
```

**做法:** 找到插件注册列表（`ALL_PLUGINS` 静态初始化块），提取每个插件的 `Identifier ID` 字段。

### 第 4 步：检查 `Component.literal()` 硬编码文本

```bash
rg "Component\.literal\(" src/main/java/ -n
```

若 `literal()` 内容为纯英文且不含 `+` → 界面静态文本，应改为 `Component.translatable()` 以支持本地化。

若含 `+` 拼接运行时值：
```java
Component.literal("Ether:" + be.getEther())  // 应改为
Component.translatable("menu.ether_craft.factory.debug.ether", be.getEther())
```

### 第 5 步：检查数据生成产生的隐含键

数据生成 (`datagen/`) 可能生成 JSON 模型或 recipe，其中可能嵌入 title/description 文本。检查：
- `src/main/resources/data/<modid>/` 下的 recipe JSON
- JEI 插件注册中的动态分类

---

## 三、动态键拼接模式速查

| 模式 | 检测特征 | 展开方式 |
|---|---|---|
| `key + enum.getName()` | 含枚举变量的 `translatable()` | 列出枚举所有常量 |
| `key + array[i]` | 循环内含数组索引 | 展开数组定义 |
| `key + Identifier(NS, path)` | 含 `id.getNamespace()` / `id.getPath()` | 扫描数据目录/注册列表 |
| `key + forEach` | 含集合/流的 `.forEach()` | 追溯集合来源 |
| `key + CONSTANT` | `translatable()` 参数为大写常量 | Ctrl+Click 查看常量定义 |

---

## 四、键命名规范

| 前缀 | 用途 | 示例 |
|---|---|---|
| `block.<modid>.<id>` | 方块名 | `block.ether_craft.ether_adapt_node` |
| `item.<modid>.<id>` | 物品名 | `item.ether_craft.wrench` |
| `itemGroup.<modid>` | 默认创造标签页名 | `itemGroup.ether_craft` |
| `tooltip.<modid>.<id>` | 物品 tooltip | `tooltip.ether_craft.heating_chip` |
| `menu.<modid>.<screen>.<widget>` | GUI 控件 | `menu.ether_craft.factory.filter` |
| `jei.<modid>.<category>` | JEI 分类标题 | `jei.ether_craft.ether_process` |
| `jei.<modid>.plugin.<ns>.<pid>` | JEI 插件描述 | `jei.ether_craft.plugin.ether_craft.magnet` |
| `jei.<modid>.category.<ns>.<path>` | JEI 动态分类 | `jei.ether_craft.category.ether_craft.special.furnace` |
| `message.<modid>.<id>` | 系统消息 | `message.ether_craft.plugin_installed` |
| `<modid>.gui.<element>` | 自定义 GUI 元素 | `ether_craft.gui.name_placeholder` |
| `<modid>_tab_main` | 自定义标签页标题 | `ether_craft_tab_main` |

---

## 五、参数占位符

- `%d` — 整数
- `%s` — 字符串
- `%f` — 浮点数
- `[%d]` — 装饰性方括号包裹的数值（不影响格式化）

底层使用 `String.format()`，与 Java 标准一致。

---

## 六、新键追加流程

1. 按上述步骤收集所有需要的键
2. 读取现有的 `src/main/resources/assets/<modid>/lang/en_us.json`
3. 找出差集：收集的键中尚不在现有 JSON 中的 → 需要新增
4. 将新键追加到 JSON 末尾，保持按前缀层级排序
5. 同步更新 `zh_cn.json`（或其他语言文件）
6. **不要覆盖或删除已有键**
