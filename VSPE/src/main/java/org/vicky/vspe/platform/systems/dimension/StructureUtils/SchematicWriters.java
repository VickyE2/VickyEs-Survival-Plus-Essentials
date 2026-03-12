package org.vicky.vspe.platform.systems.dimension.StructureUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

import net.sandrohc.schematic4j.nbt.io.NamedTag;
import net.sandrohc.schematic4j.nbt.io.NBTUtil;
import net.sandrohc.schematic4j.nbt.tag.*;
import net.sandrohc.schematic4j.parser.LitematicaParser;
import net.sandrohc.schematic4j.parser.SchematicaParser;
import net.sandrohc.schematic4j.parser.SpongeParser;
import net.sandrohc.schematic4j.schematic.SpongeSchematic;
import net.sandrohc.schematic4j.schematic.SchematicaSchematic;
import net.sandrohc.schematic4j.schematic.LitematicaSchematic;
import net.sandrohc.schematic4j.schematic.types.SchematicBlock;
import net.sandrohc.schematic4j.schematic.types.SchematicBlockPos;

/**
 * Utilities to serialize schematic4j Schematic objects to files using the bundled NBT util.
 *
 * NOTE: Adjust Tag constructors/put methods if your library version differs slightly.
 */
public final class SchematicWriters {

    // -------------------------
    // Sponge (.schem) writer
    // -------------------------
    public static void writeSpongeSchem(SpongeSchematic schematic, Path outFile, boolean compressed) throws IOException {
        CompoundTag root = new CompoundTag();

        // Version and dimensions
        root.put(SpongeParser.NBT_VERSION, new IntTag(schematic.version));
        if (schematic.dataVersion != null) root.put(SpongeParser.NBT_DATA_VERSION, new IntTag(schematic.dataVersion));
        // Width/Height/Length are shorts in the parser
        root.put(SpongeParser.NBT_WIDTH, new ShortTag((short) schematic.width));
        root.put(SpongeParser.NBT_HEIGHT, new ShortTag((short) schematic.height));
        root.put(SpongeParser.NBT_LENGTH, new ShortTag((short) schematic.length));

        // Metadata (optional)
        if (schematic.metadata != null) {
            CompoundTag meta = new CompoundTag();
            if (schematic.metadata.name != null) meta.put(SpongeParser.NBT_METADATA_NAME, new StringTag(schematic.metadata.name));
            if (schematic.metadata.author != null) meta.put(SpongeParser.NBT_METADATA_AUTHOR, new StringTag(schematic.metadata.author));
            if (schematic.metadata.date != null) meta.put(SpongeParser.NBT_METADATA_DATE, new LongTag(schematic.metadata.date.toLocalTime().getHour()));
            // RequiredMods and extra values left out for brevity; add if needed.
            root.put(SpongeParser.NBT_METADATA, meta);
        }

        // Blocks container (v3 uses "Blocks" compound)
        CompoundTag blocksTag = new CompoundTag();

        // Palette (SchematicBlock[] -> name:Int index)
        if (schematic.blockPalette != null) {
            CompoundTag palette = new CompoundTag();
            for (int i = 0; i < schematic.blockPalette.length; i++) {
                SchematicBlock b = schematic.blockPalette[i];
                // key must be the block-name with properties in string form; parser expects key -> IntTag(index)
                String key = b.name;
                if (b.states != null && !b.states.isEmpty()) {
                    // simple stable encoding -- adjust if your SchematicBlock has a canonical stringify
                    StringBuilder sb = new StringBuilder(key).append('[');
                    b.states.forEach((k, v) -> sb.append(k).append('=').append(v).append(','));
                    sb.append(']');
                    key = sb.toString();
                }
                palette.put(key, new IntTag(i));
            }
            blocksTag.put(SpongeParser.NBT_PALETTE, palette);
            // PaletteMax (optional)
            blocksTag.put(SpongeParser.NBT_PALETTE_MAX, new IntTag(schematic.blockPalette.length));
        }

        // Blocks data: `schematic.blocks` is expected as int[] indices into palette
        if (schematic.blocks != null) {
            byte[] data = encodeVarIntArray(schematic.blocks);
            // v3 uses "Data" inside "Blocks" compound
            blocksTag.put(SpongeParser.NBT_V3_DATA, new ByteArrayTag(data));
        }

        root.put(SpongeParser.NBT_V3_BLOCKS, blocksTag);

        // Biomes / entities / block entities omitted here (add analogous to blocks if present).

        // Wrap and write
        NamedTag named = new NamedTag(null, root);
        NBTUtil.Writer.write(named).compressed(compressed).to(outFile);
    }

