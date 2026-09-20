# VeinMiner BetterNether Ruby Fire Compat

A small compatibility patch for **Minecraft 1.21.1** that allows BetterNether's **Ruby's Fire** enchantment to work correctly with **Miraculixx VeinMiner**.

## The Problem

BetterNether's Ruby's Fire enchantment automatically smelts compatible block drops when mining.

For example:

```text
Iron Ore → Iron Ingot
Gold Ore → Gold Ingot
```

Normally, this works correctly when breaking blocks one at a time.

However, when using VeinMiner, only the block directly mined by the player receives Ruby's Fire conversion. The additional blocks broken by VeinMiner drop their normal items instead.

Example before this patch:

```text
8 Iron Ore vein

Directly mined block:
1 Iron Ore → Iron Ingot

VeinMiner secondary blocks:
7 Iron Ore → Raw Iron
```

This compatibility mod applies Ruby's Fire conversion to the secondary blocks broken by VeinMiner as well.

## Result

With this patch installed:

```text
8 Iron Ore vein
        ↓
VeinMiner
        ↓
Ruby's Fire
        ↓
Iron Ingots
```

All compatible blocks in the vein are processed through the Ruby's Fire conversion.

## Fortune Support

Fortune is supported.

The compatibility patch preserves Minecraft's normal loot calculation before performing Ruby's Fire conversion.

The process is effectively:

```text
Ore
 ↓
Minecraft calculates drops with Fortune
 ↓
Raw ore items
 ↓
Ruby's Fire conversion
 ↓
Smelted items
```

For example:

```text
Iron Ore + Fortune III
        ↓
3 Raw Iron
        ↓
Ruby's Fire
        ↓
3 Iron Ingots
```

This means Fortune-enhanced quantities are preserved.

## Silk Touch

Silk Touch continues to follow BetterNether's normal Ruby's Fire behavior.

If Silk Touch prevents Ruby's Fire conversion normally, this compatibility patch does the same for blocks broken through VeinMiner.

## How It Works

Miraculixx VeinMiner handles secondary blocks differently from the block directly broken by the player.

The directly mined block follows Minecraft's normal block-breaking process, allowing BetterNether to intercept the drop and apply Ruby's Fire.

VeinMiner's secondary blocks instead calculate their loot directly using Minecraft's block-drop system.

This mod adds a small compatibility hook into that secondary-drop process.

It:

1. Allows VeinMiner to calculate the original drops normally.
2. Preserves Fortune and other loot-table behavior.
3. Checks whether the player's tool has BetterNether's Ruby's Fire enchantment.
4. Looks for the appropriate blasting recipe.
5. Converts the resulting drops into their smelted equivalents.
6. Returns those converted drops back to VeinMiner.

The mod does **not** replace VeinMiner's mining logic, vein detection, durability handling, or block-breaking system.

## Compatibility

Built and tested with:

* Minecraft **1.21.1**
* NeoForge **21.1.x**
* Miraculixx VeinMiner **2.10.1**
* BetterNether **21.0.11**
* BCLib **21.0.13**
* WorldWeaver **21.0.13**
* Sinytra Connector **2.0.0-beta.14**
* Forgified Fabric API

The tested BetterNether installation uses the Fabric version of BetterNether through **Sinytra Connector** on NeoForge.

### Required

* Minecraft 1.21.1
* NeoForge
* Miraculixx VeinMiner 2.10.1
* BetterNether

BetterNether's normal dependencies are still required.

## Installation

Download:

```text
vm-betternether-rubyfire-compat-0.1.1+mc1.21.1.jar
```

Place the JAR inside your Minecraft:

```text
.minecraft/mods/
```

folder alongside VeinMiner and BetterNether.

Example:

```text
mods/
├── veinminer-neoforge-2.10.1+1.21.1.jar
├── better-nether-21.0.11.jar
├── bclib-21.0.13.jar
├── worldweaver-21.0.13.jar
└── vm-betternether-rubyfire-compat-0.1.1+mc1.21.1.jar
```

If BetterNether is being run through Sinytra Connector, keep your existing Connector and Forgified Fabric API setup unchanged.

## Multiplayer / Server Installation

For a modded server, install the compatibility JAR in the server's `mods` folder.

It is recommended to install the same compatibility JAR on both the server and connecting clients when using the same modpack.

## Tested Behavior

The following behavior has been tested successfully:

* Ruby's Fire + VeinMiner

  * Secondary iron ore blocks correctly drop iron ingots.

* Fortune + Ruby's Fire + VeinMiner

  * Fortune quantities are preserved and converted into smelted drops.

* Normal pickaxe + VeinMiner

  * Normal VeinMiner drops remain unchanged.

* Ruby's Fire without VeinMiner

  * BetterNether's original behavior remains unchanged.

* Existing worlds

  * The mod works without requiring a new world.

## Safety / World Data

This compatibility mod does not register:

* blocks
* items
* entities
* dimensions
* biomes
* world-generation content

It only modifies the drop-processing path used by VeinMiner.

Because of this, removing the compatibility mod should not damage existing worlds or leave behind missing registry entries.

As always, backups are recommended before changing mods on an important world or server.

## Version

Current version:

```text
0.1.1
```

For:

```text
Minecraft 1.21.1
VeinMiner 2.10.1
BetterNether 21.0.x
```

## Why This Exists

This patch was created to solve a very specific interaction between two otherwise working mods:

**VeinMiner bypasses the block-drop method that BetterNether normally uses to apply Ruby's Fire to mined blocks.**

Rather than replacing either mod's behavior, this patch adds the missing conversion step to VeinMiner's secondary block drops.

## Disclaimer

This is an unofficial compatibility patch and is not affiliated with or maintained by the developers of BetterNether, VeinMiner, NeoForge, or Sinytra Connector.

All credit for those projects belongs to their respective authors.

## License

This compatibility patch may be distributed under the MIT License.
