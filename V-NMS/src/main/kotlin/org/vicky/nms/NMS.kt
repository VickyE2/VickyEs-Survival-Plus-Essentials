package org.vicky.nms

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity
import net.minecraft.world.level.levelgen.WorldgenRandom
import org.bukkit.block.structure.Mirror as BukkitMirror
import org.bukkit.block.structure.StructureRotation as BukkitRotation
import net.minecraft.world.level.block.Mirror as NMSMirror
import net.minecraft.world.level.block.Rotation as NMSRotation
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement
import org.bukkit.Location
import org.bukkit.Rotation
import org.bukkit.block.structure.Mirror
import org.bukkit.craftbukkit.v1_20_R3.CraftWorld
import java.util.*
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_20_R3.util.CraftMagicNumbers


abstract class NMSHandler {
    /**
     * Place a structure at the given world location.
     * @param location The block location where to place.
     * @param structureName The structure file name (without `.nbt`).
     * @param namespace The data pack namespace (e.g., "vspe_city").
     * @param rotation The rotation to apply.
     * @param mirror Optional mirror, default NONE.
     */
    abstract fun placeStructure(
        location: Location,
        structureName: String,
        namespace: String,
        loot_table: String? = "",
        rotation: BukkitRotation = BukkitRotation.NONE,
        mirror: Mirror = Mirror.NONE,
        replacementPalette: Map<Material, PaletteFunction>
    )
    abstract fun placeJigsawStructure(
        location: Location,
        poolKey: String,
        namespace: String,
        level: ServerLevel = (location.world as CraftWorld).handle,
        loot_table: String?,
        rotation: BukkitRotation
    )
}
class NMS_1_20_4_Handler : NMSHandler() {
    override fun placeStructure(
        location: Location,
        structureName: String,
        namespace: String,
        loot_table: String?,
        rotation: BukkitRotation,
        mirror: Mirror,
        replacementPalette: Map<Material, PaletteFunction>
    ) {
        val world = (location.world as CraftWorld).handle

        // Convert enums
        val mcRotation = when (rotation) {
            BukkitRotation.NONE -> net.minecraft.world.level.block.Rotation.NONE
            BukkitRotation.CLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_90
            BukkitRotation.CLOCKWISE_180 -> net.minecraft.world.level.block.Rotation.CLOCKWISE_180
            BukkitRotation.COUNTERCLOCKWISE_90 -> net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90
        }
        val mcMirror = when (mirror) {
            Mirror.NONE -> net.minecraft.world.level.block.Mirror.NONE
            Mirror.LEFT_RIGHT -> net.minecraft.world.level.block.Mirror.LEFT_RIGHT
            Mirror.FRONT_BACK -> net.minecraft.world.level.block.Mirror.FRONT_BACK
        }

        // Load the structure template
        val structureManager = world.server.structureManager
        val resourceLocation = ResourceLocation(namespace, structureName)
        val template = structureManager.get(resourceLocation)
            .orElseThrow { IllegalArgumentException("Structure $resourceLocation not found") }
        val settings =
            StructurePlaceSettings()
                .setRotation(mcRotation)
                .setMirror(mcMirror)
                .setIgnoreEntities(false)
        val basePos = BlockPos(location.blockX, location.blockY, location.blockZ)

        // Place it at location
        template.placeInWorld(
            world,
            basePos,
            basePos,
            settings,
            world.random,
            2
        )

        if (replacementPalette.isNotEmpty()) {
            val boundingBox = template.getBoundingBox(settings, basePos)
            val center = boundingBox.center

            BlockPos.betweenClosedStream(boundingBox).forEach { pos ->
                val blockState = world.getBlockState(pos)
                val material = CraftMagicNumbers.getMaterial(blockState.block)

                val paletteFunc = replacementPalette[material]
                if (paletteFunc != null) {
                    val height = pos.y - boundingBox.minY()
                    val distanceFromCenter = pos.distManhattan(center)

                    val replacementMaterial = paletteFunc(pos, height, distanceFromCenter, world.random)
                    val replacementBlock = CraftMagicNumbers.getBlock(replacementMaterial)

                    world.setBlock(pos, replacementBlock.defaultBlockState(), 3)
                }
            }
        }

        if (!loot_table.isNullOrEmpty()) {
            val boundingBox = template.getBoundingBox(settings, basePos)

            BlockPos.betweenClosedStream(boundingBox).forEach { pos ->
                val blockEntity = world.getBlockEntity(pos)
                if (blockEntity is net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity) {
                    blockEntity.setLootTable(ResourceLocation(loot_table.split(":")[0], loot_table.split(":")[1]), world.random.nextLong())
                }
            }
        }
    }
    override fun placeJigsawStructure(
        location: Location,
        poolKey: String,
        namespace: String,
        level: ServerLevel,
        loot_table: String?,
        rotation: BukkitRotation
    ) {
        val registryAccess = level.registryAccess()
        val structureManager = level.structureManager()
        val templateManager = level.structureManager
        val chunkGenerator = level.chunkSource.generator
        val biomeSource = chunkGenerator.biomeSource
        val randomState = level.chunkSource.randomState()
        val seed = level.seed
        val worldGenRandom = WorldgenRandom(RandomSource.create(seed))
        val poolRegistry = registryAccess.registryOrThrow(Registries.TEMPLATE_POOL)
        val finalKey = ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation(namespace, poolKey))
        val holder = poolRegistry.getHolder(finalKey).orElseGet { throw IllegalArgumentException("No pool for $finalKey found") }
        val origin = BlockPos(location.blockX, location.blockY, location.blockZ)

