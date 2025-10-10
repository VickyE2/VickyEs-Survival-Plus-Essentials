package org.vicky.vspe_forge.registers;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.vicky.vspe_forge.dimension.ForgePlatformStructurePiece;
import org.vicky.vspe_forge.dimension.ForgeTypePlatformStructure;

import static org.vicky.vspe_forge.VspeForge.MODID;

public class Dimensions {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);

    public static final RegistryObject<StructureType<ForgeTypePlatformStructure>> PLATFORM_STRUCTURE =
            STRUCTURE_TYPES.register("forge_platform_structure",
                    () -> () -> ForgeTypePlatformStructure.CODEC);
    public static final RegistryObject<StructurePieceType> FORGE_PLATFORM_PIECE =
            STRUCTURE_PIECES.register("forge_platform_piece",
                    () -> ForgePlatformStructurePiece::new);
}
