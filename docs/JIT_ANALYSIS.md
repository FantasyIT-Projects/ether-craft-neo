# JVM 方法内联 / 逃逸分析流程

## 前置说明

- **目标**：分析热路径方法在 JIT（C2）下的内联行为，定位未内联的调用（尤其是接口/多态虚调用）
- **目标 JVM**：JDK 25（本文结论均基于该版本实测）
- **关键结论**：JDK 25 中 `-XX:+PrintEscapeAnalysis` 和 `-XX:+PrintEliminateAllocations` 均为 **develop 级别选项，只在 debug VM 中可用**，且 `-XX:+UnlockDeveloperVMOptions` 已被移除。因此逃逸分析无法直接打印，需改用间接手段（内联树推断 / JFR 分配采样）。

## 一、准备启动

1. 在 `build/moddev/` 下被测运行配置的 **Program Args 文件**（如 `xxxRunProgramArgs.txt`）末尾追加自动进图参数：
   ```
   --quickPlaySingleplayer
   "存档名"
   ```
2. 在 **VM Args 文件**（如 `xxxRunVmArgs.txt`）末尾追加诊断参数（见下）。
3. 通过 IDE 启动对应 `Client`/`Server` 运行配置（`waitForExit=false`）。
4. 等待游戏完全加载 + 压力场景运行足够时间，让 C2 编译触发（方法调用达到阈值，tick 类高频方法几十秒足够）。

## 二、方法内联分析

在 VM Args 文件追加：

```
-XX:+UnlockDiagnosticVMOptions
"-XX:CompileCommand=PrintInlining,<全限定类名>::<方法名>"
```

每个要分析的方法单独一行。

### 参数语法坑（极易踩）

| 错误写法 | 正确写法 | 原因 |
|---|---|---|
| `CompileCommand=printinline,...` | `CompileCommand=PrintInlining,...` | 命令名是 `PrintInlining`，`printinline` 不是合法命令 |
| `studio/pkg/Cls::method`（`/`+`::`） | `studio.pkg.Cls::method`（点分） | 方法 pattern 不能同时用 `/` 和 `::` |
| 参数不加引号直接放 argfile | `"-XX:CompileCommand=...,...` | argfile 中逗号是参数分隔符，必须用双引号包裹整个参数 |

**快速验证**（避免白等游戏加载才发现参数错误）：

```
java -XX:+UnlockDiagnosticVMOptions "-XX:CompileCommand=PrintInlining,<类>::<方法>" -version
```

能正常打印 `CompileCommand: ... = true` 且无报错即参数合法。

### 输出采集

- printinline 内联树输出到 stdout，被 IDE 捕获到 console 输出文件（run 配置返回的 `fullOutputPath`）。
- 定位：搜索 `::<方法名> (` 找到内联树；C2 热编译的树带 `inline (hot)` 标注。
- 同一方法可能有多棵树（C1 tier3 / C2 tier4 / 不同变体），全部打印，需对比分析。

## 三、结果解读

### 编译层区分

- **C1（tier3）**：加载期编译，内联保守，常见 `no static binding` / `callee is too large`。
- **C2（tier4）**：热点编译，特征为 `(hot)` 标注、`TypeProfile (N/N)`、`already compiled into a medium method`、`hot method too big` 等。

### 决策含义

| 输出 | 含义 |
|---|---|
| `inline` / `inline (hot)` | 已内联，✅ |
| `accessor` | getter/setter，已内联 |
| `(intrinsic)` | 转 CPU 指令（如 `Math::ceil`/`Math::max`）|
| `failed to inline: callee is too large` | 被调方法字节数超过内联预算 |
| `failed to inline: virtual call` | 多态虚调用，调用点 profile 显示多种接收者，无法内联 |
| `failed to inline: no static binding` | 接口/抽象调用无静态绑定（常见于 C1 或无 profile 数据）|
| `already compiled into a medium method` | callee 已独立编译且中等大小，C2 为控制代码膨胀不再内联 |

### 判断技巧

1. **接口调用是否 devirtualize**：内联树中若出现具体实现类方法名 + guard，说明 devirtualize 成功；若显示 `virtual call` / `no static binding`，则为多态虚调用，保留动态分派。
2. **连锁反应**：内层循环活跃 → 外层方法编译变大 → 外层方法在调用方也不再内联（`already compiled into a medium method`）。改内层代码时注意外层内联状态变化。
3. **self 时间高 ≠ 单次开销大**：小方法 self 高通常是**调用次数 × 单次开销** 的聚合结果（"万箭齐发"型），需量化占比（JFR / 采样）后再决定是否优化，避免改错方向。

## 四、场景代表性校验（重要）

- 确认测试场景**真正触发了目标代码路径**。例如空列表循环：迭代器会创建（`iterator` 有 profile），但 `next`/循环体调用点一次未执行 → **无 profile 数据**，C2 不打印也不内联，容易被误判为"该调用未内联"。
- 需要验证真实场景时，选择带目标数据的存档/场景（如挂载了真实能力的场景），并对比两个场景的决策差异。

## 五、清理

1. 还原两个 argfile（删除追加的 quickPlay 与诊断参数行）。
2. 删除 `run/` 下产生的 `hotspot_pid*.log`（如需 LogCompilation 时生成）。
3. 用命令行过滤 `Win32_Process` 的 CommandLine，只杀掉被测进程（命令行含 Program Args 文件路径的 java 进程），**不要杀 Gradle daemon**（命令行以 `--add-opens` 开头的 jbr java 进程）。

## 注意事项

- `-XX:+LogCompilation` 在部分 JetBrains Runtime 版本下**不生成 `<compile>` 元素**（XML 不完整），主要依赖 stdout 的 printinline 树；如用 JITWatch 需验证 XML 是否含 compile 任务。
- 诊断参数有轻微性能开销，仅分析期使用，不要长期保留在 VM Args 中。
- 逃逸分析替代方案：用 JFR 的 allocation profiling 测量目标路径的实际对象分配量（`-XX:StartFlightRecording=settings=profile`），间接推断内联/标量替换效果。