        val generationContext = Structure.GenerationContext(
            registryAccess,
            chunkGenerator,
            biomeSource,
            randomState,
            templateManager,
            worldGenRandom,
            seed,
            location.toChunkPos(),
            level
        ) { true }

        JigsawPlacement.addPieces(
            generationContext,
            holder,
            Optional.of(finalKey.location()),
            8,
            origin, // fallback name (starting jigsaw ID)
            false,
            Optional.empty(), // No heightmap
            128
        ) { it }
            .ifPresent { stub ->
                val pieces = stub.piecesBuilder.build().pieces

                for (piece in pieces) {
                    if (piece is PoolElementStructurePiece) {
                        piece.element.place(
                            templateManager,
                            level,
                            structureManager,
                            chunkGenerator,
                            origin,
                            piece.position,
                            rotation.toNMS(),
                            BoundingBox.infinite(),
                            level.random,
                            false
                        )
                        val chunk = level.getChunkAt(piece.position)
                        if (!loot_table.isNullOrEmpty())
                        for ((_, blockEntity) in chunk.blockEntities) {
                            val tables = loot_table.split(":")
                            when (blockEntity) {
                                is ChestBlockEntity -> {
                                    blockEntity.setLootTable(ResourceLocation(tables[0], tables[1]), level.random.nextLong())
                                }

                                is BarrelBlockEntity -> {
                                    blockEntity.setLootTable(ResourceLocation(tables[0], tables[1]), level.random.nextLong())
                                }

                                is ShulkerBoxBlockEntity -> {
                                    blockEntity.setLootTable(ResourceLocation(tables[0], tables[1]), level.random.nextLong())
                                }
                            }
                        }
                    }
                }
            }
    }
}

fun Location.toChunkPos(): ChunkPos {
    return ChunkPos(this.blockX shr 4, this.blockZ shr 4)
}

fun BukkitRotation.toNMS(): NMSRotation = when (this) {
    BukkitRotation.NONE -> NMSRotation.NONE
    BukkitRotation.CLOCKWISE_90 -> NMSRotation.CLOCKWISE_90
    BukkitRotation.CLOCKWISE_180 -> NMSRotation.CLOCKWISE_180
    BukkitRotation.COUNTERCLOCKWISE_90 -> NMSRotation.COUNTERCLOCKWISE_90
}

fun BukkitMirror.toNMS(): NMSMirror = when (this) {
    BukkitMirror.NONE -> NMSMirror.NONE
    BukkitMirror.FRONT_BACK -> NMSMirror.FRONT_BACK
    BukkitMirror.LEFT_RIGHT -> NMSMirror.LEFT_RIGHT
}


object NMS {
    val handler: NMSHandler = NMS_1_20_4_Handler()
}