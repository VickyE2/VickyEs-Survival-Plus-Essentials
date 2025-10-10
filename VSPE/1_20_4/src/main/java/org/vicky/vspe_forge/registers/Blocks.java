package org.vicky.vspe_forge.registers;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.objects.SmartVineBlock;

public class Blocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, VspeForge.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VspeForge.MODID);

    public static RegistryObject<Block> MAGENTA_FROST_LEAVES = BLOCKS.register("magenta_frost_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.ICE)
                    .randomTicks()
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY)
                    .destroyTime(0.12f)
                    .strength(0.2f)
                    .sound(SoundType.AMETHYST_CLUSTER)
                    .isValidSpawn((state, level, pos, type) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)
            )
    );
    public static final RegistryObject<Block> MAGENTA_FROST_VINE = BLOCKS.register("magenta_frost_vine",
            () -> new SmartVineBlock(BlockBehaviour.Properties.of()
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .randomTicks()
                    .destroyTime(0.02f)
                    .strength(0.09f)
                    .sound(SoundType.SMALL_AMETHYST_BUD),
                    3, MAGENTA_FROST_LEAVES.get(), true
            )
    );

    public static RegistryObject<Item> MAGENTA_FROST_LEAVES_ITEM = BLOCK_ITEMS.register("magenta_frost_leaves",
            () -> new BlockItem(MAGENTA_FROST_LEAVES.get(),
                    new Item.Properties()
                            .rarity(Rarity.EPIC)
            )
    );
    public static RegistryObject<Item> MAGENTA_FROST_VINE_ITEM = BLOCK_ITEMS.register("magenta_frost_vine",
            () -> new BlockItem(MAGENTA_FROST_VINE.get(),
                    new Item.Properties()
                            .setNoRepair()
                            .rarity(Rarity.EPIC)
            )
    );
}