    // VarInt encoder used by Sponge format: encode every int as VarInt in a continuous byte array
    private static byte[] encodeVarIntArray(int[] ints) {
        ArrayList<Byte> out = new ArrayList<>();
        for (int v : ints) {
            // VarInt (unsigned) encoding - write little 7-bit chunks with MSB continuation flag
            int value = v;
            do {
                byte temp = (byte) (value & 0x7F);
                value >>>= 7;
                if (value != 0) temp |= 0x80;
                out.add(temp);
            } while (value != 0);
        }
        // convert to primitive byte[]
        byte[] arr = new byte[out.size()];
        for (int i = 0; i < out.size(); i++) arr[i] = out.get(i);
        return arr;
    }

    // -------------------------
    // Schematica (.schematic) writer
    // -------------------------
    public static void writeSchematica(SchematicaSchematic schematic, Path outFile, boolean compressed) throws IOException {
        CompoundTag root = new CompoundTag();

        root.put(SchematicaParser.NBT_WIDTH, new ShortTag((short) schematic.width));
        root.put(SchematicaParser.NBT_HEIGHT, new ShortTag((short) schematic.height));
        root.put(SchematicaParser.NBT_LENGTH, new ShortTag((short) schematic.length));

        int total = schematic.width * schematic.height * schematic.length;

        // Build palette mapping (String[] palette -> index is position in array) if provided, otherwise build from blockIds and blockPalette
        String[] palette = schematic.blockPalette;
        if (palette == null) {
            // fallback: build palette from used blockIds + names (not ideal; better to set blockPalette prior)
            palette = new String[] {"minecraft:air"};
        }

        // blocksRaw: low 8 bits of id
        byte[] blocksRaw = new byte[total];
        // metadata: 8-bit metadata array
        byte[] dataRaw = new byte[total];

        // If schematic.blockIds contains palette indices, we need to map to names and/or ids.
        // Here we assume schematic.blockIds already contains palette indices that index into blockPalette.
        for (int i = 0; i < Math.min(total, schematic.blockIds.length); i++) {
            int id = schematic.blockIds[i];
            // produce a single byte for lower 8 bits
            blocksRaw[i] = (byte) (id & 0xFF);
            dataRaw[i] = (byte) (i < schematic.blockMetadata.length ? (schematic.blockMetadata[i] & 0xFF) : 0);
        }

        root.put(SchematicaParser.NBT_BLOCKS, new ByteArrayTag(blocksRaw));
        root.put(SchematicaParser.NBT_DATA, new ByteArrayTag(dataRaw));

        // If any id > 0xFF we must produce AddBlocks nibble array
        boolean needsAdd = IntStream.of(schematic.blockIds).anyMatch(v -> (v & ~0xFF) != 0);
        if (needsAdd) {
            // create nibble array length = ceil(total / 2)
            int nibbleLen = (total + 1) / 2;
            byte[] nibbleBytes = new byte[nibbleLen];
            for (int i = 0; i < total; i++) {
                int high = (schematic.blockIds[i] >> 8) & 0xF; // take high 4 bits
                int pairIndex = i / 2;
                if (i % 2 == 0) {
                    nibbleBytes[pairIndex] = (byte) ((high & 0xF) << 4);
                } else {
                    nibbleBytes[pairIndex] |= (byte) (high & 0xF);
                }
            }
            root.put(SchematicaParser.NBT_ADD_BLOCKS, new ByteArrayTag(nibbleBytes));
        }

        // mapping (SchematicaMapping) is expected by parser; create mapping mapping blockName->index
        if (schematic.blockPalette != null) {
            CompoundTag mapping = new CompoundTag();
            for (int i = 0; i < schematic.blockPalette.length; i++) {
                String blockName = schematic.blockPalette[i];
                mapping.put(blockName, new IntTag(i));
            }
            root.put(SchematicaParser.NBT_MAPPING_SCHEMATICA, mapping);
        }

        // icon/material etc omitted for brevity

        NamedTag named = new NamedTag(null, root);
        NBTUtil.Writer.write(named).compressed(compressed).to(outFile);
    }

