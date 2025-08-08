# Underwater Biomes README

This document provides detailed specifications for each underwater biome in our fully submerged world. For each biome, you’ll find:

* **Features & Structures**: Key terrain elements and notable formations.
* **Block Palette**: Recommended blocks and materials to evoke the theme.
* **Rarity**: Approximate percentage coverage of the world’s seafloor.
* **Variants**: Well-justified subtypes explaining environmental or mystical variation.
* **Java Link**: Path to the corresponding biome class for direct implementation.

At the end, a **Progress Tracker** table with colored load bars helps you monitor implementation status and plan future updates.

---

## 1. Coral Reefs

**Features & Structures**

* Branching and table corals, small arches, coral bommies.
* Sunken ship fragments encrusted in coral.
* Hidden alcoves for loot and sponges.

**Block Palette**

* **Primary**: Stained terracotta (orange, pink, magenta), prismarine bricks, sea lanterns.
* **Accents**: Magenta glass panes, bone blocks (bleached coral).
* **Ground**: Sand, red sand, gravel, moss.

**Rarity**: \~8% of shallow waters.

**Variants**:

1. **Temperate Reef**: Cooler palette (Cyan & light gray terracotta), reason: found at ocean fringes with lower light and temperature.
2. **Atoll Ring**: Ring-shaped clusters with inner lagoons, reason: currents shape coral into circular patterns.
3. **Blue Coral Reef**: Due to the intense cold it leads to a stronger bluer-tinted environment.
4. **Dead coral Reef**: After exposure to abyssal radiation, the biome deteriorated leading to the dead appearance.


**Java Link**: [CoralReefs](Biomes/CoralReefs.java)

---

## 2. Seagrass Meadows

**Features & Structures**

* Rolling fields of seagrass up to 2 m tall.
* Gently undulating sandy dunes.
* Occasional broken ruins or driftwood.

**Block Palette**

* **Primary**: Seagrass blocks, kelp.
* **Accents**: Light blue wool carpets (surface ripples).
* **Ground**: Coarse dirt, sand.

**Rarity**: \~10%.

**Variants**:

1. **Deep Seagrass**: Taller, denser beds—reason: deeper light attenuation encourages vertical growth.
2. **Algal Flats**: Mixed brown and green algae—reason: nutrient-rich upwellings favor brown algae species.

