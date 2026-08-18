# 存档加载稳定性测试流程

> 目的：逐个启动 run/saves 下的有效存档，确认 ether_craft mod 在真实存档场景下加载与运行无崩溃、无异常。
> 测试日期：2026-08-18　|　测试人：Agent（DSH）　|　测试方式：客户端 quickPlay 自动进入存档 + 日志检查

## 一、测试环境

| 项目 | 值 |
|------|-----|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.61-beta |
| Mod | ether_craft（开发环境，build/classes 直接加载） |
| 运行配置 | IDE 的 `Client`（Java 应用，`@build/moddev/clientRunProgramArgs.txt`，工作目录 `run/`） |
| 游戏目录 | `run/`（存档位于 `run/saves/`） |
| 日志 | `run/logs/latest.log` |
| 崩溃报告 | `run/crash-reports/` |

## 二、存档筛选规则

`run/saves/` 下共 30 个存档，**排除所有"新的世界"系列**（`新的世界`、`新的世界 (x)`、`新的世界2 (1)`），即跳过默认/复制生成的空白存档，只测试 21 个有具体名称的场景存档。

## 三、测试步骤（每个存档）

1. **写参数**：在 `build/moddev/clientRunProgramArgs.txt` 的 `# User Supplied Program Arguments` 后追加两行：
   ```
   --quickPlaySingleplayer
   "存档名"
   ```
   存档名用引号包裹（含空格/中文）。
2. **启动**：通过 IDE 运行配置 `Client`（`waitForExit=false`）启动客户端，游戏会自动进入目标存档。
3. **等待 60 秒**：从启动到检查总计约 60s（客户端启动 ~30s + 存档加载 + 运行观察 ~30s）。压力场景存档无需更长时间，重点验证加载期不崩溃。
4. **检查**（全部满足才判定通过）：
   - 客户端进程存活（`Get-Process java,javaw`，内存 1.5GB+ 为正常运行）；
   - 世界加载成功：日志出现 `Saving chunks for level 'ServerLevel[存档名]'` 与 `Time elapsed: xxx ms`；
   - 无 ether_craft 相关错误：日志无 `ether_craft`/包名 `studio.fantasyit` 相关的 ERROR/Exception；
   - 无 ERROR 级日志（其他 mod 的已知问题除外，见第五节）；
   - `run/crash-reports/` 无本次启动产生的新崩溃报告；
   - 存档文件夹最近写入时间与启动时间吻合（确认确实加载了目标文件夹——quickPlay 按文件夹名匹配，但日志显示的是 level.dat 的显示名，二者可能不同）。
5. **收尾**：`Stop-Process -Id <PID> -Force` 终止客户端，等待 3s，删除 args 文件中追加的两行还原参数文件。

> 注意：若某存档的 level.dat 显示名与文件夹名不同（如"建筑服场景"文件夹的显示名是"新的世界"），以**文件夹写入时间**为准判断实际加载目标，不要被日志中的显示名误导。

## 四、测试结果（21/21 通过）

| # | 存档 | 加载耗时 | 结果 |
|---|------|---------|------|
| 1 | EAN 物流压力测试 | 1364 ms | ✅ |
| 2 | test1 | 1549 ms | ✅ |
| 3 | 以太流Capabilities测试 | 1412 ms | ✅ |
| 4 | 以太流压力测试 | 1394 ms | ✅ |
| 5 | 以太流压力测试无碰撞 | 1416 ms | ✅ |
| 6 | 以太流发射器链传输 | 1373 ms | ✅ |
| 7 | 以太流展示模式压力测试 | 1328 ms | ✅ |
| 8 | 以太流破坏方块性能测试 | 1413 ms | ✅ |
| 9 | 以太流长文本测试 | 1369 ms | ✅ |
| 10 | 动态模型测试 | 1323 ms | ✅ |
| 11 | 工厂多步多线程测试 | 1322 ms | ✅ |
| 12 | 工厂多线程测试 | 1321 ms | ✅ |
| 13 | 建筑服场景 | 1454 ms | ✅ |
| 14 | 碰撞测试 | 1477 ms | ✅ |
| 15 | 边界情况测试 | 1463 ms | ✅ |
| 16 | 长以太流碰撞测试 | 1311 ms | ✅ |
| 17 | 红石测试 | 1312 ms | ✅ |
| 18 | 简单测试 | 1416 ms | ✅ |
| 19 | 建筑服0811 | 1238 ms | ✅ |
| 20 | 建筑服0818 | 1218 ms | ✅ |
| 21 | 携带实体测试 | 1450 ms | ✅ |

> 注：2026-08-18 重跑全部 21 个场景存档（较上次新增 建筑服0818、携带实体测试），加载耗时为本轮实测值。

**结论：ether_craft 在全部 21 个场景存档中加载与运行均无崩溃、无异常。** 压力测试类存档（以太流/工厂系列）中的 block entity tick 在观察期正常运转，capabilities、发射器链传输、碰撞、红石、携带实体等场景均正常。

## 五、日志噪音（非 ether_craft 问题，无需处理）

- `powertool:fake_water_block` 等 blockstate 定义加载失败（ERROR）——powertool mod 资源缺失。
- 个别第三方 mod 的 mixin 类找不到（ClassNotFoundException：cable_facades / justdirethings / mekanism / immersiveengineering 客户端类）。
- Iris 更新检查网络失败（SocketException）。
- ether_craft 启动时存在一批配方 `can't be placed due to empty ingredients` 的 **WARN**（非 ERROR），为已知占位配方警告，不影响稳定性。

## 六、注意事项

- 测试前确认无残留 Minecraft 进程（`Get-Process java,javaw`），避免存档文件锁冲突。
- 进程终止后等待约 3s 再启动下一个实例。
- `--quickPlaySingleplayer` 匹配的是**存档文件夹名**；日志中 `ServerLevel[...]` 显示的是 level.dat 的显示名。
- 测试完成后务必还原 `clientRunProgramArgs.txt`（删除追加的 quickPlay 两行），否则下次手动启动会直接进存档。
- 本流程未做性能 profiling（JProfiler 部分见 docs/PROFILING.md），仅验证稳定性。
