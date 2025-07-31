package org.vicky.vspe.systems.dimension.dimensions.ChromaticUnderWater;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.vicky.guiparent.GuiCreator;
import org.vicky.vspe.systems.dimension.BaseDimension;
import org.vicky.vspe.systems.dimension.DimensionType;
import org.vicky.vspe.systems.dimension.Exceptions.NoGeneratorException;
import org.vicky.vspe.systems.dimension.Exceptions.WorldNotExistsException;
import org.vicky.vspe.utilities.Manager.ManagerNotFoundException;

import java.util.List;

import static org.vicky.vspe.systems.dimension.Generator.utils.Utilities.generateRandomSeed;

public class ChromaticUnderwaterDimension extends BaseDimension {

    public ChromaticUnderwaterDimension() throws WorldNotExistsException, NoGeneratorException, ManagerNotFoundException {
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
        isGlobalSpawning(new Location(this.getWorld(), 0, 120, 0));
    }

    @Override
    protected List<GuiCreator.ItemConfig> dimensionAdvancementGainItems() {
        return List.of();
    }

    @Override
    protected void dimensionAdvancementGainProcedures(Player player) {

    }

    @Override
    public void applyJoinMechanics(Player var1) {

    }

    @Override
    protected boolean dimensionJoinCondition(Player player) {
        return false;
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
}
