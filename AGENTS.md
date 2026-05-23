# AGENTS.md — ether_craft

## CRITICAL: Tool Usage Mandate

**THIS IS NON-NEGOTIABLE. VIOLATING THIS RULE MEANS CORRUPTED RESULTS AND WASTED TIME.**

When viewing **ANY file within this project** or **ANY class from external dependencies/libraries** (NeoForge, Minecraft, etc.), you **MUST ALWAYS** use the IDE-provided MCP tools:

- `idea_read_file` — for reading project files
- `idea_get_file_text_by_path` — for reading project files by path
- `idea_search_symbol` — for looking up classes/methods/fields by name
- `idea_get_symbol_info` — for inspecting a specific symbol's declaration

**YOU ARE ABSOLUTELY FORBIDDEN FROM USING**: `Read` tool (local filesystem read), `Glob`, `Grep`, `Bash` file operations, or any other non-IDE tool to **read, search, or inspect** project source files or dependency classes.

**When editing/writing code**, you may use any tool: `Edit`, `Write`, `idea_replace_text_in_file`, `idea_create_new_file` are all acceptable. There is no restriction on editing tools.

**THE ONLY EXCEPTION** is for non-Java files like `build.gradle`, `gradle.properties`, JSON resources, Gradle wrapper, `.gitignore`, and other config/asset files — these may be read with any tool.

**IF ANY IDE MCP TOOL FAILS OR RETURNS AN ERROR, OR IF THE TOOLS ARE NOT AVAILABLE IN YOUR ENVIRONMENT:**
1. **STOP IMMEDIATELY.** Do NOT proceed. Do NOT attempt to fall back to other tools.
2. Report the exact error clearly to the user and wait for instructions.
3. Under no circumstances attempt to read project Java files or dependency classes through alternative means.

This requirement exists because the IDE MCP tools understand Java semantics, resolve symbols correctly, navigate inheritance hierarchies, and decompile class files — capabilities that raw filesystem tools lack. Using raw tools on Java source will produce incomplete, misleading, or flat-out wrong results.

## CRITICAL: Subagent Usage

**THIS IS NON-NEGOTIABLE. USING THE WRONG SUBAGENT TYPE MEANS INCOMPLETE OR INCORRECT RESULTS.**

When dispatching subagents (via the `Task` tool):

- **ALWAYS use `subagent_type = "general"`. NEVER use `"explore"`.** The `explore` agent is inferior — it lacks the full toolset, cannot use IDEA MCP tools properly, and routinely produces incomplete, misleading results. Do not even consider it.

- **All subagents MUST use IDEA MCP tools** (`idea_read_file`, `idea_search_symbol`, `idea_get_symbol_info`, etc.) for reading or inspecting any Java source (project files or dependencies). Subagents are bound by the same Tool Usage Mandate above. When giving a subagent its task prompt, **you MUST explicitly instruct it** to use IDEA MCP tools for all file reads and symbol searches, and to never fall back to filesystem read tools.

- **To create a read-only subagent**, instruct it in the prompt: "You are a read-only agent. Do not create, edit, or write any files." The `general` agent will respect this. There is no need to use `explore` for read-only work — `general` with a read-only directive is always the correct choice.

- **Subagent prompts must contain**: (1) a clear instruction to use IDEA MCP tools exclusively for Java source, (2) the specific task and expected output format, (3) whether file editing is permitted or forbidden.

**YOU ARE NEVER PERMITTED TO USE `subagent_type = "explore"`. PRETEND IT DOES NOT EXIST.**

## Build & Run

Java 25 is required. Use the IDE MCP build tool to compile and verify code:

- `idea_build_project` — triggers a Gradle build via the IDE and returns compile errors. Use this after every edit to validate correctness. Accepts `rebuild`, `filesToRebuild`, and `timeout` parameters.
- `idea_execute_run_configuration` — launches `runClient`, `runServer`, `runData`, `runGameTestServer` via existing IDE run configurations, or from a code location (`filePath` + `line`).
- `idea_get_run_configurations` — lists available run configurations.

There is no lint/typecheck step beyond `build`. No test suite aside from gametests.

## Architecture

- **Mod ID**: `ether_craft` (`EtherCraft.MODID`)
- **Base package**: `studio.fantasyit.ether_craft`
- **Framework**: NeoForge, Minecraft 26.1.2, `moddev` plugin 2.0.141
- **Registries** live in `register/` — each follows the `DeferredRegister` + static `DeferredHolder` fields + static `register(IEventBus)` convention. Registries are wired in `EtherCraft.java:44-54`.
- **Blocks**: `EtherAdaptNode`, `EtherProcessFactory`, `EtherStreamEmitter` via `BlockRegistry`.
- **Block entities** handle item transfer through NeoForge's `ResourceHandler<ItemResource>` API with `Transaction`/`TransactionContext`. `BaseEtherContainerBlockEntity` is the shared base.
- **Networking** (`network/`): Uses NeoForge payload handlers. C2S packets for GUI interactions, S2C packets for syncing block data. Protocol version `"1"`. Server/client registrars are split via `@EventBusSubscriber(Dist)` inner classes in `Network.java`.
- **Capabilities** (`CapabilityRegistry`): Registers `EtherContainer` and `Capabilities.Item.BLOCK` on all three block types.
- **Config** (`Config.java`): `ModConfigSpec` with `@EventBusSubscriber(modid = ...)` for `onLoad`. Values exposed as public static int/List fields.
- **Recipes**: Two custom recipe types — `NodeProcessRecipe` and `EtherProcessFactoryRecipe`. Both registered in `RecipeTypeRegistry`/`RecipeSerializerRegistry`. Uses `DelayedIngredient` wrapper that resolves `SizedIngredient` or tag-based lookups lazily.
- **Data generation** (`datagen/`): Tag generators (`TagGenBlock`, `TagGenItem`), model gen (`ModelDataGen`), data map gen (`DataMapGen`), and a gathered event (`GenerateGatherEvent`).
- **Mixins**: Config file exists (`ether_craft.mixins.json`) but is empty — no mixins are currently in use. Mixin package declared as `studio.fantasyit.ether_craft.mixin`. Do NOT fill this in speculatively.
- **Resource loading**: `DataLoadRegister` listens on both `AddServerReloadListenersEvent` and `AddClientReloadListenersEvent`.
- **Mod metadata**: Generated from `src/main/templates/META-INF/neoforge.mods.toml` via the `generateModMetadata` Gradle task. Properties are expanded from `gradle.properties`.

## Conventions

- Utility for mod-scoped identifiers: `EtherCraft.id(path)` returns `Identifier("ether_craft", path)`.
- Block entities that need per-tick logic implement the `ITickable` interface (not Minecraft's built-in ticking — check `block/base/ITickable.java`).
- Item transfer uses `Transaction.openRoot()` try-with-resources and explicit `commit()`. Insert/extract indices are segmented: input slot range, internal range, output range.
- Components/capabilities serialize via `ValueInput`/`ValueOutput` (NeoForge's data attachment API), not NBT directly.
- There is no standard `@NotNull` on overrides — some files use `@NotNull`, others don't. Match the file you're editing.
