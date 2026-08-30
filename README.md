# Welcome to Ether Craft!

Ether Craft (以太工艺) is a tech mod built on **slot placement (摆格子)** as its underlying design. Unlike most tech mods, it also leaves "how a machine is made" to the player: a machine's specific functions are decided by what the player puts into its grid.

"Slot placement" shows up in two places: the plugins dropped into an Ether Adapt Node's functional/upgrade slots, and the production line you build chip-by-chip inside an Ether Process Factory.

The mod is still in development and some content is unfinished. For feedback or suggestions, reach us via the [GitHub repository](https://github.com/FantasyIT-Projects/ether-craft-neo) or the mod QQ group (1038876287).

## 1 Ether Adapt Node: put plugins into its slots

The node is empty when first crafted; it has no function until you put plugins into its slots.

Plugins come in two kinds — "function" and "upgrade". The former produce or use Ether, while the latter are mostly numeric boosts. Take the upgrades related to Ether streams as examples: any pickaxe/axe/shovel/hoe lets an Ether stream break matching blocks, a sword lets it attack mobs; a boat or minecart lets it carry entities; a written book lets it display text in flight. Different plugins also change the node's appearance.

![Several Ether Adapt Nodes: plugins appear on the faces, with Ether-glass bases and Ether-stream paths on the floor](img/img_ean.png)

## 2 Ether Process Factory: build the production line inside the machine

The factory is the clearest expression of "slot placement": you build the production line inside the machine, one grid cell at a time using chips and separators. The image below is the processing UI, with chips already laid out as the line:

![Ether Process Factory UI: chips arranged along the line, Ether bar lower-left, output slot on the right](img/img_factory_processing.gif)

The line must be a strict one-block-wide "channel", whose walls are made of separators or chips; the chip on a wall decides that step's processing, and JEI indicates whether one or two chips are needed. Once laid out, you can preview the product even without Ether; if the recipe doesn't take, it's usually because an extra step was laid in. Feed Ether to start. But the Ether inside a factory decays over time — an incomplete recipe, a gap in a wall, or an extra empty row outside the path all leak Ether (hover the bar to check; it should read 0). When the Ether reaches roughly 10× the total storage of all chips, the machine speeds up and costs more, so it tends to alternate between two speed tiers.

## 3 Ether & Ether Stream: combine functions, connect blocks

Ether is what powers things. It exists as an item — one item equals 100 points of Ether energy (E). The node produces it and feeds the machines; dropped on the ground, it slowly decays, and a stack eventually becomes a single deactivated Ether. Deactivated Ether is the start of the mod: mined from Ether ore, it crafts the "Ether Craft Guide" with a book, or crafts an Ether Adapt Node.

Ether is used through the Ether stream. The stream is an energy beam fired by emitter plugins, doing logistics and power at once: fire it out, and it automatically pours into any block that accepts Ether, dropping off items it carries along the way; it also recharges plated equipment.

![An Ether stream flowing between nodes, through an Ether-glass channel, carrying items along the way](img/img_ether_stream.gif)

## 4 Plating: equipment enhancement

Plating is another way to enhance equipment, and it doesn't conflict with enchantments or affixes. To plate an item, use an Ether stream fitted with a converging-chip plugin (the "plating stream"); the key is a piece of Ether Dust. Adding other items grants different effects, and multiple layers can be stacked at once.

## 5 Glass & miscellaneous

Ether glass is the mod's frameless glass: an Ether stream passing through normal glass, or hitting a glass item on the ground, converts it into Ether glass. It doesn't shatter and can be removed quickly with a wrench; an Ether stream inside it pays less flight cost, and holding Alt lets you operate blocks behind it (though you can't walk through it).

![An Ether-glass passage: the Ether stream runs along the floor, a dropped item beside it](img/img_misc.png)

The wrench can rotate and break blocks, and can itself be plated; with a wrench in the offhand and a plugin in the main hand, clicking a node installs the plugin quickly.
