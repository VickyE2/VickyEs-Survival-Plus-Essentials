package org.vicky.vspe.structure_gen

import org.bukkit.plugin.java.JavaPlugin
import org.vicky.utilities.ConfigManager
import org.vicky.utilities.ContextLogger.ContextLogger
import org.vicky.utilities.Theme.ThemeUnzipper
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

inline fun <reified T : Any> ConfigManager.readDataClass(path: String = ""): T? {
    return try {
        val node = if (path.isEmpty()) rootNode else rootNode.node(*path.split(".").toTypedArray())
        val type = T::class.java
        val serializer = node.options().serializers().get(type)
        serializer?.deserialize(type, node)
    } catch (e: Exception) {
        logger.print("❌ Failed to load data class for path '$path': ${e.message}")
        null
    }
}

class KraterosGenerationEngine (
    private val javaPlugin: JavaPlugin
) {
    val logger = ContextLogger(ContextLogger.ContextType.SYSTEM, "STRUCTURE_ENGINE")
    private val _loadedStructures = mutableListOf<StructurePack>()
    val loadedStructures: List<StructurePack> get() = _loadedStructures

    fun generateStructurePacks() {
        val themesPath = Paths.get(javaPlugin.dataFolder.absolutePath, "structure_packs")
        val zipFiles = ThemeUnzipper.getAllZipFiles(themesPath.toString())
        if (zipFiles.isNotEmpty()) {
            zip@for (zipPath in zipFiles) {
                val config = ConfigManager(false)

                // Open zip and check contents
                var shouldContinue = false

                ZipFile(zipPath.toFile()).use { zipFile ->
                    val requiredEntry = zipFile.getEntry("structure_pack.yml")
                    if (requiredEntry == null || requiredEntry.isDirectory) {
                        logger.print(
                            "Required file, structure_pack.yml, not found in Theme Pack: ${zipFile.name}",
                            true
                        )
                        shouldContinue = true
                    }
                }

                if (shouldContinue) continue@zip

                // Now safe to load config AFTER zip is closed (uses new ZipFile internally)
                config.loadConfigFromZip(zipPath, "structure_pack.yml")
                val pack: StructurePack? = config.readDataClass()

                if (pack != null) {
                    logger.print("Loaded structure pack: ${pack.name}")
                    logger.print("Proceeding to datapack generation", ContextLogger.LogType.PENDING)

                    val outputZipFile = File(javaPlugin.dataFolder,"datapacks/${pack.name}_generated.zip")

                    ZipOutputStream(FileOutputStream(outputZipFile)).use { zipStream ->
                        val packZip = ZipFile(zipPath.toFile())

                        pack.processPieces(zipStream, packZip)
                        pack.processBuildings(zipStream)
                        pack.processLootTables(zipStream)
                        pack.processRoads(zipStream, packZip)
                    }

                    pack.withNamespace()
                    _loadedStructures.add(pack)
                } else {
                    logger.print("Failed to load structure pack from $zipPath", true)
                }
            }
        }
    }

    private fun StructurePack.processPieces(
        zipStream: ZipOutputStream,
        packZip: ZipFile
    ) {
        for (piece in this.pieces) {
            val piecePool = generatePiecePool(piece, namespace)
            addToZip(zipStream, "data/${this.namespace}/worldgen/template_pool/${piece.name}_pool.json", piecePool)
            addZippedToZip(zipStream, packZip, "data/${this.namespace}/structures/${piece.file}", "pieces/${piece.file}")
        }
    }

    private fun StructurePack.processBuildings(
        zipStream: ZipOutputStream
    ) {
        building@ for (building in this.buildings) {
            for (component in building.components) {
                for (piece in component.pieces) {
                    if (this.pieces.none { it.name == piece }) {
                        logger.print("Unable to load building ${building.name} : No piece named $piece")
                        continue@building
                    }
                }
            }
            val structureSet = generateStructureSet(building, namespace)
            val structureJson = generateStructureJson(building, namespace)
            val poolJson = generatePool(building, namespace)
            addToZip(zipStream, "data/${this.namespace}/worldgen/structure_set/${building.name}_set.json", structureSet)
            addToZip(zipStream, "data/${this.namespace}/worldgen/structure/${building.name}.json", structureJson)
            addToZip(zipStream, "data/${this.namespace}/worldgen/template_pool/${building.name}_start.json", poolJson)
        }
    }

    private fun addToZip(zip: ZipOutputStream, entryPath: String, content: String) {
        val zipEntry = ZipEntry(entryPath.replace("\\", "/")) // Always use forward slashes
        zip.putNextEntry(zipEntry)
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }
    private fun addFileToZip(zip: ZipOutputStream, sourceFile: File, entryPath: String) {
        val zipEntry = ZipEntry(entryPath.replace("\\", "/"))
        zip.putNextEntry(zipEntry)
        sourceFile.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }
    private fun addZippedToZip(zip: ZipOutputStream, zipFile: ZipFile, entryPath: String, fileName: String) {
        val zipEntry = ZipEntry(entryPath.replace("\\", "/"))
        zip.putNextEntry(zipEntry)
        zipFile.getInputStream(zipFile.getEntry(fileName))
            .use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun StructurePack.processLootTables(zipStream: ZipOutputStream) {
        for (loot_table in this.loot_tables) {
            val json = loot_table.toMinecraftJson()
            addToZip(zipStream, "data/${this.namespace}/worldgen/template_pool/${loot_table.name}_pool.json", json)
        }
    }

    private fun StructurePack.processRoads(zip: ZipOutputStream, packZip: ZipFile) {
        val roadSet = this.road
        val baseName = "${namespace}_road"

        for ((type, file) in roadSet.files) {
            val fullName = "${baseName}_${type}"

            val structureSet = generateStructureSetTemplate(namespace, fullName, 0)
            val structureJson = generateStructureTemplate(namespace, fullName + "_poll", 0)
            val poolJson = generatePoolTemplate(namespace, file.name, 1, mapOf(), StructureType.ROADS)

            addToZip(zip, "data/${namespace}/worldgen/structure_set/${fullName}_set.json", structureSet)
            addToZip(zip, "data/${namespace}/worldgen/structure/${fullName}.json", structureJson)
            addToZip(zip, "data/${namespace}/worldgen/template_pool/${fullName}_pool.json", poolJson)
            addZippedToZip(zip, packZip, "data/${namespace}/structures/${file.name}", "roads/${file.name}")
        }
    }

    fun StructurePack.withNamespace(): StructurePack {
        this.buildings.forEach { it.namespace = this.namespace }
        this.pieces.forEach { it.namespace = this.namespace }
        this.decorators.forEach { it.namespace = this.namespace }
        this.road.namespace = this.namespace
        for (it in road.files.values) {
            it.namespace = this.namespace
        }
        return this
    }
}
