package org.vicky.vspe_forge.logic;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.vicky.vspe_forge.VspeForge;

import java.util.Optional;

@Mod.EventBusSubscriber(modid = VspeForge.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CommandSpawnStructure {
    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(
                Commands.literal("spawnstructure")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("structure", ResourceLocationArgument.id())
                                .executes(ctx -> spawn(
                                        ctx.getSource(),
                                        ResourceLocationArgument.getId(ctx, "structure")
                                ))
                        )
        );
    }

    private static int spawn(CommandSourceStack source, ResourceLocation id) throws CommandSyntaxException {
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPos.containing(source.getPosition());

        // Retrieve structure from registry
        var optStruct = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .getOptional(id);

        if (optStruct.isEmpty()) {
            source.sendFailure(Component.literal("Unknown structure: " + id));
            return 0;
        }

        Structure structure = optStruct.get();
        spawnStructureNow(level, structure, pos);
        source.sendSuccess(() -> Component.literal("Spawned " + id + " at " + pos), true);
        return 1;
    }

    private static void spawnStructureNow(ServerLevel level, Structure structure, BlockPos pos) {
        // Create context similar to worldgen
        ChunkPos chunkPos = new ChunkPos(pos);
        RandomSource random = RandomSource.create();

        // Wrap a fake Structure.GenerationContext
        Structure.GenerationContext context = new Structure.GenerationContext(
                level.registryAccess(),
                level.getChunkSource().getGenerator(),
                level.getChunkSource().getGenerator().getBiomeSource(),
                level.getChunkSource().randomState(),
                level.getServer().getStructureManager(),
                level.getSeed(),
                chunkPos,
                level,
                holder -> true
        );

        Optional<Structure.GenerationStub> stub = structure.findValidGenerationPoint(context);

        if (stub.isEmpty()) {
            level.players().forEach(p -> p.sendSystemMessage(Component.literal("Structure refused to generate here.")));
            return;
        }

        Structure.GenerationStub stubVal = stub.get();

        // Actually place the structure now
        placeStubInWorld(stubVal, level, level.getChunkSource().getGenerator(), random, BoundingBox.infinite(), chunkPos);

        // Optionally notify players
        level.players().forEach(p -> p.sendSystemMessage(Component.literal("Structure spawned.")));
    }

    public static boolean placeStubInWorld(
            Structure.GenerationStub stub,
            ServerLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos) {
        StructureManager structureManager = level.structureManager();
        StructurePiecesBuilder builder = stub.getPiecesBuilder();

        boolean placed = false;

        for (StructurePiece piece : builder.build().pieces()) {
            if (piece.getBoundingBox().intersects(box)) {
                piece.postProcess(
                        level,
                        structureManager, // ✅ This one, not template manager
                        generator,
                        random,
                        box,
                        chunkPos,
                        stub.position()
                );
                placed = true;
            }
        }

        return placed;
    }

}