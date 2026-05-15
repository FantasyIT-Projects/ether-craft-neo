# JEI Integration — Design Spec

**Date**: 2026-05-15  
**Topic**: Add JEI dependency and create recipe viewer for EtherProcessFactory tree recipes

## Scope

Add the Just Enough Items (JEI) mod as a Gradle dependency and create a custom JEI recipe category that renders the tree-structured `EtherProcessFactoryRecipe` as a left-to-right flowchart.

## Dependencies

- JEI artifact: `mezz.jei:jei-26.1.2-neoforge:29.5.0.28` (runtime)
- JEI API: `mezz.jei:jei-26.1.2-common-api:29.5.0.28` (compileOnly)
- JEI NeoForge API: `mezz.jei:jei-26.1.2-neoforge-api:29.5.0.28` (compileOnly)
- Maven repo: `https://maven.blamejared.com`

## File Placement

```
src/main/java/studio/fantasyit/ether_craft/integration/jei/
├── JEIPlugin.java              // IModPlugin — registers recipe category & recipes
└── EtherProcessCategory.java   // IRecipeCategory<EtherProcessFactoryRecipe> — renders tree
```

## Recipe Structure (Recap)

`EtherProcessFactoryRecipe` contains:
- `TreeLike<Integer, List<DelayedIngredient>> process` — directed tree from inputs → processes → output
- `List<SizedIngredient> input` — input items
- `List<ItemStackTemplate> output` — output items

The tree has:
- **Input nodes**: actual item ingredients (leaves)
- **Process nodes**: require chips (intermediate nodes)
- **Root node**: output (root, has no outgoing parent edge)
- DIRECT_INPUT virtual nodes exist between inputs and their process node targets — these are implementation details and should **not** be shown to the player.

Example diamond recipe tree:
```
I0:Coal ──→ [P1:Heating Chip] ──┐
                                 ├──→ [P0:Heating Chip ×2] ──→ Diamond
I1:Coal ──→ [P2:Heating Chip] ──┘
```

## Layout Algorithm

### 1. Compute Levels (BFS from root backward)

Starting from the root (output) node, traverse edges in reverse:
- Root node = level 0
- Each edge backward increments level
- The outermost input nodes get the maximum level

### 2. Position Nodes

- X coordinate: `level × 80px` (columns)
- Y coordinate: centered vertically within the column, with 24px between nodes
- Total GUI width: `(maxLevel + 1) × 80px`
- Total GUI height: `max(nodesPerLevel) × 24px + paddings`

### 3. Render Slots

| Node Type | JEI Slot Type | Content |
|-----------|---------------|---------|
| Input items | `INPUT` | `SizedIngredient` from `recipe.input` |
| Process chips | `CATALYST` | Chip icon/info from `DelayedIngredient` on edges |
| Output | `OUTPUT` | `ItemStackTemplate` from `recipe.output` |

For process nodes that have multiple chip ingredients, show them stacked or listed in a small grid (max 3×2).

### 4. Draw Arrows

Between each connected pair (parent → child in tree, which means process → its previous step):
- Start from right edge of source node slot
- Draw horizontal line to midpoint
- Vertical line to align with target's Y
- Then horizontal to left edge of target node slot

Use `GuiGraphics` methods (not JEI arrow textures) for custom path lines.

### 5. Filter Out DIRECT_INPUT Nodes

When building the layout, skip any node whose value (Integer ID) corresponds to a DIRECT_INPUT virtual entry. Only show actual input items and process steps.

## Catalyst Interaction

Clicking a process chip slot should show JEI's built-in tooltip for the chip item. No custom recipe lookup depth needed for chips. The `CATALYST` slot type in JEI properly handles this.

## Build Changes

### `build.gradle`

Add under `repositories`:
```gradle
maven {
    url "https://maven.blamejared.com"
}
```

Add under `dependencies`:
```gradle
compileOnly "mezz.jei:jei-26.1.2-common-api:29.5.0.28"
compileOnly "mezz.jei:jei-26.1.2-neoforge-api:29.5.0.28"
runtimeOnly "mezz.jei:jei-26.1.2-neoforge:29.5.0.28"
```

### `gradle.properties`

Add variable:
```properties
jei_version=29.5.0.28
```

## Edge Cases

- **Recipes with single input**: Degenerate tree — input → [chip] → output. Still rendered as 3-column layout.
- **Wide trees (deep levels)**: Scrollable JEI recipe page handles this natively.
- **Tall trees (many same-level nodes)**: JEI scrolls vertically.
- **No chips in a process step**: Should not occur in valid recipes, but handle gracefully (skip empty chip lists).
- **DIRECT_INPUT path**: Input nodes also have a DIRECT_INPUT variant edge — skip these in visualization.
