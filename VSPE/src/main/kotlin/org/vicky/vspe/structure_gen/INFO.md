# Structure layout and pallet-ing
| Block                | Palette Group | Use / Semantic Meaning                        | Notes / Replacement Logic                                |
|----------------------|---------------|-----------------------------------------------|----------------------------------------------------------|
| `White Wool`         | `Wall`        | Walls                                         | Primary exterior walls – varies by palette               |
| `Gray Wool`          | `WoodTrim`    | Wall-integrated planks or detail pieces       | Decorative crossbeams, shutters, etc.                    |
| `Red Wool`           | `Floor`       | Floor surface                                 | Floor type (wood, tile, stone)                           |
| `Stripped Oak Wood`  | `Support`     | Support beams / vertical logs                 | Wood logs or pillars, rotated with orientation           |
| `Oak Stairs`         | `Stairs`      | Stairs                                        | Wood stairs, rotated with orientation                    |
| `Red Concrete`       | `RoofBlock`   | Roof (flat or solid)                          | Swap with slab/stair variant depending on palette        |
| `Red Sandstone Slab` | `RoofSlab`    | Roof slopes or secondary height roof parts    | Use stairs or slabs (pitch logic optional)               |
| `Light Blue Wool`    | `Window`      | Glass / window placeholders                   | Replace with glass pane types or stained glass variants  |
| `Oak Door`           | `Door`        | Door / entry point marker                     | Swap with double/trapdoor types; code places actual door |
| `Green Wool`         | `Foliage`     | Decorative foliage or vines                   | Replace with leaves, moss, vines, depending on palette   |
| `Yellow Wool`        | `LightSource` | Lighting spot                                 | Torch, lantern, glowstone, end rod, etc. based on theme  |
| `Blue Wool`          | `Furniture`   | Furniture anchors / interior marker           | Seats, beds, tables — spawn with additional logic        |
| `Magenta Wool`       | `Balcony`     | Balcony or ledge elements                     | Projected structures, fences, railings                   |
| `Orange Wool`        | `Pathing`     | Path or step anchors                          | Replace with cobble, gravel, packed mud, etc.            |
| `Purple Wool`        | `Utility`     | Functional block anchor (e.g. chest, furnace) | You can add loot tables, interactions                    |
| `Lime Wool`          | `VariantTag`  | Variant tag trigger                           | Code reads this to alter sub-style or palette variant    |
| `Pink Wool`          | `Custom`      | Reserved/custom semantic (you decide)         | Leave for plugin-specific behavior                       |