    // -------------------------
    // Litematica (.litematic) writer
    // -------------------------
    public static void writeLitematica(LitematicaSchematic schematic, Path outFile, boolean compressed) throws IOException {
        CompoundTag root = new CompoundTag();
        root.put(LitematicaParser.NBT_VERSION, new IntTag(schematic.version));
        if (schematic.minecraftDataVersion != null) root.put(LitematicaParser.NBT_MINECRAFT_DATA_VERSION, new IntTag(schematic.minecraftDataVersion));

        // metadata (optional) - minimal
        if (schematic.metadata != null) {
            CompoundTag meta = new CompoundTag();
            if (schematic.metadata.name != null) meta.put(LitematicaParser.NBT_METADATA_NAME, new StringTag(schematic.metadata.name));
            if (schematic.metadata.description != null) meta.put(LitematicaParser.NBT_METADATA_DESCRIPTION, new StringTag(schematic.metadata.description));
            if (schematic.metadata.author != null) meta.put(LitematicaParser.NBT_METADATA_AUTHOR, new StringTag(schematic.metadata.author));
            root.put(LitematicaParser.NBT_METADATA, meta);
        }

        // Regions compound (single region supported)
        CompoundTag regions = new CompoundTag();

        // We'll create one region named "Region0"
        if (schematic.regions != null && schematic.regions.length > 0) {
            for (int r = 0; r < schematic.regions.length; r++) {
                LitematicaSchematic.Region region = schematic.regions[r];
                CompoundTag regionTag = new CompoundTag();

                // Position & Size: both SchematicBlockPos -> Compound
                if (region.position != null) {
                    regionTag.put(LitematicaParser.NBT_REGION_POSITION, SchematicBlockPosToCompound(region.position));
                }
                if (region.size != null) {
                    regionTag.put(LitematicaParser.NBT_REGION_SIZE, SchematicBlockPosToCompound(region.size));
                }

                // BlockStatePalette: list of CompoundTag entries each containing "Name" and "Properties" as appropriate
                if (region.blockStatePalette != null) {
                    ListTag<CompoundTag> paletteList = new ListTag<>(CompoundTag.class);
                    for (SchematicBlock sb : region.blockStatePalette) {
                        CompoundTag entry = new CompoundTag();
                        entry.put("Name", new StringTag(sb.name));
                        if (sb.states != null && !sb.states.isEmpty()) {
                            CompoundTag props = new CompoundTag();
                            sb.states.forEach((k, v) -> props.put(k, new StringTag(v)));
                            entry.put("Properties", props);
                        }
                        paletteList.add(entry);
                    }
                    regionTag.put(LitematicaParser.NBT_REGION_BLOCK_STATE_PALETTE, paletteList);
                }

                // BlockStates: long[] bitpacked
                if (region.blockStates != null) {
                    long[] packed = packBlockStates(region.blockStates, region.blockStatePalette == null ? 0 : region.blockStatePalette.length);
                    regionTag.put(LitematicaParser.NBT_REGION_BLOCK_STATES, new LongArrayTag(packed));
                }

                // TileEntities/Entities omitted for brevity
                regions.put("Region" + r, regionTag);
            }
        }

        root.put(LitematicaParser.NBT_REGIONS, regions);

        NamedTag named = new NamedTag(null, root);
        NBTUtil.Writer.write(named).compressed(compressed).to(outFile);
    }

    private static CompoundTag SchematicBlockPosToCompound(SchematicBlockPos p) {
        CompoundTag out = new CompoundTag();
        out.put("x", new IntTag(p.x));
        out.put("y", new IntTag(p.y));
        out.put("z", new IntTag(p.z));
        return out;
    }

    // Pack blockStates[] indices into long[] using bitsPerEntry similar to the parser logic
    private static long[] packBlockStates(int[] blockStates, int paletteSize) {
        if (blockStates == null) return new long[0];
        int total = blockStates.length;
        int bitsPerEntry = Math.max(2, Integer.SIZE - Integer.numberOfLeadingZeros(Math.max(1, paletteSize - 1)));
        long maxEntry = (1L << bitsPerEntry) - 1L;
        long neededBits = (long) total * bitsPerEntry;
        int longsNeeded = (int) ((neededBits + 63) / 64);
        long[] packed = new long[longsNeeded];

        for (int i = 0; i < total; i++) {
            long value = blockStates[i] & maxEntry;
            long startBit = (long) i * bitsPerEntry;
            int startLong = (int) (startBit >>> 6);
            int startOffset = (int) (startBit & 63);
            packed[startLong] |= (value << startOffset);
            int endLong = startLong;
            int remainingBits = bitsPerEntry + startOffset;
            if (remainingBits > 64) {
                // split across two longs
                endLong = startLong + 1;
                packed[endLong] |= (value >>> (64 - startOffset));
            }
        }
        return packed;
    }
}

