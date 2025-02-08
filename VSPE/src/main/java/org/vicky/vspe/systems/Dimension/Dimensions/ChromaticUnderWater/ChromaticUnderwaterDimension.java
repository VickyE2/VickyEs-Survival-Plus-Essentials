package org.vicky.vspe.systems.Dimension.Dimensions.ChromaticUnderWater;

import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.vicky.vspe.systems.Dimension.BaseDimension;
import org.vicky.vspe.systems.Dimension.DimensionType;
import org.vicky.vspe.systems.Dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.systems.Dimension.Exceptions.WorldNotExistsException;

import java.util.List;

import static org.vicky.vspe.systems.Dimension.Generator.utils.Utilities.generateRandomSeed;

public class ChromaticUnderwaterDimension extends BaseDimension {

    public ChromaticUnderwaterDimension() throws WorldNotExistsException, NoGeneratorException {
        super(
                "Chromatic Underwater",
                "chromatic_underwater",
                List.of(DimensionType.AQUATIC_WORLD),
                World.Environment.NORMAL,
                generateRandomSeed(),
                WorldType.NORMAL,
                false,
                ChromaticUnderwater.class
        );
    }

    @Override
    public void applyMechanics(Player var1) {
        var1.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, -1, 2));
        var1.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 2));
        var1.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, -1, 2));
    }

    @Override
    public void disableMechanics(Player var1) {
        var1.removePotionEffect(PotionEffectType.WATER_BREATHING);
        var1.removePotionEffect(PotionEffectType.NIGHT_VISION);
        var1.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
    }

    @Override
    public void applyJoinMechanics(Player var1) {

    }
}