**Java Link**: [src/biomes/SeagrassMeadow.java](#)

---

## 3. Rocky Coves

**Features & Structures**

* Underwater cliffs with caves, overhangs.
* Rocky sea arches.
* Hidden treasure chests in crevices.

**Block Palette**

* **Primary**: Andesite, diorite, granite.
* **Accents**: Mossy stone bricks, cracked stone bricks.
* **Ground**: Gravel, cobblestone.

**Rarity**: \~6%.

**Variants**:

1. **Volcanic Cove**: Blackstone & magma vents—reason: underwater volcanic activity.
2. **Granite Bay**: Smooth boulders & pebble shelves—reason: erosion of granite outcrops.

**Java Link**: [src/biomes/RockyCove.java](#)

---

## 4. Sunken Gardens

**Features & Structures**

* Coral-stone statues and columns.
* Giant underwater flowers, glowing mushrooms.
* Reflective pools with bioluminescent spores.

**Block Palette**

* **Primary**: Mossy stone bricks, glazed terracotta (floral), prismarine.
* **Accents**: Amethyst clusters, glowstone.
* **Ground**: Podzol, smooth sandstone.

**Rarity**: \~4%.

**Variants**:

1. **Crystal Grotto**: Walls lined with amethyst and quartz—reason: mineral-rich spring leaks.
2. **Mushroom Dell**: Giant bioluminescent mushrooms—reason: mycelial networks colonize shaded ruins.

**Java Link**: [src/biomes/SunkenGarden.java](#)

---

## 5. Mangrove Swamps

**Features & Structures**

* Tangled mangrove prop-roots forming mazes.
* Mudflats and shallow channels.
* Submerged wooden docks.

**Block Palette**

* **Primary**: Stripped mangrove wood, mangrove leaves, mud.
* **Accents**: Sponge blocks, cracked mud bricks.
* **Ground**: Mud, clay.

**Rarity**: \~7%.

**Variants**:

1. **Black Mangrove Bay**: Dark water & peat—reason: decayed organic buildup.
2. **Red Mangrove Labyrinth**: Denser root networks—reason: higher salinity fostering red mangrove growth.

**Java Link**: [src/biomes/MangroveSwamp.java](#)

---

## 6. Kelp Forests

**Features & Structures**

* Towering kelp stalks forming caverns of green.
* Bioluminescent algae clumps.
* Submarine caves carved by currents.

**Block Palette**

* **Primary**: Kelp blocks, warped stems.
* **Accents**: Prismarine shards, sea lanterns.
* **Ground**: Sand, gravel.

**Rarity**: \~12%.

**Variants**:

1. **Cold Kelp**: Darker blue-green kelp—reason: colder currents encourage pigment shifts.
2. **Biolume Kelp**: Tips glow faintly—reason: symbiosis with bioluminescent microbes.

**Java Link**: [src/biomes/KelpForest.java](#)

---

## 7. Deep Trenches

**Features & Structures**

* Vertical walls with sediment layers.
* Hydrothermal and cold seeps.
* Fossilized skeletons.

**Block Palette**

* **Primary**: Blackstone, basalt, tuff.
* **Accents**: Magma blocks, dripstone.
* **Ground**: Gravel, deepslate.

**Rarity**: \~5%.

**Variants**:

1. **Glacial Trench**: Ice shards & brine pools—reason: glacial melt infiltration.
2. **Volcanic Rift**: Magma veins & heated vents—reason: tectonic seam activity.

**Java Link**: [src/biomes/DeepTrench.java](#)

---

## 8. Twilight Caverns

**Features & Structures**

* Vaulted cave ceilings with stalactites.
* Bioluminescent fungi gardens.
* Hidden pre-human carvings.

**Block Palette**

* **Primary**: Dripstone, smooth basalt, cobbled deepslate.
* **Accents**: Glow lichen, glowstone, amethyst buds.
* **Ground**: Mud, gravel.

**Rarity**: \~9%.

**Variants**:

1. **Crystal Cavern**: Faceted quartz and amethyst—reason: silica-rich water percolation.
2. **Mushroom Grotto**: Fungal canopies—reason: nutrient runoff supports fungal growth.

**Java Link**: [src/biomes/TwilightCavern.java](#)

---

## 9. Silted Wrecks

**Features & Structures**

* Partially buried hulls, broken masts.
* Rusty cannons & cargo crates.
* Overgrown pillars and statues.

**Block Palette**

* **Primary**: Weathered planks, oxidized copper, stone bricks.
* **Accents**: Mossy bricks, iron bars.
* **Ground**: Custom silt block, mud.

**Rarity**: \~6%.

**Variants**:

1. **Pirate’s Grave**: Tattered sails & treasure chests—reason: pirate fleet ambush site.
2. **Temple Ruins**: Ornate stone pillars—reason: submerged sea-god temple.

**Java Link**: [src/biomes/SiltedWreck.java](#)

---

## 10. Abyssal Plateaus

**Features & Structures**

* Flat silt plains dotted with mineral mounds.
* Occasional vent chimneys.
* Glassy silica veins.

**Block Palette**

* **Primary**: Silt, deepslate slabs, hardened clay.
* **Accents**: Iron/gold ore nodules, basalt pillars.
* **Ground**: Powder snow (cold pockets).

**Rarity**: \~7%.

**Variants**:

1. **Mineral Field**: Scattered ore clusters—reason: uplift of ore-rich mantle.
2. **Vent Cluster**: Multiple cold seeps—reason: crustal fractures allow fluid escape.

**Java Link**: [src/biomes/AbyssalPlateau.java](#)

---

## 11. Mystic Crystal Rift

**Features & Structures**

* Obsidian-black & red crystal spires.
* Magma fissures under glassy crystal surfaces.
* Geode caverns with echoing amphitheaters.

**Block Palette**

* **Primary**: Blackstone, obsidian pillars, deepslate.
* **Crystals**: Red nether quartz, modded crystal blocks, glowing red amethyst.
* **Magma**: Magma blocks, black stained glass, dripstone.

**Rarity**: \~4%.

**Variants**:

1. **Shardfall Ravine**: Small clusters & heat vents—variation due to minor fractures.
2. **Geode Cathedral**: Vaulted geode chambers—variation where geodes grew around large voids.
3. **Volcanic Heart**: Lava lakes & crystal ribs—variation from central magma chamber.

**Java Link**: [src/biomes/MysticCrystalRift.java](#)

---

# Progress Tracker

| Biome               | Status    | Progress             |
| ------------------- |-----------| -------------------- |
| Coral Reefs         | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜ |
| Seagrass Meadows    | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜ |
| Rocky Coves         | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜ |
| Sunken Gardens      | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜ |
| Mangrove Swamps     | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜ |
| Kelp Forests        | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜     |
| Deep Trenches       | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜          |
| Twilight Caverns    | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜          |
| Silted Wrecks       | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜          |
| Abyssal Plateaus    | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜          |
| Mystic Crystal Rift | Planned   | 🔴⬜⬜⬜⬜⬜⬜⬜⬜⬜          |

*Legend:* 🟩 = implemented, 🟦 = template created, ⬜ = not started, 🔴 = pending refactoring

---