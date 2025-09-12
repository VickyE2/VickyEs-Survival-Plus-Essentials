package org.vicky.vspe_forge.registers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.vicky.vspe_forge.VspeForge;
import org.vicky.vspe_forge.objects.SmartVineBlock;

public class Blocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, VspeForge.MODID);
    public static final DeferredRegister<Item> BLOCK_ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, VspeForge.MODID);

    public static RegistryObject<Block> MAGENTA_FROST_LEAVES = BLOCKS.register("magenta_frost_leaves",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .destroyTime(0.5f)
                    .strength(0.5f)
                    .sound(SoundType.LARGE_AMETHYST_BUD)
            )
    );
    public static final RegistryObject<Block> MAGENTA_FROST_VINE = BLOCKS.register("magenta_frost_vine",
            () -> new SmartVineBlock(BlockBehaviour.Properties.of()
                    .noOcclusion()
                    .strength(0.2f)
                    .sound(SoundType.SMALL_AMETHYST_BUD)
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
                            .rarity(Rarity.EPIC)
            )
    );
}
